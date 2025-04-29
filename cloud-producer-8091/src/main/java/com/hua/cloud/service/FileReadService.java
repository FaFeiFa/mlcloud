package com.hua.cloud.service;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.RandomAccessFile;

@Service
@Slf4j
public class FileReadService {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Resource
    RedisOffsetService redisOffsetService;
    public void processLargeFile(String filePath, String topic) {
        long byteOffset = redisOffsetService.readByteOffset(filePath);

        try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
            raf.seek(byteOffset);

            String line;
            while ((line = readNextLine(raf)) != null) {
                kafkaTemplate.send(topic, line);

                long newOffset = raf.getFilePointer();
                redisOffsetService.saveOffset(filePath, newOffset);

                System.out.println("发送成功:" + System.currentTimeMillis());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String readNextLine(RandomAccessFile raf) throws IOException {
        StringBuilder sb = new StringBuilder();
        int ch;
        while ((ch = raf.read()) != -1) {
            if (ch == '\n') {
                break;
            } else if (ch == '\r') {
                // 处理 \r\n 的情况
                long pos = raf.getFilePointer();
                if (raf.read() != '\n') {
                    raf.seek(pos); // 回退
                }
                break;
            }
            sb.append((char) ch);
        }
        return (ch == -1 && sb.length() == 0) ? null : sb.toString();
    }

}
