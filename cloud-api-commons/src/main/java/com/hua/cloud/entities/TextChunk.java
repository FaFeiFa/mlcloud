package com.hua.cloud.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {
    private String chunkId;      // 分块唯一ID（如原始ID+序号）
    private String bigId;         // 分块后的文本
    private String date;         // 原始日期
}
