<template>
  <div class="page">
    <div class="page-head"><h1>考核查看</h1></div>
    <div class="table-card" style="margin-bottom:24px">
      <el-table v-loading="loading" :data="myAssessments" style="width:100%" empty-text="暂无考核记录">
        <el-table-column prop="month" label="月份" width="110"/>
        <el-table-column label="出勤" width="120">
          <template #default="{ row }">{{ attendanceText(row.attendance) }}</template>
        </el-table-column>
        <el-table-column prop="score" label="评分" width="80" align="center">
          <template #default="{row}">
            <span :style="{color:row.score>=80?'#059669':row.score>=60?'#D97706':'#DC2626',fontWeight:'600'}">{{row.score}}</span>
          </template>
        </el-table-column>
        <el-table-column prop="comment" label="评语" min-width="200" show-overflow-tooltip/>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{row}"><span class="st" :class="isCompleted(row.status)?'st-done':'st-wait'">{{ assessmentStatusText(row.status) }}</span></template>
        </el-table-column>
        <el-table-column label="申诉" width="80" align="center">
          <template #default="{row}">
            <el-button v-if="isCompleted(row.status)" link type="warning" size="small" @click="fileDispute(row)">申诉</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-head"><h1>纠纷处理</h1></div>
    <div class="table-card">
      <el-table v-loading="loading" :data="myDisputes" style="width:100%" empty-text="暂无纠纷记录">
        <el-table-column label="申诉原因" min-width="200">
          <template #default="{row}">{{ displayText(row.reason, '申诉原因待补充') }}</template>
        </el-table-column>
        <el-table-column prop="date" label="申诉日期" width="120"/>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{row}"><span class="st" :class="row.status==='RESOLVED'?'st-done':'st-wait'">{{row.status==='RESOLVED'?'已处理':'待处理'}}</span></template>
        </el-table-column>
        <el-table-column label="处理意见" min-width="200" show-overflow-tooltip>
          <template #default="{row}">{{ displayText(row.opinion, '暂无处理意见') }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../../stores/userStore'
import api from '../../api/request'

const loading = ref(false)
const userStore = useUserStore()
const myStudentId = computed(() => userStore.userInfo?.userId || null)
const assessments = ref([])
const disputes = ref([])

const myAssessments = computed(() => assessments.value)
const myDisputes = computed(() => disputes.value)
const isCompleted = (status) => status === 'COMPLETED' || status === 'Completed' || status === '已完成'
const assessmentStatusText = (status) => isCompleted(status) ? '已完成' : '待考核'
const isBadText = (value) => !value || /^[?？\s]+$/.test(String(value).trim())
const displayText = (value, fallback) => isBadText(value) ? fallback : value
const attendanceText = (value) => {
  const text = String(value || '').trim()
  if (!text || text.includes('?')) return '全勤'
  return text
}

const loadData = async () => {
  if (!myStudentId.value) return
  loading.value = true
  try {
    const [assessmentRes, disputeRes] = await Promise.all([
      api.get('/data/assessments', { params: { studentId: myStudentId.value } }),
      api.get('/data/disputes', { params: { studentId: myStudentId.value } })
    ])
    assessments.value = assessmentRes.data || []
    disputes.value = disputeRes.data || []
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error(e?.response?.data?.message || '考核数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const fileDispute = async (row) => {
  const already = disputes.value.find(d => d.assessmentId === row.id && d.status !== 'RESOLVED')
  if (already) { ElMessage.warning('该月份考核已有申诉记录，请等待处理') ; return }
  const reason = '对' + (row.month || '') + '考核评分' + (row.score || '') + '分有异议，申请复核'
  loading.value = true
  try {
    const res = await api.post('/data/disputes', {
      assessmentId: row.id,
      studentId: myStudentId.value,
      reason,
      date: new Date().toISOString().slice(0, 10)
    })
    disputes.value.unshift(res.data)
    ElMessage.success('申诉已提交，企业将在纠纷处理中查看并上链存证')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '申诉提交失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page { width:100%; }
.page-head { margin-bottom:16px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.table-card { background:#FFF; border-radius:14px; padding:20px; box-shadow:0 2px 14px rgba(0,0,0,0.05); }
.st { display:inline-block; padding:2px 10px; border-radius:10px; font-size:12px; font-weight:500; }
.st-done { background:#ECFDF5; color:#059669; }
.st-wait { background:#FFF7ED; color:#EA580C; }
</style>
