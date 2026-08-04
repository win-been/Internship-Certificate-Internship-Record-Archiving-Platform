<template>
  <div class="page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Review Center</p>
        <h1>{{ pageTitle }}</h1>
      </div>
      <div class="head-actions">
        <el-segmented v-model="statusFilter" :options="filterOptions" />
        <el-button :loading="loading" @click="loadApprovals">刷新</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="visibleApprovals" style="width: 100%" empty-text="暂无审核记录">
        <el-table-column label="类型" width="120">
          <template #default="{ row }">{{ typeLabel(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="name" label="名称 / 姓名" min-width="150" />
        <el-table-column label="编号" min-width="170">
          <template #default="{ row }">{{ maskCode(row) }}</template>
        </el-table-column>
        <el-table-column :label="isSchoolReviewer ? '所属学校' : '企业联系人'" min-width="170">
          <template #default="{ row }">{{ row.school || row.contact || '-' }}</template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="130">
          <template #default="{ row }">{{ row.major || '-' }}</template>
        </el-table-column>
        <el-table-column prop="date" label="申请时间" width="120" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <span class="st" :class="statusClass(row.status)">{{ statusLabel(row.status) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button class="btn-sm btn-green" size="small" :loading="row._busy" @click="review(row, 'APPROVED')">通过</el-button>
              <el-button class="btn-sm btn-gray" size="small" :loading="row._busy" @click="review(row, 'REJECTED')">驳回</el-button>
            </template>
            <span v-else class="done-text">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../../api/request'
import { useUserStore } from '../../stores/userStore'

const loading = ref(false)
const approvals = ref([])
const statusFilter = ref('全部')
const userStore = useUserStore()
const isSchoolReviewer = computed(() => userStore.userInfo?.role === 'SCHOOL_ADMIN')
const pageTitle = computed(() => isSchoolReviewer.value ? '本校学生实名认证审核' : '企业资质审核')
const filterOptions = ['全部', '待审核', '已通过', '已驳回']

const visibleApprovals = computed(() => {
  const statusMap = { 待审核: 'PENDING', 已通过: 'APPROVED', 已驳回: 'REJECTED' }
  const status = statusMap[statusFilter.value]
  return status ? approvals.value.filter(item => item.status === status) : approvals.value
})

const typeLabel = (type) => {
  if (type === 'IDENTITY_VERIFICATION') return '实名认证'
  if (type && type.includes('ENTERPRISE')) return '企业资质'
  return type || '审核'
}

const statusLabel = (status) => {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' }
  return map[status] || status || '-'
}

const statusClass = (status) => {
  if (status === 'PENDING') return 'st-pend'
  if (status === 'APPROVED') return 'st-ok'
  if (status === 'REJECTED') return 'st-reject'
  return 'st-info'
}

const maskCode = (row) => {
  const code = row.code || ''
  if (row.type === 'IDENTITY_VERIFICATION' && code.length >= 8) {
    return code.slice(0, 4) + '**********' + code.slice(-4)
  }
  return code || '-'
}

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

const isTransientLoadError = (error) => {
  const status = error?.response?.status
  return !status || [500, 502, 503, 504].includes(status)
}

const fetchApprovals = async (url, retries = 0) => {
  try {
    return await api.get(url)
  } catch (error) {
    if (retries > 0 && isTransientLoadError(error)) {
      await sleep(800)
      return fetchApprovals(url, retries - 1)
    }
    throw error
  }
}

const loadApprovals = async (options = {}) => {
  loading.value = true
  try {
    const url = isSchoolReviewer.value ? '/data/approvals/school' : '/data/approvals'
    const res = await fetchApprovals(url, options.initialLoad ? 8 : 0)
    approvals.value = (res.data || []).map(item => ({ ...item, _busy: false }))
  } catch (error) {
    approvals.value = []
    ElMessage.error(error?.response?.data?.message || '审核数据加载失败')
  } finally {
    loading.value = false
  }
}

const review = async (row, status) => {
  row._busy = true
  try {
    const res = await api.put('/data/approvals/' + row.id, { status })
    Object.assign(row, res.data || {}, { _busy: false })
    ElMessage.success((row.name || '审核记录') + (status === 'APPROVED' ? ' 已通过' : ' 已驳回'))
  } catch (error) {
    row._busy = false
    ElMessage.error(error?.response?.data?.message || '审核处理失败')
  }
}

onMounted(() => loadApprovals({ initialLoad: true }))
</script>

<style scoped>
.page { width: 100%; }
.page-head { display: flex; justify-content: space-between; gap: 16px; align-items: center; margin-bottom: 16px; }
.head-actions { display: flex; align-items: center; gap: 10px; }
.eyebrow { margin: 0 0 5px; font-size: 12px; color: #64748b; letter-spacing: 0; }
.page-head h1 { margin: 0; font-size: 24px; font-weight: 800; color: #111827; }
.st { display: inline-block; padding: 3px 10px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.st-pend { background: #fff7ed; color: #c2410c; }
.st-ok { background: #ecfdf5; color: #047857; }
.st-reject { background: #fef2f2; color: #b91c1c; }
.st-info { background: #eff6ff; color: #1d4ed8; }
.btn-sm { padding: 3px 10px !important; font-size: 12px !important; border-radius: 6px !important; }
.btn-green { background: #ecfdf5 !important; border: 1px solid #a7f3d0 !important; color: #047857 !important; }
.btn-gray { background: #f8fafc !important; border: 1px solid #e2e8f0 !important; color: #64748b !important; }
.btn-gray:hover { background: #fef2f2 !important; color: #dc2626 !important; border-color: #fecaca !important; }
.done-text { font-size: 12px; color: #64748b; }
@media (max-width: 760px) {
  .page-head { align-items: flex-start; flex-direction: column; }
  .head-actions { width: 100%; justify-content: space-between; }
}
</style>
