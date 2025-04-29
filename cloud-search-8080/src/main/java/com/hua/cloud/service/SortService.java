package com.hua.cloud.service;

import com.hua.cloud.aop.annotation.CacheDocument;
import com.hua.cloud.entities.Document;
import com.hua.cloud.mapper.FullTextMapper;
import com.hua.cloud.mapper.VectorMapper;
import io.milvus.v2.service.vector.response.SearchResp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SortService {

    @Resource
    private VectorMapper vectorMapper;
    @Resource
    private FullTextMapper fullTextMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final LocalDate NOW = LocalDate.now();

    private static final int MIN_LENGTH = 20;
    @Resource(name = "taskExecutor")
    private Executor QUERY_EXECUTOR;
    private static final int RRF_CONSTANT = 60;

    @Resource
    RRFService rrfService;

    @CacheDocument()
    public List<Document> searchAndReSort(String question, List<String> collectionNames) {
        try {

            List<CompletableFuture<List<Document>>> processedFutures = new ArrayList<>();

            for(String cname : collectionNames){
                CompletableFuture<List<Document>> vectorFuture = CompletableFuture
                        .supplyAsync(() ->
                        {
                            try {
                                return extractRankedDocs(vectorMapper.vectorSelect(question, "cname"));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },QUERY_EXECUTOR)
                        .exceptionally(
                                ex -> {
                                    log.error("向量查询失败，启用保底结果", ex);
                                    return Collections.emptyList(); // 保底: 返回空列表
                                }
                        );
                CompletableFuture<List<Document>> textFuture = CompletableFuture
                        .supplyAsync(() ->
                        {
                            try {
                                return extractRankedDocs(fullTextMapper.fullTextSelect(cname + "_fullText", question));
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        },QUERY_EXECUTOR)
                        .exceptionally(
                                ex -> {
                                    log.error("向量查询失败，启用保底结果", ex);
                                    return Collections.emptyList();
                                }
                        );

                // 合并vectorFuture和textFuture
                CompletableFuture<List<Document>> processedFuture = vectorFuture.thenCombine(textFuture, this::cleanAndMerge);
                processedFutures.add(processedFuture);
            }

            CompletableFuture<Void> allProcessed = CompletableFuture.allOf(processedFutures.toArray(new CompletableFuture[0]));

            double timeWeight = calculateTimeWeight(question);

            return allProcessed.thenApply(v -> {
                List<List<Document>> allRankLists = processedFutures.stream()
                        .map(CompletableFuture::join) // 每个processedFuture的结果
                        .collect(Collectors.toList());
                return rrfService.optimizedRRFWithDate(allRankLists, timeWeight);
            }).join();

        } catch (Exception e) {
            throw new RuntimeException("Re-sorting failed", e);
        }
    }

    private List<Document> extractRankedDocs(List<?> results) {
        if (results == null || results.isEmpty()) return Collections.emptyList();

        return results.stream().map(result -> {
            if (result instanceof SearchResp.SearchResult) {
                SearchResp.SearchResult qr = (SearchResp.SearchResult) result;
                return new Document(
                        String.valueOf(qr.getId()),
                        qr.getScore().longValue(),
                        String.valueOf(qr.getEntity().get("text")),
                        formatDate(qr.getEntity().get("date")),
                        Long.valueOf(String.valueOf(qr.getEntity().get("bigId")))
                );
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private String formatDate(Object dateField) {
        try {
            if (dateField instanceof Number) {
                return LocalDate.ofEpochDay(((Number) dateField).longValue()).format(DATE_FORMATTER);
            }
            return String.valueOf(dateField);
        } catch (Exception e) {
            return NOW.format(DATE_FORMATTER); // 默认返回当天
        }
    }

    private List<Document> cleanAndMerge(List<Document> vectorDocs, List<Document> textDocs) {
        if (vectorDocs == null) vectorDocs = Collections.emptyList();
        if (textDocs == null) textDocs = Collections.emptyList();

        //清洗
        List<Document> cleanedVectorDocs = vectorDocs.stream()
                .filter(doc -> doc.getText() != null && doc.getText().length() >= MIN_LENGTH)
                .collect(Collectors.toList());

        //去重
        Set<String> textIds = textDocs.stream()
                .map(Document::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Document> deduplicatedVectorDocs = cleanedVectorDocs.stream()
                .filter(doc -> doc.getBigId() == null || !textIds.contains(String.valueOf(doc.getBigId())))
                .collect(Collectors.toList());

        List<Document> merged = new ArrayList<>(deduplicatedVectorDocs.size() + textDocs.size());
        merged.addAll(deduplicatedVectorDocs);
        merged.addAll(textDocs);

        return merged;
    }

    private double calculateTimeWeight(String question) {
        // 可加入NLP分析问题时效性需求
        return 0.3; // 默认30%时间权重
    }


}