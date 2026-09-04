package com.incidex.controller;

import com.incidex.model.Incident;
import com.incidex.repository.IncidentRepository;
import com.incidex.service.IncidentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    @Autowired
    private IncidentService incidentService;

    @Autowired
    private IncidentRepository incidentRepository;

    @GetMapping
    public List<Incident> getAllIncidents() {
        return incidentService.getAllIncidents();
    }

    /**
     * 1. 员工发送第一条消息：创建新对话工单（默认状态 PENDING_CONFIRM，非转人工）
     */
    @PostMapping
    public Incident createIncident(@RequestBody Map<String, String> payload) {
        String title = payload.get("title");
        String issueDescription = payload.get("issueDescription");
        String status = payload.getOrDefault("status", "PENDING_CONFIRM");
        
        Incident incident = incidentService.createIncident(title, issueDescription);
        incident.setStatus(status);
        return incidentRepository.save(incident);
    }

    /**
     * 2. 员工/管理员变更状态：只有当 status 传入 ESCALATED 时才触发转人工
     */
    @PutMapping("/{id}")
    public Incident updateIncidentStatus(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String status = payload.get("status");
        Incident incident = incidentRepository.findById(id).orElseThrow();
        if (status != null) {
            incident.setStatus(status);
        }
        return incidentRepository.save(incident);
    }
}