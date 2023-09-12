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

package kafka.log.s3.streams;

import kafka.log.s3.objects.OpenStreamMetadata;
import org.apache.kafka.metadata.stream.StreamMetadata;

import java.util.List;
import java.util.concurrent.CompletableFuture;


public interface StreamManager {

    /**
     * Get current server opening streams.
     * When server is starting or recovering, wal in EBS need streams offset to determine the recover point.
     * @return list of {@link StreamMetadata}
     */
    CompletableFuture<List<StreamMetadata>> getOpeningStreams();
    
    
    /**
     * Create a new stream.
     *
     * @return stream id.
     */
    CompletableFuture<Long> createStream();

    /**
     * Open stream with newer epoch. The controller will:
     * 1. update stream epoch to fence old stream writer to commit object.
     * 2. calculate the last range endOffset.
     * 2. create a new range with serverId = current serverId, startOffset = last range endOffset.
     *
     * @param streamId stream id.
     * @param epoch    stream epoch.
     * @return {@link OpenStreamMetadata}
     */
    CompletableFuture<OpenStreamMetadata> openStream(long streamId, long epoch);

    /**
     * Trim stream to new start offset.
     *
     * @param streamId       stream id.
     * @param epoch          stream epoch.
     * @param newStartOffset new start offset.
     */
    CompletableFuture<Void> trimStream(long streamId, long epoch, long newStartOffset);

    /**
     * Close stream. Other server can open stream with newer epoch.
     *
     * @param streamId stream id.
     * @param epoch    stream epoch.
     */
    CompletableFuture<Void> closeStream(long streamId, long epoch);

    /**
     * Delete stream.
     *
     * @param streamId stream id.
     * @param epoch    stream epoch.
     */
    CompletableFuture<Void> deleteStream(long streamId, long epoch);
}

