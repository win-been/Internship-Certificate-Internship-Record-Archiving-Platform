<template>
  <div class="dashboard">
    <h1 class="page-title">仪表板</h1>
    <div class="stats-grid">
      <div class="stat-card" v-for="s in stats" :key="s.label">
        <div class="stat-icon" :style="{background:s.color}"><el-icon :size="22"><component :is="s.icon" /></el-icon></div>
        <div class="stat-body"><span class="stat-num">{{ s.value }}</span><span class="stat-label">{{ s.label }}</span></div>
      </div>
    </div>
    <div class="cards-row">
      <div class="panel">
        <h3>最近实习</h3>
        <el-table :data="internships" style="width:100%" size="small" v-if="internships.length">
          <el-table-column prop="enterpriseName" label="企业" />
          <el-table-column prop="position" label="岗位" />
          <el-table-column prop="startDate" label="开始" width="110" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{row}"><el-tag size="small" :type="row.status==='ACTIVE'?'success':'info'">{{ row.status }}</el-tag></template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无数据" />
      </div>
      <div class="panel">
        <h3>最近记录</h3>
        <el-table :data="records" style="width:100%" size="small" v-if="records.length">
          <el-table-column prop="recordDate" label="日期" width="110" />
          <el-table-column prop="content" label="内容" show-overflow-tooltip />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{row}"><el-tag size="small" :type="row.status==='APPROVED'?'success':row.status==='REJECTED'?'danger':'warning'">{{ row.status }}</el-tag></template>
          </el-table-column>
        </el-table>
        <el-empty v-else description="暂无数据" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api/request'
import { useUserStore } from '../stores/userStore'

const userStore = useUserStore()
const stats = ref([
  { label: '实习信息', value: 0, icon: 'Briefcase', color: '#0f766e' },
  { label: '实习记录', value: 0, icon: 'Notebook', color: '#0369a1' },
  { label: '实习证明', value: 0, icon: 'Stamp', color: '#7c3aed' },
  { label: '已上链', value: 0, icon: 'Link', color: '#059669' }
])
const internships = ref([])
const records = ref([])

onMounted(async () => {
  try {
    const uid = userStore.userInfo.userId
    const [iRes, rRes] = await Promise.all([
      api.get(`/internships/student/${uid}?size=5`),
      api.get(`/daily-records/student/${uid}?size=5`)
    ])
    internships.value = iRes.data || []
    records.value = rRes.data || []
    stats.value[0].value = iRes.data.length || 0
    stats.value[1].value = rRes.data.length || 0
  } catch (e) { console.error(e) }
})
</script>

<style scoped>
.dashboard { width: 100%; }
.page-title { color: #e2e8f0; font-size: 22px; margin-bottom: 20px; }

.stats-grid { display: grid; grid-template-columns: repeat(4,1fr); gap: 16px; margin-bottom: 24px; }
.stat-card { background: #1e293b; border:1px solid #334155; border-radius:12px; padding:20px; display:flex; align-items:center; gap:14px; }
.stat-icon { width:44px;height:44px;border-radius:10px;display:flex;align-items:center;justify-content:center;color:#fff; }
.stat-num { font-size:26px;font-weight:700;color:#e2e8f0;display:block; }
.stat-label { font-size:13px;color:#64748b; }

.cards-row { display:grid; grid-template-columns:1fr 1fr; gap:20px; }
.panel { background:#1e293b; border:1px solid #334155; border-radius:12px; padding:20px; }
.panel h3 { color:#e2e8f0; font-size:15px; margin-bottom:14px; }
</style>