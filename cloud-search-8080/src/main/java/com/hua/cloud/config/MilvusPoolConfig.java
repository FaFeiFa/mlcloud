package com.hua.cloud.config;

import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Author: Hua
 * Date: 2024/6/25 11:10
 */
@Component
public class MilvusPoolConfig {

    @Bean
    public ConnectConfig connectConfig() {
        return ConnectConfig.builder()
                .uri("http://192.168.234.120:19530")
                .token("root:")
                .dbName("default")
                .build();
    }

    @Bean
    public GenericObjectPoolConfig<MilvusClientV2> poolConfig() {
        GenericObjectPoolConfig<MilvusClientV2> config = new GenericObjectPoolConfig<>();
        config.setMaxTotal(10);
        config.setMaxIdle(5);
        config.setMinIdle(2);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);
        config.setJmxNamePrefix("milvusPool");
        config.setJmxEnabled(false);
        return config;
    }

    @Bean("milvusPool")
    public GenericObjectPool<MilvusClientV2> milvusPool(MilvusFactory milvusFactory, GenericObjectPoolConfig<MilvusClientV2> poolConfig) {
        return new GenericObjectPool<>(milvusFactory, poolConfig);

    }

}
