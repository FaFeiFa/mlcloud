package com.hua.cloud.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

//分块器
/*
优先按段落（\n\n）分割
次优按换行符（\n）分割
再次按空格分割
最后按句子分割
最终手段使用滑动窗口
*/
public class TextSplitterUtil {
    private static final int CHUNK_SIZE = 500;
    private static final int OVERLAP = 50;
    private static final int MAX_FIELD_LENGTH = 1000; // Milvus字段最大长度

    public static List<String> splitContent(String content) {
        List<String> chunks = new ArrayList<>();
        processRecursively(content, chunks);
        return chunks;
    }

    private static void processRecursively(String text, List<String> chunks) {
        // 1. 按段落分割
        List<String> paragraphs = splitByParagraphs(text);
        for (String para : paragraphs) {
            if (para.length() <= CHUNK_SIZE) {
                addWithOverlap(chunks, para);
            } else {
                // 2. 段落过大，按换行符分割
                List<String> lines = splitByLines(para);
                for (String line : lines) {
                    if (line.length() <= CHUNK_SIZE) {
                        addWithOverlap(chunks, line);
                    } else {
                        // 3. 按单词分割
                        List<String> words = splitByWords(line);
                        List<String> wordChunks = mergeWithOverlap(words, CHUNK_SIZE, OVERLAP, true);
                        chunks.addAll(wordChunks);
                    }
                }
            }
        }
    }

    // 分割工具方法
    private static List<String> splitByParagraphs(String text) {
        return Arrays.asList(text.split("\\n\\n+"));
    }

    private static List<String> splitByLines(String text) {
        return Arrays.asList(text.split("\\n"));
    }

    private static List<String> splitByWords(String text) {
        return Arrays.asList(text.split("\\s+"));
    }

    private static List<String> splitBySentences(String text) {
        return Arrays.asList(text.split("(?<=[.!?])\\s+"));
    }

    // 带重叠的智能合并
    private static List<String> mergeWithOverlap(List<String> elements, int chunkSize, int overlap, boolean allowSplit) {
        List<String> merged = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String element : elements) {
            // 计算新增内容后的长度
            int newLength = buffer.length() + (buffer.length() > 0 ? 1 : 0) + element.length();

            if (newLength > chunkSize) {
                if (buffer.length() > 0) {
                    merged.add(buffer.toString());
                    // 保留重叠部分
                    int overlapLength = Math.min(overlap, buffer.length());
                    buffer = new StringBuilder(buffer.substring(buffer.length() - overlapLength));
                }
                // 处理超长单个元素
                if (element.length() > chunkSize) {
                    if (allowSplit) {
                        merged.addAll(splitHugeElement(element, chunkSize, overlap));
                        buffer.setLength(0);
                        continue;
                    }
                }
            }

            if (buffer.length() > 0) {
                buffer.append(" ");
            }
            buffer.append(element);
        }

        if (buffer.length() > 0) {
            merged.add(buffer.toString());
        }

        // 后处理：递归处理仍超长的块
        List<String> finalChunks = new ArrayList<>();
        for (String chunk : merged) {
            if (chunk.length() > chunkSize) {
                if (allowSplit) {
                    // 尝试按句子分割
                    List<String> sentences = splitBySentences(chunk);
                    List<String> sentenceChunks = mergeWithOverlap(sentences, chunkSize, overlap, false);
                    finalChunks.addAll(sentenceChunks);
                } else {
                    // 最终手段：滑动窗口分割
                    finalChunks.addAll(splitBySlidingWindow(chunk, chunkSize, overlap));
                }
            } else {
                finalChunks.add(chunk);
            }
        }
        return finalChunks;
    }

    // 处理超长元素
    private static List<String> splitHugeElement(String element, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < element.length()) {
            int end = Math.min(start + chunkSize, element.length());
            chunks.add(element.substring(start, end));

            // 到达文本末尾时立即终止
            if (end == element.length()) break;

            // 确保至少前进 (chunkSize - overlap) 个字符
            start = Math.max(end - overlap, start + 1);

            // 防死锁保护：当无法前进时强制跳出
            if (start >= end) {
                start = end; // 直接跳到下一个位置
            }
        }
        return chunks;
    }

    // 滑动窗口分割
    private static List<String> splitBySlidingWindow(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int safetyCounter = 0;

        while (start < text.length() && safetyCounter < 1000) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));

            // 动态计算步长（保证至少前进1字符）
            int step = Math.max(chunkSize - overlap, 1);
            start += step;

            // 防无限循环保护
            safetyCounter++;
        }
        return validateChunks(chunks); // 添加最终校验
    }
    // 在最终输出前添加安全校验
    private static List<String> validateChunks(List<String> chunks) {
        return chunks.stream()
                .map(chunk -> {
                    if (chunk.length() > MAX_FIELD_LENGTH) {
                        // 紧急截断策略
                        return chunk.substring(0, MAX_FIELD_LENGTH);
                    }
                    return chunk;
                })
                .filter(chunk -> !chunk.isEmpty())
                .collect(Collectors.toList());
    }

    // 带重叠的块添加逻辑
    private static void addWithOverlap(List<String> chunks, String newChunk) {
        if (chunks.isEmpty()) {
            chunks.add(newChunk);
            return;
        }

        String lastChunk = chunks.get(chunks.size() - 1);
        int availableOverlap = Math.min(OVERLAP, lastChunk.length());
        String merged = lastChunk.substring(lastChunk.length() - availableOverlap) + newChunk;

        if (merged.length() <= CHUNK_SIZE) {
            chunks.set(chunks.size() - 1, merged);
        } else {
            chunks.add(newChunk);
        }
    }
}