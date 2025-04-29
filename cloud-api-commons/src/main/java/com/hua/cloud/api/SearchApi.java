package com.hua.cloud.api;

import com.hua.cloud.entities.Document;
import com.hua.cloud.resp.ResultData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "service-search")
public interface SearchApi {

    @PostMapping("/search")
    ResultData<List<Document>> Search(@RequestParam("question") String question);

    @GetMapping("/reCreate")
    ResultData<Boolean> reCreate();

    @PostMapping("/select")
    ResultData<String> select(@RequestParam("question") String question);

}
