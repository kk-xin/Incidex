from fastapi import APIRouter
from pydantic import BaseModel
from core.llm import analyze_incident_with_gemini

router = APIRouter(prefix="/api/v1", tags=["triage"])

class IncidentRequest(BaseModel):
    issue: str

@router.post("/triage")
def triage_incident(req: IncidentRequest):
    # 调用 core 里的 Gemini 大模型进行诊断
    ai_analysis = analyze_incident_with_gemini(req.issue)
    
    return {
        "status": "success",
        "input_issue": req.issue,
        "agent_suggestion": ai_analysis
    }