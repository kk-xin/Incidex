<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import MarkdownIt from 'markdown-it'
import { 
  DataAnalysis, 
  Cpu, 
  Document, 
  MagicStick, 
  Loading,
  User,
  Service,
  Promotion,
  Check,
  Warning,
  CircleCheck,
  ChatDotRound,
  Reading
} from '@element-plus/icons-vue'

const md = new MarkdownIt({ html: true, linkify: true, typographer: true })
const router = useRouter()

// 3 个分类：escalated (需人工), pending (AI响应中), resolved (已解决)
const activeTab = ref('escalated') 
const selectedTicketId = ref('')
const ticketList = ref([])
const currentTicket = ref(null)

const adminReplyText = ref('')
const chatMessages = ref([])

let pollTimer = null // 定时器变量

// 核心：从数据库拉取最新工单数据
const fetchIncidentsFromDB = async () => {
  try {
    const response = await axios.get('/api/incidents')
    if (response.data && Array.isArray(response.data)) {
      const updatedList = response.data.map(item => {
        const st = item.status ? item.status.toUpperCase() : 'PENDING_CONFIRM'

        return {
          id: `INC-${item.id}`,
          rawId: item.id,
          title: item.title,
          status: st,
          priority: 'P1',
          service: 'Support Portal',
          createdAt: 'Recently',
          issueDescription: item.issueDescription,
          aiAnalysis: item.aiAnalysis
        }
      })

      ticketList.value = updatedList

      // 如果当前没有选中任何工单且列表中有数据，默认选中第一条
      if (!selectedTicketId.value && ticketList.value.length > 0) {
        const targetList = displayedList.value
        if (targetList.length > 0) {
          selectTicket(targetList[0])
        } else {
          selectTicket(ticketList.value[0])
        }
      }
    }
  } catch (error) {
    console.error('Failed to fetch DB incidents:', error)
  }
}

// 页面挂载：先查一次，随后开启 3 秒轮询
onMounted(() => {
  fetchIncidentsFromDB()
  pollTimer = setInterval(() => {
    fetchIncidentsFromDB()
  }, 3000) // 每 3 秒自动与数据库同步一次，无需人工手动按刷新
})

// 页面销毁：清除定时器，避免内存泄漏
onUnmounted(() => {
  if (pollTimer) {
    clearInterval(pollTimer)
  }
})

// 分流过滤计算：严格按状态分类
const escalatedTickets = computed(() => ticketList.value.filter(item => item.status === 'ESCALATED' || item.status === 'OPEN'))
const pendingTickets = computed(() => ticketList.value.filter(item => item.status === 'PENDING_CONFIRM' || item.status === 'AI_PROCESSING'))
const resolvedTickets = computed(() => ticketList.value.filter(item => item.status === 'RESOLVED'))

const displayedList = computed(() => {
  if (activeTab.value === 'escalated') return escalatedTickets.value
  if (activeTab.value === 'pending') return pendingTickets.value
  return resolvedTickets.value
})

const selectTicket = (ticket) => {
  if (!ticket) return
  selectedTicketId.value = ticket.id
  currentTicket.value = { ...ticket }
  aiAnalysisRaw.value = ticket.aiAnalysis || ''

  chatMessages.value = [
    {
      id: 1,
      sender: 'employee',
      senderName: '普通员工提报',
      time: 'Just now',
      content: ticket.issueDescription
    }
  ]

  if (ticket.aiAnalysis) {
    chatMessages.value.push({
      id: 2,
      sender: 'ai',
      senderName: 'IT 智能助手',
      time: 'Just now',
      content: ticket.aiAnalysis
    })
  }

  if (ticket.status === 'RESOLVED') {
    chatMessages.value.push({
      id: 3,
      sender: 'system',
      senderName: '系统通知',
      time: 'Just now',
      content: '✅ 员工已确认方案有效，本条工单已自动标记为 Resolved。'
    })
  } else if (ticket.status === 'ESCALATED' || ticket.status === 'OPEN') {
    chatMessages.value.push({
      id: 3,
      sender: 'system',
      senderName: '系统通知',
      time: 'Just now',
      content: '🚨 员工点击了【申请人工 SRE 介入】，工单已正式进入你的待办队列。'
    })
  } else {
    chatMessages.value.push({
      id: 3,
      sender: 'system',
      senderName: '系统通知',
      time: 'Just now',
      content: '💬 员工正在与 AI 进行交互问答，尚未确定是否需要人工介入。'
    })
  }
}

const handleSendAdminReply = () => {
  if (!adminReplyText.value.trim()) return

  chatMessages.value.push({
    id: Date.now(),
    sender: 'admin',
    senderName: 'SRE 运维专家',
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    content: adminReplyText.value
  })

  adminReplyText.value = ''
}

const draftReplyFromAI = () => {
  if (aiAnalysisRaw.value) {
    adminReplyText.value = `你好，SRE 团队已为你介入排查：\n` + aiAnalysisRaw.value.slice(0, 150) + `...\n请先尝试上述步骤。`
  }
}

const markAsResolved = async () => {
  if (currentTicket.value) {
    try {
      await axios.put(`/api/incidents/${currentTicket.value.rawId}`, {
        status: 'RESOLVED'
      })
      fetchIncidentsFromDB()
    } catch (e) {
      console.error(e)
    }
  }
}

const loading = ref(false)
const aiAnalysisRaw = ref('')
const renderedMarkdown = computed(() => md.render(aiAnalysisRaw.value || ''))

const ragKnowledgeList = ref([
  {
    id: 1,
    title: 'PostgreSQL Thread Pool Exhaustion Runbook',
    relevance: '92%',
    snippet: 'When active connections reach pool limits, check unindexed queries on large tables using pg_stat_activity.'
  }
])

const triggerAIDiagnosis = async () => {
  if (!currentTicket.value || !currentTicket.value.title.trim()) return

  loading.value = true
  aiAnalysisRaw.value = ''

  try {
    const response = await axios.post('/api/incidents', {
      title: currentTicket.value.title,
      issueDescription: `[Service: ${currentTicket.value.service}] [Priority: ${currentTicket.value.priority}]\n` + currentTicket.value.issueDescription
    })

    if (response.data && response.data.aiAnalysis) {
      aiAnalysisRaw.value = response.data.aiAnalysis
    }
  } catch (error) {
    console.error('Diagnostic error:', error)
  } finally {
    loading.value = false
  }
}

const logout = () => {
  localStorage.removeItem('user_role')
  router.push('/')
}
</script>

<template>
  <div class="app-layout">
    <header class="top-header">
      <div class="logo-area">
        <el-icon class="logo-icon"><DataAnalysis /></el-icon>
        <span class="logo-text">Incidex SRE Console</span>
        <span class="sub-badge">Enterprise Operations Desk</span>
      </div>
      <el-button type="info" link class="logout-link" @click="logout">退出到登录页</el-button>
    </header>

    <main class="three-column-container">
      <aside class="column col-left">
        <div class="col-header border-b">
          <span class="col-title">工单队列分流</span>
        </div>

        <div class="tab-bar">
          <div 
            class="tab-item" 
            :class="{ active: activeTab === 'escalated' }"
            @click="activeTab = 'escalated'"
          >
            <el-icon class="tab-icon danger"><Warning /></el-icon>
            <span>需人工介入</span>
            <span class="count-badge red">{{ escalatedTickets.length }}</span>
          </div>

          <div 
            class="tab-item" 
            :class="{ active: activeTab === 'pending' }"
            @click="activeTab = 'pending'"
          >
            <el-icon class="tab-icon info"><ChatDotRound /></el-icon>
            <span>AI响应中</span>
            <span class="count-badge gray">{{ pendingTickets.length }}</span>
          </div>

          <div 
            class="tab-item" 
            :class="{ active: activeTab === 'resolved' }"
            @click="activeTab = 'resolved'"
          >
            <el-icon class="tab-icon success"><CircleCheck /></el-icon>
            <span>已解决</span>
            <span class="count-badge gray">{{ resolvedTickets.length }}</span>
          </div>
        </div>

        <div class="ticket-list">
          <div 
            v-for="item in displayedList" 
            :key="item.id" 
            class="ticket-card" 
            :class="{ active: selectedTicketId === item.id }"
            @click="selectTicket(item)"
          >
            <div class="card-top">
              <span class="ticket-id">#{{ item.id }}</span>
              <span class="ticket-time">{{ item.createdAt }}</span>
            </div>
            <div class="card-title">{{ item.title }}</div>
            <div class="card-bottom">
              <span class="pill-badge priority" :class="item.priority">{{ item.priority }}</span>
              <span class="pill-badge" :class="{
                'status-escalated': item.status === 'ESCALATED' || item.status === 'OPEN',
                'status-pending': item.status === 'PENDING_CONFIRM' || item.status === 'AI_PROCESSING',
                'status-resolved': item.status === 'RESOLVED'
              }">
                {{ item.status }}
              </span>
            </div>
          </div>
        </div>
      </aside>

      <section class="column col-middle">
        <template v-if="currentTicket && currentTicket.id">
          <div class="col-header border-b flex-between">
            <h2 class="main-incident-title">#{{ currentTicket.id }}: {{ currentTicket.title }}</h2>
            <el-tag :type="currentTicket.status === 'RESOLVED' ? 'success' : (currentTicket.status === 'ESCALATED' ? 'danger' : 'info')" size="small">
              {{ currentTicket.status }}
            </el-tag>
          </div>

          <div class="chat-thread">
            <div 
              v-for="msg in chatMessages" 
              :key="msg.id" 
              class="chat-bubble-row" 
              :class="msg.sender"
            >
              <div class="chat-avatar" v-if="msg.sender !== 'system'">
                <el-icon v-if="msg.sender === 'employee'"><User /></el-icon>
                <el-icon v-else-if="msg.sender === 'ai'"><Service /></el-icon>
                <el-icon v-else><MagicStick /></el-icon>
              </div>

              <div v-if="msg.sender === 'system'" class="system-notify" :class="{ resolved: currentTicket.status === 'RESOLVED', escalated: currentTicket.status === 'ESCALATED' }">
                {{ msg.content }}
              </div>

              <div v-else class="chat-content-box">
                <div class="chat-sender-info">
                  <span class="sender-name">{{ msg.senderName }}</span>
                  <span class="msg-time">{{ msg.time }}</span>
                </div>
                <div class="chat-bubble">
                  <pre>{{ msg.content }}</pre>
                </div>
              </div>
            </div>
          </div>

          <div class="chat-input-area border-t">
            <el-input
              v-model="adminReplyText"
              type="textarea"
              :rows="2"
              placeholder="作为 SRE 运维专家追加回复员工..."
              @keydown.enter.prevent="handleSendAdminReply"
            />
            <div class="input-actions">
              <span class="tip-text">按 Enter 快捷发送</span>
              <el-button type="primary" size="small" @click="handleSendAdminReply">
                <el-icon><Promotion /></el-icon> 发送回复
              </el-button>
            </div>
          </div>
        </template>

        <template v-else>
          <div class="empty-holder">
            <el-empty description="当前队列暂无工单" />
          </div>
        </template>
      </section>

      <aside class="column col-right">
        <div class="col-header border-b">
          <span class="col-title">SRE 智能辅助排查面板</span>
        </div>

        <div class="copilot-scroll-area" v-if="currentTicket">
          <div class="copilot-section">
            <div class="section-title">工单状态处理</div>
            <div class="action-buttons">
              <el-button 
                size="small" 
                type="success" 
                :disabled="currentTicket.status === 'RESOLVED'"
                @click="markAsResolved"
              >
                <el-icon><Check /></el-icon> 确认故障并解决 (Resolve)
              </el-button>
            </div>
          </div>

          <div class="copilot-section">
            <div class="section-title flex-between">
              <span>Deep Agent 排查诊断</span>
              <el-button type="primary" link size="small" @click="draftReplyFromAI" v-if="aiAnalysisRaw">
                一键代写到回复框
              </el-button>
            </div>

            <el-button 
              type="primary" 
              class="generate-btn" 
              size="default" 
              :loading="loading" 
              @click="triggerAIDiagnosis"
            >
              <el-icon v-if="!loading"><Cpu /></el-icon>
              {{ loading ? 'SRE Agent 推理中...' : '生成 SRE 深度诊断报告' }}
            </el-button>

            <div v-if="loading" class="ai-loading">
              <el-icon class="is-loading"><Loading /></el-icon>
              <span>Qwen2.5 诊断推理中...</span>
            </div>
            <div v-else-if="aiAnalysisRaw" class="analysis-box">
              <div class="markdown-body" v-html="renderedMarkdown"></div>
            </div>
          </div>

          <div class="copilot-section">
            <div class="section-title flex-between">
              <span>关联 Runbook 知识库 (RAG)</span>
              <span class="vector-badge">Vector DB</span>
            </div>
            <div v-for="rag in ragKnowledgeList" :key="rag.id" class="rag-card">
              <div class="rag-card-header">
                <span class="rag-title"><el-icon><Reading /></el-icon> {{ rag.title }}</span>
                <span class="relevance-pill">匹配度 {{ rag.relevance }}</span>
              </div>
              <p class="rag-snippet">"{{ rag.snippet }}"</p>
            </div>
          </div>
        </div>

        <div v-else class="empty-holder">
          <span class="tip-text">请在左侧选择工单查看辅助面板</span>
        </div>
      </aside>
    </main>
  </div>
</template>

<style scoped>
.app-layout { height: 100vh; display: flex; flex-direction: column; background-color: #f3f4f6; }
.top-header { height: 52px; background: #ffffff; border-bottom: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; }
.logo-area { display: flex; align-items: center; gap: 10px; }
.logo-icon { font-size: 22px; color: #2563eb; }
.logo-text { font-weight: 700; font-size: 16px; color: #111827; }
.sub-badge { font-size: 12px; color: #6b7280; background: #f3f4f6; padding: 2px 8px; border-radius: 12px; }
.three-column-container { flex: 1; display: flex; padding: 16px; gap: 16px; overflow: hidden; }
.column { background: #ffffff; border-radius: 12px; border: 1px solid #e5e7eb; display: flex; flex-direction: column; }
.col-left { width: 300px; }
.col-middle { flex: 1; }
.col-right { width: 360px; }
.col-header { height: 48px; padding: 0 16px; display: flex; align-items: center; justify-content: space-between; }
.border-b { border-bottom: 1px solid #f3f4f6; }
.border-t { border-top: 1px solid #e5e7eb; }
.col-title { font-weight: 700; font-size: 15px; color: #111827; }
.tab-bar { display: flex; background: #f9fafb; border-bottom: 1px solid #e5e7eb; }
.tab-item { flex: 1; padding: 10px 4px; font-size: 11px; font-weight: 600; color: #6b7280; display: flex; align-items: center; justify-content: center; gap: 2px; cursor: pointer; border-bottom: 2px solid transparent; }
.tab-item.active { color: #2563eb; background: #ffffff; border-bottom-color: #2563eb; }
.count-badge { font-size: 10px; padding: 1px 5px; border-radius: 10px; }
.count-badge.red { background: #fee2e2; color: #dc2626; }
.count-badge.gray { background: #e5e7eb; color: #4b5563; }
.ticket-list { flex: 1; overflow-y: auto; padding: 12px; }
.ticket-card { padding: 12px; border-radius: 10px; background-color: #ffffff; border: 1px solid #f3f4f6; margin-bottom: 10px; cursor: pointer; transition: all 0.2s ease; }
.ticket-card:hover { border-color: #2563eb; transform: translateY(-1px); }
.ticket-card.active { background-color: #eff6ff; border-color: #bfdbfe; }
.card-top { display: flex; justify-content: space-between; font-size: 12px; color: #6b7280; }
.ticket-id { font-weight: 600; color: #111827; }
.card-title { font-size: 13px; font-weight: 600; margin: 6px 0 10px 0; color: #1f2937; }
.card-bottom { display: flex; gap: 6px; }
.pill-badge { font-size: 11px; font-weight: 600; padding: 3px 8px; border-radius: 12px; }
.pill-badge.priority { background-color: #fee2e2; color: #dc2626; }
.status-resolved { background-color: #dcfce7; color: #15803d; }
.status-escalated { background-color: #fee2e2; color: #dc2626; }
.status-pending { background-color: #e0f2fe; color: #0369a1; }
.main-incident-title { font-size: 15px; font-weight: 700; color: #111827; margin: 0; }
.chat-thread { flex: 1; overflow-y: auto; padding: 20px; background-color: #fafafa; display: flex; flex-direction: column; gap: 16px; }
.chat-bubble-row { display: flex; gap: 12px; max-width: 85%; }
.chat-bubble-row.employee { align-self: flex-start; }
.chat-bubble-row.ai { align-self: flex-start; }
.chat-bubble-row.admin { align-self: flex-end; flex-direction: row-reverse; }
.chat-avatar { width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; flex-shrink: 0; }
.chat-bubble-row.employee .chat-avatar { background-color: #2563eb; }
.chat-bubble-row.ai .chat-avatar { background-color: #059669; }
.chat-bubble-row.admin .chat-avatar { background-color: #7c3aed; }
.chat-content-box { display: flex; flex-direction: column; }
.chat-sender-info { display: flex; gap: 8px; font-size: 11px; color: #6b7280; margin-bottom: 4px; }
.chat-bubble-row.admin .chat-sender-info { justify-content: flex-end; }
.chat-bubble { background: #ffffff; border: 1px solid #e5e7eb; padding: 10px 14px; border-radius: 12px; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05); }
.chat-bubble-row.ai .chat-bubble { background-color: #f0fdf4; border-color: #bbf7d0; }
.chat-bubble-row.admin .chat-bubble { background-color: #eff6ff; border-color: #bfdbfe; }
.chat-bubble pre { margin: 0; white-space: pre-wrap; font-family: inherit; font-size: 13px; color: #1f2937; line-height: 1.5; }
.system-notify { width: 100%; text-align: center; background: #fef3c7; border: 1px solid #fde68a; color: #b45309; font-size: 12px; padding: 6px; border-radius: 8px; }
.system-notify.resolved { background: #dcfce7; border-color: #bbf7d0; color: #15803d; }
.system-notify.escalated { background: #fef2f2; border-color: #fecaca; color: #991b1b; }
.chat-input-area { padding: 12px 16px; background: #ffffff; }
.input-actions { display: flex; justify-content: space-between; align-items: center; margin-top: 8px; }
.tip-text { font-size: 11px; color: #9ca3af; }
.empty-holder { height: 100%; display: flex; align-items: center; justify-content: center; }
.copilot-scroll-area { padding: 16px; flex: 1; overflow-y: auto; }
.generate-btn { width: 100%; margin-bottom: 12px; }
.action-buttons { display: flex; gap: 8px; margin-bottom: 8px; }
.section-title { font-size: 13px; font-weight: 700; color: #374151; margin-bottom: 10px; }
.vector-badge { font-size: 11px; background-color: #f3f4f6; color: #6b7280; padding: 2px 8px; border-radius: 12px; }
.analysis-box { background: #ffffff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 14px; font-size: 12px; color: #1f2937; line-height: 1.6; max-height: 240px; overflow-y: auto; }
.rag-card { background: #ffffff; border: 1px solid #e5e7eb; border-radius: 10px; padding: 12px; margin-top: 10px; }
.rag-card-header { display: flex; justify-content: space-between; align-items: center; }
.rag-title { font-size: 12px; font-weight: 600; color: #111827; display: flex; align-items: center; gap: 4px; }
.relevance-pill { font-size: 11px; font-weight: 600; background-color: #dcfce7; color: #15803d; padding: 2px 8px; border-radius: 12px; }
.rag-snippet { font-size: 11px; color: #6b7280; margin: 6px 0 0 0; line-height: 1.4; }
</style>