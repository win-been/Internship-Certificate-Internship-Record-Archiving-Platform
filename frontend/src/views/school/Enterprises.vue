<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>校企管控</h1>
        <span class="page-sub">企业合作名单与实习学生关联管理</span>
      </div>
      <el-button type="primary" @click="openAdd">录入企业名单</el-button>
    </div>

    <div class="stat-mini-row">
      <div class="mini-stat"><span class="mini-num" style="color:#059669">{{ summary.find(s=>s.label==='已入驻')?.value || 0 }}</span><span class="mini-label">已入驻</span></div>
      <div class="mini-stat"><span class="mini-num" style="color:#D97706">{{ summary.find(s=>s.label==='待审核')?.value || 0 }}</span><span class="mini-label">待审核</span></div>
      <div class="mini-stat"><span class="mini-num" style="color:#DC2626">{{ summary.find(s=>s.label==='已驳回')?.value || 0 }}</span><span class="mini-label">已驳回</span></div>
      <div class="mini-stat"><span class="mini-num" style="color:#2563EB">{{ summary.find(s=>s.label==='实习学生')?.value || 0 }}</span><span class="mini-label">实习学生</span></div>
    </div>

    <el-card class="content-card" shadow="never">
      <div class="card-toolbar">
        <el-input v-model="search" placeholder="搜索企业名称 / 编码..." prefix-icon="Search" clearable style="width:220px"/>
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:110px">
          <el-option label="已入驻" value="APPROVED"/>
          <el-option label="名单待注册" value="ROSTER"/>
          <el-option label="待审核" value="PENDING"/>
          <el-option label="已驳回" value="REJECTED"/>
        </el-select>
        <span class="toolbar-hint">共 {{ filteredEnterprises.length }} 家企业</span>
      </div>

      <el-table :data="filteredEnterprises" empty-text="暂无企业数据" size="default" v-loading="loading">
        <el-table-column prop="name" label="企业名称" min-width="140"/>
        <el-table-column prop="code" label="信用代码" width="180"/>
        <el-table-column prop="industry" label="行业" width="100"/>
        <el-table-column prop="scale" label="规模" width="80"/>
        <el-table-column prop="contact" label="联系人" width="90"/>
        <el-table-column label="实习人数" width="90" align="center">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="showStudents(row)">{{ getStudentCount(row) }}</el-button>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{row}">
            <span class="status-tag" :class="'tag-' + row.statusColor">{{ row.statusLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="企业详情" width="520px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="企业名称">{{ current?.name }}</el-descriptions-item>
        <el-descriptions-item label="信用代码">{{ current?.code }}</el-descriptions-item>
        <el-descriptions-item label="行业">{{ current?.industry }}</el-descriptions-item>
        <el-descriptions-item label="规模">{{ current?.scale }}</el-descriptions-item>
        <el-descriptions-item label="地址" :span="2">{{ current?.address }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ current?.contact }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ current?.phone }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ current?.email }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span class="status-tag" :class="'tag-' + current?.statusColor">{{ current?.statusLabel }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="addVisible" title="录入企业名单" width="520px">
      <el-form :model="addForm" label-width="96px">
        <el-form-item label="企业名称" required><el-input v-model.trim="addForm.name" placeholder="需与注册时填写一致"/></el-form-item>
        <el-form-item label="信用代码" required><el-input v-model.trim="addForm.code" placeholder="统一社会信用代码"/></el-form-item>
        <el-form-item label="联系人"><el-input v-model.trim="addForm.contact" placeholder="企业联系人"/></el-form-item>
        <el-form-item label="手机号"><el-input v-model.trim="addForm.phone" placeholder="联系人手机号"/></el-form-item>
        <el-form-item label="邮箱"><el-input v-model.trim="addForm.email" placeholder="可选"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible=false">取消</el-button>
        <el-button type="primary" :loading="savingAdd" @click="submitAdd">保存名单</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="studentVisible" title="实习学生" width="600px">
      <el-table :data="getStudentsOf(currentEnterprise)" empty-text="暂无实习学生" size="small">
        <el-table-column prop="studentNo" label="学号" width="100"/>
        <el-table-column prop="name" label="姓名" width="90"/>
        <el-table-column prop="major" label="专业" min-width="120"/>
        <el-table-column prop="position" label="岗位" min-width="100"/>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{row}">
            <span class="status-tag" :class="'tag-' + row.statusColor">{{ row.statusLabel }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useDataStore } from '../../stores/dataStore'
import { useUserStore } from '../../stores/userStore'
import api from '../../api/request'

const userStore = useUserStore()
const mySchoolId = computed(() => userStore.userInfo?.schoolId || userStore.userInfo?.userId || null)

const search = ref('')
const statusFilter = ref('')
const detailVisible = ref(false)
const studentVisible = ref(false)
const addVisible = ref(false)
const savingAdd = ref(false)
const current = ref(null)
const currentEnterprise = ref(null)
const loading = ref(false)
const enterprises = ref([])
const students = ref([])
const addForm = reactive({ name: '', code: '', contact: '', phone: '', email: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const [eRes, sRes] = await Promise.all([
      api.get('/data/enterprise-list'),
      api.get('/data/school-students', { params: { schoolId: mySchoolId.value } })
    ])
    enterprises.value = (eRes.data || []).map(e => ({
      ...e,
      status: e.status || 'PENDING',
      statusLabel: e.status === 'APPROVED' ? '已入驻' : e.status === 'ROSTER' ? '名单待注册' : e.status === 'REJECTED' ? '已驳回' : '平台审核中',
      statusColor: e.status === 'APPROVED' ? 'green' : e.status === 'ROSTER' ? 'blue' : e.status === 'REJECTED' ? 'red' : 'orange'
    }))
    if (enterprises.value.length === 0) {
      const ds = useDataStore()
      enterprises.value = (ds.enterpriseList && ds.enterpriseList.length > 0)
        ? ds.enterpriseList.map(e => ({
            ...e,
            status: e.status || 'PENDING',
            statusLabel: e.status === 'APPROVED' ? '已入驻' : e.status === 'ROSTER' ? '名单待注册' : e.status === 'REJECTED' ? '已驳回' : '平台审核中',
            statusColor: e.status === 'APPROVED' ? 'green' : e.status === 'ROSTER' ? 'blue' : e.status === 'REJECTED' ? 'red' : 'orange'
          }))
        : []
    }
    students.value = (sRes.data || []).map(s => ({
      ...s,
      status: s.status || 'INACTIVE',
      statusLabel: s.status === 'ACTIVE' ? '在岗' : s.status === 'GRADUATED' ? '毕业' : '离岗',
      statusColor: s.status === 'ACTIVE' ? 'green' : s.status === 'GRADUATED' ? 'gray' : 'orange'
    }))
  } catch (e) {
    console.log('Backend unavailable, using shared dataStore')
    const ds = useDataStore()
    enterprises.value = (ds.enterpriseList && ds.enterpriseList.length > 0)
      ? ds.enterpriseList.map(e => ({
          ...e,
          status: e.status || 'PENDING',
          statusLabel: e.status === 'APPROVED' ? '已入驻' : e.status === 'ROSTER' ? '名单待注册' : e.status === 'REJECTED' ? '已驳回' : '平台审核中',
          statusColor: e.status === 'APPROVED' ? 'green' : e.status === 'ROSTER' ? 'blue' : e.status === 'REJECTED' ? 'red' : 'orange'
        }))
      : []
    students.value = (ds.schoolStudents && ds.schoolStudents.length > 0)
      ? ds.schoolStudents.map(s => ({
          ...s,
          status: s.status || 'INACTIVE',
          statusLabel: s.status === 'ACTIVE' ? '在岗' : s.status === 'GRADUATED' ? '毕业' : '离岗',
          statusColor: s.status === 'ACTIVE' ? 'green' : s.status === 'GRADUATED' ? 'gray' : 'orange'
        }))
      : []
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

const filteredEnterprises = computed(() => {
  let list = enterprises.value
  if (search.value) {
    const kw = search.value.toLowerCase()
    list = list.filter(e => (e.name || '').toLowerCase().includes(kw) || (e.code || '').includes(kw))
  }
  if (statusFilter.value) list = list.filter(e => e.status === statusFilter.value)
  return list
})

const summary = computed(() => [
  { label: '已入驻', value: enterprises.value.filter(e => e.status === 'APPROVED').length, color: '#059669' },
  { label: '待审核', value: enterprises.value.filter(e => e.status === 'PENDING').length, color: '#D97706' },
  { label: '已驳回', value: enterprises.value.filter(e => e.status === 'REJECTED').length, color: '#DC2626' },
  { label: '实习学生', value: students.value.filter(s => s.status === 'ACTIVE').length, color: '#2563EB' },
])

const getStudentCount = (enterprise) =>
  enterprise ? students.value.filter(s => s.enterprise === enterprise.name).length : 0

const getStudentsOf = (enterprise) =>
  enterprise ? students.value.filter(s => s.enterprise === enterprise.name) : []

const viewDetail = (row) => { current.value = row; detailVisible.value = true }
const showStudents = (row) => { currentEnterprise.value = row; studentVisible.value = true }
const openAdd = () => {
  Object.assign(addForm, { name: '', code: '', contact: '', phone: '', email: '' })
  addVisible.value = true
}

const submitAdd = async () => {
  if (!addForm.name || !addForm.code) {
    ElMessage.warning('请填写企业名称和信用代码')
    return
  }
  savingAdd.value = true
  try {
    const res = await api.post('/data/enterprise-list', addForm)
    enterprises.value.unshift({
      ...res.data,
      statusLabel: '名单待注册',
      statusColor: 'blue'
    })
    addVisible.value = false
    ElMessage.success('企业名单已录入，企业可用该名称和信用代码自动注册')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '企业名单录入失败')
  } finally {
    savingAdd.value = false
  }
}

</script>

<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:flex-start; margin-bottom:16px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.page-sub { font-size:13px; color:#94A3B8; display:block; margin-top:2px; }
.stat-mini-row { display:flex; gap:16px; margin-bottom:16px; }
.mini-stat { flex:1; background:#FFF; border-radius:12px; padding:14px 16px; text-align:center; box-shadow:0 1px 4px rgba(0,0,0,0.04); }
.mini-num { font-size:24px; font-weight:700; display:block; }
.mini-label { font-size:12px; color:#94A3B8; margin-top:2px; display:block; }
.content-card { border-radius:14px; border:none; box-shadow:0 2px 14px rgba(0,0,0,0.05); }
.card-toolbar { display:flex; align-items:center; gap:12px; margin-bottom:16px; flex-wrap:wrap; }
.toolbar-hint { font-size:12px; color:#94A3B8; }
.status-tag { display:inline-block; padding:2px 10px; border-radius:10px; font-size:12px; font-weight:500; }
.tag-green { background:#ECFDF5; color:#059669; }
.tag-orange { background:#FFF7ED; color:#EA580C; }
.tag-red { background:#FEF2F2; color:#DC2626; }
.tag-blue { background:#EFF6FF; color:#2563EB; }
.tag-gray { background:#F1F5F9; color:#64748B; }
</style>
