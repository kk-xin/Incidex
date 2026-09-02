package com.incidex.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.incidex.model.Incident;
import com.incidex.model.TriageRequest;
import com.incidex.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Value("${ai-engine.url}")
    private String aiEngineUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public IncidentService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(60000); // 60 秒超时，完美覆盖 CPU 推理时间
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public Incident createIncident(String title, String issueDescription) {
        TriageRequest requestBody = new TriageRequest(issueDescription);

        String aiSuggestion;
        try {
            // 1. 用 String.class 接收原始 JSON 字符串，避开强类型反序列化的转义异常
            String rawJson = restTemplate.postForObject(aiEngineUrl, requestBody, String.class);
            
            // 2. 用 Jackson JsonNode 动态提炼 agent_suggestion 字段
            JsonNode rootNode = objectMapper.readTree(rawJson);
            if (rootNode.has("agent_suggestion") && !rootNode.get("agent_suggestion").isNull()) {
                aiSuggestion = rootNode.get("agent_suggestion").asText();
            } else {
                aiSuggestion = "AI 诊断成功，但未解析到 agent_suggestion 字段内容。原始响应: " + rawJson;
            }

        } catch (Exception e) {
            e.printStackTrace();
            aiSuggestion = "AI 诊断调用失败, 捕获异常: [" + e.getClass().getName() + "], 报错原因: " + e.getMessage();
        }

        // 3. 构造实体并保存到数据库
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setIssueDescription(issueDescription);
        incident.setAiAnalysis(aiSuggestion);
        incident.setStatus("OPEN");

        return incidentRepository.save(incident);
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }
}