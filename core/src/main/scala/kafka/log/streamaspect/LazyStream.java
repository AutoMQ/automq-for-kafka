/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kafka.log.streamaspect;

import com.automq.stream.api.AppendResult;
import com.automq.stream.api.CreateStreamOptions;
import com.automq.stream.api.FetchResult;
import com.automq.stream.api.OpenStreamOptions;
import com.automq.stream.api.RecordBatch;
import com.automq.stream.api.Stream;
import com.automq.stream.api.StreamClient;
import com.automq.stream.utils.FutureUtil;
import com.automq.stream.utils.ThreadUtils;
import com.automq.stream.utils.Threads;
import kafka.log.streamaspect.client.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Lazy stream, create stream when append record.
 */
public class LazyStream implements Stream {
    private static final Logger LOGGER = LoggerFactory.getLogger(LazyStream.class);
    private static final ExecutorService STREAM_LISTENER_EXECUTOR = Threads.newSingleThreadScheduledExecutor(
            ThreadUtils.createThreadFactory("lazy_stream_listener_%d", true), LOGGER);
    public static final long NOOP_STREAM_ID = -1L;
    private static final Stream NOOP_STREAM = new NoopStream();
    private final String name;
    private final StreamClient client;
    private final int replicaCount;
    private final long epoch;
    private volatile Stream inner = NOOP_STREAM;
    private ElasticStreamEventListener eventListener;

    public LazyStream(String name, long streamId, StreamClient client, int replicaCount, long epoch) throws IOException {
        this.name = name;
        this.client = client;
        this.replicaCount = replicaCount;
        this.epoch = epoch;
        if (streamId != NOOP_STREAM_ID) {
            try {
                // open exist stream
                inner = client.openStream(streamId, OpenStreamOptions.newBuilder().epoch(epoch).build()).get();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof IOException) {
                    throw (IOException) (e.getCause());
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
            LOGGER.info("opened existing stream: stream_id={}, epoch={}, name={}", streamId, epoch, name);
        }
    }

    @Override
    public long streamId() {
        return inner.streamId();
    }

    @Override
    public long startOffset() {
        return inner.startOffset();
    }

    @Override
    public long nextOffset() {
        return inner.nextOffset();
    }


    @Override
    public synchronized CompletableFuture<AppendResult> append(RecordBatch recordBatch) {
        if (this.inner == NOOP_STREAM) {
            try {
                CompletableFuture<Stream> creatingCf = client.createAndOpenStream(CreateStreamOptions.newBuilder().replicaCount(replicaCount)
                        .epoch(epoch).build());
                this.inner = new CreatingStream(creatingCf);
                if (Context.isTestMode()) {
                    this.inner = creatingCf.get();
                    notifyListener(ElasticStreamMetaEvent.STREAM_DO_CREATE);
                } else {
                    creatingCf.thenAcceptAsync(stream -> {
                        LOGGER.info("created and opened a new stream: stream_id={}, epoch={}, name={}", this.inner.streamId(), epoch, name);
                        this.inner = stream;
                        notifyListener(ElasticStreamMetaEvent.STREAM_DO_CREATE);
                    }, STREAM_LISTENER_EXECUTOR);
                }
            } catch (Throwable e) {
                return FutureUtil.failedFuture(new IOException(e));
            }
        }
        return inner.append(recordBatch);
    }

    @Override
    public CompletableFuture<Void> trim(long newStartOffset) {
        return inner.trim(newStartOffset);
    }

    @Override
    public CompletableFuture<FetchResult> fetch(long startOffset, long endOffset, int maxBytesHint) {
        return inner.fetch(startOffset, endOffset, maxBytesHint);
    }

    @Override
    public CompletableFuture<Void> close() {
        return inner.close();
    }

    @Override
    public CompletableFuture<Void> destroy() {
        return inner.destroy();
    }

    @Override
    public String toString() {
        return "LazyStream{" + "name='" + name + '\'' + "streamId='" + inner.streamId() + '\'' + ", replicaCount=" + replicaCount + '}';
    }

    public void setListener(ElasticStreamEventListener listener) {
        this.eventListener = listener;
    }

    public void notifyListener(ElasticStreamMetaEvent event) {
        try {
            Optional.ofNullable(eventListener).ifPresent(listener -> listener.onEvent(inner.streamId(), event));
        } catch (Throwable e) {
            LOGGER.error("got notify listener error", e);
        }
    }

    static class NoopStream implements Stream {
        @Override
        public long streamId() {
            return NOOP_STREAM_ID;
        }

        @Override
        public long startOffset() {
            return 0;
        }

        @Override
        public long nextOffset() {
            return 0;
        }

        @Override
        public CompletableFuture<AppendResult> append(RecordBatch recordBatch) {
            return FutureUtil.failedFuture(new UnsupportedOperationException("noop stream"));
        }

        @Override
        public CompletableFuture<Void> trim(long newStartOffset) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<FetchResult> fetch(long startOffset, long endOffset, int maxBytesHint) {
            return CompletableFuture.completedFuture(Collections::emptyList);
        }

        @Override
        public CompletableFuture<Void> close() {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<Void> destroy() {
            return CompletableFuture.completedFuture(null);
        }
    }

    static class CreatingStream implements Stream {
        private final CompletableFuture<Stream> creatingCf;

        public CreatingStream(CompletableFuture<Stream> creatingCf) {
            this.creatingCf = creatingCf;
        }

        @Override
        public long streamId() {
            try {
                return creatingCf.thenApply(Stream::streamId).get();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public long startOffset() {
            try {
                return creatingCf.thenApply(Stream::startOffset).get();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public long nextOffset() {
            try {
                return creatingCf.thenApply(Stream::nextOffset).get();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public CompletableFuture<AppendResult> append(RecordBatch recordBatch) {
            return creatingCf.thenCompose(s -> s.append(recordBatch));
        }

        @Override
        public CompletableFuture<FetchResult> fetch(long startOffset, long endOffset, int maxBytesHint) {
            return creatingCf.thenCompose(s -> s.fetch(startOffset, endOffset, maxBytesHint));
        }

        @Override
        public CompletableFuture<Void> trim(long newStartOffset) {
            return creatingCf.thenCompose(s -> s.trim(newStartOffset));
        }

        @Override
        public CompletableFuture<Void> close() {
            return creatingCf.thenCompose(Stream::close);
        }

        @Override
        public CompletableFuture<Void> destroy() {
            return creatingCf.thenCompose(Stream::destroy);
        }
    }
}
