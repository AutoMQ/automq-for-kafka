/*
 * Copyright 2024, AutoMQ CO.,LTD.
 *
 * Use of this software is governed by the Business Source License
 * included in the file BSL.md
 *
 * As of the Change Date specified in that file, in accordance with
 * the Business Source License, use of this software will be governed
 * by the Apache License, Version 2.0
 */

package com.automq.stream.s3.operator;

import com.automq.stream.s3.ByteBufAlloc;
import com.automq.stream.s3.metrics.MetricsLevel;
import com.automq.stream.s3.metrics.TimerUtil;
import com.automq.stream.s3.metrics.stats.S3ObjectStats;
import com.automq.stream.s3.network.ThrottleStrategy;
import com.automq.stream.utils.FutureUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import software.amazon.awssdk.services.s3.model.CompletedPart;


public class MultiPartWriter implements Writer {
    private static final long MAX_MERGE_WRITE_SIZE = 16L * 1024 * 1024;
    final CompletableFuture<String> uploadIdCf = new CompletableFuture<>();
    private final Context context;
    private final S3Operator operator;
    private final String path;
    private final List<CompletableFuture<CompletedPart>> parts = new LinkedList<>();
    private final AtomicInteger nextPartNumber = new AtomicInteger(1);
    /**
     * The minPartSize represents the minimum size of a part for a multipart object.
     */
    private final long minPartSize;
    private final TimerUtil timerUtil = new TimerUtil();
    private final ThrottleStrategy throttleStrategy;
    private final AtomicLong totalWriteSize = new AtomicLong(0L);
    private String uploadId;
    private CompletableFuture<Void> closeCf;
    private ObjectPart objectPart = null;

    public MultiPartWriter(Context context, S3Operator operator, String path, long minPartSize, ThrottleStrategy throttleStrategy) {
        this.context = context;
        this.operator = operator;
        this.path = path;
        this.minPartSize = minPartSize;
        this.throttleStrategy = throttleStrategy;
        init();
    }

    private void init() {
        FutureUtil.propagate(
            operator.createMultipartUpload(path).thenApply(uploadId -> {
                this.uploadId = uploadId;
                return uploadId;
            }),
            uploadIdCf
        );
    }

    @Override
    public CompletableFuture<Void> write(ByteBuf data) {
        totalWriteSize.addAndGet(data.readableBytes());

        if (objectPart == null) {
            objectPart = new ObjectPart(throttleStrategy);
        }
        ObjectPart objectPart = this.objectPart;

        objectPart.write(data);
        if (objectPart.size() > minPartSize) {
            objectPart.upload();
            // finish current part.
            this.objectPart = null;
        }
        return objectPart.getFuture();
    }

    @Override
    public void copyOnWrite() {
        if (objectPart != null) {
            objectPart.copyOnWrite();
        }
    }

    @Override
    public boolean hasBatchingPart() {
        return objectPart != null;
    }

    @Override
    public void copyWrite(String sourcePath, long start, long end) {
        long nextStart = start;
        for (; ; ) {
            long currentEnd = Math.min(nextStart + Writer.MAX_PART_SIZE, end);
            copyWrite0(sourcePath, nextStart, currentEnd);
            nextStart = currentEnd;
            if (currentEnd == end) {
                break;
            }
        }
    }

    public void copyWrite0(String sourcePath, long start, long end) {
        long targetSize = end - start;
        if (objectPart == null) {
            if (targetSize < minPartSize) {
                this.objectPart = new ObjectPart(throttleStrategy);
                objectPart.readAndWrite(sourcePath, start, end);
            } else {
                new CopyObjectPart(sourcePath, start, end);
            }
        } else {
            if (objectPart.size() + targetSize > MAX_MERGE_WRITE_SIZE) {
                long readAndWriteCopyEnd = start + minPartSize - objectPart.size();
                objectPart.readAndWrite(sourcePath, start, readAndWriteCopyEnd);
                objectPart.upload();
                this.objectPart = null;
                new CopyObjectPart(sourcePath, readAndWriteCopyEnd, end);
            } else {
                objectPart.readAndWrite(sourcePath, start, end);
                if (objectPart.size() > minPartSize) {
                    objectPart.upload();
                    this.objectPart = null;
                }
            }
        }
    }

    @Override
    public CompletableFuture<Void> close() {
        if (closeCf != null) {
            return closeCf;
        }

        if (objectPart != null) {
            // force upload the last part which can be smaller than minPartSize.
            objectPart.upload();
            objectPart = null;
        }

        S3ObjectStats.getInstance().objectStageReadyCloseStats.record(timerUtil.elapsedAs(TimeUnit.NANOSECONDS));
        closeCf = new CompletableFuture<>();
        CompletableFuture<Void> uploadDoneCf = uploadIdCf.thenCompose(uploadId -> CompletableFuture.allOf(parts.toArray(new CompletableFuture[0])));
        FutureUtil.propagate(uploadDoneCf.thenCompose(nil -> operator.completeMultipartUpload(path, uploadId, genCompleteParts())), closeCf);
        closeCf.whenComplete((nil, ex) -> {
            S3ObjectStats.getInstance().objectStageTotalStats.record(timerUtil.elapsedAs(TimeUnit.NANOSECONDS));
            S3ObjectStats.getInstance().objectNumInTotalStats.add(MetricsLevel.DEBUG, 1);
            S3ObjectStats.getInstance().objectUploadSizeStats.record(totalWriteSize.get());
        });
        return closeCf;
    }

    @Override
    public CompletableFuture<Void> release() {
        // wait for all ongoing uploading parts to finish and release pending part
        return CompletableFuture.allOf(parts.toArray(new CompletableFuture[0])).whenComplete((nil, ex) -> {
            if (objectPart != null) {
                objectPart.release();
            }
        });
    }

    private List<CompletedPart> genCompleteParts() {
        return this.parts.stream().map(cf -> {
            try {
                return cf.get();
            } catch (Throwable e) {
                // won't happen.
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    class ObjectPart {
        private final int partNumber = nextPartNumber.getAndIncrement();
        private final CompletableFuture<CompletedPart> partCf = new CompletableFuture<>();
        private final ThrottleStrategy throttleStrategy;
        private CompositeByteBuf partBuf = ByteBufAlloc.compositeByteBuffer();
        private CompletableFuture<Void> lastRangeReadCf = CompletableFuture.completedFuture(null);
        private long size;

        public ObjectPart(ThrottleStrategy throttleStrategy) {
            this.throttleStrategy = throttleStrategy;
            parts.add(partCf);
        }

        public void write(ByteBuf data) {
            size += data.readableBytes();
            // ensure addComponent happen before following write or copyWrite.
            this.lastRangeReadCf = lastRangeReadCf.thenAccept(nil -> partBuf.addComponent(true, data));
        }

        public void copyOnWrite() {
            int size = partBuf.readableBytes();
            if (size > 0) {
                ByteBuf buf = ByteBufAlloc.byteBuffer(size, context.allocType());
                buf.writeBytes(partBuf.duplicate());
                CompositeByteBuf copy = ByteBufAlloc.compositeByteBuffer().addComponent(true, buf);
                this.partBuf.release();
                this.partBuf = copy;
            }
        }

        public void readAndWrite(String sourcePath, long start, long end) {
            size += end - start;
            // TODO: parallel read and sequence add.
            this.lastRangeReadCf = lastRangeReadCf
                .thenCompose(nil -> operator.rangeRead(sourcePath, start, end, throttleStrategy))
                .thenAccept(buf -> partBuf.addComponent(true, buf));
        }

        public void upload() {
            this.lastRangeReadCf.whenComplete((nil, ex) -> {
                if (ex != null) {
                    partCf.completeExceptionally(ex);
                } else {
                    upload0();
                }
            });
        }

        private void upload0() {
            TimerUtil timerUtil = new TimerUtil();
            FutureUtil.propagate(uploadIdCf.thenCompose(uploadId -> operator.uploadPart(path, uploadId, partNumber, partBuf, throttleStrategy)), partCf);
            partCf.whenComplete((nil, ex) -> {
                S3ObjectStats.getInstance().objectStageUploadPartStats.record(timerUtil.elapsedAs(TimeUnit.NANOSECONDS));
            });
        }

        public long size() {
            return size;
        }

        public CompletableFuture<Void> getFuture() {
            return partCf.thenApply(nil -> null);
        }

        public void release() {
            partBuf.release();
        }
    }

    class CopyObjectPart {
        private final CompletableFuture<CompletedPart> partCf = new CompletableFuture<>();

        public CopyObjectPart(String sourcePath, long start, long end) {
            int partNumber = nextPartNumber.getAndIncrement();
            parts.add(partCf);
            FutureUtil.propagate(uploadIdCf.thenCompose(uploadId -> operator.uploadPartCopy(sourcePath, path, start, end, uploadId, partNumber)), partCf);
        }

        public CompletableFuture<Void> getFuture() {
            return partCf.thenApply(nil -> null);
        }
    }
}
