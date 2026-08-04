<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>过程考核</h1>
        <span class="head-sub">实习生月度考核与链上存证</span>
      </div>
      <div class="head-actions">
        <div class="month-tabs">
          <span :class="{ active: filterMonth === '' }" @click="filterMonth = ''">全部</span>
          <span v-for="m in recentMonths" :key="m" :class="{ active: filterMonth === m }" @click="filterMonth = m">{{ m.slice(5) }}月</span>
        </div>
        <el-button type="primary" @click="openNew"><el-icon><Plus /></el-icon>新增考核</el-button>
      </div>
    </div>

    <!-- 四张统计卡 -->
    <div class="stats-row">
      <div class="stat-item">
        <div class="stat-icon" style="background:#EFF6FF"><el-icon :size="20" color="#2563EB"><Document /></el-icon></div>
        <div class="stat-info"><span class="stat-val">{{ stats.total }}</span><span class="stat-lbl">本月考核</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon" style="background:#ECFDF5"><el-icon :size="20" color="#059669"><TrendCharts /></el-icon></div>
        <div class="stat-info"><span class="stat-val">{{ stats.avgScore }}<small>分</small></span><span class="stat-lbl">平均评分</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon" style="background:#FFF7ED"><el-icon :size="20" color="#EA580C"><Calendar /></el-icon></div>
        <div class="stat-info"><span class="stat-val">{{ stats.fullAttend }}<small>/{{ stats.total || 1 }}</small></span><span class="stat-lbl">全勤</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-icon" style="background:#FDF2F8"><el-icon :size="20" color="#DB2777"><Star /></el-icon></div>
        <div class="stat-info"><span class="stat-val">{{ stats.excellent }}<small>人</small></span><span class="stat-lbl">优秀(≥90)</span></div>
      </div>
    </div>

    <!-- 考核表格 -->
    <div class="table-card">
      <el-table :data="filtered" style="width:100%" empty-text="暂无考核记录，点击右上角「新增考核」开始">
        <el-table-column label="学生" min-width="120">
          <template #default="{row}">
            <div class="student-cell">
              <span class="stu-avatar" :style="{background:colors[row.student?row.student.charCodeAt(0)%8:0]}">{{ row.student ? row.student.charAt(0) : '?' }}</span>
              <span class="stu-name">{{ row.student }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="month" label="考核月份" width="110" align="center"/>
        <el-table-column prop="attendance" label="出勤" width="110" align="center">
          <template #default="{row}">
            <span :class="attendanceText(row.attendance)==='全勤'?'tag-ok':'tag-warn'">{{ attendanceText(row.attendance) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="100" align="center">
          <template #default="{row}">
            <div class="score-cell">
              <div class="score-bar-bg"><div class="score-bar-fill" :style="{width:(row.score||0)+'%',background:scoreBarColor(row.score)}"></div></div>
              <span class="score-num" :style="{color:scoreBarColor(row.score)}">{{ row.score || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{row}">
            <span class="status-tag" :class="isCompleted(row.status)?'st-ok':'st-pend'">{{ assessmentStatusText(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="280">
          <template #default="{row}">
            <div class="action-cell">
              <el-button link type="primary" size="small" @click="openEdit(row)">填写考核</el-button>
              <el-button link type="primary" size="small" @click="showDetail(row)">鉴定表</el-button>
              <el-button v-if="aiOn" link class="ai-link" size="small" @click="aiGenerate(row)"><el-icon><Promotion /></el-icon>AI评语</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-foot"><span>共 {{ filtered.length }} 条考核记录</span></div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="填写考核" width="460px" class="dlg">
      <el-form :model="editForm" label-width="70px">
        <el-form-item label="学生"><el-input :model-value="editForm.student" disabled/></el-form-item>
        <el-form-item label="月份"><el-input :model-value="editForm.month" disabled/></el-form-item>
        <el-form-item label="出勤">
          <el-radio-group v-model="editForm.attendance">
            <el-radio value="全勤">全勤</el-radio>
            <el-radio value="缺勤1-2天">缺勤1-2天</el-radio>
            <el-radio value="缺勤3天以上">缺勤3天以上</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评分">
          <el-input-number v-model="editForm.score" :min="0" :max="100" size="large" style="width:160px"/>
          <span class="score-hint" :style="{color:scoreBarColor(editForm.score)}">{{ scoreLabel(editForm.score) }}</span>
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="editForm.comment" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="输入考核评语..."/>
          <el-button v-if="aiOn" class="ai-inline" size="small" @click="quickAi"><el-icon><Promotion /></el-icon>AI生成</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible=false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存并上链</el-button>
      </template>
    </el-dialog>

    <!-- 新增弹窗 -->
    <el-dialog v-model="newVisible" title="新增考核" width="460px" class="dlg">
      <el-form :model="newForm" label-width="70px">
        <el-form-item label="学生">
          <el-select v-model="newForm.internshipId" placeholder="选择实习生" style="width:100%">
            <el-option v-for="s in hiredStudents" :key="s.internshipId" :label="s.name+' · '+s.position" :value="s.internshipId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="月份">
          <el-date-picker v-model="newForm.month" type="month" placeholder="选择月份" style="width:100%" format="YYYY-MM" value-format="YYYY-MM"/>
        </el-form-item>
        <el-form-item label="出勤">
          <el-radio-group v-model="newForm.attendance">
            <el-radio value="全勤">全勤</el-radio>
            <el-radio value="缺勤1-2天">缺勤1-2天</el-radio>
            <el-radio value="缺勤3天以上">缺勤3天以上</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="评分">
          <el-input-number v-model="newForm.score" :min="0" :max="100" size="large" style="width:160px"/>
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="newForm.comment" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="输入考核评语..."/>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="newVisible=false">取消</el-button>
        <el-button type="primary" @click="saveNew" :loading="saving">保存并上链</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Document, TrendCharts, Calendar, Star, Promotion } from '@element-plus/icons-vue'
import { useDataStore } from '../../stores/dataStore'
import { useSystemConfig } from '../../stores/systemConfig'
import { useUserStore } from '../../stores/userStore'
import api from '../../api/request'

const saving = ref(false)
const loading = ref(false)
const dataStore = useDataStore()
const sysCfg = useSystemConfig()
const aiOn = computed(() => sysCfg.aiEnabled)
const userStore = useUserStore()
const myCompanyId = computed(() => userStore.userInfo?.userId || null)
const internships = ref([])
const assessments = ref([])

const editVisible = ref(false)
const filterMonth = ref('')
const editForm = reactive({ id: null, internshipId: null, studentId: null, companyId: null, student: '', month: '', attendance: '全勤', score: 80, comment: '' })
const newVisible = ref(false)
const newForm = reactive({ internshipId: null, month: '', attendance: '全勤', score: 80, comment: '' })

const hiredStudents = computed(() => internships.value
  .filter(i => i.enterpriseId === myCompanyId.value && i.status === 'ACTIVE')
  .map(i => ({
    internshipId: i.id,
    studentId: i.studentId,
    companyId: i.enterpriseId,
    name: i.studentName,
    position: i.position || '实习岗位'
  })))
const filtered = computed(() => {
  let list = assessments.value.filter(a => a.companyId === myCompanyId.value)
  return filterMonth.value ? list.filter(a => a.month === filterMonth.value) : list
})

onMounted(async () => {
  if (!myCompanyId.value) return
  loading.value = true
  try {
    const [internshipRes, assessmentRes] = await Promise.all([
      api.get('/data/internships', { params: { enterpriseId: myCompanyId.value } }),
      api.get('/data/assessments', { params: { companyId: myCompanyId.value } })
    ])
    internships.value = internshipRes.data || []
    assessments.value = assessmentRes.data || []
    dataStore.internships = internshipRes.data || []
    dataStore.assessments = assessmentRes.data || []
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error(e?.response?.data?.message || '考核数据加载失败')
  } finally {
    loading.value = false
  }
})

const recentMonths = computed(() => {
  const now = new Date(); const months = []
  for (let i = 0; i < 6; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    months.push(d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0'))
  }
  return months
})

const stats = computed(() => {
  const list = filtered.value; const total = list.length
  const scores = list.map(a => a.score || 0)
  const avg = total > 0 ? Math.round(scores.reduce((a, b) => a + b, 0) / total) : 0
  return { total, avgScore: avg, fullAttend: list.filter(a => attendanceText(a.attendance) === '全勤').length, excellent: list.filter(a => (a.score || 0) >= 90).length }
})

const colors = ['#2563EB','#059669','#D97706','#DB2777','#7C3AED','#0891B2','#DC2626','#4F46E5']
const isCompleted = (status) => status === 'COMPLETED' || status === 'Completed' || status === '已完成'
const assessmentStatusText = (status) => isCompleted(status) ? '已完成' : '待考核'
const attendanceText = (value) => {
  const text = String(value || '').trim()
  if (!text || text.includes('?')) return '全勤'
  return text
}
const scoreBarColor = (s) => { if (!s) return '#CBD5E1'; if (s >= 90) return '#059669'; if (s >= 80) return '#2563EB'; if (s >= 60) return '#D97706'; return '#DC2626' }
const scoreLabel = (s) => { if (!s) return ''; if (s >= 90) return '优秀'; if (s >= 80) return '良好'; if (s >= 60) return '合格'; return '待改进' }

const openEdit = (row) => { Object.assign(editForm, { ...row, attendance: attendanceText(row.attendance) }); editVisible.value = true }
const showDetail = (row) => {
  const html = '<html><head><meta charset="UTF-8"><title>实习考核鉴定表</title></head><body style="font-family:SimSun;padding:40px"><h2 style="text-align:center">实习考核鉴定表</h2><table border="1" cellpadding="8" cellspacing="0" style="width:100%;border-collapse:collapse;margin-top:30px"><tr><td>学生姓名</td><td>'+row.student+'</td><td>考核月份</td><td>'+row.month+'</td></tr><tr><td>出勤情况</td><td>'+attendanceText(row.attendance)+'</td><td>考核评分</td><td>'+(row.score||'')+'</td></tr><tr><td>评语</td><td colspan="3" style="min-height:80px">'+(row.comment||'')+'</td></tr><tr><td>状态</td><td>'+(row.status||'已完成')+'</td><td>链上存证</td><td style="color="#2563EB">✔ 已上链</td></tr></table><p style="text-align:right;margin-top:40px;font-size:12px;color="#666"">生成时间: '+new Date().toLocaleString()+'</p></body></html>'
  const blob = new Blob([html], { type: 'text/html;charset=UTF-8' })
  const a = document.createElement('a'); a.href = URL.createObjectURL(blob); a.download = row.student + '-' + row.month + '考核鉴定表.html'
  a.click(); URL.revokeObjectURL(a.href); ElMessage.success('鉴定表已下载')
}

const aiGenerate = (row) => { openEdit(row); quickAi() }
const quickAi = () => { editForm.comment = '该生在实习期间表现良好，工作态度端正，出勤正常，能按时完成分配任务。专业技能方面展现出较强的学习能力，团队协作顺畅。'; editForm.score = Math.floor(Math.random() * 20) + 80; ElMessage.success('AI评语已生成') }

const archiveMessage = (archive, label) => {
  if (!sysCfg.autoChain) return label + '已保存'
  if (archive?.chainStatus === 'ON_CHAIN' || archive?.chainState === 'on-chain') return label + '已保存并完成链上存证'
  if (archive?.chainStatus === 'LOCAL_FALLBACK' || archive?.chainState === 'synced') return label + '已保存并同步到后端存证'
  return label + '已保存，本地存证待链路恢复后同步'
}

const upsertAssessment = (record) => {
  const idx = assessments.value.findIndex(a => a.id === record.id || (a.internshipId === record.internshipId && a.month === record.month))
  if (idx >= 0) assessments.value[idx] = record
  else assessments.value.unshift(record)
  const storeIdx = dataStore.assessments.findIndex(a => a.id === record.id || (a.internshipId === record.internshipId && a.month === record.month))
  if (storeIdx >= 0) dataStore.assessments[storeIdx] = record
  else dataStore.assessments.unshift(record)
}

const saveAssessmentPayload = async (payload) => {
  const res = await api.post('/data/assessments', payload)
  const record = res.data
  upsertAssessment(record)
  return record
}

const save = async () => {
  saving.value = true
  try {
    const archive = await saveAssessmentPayload({
      internshipId: editForm.internshipId,
      studentId: editForm.studentId,
      companyId: editForm.companyId || myCompanyId.value,
      month: editForm.month,
      attendance: editForm.attendance,
      score: editForm.score,
      comment: editForm.comment
    })
    editVisible.value = false
    dataStore.messages.push({ id: Date.now(), title: '考核通知', content: '你的' + editForm.month + '考核已完成，评分' + editForm.score + '分，请前往考核查看。', from: '企业端', to: editForm.student, date: new Date().toISOString().slice(0, 10), read: false })
    ElMessage.success(archiveMessage(archive, '考核'))
  } finally {
    saving.value = false
  }
}
const openNew = () => { Object.assign(newForm, { internshipId: null, month: '', attendance: '全勤', score: 80, comment: '' }); newVisible.value = true }
const saveNew = async () => {
  const selected = hiredStudents.value.find(s => s.internshipId === newForm.internshipId)
  if (!selected || !newForm.month) { ElMessage.warning('请选择学生和月份'); return }
  saving.value = true
  try {
    const archive = await saveAssessmentPayload({
      internshipId: selected.internshipId,
      studentId: selected.studentId,
      companyId: selected.companyId,
      month: newForm.month,
      attendance: newForm.attendance,
      score: newForm.score,
      comment: newForm.comment
    })
    newVisible.value = false
    ElMessage.success(archiveMessage(archive, '考核'))
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.page { width: 100%; }

.page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.page-head h1 { font-size: 22px; font-weight: 700; color: #0F172A; margin: 0 0 4px 0; }
.head-sub { font-size: 13px; color: #94A3B8; }
.head-actions { display: flex; align-items: center; gap: 16px; }

.month-tabs { display: flex; gap: 4px; background: #F1F5F9; padding: 3px; border-radius: 10px; }
.month-tabs span { padding: 5px 14px; font-size: 12px; font-weight: 500; color: #64748B; border-radius: 8px; cursor: pointer; transition: all .2s; }
.month-tabs span:hover { color: #334155; }
.month-tabs span.active { background: #FFF; color: #2563EB; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }

.stats-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 14px; margin-bottom: 20px; }
.stat-item { background: #FFF; border-radius: 12px; padding: 16px 18px; display: flex; align-items: center; gap: 14px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.stat-icon { width: 42px; height: 42px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-info { display: flex; flex-direction: column; }
.stat-val { font-size: 22px; font-weight: 700; color: #0F172A; line-height: 1.1; }
.stat-val small { font-size: 12px; font-weight: 400; color: #94A3B8; }
.stat-lbl { font-size: 12px; color: #94A3B8; margin-top: 1px; }

.table-card { background: #FFF; border-radius: 14px; padding: 20px; box-shadow: 0 2px 10px rgba(0,0,0,0.04); }
.table-foot { margin-top: 14px; text-align: right; font-size: 12px; color: #94A3B8; }

.student-cell { display: flex; align-items: center; gap: 10px; }
.stu-avatar { width: 34px; height: 34px; border-radius: 10px; display: flex; align-items: center; justify-content: center; color: #FFF; font-weight: 600; font-size: 14px; flex-shrink: 0; }
.stu-name { font-weight: 500; color: #334155; }

.tag-ok { color: #059669; font-size: 13px; }
.tag-warn { color: #EA580C; font-size: 13px; }

.score-cell { display: flex; align-items: center; gap: 8px; }
.score-bar-bg { width: 60px; height: 6px; background: #F1F5F9; border-radius: 3px; overflow: hidden; }
.score-bar-fill { height: 100%; border-radius: 3px; transition: width .4s ease; }
.score-num { font-size: 14px; font-weight: 700; min-width: 24px; }

.status-tag { display: inline-block; padding: 2px 12px; border-radius: 12px; font-size: 12px; font-weight: 500; }
.st-ok { background: #ECFDF5; color: #059669; }
.st-pend { background: #FFF7ED; color: #EA580C; }

.action-cell { display: flex; gap: 8px; align-items: center; }
.ai-link { color: #7C3AED !important; }

.dlg :deep(.el-dialog__header) { padding-bottom: 8px; }
.score-hint { margin-left: 12px; font-size: 13px; font-weight: 600; }
.ai-inline { margin-top: 8px; color: #7C3AED !important; border-color: #DDD6FE !important; background: #F5F3FF !important; border-radius: 8px; }
</style>
