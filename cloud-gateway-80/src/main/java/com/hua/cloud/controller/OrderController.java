package com.hua.cloud.controller;

import com.hua.cloud.api.ProducerApi;
import com.hua.cloud.api.SearchApi;
import com.hua.cloud.resp.ResultData;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class OrderController {

    @Resource
    ProducerApi produceApi;
    @Resource
    SearchApi searchApi;

    @GetMapping("/add")
    public void addFile() {
        try {
/*            List<String> filePaths = getFilePaths("/develop/test");
            for(String path : filePaths){
                produceApi.begin(path);
            }*/
            produceApi.begin("/develop/vector/text.jsonl");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public static List<String> getFilePaths(String folderPath) {
        List<String> paths = new ArrayList<>();
        File dir = new File(folderPath);

        // 验证路径是否为文件夹
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("路径无效或非文件夹: " + folderPath);
            return paths;
        }

        File[] files = dir.listFiles(File::isFile); //过滤仅文件
        if (files == null) return paths;

        int i = 0;
        for (File file : files) {
            if(i == 10) break;
            paths.add(file.getAbsolutePath());
            i++;
        }
        return paths;
    }

    @GetMapping("/create")
    public Map<String, Object> create() {
        Map<String, Object> response = new HashMap<>();
        try {
            System.out.println("重建集合");
            ResultData<Boolean> booleanResultData = searchApi.reCreate();
            System.out.println(booleanResultData);
            response.put("success", true);
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    @GetMapping("/select")
    public String select(String question) {
        ResultData<String> select = searchApi.select(question);
        return select.getData();
    }

}
