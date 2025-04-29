package com.hua.cloud.entities;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Article {
    private String id;
    private String content;
    private String title;
    private String language;
    private String date;
    @JsonProperty("num_words")
    private int numWords;
}
