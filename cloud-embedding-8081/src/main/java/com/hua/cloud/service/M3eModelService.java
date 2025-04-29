package com.hua.cloud.service;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.TranslateException;
import io.milvus.v2.service.vector.request.data.FloatVec;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static ai.djl.ndarray.NDManager.newBaseManager;

/**
 * Author: Hua
 * Date: 2024/6/25 11:52
 */
@Component
public class M3eModelService {

    @Resource
    HuggingFaceTokenizer huggingFaceTokenizer;

    @Resource
    Predictor<NDList, NDList> predictor;

    /**
     * 句子转向量
     * @param sentence 你要转为向量的句子
     * @return 转为的向量，格式为float[]
     */
    public FloatVec toVector(String sentence) {
        try (NDList ndList = toNDList(sentence)) {
            try {
                NDList ndArrays = useModel(ndList);
                NDArray ndArrayNow = ndArrays.singletonOrThrow();
                float[] floatArray = ndArrayNow.toFloatArray();

                // 计算 L2 范数
                double sum = 0.0;
                for (float f : floatArray) {
                    sum += f * f;
                }
                float norm = (float) Math.sqrt(sum);

                // 归一化
                List<Float> normalizedVector = new ArrayList<>();
                for (float f : floatArray) {
                    normalizedVector.add(f / norm);
                }

                return new FloatVec(normalizedVector);
            } catch (TranslateException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 分词器 sentence -> ids marks
     * @param sentence 转化的句子
     * @return NDList
     */

    public NDList toNDList(String sentence){
        List<String> sentences = new ArrayList<>();
        sentences.add(sentence);
        Encoding[] encodings = huggingFaceTokenizer.batchEncode(sentences);
        long[][] inputIdsArray = new long[encodings.length][];
        for (int i = 0 ; i < encodings.length ; i++){
            long[] ids = encodings[i].getIds();
            inputIdsArray[i] = ids;
        }
        long[][] attentionMask  = new long[encodings.length][];
        for (int i = 0 ; i < encodings.length ; i++){
            long[] attentionMasks = encodings[i].getAttentionMask();
            attentionMask[i] = attentionMasks;
        }
        NDManager manager = newBaseManager();
        NDArray ndArrayIds = manager.create(inputIdsArray);
        NDArray ndArrayMarks = manager.create(attentionMask);
        return new NDList(ndArrayIds,ndArrayMarks);
    }

    /**
     * 使用模型进行转换
     * @param input 分词器的结果
     * @return 转换后的结果
     * @throws TranslateException 转换异常
     */

    public NDList useModel(NDList input) throws TranslateException {
        // 执行推理
        return predictor.predict(input);
    }

}
