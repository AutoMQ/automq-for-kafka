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

package kafka.log.stream.s3;

import com.automq.stream.api.KVClient;
import com.automq.stream.api.KeyValue;
import kafka.log.stream.s3.network.ControllerRequestSender;
import kafka.log.stream.s3.network.ControllerRequestSender.RequestTask;
import kafka.log.stream.s3.network.ControllerRequestSender.ResponseHandleResult;
import kafka.log.stream.s3.network.request.WrapRequest;
import org.apache.kafka.common.message.DeleteKVRequestData;
import org.apache.kafka.common.message.DeleteKVResponseData;
import org.apache.kafka.common.message.GetKVRequestData;
import org.apache.kafka.common.message.GetKVResponseData;
import org.apache.kafka.common.message.PutKVRequestData;
import org.apache.kafka.common.message.PutKVResponseData;
import org.apache.kafka.common.protocol.ApiKeys;
import org.apache.kafka.common.protocol.Errors;
import org.apache.kafka.common.requests.AbstractRequest.Builder;
import org.apache.kafka.common.requests.s3.DeleteKVRequest;
import org.apache.kafka.common.requests.s3.DeleteKVResponse;
import org.apache.kafka.common.requests.s3.GetKVRequest;
import org.apache.kafka.common.requests.s3.GetKVResponse;
import org.apache.kafka.common.requests.s3.PutKVRequest;
import org.apache.kafka.common.requests.s3.PutKVResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class ControllerKVClient implements KVClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(ControllerKVClient.class);
    private final ControllerRequestSender requestSender;

    public ControllerKVClient(ControllerRequestSender requestSender) {
        this.requestSender = requestSender;
    }

    @Override
    public CompletableFuture<Void> putKV(List<KeyValue> list) {
        LOGGER.trace("[ControllerKVClient]: Put KV: {}", list);
        PutKVRequestData request = new PutKVRequestData()
            .setKeyValues(list.stream().map(kv -> new PutKVRequestData.KeyValue()
                .setKey(kv.key())
                .setValue(kv.value().array())
            ).collect(Collectors.toList()));
        WrapRequest req = new WrapRequest() {
            @Override
            public ApiKeys apiKey() {
                return ApiKeys.PUT_KV;
            }

            @Override
            public Builder toRequestBuilder() {
                return new PutKVRequest.Builder(request);
            }
        };
        CompletableFuture<Void> future = new CompletableFuture<>();
        RequestTask<PutKVResponse, Void> task = new RequestTask<PutKVResponse, Void>(req, future, response -> {
            PutKVResponseData resp = response.data();
            Errors code = Errors.forCode(resp.errorCode());
            switch (code) {
                case NONE:
                    LOGGER.trace("[ControllerKVClient]: Put KV: {}, result: {}", list, resp);
                    return ResponseHandleResult.withSuccess(null);
                default:
                    LOGGER.error("[ControllerKVClient]: Failed to Put KV: {}, code: {}, retry later", list, code);
                    return ResponseHandleResult.withRetry();
            }
        });
        this.requestSender.send(task);
        return future;
    }

    @Override
    public CompletableFuture<List<KeyValue>> getKV(List<String> list) {
        LOGGER.trace("[ControllerKVClient]: Get KV: {}", list);
        GetKVRequestData request = new GetKVRequestData()
            .setKeys(list);
        WrapRequest req = new WrapRequest() {
            @Override
            public ApiKeys apiKey() {
                return ApiKeys.GET_KV;
            }

            @Override
            public Builder toRequestBuilder() {
                return new GetKVRequest.Builder(request);
            }
        };
        CompletableFuture<List<KeyValue>> future = new CompletableFuture<>();
        RequestTask<GetKVResponse, List<KeyValue>> task = new RequestTask<>(req, future, response -> {
            GetKVResponseData resp = response.data();
            Errors code = Errors.forCode(resp.errorCode());
            switch (code) {
                case NONE:
                    List<KeyValue> keyValues = resp.keyValues()
                        .stream()
                        .map(kv -> KeyValue.of(kv.key(), kv.value() != null ? ByteBuffer.wrap(kv.value()) : null))
                        .collect(Collectors.toList());
                    LOGGER.trace("[ControllerKVClient]: Get KV: {}, result: {}", list, keyValues);
                    return ResponseHandleResult.withSuccess(keyValues);
                default:
                    LOGGER.error("[ControllerKVClient]: Failed to Get KV: {}, code: {}, retry later", list, code);
                    return ResponseHandleResult.withRetry();
            }
        });
        this.requestSender.send(task);
        return future;
    }

    @Override
    public CompletableFuture<Void> delKV(List<String> list) {
        LOGGER.trace("[ControllerKVClient]: Delete KV: {}", String.join(",", list));
        DeleteKVRequestData request = new DeleteKVRequestData()
            .setKeys(list);
        WrapRequest req = new WrapRequest() {
            @Override
            public ApiKeys apiKey() {
                return ApiKeys.DELETE_KV;
            }

            @Override
            public Builder toRequestBuilder() {
                return new DeleteKVRequest.Builder(request);
            }
        };
        CompletableFuture<Void> future = new CompletableFuture<>();
        RequestTask<DeleteKVResponse, Void> task = new RequestTask<>(req, future, response -> {
            DeleteKVResponseData resp = response.data();
            Errors code = Errors.forCode(resp.errorCode());
            switch (code) {
                case NONE:
                    LOGGER.trace("[ControllerKVClient]: Delete KV: {}, result: {}", list, resp);
                    return ResponseHandleResult.withSuccess(null);
                default:
                    LOGGER.error("[ControllerKVClient]: Failed to Delete KV: {}, code: {}, retry later", String.join(",", list), code);
                    return ResponseHandleResult.withRetry();
            }
        });
        this.requestSender.send(task);
        return future;
    }
}
