package com.hua.cloud.config;

import ai.djl.MalformedModelException;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDList;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ModelZoo;
import ai.djl.repository.zoo.ZooModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Author: Hua
 * Date: 2024/6/25 11:10
 */
@Component
public class MilvusPoolConfig {

    //768维度
    private final static String MODEL_PATH = "D:\\Develop\\work\\model\\m3e-base\\tokenizer.json";
    private final static String MODEL_PT_PATH = "D:\\Develop\\Project\\AI platform\\pythonProject\\m3e-base.pt";

    @Bean
    @Scope("singleton")
    public ZooModel<NDList, NDList> model(){
        Criteria<NDList, NDList> criteria = Criteria.builder()
                .setTypes(NDList.class, NDList.class) // 定义输入和输出数据类型
                .optEngine("PyTorch") // 指定引擎为 PyTorch
                .optModelPath(Paths.get(MODEL_PT_PATH)) // 指定模型路径
                .build();
        try {
            return ModelZoo.loadModel(criteria);
        } catch (IOException | ModelNotFoundException | MalformedModelException e) {
            throw new RuntimeException(e);
        }
    }

    @Bean
    @Scope("singleton")
    public Predictor<NDList, NDList> predictor(ZooModel<NDList, NDList> zooModel){
        return zooModel.newPredictor();
    }

    @Bean
    @Scope("singleton")
    public HuggingFaceTokenizer tokenizer(){
        try {
            return HuggingFaceTokenizer.newInstance(Paths.get(MODEL_PATH));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
