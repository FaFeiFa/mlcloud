package com.hua.cloud.api;

import com.hua.cloud.resp.ResultData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "service-producer")
public interface ProducerApi {

    @PostMapping("/begin")
    ResultData<Void> begin(@RequestParam("filePath") String filePath);

}
