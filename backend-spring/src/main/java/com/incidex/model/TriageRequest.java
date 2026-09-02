package com.incidex.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TriageRequest {

    @JsonProperty("issue") // 告诉 Jackson 序列化时，把 issueDescription 变成 JSON 的 "issue" 字段
    private String issueDescription;
}