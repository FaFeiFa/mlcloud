package com.hua.cloud.service;

import com.hua.cloud.mapper.FullTextMapper;
import com.hua.cloud.mapper.VectorMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class MilvusService {
    @Resource
    VectorMapper vectorMapper;
    @Resource
    FullTextMapper fullTextMapper;

    public void reCreat(){
        try {
            vectorMapper.createVectorCollection("test");
            fullTextMapper.createFullTextCollection("test_fullText");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
