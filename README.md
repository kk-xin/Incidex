# Incidex - AI-Driven Incident Triage System

Incidex 是一个基于微服务架构的智能故障工单诊断系统，结合了 Java 后端、Python AI 引擎以及本地开源大模型（Qwen2.5），支持智能分析故障根因并给出 SRE 排查建议。

## 🏗️ 架构概览

* **Frontend**: Vue 3 + Vite
* **Backend**: Java Spring Boot (Port 8080) + PostgreSQL
* **AI Engine**: Python FastAPI (Port 8000) + LangChain
* **Local LLM**: Ollama / Qwen2.5 (Port 11434)

---

## 🚀 本地服务快速启动指南 (Quick Start)

### 1. 基础设施 (Database & LLM)

# 1. 启动 PostgreSQL 数据库 (容器化)
sudo docker compose up -d postgres

# 2. 验证本地 Ollama 后台服务正常运行
curl http://localhost:11434/

### 2. Python AI 引擎 (FastAPI Service - Port 8000)

> ⚠️ 注意：必须先 cd 进入 ai-engine 目录再启动服务，否则会抛出 Could not import module "main" 错误。

# 激活 Conda 虚拟环境
conda activate Incidex

# 进入 AI 引擎目录并启动 uvicorn
cd ~/workspace/Incidex/ai-engine
uvicorn main:app --reload

### 3. Java 主后端 (Spring Boot Service - Port 8080)

# 进入 Java 后端目录并使用系统 mvn 启动
cd ~/workspace/Incidex/backend-spring
mvn spring-boot:run

---

## ⚡ 核心调用流程 (Data Flow)

1. Client 向 Java 后端发送请求 (POST http://localhost:8080/api/incidents)。
2. Java Backend 接收工单描述，调用 Python AI 引擎 (POST http://localhost:8000/api/v1/triage)。
3. Python AI Engine 调用本地 Ollama (http://localhost:11434) 推理生成 SRE 诊断建议。
4. PostgreSQL 保存工单信息与 AI 诊断结果并返回全量数据。