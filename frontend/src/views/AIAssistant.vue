<template>
  <div class="page">
    <h1>AI 助手</h1>
    <div class="chat-panel">
      <div class="chat-messages" ref="msgBox">
        <div v-for="(m,i) in messages" :key="i" :class="['msg', m.role==='user'?'msg-user':'msg-ai']">
          <div class="msg-avatar"><el-icon :size="18" v-if="m.role==='ai'"><Cpu /></el-icon><el-icon :size="18" v-else><User /></el-icon></div>
          <div class="msg-bubble">{{ m.content }}</div>
        </div>
        <div v-if="waiting" class="msg msg-ai"><div class="msg-avatar"><el-icon :size="18"><Cpu /></el-icon></div><div class="msg-bubble typing">...</div></div>
      </div>
      <div class="chat-input">
        <el-input v-model="input" placeholder="输入问题..." @keyup.enter="send" :disabled="waiting" size="large">
          <template #append><el-button @click="send" :loading="waiting" :icon="Promotion" style="background:#06b6d4;border-color:#06b6d4;color:#fff">发送</el-button></template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import api from '../api/request'
import { Cpu, User, Promotion } from '@element-plus/icons-vue'

const messages = ref([])
const input = ref('')
const waiting = ref(false)
const msgBox = ref()

const scrollDown = async () => { await nextTick(); if(msgBox.value) msgBox.value.scrollTop = msgBox.value.scrollHeight }

const send = async () => {
  const text = input.value.trim()
  if(!text || waiting.value) return
  messages.value.push({ role:'user', content:text })
  input.value = ''
  waiting.value = true
  await scrollDown()
  try {
    const res = await api.post('/ai-assistant/chat', { message:text })
    messages.value.push({ role:'ai', content:res.data.response || '暂无回复' })
  } catch(e) {
    messages.value.push({ role:'ai', content:'AI服务暂不可用，请稍后重试' })
  }
  waiting.value = false
  await scrollDown()
}

onMounted(()=>{ messages.value.push({ role:'ai', content:'你好！我是实习存证平台AI助手，可以帮你解答实习相关问题、生成证明内容、分析实习记录。' }) })
</script>

<style scoped>
.page { width: 100%; }
.page h1 { color:#e2e8f0; font-size:22px; margin-bottom:20px; }

.chat-panel { background:#1e293b; border:1px solid #334155; border-radius:12px; display:flex; flex-direction:column; height:calc(100vh - 160px); }

.chat-messages { flex:1; overflow-y:auto; padding:20px; display:flex; flex-direction:column; gap:12px; }
.msg { display:flex; gap:10px; max-width:80%; }
.msg-user { align-self:flex-end; flex-direction:row-reverse; }
.msg-avatar { width:32px;height:32px;border-radius:50%;background:#334155;display:flex;align-items:center;justify-content:center;color:#94a3b8;flex-shrink:0; }
.msg-bubble { padding:10px 14px; border-radius:12px; font-size:14px; line-height:1.6; }
.msg-ai .msg-bubble { background:#1e3a5f; color:#e2e8f0; border-bottom-left-radius:4px; }
.msg-user .msg-bubble { background:#0f766e; color:#fff; border-bottom-right-radius:4px; }
.typing { animation: blink 1s infinite; }
@keyframes blink { 0%,100%{opacity:0.3} 50%{opacity:1} }

.chat-input { padding:16px; border-top:1px solid #334155; }
</style>