from fastapi import FastAPI
from api.triage import router as triage_router

app = FastAPI(title="Incidex AI Engine")

# 注册路由：把 api/triage.py 接进来
app.include_router(triage_router)

@app.get("/health")
def health_check():
    return {"status": "ok", "service": "Incidex AI Engine"}