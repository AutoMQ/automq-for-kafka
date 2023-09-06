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

package org.apache.kafka.metadata.stream;


import java.util.Objects;

public class S3ObjectMetadata {
    private final long objectId;
    private long objectSize;
    private final S3ObjectType type;

    public S3ObjectMetadata(long objectId, long objectSize, S3ObjectType type) {
        this.objectId = objectId;
        this.objectSize = objectSize;
        this.type = type;
    }

    public void setObjectSize(long objectSize) {
        this.objectSize = objectSize;
    }

    public long getObjectId() {
        return objectId;
    }

    public long getObjectSize() {
        return objectSize;
    }

    public S3ObjectType getType() {
        return type;
    }

    public String key() {
        return ObjectUtils.genKey(0, "todocluster", objectId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        S3ObjectMetadata that = (S3ObjectMetadata) o;
        return objectId == that.objectId && objectSize == that.objectSize && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(objectId, objectSize, type);
    }
}
