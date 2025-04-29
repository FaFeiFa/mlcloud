package com.hua.cloud.service;

import com.hua.cloud.entities.Document;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RRFService {
    private static final int RRF_CONSTANT = 60;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final LocalDate NOW = LocalDate.now();


    // RRF核心方法
    public List<Document> optimizedRRFWithDate(List<List<Document>> allRankLists, double timeWeight) {

        ConcurrentHashMap<String, Double> rrfScores = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, String> docDates = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, Document> docCache = new ConcurrentHashMap<>();

        allRankLists.parallelStream().forEach(rankList -> {
            final int batchSize = 100;
            for (int i = 0; i < rankList.size(); i += batchSize) {
                int start = i;
                int end = Math.min(i + batchSize, rankList.size());
                rankList.subList(start, end).parallelStream().forEach(posDoc -> {
                    int pos = rankList.indexOf(posDoc);
                    rrfScores.merge(posDoc.getId(), 1.0 / (pos + RRF_CONSTANT), Double::sum);
                    docDates.putIfAbsent(posDoc.getId(), posDoc.getDate());
                    docCache.putIfAbsent(posDoc.getId(), posDoc);
                });
            }
        });

        Map<String, Double> timeScores = calculateTimeScores(docDates);

        return rrfScores.entrySet().parallelStream()
                .map(entry -> {
                    String docId = entry.getKey();
                    double finalScore = (1 - timeWeight) * entry.getValue()
                            + timeWeight * timeScores.getOrDefault(docId, 0.0);
                    return new AbstractMap.SimpleEntry<>(docId, finalScore);
                })
                .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                .map(entry -> docCache.get(entry.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private Map<String, Double> calculateTimeScores(Map<String, String> docDates) {
        ConcurrentHashMap<String, Long> daysDiffMap = new ConcurrentHashMap<>();

        docDates.entrySet().parallelStream().forEach(entry -> {
            try {
                LocalDate date = LocalDate.parse(entry.getValue(), DATE_FORMATTER);
                daysDiffMap.put(entry.getKey(), ChronoUnit.DAYS.between(date, NOW));
            } catch (Exception e) {
                daysDiffMap.put(entry.getKey(), Long.MAX_VALUE);
            }
        });

        LongSummaryStatistics stats = daysDiffMap.values().parallelStream()
                .filter(diff -> diff != Long.MAX_VALUE)
                .collect(LongSummaryStatistics::new,
                        LongSummaryStatistics::accept,
                        LongSummaryStatistics::combine);

        final long minDiff = stats.getMin();
        final long maxDiff = stats.getMax() == minDiff ? minDiff + 1 : stats.getMax();

        ConcurrentHashMap<String, Double> scores = new ConcurrentHashMap<>();
        daysDiffMap.forEach((docId, diff) -> {
            if (diff == Long.MAX_VALUE) {
                scores.put(docId, 0.0);
            } else {
                double normalized = 1.0 - (double) (diff - minDiff) / (maxDiff - minDiff);
                scores.put(docId, Math.max(0.0, normalized));
            }
        });
        return scores;
    }


}
