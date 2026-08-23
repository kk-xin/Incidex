from langchain_google_genai import ChatGoogleGenerativeAI
from config import settings

def analyze_incident_with_gemini(issue_description: str) -> str:
    """调用 Gemini 大模型进行故障分析"""
    
    # 直接使用经过 config.py 统一管理和解析的 API Key
    llm = ChatGoogleGenerativeAI(
        model="gemini-3.6-flash", 
        google_api_key=settings.GEMINI_API_KEY,
        temperature=0.2
    )
    
    prompt = f"""
You are a senior Site Reliability Engineer (SRE) and AI incident diagnosis agent.
Analyze the following system fault description submitted by the user:

Fault Description: {issue_description}

Please provide your response ALWAYS in English:
1. Probable Root Causes (key investigation directions).
2. Recommended Emergency Action Steps (bulleted list).

Keep the response professional, concise, and actionable.
"""
    
    response = llm.invoke(prompt)
    return response.content