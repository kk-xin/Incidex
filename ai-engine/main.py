from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="Incidex AI Engine")

class IncidentRequest(BaseModel):
    issue: str

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "Incidex AI Engine"}

@app.post("/api/v1/triage")
def triage_incident(req: IncidentRequest):
    return {
        "status": "received",
        "input_issue": req.issue,
        "agent_suggestion": f"Agent 已接收故障描述: '{req.issue}'，即将进入 LangGraph 分流节点。"
    }