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

package com.automq.stream.s3.metrics;

import io.opentelemetry.api.common.AttributeKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class S3StreamMetricsConstant {
    // value = 16KB * 2^i
    public static final String[] OBJECT_SIZE_BUCKET_NAMES = {
        "16KB",
        "32KB",
        "64KB",
        "128KB",
        "256KB",
        "512KB",
        "1MB",
        "2MB",
        "4MB",
        "8MB",
        "16MB",
        "32MB",
        "64MB",
        "128MB",
        "inf"};

    public static final String UPLOAD_SIZE_METRIC_NAME = "upload.size";
    public static final String DOWNLOAD_SIZE_METRIC_NAME = "download.size";
    public static final String OPERATION_COUNT_METRIC_NAME = "operation.count";
    public static final String OPERATION_LATENCY_METRIC_NAME = "operation.latency";
    public static final String OBJECT_COUNT_METRIC_NAME = "object.count";
    public static final String OBJECT_STAGE_COST_METRIC_NAME = "object.stage.cost";
    public static final String NETWORK_INBOUND_USAGE_METRIC_NAME = "network.inbound.usage";
    public static final String NETWORK_OUTBOUND_USAGE_METRIC_NAME = "network.outbound.usage";
    public static final String NETWORK_INBOUND_AVAILABLE_BANDWIDTH_METRIC_NAME = "network.inbound.available.bandwidth";
    public static final String NETWORK_OUTBOUND_AVAILABLE_BANDWIDTH_METRIC_NAME = "network.outbound.available.bandwidth";
    public static final String NETWORK_INBOUND_LIMITER_QUEUE_SIZE_METRIC_NAME = "network.inbound.limiter.queue.size";
    public static final String NETWORK_OUTBOUND_LIMITER_QUEUE_SIZE_METRIC_NAME = "network.outbound.limiter.queue.size";
    public static final String NETWORK_INBOUND_LIMITER_QUEUE_TIME_METRIC_NAME = "network.inbound.limiter.queue.time";
    public static final String NETWORK_OUTBOUND_LIMITER_QUEUE_TIME_METRIC_NAME = "network.outbound.limiter.queue.time";
    public static final String READ_AHEAD_SIZE_METRIC_NAME = "read.ahead.size";
    public static final String SUM_METRIC_NAME_SUFFIX = ".sum";
    public static final String COUNT_METRIC_NAME_SUFFIX = ".count";
    public static final String P50_METRIC_NAME_SUFFIX = ".50p";
    public static final String P99_METRIC_NAME_SUFFIX = ".99p";
    public static final String MEAN_METRIC_NAME_SUFFIX = ".mean";
    public static final String MAX_METRIC_NAME_SUFFIX = ".max";
    public static final String WAL_START_OFFSET = "wal.start.offset";
    public static final String WAL_TRIMMED_OFFSET = "wal.trimmed.offset";
    public static final String DELTA_WAL_CACHE_SIZE = "delta.wal.cache.size";
    public static final String BLOCK_CACHE_SIZE = "block.cache.size";
    public static final String AVAILABLE_INFLIGHT_READ_AHEAD_SIZE_METRIC_NAME = "available.inflight.readahead.size";
    public static final String READ_AHEAD_QUEUE_TIME_METRIC_NAME = "readahead.limiter.queue.time";
    public static final String AVAILABLE_S3_INFLIGHT_READ_QUOTA_METRIC_NAME = "available.s3.inflight.read.quota";
    public static final String AVAILABLE_S3_INFLIGHT_WRITE_QUOTA_METRIC_NAME = "available.s3.inflight.write.quota";
    public static final String INFLIGHT_WAL_UPLOAD_TASKS_COUNT_METRIC_NAME = "inflight.wal.upload.tasks.count";
    public static final String COMPACTION_READ_SIZE_METRIC_NAME = "compaction.read.size";
    public static final String COMPACTION_WRITE_SIZE_METRIC_NAME = "compaction.write.size";
    public static final String BUFFER_ALLOCATED_MEMORY_SIZE_METRIC_NAME = "buffer.allocated.memory.size";
    public static final String BUFFER_USED_MEMORY_SIZE_METRIC_NAME = "buffer.used.memory.size";
    public static final AttributeKey<String> LABEL_OPERATION_TYPE = AttributeKey.stringKey("operation_type");
    public static final AttributeKey<String> LABEL_OPERATION_NAME = AttributeKey.stringKey("operation_name");
    public static final AttributeKey<String> LABEL_SIZE_NAME = AttributeKey.stringKey("size");
    public static final AttributeKey<String> LABEL_STAGE = AttributeKey.stringKey("stage");
    public static final AttributeKey<String> LABEL_STATUS = AttributeKey.stringKey("status");
    public static final AttributeKey<String> LABEL_ALLOC_TYPE = AttributeKey.stringKey("type");
    public static final String LABEL_STATUS_SUCCESS = "success";
    public static final String LABEL_STATUS_FAILED = "failed";
    public static final String LABEL_STATUS_HIT = "hit";
    public static final String LABEL_STATUS_MISS = "miss";
    public static final String LABEL_STATUS_SYNC = "sync";
    public static final String LABEL_STATUS_ASYNC = "async";

}
