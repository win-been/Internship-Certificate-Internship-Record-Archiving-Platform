<template>
  <div class="page">
    <div class="page-head"><h1>AI 助手</h1></div>
    <div class="ai-card">
      <el-tabs v-model="tab">
        <el-tab-pane label="数据报表" name="report">
          <div class="ai-section">
            <el-select v-model="reportType"><el-option label="全平台月报" value="月报" /><el-option label="全平台年报" value="年报" /></el-select>
            <el-button type="primary" style="margin-top:12px" @click="genReport">自动生成</el-button>
            <div class="ai-result" v-if="reportResult">{{ reportResult }}</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="风控识别" name="risk">
          <div class="ai-section">
            <el-table :data="risks" size="small">
              <el-table-column prop="student" label="学生" />
              <el-table-column prop="issue" label="风险描述" />
              <el-table-column prop="level" label="等级"><template #default="{ row }"><span class="risk-tag" :class="row.level === '高' ? 'tag-high' : 'tag-mid'">{{ row.level }}</span></template></el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
        <el-tab-pane label="运维答疑" name="ops">
          <div class="ai-section">
            <el-input v-model="opsQ" placeholder="输入运维问题..." />
            <el-button type="primary" style="margin-top:12px" @click="answerOps">查询</el-button>
            <div class="ai-result" v-if="opsResult">{{ opsResult }}</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="制度草拟" name="doc">
          <div class="ai-section">
            <el-select v-model="docType"><el-option label="实习管理规范" value="管理" /><el-option label="存证规范文档" value="存证" /></el-select>
            <el-button type="primary" style="margin-top:12px" @click="genDoc">生成文档</el-button>
            <div class="ai-result" v-if="docResult">{{ docResult }}</div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
    <div class="history-card">
      <div class="history-head">历史记录</div>
      <div v-if="history.length === 0" class="history-empty">暂无历史记录</div>
      <div v-for="item in history" :key="item.id" class="history-item" @click="restoreHistory(item)">
        <div class="history-title">{{ item.title }}</div>
        <div class="history-meta">{{ item.type }} · {{ item.date }}</div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, watch } from 'vue'
import { useDataStore } from '../../stores/dataStore'
import { useUserStore } from '../../stores/userStore'
import { ElMessage } from 'element-plus'
import api from '../../api/request'
const dataStore = useDataStore()
const userStore = useUserStore()
const tab = ref('report'), reportType = ref('月报'), reportResult = ref(''), opsQ = ref(''), opsResult = ref(''), docType = ref('管理'), docResult = ref('')
const storageKey = computed(() => `ai_history_platform_${userStore.userInfo?.userId || 'guest'}`)
const loadHistory = () => {
  try { return JSON.parse(localStorage.getItem(storageKey.value) || '[]') } catch { return [] }
}
const history = ref(loadHistory())
const saveHistory = (type, title, prompt, result) => {
  history.value.unshift({ id: Date.now(), type, title, prompt, result, date: new Date().toISOString().slice(0, 10) })
  history.value = history.value.slice(0, 30)
}
const restoreHistory = (item) => {
  if (item.type === '数据报表') { tab.value = 'report'; reportResult.value = item.result }
  else if (item.type === '运维答疑') { tab.value = 'ops'; opsResult.value = item.result; opsQ.value = item.prompt || '' }
  else { tab.value = 'doc'; docResult.value = item.result }
}
watch(history, () => localStorage.setItem(storageKey.value, JSON.stringify(history.value)), { deep: true })
const risks = computed(() => {
  const interns = (dataStore.internships || []).filter(i => i.status === 'ACTIVE')
  const items = []
  // Check for internships without recent activity
  const now = new Date()
  interns.forEach(intern => {
    const startDate = intern.startDate ? new Date(intern.startDate) : null
    if (startDate && (now - startDate) > 15 * 24 * 3600 * 1000) {
      const hasRecords = (dataStore.archives || []).some(a => a.studentId && a.time && new Date(a.time) > new Date(now - 15 * 24 * 3600 * 1000))
      if (!hasRecords) {
        items.push({ student: intern.studentName || '未知', issue: '连续15天无活动记录', level: '高' })
      }
    }
  })
  if (items.length === 0) {
    items.push({ student: '暂无', issue: '当前无风险预警', level: '低' })
  }
  return items
})
const askDeepSeek = async (prompt, fallback) => {
  try {
    const res = await api.post('/ai-assistant/chat', { message: prompt })
    return res.data?.response || fallback
  } catch (e) {
    ElMessage.warning('DeepSeek 暂不可用，已使用本地结果')
    return fallback
  }
}
const genReport = async () => {
  const now = new Date()
  const ym = String(now.getFullYear()) + '年' + String(now.getMonth()+1) + '月'
  const stats = '入驻企业:' + (dataStore.enterpriseList || []).length +
    '，已审批:' + (dataStore.enterpriseList || []).filter(e => e.status === 'APPROVED').length +
    '，发布岗位:' + (dataStore.jobs || []).length +
    '，在岗实习生:' + (dataStore.hiredStudents || []).filter(h => h.signed).length +
    '，存证总数:' + (dataStore.archives || []).length +
    '，纠纷待处理:' + (dataStore.disputes || []).filter(d => d.status === 'PENDING').length
  const prompt = '请根据以下数据生成一份全平台' + ym + reportType.value + '，要求分点、简洁、适合管理员查看：' + stats
  reportResult.value = await askDeepSeek(prompt, '【全平台' + ym + '报告】\n' + stats)
  saveHistory('数据报表', ym + reportType.value, prompt, reportResult.value)
  ElMessage.success('报告已生成')
}
const answerOps = async () => {
  if (!opsQ.value.trim()) {
    ElMessage.warning('请输入运维问题')
    return
  }
  const prompt = opsQ.value.trim()
  opsResult.value = await askDeepSeek('你是实习存证平台运维助手，请回答这个问题：' + prompt, '【运维建议】\n请先检查后端服务、Vite代理、登录token与WeBASE链路配置。')
  saveHistory('运维答疑', prompt.slice(0, 20) || '运维答疑', prompt, opsResult.value)
  ElMessage.success('运维答疑已生成')
}
const genDoc = async () => {
  const fallback = docType.value === '存证'
    ? '【存证规范文档草案】\n1. 实习备案、日报、考核、证明等关键节点应生成内容哈希。\n2. 链上写入失败时标记为本地待同步。\n3. 核验页应展示哈希、时间、区块号与同步状态。'
    : '【实习管理规范草案】\n1. 学生提交备案后由学校与企业协同确认。\n2. 企业导师按月完成考核并归档。\n3. 学校可巡检过程记录。'
  const prompt = '请草拟一份' + docType.value + '相关制度文档，适用于实习证明与实习记录存证平台。'
  docResult.value = await askDeepSeek(prompt, fallback)
  saveHistory('制度草拟', docType.value + '制度文档', prompt, docResult.value)
  ElMessage.success('制度文档已生成')
}
</script>
<style scoped>

@keyframes dashIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
.page { width: 100%; max-width: 800px; }
.page-head { margin-bottom: 20px; }
.page-head h1 { font-size: 22px; font-weight: 700; color: #334155; letter-spacing: -0.5px; }
.ai-card { background: #FFF; border: none; border-radius: 14px; box-shadow: 0 2px 14px rgba(0,0,0,0.05); padding: 8px 20px 20px; }
.ai-section { padding: 8px 0; }
.ai-result { background: #F8FAFC; border: 1px solid #F1F5F9; border-radius: 8px; padding: 16px; margin-top: 12px; color: #334155; font-size: 13px; white-space: pre-wrap; line-height: 1.6; }
.risk-tag { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.tag-high { background: #FEF2F2; color: #DC2626; }
.tag-mid { background: #FFFBEB; color: #D97706; }
.history-card { margin-top:14px; background:#FFF; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:16px; }
.history-head { font-size:14px; font-weight:700; color:#334155; margin-bottom:10px; }
.history-empty { color:#94A3B8; font-size:13px; padding:10px 0; }
.history-item { padding:10px 12px; border-radius:10px; cursor:pointer; }
.history-item:hover { background:#F8FAFC; }
.history-title { color:#334155; font-size:13px; font-weight:600; }
.history-meta { color:#94A3B8; font-size:11px; margin-top:3px; }
</style>
