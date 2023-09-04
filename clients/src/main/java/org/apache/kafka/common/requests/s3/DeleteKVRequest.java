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

package org.apache.kafka.common.requests.s3;

import java.nio.ByteBuffer;
import org.apache.kafka.common.message.CreateStreamResponseData;
import org.apache.kafka.common.message.DeleteKVRequestData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.ByteBufferAccessor;
import org.apache.kafka.common.requests.AbstractRequest;
import org.apache.kafka.common.requests.ApiError;

public class DeleteKVRequest extends AbstractRequest {
    public static class Builder extends AbstractRequest.Builder<DeleteKVRequest> {

        private final DeleteKVRequestData data;
        public Builder(DeleteKVRequestData data) {
            super(ApiKeys.DELETE_KV);
            this.data = data;
        }

        @Override
        public DeleteKVRequest build(short version) {
            return new DeleteKVRequest(data, version);
        }

        @Override
        public String toString() {
            return data.toString();
        }
    }

    private final DeleteKVRequestData data;

    public DeleteKVRequest(DeleteKVRequestData data, short version) {
        super(ApiKeys.DELETE_KV, version);
        this.data = data;
    }

    @Override
    public CreateStreamResponse getErrorResponse(int throttleTimeMs, Throwable e) {
        ApiError apiError = ApiError.fromThrowable(e);
        CreateStreamResponseData response = new CreateStreamResponseData()
            .setErrorCode(apiError.error().code())
            .setThrottleTimeMs(throttleTimeMs);
        return new CreateStreamResponse(response);
    }

    @Override
    public DeleteKVRequestData data() {
        return data;
    }

    public static DeleteKVRequest parse(ByteBuffer buffer, short version) {
        return new DeleteKVRequest(new DeleteKVRequestData(
            new ByteBufferAccessor(buffer), version), version);
    }
}
