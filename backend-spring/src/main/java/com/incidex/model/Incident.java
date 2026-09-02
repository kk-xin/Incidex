package com.incidex.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidents")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Incident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 主键自增 ID

    @Column(nullable = false)
    private String title; // 故障标题

    @Column(columnDefinition = "TEXT", nullable = false)
    private String issueDescription; // 详细故障描述（使用 TEXT 类型防止文字过长）

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis; // Python AI 引擎返回的 Gemini 诊断报告

    @Column(nullable = false)
    private String status; // 状态，例如：OPEN, IN_PROGRESS, RESOLVED

    private LocalDateTime createdAt; // 记录创建时间

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now(); // 存入数据库前自动填入当前时间
        if (this.status == null) {
            this.status = "OPEN"; // 默认状态为 OPEN
        }
    }
}