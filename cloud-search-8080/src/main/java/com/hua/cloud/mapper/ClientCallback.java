package com.hua.cloud.mapper;

import io.milvus.v2.client.MilvusClientV2;

@FunctionalInterface
public interface ClientCallback<T> {
    T execute(MilvusClientV2 client) throws Exception;
}
