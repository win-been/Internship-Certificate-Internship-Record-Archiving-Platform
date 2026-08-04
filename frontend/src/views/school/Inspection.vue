<template>
  <div class="page">
    <div class="page-head"><h1>过程巡查</h1><div class="fr"><el-input v-model="search" placeholder="搜索学生..." size="default" style="width:180px" clearable/></div></div>
    <div class="table-card">
      <el-table v-loading="loading" :data="filtered" style="width:100%" empty-text="暂无巡查记录">
        <el-table-column prop="student" label="学生" width="80"/><el-table-column prop="enterprise" label="企业" min-width="120"/>
        <el-table-column prop="position" label="岗位" min-width="120"/><el-table-column prop="days" label="在岗天数" width="80" align="center"/>
        <el-table-column prop="lastReport" label="最后日报" width="110"/><el-table-column label="状态" width="90" align="center"><template #default="{row}"><span class="st" :class="row.ok?'st-ok':'st-warn'">{{row.ok?'正常':'异常'}}</span></template></el-table-column>
        <el-table-column label="操作" width="100" align="center"><template #default="{row}"><el-button link type="primary" size="small" @click="viewDetail(row)">查看详情</el-button></template></el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="detailVisible" title="巡查详情" width="460px">
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="学生">{{ curr?.student }}</el-descriptions-item>
        <el-descriptions-item label="企业">{{ curr?.enterprise }}</el-descriptions-item>
        <el-descriptions-item label="岗位">{{ curr?.position }}</el-descriptions-item>
        <el-descriptions-item label="在岗天数">{{ curr?.days }} 天</el-descriptions-item>
        <el-descriptions-item label="最后日报">{{ curr?.lastReport }}</el-descriptions-item>
      </el-descriptions>
      <template #footer><el-button @click="detailVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api/request'
import { useUserStore } from '../../stores/userStore'
const userStore = useUserStore()
const search = ref(''), detailVisible = ref(false), curr = ref(null), loading = ref(false)
const internships = ref([])
const reports = ref([])
const assessments = ref([])
const mySchoolId = computed(() => userStore.userInfo?.schoolId || userStore.userInfo?.userId || null)

const loadData = async () => {
  if (!mySchoolId.value) return
  loading.value = true
  try {
    const [internshipsRes, reportsRes, assessmentsRes] = await Promise.all([
      api.get('/data/internships', { params: { schoolId: mySchoolId.value } }),
      api.get('/data/reports', { params: { schoolId: mySchoolId.value } }),
      api.get('/data/assessments', { params: { schoolId: mySchoolId.value } })
    ])
    internships.value = internshipsRes.data || []
    reports.value = reportsRes.data || []
    assessments.value = assessmentsRes.data || []
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error(e?.response?.data?.message || '过程巡检数据加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const records = computed(() => {
  const interns = (internships.value || []).filter(i => i.status === 'ACTIVE')
  const reportList = (reports.value || [])
  const assessmentList = (assessments.value || [])
  return interns.map(intern => {
    const studentReports = reportList.filter(r => r.internshipId === intern.id || (!r.internshipId && r.studentId === intern.studentId))
    const lastReport = studentReports.length > 0 ? studentReports.sort((a,b) => (b.date||'').localeCompare(a.date||''))[0] : null
    const studentAssessments = assessmentList.filter(a => a.internshipId === intern.id || (!a.internshipId && a.studentId === intern.studentId))
    const allPassed = studentAssessments.length > 0 && studentAssessments.every(a => (a.score || 0) >= 60)
    const daysSinceStart = intern.startDate ? Math.floor((new Date() - new Date(intern.startDate)) / (1000*60*60*24)) : 0
    return {
      student: intern.studentName || '未知',
      enterprise: intern.enterpriseName || '未知',
      position: intern.position || '未知',
      days: daysSinceStart,
      lastReport: lastReport ? lastReport.date : '无',
      ok: daysSinceStart > 0 && (lastReport || studentAssessments.length > 0)
    }
  })
})
const filtered = computed(() => search.value ? records.value.filter(r=>r.student.includes(search.value)) : records.value)
const viewDetail = (row) => { curr.value = row; detailVisible.value = true }
</script>
<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.fr { display:flex; gap:10px; }
.st { display:inline-block; padding:2px 10px; border-radius:10px; font-size:12px; font-weight:500; }
.st-ok { background:#ECFDF5; color:#059669; }
.st-warn { background:#FEF2F2; color:#DC2626; }
</style>
