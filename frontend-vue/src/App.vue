<script setup>
import { ref, computed } from 'vue'
import axios from 'axios'
import MarkdownIt from 'markdown-it'
import { Loading, Management, Discount, Position } from '@element-plus/icons-vue'

// 初始化 Markdown 解析器
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
})

// 定义响应式变量
const title = ref('')
const issueDescription = ref('')
const loading = ref(false)
const aiAnalysisRaw = ref('')

// 计算属性：将后端返回的 Markdown 文本解析转换为 HTML 文本
const renderedMarkdown = computed(() => {
  if (!aiAnalysisRaw.value) return ''
  return md.render(aiAnalysisRaw.value)
})

// 提交工单函数
const submitIncident = async () => {
  if (!title.value.trim() || !issueDescription.value.trim()) {
    return
  }

  loading.value = true
  aiAnalysisRaw.value = ''

  try {
    // 请求 Spring Boot 后端创建工单接口
    const response = await axios.post('/api/incidents', {
      title: title.value,
      issueDescription: issueDescription.value
    })

    // 拿到 Java 返回的实体数据（包含 AI 诊断字段 aiAnalysis）
    if (response.data && response.data.aiAnalysis) {
      aiAnalysisRaw.value = response.data.aiAnalysis
    }
  } catch (error) {
    console.error('提交工单异常:', error)
    aiAnalysisRaw.value = '### 提交失败\n无法与后端通讯或诊断服务出错，请检查服务状态。'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="app-container">
    <!-- 页头标题 -->
    <header class="header">
      <el-icon class="logo-icon"><Management /></el-icon>
      <h1>Incidex AI 智能工单诊断平台</h1>
    </header>

    <main class="main-content">
      <!-- 左侧：工单提交表单 -->
      <el-card class="box-card form-card">
        <template #header>
          <div class="card-header">
            <span><el-icon><Discount /></el-icon> 提交新工单</span>
          </div>
        </template>
        
        <el-form label-position="top">
          <el-form-item label="工单标题">
            <el-input 
              v-model="title" 
              placeholder="例如: PostgreSQL 慢查询报警" 
              clearable 
            />
          </el-form-item>

          <el-form-item label="故障描述 (系统报错信息)">
            <el-input 
              v-model="issueDescription" 
              type="textarea" 
              :rows="8" 
              placeholder="例如: CPU 飙升到 100%, 系统接口响应极其缓慢，怀疑缺少索引。" 
            />
          </el-form-item>

          <el-button 
            type="primary" 
            class="submit-btn" 
            :loading="loading" 
            @click="submitIncident"
          >
            <el-icon v-if="!loading"><Position /></el-icon>
            {{ loading ? 'AI 诊断计算中 (约 15-20 秒)...' : '提交工单并触发 AI 诊断' }}
          </el-button>
        </el-form>
      </el-card>

      <!-- 右侧：AI 诊断轨迹与分析报告 -->
      <el-card class="box-card result-card">
        <template #header>
          <div class="card-header">
            <span>AI SRE 智能诊断报告</span>
          </div>
        </template>

        <!-- 加载中动画展示 -->
        <div v-if="loading" class="loading-state">
          <el-icon class="is-loading loading-icon"><Loading /></el-icon>
          <p>Qwen2.5 诊断引擎正在分析系统日志与根因...</p>
        </div>

        <!-- 诊断结果 Markdown 渲染区 -->
        <div 
          v-else-if="aiAnalysisRaw" 
          class="markdown-body" 
          v-html="renderedMarkdown"
        ></div>

        <!-- 空状态提示 -->
        <el-empty 
          v-else 
          description="暂无诊断报告，请在左侧填写信息并提交工单" 
        />
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.app-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #eaedf1;
}

.header h1 {
  font-size: 24px;
  margin: 0;
  color: #1f2937;
}

.logo-icon {
  font-size: 28px;
  color: #409eff;
}

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.card-header {
  font-weight: bold;
  font-size: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.submit-btn {
  width: 100%;
  margin-top: 12px;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: #909399;
}

.loading-icon {
  font-size: 40px;
  margin-bottom: 16px;
  color: #409eff;
}

.markdown-body {
  line-height: 1.6;
  color: #2c3e50;
  max-height: 600px;
  overflow-y: auto;
  padding-right: 8px;
}

.markdown-body :deep(h3) {
  margin-top: 16px;
  color: #1a0dab;
}

.markdown-body :deep(ul), .markdown-body :deep(ol) {
  padding-left: 20px;
}
</style>