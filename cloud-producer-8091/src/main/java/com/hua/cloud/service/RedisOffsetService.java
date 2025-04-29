package com.hua.cloud.service;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisOffsetService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    public void saveOffset(String filePath, long byteOffset) {
        String key = "file:byteOffset:" + filePath;
        redisTemplate.opsForValue().set(key, byteOffset);
    }

    public long readByteOffset(String filePath) {
        String key = "file:byteOffset:" + filePath;
        Object o = redisTemplate.opsForValue().get(key);
        if(o == null){
            return 0L;
        }
        return Long.parseLong(String.valueOf(0));
    }
}
