<script setup>
import { useRouter } from 'vue-router'
import { User, Monitor, DataAnalysis } from '@element-plus/icons-vue'

const router = useRouter()

const loginAs = (role) => {
  localStorage.setItem('user_role', role)
  if (role === 'employee') {
    router.push('/employee')
  } else {
    router.push('/sre-console')
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="brand">
        <el-icon class="brand-icon"><DataAnalysis /></el-icon>
        <h2>Incidex Help Desk</h2>
      </div>
      <p class="subtitle">请选择你的身份以进入平台</p>

      <div class="role-grid">
        <div class="role-card" @click="loginAs('employee')">
          <el-icon class="role-icon"><User /></el-icon>
          <h3>普通员工入口</h3>
          <p>提交故障求助、获得 AI 自动答复、跟踪工单状态</p>
          <el-button type="primary" plain class="w-full">以员工身份进入</el-button>
        </div>

        <div class="role-card sre" @click="loginAs('sre')">
          <el-icon class="role-icon"><Monitor /></el-icon>
          <h3>SRE 运维 / 管理员</h3>
          <p>故障深度排查、看 Stacktrace 堆栈、管理 RAG 知识库</p>
          <el-button type="primary" class="w-full">进入 SRE 工作台</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f1f5f9;
}
.login-card {
  background: white;
  padding: 40px;
  border-radius: 12px;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05);
  text-align: center;
  max-width: 640px;
  width: 100%;
}
.brand {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.brand-icon {
  font-size: 32px;
  color: #2563eb;
}
.brand h2 {
  font-size: 24px;
  margin: 0;
  color: #0f172a;
}
.subtitle {
  color: #64748b;
  margin-top: 8px;
  margin-bottom: 32px;
}
.role-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}
.role-card {
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  padding: 24px 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.role-card:hover {
  border-color: #2563eb;
  transform: translateY(-2px);
}
.role-icon {
  font-size: 40px;
  color: #64748b;
  margin-bottom: 12px;
}
.role-card.sre .role-icon {
  color: #2563eb;
}
.role-card h3 {
  font-size: 16px;
  margin: 0 0 8px 0;
  color: #0f172a;
}
.role-card p {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 20px;
  min-height: 36px;
}
.w-full {
  width: 100%;
}
</style>