package com.hua.cloud.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Document {
    private String id;
    private double score;
    private String text;
    private String date;
    private Long bigId;
}
