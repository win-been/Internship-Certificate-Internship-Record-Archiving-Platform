<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>学生名单录入</h1>
        <span class="page-sub">学生注册前先在这里录入本校名单，注册时按所属学校和身份证号自动核验</span>
      </div>
      <el-button type="primary" @click="showAdd=true">录入学生名单</el-button>
    </div>

    <div class="stat-mini-row">
      <div class="mini-stat"><span class="mini-num" style="color:#059669">{{ summary.active }}</span><span class="mini-label">在岗</span></div>
      <div class="mini-stat"><span class="mini-num" style="color:#EA580C">{{ summary.inactive }}</span><span class="mini-label">离岗</span></div>
      <div class="mini-stat"><span class="mini-num" style="color:#64748B">{{ summary.graduated }}</span><span class="mini-label">毕业</span></div>
      <div class="mini-stat"><span class="mini-num" style="color:#2563EB">{{ summary.total }}</span><span class="mini-label">总计</span></div>
    </div>

    <el-card class="content-card" shadow="never">
      <div class="card-toolbar">
        <el-input v-model="search" placeholder="搜索姓名 / 学号 / 身份证后四位..." prefix-icon="Search" clearable style="width:260px"/>
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:110px">
          <el-option label="在岗" value="ACTIVE"/>
          <el-option label="离岗" value="INACTIVE"/>
          <el-option label="毕业" value="GRADUATED"/>
        </el-select>
        <span class="toolbar-hint">共 {{ filteredStudents.length }} 名学生</span>
      </div>

      <el-table :data="filteredStudents" empty-text="暂无学生数据" size="default" v-loading="loading">
        <el-table-column prop="studentNo" label="学号" width="120"/>
        <el-table-column prop="name" label="姓名" width="90"/>
        <el-table-column label="身份证号" width="150">
          <template #default="{row}">{{ maskIdCard(row.idCard) }}</template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="120"/>
        <el-table-column label="账号状态" width="110" align="center">
          <template #default="{row}">
            <span class="status-tag" :class="accountStatusClass(row.accountStatus)">{{ row.accountStatusLabel || '名单未注册' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="enterprise" label="实习企业" min-width="130"/>
        <el-table-column prop="position" label="实习岗位" min-width="120"/>
        <el-table-column prop="startDate" label="开始日期" width="110" align="center"/>
        <el-table-column label="状态" width="80" align="center">
          <template #default="{row}">
            <span class="status-tag" :class="'tag-' + row.statusColor">{{ row.statusLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status==='ACTIVE'" link type="danger" size="small" @click="toggle(row)">离岗</el-button>
            <el-button v-else link type="success" size="small" @click="toggle(row)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="detailVisible" title="学生详情" width="500px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="学号">{{ current?.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="姓名">{{ current?.name }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ maskIdCard(current?.idCard) }}</el-descriptions-item>
        <el-descriptions-item label="账号状态">{{ current?.accountStatusLabel || '名单未注册' }}</el-descriptions-item>
        <el-descriptions-item label="专业">{{ current?.major }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ current?.userId }}</el-descriptions-item>
        <el-descriptions-item label="实习企业">{{ current?.enterprise }}</el-descriptions-item>
        <el-descriptions-item label="实习岗位">{{ current?.position }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ current?.startDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <span class="status-tag" :class="'tag-' + current?.statusColor">{{ current?.statusLabel }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <el-dialog v-model="showAdd" title="录入学生名单" width="460px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="学号"><el-input v-model.trim="form.studentNo" placeholder="2021006，可不填"/></el-form-item>
        <el-form-item label="姓名"><el-input v-model.trim="form.name" placeholder="学生真实姓名"/></el-form-item>
        <el-form-item label="身份证"><el-input v-model.trim="form.idCard" placeholder="学生注册时必须与这里一致"/></el-form-item>
        <el-form-item label="专业"><el-input v-model="form.major" placeholder="请输入专业"/></el-form-item>
        <el-form-item label="企业"><el-input v-model="form.enterprise" placeholder="实习企业"/></el-form-item>
        <el-form-item label="岗位"><el-input v-model="form.position" placeholder="实习岗位"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAdd=false">取消</el-button>
        <el-button type="primary" @click="doAdd">确认录入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../stores/userStore'
import api from '../../api/request'

const userStore = useUserStore()
const mySchoolId = computed(() => userStore.userInfo?.schoolId || userStore.userInfo?.userId || null)

const search = ref('')
const statusFilter = ref('')
const showAdd = ref(false)
const detailVisible = ref(false)
const current = ref(null)
const loading = ref(false)
const allStudents = ref([])
const form = reactive({ studentNo: '', name: '', idCard: '', major: '', enterprise: '', position: '' })

const fetchStudents = async () => {
  if (!mySchoolId.value) {
    allStudents.value = []
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error('请先使用学校账号登录')
    return
  }
  loading.value = true
  try {
    const res = await api.get('/data/school-students', { params: { schoolId: mySchoolId.value } })
    allStudents.value = (res.data || []).map(s => ({
      ...s,
      status: s.status || 'INACTIVE',
      statusLabel: s.status === 'ACTIVE' ? '在岗' : s.status === 'GRADUATED' ? '毕业' : '离岗',
      statusColor: s.status === 'ACTIVE' ? 'green' : s.status === 'GRADUATED' ? 'gray' : 'orange'
    }))
  } catch (e) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error('学生数据加载失败')
    allStudents.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchStudents)

const filteredStudents = computed(() => {
  let list = allStudents.value
  if (search.value) {
    const kw = search.value.toLowerCase()
    list = list.filter(s =>
      (s.name || '').toLowerCase().includes(kw) ||
      (s.studentNo || '').includes(kw) ||
      (s.idCard || '').includes(kw)
    )
  }
  if (statusFilter.value) list = list.filter(s => s.status === statusFilter.value)
  return list
})

const summary = computed(() => ({
  active: allStudents.value.filter(s => s.status === 'ACTIVE').length,
  inactive: allStudents.value.filter(s => s.status === 'INACTIVE').length,
  graduated: allStudents.value.filter(s => s.status === 'GRADUATED').length,
  total: allStudents.value.length,
}))

const maskIdCard = (idCard) => {
  const text = String(idCard || '').trim()
  if (!text) return '未录入'
  return text.replace(/^(.{4}).+(.{4})$/, '$1**********$2')
}

const accountStatusClass = (status) => {
  if (status === 'APPROVED') return 'tag-green'
  if (status === 'ROSTER') return 'tag-blue'
  if (status === 'REJECTED') return 'tag-red'
  return 'tag-orange'
}

const openDetail = (row) => { current.value = row; detailVisible.value = true }

const toggle = async (row) => {
  const targetStatus = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  const action = targetStatus === 'INACTIVE' ? '离岗' : '恢复'
  try {
    await ElMessageBox.confirm('确定将' + row.name + '标记为' + action + '？', '状态变更', { type: 'warning' })
    const res = await api.put('/data/school-students/' + row.studentNo + '/status', { status: targetStatus })
    Object.assign(row, res.data || {
      status: targetStatus,
      statusLabel: targetStatus === 'ACTIVE' ? '在岗' : '离岗',
      statusColor: targetStatus === 'ACTIVE' ? 'green' : 'orange'
    })
    ElMessage.success(row.name + ' 已' + action)
  } catch (e) { /* cancelled */ }
}

const doAdd = async () => {
  if (!mySchoolId.value) { ElMessage.warning('请先使用学校账号登录'); return }
  if (!form.name) { ElMessage.warning('请输入学生真实姓名'); return }
  if (!/^\d{17}[\dXx]$/.test(form.idCard)) { ElMessage.warning('请输入18位身份证号，末位可为X'); return }
  const newStudent = {
    studentNo: form.studentNo || String(allStudents.value.length + 2021001),
    name: form.name,
    idCard: form.idCard || '',
    major: form.major || '未填写',
    enterprise: form.enterprise || '-',
    position: form.position || '-',
    startDate: new Date().toISOString().slice(0, 10),
    schoolId: mySchoolId.value,
    status: 'INACTIVE',
    statusLabel: '离岗',
    statusColor: 'orange'
  }
  try {
    const res = await api.post('/data/school-students', newStudent)
    allStudents.value.push({ ...newStudent, ...(res.data || {}) })
    ElMessage.success(form.name + ' 已录入学生名单，可使用所选学校和身份证号注册')
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '学生名单添加失败')
    return
  }
  showAdd.value = false
  Object.assign(form, { studentNo: '', name: '', idCard: '', major: '', enterprise: '', position: '' })
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
.tag-blue { background:#EFF6FF; color:#2563EB; }
.tag-orange { background:#FFF7ED; color:#EA580C; }
.tag-red { background:#FEF2F2; color:#DC2626; }
.tag-gray { background:#F1F5F9; color:#64748B; }
</style>
