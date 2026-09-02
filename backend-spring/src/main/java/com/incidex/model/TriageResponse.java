package com.incidex.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TriageResponse {

    private String status;

    @JsonProperty("input_issue")
    private String inputIssue;

    @JsonProperty("agent_suggestion")
    private String agentSuggestion;

    public TriageResponse() {
    }

    public TriageResponse(String status, String inputIssue, String agentSuggestion) {
        this.status = status;
        this.inputIssue = inputIssue;
        this.agentSuggestion = agentSuggestion;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonProperty("input_issue")
    public String getInputIssue() {
        return inputIssue;
    }

    @JsonProperty("input_issue")
    public void setInputIssue(String inputIssue) {
        this.inputIssue = inputIssue;
    }

    // 【核心修复】：直接在 Getter / Setter 方法上显式指定 agent_suggestion
    @JsonProperty("agent_suggestion")
    public String getAgentSuggestion() {
        return agentSuggestion;
    }

    @JsonProperty("agent_suggestion")
    public void setAgentSuggestion(String agentSuggestion) {
        this.agentSuggestion = agentSuggestion;
    }

    @Override
    public String toString() {
        return "TriageResponse{" +
                "status='" + status + '\'' +
                ", inputIssue='" + inputIssue + '\'' +
                ", agentSuggestion='" + agentSuggestion + '\'' +
                '}';
    }
}