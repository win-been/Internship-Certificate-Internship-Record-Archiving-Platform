<template>
  <div class="page-full">
    <div class="page-head"><h1>AI 助手</h1></div>

    <div class="ai-layout">
      <!-- 左侧：历史记录卡片 -->
      <div class="ai-left-card">
        <div class="left-head">
          <span class="left-title">历史记录</span>
          <el-button class="btn-new" size="small" @click="startNew"><el-icon><Plus /></el-icon>新建</el-button>
        </div>
        <div class="history-list">
          <div v-for="(item, i) in history" :key="i" class="history-item" :class="{ active: activeHistory === i }" @click="selectHistory(i)">
            <div class="hi-icon" :style="{ background: item.bg, color: item.iconColor }">
              <el-icon :size="14"><component :is="item.icon" /></el-icon>
            </div>
            <div class="hi-info">
              <div class="hi-title">{{ item.title }}</div>
              <div class="hi-meta">{{ item.type }} &middot; {{ item.date }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：对话区卡片 -->
      <div class="ai-right-card">
        <!-- 空状态 -->
        <div v-if="activeHistory === -1" class="empty-state">
          <div class="empty-icon"><el-icon :size="36" color="#93C5FD"><ChatDotRound /></el-icon></div>
          <h3>选择一条记录或开始新对话</h3>
          <p>AI 生成的所有评语、JD、申诉回复都会保存在这里</p>
          <div class="empty-input">
            <el-input v-model="chatInput" placeholder="输入岗位、考核、录用相关问题..." @keyup.enter="sendMessage" />
            <el-button type="primary" :loading="sending" @click="sendMessage"><el-icon><Promotion /></el-icon>发送</el-button>
          </div>
        </div>

        <!-- 内容区 -->
        <template v-else>
          <div class="student-bar" v-if="activeContent?.student">
            <div class="sb-avatar"><el-icon :size="18"><User /></el-icon></div>
            <div class="sb-info">
              <div class="sb-name">{{ activeContent.student }}</div>
              <div class="sb-meta">{{ activeContent.type }} &middot; {{ activeContent.date }}</div>
            </div>
            <div class="sb-actions">
              <el-tooltip content="复制"><el-button circle size="small" @click="copyContent"><el-icon><DocumentCopy /></el-icon></el-button></el-tooltip>
              <el-tooltip content="导出"><el-button circle size="small" @click="exportContent"><el-icon><Download /></el-icon></el-button></el-tooltip>
              <el-tooltip content="存证"><el-button circle size="small" class="btn-proof" @click="archiveContent"><el-icon><CircleCheck /></el-icon></el-button></el-tooltip>
            </div>
          </div>

          <div class="content-flow">
            <div v-for="(msg, i) in activeMessages" :key="i" class="flow-msg" :class="{ 'is-user': msg.role === 'user' }">
              <div class="msg-avatar"><el-icon :size="14"><component :is="msg.role === 'user' ? User : ChatDotRound" /></el-icon></div>
              <div class="msg-bubble">{{ msg.text }}</div>
            </div>
          </div>

          <!-- 底部输入区 -->
          <div class="bottom-input">
            <div class="input-row">
              <el-input v-model="chatInput" placeholder="继续编辑或输入新指令..." size="default" class="chat-input" @keyup.enter="sendMessage" />
              <el-button type="primary" class="btn-send" :loading="sending" @click="sendMessage"><el-icon><Promotion /></el-icon>发送</el-button>
            </div>
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useDataStore } from '../../stores/dataStore'
import { useUserStore } from '../../stores/userStore'
import { ElMessage } from 'element-plus'
import { ChatDotRound, DocumentCopy, Download, CircleCheck, User, Plus, Promotion } from '@element-plus/icons-vue'
import api from '../../api/request'

const activeHistory = ref(-1)
const chatInput = ref('')
const sending = ref(false)
const dataStore = useDataStore()
const userStore = useUserStore()
const storageKey = computed(() => `ai_history_enterprise_${userStore.userInfo?.userId || 'guest'}`)

const toDisplayRecord = (record) => ({
  id: record.id || Date.now() + Math.random(),
  title: record.title || '新对话',
  type: record.type || 'DeepSeek',
  date: record.date || new Date().toISOString().slice(0, 10),
  student: record.student || '',
  content: record.content || '',
  messages: Array.isArray(record.messages) && record.messages.length
    ? record.messages
    : (record.content ? [{ role: 'assistant', text: record.content }] : []),
  icon: ChatDotRound,
  iconColor: record.iconColor || '#2563EB',
  bg: record.bg || '#EFF6FF'
})
const loadHistory = () => {
  try {
    return JSON.parse(localStorage.getItem(storageKey.value) || '[]').map(toDisplayRecord)
  } catch {
    return []
  }
}
const history = ref(loadHistory())

const activeContent = computed(() => activeHistory.value >= 0 ? history.value[activeHistory.value] : null)
const activeMessages = computed(() => activeContent.value?.messages || [])
const transcriptText = (record) => (record?.messages || [])
  .map(msg => (msg.role === 'user' ? '我' : 'AI') + '：' + msg.text)
  .join('\n\n') || record?.content || ''

const persistHistory = () => {
  const plain = history.value.map(({ icon, ...item }) => item)
  localStorage.setItem(storageKey.value, JSON.stringify(plain))
}

watch(history, persistHistory, { deep: true })

const archiveMetaForContent = () => {
  const studentName = activeContent.value?.student
  if (!studentName) return {}
  const hired = (dataStore.hiredStudents || []).find(h => h.name === studentName)
  const schoolStudent = (dataStore.schoolStudents || []).find(s => s.name === studentName)
  return {
    studentId: hired?.studentId || schoolStudent?.userId || null,
    companyId: hired?.companyId || null
  }
}

const selectHistory = (i) => {
  activeHistory.value = i
  chatInput.value = ''
}

const startNew = () => {
  activeHistory.value = -1
  chatInput.value = ''
  ElMessage.info('已新建空白对话，请直接输入问题')
}

const copyContent = () => {
  navigator.clipboard?.writeText(transcriptText(activeContent.value))
  ElMessage.success('已复制到剪贴板')
}
const exportContent = () => {
  if (!activeContent.value) return
  const text = [
    '实习存证平台 AI 生成内容',
    '标题: ' + (activeContent.value.title || ''),
    '类型: ' + (activeContent.value.type || ''),
    '日期: ' + (activeContent.value.date || ''),
    '',
    transcriptText(activeContent.value),
    '',
    '本文档由AI助手生成，可直接使用或修改。'
  ].join('\n')
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url; a.download = (activeContent.value.title || 'export') + '.txt'
  a.click(); URL.revokeObjectURL(url)
  ElMessage.success('导出成功: ' + (activeContent.value.title || 'export') + '.txt')
}
const archiveContent = () => {
  if (!activeContent.value) return
  dataStore.addArchive('AI生成', activeContent.value.title || 'AI内容', transcriptText(activeContent.value), archiveMetaForContent()).then((archive) => {
    const prefix = archive?.chainState === 'on-chain' ? '内容已上链存证' : archive?.chainState === 'synced' ? '内容已同步到后端存证' : '内容已本地存证，待链路恢复后同步'
    ElMessage.success(prefix + '，哈希: ' + (archive?.hash || '').slice(0,14) + '...')
  })
}

const sendMessage = async () => {
  if (!chatInput.value.trim()) return
  const q = chatInput.value.trim()
  const today = new Date().toISOString().slice(0, 10)
  let targetIndex = activeHistory.value
  if (targetIndex < 0) {
    history.value.unshift(toDisplayRecord({
      title: q.slice(0, 18) || '新对话',
      type: 'DeepSeek',
      date: today,
      messages: []
    }))
    targetIndex = 0
    activeHistory.value = 0
  }
  history.value[targetIndex].messages.push({ role: 'user', text: q, time: new Date().toISOString() })
  history.value[targetIndex].date = today
  sending.value = true
  ElMessage.info('AI 正在处理你的指令...')
  try {
    const res = await api.post('/ai-assistant/chat', { message: '企业端问题：' + q })
    const reply = res.data?.response || '暂无回复'
    history.value[targetIndex].messages.push({ role: 'assistant', text: reply, time: new Date().toISOString() })
    history.value[targetIndex].content = reply
    ElMessage.success('DeepSeek 已回复')
  } catch (e) {
    const reply = e?.response?.data?.message || 'DeepSeek 暂不可用，请稍后再试'
    history.value[targetIndex].messages.push({ role: 'assistant', text: reply, time: new Date().toISOString() })
    history.value[targetIndex].content = reply
    ElMessage.error(e?.response?.data?.message || 'DeepSeek 暂不可用')
  } finally {
    sending.value = false
  }
  chatInput.value = ''
}
</script>

<style scoped>
.page-full { width: 100%; display: flex; flex-direction: column; height: calc(100vh - 120px); animation: dashIn 0.45s ease-out; }
@keyframes dashIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-shrink: 0; animation: cardIn 0.4s ease-out both; }
.page-head h1 { font-size: 22px; font-weight: 700; color: #334155; letter-spacing: -0.5px; }

.ai-layout { flex: 1; display: flex; gap: 16px; min-height: 0; }

/* 左侧卡片 */
.ai-left-card {
  width: 300px; background: #FFF; border-radius: 14px;
  box-shadow: 0 2px 14px rgba(0,0,0,0.05); display: flex; flex-direction: column; flex-shrink: 0; overflow: hidden;
  animation: cardIn 0.45s ease-out 0.05s both;
}
@keyframes cardIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
.left-head { display: flex; justify-content: space-between; align-items: center; padding: 16px 18px; border-bottom: 1px solid #F1F5F9; }
.left-title { font-size: 14px; font-weight: 600; color: #334155; }
.btn-new { background: #2563EB !important; border-color: #2563EB !important; color: #FFF !important; border-radius: 8px !important; font-weight: 500; height: 30px !important; }
.btn-new:hover { background: #1D4ED8 !important; }

.history-list { flex: 1; overflow-y: auto; padding: 8px 10px; }
.history-item {
  display: flex; align-items: center; gap: 10px; padding: 10px 12px;
  border-radius: 10px; cursor: pointer; transition: all 0.15s;
}
.history-item:hover { background: #F8FAFC; }
.history-item.active { background: #EFF6FF; }
.history-item.active .hi-title { color: #2563EB; }
.hi-icon { width: 34px; height: 34px; border-radius: 8px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.hi-info { flex: 1; min-width: 0; }
.hi-title { font-size: 13px; font-weight: 500; color: #334155; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.hi-meta { font-size: 11px; color: #94A3B8; margin-top: 2px; }

/* 右侧卡片 */
.ai-right-card {
  flex: 1; background: #FFF; border-radius: 14px;
  box-shadow: 0 2px 14px rgba(0,0,0,0.05); display: flex; flex-direction: column; overflow: hidden; min-width: 0;
  animation: cardIn 0.45s ease-out 0.1s both;
}

/* 空状态 */
.empty-state { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; }
.empty-icon {
  width: 72px; height: 72px; border-radius: 50%; background: #EFF6FF;
  display: flex; align-items: center; justify-content: center;
}
.empty-state h3 { font-size: 15px; color: #334155; font-weight: 600; }
.empty-state p { font-size: 12px; color: #94A3B8; }
.empty-input { width: min(560px, 80%); display: flex; gap: 10px; margin-top: 8px; }
.empty-input .el-input { flex: 1; }

/* 学生信息条 */
.student-bar { display: flex; align-items: center; gap: 10px; padding: 14px 20px; border-bottom: 1px solid #F1F5F9; flex-shrink: 0; }
.sb-avatar { width: 36px; height: 36px; border-radius: 50%; background: #EFF6FF; color: #2563EB; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sb-info { flex: 1; }
.sb-name { font-size: 14px; font-weight: 600; color: #334155; }
.sb-meta { font-size: 11px; color: #94A3B8; margin-top: 1px; }
.sb-actions { display: flex; gap: 6px; }
.btn-proof { background: #ECFDF5 !important; border-color: #A7F3D0 !important; color: #059669 !important; }
.btn-proof:hover { background: #D1FAE5 !important; }

/* 内容流 */
.content-flow { flex: 1; overflow-y: auto; padding: 20px 24px; }
.flow-msg { display: flex; gap: 12px; margin-bottom: 14px; }
.flow-msg.is-user { flex-direction: row-reverse; }
.msg-avatar {
  width: 30px; height: 30px; border-radius: 8px; background: #EFF6FF;
  color: #2563EB; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.msg-bubble {
  background: #F8FAFC; border: 1px solid #F1F5F9; border-radius: 12px;
  padding: 14px 18px; font-size: 13px; line-height: 1.8; color: #334155; white-space: pre-wrap;
  max-width: min(760px, 78%);
}
.flow-msg.is-user .msg-avatar { background: #ECFDF5; color: #059669; }
.flow-msg.is-user .msg-bubble { background: #ECFDF5; border-color: #BBF7D0; }

/* 底部输入 */
.bottom-input { padding: 14px 20px; border-top: 1px solid #F1F5F9; flex-shrink: 0; }
.input-row { display: flex; gap: 10px; align-items: center; }
.chat-input { flex: 1; }
.chat-input :deep(.el-input__wrapper) { border-radius: 8px !important; background: #F8FAFC !important; }
.btn-send { border-radius: 8px !important; padding: 8px 16px !important; font-weight: 500; }
</style>
