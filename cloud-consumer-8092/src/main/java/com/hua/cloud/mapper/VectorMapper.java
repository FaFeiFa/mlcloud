package com.hua.cloud.mapper;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hua.cloud.api.VectorApi;
import io.milvus.exception.MilvusException;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.Resource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class VectorMapper {
    @Resource(name = "milvusPool")
    private GenericObjectPool<MilvusClientV2> milvusPool;

    @Resource
    private VectorApi vectorApi;

    @Resource
    private PublicMapper publicMapper;


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
    // 创建集合
    public boolean createVectorCollection(String collectionName)
            throws MilvusException {

        try {
            return executeWithClient(client -> {
                // 检查集合是否存在（避免重复创建）
                // 1. 检查集合存在性
                boolean exists = client.hasCollection(HasCollectionReq.builder()
                        .collectionName(collectionName)
                        .build());

                // 2. 存在则删除（生产环境应添加确认）
                if (exists) {
                    client.dropCollection(DropCollectionReq.builder()
                            .collectionName(collectionName)
                            .build());
                    System.out.println("已删除旧集合: " + collectionName);
                }

                CreateCollectionReq.CollectionSchema schema = client.createSchema();

                schema.addField(AddFieldReq.builder()
                        .fieldName("id")
                        .dataType(DataType.Int64)
                        .isPrimaryKey(true)
                        .autoID(false)
                        .build());
                schema.addField(AddFieldReq.builder()
                        .fieldName("vector")
                        .dataType(DataType.FloatVector)
                        .dimension(768)
                        .build());
                schema.addField(AddFieldReq.builder()
                        .fieldName("bigId")
                        .dataType(DataType.VarChar)
                        .maxLength(100)
                        .build());
                schema.addField(AddFieldReq.builder()
                        .fieldName("date")
                        .dataType(DataType.VarChar)
                        .build());

                // 2. 构建索引参数
                List<IndexParam> indexParams = new ArrayList<>();

                Map<String, Object> hnswParams = new HashMap<>();
                hnswParams.put("M", 16);
                hnswParams.put("efConstruction", 200);

                IndexParam vectorIndex = IndexParam.builder()
                        .fieldName("vector")
                        .indexType(IndexParam.IndexType.HNSW)
                        .metricType(IndexParam.MetricType.IP)
                        .extraParams(hnswParams)
                        .build();
                indexParams.add(vectorIndex);


                CreateCollectionReq customizedSetupReq1 = CreateCollectionReq.builder()
                        .collectionName(collectionName)
                        .collectionSchema(schema)
                        .indexParams(indexParams)
                        .build();

                client.createCollection(customizedSetupReq1);

                return true;
            });
        } catch (MilvusException e) {
            throw new MilvusException("Failed to create collection: " + e.getMessage(), e.getStatus());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 插入数据
    public long VectorInsert(List<JsonObject> data, String collectionName) throws Exception {
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

    public List<List<SearchResp.SearchResult>> vectorSelect(String collectionName, String sentence) throws Exception {
        return executeWithClient(client -> {
            HashMap<String, Object> searchParams = new HashMap<>();
            searchParams.put("ef","50");
            searchParams.put("metric_type","IP");

            SearchReq searchReq = SearchReq.builder()
                    .collectionName(collectionName)
                    .data(Collections.singletonList(vectorApi.toM3eVector(sentence).getData()))
                    .topK(5)
                    .searchParams(searchParams) // 关键参数
                    .outputFields(Collections.singletonList("bigId"))
                    .build();

            SearchResp searchResp = client.search(searchReq);

            return searchResp.getSearchResults();
        });
    }

    public void vectorAdd(Long id, String chunk, String bigId, String date, String cname) {
        // 生成向量（已包含归一化处理）
        //FloatVec vector = vectorApi.toM3eVector(chunk).getData();
        FloatVec vector = null;


        Gson gson = new Gson();
        JsonArray jsonElements = new JsonArray();
        //jsonElements.addAll(gson.toJsonTree(vector.getData()).getAsJsonArray());
        jsonElements.addAll(gson.toJsonTree("").getAsJsonArray());
        // 构造数据对象
        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("id", id);
        dataObject.add("vector", jsonElements);
        dataObject.addProperty("bigId", bigId);
        dataObject.addProperty("date", date);

        // 执行插入
        try {
            publicMapper.insert(Collections.singletonList(dataObject), cname);
        } catch (Exception e) {
            throw new RuntimeException("Insert failed for chunk: " + chunk, e);
        }
    }



}
