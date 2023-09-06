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

package kafka.log.s3.model;

import java.util.List;

public class StreamMetadata {
    private long streamId;
    private long epoch;
    private int rangeIndex;
    private long startOffset;

    private List<RangeMetadata> ranges;

    public StreamMetadata(long streamId, long epoch, int rangeIndex, long startOffset, List<RangeMetadata> ranges) {
        this.streamId = streamId;
        this.epoch = epoch;
        this.rangeIndex = rangeIndex;
        this.startOffset = startOffset;
        this.ranges = ranges;
    }

    public long getStreamId() {
        return streamId;
    }

    public void setStreamId(long streamId) {
        this.streamId = streamId;
    }

    public long getEpoch() {
        return epoch;
    }

    public void setEpoch(long epoch) {
        this.epoch = epoch;
    }

    public long getStartOffset() {
        return startOffset;
    }

    public void setStartOffset(long startOffset) {
        this.startOffset = startOffset;
    }

    public List<RangeMetadata> getRanges() {
        return ranges;
    }

    public void setRanges(List<RangeMetadata> ranges) {
        this.ranges = ranges;
    }

    public int getRangeIndex() {
        return rangeIndex;
    }

    public void setRangeIndex(int rangeIndex) {
        this.rangeIndex = rangeIndex;
    }
}