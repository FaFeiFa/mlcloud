package com.hua.cloud.service;

import com.hua.cloud.entities.Document;
import com.hua.cloud.mapper.FullTextMapper;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DocumentCacheService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String DOCUMENT_CACHE_LRU_KEY = "document_cache_lru";
    private static final int MAX_CACHE_SIZE = 1000;

    @Resource
    private FullTextMapper fullTextMapper;

    public DocumentCacheService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void cacheDocumentsAndIds(String question, List<Document> documents) {
        ValueOperations<String, Object> ops = redisTemplate.opsForValue();
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();

        for (Document doc : documents) {
            String documentKey = buildDocumentKey(doc.getId());

            //默认30分钟
            ops.set(documentKey, doc,30,TimeUnit.MINUTES);

            // 更新LRU列表
            zSetOps.add(DOCUMENT_CACHE_LRU_KEY, doc.getId(), Instant.now().toEpochMilli());
        }

        //检查并清理多余的Document
        Long currentSize = zSetOps.size(DOCUMENT_CACHE_LRU_KEY);
        if (currentSize != null && currentSize > MAX_CACHE_SIZE) {

            long toRemove = currentSize - MAX_CACHE_SIZE;

            // 获取最老的ID
            Set<Object> oldIds = zSetOps.range(DOCUMENT_CACHE_LRU_KEY, 0, toRemove - 1);

            if (oldIds != null && !oldIds.isEmpty()) {
                // 删除Redis中的Document
                List<String> oldDocumentKeys = oldIds.stream()
                        .map(id -> buildDocumentKey(id.toString()))
                        .collect(Collectors.toList());
                redisTemplate.delete(oldDocumentKeys);

                // 从LRU列表中移除
                zSetOps.remove(DOCUMENT_CACHE_LRU_KEY, oldIds.toArray());
            }
        }

        // 保存ID列表
        String idListKey = buildIdListKey(question);
        List<String> idList = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        redisTemplate.delete(idListKey);
        redisTemplate.opsForList().rightPushAll(idListKey, idList);
        redisTemplate.expire(idListKey, 30, TimeUnit.MINUTES);
    }

    public List<String> getIdList(String question) {
        String idListKey = buildIdListKey(question);
        List<Object> rawList = redisTemplate.opsForList().range(idListKey, 0, -1);
        if (rawList == null || rawList.isEmpty()) {
            return null;
        }
        return rawList.stream()
                .map(Object::toString)
                .collect(Collectors.toList());
    }

    public List<Document> getDocumentsByIds(List<String> ids) {
        List<String> keys = ids.stream()
                .map(this::buildDocumentKey)
                .collect(Collectors.toList());

        List<Object> rawDocs = redisTemplate.opsForValue().multiGet(keys);
        List<Document> documents = rawDocs.stream()
                .filter(obj -> obj instanceof Document)
                .map(obj -> (Document) obj)
                .collect(Collectors.toList());
        HashSet<String> keySet = new HashSet<>(keys);
        List<String> needIds = new ArrayList<>();
        for(Document document : documents){
            keySet.remove(document.getId());
        }
        needIds = new ArrayList<>(keySet);
        List<Document> documentsByIds = getDocumentsByIds(needIds);

        Map<String, Document> documentMap = new HashMap<>();

        for (Document doc : documents) {
            documentMap.put(doc.getId(), doc);
        }
        for (Document doc : documentsByIds) {
            documentMap.put(doc.getId(), doc);
        }
        List<Document> mergedList = new ArrayList<>();
        for (String id : ids) {
            Document doc = documentMap.get(id);
            if (doc != null) {
                mergedList.add(doc);
            }
        }
        return mergedList;
    }

    public List<Document> getDocumentsByIds(List<String> ids, Function<List<String>, List<Document>> loader) {
        List<String> keys = ids.stream()
                .map(this::buildDocumentKey)
                .collect(Collectors.toList());

        List<Object> cachedDocs = redisTemplate.opsForValue().multiGet(keys);

        List<Document> result = new ArrayList<>();
        List<String> missingIds = new ArrayList<>();

        for (int i = 0; i < ids.size(); i++) {
            Object cached = cachedDocs != null ? cachedDocs.get(i) : null;
            if (cached instanceof Document) {
                // 缓存命中，刷新LRU时间
                String id = ids.get(i);
                redisTemplate.opsForZSet().add(DOCUMENT_CACHE_LRU_KEY, id, Instant.now().toEpochMilli());
                result.add((Document) cached);
            } else {
                // 缓存缺失
                missingIds.add(ids.get(i));
            }
        }

        if (!missingIds.isEmpty()) {
            List<Document> loadedDocs = null;
                    //fullTextMapper.fullTextSelect(missingIds);
            if (loadedDocs != null && !loadedDocs.isEmpty()) {
                for (Document doc : loadedDocs) {
                    String documentKey = buildDocumentKey(doc.getId());
                    redisTemplate.opsForValue().set(documentKey, doc, 30, TimeUnit.MINUTES);

                    // 更新LRU
                    redisTemplate.opsForZSet().add(DOCUMENT_CACHE_LRU_KEY, doc.getId(), Instant.now().toEpochMilli());

                    result.add(doc);
                }
            }
        }

        return result;
    }


    private String buildDocumentKey(String id) {
        return "document:" + id;
    }

    private String buildIdListKey(String question) {
        return "question:" + question;
    }

}

