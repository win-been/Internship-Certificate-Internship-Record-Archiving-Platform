<template>
  <div class="page">
    <div class="page-head"><h1>实习信息</h1><el-button type="primary" @click="showDialog=true" :icon="Plus" style="background:#06b6d4;border-color:#06b6d4">新建实习</el-button></div>

    <el-table :data="list" style="width:100%" v-loading="loading" empty-text="暂无实习信息" class="fancy-table" row-class-name="anim-row">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="enterpriseName" label="企业" />
      <el-table-column prop="schoolName" label="学校" />
      <el-table-column prop="position" label="岗位" />
      <el-table-column prop="startDate" label="开始" width="110" />
      <el-table-column prop="endDate" label="结束" width="110" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'info'">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column label="链上存证" width="200">
        <template #default="{row}"><ChainBadge :tx-hash="row.blockchainTxHash" /></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" title="新建实习" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="学生姓名"><el-input v-model="form.studentName"/></el-form-item>
        <el-form-item label="身份证号"><el-input v-model="form.studentIdCard"/></el-form-item>
        <el-form-item label="企业名称"><el-input v-model="form.enterpriseName"/></el-form-item>
        <el-form-item label="企业代码"><el-input v-model="form.enterpriseCode"/></el-form-item>
        <el-form-item label="学校名称"><el-input v-model="form.schoolName"/></el-form-item>
        <el-form-item label="岗位"><el-input v-model="form.position"/></el-form-item>
        <el-form-item label="部门"><el-input v-model="form.department"/></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" style="width:100%"/></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" style="width:100%"/></el-form-item></el-col>
        </el-row>
        <el-form-item label="导师"><el-input v-model="form.mentorName"/></el-form-item>
        <el-form-item label="导师电话"><el-input v-model="form.mentorPhone"/></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog=false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="saving">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/request'
import { useUserStore } from '../stores/userStore'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import ChainBadge from '../components/ChainBadge.vue'

const userStore = useUserStore()
const list = ref([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const form = ref({ studentName:'',studentIdCard:'',enterpriseName:'',enterpriseCode:'',schoolName:'',position:'',department:'',startDate:null,endDate:null,mentorName:'',mentorPhone:'',description:'' })

const load = async () => {
  const uid = userStore.userInfo.userId
  if (!uid) return
  loading.value = true
  try {
    const res = await api.get(`/internships/student/${uid}?size=50`)
    list.value = res.data || []
  } catch(e){ console.error(e) }
  finally { loading.value = false }
}

const handleCreate = async () => {
  saving.value = true
  try {
    const uid = userStore.userInfo.userId
    const body = {
      ...form.value,
      studentId: uid,
      enterpriseId: 1,
      schoolId: 1,
      startDate: form.value.startDate ? form.value.startDate.toISOString().split('T')[0] : undefined,
      endDate: form.value.endDate ? form.value.endDate.toISOString().split('T')[0] : undefined
    }
    await api.post('/internships', body)
    ElMessage.success('创建成功')
    showDialog.value = false
    load()
  } catch(e){ ElMessage.error('创建失败') }
  finally { saving.value = false }
}

onMounted(() => { if (userStore.isLoggedIn) load() })
</script>

<style scoped>
.page { width: 100%; animation: page-in .4s ease both; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-head h1 { color:#e2e8f0; font-size:22px; }

@keyframes page-in {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>

<style>
.fancy-table .anim-row:hover td { background-color: rgba(6, 182, 212, .07) !important; }
.fancy-table .el-table__row td { transition: background-color .18s ease; }
</style>