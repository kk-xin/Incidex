<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { 
  Service, 
  User, 
  Position, 
  CircleCheck, 
  Headset, 
  Plus, 
  ChatDotSquare, 
  Fold, 
  Expand 
} from '@element-plus/icons-vue'

const router = useRouter()

// 默认欢迎语
const defaultWelcomeMessage = {
  id: 'welcome',
  sender: 'ai',
  senderName: 'IT 智能助手',
  time: 'Just now',
  text: '你好！我是企业 IT 智能小助手。遇到任何系统故障或权限问题，随时发消息告诉我。'
}

// 响应式状态
const historyList = ref([]) // 历史工单列表
const currentIncidentId = ref(null) // 当前选中的工单 ID
const messages = ref([defaultWelcomeMessage])
const inputQuery = ref('')
const loading = ref(false)
const chatContainer = ref(null)
const isSidebarCollapsed = ref(false)

const scrollToBottom = async () => {
  await nextTick()
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

// 1. 从后端数据库加载所有历史会话列表
const loadHistoryList = async () => {
  try {
    const response = await axios.get('/api/incidents')
    if (response.data && Array.isArray(response.data)) {
      // 倒序排列，让最新的会话排在最上面
      historyList.value = response.data.reverse()
    }
  } catch (error) {
    console.error('Failed to load incident history:', error)
  }
}

// 2. 点击侧边栏切换指定的历史工单
const selectIncident = (incident) => {
  currentIncidentId.value = incident.id
  
  // 构建该工单的历史聊天消息队列
  const chatThread = [
    defaultWelcomeMessage,
    {
      id: `emp-${incident.id}`,
      sender: 'employee',
      senderName: '我',
      time: incident.createdAt ? new Date(incident.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '历史记录',
      text: incident.issueDescription
    }
  ]

  if (incident.aiAnalysis) {
    chatThread.push({
      id: `ai-${incident.id}`,
      sender: 'ai',
      senderName: 'IT 智能助手',
      time: incident.createdAt ? new Date(incident.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '历史记录',
      text: incident.aiAnalysis
    })
  }

  // 根据当前工单状态展示系统状态通知
  if (incident.status === 'RESOLVED') {
    chatThread.push({
      id: `sys-${incident.id}`,
      sender: 'system',
      senderName: '系统通知',
      time: '系统',
      text: '✅ 本工单已确认解决并归档。'
    })
  } else if (incident.status === 'ESCALATED') {
    chatThread.push({
      id: `sys-${incident.id}`,
      sender: 'system',
      senderName: '系统通知',
      time: '系统',
      text: '🚨 已为你申请人工介入，工单已推送至 SRE 待办队列。'
    })
  }

  messages.value = chatThread
  scrollToBottom()
}

// 3. 点击【+ 新建对话】按钮
const startNewChat = () => {
  currentIncidentId.value = null
  messages.value = [defaultWelcomeMessage]
  inputQuery.value = ''
  scrollToBottom()
}

// 4. 发送消息
const handleSendMessage = async () => {
  const userText = inputQuery.value.trim()
  if (!userText || loading.value) return

  messages.value.push({
    id: Date.now(),
    sender: 'employee',
    senderName: '我',
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    text: userText
  })

  inputQuery.value = ''
  scrollToBottom()
  loading.value = true

  try {
    if (!currentIncidentId.value) {
      // 创建新工单会话（状态 PENDING_CONFIRM，静默记录）
      const response = await axios.post('/api/incidents', {
        title: '员工提报: ' + userText.slice(0, 15) + '...',
        issueDescription: userText
      })

      if (response.data) {
        currentIncidentId.value = response.data.id
        if (response.data.aiAnalysis) {
          messages.value.push({
            id: Date.now() + 1,
            sender: 'ai',
            senderName: 'IT 智能助手',
            time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
            text: response.data.aiAnalysis
          })
        }
        // 刷新左侧历史列表
        await loadHistoryList()
      }
    } else {
      // 当前已有会话追问
      messages.value.push({
        id: Date.now() + 1,
        sender: 'ai',
        senderName: 'IT 智能助手',
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        text: '正在针对你的补充描述进行进一步分析...'
      })
    }
  } catch (error) {
    console.error('Chat error:', error)
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 5. 标记已解决
const markSolved = async () => {
  if (currentIncidentId.value) {
    try {
      await axios.put(`/api/incidents/${currentIncidentId.value}`, { status: 'RESOLVED' })
      await loadHistoryList()
    } catch (e) {
      console.error(e)
    }
  }

  messages.value.push({
    id: Date.now(),
    sender: 'system',
    senderName: '系统通知',
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    text: '✅ 已确认解决！工单已标记为已解决 (RESOLVED)。'
  })
  scrollToBottom()
}

// 6. 申请人工 SRE 介入
const requestHumanService = async () => {
  if (currentIncidentId.value) {
    try {
      await axios.put(`/api/incidents/${currentIncidentId.value}`, { status: 'ESCALATED' })
      await loadHistoryList()
    } catch (e) {
      console.error(e)
    }
  }

  messages.value.push({
    id: Date.now(),
    sender: 'system',
    senderName: '系统通知',
    time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    text: '🚨 已为你申请人工介入！工单已推送至 SRE 运维待办队列。'
  })
  scrollToBottom()
}

const logout = () => {
  localStorage.removeItem('user_role')
  router.push('/')
}

onMounted(async () => {
  await loadHistoryList()
  // 默认选中最新的未解决会话（如果有的话）
  if (historyList.value.length > 0) {
    const latest = historyList.value[0]
    if (latest.status !== 'RESOLVED') {
      selectIncident(latest)
    }
  }
})
</script>

<template>
  <div class="employee-page">
    <!-- 顶部导航栏 -->
    <header class="top-bar">
      <div class="brand">
        <el-button link class="toggle-btn" @click="isSidebarCollapsed = !isSidebarCollapsed">
          <el-icon><Expand v-if="isSidebarCollapsed" /><Fold v-else /></el-icon>
        </el-button>
        <el-icon class="icon"><Service /></el-icon>
        <span>企业 IT 智能自助服务台</span>
      </div>
      <el-button type="info" link @click="logout">退出登录</el-button>
    </header>

    <div class="main-body">
      <!-- 左侧 Gemini 风格历史记录侧边栏 -->
      <aside class="history-sidebar" :class="{ collapsed: isSidebarCollapsed }">
        <div class="sidebar-header">
          <el-button class="new-chat-btn" type="primary" plain @click="startNewChat">
            <el-icon><Plus /></el-icon>
            <span v-if="!isSidebarCollapsed">新建对话</span>
          </el-button>
        </div>

        <div class="history-scroll-area" v-if="!isSidebarCollapsed">
          <div class="sidebar-label">历史工单记录</div>
          <div 
            v-for="item in historyList" 
            :key="item.id" 
            class="history-item"
            :class="{ active: currentIncidentId === item.id }"
            @click="selectIncident(item)"
          >
            <el-icon class="chat-icon"><ChatDotSquare /></el-icon>
            <span class="history-title" :title="item.title">{{ item.title || item.issueDescription }}</span>
            <span class="status-tag" :class="item.status ? item.status.toLowerCase() : 'pending_confirm'">
              {{ item.status === 'RESOLVED' ? '已解决' : (item.status === 'ESCALATED' ? '人工中' : 'AI中') }}
            </span>
          </div>

          <div v-if="historyList.length === 0" class="empty-history">
            暂无历史工单记录
          </div>
        </div>
      </aside>

      <!-- 右侧对话主区域 -->
      <main class="chat-wrapper">
        <div class="chat-thread" ref="chatContainer">
          <div v-for="msg in messages" :key="msg.id" class="chat-row" :class="msg.sender">
            <div class="avatar" v-if="msg.sender !== 'system'">
              <el-icon v-if="msg.sender === 'employee'"><User /></el-icon>
              <el-icon v-else><Service /></el-icon>
            </div>
            <div v-if="msg.sender === 'system'" class="system-badge">{{ msg.text }}</div>
            <div v-else class="chat-box">
              <div class="msg-info">
                <span class="msg-sender">{{ msg.senderName }}</span>
                <span class="msg-time">{{ msg.time }}</span>
              </div>
              <div class="msg-bubble"><pre>{{ msg.text }}</pre></div>
            </div>
          </div>

          <div v-if="loading" class="chat-row ai">
            <div class="avatar"><el-icon><Service /></el-icon></div>
            <div class="chat-box">
              <div class="msg-bubble loading-bubble">
                <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                <span class="loading-text">AI 正在思考中...</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 快捷反馈工具栏 -->
        <div class="quick-feedback-bar">
          <el-button size="small" type="success" plain @click="markSolved">
            <el-icon><CircleCheck /></el-icon> 方案有用，标记已解决
          </el-button>
          <el-button size="small" type="danger" plain @click="requestHumanService">
            <el-icon><Headset /></el-icon> 没解决，申请人工 SRE 介入
          </el-button>
        </div>

        <!-- 输入框 -->
        <div class="chat-input-box">
          <el-input 
            v-model="inputQuery" 
            type="textarea" 
            :rows="2" 
            placeholder="描述你的系统报错..." 
            @keydown.enter.prevent="handleSendMessage" 
          />
          <div class="input-bottom-row">
            <span class="tip">按 Enter 快捷发送消息</span>
            <el-button type="primary" size="default" :loading="loading" @click="handleSendMessage">
              <el-icon v-if="!loading"><Position /></el-icon>
              {{ loading ? '发送中...' : '发送' }}
            </el-button>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.employee-page { height: 100vh; display: flex; flex-direction: column; background-color: #f3f4f6; }
.top-bar { height: 52px; background: #ffffff; border-bottom: 1px solid #e5e7eb; display: flex; align-items: center; justify-content: space-between; padding: 0 20px; z-index: 10; }
.brand { display: flex; align-items: center; gap: 10px; font-weight: 700; font-size: 16px; color: #111827; }
.brand .icon { color: #2563eb; font-size: 22px; }
.toggle-btn { font-size: 20px; color: #4b5563; margin-right: 4px; }

/* 1. 主区域：增加统一间距并锁定边界 */
.main-body { 
  flex: 1; 
  display: flex; 
  padding: 16px; 
  gap: 16px; 
  overflow: hidden; 
  background-color: #f3f4f6; 
}

/* 2. 侧边栏：12px 圆角悬浮卡片 */
.history-sidebar { 
  width: 260px; 
  background: #ffffff; 
  border-radius: 12px; 
  border: 1px solid #e5e7eb; 
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05); 
  display: flex; 
  flex-direction: column; 
  transition: all 0.2s ease; 
  overflow: hidden; 
}
.history-sidebar.collapsed { width: 64px; }
.sidebar-header { padding: 12px; border-bottom: 1px solid #f3f4f6; }
.new-chat-btn { width: 100%; justify-content: center; }
.history-scroll-area { flex: 1; overflow-y: auto; padding: 12px; }
.sidebar-label { font-size: 11px; font-weight: 700; color: #9ca3af; margin-bottom: 8px; padding-left: 4px; text-transform: uppercase; }
.history-item { display: flex; align-items: center; gap: 8px; padding: 10px; border-radius: 8px; cursor: pointer; color: #374151; font-size: 13px; margin-bottom: 4px; transition: background 0.15s; }
.history-item:hover { background-color: #f3f4f6; }
.history-item.active { background-color: #eff6ff; color: #2563eb; font-weight: 600; }
.chat-icon { font-size: 16px; flex-shrink: 0; }
.history-title { flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status-tag { font-size: 10px; padding: 1px 6px; border-radius: 4px; flex-shrink: 0; }
.status-tag.resolved { background: #dcfce7; color: #15803d; }
.status-tag.escalated { background: #fee2e2; color: #dc2626; }
.status-tag.pending_confirm { background: #e0f2fe; color: #0369a1; }
.empty-history { font-size: 12px; color: #9ca3af; text-align: center; padding: 20px 0; }

/* 3. 右侧对话框：重置外边距（margin: 0）并保持统一圆角与阴影 */
.chat-wrapper { 
  flex: 1; 
  display: flex; 
  flex-direction: column; 
  background: #ffffff; 
  margin: 0; 
  border-radius: 12px; 
  border: 1px solid #e5e7eb; 
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05); 
  overflow: hidden; 
}
.chat-thread { flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 16px; background-color: #fafafa; }
.chat-row { display: flex; gap: 12px; max-width: 85%; }
.chat-row.employee { align-self: flex-end; flex-direction: row-reverse; }
.chat-row.ai { align-self: flex-start; }
.avatar { width: 36px; height: 36px; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; flex-shrink: 0; }
.chat-row.employee .avatar { background-color: #2563eb; }
.chat-row.ai .avatar { background-color: #059669; }
.chat-box { display: flex; flex-direction: column; }
.msg-info { display: flex; gap: 8px; font-size: 11px; color: #6b7280; margin-bottom: 4px; }
.chat-row.employee .msg-info { justify-content: flex-end; }
.msg-bubble { background: #ffffff; border: 1px solid #e5e7eb; padding: 12px 16px; border-radius: 12px; box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05); }
.chat-row.employee .msg-bubble { background-color: #eff6ff; border-color: #bfdbfe; }
.chat-row.ai .msg-bubble { background-color: #ffffff; border-color: #e5e7eb; }
.msg-bubble pre { margin: 0; white-space: pre-wrap; font-family: inherit; font-size: 13px; color: #1f2937; line-height: 1.6; }
.system-badge { width: 100%; text-align: center; background: #fef3c7; border: 1px solid #fde68a; color: #b45309; font-size: 12px; padding: 8px; border-radius: 8px; margin: 8px 0; }
.quick-feedback-bar { padding: 8px 20px; background: #f8fafc; border-top: 1px solid #e5e7eb; display: flex; gap: 10px; }
.chat-input-box { padding: 12px 20px 16px 20px; background: #ffffff; border-top: 1px solid #e5e7eb; }
.input-bottom-row { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
.tip { font-size: 12px; color: #9ca3af; }
.loading-bubble { display: flex; align-items: center; gap: 8px; color: #6b7280; font-size: 13px; }
.dot { width: 6px; height: 6px; background-color: #2563eb; border-radius: 50%; animation: pulse 1.4s infinite ease-in-out both; }
@keyframes pulse { 0%, 80%, 100% { transform: scale(0); } 40% { transform: scale(1.0); } }
</style>