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

package com.automq.stream.s3.metrics.stats;

import com.automq.stream.s3.metrics.Counter;
import com.automq.stream.s3.metrics.Histogram;
import com.automq.stream.s3.metrics.S3StreamMetricsRegistry;
import com.automq.stream.s3.metrics.operations.S3MetricsType;
import com.automq.stream.s3.metrics.operations.S3ObjectStage;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class S3ObjectMetricsStats {
    private static final Map<String, Histogram> S3_OBJECT_TIME_MAP = new ConcurrentHashMap<>();
    public static final Counter S3_OBJECT_COUNT = S3StreamMetricsRegistry.getMetricsGroup().newCounter(S3MetricsType.S3Object.getName(),
            S3MetricsType.S3Object.getName() + "Count", Collections.emptyMap());
    public static final Histogram S3_OBJECT_SIZE = S3StreamMetricsRegistry.getMetricsGroup().newHistogram(S3MetricsType.S3Object.getName(),
            S3MetricsType.S3Object.getName() + "Size", Collections.emptyMap());

    public static Histogram getOrCreateS3ObjectMetrics(S3ObjectStage stage) {
        return S3_OBJECT_TIME_MAP.computeIfAbsent(stage.getName(), op -> {
            Map<String, String> tags = Map.of("stage", stage.getName());
            return S3StreamMetricsRegistry.getMetricsGroup().newHistogram(S3MetricsType.S3Object.getName(),
                    S3MetricsType.S3Object.getName() + "Time", tags);
        });
    }
}