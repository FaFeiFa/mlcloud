package com.hua.cloud.controller;

import com.hua.cloud.resp.ResultData;
import com.hua.cloud.service.FileReadService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
public class ProduceController {
    @Resource
    FileReadService fileReadService;

    private static final String TOPIC = "my-topic";

    @PostMapping("/begin")
    public ResultData<Boolean> begin(@RequestParam("filePath") String filePath){
        fileReadService.processLargeFile(filePath,TOPIC);
        return ResultData.success(true);
    }
}
