package com.hua.cloud.controller;

import com.hua.cloud.resp.ResultData;
import com.hua.cloud.service.M3eModelService;
import io.milvus.v2.service.vector.request.data.FloatVec;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class VectorController {

    @Resource
    M3eModelService m3eModelService;

    @PostMapping("/toM3eVector")
    public ResultData<FloatVec> toM3eVector(@RequestParam("sentence") String sentence){
        System.out.println("向量化！");
        FloatVec vector = m3eModelService.toVector(sentence);
        return ResultData.success(vector);
    }

}
