<template>
  <div class="page">
    <div class="page-head"><h1>AI 助手</h1></div>
    <div class="ai-card">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="报表生成" name="report">
          <div class="ai-section">
            <el-select v-model="reportType"><el-option label="月度汇总报告" value="月度"/><el-option label="年度汇总报告" value="年度"/></el-select>
            <el-button type="primary" style="margin-top:12px" @click="genReport">一键生成</el-button>
            <div class="ai-result" v-if="reportResult">{{ reportResult }}</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="资质筛查" name="audit">
          <div class="ai-section">
            <el-upload drag action="#" :auto-upload="false" class="upload-wide"><el-icon :size="40" color="#93C5FD"><UploadFilled /></el-icon><div class="upload-text">上传企业营业执照</div></el-upload>
            <el-button type="primary" style="margin-top:12px" @click="auditLicense">AI 筛查</el-button>
            <div class="ai-result" v-if="auditResult">{{ auditResult }}</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="异常分析" name="anomaly">
          <div class="ai-section">
            <el-table :data="anomalies" size="small"><el-table-column prop="student" label="学生"/><el-table-column prop="issue" label="异常描述"/><el-table-column label="操作"><template #default="{ row }"><el-button link type="primary" size="small" @click="judgeAnomaly(row)">AI研判</el-button></template></el-table-column></el-table>
            <div class="ai-result" v-if="anomalyResult">{{ anomalyResult }}</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="审批批注" name="approve">
          <div class="ai-section">
            <el-select v-model="rejectType"><el-option label="备案驳回" value="备案"/><el-option label="证书驳回" value="证书"/></el-select>
            <el-button type="primary" style="margin-top:12px" @click="genRejectReason">生成驳回理由</el-button>
            <div class="ai-result" v-if="rejectResult">{{ rejectResult }}</div>
          </div>
        </el-tab-pane>
        <el-tab-pane label="政策咨询" name="policy">
          <div class="ai-section">
            <el-input v-model="policyQ" placeholder="输入政策问题..."/>
            <el-button type="primary" style="margin-top:12px" @click="answerPolicy">咨询</el-button>
            <div class="ai-result" v-if="policyA">{{ policyA }}</div>
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
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import api from '../../api/request'
import { useUserStore } from '../../stores/userStore'
const userStore = useUserStore()
const activeTab=ref('report'), reportType=ref('月度'), reportResult=ref('')
const rejectType=ref('备案'), policyQ=ref(''), policyA=ref(''), auditResult=ref(''), anomalyResult=ref(''), rejectResult=ref('')
const anomalies=ref([{student:'王五',issue:'连续15天无日报填报'}])
const storageKey = computed(() => `ai_history_school_${userStore.userInfo?.userId || 'guest'}`)
const loadHistory = () => {
  try { return JSON.parse(localStorage.getItem(storageKey.value) || '[]') } catch { return [] }
}
const history = ref(loadHistory())
const saveHistory = (type, title, prompt, result, targetTab) => {
  history.value.unshift({ id: Date.now(), type, title, prompt, result, targetTab, date: new Date().toISOString().slice(0, 10) })
  history.value = history.value.slice(0, 30)
}
const restoreHistory = (item) => {
  activeTab.value = item.targetTab || 'report'
  if (item.targetTab === 'audit') auditResult.value = item.result
  else if (item.targetTab === 'anomaly') anomalyResult.value = item.result
  else if (item.targetTab === 'approve') rejectResult.value = item.result
  else if (item.targetTab === 'policy') { policyA.value = item.result; policyQ.value = item.prompt || '' }
  else reportResult.value = item.result
}
watch(history, () => localStorage.setItem(storageKey.value, JSON.stringify(history.value)), { deep: true })
const askDeepSeek = async (prompt, fallback) => {
  try {
    const res = await api.post('/ai-assistant/chat', { message: prompt })
    return res.data?.response || fallback
  } catch (e) {
    ElMessage.warning('DeepSeek 暂不可用，已使用本地结果')
    return fallback
  }
}
const genReport=async()=>{const prompt='请生成学校端' + reportType.value + '实习汇总报告，包含在岗学生、备案、日报、考核、风险建议。'; reportResult.value=await askDeepSeek(prompt,'【实习汇总报告】\n请关注在岗学生、企业岗位、过程材料和异常预警。'); saveHistory('报表生成', reportType.value + '汇总报告', prompt, reportResult.value, 'report'); ElMessage.success('报告已生成')}
const auditLicense=async()=>{const prompt='请给出企业营业执照/企业资质审核时的核查要点，输出简洁清单。'; auditResult.value=await askDeepSeek(prompt,'【资质筛查结果】\n建议核对企业名称、统一社会信用代码、主体一致性和岗位真实性。'); saveHistory('资质筛查', '企业资质筛查', prompt, auditResult.value, 'audit'); ElMessage.success('筛查结果已生成')}
const judgeAnomaly=async(row)=>{const prompt='请研判学校实习异常：学生' + row.student + '，问题：' + row.issue + '。给出处置建议。'; anomalyResult.value=await askDeepSeek(prompt,'【异常研判】\n建议先提醒补交材料，逾期转入巡检记录。'); saveHistory('异常分析', row.student + '异常研判', prompt, anomalyResult.value, 'anomaly'); ElMessage.success('研判已生成')}
const genRejectReason=async()=>{const prompt='请生成' + rejectType.value + '驳回理由，语气正式，要求说明需补充哪些材料。'; rejectResult.value=await askDeepSeek(prompt,'【' + rejectType.value + '驳回理由】\n材料信息暂不完整，请补充可核验材料后重新提交。'); saveHistory('审批批注', rejectType.value + '驳回理由', prompt, rejectResult.value, 'approve'); ElMessage.success('驳回理由已生成')}
const answerPolicy=async()=>{
  if (!policyQ.value.trim()) { ElMessage.warning('请输入政策问题'); return }
  const prompt = policyQ.value.trim()
  policyA.value=await askDeepSeek('请以学校实习管理人员身份回答政策咨询：' + prompt, '【政策咨询】\n建议依据学校实习管理办法处理，过程材料缺失先补正，企业资质风险先复核。')
  saveHistory('政策咨询', prompt.slice(0, 20) || '政策咨询', prompt, policyA.value, 'policy')
  ElMessage.success('咨询结果已生成')
}
</script>
<style scoped>

@keyframes dashIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
.page { width:100%; animation: dashIn 0.5s ease-out; max-width:800px; }
.page-head { margin-bottom:20px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; letter-spacing:-0.5px; }
.ai-card { background:#FFF; border:none; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:8px 20px 20px; }
.ai-section { padding:8px 0; }
.ai-result { background:#F8FAFC; border:1px solid #F1F5F9; border-radius:8px; padding:16px; margin-top:12px; color:#334155; font-size:13px; white-space:pre-wrap; line-height:1.6; }
.upload-wide { width:100% !important; }
.upload-wide :deep(.el-upload-dragger) { min-height:120px; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:8px; background:#F8FAFC; border:2px dashed #CBD5E1; border-radius:10px; }
.upload-wide :deep(.el-upload-dragger:hover) { border-color:#2563EB; background:#EFF6FF; }
.upload-text { color:#94A3B8; font-size:13px; }
.history-card { margin-top:14px; background:#FFF; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:16px; }
.history-head { font-size:14px; font-weight:700; color:#334155; margin-bottom:10px; }
.history-empty { color:#94A3B8; font-size:13px; padding:10px 0; }
.history-item { padding:10px 12px; border-radius:10px; cursor:pointer; }
.history-item:hover { background:#F8FAFC; }
.history-title { color:#334155; font-size:13px; font-weight:600; }
.history-meta { color:#94A3B8; font-size:11px; margin-top:3px; }
</style>
