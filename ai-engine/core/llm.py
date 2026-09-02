# ai-engine/core/llm.py
from langchain_ollama import ChatOllama

def analyze_incident_with_gemini(issue_description: str) -> str:
    """调用本地 Qwen2.5 大模型进行故障诊断"""
    
    # 使用官方推荐的 ChatOllama 实例
    llm = ChatOllama(
        model="qwen2.5:3b",
        base_url="http://localhost:11434",
        temperature=0.3
    )

    prompt = f"""
You are a senior Site Reliability Engineer (SRE).
Analyze the following fault description: {issue_description}

Please provide your response ALWAYS in English:
1. Probable Root Causes (key investigation directions).
2. Recommended Emergency Action Steps (bulleted list).

Keep the response professional, concise, and actionable.
"""

    try:
        response = llm.invoke(prompt)
        return response.content
    except Exception as e:
        # 【关键修正】：把真实的异常直接打印到终端控制台，并返回给接口，拒绝隐瞒错误！
        print(f"\n================ [OLLAMA ERROR DETAIL] ================")
        print(f"Type: {type(e)}")
        print(f"Error: {e}")
        print(f"=======================================================\n")
        return f"[Ollama Call Failed]: {type(e).__name__} - {str(e)}"