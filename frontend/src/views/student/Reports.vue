<template>
  <div class="page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Daily Report</p>
        <h1>日常填报</h1>
      </div>
      <el-button type="primary" :loading="loading" @click="openCreate">新建日报</el-button>
    </div>

    <div class="table-card">
      <el-table v-loading="loading" :data="reports" style="width:100%" empty-text="暂无日报记录">
        <el-table-column prop="date" label="日期" width="115" />
        <el-table-column prop="enterpriseName" label="企业" min-width="180" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="hours" label="工时(h)" width="90" align="center">
          <template #default="{ row }">{{ row.hours ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95" align="center">
          <template #default="{ row }">
            <span class="st" :class="row.submitted ? 'st-ok' : 'st-pend'">{{ row.submitted ? '已提交' : '草稿' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewReport(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer"><span class="table-total">共 {{ reports.length }} 条</span></div>
    </div>

    <el-dialog v-model="showDialog" title="新建日报" width="560px">
      <el-form :model="form" label-width="78px">
        <el-form-item label="实习备案">
          <el-select v-model="form.internshipId" placeholder="选择在岗备案" style="width:100%">
            <el-option
              v-for="item in activeInternships"
              :key="item.id"
              :label="`${item.enterpriseName || '企业'} / ${item.position || '岗位'}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="姓名">
          <el-input :model-value="userStore.userInfo?.realName || userStore.userInfo?.username || ''" disabled />
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="form.date" type="date" style="width:100%" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="今日工作概要" />
        </el-form-item>
        <el-form-item label="工时">
          <el-input-number v-model="form.hours" :min="0" :max="24" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="填写今日完成的工作、问题和收获" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitForm">提交</el-button>
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
const reports = ref([])
const internships = ref([])
const form = reactive({ internshipId: null, date: null, title: '', hours: 8, content: '' })

const studentId = computed(() => userStore.userInfo?.userId)
const activeInternships = computed(() => internships.value.filter(item => item.status === 'ACTIVE'))

const dateText = (value) => {
  if (!value) return new Date().toISOString().slice(0, 10)
  return value instanceof Date ? value.toISOString().slice(0, 10) : value
}

const loadData = async () => {
  if (!studentId.value) return
  loading.value = true
  try {
    const [reportRes, internRes] = await Promise.all([
      api.get('/data/reports', { params: { studentId: studentId.value } }),
      api.get('/data/internships', { params: { studentId: studentId.value } })
    ])
    reports.value = reportRes.data || []
    internships.value = internRes.data || []
  } catch (error) {
    if (!userStore.isLoggedIn || userStore.isLoggingOut) return
    ElMessage.error(error?.response?.data?.message || '日报数据加载失败')
  } finally {
    loading.value = false
  }
}

const openCreate = async () => {
  await loadData()
  if (!activeInternships.value.length) {
    ElMessage.warning('请先完成实习备案，且备案状态为在岗后再提交日报')
    return
  }
  Object.assign(form, {
    internshipId: activeInternships.value[0].id,
    date: new Date(),
    title: '',
    hours: 8,
    content: ''
  })
  showDialog.value = true
}

const submitForm = async () => {
  if (!form.internshipId) {
    ElMessage.warning('请选择实习备案')
    return
  }
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  saving.value = true
  try {
    const res = await api.post('/data/reports', {
      studentId: studentId.value,
      internshipId: form.internshipId,
      date: dateText(form.date),
      title: form.title,
      hours: form.hours,
      content: form.content,
      submitted: true
    })
    reports.value.unshift(res.data)
    showDialog.value = false
    ElMessage.success('日报已提交并关联到实习备案')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '日报提交失败')
  } finally {
    saving.value = false
  }
}

const viewReport = (row) => {
  ElMessageBox.alert(
    `日期：${row.date || ''}\n企业：${row.enterpriseName || '-'}\n标题：${row.title || ''}\n工时：${row.hours ?? '-'} 小时\n\n${row.content || '暂无内容'}`,
    '日报详情',
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
.st-ok { background:#ecfdf5; color:#047857; }
.st-pend { background:#fff7ed; color:#c2410c; }
.table-total { color:#64748b; font-size:13px; }
</style>
