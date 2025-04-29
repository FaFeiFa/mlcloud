package com.hua.cloud.service;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.hua.cloud.entities.Document;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp.SearchResult;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class LLMService {

    private static final String API_URL = "https://api.deepseek.com/chat/completions";
    private static final String API_KEY = ""; // 密钥存储于环境变量

    public String generateAnswerWithLLM(List<Document> documents, String question) {
        // 构建提示词
        StringBuilder context = new StringBuilder();
        context.append("---\n问题：").append(question);
        context.append("\n---\n你可以参考以下知识库片段回答问题，给出你每个片段的相关度，使用你觉得有用的片段。");
        context.append("\n---\n注意：回答时不要提及知识库相关信息，不要提到你的答案来自哪个片段。\n---\n");
        for (int i = 0; i < documents.size(); i++) {
            Document result = documents.get(i);
            context.append(String.format(
                    "[片段%d]（相关度：%.2f）\n%s\n\n",
                    i + 1, result.getScore(), result.getText()
            ));
        }
        System.out.println(context);
        try {
            return generateText(context.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // 同步调用方法
    public static String generateText(String userPrompt) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        Gson gson = new Gson();

        // 构建请求体
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", userPrompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("model", "deepseek-chat"); // 指定模型
        requestBody.add("messages", messages);
        requestBody.addProperty("stream", false);

        RequestBody body = RequestBody.create(
                gson.toJson(requestBody),
                MediaType.parse("application/json")
        );

        // 构建请求
        Request request = new Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .post(body)
                .build();

        // 发送请求并解析响应
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("请求失败: HTTP " + response.code()+ response.message() + response.toString());
            }

            JsonObject jsonResponse = gson.fromJson(response.body().string(), JsonObject.class);
            return jsonResponse.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .get("message").getAsJsonObject()
                    .get("content").getAsString();
        }
    }
}
