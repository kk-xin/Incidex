package com.incidex.controller;

import com.incidex.model.Incident;
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

    /**
     * POST /api/incidents
     * 接收前端发来的故障工单提交请求
     */
    @PostMapping
    public Incident createIncident(@RequestBody Map<String, String> payload) {
        String title = payload.get("title");
        String issueDescription = payload.get("issueDescription");
        
        return incidentService.createIncident(title, issueDescription);
    }

    /**
     * GET /api/incidents
     * 获取所有故障工单列表
     */
    @GetMapping
    public List<Incident> getAllIncidents() {
        return incidentService.getAllIncidents();
    }
}