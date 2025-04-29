package com.hua.cloud.mapper;

import com.google.gson.JsonObject;
import com.hua.cloud.api.VectorApi;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.GetReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.response.GetResp;
import io.milvus.v2.service.vector.response.QueryResp;
import jakarta.annotation.Resource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
@Component
public class PublicMapper {

    @Resource(name = "milvusPool")
    private GenericObjectPool<MilvusClientV2> milvusPool;

    @Resource
    private VectorApi vectorApi;

    private <T> T executeWithClient(ClientCallback<T> callback) throws Exception {
        MilvusClientV2 client = null;
        try {
            client = milvusPool.borrowObject();
            return callback.execute(client);
        } finally {
            if (client != null) {
                milvusPool.returnObject(client);
            }
        }
    }
    public boolean exist(String collectionName) throws Exception {
        return executeWithClient(client -> {
            HasCollectionReq req = HasCollectionReq.builder()
                    .collectionName(collectionName)
                    .build();
            return client.hasCollection(req);
        });
    }

    public List<QueryResp.QueryResult> selectFromBig(List<Object> ids, String cname) throws Exception {
        return executeWithClient(client -> {
            GetReq getReq = GetReq.builder()
                    .collectionName(cname)
                    .ids(ids)
                    .outputFields(Collections.singletonList("text"))
                    .build();

            GetResp getResp = client.get(getReq);
            return getResp.getGetResults();
        });
    }

    // 删除数据
    public long deleteByIds(List<Long> ids, String collectionName) throws Exception {
        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("ID列表不能为空");
        }
        return executeWithClient(client -> {
            DeleteReq req = DeleteReq.builder()
                    .collectionName(collectionName)
                    .ids(Arrays.asList(ids.toArray()))
                    .build();
            return client.delete(req).getDeleteCnt();
        });
    }

    // 插入数据
    public long insert(List<JsonObject> data, String collectionName) throws Exception {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("数据不能为空");
        }
        return executeWithClient(client -> {
            InsertReq req = InsertReq.builder()
                    .collectionName(collectionName)
                    .data(data)
                    .build();
            return client.insert(req).getInsertCnt();
        });
    }



}
