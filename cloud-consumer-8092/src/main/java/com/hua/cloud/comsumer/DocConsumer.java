package com.hua.cloud.comsumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hua.cloud.entities.Article;
import com.hua.cloud.entities.TextChunk;
import com.hua.cloud.mapper.FullTextMapper;
import com.hua.cloud.mapper.VectorMapper;
import com.hua.cloud.utils.SnowflakeIdGenerator;
import com.hua.cloud.utils.TextSplitterUtil;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DocConsumer {

    private static final String CNAME = "test";
    private static final ObjectMapper mapper = new ObjectMapper();
    @Resource
    VectorMapper vectorMapper;
    @Resource
    FullTextMapper fullTextMapper;

    @Resource(name = "idGenerator")
    SnowflakeIdGenerator snowflakeIdGenerator;

    @KafkaListener(topics = "my-topic", groupId = "my-group")
    public void listen(String message, Acknowledgment acknowledgment) {
        // 解析原始数据
        Article article = null;
        try {
            System.out.println("消费数据");
            article = mapper.readValue(message, Article.class);

            //幂等性检测

            // 分块处理
            List<String> chunks = TextSplitterUtil.splitContent(article.getContent());

            System.out.println("分块成功！");
            // 为每个块生成向量
            for (int i = 0; i < chunks.size(); i++) {
                TextChunk chunk = new TextChunk();
                chunk.setChunkId(article.getId() + "_" + i);
                vectorMapper.vectorAdd(snowflakeIdGenerator.nextId(), chunks.get(i) ,article.getId(),article.getDate(), CNAME);
            }
            fullTextMapper.fullTextAdd(article.getId(),article.getContent(),article.getDate(),CNAME+"_fullText");
            acknowledgment.acknowledge();
        } catch (JsonProcessingException e) {
            acknowledgment.acknowledge();
            throw new RuntimeException(e);
        }catch (Exception e){
            System.out.println(e.getMessage());
        }
    }
}
