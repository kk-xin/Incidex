package com.incidex.service;

import com.incidex.model.Incident;
import com.incidex.model.TriageRequest;
import com.incidex.model.TriageResponse;
import com.incidex.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    @Value("${ai-engine.url}")
    private String aiEngineUrl; // 自动读取 application.yml 里的 http://localhost:8000/api/v1/triage

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 创建故障工单：调用 Python AI 引擎生成诊断，并存入数据库
     */
    public Incident createIncident(String title, String issueDescription) {
        // 1. 构造发给 Python AI 引擎的请求体
        TriageRequest requestBody = new TriageRequest(issueDescription);

        String aiSuggestion = "AI 诊断暂时不可用";
        try {
            // 2. 跨服务 HTTP POST 调用 Python 端 (http://localhost:8000/api/v1/triage)
            TriageResponse response = restTemplate.postForObject(aiEngineUrl, requestBody, TriageResponse.class);
            if (response != null && response.getAgentSuggestion() != null) {
                aiSuggestion = response.getAgentSuggestion();
            }
        } catch (Exception e) {
            // 容错处理：如果 Python AI 引擎没启动或报错，不影响工单基础信息的创建
            System.err.println("调用 Python AI Engine 失败: " + e.getMessage());
            aiSuggestion = "AI 诊断调用失败，原因: " + e.getMessage();
        }

        // 3. 构建 Incident 实体对象
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setIssueDescription(issueDescription);
        incident.setAiAnalysis(aiSuggestion); // 填入 Python 返回的 Gemini 诊断报告
        incident.setStatus("OPEN");

        // 4. 调用 Repository 保存到 PostgreSQL 数据库并返回
        return incidentRepository.save(incident);
    }

    /**
     * 查询所有故障工单列表
     */
    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }
}