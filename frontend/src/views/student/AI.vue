<template>
  <div class="page-full">
    <div class="page-head"><h1>AI 助手</h1></div>
    <div class="ai-layout">
      <div class="ai-left-card">
        <div class="left-head"><span class="left-title">历史记录</span><el-button class="btn-new" size="small" @click="startNew"><el-icon><Plus /></el-icon>新建</el-button></div>
        <div class="history-list">
          <div v-for="(item, i) in history" :key="i" class="history-item" :class="{ active: activeHistory === i }" @click="selectHistory(i)">
            <div class="hi-icon" :style="{ background: item.bg, color: item.iconColor }"><el-icon :size="14"><component :is="item.icon" /></el-icon></div>
            <div class="hi-info"><div class="hi-title">{{ item.title }}</div><div class="hi-meta">{{ item.type }} &middot; {{ item.date }}</div></div>
          </div>
        </div>
      </div>
      <div class="ai-right-card">
        <div v-if="activeHistory === -1" class="empty-state">
          <div class="empty-icon"><el-icon :size="36" color="#93C5FD"><ChatDotRound /></el-icon></div>
          <h3>选择一条记录或开始新对话</h3>
          <p>AI 可帮你生成简历、面试准备、实习报告等</p>
          <div class="empty-input"><el-input v-model="chatInput" placeholder="输入简历、面试、实习相关问题..." @keyup.enter="sendMessage"/><el-button type="primary" :loading="sending" @click="sendMessage"><el-icon><Promotion /></el-icon>发送</el-button></div>
        </div>
        <template v-else>
          <div class="content-flow">
            <div v-for="(msg, i) in activeMessages" :key="i" class="flow-msg" :class="{ 'is-user': msg.role === 'user' }">
              <div class="msg-avatar"><el-icon><component :is="msg.role === 'user' ? User : ChatDotRound" /></el-icon></div>
              <div class="msg-bubble">{{ msg.text }}</div>
            </div>
          </div>
          <div class="bottom-input"><div class="input-row"><el-input v-model="chatInput" placeholder="继续编辑..." size="default" @keyup.enter="sendMessage"/><el-button type="primary" class="btn-send" :loading="sending" @click="sendMessage"><el-icon><Promotion /></el-icon>发送</el-button></div></div>
        </template>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Document, Briefcase, Plus, Promotion, User } from '@element-plus/icons-vue'
import api from '../../api/request'
import { useUserStore } from '../../stores/userStore'
const userStore = useUserStore()
const activeHistory = ref(-1), chatInput = ref(''), sending = ref(false)
const storageKey = computed(() => `ai_history_student_${userStore.userInfo?.userId || 'guest'}`)
const seedHistory = [
  { title:'简历优化建议', type:'简历', date:'2025-06-05', icon:Document, iconColor:'#2563EB', bg:'#EFF6FF', content:'建议在简历中突出Vue3项目经验，添加GitHub链接。强调团队协作能力和独立解决问题的案例。' },
  { title:'前端实习生面试准备', type:'面试', date:'2025-06-03', icon:Briefcase, iconColor:'#7C3AED', bg:'#F5F3FF', content:'面试准备要点：1. 复习JS基础（闭包、原型链）2. Vue3响应式原理 3. CSS布局（Flex/Grid）4. 准备1-2个项目介绍' },
]
const iconForType = (type) => type === '面试' ? Briefcase : Document
const toDisplayRecord = (record) => ({
  id: record.id || Date.now() + Math.random(),
  title: record.title || '新对话',
  type: record.type || 'DeepSeek',
  date: record.date || new Date().toISOString().slice(0, 10),
  content: record.content || '',
  messages: Array.isArray(record.messages) && record.messages.length
    ? record.messages
    : (record.content ? [{ role: 'assistant', text: record.content }] : []),
  icon: iconForType(record.type),
  iconColor: record.iconColor || '#2563EB',
  bg: record.bg || '#EFF6FF'
})
const loadHistory = () => {
  try {
    const stored = JSON.parse(localStorage.getItem(storageKey.value) || '[]')
    return (stored.length ? stored : seedHistory).map(toDisplayRecord)
  } catch {
    return seedHistory.map(toDisplayRecord)
  }
}
const history = ref(loadHistory())
const activeContent = computed(() => activeHistory.value >= 0 ? history.value[activeHistory.value] : null)
const activeMessages = computed(() => activeContent.value?.messages || [])
const persistHistory = () => {
  const plain = history.value.map(({ icon, ...item }) => item)
  localStorage.setItem(storageKey.value, JSON.stringify(plain))
}
watch(history, persistHistory, { deep: true })
const selectHistory = (i) => { activeHistory.value = i; chatInput.value = '' }
const startNew = () => { activeHistory.value = -1; chatInput.value = ''; ElMessage.info('已新建空白对话，请直接输入问题') }
const sendMessage = async () => {
  if (!chatInput.value.trim()) return
  const q = chatInput.value.trim()
  const today = new Date().toISOString().slice(0, 10)
  let targetIndex = activeHistory.value
  if (targetIndex < 0) {
    history.value.unshift(toDisplayRecord({ title: q.slice(0, 18) || '新对话', type: 'DeepSeek', date: today, messages: [] }))
    targetIndex = 0
    activeHistory.value = 0
  }
  history.value[targetIndex].messages.push({ role: 'user', text: q, time: new Date().toISOString() })
  history.value[targetIndex].date = today
  sending.value = true
  ElMessage.info('AI正在分析你的问题...')
  const responses = {
    '简历': '建议简历中突出项目经验和技术栈，用数据量化成果（如“优化后性能提升30%”）。确保联系方式清晰。',
    '面试': '面试前建议复习基础知识，准备好1-2个项目介绍，练习常见面试题。了解公司业务和技术栈。',
    '实习': '实习期间建议每天写日报，主动沟通需求，及时记录工作成果。这些将作为实习证明的重要依据。',
    '考核': '如果对考核结果有异议，可以通过纠纷申诉流程提出复核。请保留好工作记录作为证据。',
  }
  let reply = ''
  for (const [kw, ans] of Object.entries(responses)) {
    if (q.includes(kw)) { reply = ans; break }
  }
  if (!reply) reply = '谢谢你的提问！建议你前往“实习择业”查看岗位，或在“实习记录”中维护你的工作日志。'
  try {
    const res = await api.post('/ai-assistant/chat', { message: '学生端问题：' + q })
    reply = res.data?.response || reply
  } catch (e) {
    ElMessage.warning('DeepSeek 暂不可用，已使用本地建议')
  }
  history.value[targetIndex].messages.push({ role: 'assistant', text: reply, time: new Date().toISOString() })
  history.value[targetIndex].content = reply
  ElMessage({ message: reply, type: 'success', duration: 5000, showClose: true })
  sending.value = false
  chatInput.value = ''
}
</script>
<style scoped>
.page-full { width:100%; display:flex; flex-direction:column; height:calc(100vh - 120px); animation:dashIn 0.45s ease-out; }
@keyframes dashIn { from{opacity:0;transform:translateY(12px)} to{opacity:1;transform:translateY(0)} }
.page-head { margin-bottom:16px; flex-shrink:0; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.ai-layout { flex:1; display:flex; gap:16px; min-height:0; }
.ai-left-card { width:280px; background:#FFF; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); display:flex; flex-direction:column; flex-shrink:0; overflow:hidden; animation:cardIn 0.45s ease-out 0.05s both; }
@keyframes cardIn { from{opacity:0;transform:translateY(10px)} to{opacity:1;transform:translateY(0)} }
.left-head { display:flex; justify-content:space-between; align-items:center; padding:16px 18px; border-bottom:1px solid #F1F5F9; }
.left-title { font-size:14px; font-weight:600; color:#334155; }
.btn-new { background:#2563EB!important; border-color:#2563EB!important; color:#FFF!important; border-radius:8px!important; height:30px!important; }
.history-list { flex:1; overflow-y:auto; padding:8px 10px; }
.history-item { display:flex; align-items:center; gap:10px; padding:10px 12px; border-radius:10px; cursor:pointer; transition:all 0.15s; }
.history-item:hover { background:#F8FAFC; }
.history-item.active { background:#EFF6FF; }
.history-item.active .hi-title { color:#2563EB; }
.hi-icon { width:34px; height:34px; border-radius:8px; display:flex; align-items:center; justify-content:center; flex-shrink:0; }
.hi-info { flex:1; min-width:0; }
.hi-title { font-size:13px; font-weight:500; color:#334155; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.hi-meta { font-size:11px; color:#94A3B8; margin-top:2px; }
.ai-right-card { flex:1; background:#FFF; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); display:flex; flex-direction:column; overflow:hidden; animation:cardIn 0.45s ease-out 0.1s both; }
.empty-state { flex:1; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:12px; }
.empty-icon { width:72px; height:72px; border-radius:50%; background:#EFF6FF; display:flex; align-items:center; justify-content:center; }
.empty-state h3 { font-size:15px; color:#334155; font-weight:600; }
.empty-state p { font-size:12px; color:#94A3B8; }
.empty-input { width:min(560px, 80%); display:flex; gap:10px; margin-top:8px; }
.empty-input .el-input { flex:1; }
.content-flow { flex:1; overflow-y:auto; padding:20px 24px; }
.flow-msg { display:flex; gap:12px; margin-bottom:14px; }
.flow-msg.is-user { flex-direction:row-reverse; }
.msg-avatar { width:30px; height:30px; border-radius:8px; background:#EFF6FF; color:#2563EB; display:flex; align-items:center; justify-content:center; flex-shrink:0; }
.msg-bubble { background:#F8FAFC; border:1px solid #F1F5F9; border-radius:12px; padding:14px 18px; font-size:13px; line-height:1.8; color:#334155; white-space:pre-wrap; max-width:min(760px, 78%); }
.flow-msg.is-user .msg-avatar { background:#ECFDF5; color:#059669; }
.flow-msg.is-user .msg-bubble { background:#ECFDF5; border-color:#BBF7D0; }
.bottom-input { padding:14px 20px; border-top:1px solid #F1F5F9; flex-shrink:0; }
.input-row { display:flex; gap:10px; }
.btn-send { border-radius:8px!important; padding:8px 16px!important; }
</style>
