package com.hua.cloud.api;

import com.hua.cloud.resp.ResultData;
import io.milvus.v2.service.vector.request.data.FloatVec;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "service-vector")
public interface VectorApi {

    @PostMapping("/toM3eVector")
    ResultData<FloatVec> toM3eVector(@RequestParam("sentence") String sentence);

}
