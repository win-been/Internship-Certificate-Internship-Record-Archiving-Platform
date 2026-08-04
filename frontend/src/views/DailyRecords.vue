<template>
  <div class="page">
    <div class="page-head"><h1>实习记录</h1><el-button type="primary" @click="showDialog=true" :icon="Plus" style="background:#06b6d4;border-color:#06b6d4">写记录</el-button></div>

    <el-table :data="list" v-loading="loading" empty-text="暂无记录" class="fancy-table" row-class-name="anim-row">
      <el-table-column prop="id" label="ID" width="60"/>
      <el-table-column prop="recordDate" label="日期" width="110"/>
      <el-table-column prop="content" label="内容" show-overflow-tooltip/>
      <el-table-column prop="workHours" label="工时" width="70"/>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{row}"><el-tag size="small" :type="row.status==='APPROVED'?'success':row.status==='REJECTED'?'danger':'warning'">{{ row.status }}</el-tag></template>
      </el-table-column>
      <el-table-column label="链上存证" width="220">
        <template #default="{row}"><ChainBadge :tx-hash="row.blockchainTxHash" :index="row.recordIndex" /></template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" title="写实习记录" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="日期"><el-date-picker v-model="form.recordDate" type="date" style="width:100%"/></el-form-item>
        <el-form-item label="工时"><el-input-number v-model="form.workHours" :min="0" :max="24" style="width:100%"/></el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="5" placeholder="今天做了什么..."/></el-form-item>
        <el-form-item label="完成任务"><el-input v-model="form.taskCompleted" placeholder="完成了哪些任务"/></el-form-item>
        <el-form-item label="学习收获"><el-input v-model="form.learningPoints" type="textarea" :rows="2"/></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog=false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="saving">提交并上链</el-button>
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
const form = ref({ recordDate:null, content:'', workHours:8, taskCompleted:'', learningPoints:'' })

const load = async () => {
  loading.value = true
  try {
    const uid = userStore.userInfo.userId
    const res = await api.get(`/daily-records/student/${uid}?size=50`)
    list.value = res.data || []
  } catch(e){ console.error(e) }
  finally { loading.value = false }
}

const handleCreate = async () => {
  saving.value = true
  try {
    const uid = userStore.userInfo.userId
    await api.post('/daily-records', {
      ...form.value,
      internshipId: 1,
      studentId: uid,
      recordDate: form.value.recordDate ? form.value.recordDate.toISOString().split('T')[0] : ''
    })
    ElMessage.success('记录已提交并上链')
    showDialog.value = false
    load()
  } catch(e){ ElMessage.error('提交失败') }
  finally { saving.value = false }
}

onMounted(load)
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
.fancy-table .anim-row { transition: background-color .18s ease, transform .18s ease; }
.fancy-table .anim-row:hover td { background-color: rgba(6, 182, 212, .07) !important; }
.fancy-table .el-table__row td { transition: background-color .18s ease; }
</style>