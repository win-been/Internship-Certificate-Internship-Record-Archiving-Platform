<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>岗位管理</h1>
        <p>发布岗位后，学生端和学校端会同步看到真实岗位数据。</p>
      </div>
      <el-button type="primary" @click="openCreate" size="large" class="btn-publish">
        <el-icon><Plus /></el-icon>
        发布岗位
      </el-button>
    </div>

    <div class="table-card">
      <el-table :data="jobs" style="width:100%" empty-text="暂无发布岗位" v-loading="loading">
        <el-table-column prop="title" label="岗位名称" min-width="160">
          <template #default="{row}"><span class="cell-title">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column prop="location" label="地点" width="90" align="center" />
        <el-table-column prop="salary" label="薪资" width="150" align="center" />
        <el-table-column label="申请数" width="90" align="center">
          <template #default="{row}"><span class="cell-num">{{ getAppCount(row.id) }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{row}">
            <span class="status-tag" :class="row.status === 'OPEN' ? 'tag-open' : 'tag-closed'">
              {{ row.status === 'OPEN' ? '开放中' : '已关闭' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="170" align="center">
          <template #default="{row}">
            <div class="op-row">
              <el-button link class="btn-edit" @click="openEdit(row)">
                <el-icon><Edit /></el-icon>
                编辑
              </el-button>
              <el-dropdown trigger="click" @command="(cmd) => handleCmd(cmd, row)">
                <span class="action-more" @click.stop>
                  <el-icon :size="16"><MoreFilled /></el-icon>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="toggle">{{ row.status === 'OPEN' ? '下架' : '上架' }}</el-dropdown-item>
                    <el-dropdown-item command="applicants">查看申请({{ getAppCount(row.id) }})</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><span class="table-total">共 {{ jobs.length }} 个岗位</span></div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      width="720px"
      append-to-body
      class="job-dialog"
      :show-close="false"
    >
      <template #header>
        <div class="dialog-hero">
          <div>
            <span class="dialog-kicker">{{ isEditing ? '更新岗位' : '链上发布' }}</span>
            <h2>{{ isEditing ? '编辑岗位信息' : '发布新的实习岗位' }}</h2>
            <p>岗位会绑定当前企业账号，发布成功后同步写入链上归档。</p>
          </div>
          <div class="dialog-orbit"><el-icon><Tickets /></el-icon></div>
        </div>
      </template>

      <el-form :model="form" label-position="top" class="job-form">
        <div class="form-grid">
          <el-form-item label="岗位名称" class="span-2">
            <el-input v-model="form.title" placeholder="例如：前端开发实习生" />
          </el-form-item>
          <el-form-item label="企业名称">
            <el-input v-model="form.company" placeholder="企业名称" disabled />
          </el-form-item>
          <el-form-item label="工作地点">
            <el-input v-model="form.location" placeholder="例如：北京" />
          </el-form-item>
          <el-form-item label="薪资范围">
            <el-input v-model="form.salary" placeholder="例如：3,000-5,000 元/月" />
          </el-form-item>
          <el-form-item label="实习类型">
            <el-select v-model="form.type" style="width:100%">
              <el-option label="全职实习" value="全职实习" />
              <el-option label="兼职实习" value="兼职实习" />
            </el-select>
          </el-form-item>
          <el-form-item label="岗位状态">
            <el-radio-group v-model="form.status" class="status-switch">
              <el-radio-button value="OPEN">开放中</el-radio-button>
              <el-radio-button value="CLOSED">已关闭</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="岗位描述" class="span-2">
            <el-input
              v-model="form.description"
              type="textarea"
              :rows="4"
              placeholder="写清楚工作内容、能力要求和实习周期"
            />
          </el-form-item>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <span class="chain-note">保存后生成岗位归档交易记录</span>
          <div>
            <el-button @click="dialogVisible=false">取消</el-button>
            <el-button type="primary" @click="saveJob" :loading="saving">
              {{ isEditing ? '保存修改' : '发布岗位' }}
            </el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="applicantsVisible" title="申请列表" width="520px" append-to-body>
      <el-table :data="applicantList" size="small" empty-text="暂无申请">
        <el-table-column prop="name" label="姓名" />
        <el-table-column prop="school" label="学校" />
        <el-table-column prop="date" label="投递时间" />
      </el-table>
      <template #footer><el-button @click="applicantsVisible=false">关闭</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { MoreFilled, Edit, Plus, Tickets } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/userStore'
import { useDataStore } from '../../stores/dataStore'
import api from '../../api/request'

const userStore = useUserStore()
const dataStore = useDataStore()
const myCompanyId = computed(() => userStore.userInfo?.userId || null)
const myCompanyName = computed(() => {
  const fromUser = userStore.userInfo?.organizationName
  if (fromUser && fromUser !== '未命名企业') return fromUser
  const found = (dataStore.enterpriseList || []).find(e => e.id === myCompanyId.value)
  return found?.name || fromUser || userStore.userInfo?.realName || '企业名称未设置'
})

const dialogVisible = ref(false)
const applicantsVisible = ref(false)
const isEditing = ref(false)
const editingId = ref(0)
const loading = ref(false)
const saving = ref(false)
const jobs = ref([])
const applications = ref([])
const applicantList = ref([])
const qualificationStatus = ref('UNSUBMITTED')
const form = reactive({ title:'', company:'', location:'', salary:'', type:'全职实习', description:'', status:'OPEN' })

const fetchJobs = async () => {
  if (!myCompanyId.value) {
    jobs.value = []
    applications.value = []
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error('请先使用企业账号登录')
    return
  }
  loading.value = true
  try {
    const [jobsRes, appsRes, qualRes] = await Promise.all([
      api.get('/data/jobs', { params: { companyId: myCompanyId.value } }),
      api.get('/data/applications', { params: { companyId: myCompanyId.value } }),
      api.get('/data/enterprise-qualification/current').catch(() => null)
    ])
    jobs.value = jobsRes.data || []
    applications.value = appsRes.data || []
    qualificationStatus.value = qualRes?.data?.approval?.status || 'UNSUBMITTED'
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error(getApiError(e, '岗位数据加载失败'))
    jobs.value = []
    applications.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchJobs)

const getAppCount = (jobId) => {
  return applications.value.filter(a => a.jobId === jobId && a.status !== 'rejected').length
}

const openCreate = () => {
  if (qualificationStatus.value !== 'APPROVED') {
    ElMessage.warning('企业资质通过平台审核后才能发布岗位')
    return
  }
  isEditing.value = false
  editingId.value = 0
  Object.assign(form, { title:'', company: myCompanyName.value, location:'', salary:'', type:'全职实习', description:'', status:'OPEN' })
  dialogVisible.value = true
}

const openEdit = (row) => {
  if (qualificationStatus.value !== 'APPROVED') {
    ElMessage.warning('企业资质通过平台审核后才能维护岗位')
    return
  }
  isEditing.value = true
  editingId.value = row.id
  Object.assign(form, row, { company: row.company || myCompanyName.value })
  dialogVisible.value = true
}

const syncLocalJob = (job) => {
  const idx = (dataStore.jobs || []).findIndex(j => j.id === job.id)
  if (idx >= 0) dataStore.jobs[idx] = { ...dataStore.jobs[idx], ...job }
  else dataStore.jobs.push(job)
}

const saveJob = async () => {
  if (!form.title?.trim()) {
    ElMessage.warning('请填写岗位名称')
    return
  }
  saving.value = true
  const payload = { ...form, companyId: myCompanyId.value, company: myCompanyName.value }
  try {
    if (isEditing.value) {
      const res = await api.put('/data/jobs/' + editingId.value, payload)
      const saved = res.data || { ...payload, id: editingId.value }
      const idx = jobs.value.findIndex(j => j.id === editingId.value)
      if (idx >= 0) jobs.value[idx] = saved
      syncLocalJob(saved)
      ElMessage.success(saved.txHash ? '岗位已更新，并已写入链上归档' : '岗位已更新')
    } else {
      const res = await api.post('/data/jobs', payload)
      const saved = res.data || { ...payload, id: Date.now(), count: 0 }
      jobs.value.push(saved)
      syncLocalJob(saved)
      ElMessage.success(saved.txHash ? '岗位已发布，并已写入链上归档' : '岗位已发布')
    }
    dialogVisible.value = false
  } catch (e) {
    ElMessage.error(getApiError(e, '岗位保存失败'))
  } finally {
    saving.value = false
  }
}

const handleCmd = async (cmd, row) => {
  if (cmd === 'toggle') {
    if (qualificationStatus.value !== 'APPROVED') {
      ElMessage.warning('企业资质通过平台审核后才能上架岗位')
      return
    }
    const newStatus = row.status === 'OPEN' ? 'CLOSED' : 'OPEN'
    try {
      const res = await api.put('/data/jobs/' + row.id, { status: newStatus })
      Object.assign(row, res.data || { status: newStatus })
      syncLocalJob(row)
      ElMessage.success(newStatus === 'OPEN' ? '已上架' : '已下架')
    } catch (e) {
      ElMessage.error(getApiError(e, '状态更新失败'))
    }
  } else if (cmd === 'applicants') {
    try {
      const res = await api.get('/data/applications', { params: { companyId: myCompanyId.value } })
      applications.value = res.data || []
    } catch (e) {
      ElMessage.error(getApiError(e, '投递数据加载失败'))
      applications.value = []
    }
    const apps = applications.value.filter(a => a.jobId === row.id)
    applicantList.value = apps.map(a => ({ name: a.name || '', school: a.school || '', date: a.applyDate || '' }))
    applicantsVisible.value = true
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('确定删除该岗位？', '提示', { type: 'warning' }).then(async () => {
      try {
        await api.delete('/data/jobs/' + row.id)
      } catch (e) {
        ElMessage.error(getApiError(e, '删除失败'))
        return
      }
      jobs.value = jobs.value.filter(j => j.id !== row.id)
      ElMessage.success('已删除')
    }).catch(() => {})
  }
}

const getApiError = (error, fallback) => {
  const data = error?.response?.data
  return data?.message || data?.error || (typeof data === 'string' ? data : '') || fallback
}
</script>

<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; gap:16px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; margin:0; }
.page-head p { margin:6px 0 0; color:#64748B; font-size:13px; }
.btn-publish { border-radius:10px !important; padding:10px 22px !important; font-weight:600; box-shadow:0 10px 22px rgba(37,99,235,0.22); }
.table-card { background:rgba(255,255,255,0.92); border:1px solid rgba(226,232,240,0.86); border-radius:14px; padding:20px; box-shadow:0 18px 45px rgba(15,23,42,0.06); }
.cell-title { font-weight:600; color:#334155; }
.cell-num { font-weight:700; color:#2563EB; }
.status-tag { display:inline-block; padding:3px 10px; border-radius:999px; font-size:12px; font-weight:600; }
.tag-open { background:#ECFDF5; color:#059669; }
.tag-closed { background:#F1F5F9; color:#64748B; }
.op-row { display:flex; align-items:center; gap:6px; justify-content:center; }
.btn-edit { color:#64748B !important; font-size:13px; }
.btn-edit:hover { color:#2563EB !important; }
.action-more { cursor:pointer; padding:5px 8px; border-radius:8px; display:inline-flex; align-items:center; }
.action-more:hover { background:#F1F5F9 !important; }
.table-footer { margin-top:12px; text-align:right; }
.table-total { font-size:12px; color:#94A3B8; }

:global(.job-dialog) { border-radius:22px; overflow:hidden; background:rgba(255,255,255,0.86); backdrop-filter:blur(24px); box-shadow:0 28px 90px rgba(15,23,42,0.24); }
:global(.job-dialog .el-dialog__header) { padding:0; margin:0; }
:global(.job-dialog .el-dialog__body) { padding:24px 26px 10px; background:linear-gradient(180deg, rgba(248,250,252,0.85), rgba(255,255,255,0.96)); }
:global(.job-dialog .el-dialog__footer) { padding:0 26px 24px; background:rgba(255,255,255,0.96); }
.dialog-hero { min-height:138px; padding:26px; color:#fff; display:flex; justify-content:space-between; align-items:center; background:linear-gradient(135deg,#0F172A 0%,#164E63 55%,#2563EB 100%); position:relative; overflow:hidden; }
.dialog-hero::after { content:''; position:absolute; right:78px; top:-70px; width:210px; height:210px; border:1px solid rgba(255,255,255,0.18); border-radius:50%; }
.dialog-kicker { display:inline-flex; padding:5px 10px; border-radius:999px; background:rgba(255,255,255,0.14); font-size:12px; letter-spacing:0; }
.dialog-hero h2 { margin:12px 0 6px; font-size:24px; font-weight:800; letter-spacing:0; }
.dialog-hero p { margin:0; color:rgba(255,255,255,0.76); font-size:13px; }
.dialog-orbit { width:62px; height:62px; border-radius:18px; background:rgba(255,255,255,0.16); display:flex; align-items:center; justify-content:center; position:relative; z-index:1; }
.dialog-orbit .el-icon { font-size:30px; }
.job-form :deep(.el-form-item__label) { color:#475569; font-weight:700; font-size:12px; padding-bottom:7px; }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:14px 16px; }
.span-2 { grid-column:span 2; }
.status-switch { width:100%; display:flex; }
.status-switch :deep(.el-radio-button) { flex:1; }
.status-switch :deep(.el-radio-button__inner) { width:100%; }
.dialog-footer { display:flex; justify-content:space-between; align-items:center; gap:14px; border-top:1px solid #E2E8F0; padding-top:18px; }
.chain-note { font-size:12px; color:#64748B; }
@media (max-width: 760px) {
  .page-head { align-items:flex-start; flex-direction:column; }
  .form-grid { grid-template-columns:1fr; }
  .span-2 { grid-column:span 1; }
  .dialog-footer { align-items:flex-start; flex-direction:column; }
}
</style>
