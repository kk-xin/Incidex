package com.incidex.service;

import com.incidex.model.Incident;
import com.incidex.model.TriageRequest;
import com.incidex.model.TriageResponse;
import com.incidex.repository.IncidentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class IncidentService {

    @Autowired
    private IncidentRepository incidentRepository;

    private final RestTemplate restTemplate;

    public IncidentService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 连接超时 10 秒
        factory.setReadTimeout(30000);    // 读取超时 30 秒（解决大模型推理 Read timed out）
        this.restTemplate = new RestTemplate(factory);
    }

    public List<Incident> getAllIncidents() {
        return incidentRepository.findAll();
    }

    /**
     * 1. 员工发送消息：创建新对话，落盘数据库（状态显式设为 PENDING_CONFIRM，非需人工）
     */
    public Incident createIncident(String title, String issueDescription) {
        Incident incident = new Incident();
        incident.setTitle(title);
        incident.setIssueDescription(issueDescription);
        incident.setStatus("PENDING_CONFIRM"); // 核心：静默状态，管理端不爆红灯

        try {
            // 使用你现有的 TriageRequest 模型（映射 issueDescription 到 JSON 的 "issue" 字段）
            TriageRequest request = new TriageRequest(issueDescription);

            // 调用 Python AI 引擎
            TriageResponse response = restTemplate.postForObject(
                "http://localhost:8000/api/v1/triage",
                request,
                TriageResponse.class
            );

            // 使用你现有的 TriageResponse 模型中的 getAgentSuggestion() 映射
            if (response != null && response.getAgentSuggestion() != null) {
                incident.setAiAnalysis(response.getAgentSuggestion());
            }
        } catch (Exception e) {
            System.err.println("AI Engine Triage Warning: " + e.getMessage());
            incident.setAiAnalysis("AI 响应稍有延迟，对话已在后台挂起。如需协助可直接申请人工介入。");
        }

        return incidentRepository.save(incident);
    }

    /**
     * 2. 状态更新：只有当员工点击【标记已解决】或【申请人工】时，才更新状态
     */
    public Incident updateStatus(Long id, String status) {
        Incident incident = incidentRepository.findById(id).orElseThrow();
        if (status != null) {
            incident.setStatus(status);
        }
        return incidentRepository.save(incident);
    }
}