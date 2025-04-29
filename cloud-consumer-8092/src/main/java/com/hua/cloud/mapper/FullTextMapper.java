package com.hua.cloud.mapper;

import com.google.gson.JsonObject;
import io.milvus.common.clientenum.FunctionType;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.DropCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.EmbeddedText;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.Resource;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.stereotype.Component;

import java.util.*;
@Component
public class FullTextMapper {

    @Resource(name = "milvusPool")
    private GenericObjectPool<MilvusClientV2> milvusPool;

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

    public boolean createFullTextCollection(String collectionName) throws Exception{
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

            CreateCollectionReq.CollectionSchema schema = CreateCollectionReq.CollectionSchema.builder()
                    .build();
            schema.addField(AddFieldReq.builder()
                    .fieldName("id")
                    .dataType(DataType.VarChar)
                    .maxLength(100)
                    .isPrimaryKey(true)
                    .build());
            schema.addField(AddFieldReq.builder()
                    .fieldName("text")
                    .dataType(DataType.VarChar)
                    .enableAnalyzer(true)
                    .build());
            schema.addField(AddFieldReq.builder()
                    .fieldName("date")
                    .dataType(DataType.VarChar)
                    .build());
            schema.addField(AddFieldReq.builder()
                    .fieldName("sparse")
                    .dataType(DataType.SparseFloatVector)
                    .build());
            schema.addFunction(CreateCollectionReq.Function.builder()
                    .functionType(FunctionType.BM25)
                    .name("text_bm25_emb")
                    .inputFieldNames(Collections.singletonList("text"))
                    .outputFieldNames(Collections.singletonList("sparse"))
                    .build());
            List<IndexParam> indexes = new ArrayList<>();
            indexes.add(IndexParam.builder()
                    .fieldName("sparse")
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .metricType(IndexParam.MetricType.BM25)
                    .build());
            CreateCollectionReq requestCreate = CreateCollectionReq.builder()
                    .collectionName(collectionName)
                    .collectionSchema(schema)
                    .indexParams(indexes)
                    .build();
            client.createCollection(requestCreate);
            return true;
        });
    }

    public List<List<SearchResp.SearchResult>> fullTextSelect(String cname, String sentence) throws Exception {
        return executeWithClient(client -> {
            Map<String, Object> searchParams = new HashMap<>();
            searchParams.put("drop_ratio_search", 0.2);
            SearchResp searchResp = client.search(SearchReq.builder()
                    .collectionName(cname)
                    .data(Collections.singletonList(new EmbeddedText(sentence)))
                    .annsField("sparse")
                    .topK(3)
                    .searchParams(searchParams)
                    .outputFields(Collections.singletonList("text"))
                    .build());
            System.out.println(searchResp.getSearchResults().size());
            System.out.println(searchResp.getSearchResults().get(0).size());
            System.out.println(new EmbeddedText(sentence));
            return searchResp.getSearchResults();
        });
    }

    public void fullTextAdd(String id, String text, String date, String cname) {

        // 构造数据对象
        JsonObject dataObject = new JsonObject();
        dataObject.addProperty("id", id);
        dataObject.addProperty("text", text);
        dataObject.addProperty("date", date);

        // 执行插入
        try {
            publicMapper.insert(Collections.singletonList(dataObject), cname);
        } catch (Exception e) {
            throw new RuntimeException("Insert failed for chunk: " + id, e);
        }
    }




}
