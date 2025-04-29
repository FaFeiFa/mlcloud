package com.hua.cloud.controller;

import com.hua.cloud.entities.Document;
import com.hua.cloud.resp.ResultData;
import com.hua.cloud.service.LLMService;
import com.hua.cloud.service.MilvusService;
import com.hua.cloud.service.SortService;
import io.milvus.v2.service.vector.response.QueryResp;
import io.milvus.v2.service.vector.response.SearchResp;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SearchController {
    @Resource
    SortService sortService;
    @Resource
    MilvusService milvusService;
    @Resource
    LLMService llmService;

    @PostMapping("/search")
    public ResultData<List<Document>> Search(@RequestParam("question") String question) {
        System.out.println("/search");
        ArrayList<String> cs = new ArrayList<>();
        cs.add("test");
        List<Document> documents = sortService.searchAndReSort(question,cs);
        return ResultData.success(documents);
    }

    @GetMapping("/reCreate")
    public ResultData<Boolean> reCreate() {
        System.out.println("/reCreate");
        milvusService.reCreat();
        return ResultData.success(true);

    }

    @PostMapping("/select")
    ResultData<String> select(@RequestParam("question") String question){
        System.out.println("/select");
        ArrayList<String> cs = new ArrayList<>();
        cs.add("test");
        List<Document> documents = sortService.searchAndReSort(question,cs);

        String result = llmService.generateAnswerWithLLM(documents, question);
        return ResultData.success(result);
    }

}
