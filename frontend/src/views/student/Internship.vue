<template>
  <div class="page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Internship Filing</p>
        <h1>实习备案</h1>
      </div>
      <el-button type="primary" :loading="loading" @click="openCreate">新建备案</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="internships" style="width:100%" empty-text="暂无实习备案">
        <el-table-column prop="enterpriseName" label="企业名称" min-width="180" />
        <el-table-column prop="schoolName" label="学校名称" min-width="150" />
        <el-table-column prop="position" label="岗位" min-width="150" />
        <el-table-column prop="startDate" label="开始日期" width="115" />
        <el-table-column prop="endDate" label="结束日期" width="115">
          <template #default="{ row }">{{ row.endDate || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="95" align="center">
          <template #default="{ row }">
            <span class="st" :class="row.status === 'ACTIVE' ? 'st-active' : 'st-done'">{{ row.status === 'ACTIVE' ? '在岗' : '已结束' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="链上交易" min-width="170">
          <template #default="{ row }">{{ row.blockchainTxHash || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><span class="table-total">共 {{ internships.length }} 条</span></div>
    </div>

    <el-dialog v-model="showDialog" title="新建实习备案" width="620px">
      <el-form :model="form" label-width="92px">
        <el-form-item label="录用岗位">
          <el-select v-model="form.applicationId" filterable placeholder="选择企业已录用的岗位" style="width:100%" @change="fillByApplication">
            <el-option
              v-for="app in acceptedApplications"
              :key="app.id"
              :label="`${app.company || '企业'} / ${app.jobTitle || '岗位'}`"
              :value="app.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="企业名称">
          <el-input :model-value="selectedApplication?.company || ''" disabled />
        </el-form-item>
        <el-form-item label="岗位">
          <el-input v-model="form.position" placeholder="默认使用录用岗位名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期">
              <el-date-picker v-model="form.startDate" type="date" style="width:100%" placeholder="选择日期" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期">
              <el-date-picker v-model="form.endDate" type="date" style="width:100%" placeholder="可暂不填写" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="部门">
          <el-input v-model="form.department" placeholder="请输入实习部门" />
        </el-form-item>
        <el-form-item label="导师">
          <el-input v-model="form.mentorName" placeholder="请输入企业导师姓名" />
        </el-form-item>
        <el-form-item label="导师电话">
          <el-input v-model="form.mentorPhone" placeholder="请输入企业导师电话" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="填写备案说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">提交备案</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../../api/request'
import { useUserStore } from '../../stores/userStore'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const internships = ref([])
const applications = ref([])
const form = reactive({
  applicationId: null,
  position: '',
  startDate: null,
  endDate: null,
  department: '',
  mentorName: '',
  mentorPhone: '',
  description: ''
})

const studentId = computed(() => userStore.userInfo?.userId)
const acceptedApplications = computed(() => applications.value.filter(app => app.status === 'accepted'))
const selectedApplication = computed(() => acceptedApplications.value.find(app => app.id === form.applicationId))

const dateText = (value) => {
  if (!value) return null
  return value instanceof Date ? value.toISOString().slice(0, 10) : value
}

const resetForm = () => {
  Object.assign(form, {
    applicationId: null,
    position: '',
    startDate: new Date(),
    endDate: null,
    department: '',
    mentorName: '',
    mentorPhone: '',
    description: ''
  })
}

const loadData = async () => {
  if (!studentId.value) return
  loading.value = true
  try {
    const [internRes, appRes] = await Promise.all([
      api.get('/data/internships', { params: { studentId: studentId.value } }),
      api.get('/data/applications', { params: { studentId: studentId.value } })
    ])
    internships.value = internRes.data || []
    applications.value = appRes.data || []
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '实习备案加载失败')
  } finally {
    loading.value = false
  }
}

const openCreate = async () => {
  await loadData()
  if (!acceptedApplications.value.length) {
    ElMessage.warning('还没有企业录用记录，不能新建实习备案')
    return
  }
  resetForm()
  form.applicationId = acceptedApplications.value[0].id
  fillByApplication()
  showDialog.value = true
}

const fillByApplication = () => {
  if (!selectedApplication.value) return
  form.position = selectedApplication.value.jobTitle || ''
}

const handleCreate = async () => {
  const app = selectedApplication.value
  if (!app) {
    ElMessage.warning('请选择已录用岗位')
    return
  }
  saving.value = true
  try {
    const res = await api.post('/data/internships', {
      studentId: studentId.value,
      enterpriseId: app.companyId,
      companyId: app.companyId,
      schoolId: userStore.userInfo?.schoolId,
      schoolName: app.school || userStore.userInfo?.organizationName,
      position: form.position || app.jobTitle,
      startDate: dateText(form.startDate),
      endDate: dateText(form.endDate),
      department: form.department,
      mentorName: form.mentorName,
      mentorPhone: form.mentorPhone,
      description: form.description
    })
    internships.value.unshift(res.data)
    showDialog.value = false
    ElMessage.success(res.data?.blockchainTxHash ? '备案已提交并完成上链' : '备案已提交，链上同步待确认')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '备案提交失败')
  } finally {
    saving.value = false
  }
}

const viewDetail = (row) => {
  ElMessageBox.alert(
    `企业：${row.enterpriseName || '-'}\n岗位：${row.position || '-'}\n开始：${row.startDate || '-'}\n结束：${row.endDate || '-'}\n导师：${row.mentorName || '-'}\n链上交易：${row.blockchainTxHash || '-'}`,
    '备案详情',
    { confirmButtonText: '关闭' }
  ).catch(() => {})
}

onMounted(loadData)
</script>

<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:16px; }
.eyebrow { margin:0 0 5px; font-size:12px; color:#64748b; letter-spacing:0; }
.page-head h1 { margin:0; font-size:24px; font-weight:800; color:#111827; }
.st { display:inline-block; padding:3px 10px; border-radius:999px; font-size:12px; font-weight:700; }
.st-active { background:#ecfdf5; color:#047857; }
.st-done { background:#f1f5f9; color:#64748b; }
.table-total { color:#64748b; font-size:13px; }
</style>
