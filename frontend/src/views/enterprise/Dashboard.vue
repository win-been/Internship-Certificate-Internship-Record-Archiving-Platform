<template>
  <div class="dashboard" :class="{ 'is-switching': switching }">
    <!-- 顶部 -->
    <div class="dash-top">
      <div>
        <h1 class="dash-title">数据看板</h1>
        <span class="dash-updated">最后更新：{{ now }}</span>
      </div>
    </div>

    <!-- 6 统计卡片 -->
    <div class="stat-grid">
      <div v-for="(s, i) in stats" :key="s.label" class="stat-card" :class="{ 'is-blue': s.blue, 'is-clickable': s.to }" :style="{ animationDelay: (i * 0.05) + 's' }" @click="openStat(s)">
        <div class="sc-top">
          <div class="sc-icon" :style="{ background: s.blue ? 'rgba(255,255,255,0.2)' : s.bg }">
            <el-icon :size="18" :color="s.blue ? '#FFF' : s.color"><component :is="s.icon" /></el-icon>
          </div>
          <span class="sc-trend" v-if="s.trend !== 0" :class="s.trend > 0 ? 'up' : 'down'">
            {{ s.trend > 0 ? '↑' : '↓' }}{{ Math.abs(s.trend) }}%
          </span>
          <span class="sc-trend flat" v-else>持平</span>
        </div>
        <div class="sc-num">{{ s.value }}</div>
        <div class="sc-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 图表 row 1 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-top">
          <span class="chart-name">在岗实习生</span>
          <el-select v-model="lineRange" size="small" class="chart-pick" @change="switchLine">
            <el-option label="日" value="day"/><el-option label="周" value="week"/><el-option label="月" value="month"/>
          </el-select>
        </div>
        <div class="chart-canvas">
          <svg viewBox="0 0 500 180" preserveAspectRatio="xMidYMid meet">
            <line v-for="y in [0,40,80,120,160]" :key="'gl'+y" x1="32" :y1="y" x2="492" :y2="y" stroke="#F1F5F9" stroke-width="1"/>
            <defs><linearGradient id="lg1" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#2563EB" stop-opacity="0.14"/><stop offset="100%" stop-color="#2563EB" stop-opacity="0"/></linearGradient></defs>
            <polygon class="svg-morph" :points="lineAreaPoints" fill="url(#lg1)"/>
            <polyline class="svg-morph" :points="linePoints" fill="none" stroke="#2563EB" stroke-width="2" stroke-linejoin="round" stroke-linecap="round"/>
            <circle v-for="(p,i) in displayLineData" :key="'lc'+i" class="svg-dot" :cx="lineCx(i)" :cy="150 - p * lineScale" r="3.5" fill="#2563EB" stroke="#FFF" stroke-width="1.5"/>
            <text v-for="(p,i) in displayLineData" :key="'lt'+i" class="svg-label" :x="lineCx(i)" y="172" text-anchor="middle" v-if="lineShowLabel(i)">{{ lineLabels[i] }}</text>
          </svg>
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-top">
          <span class="chart-name">投递与录用统计</span>
        </div>
        <div class="bar-chart">
          <div class="bar-item" v-for="b in displayBarData" :key="b.month">
            <span class="bar-val" v-if="b.apply > 0">{{ b.apply }}</span>
            <span class="bar-val bar-val-zero" v-else></span>
            <div class="bar-wrap">
              <div class="bar apply morph-bar" :style="{ height: (b.apply / maxBar * 100) + '%' }"></div>
              <div class="bar hired morph-bar" :style="{ height: (b.hired / maxBar * 100) + '%' }"></div>
            </div>
            <span class="bar-label">{{ b.month }}</span>
          </div>
          <div class="bar-legend"><span class="dot apply-dot"></span>投递<span class="dot hired-dot"></span>录用</div>
        </div>
        <div class="bar-metrics">
          <div class="bm-item"><span class="bm-val">{{ barTotalApply }}</span><span class="bm-lbl">简历投递总量</span></div>
          <div class="bm-item"><span class="bm-val">{{ barTotalHired }}</span><span class="bm-lbl">已录用人数</span></div>
          <div class="bm-item"><span class="bm-val">{{ myJobCount }}</span><span class="bm-lbl">在招岗位</span></div>
        </div>
      </div>
    </div>

    <!-- 图表 row 2 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-top">
          <span class="chart-name">实习考核等级分布</span>
          <el-select v-model="pieRange" size="small" class="chart-pick" @change="switchPie">
            <el-option label="日" value="day"/><el-option label="周" value="week"/><el-option label="月" value="month"/>
          </el-select>
        </div>
        <div class="pie-wrap">
          <svg viewBox="0 0 200 200" width="170" height="170">
            <circle v-for="(seg,i) in pieSegs" :key="i" class="svg-morph" cx="100" cy="100" r="78" fill="none" :stroke="seg.color" stroke-width="20" :stroke-dasharray="seg.dash + ' ' + (490 - seg.dash)" :stroke-dashoffset="seg.offset" transform="rotate(-90 100 100)" stroke-linecap="butt"/>
            <text x="100" y="96" text-anchor="middle" fill="#1E293B" font-size="22" font-weight="700">{{ pieTotal }}</text>
            <text x="100" y="114" text-anchor="middle" fill="#94A3B8" font-size="10">总人数</text>
          </svg>
          <div class="pie-legend">
            <div v-for="s in displayPieData" :key="s.label" class="legend-row">
              <span class="lr-dot" :style="{ background: s.color }"></span>
              <span class="lr-name">{{ s.label }}</span>
              <span class="lr-pct">{{ pieTotal > 0 ? Math.round(s.value / pieTotal * 100) : 0 }}%</span>
              <span class="lr-val">{{ s.value }}人</span>
            </div>
          </div>
        </div>
        <!-- 在岗学生专业分布 -->
        <div class="major-bars">
          <div class="mb-title">在岗学生专业分布</div>
          <div class="mb-row" v-for="m in majorData" :key="m.name">
            <span class="mb-name">{{ m.name }}</span>
            <div class="mb-track"><div class="mb-fill" :style="{ width: (m.count / majorMax * 100) + '%' }"></div></div>
            <span class="mb-num">{{ m.count }}人</span>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-top">
          <span class="chart-name">存证单据趋势</span>
          <el-select v-model="trendRange" size="small" class="chart-pick" @change="switchTrend">
            <el-option label="日" value="day"/><el-option label="周" value="week"/><el-option label="月" value="month"/>
          </el-select>
        </div>
        <div class="chart-canvas">
          <svg viewBox="0 0 500 180" preserveAspectRatio="xMidYMid meet">
            <line v-for="y in [0,40,80,120,160]" :key="'gt'+y" x1="32" :y1="y" x2="492" :y2="y" stroke="#F1F5F9" stroke-width="1"/>
            <defs><linearGradient id="tg1" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stop-color="#2563EB" stop-opacity="0.14"/><stop offset="100%" stop-color="#2563EB" stop-opacity="0"/></linearGradient></defs>
            <polygon class="svg-morph" :points="trendAreaPoints" fill="url(#tg1)"/>
            <polyline class="svg-morph" :points="trendPoints" fill="none" stroke="#2563EB" stroke-width="2" stroke-linejoin="round" stroke-linecap="round"/>
            <circle v-for="(p,i) in displayTrendData" :key="'tc'+i" class="svg-dot" :cx="trendCx(i)" :cy="150 - p * trendScale" r="4" fill="#2563EB" stroke="#FFF" stroke-width="2"/>
            <text v-for="(p,i) in displayTrendData" :key="'tt'+i" class="svg-label" :x="trendCx(i)" y="172" text-anchor="middle">{{ trendLabels[i] }}</text>
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Briefcase, DataAnalysis, Document, Warning, Bell } from '@element-plus/icons-vue'
import api from '../../api/request'
import { useDataStore } from '../../stores/dataStore'
import { useUserStore } from '../../stores/userStore'
const dataStore = useDataStore()
const userStore = useUserStore()
const router = useRouter()
const myCompanyId = computed(() => userStore.userInfo?.userId || null)

const now = ref('')
const switching = ref(false)

const triggerSwitch = (fn) => {
  switching.value = true
  setTimeout(() => { fn(); setTimeout(() => { switching.value = false }, 120) }, 60)
}

// ===== 基础计数值 (从 dataStore 动态获取) =====
const myInternships = computed(() => (dataStore.internships || []).filter(i => (i.enterpriseId || i.companyId) === myCompanyId.value))
const hiredSignedCount = computed(() => myInternships.value.filter(i => i.status === 'ACTIVE' || i.agreementSigned).length)
const myJobCount = computed(() => (dataStore.jobs || []).filter(j => j.companyId === myCompanyId.value).length)
const myJobs = computed(() => (dataStore.jobs || []).filter(j => j.companyId === myCompanyId.value))
const myAssessmentCount = computed(() => (dataStore.assessments || []).filter(a => a.companyId === myCompanyId.value).length)
const myArchiveCount = computed(() => (dataStore.archives || []).filter(a => a.companyId === myCompanyId.value).length)
const isCompletedAssessment = (status) => status === 'COMPLETED' || status === 'Completed' || status === '已完成'
const myPendingAssessmentCount = computed(() => (dataStore.assessments || []).filter(a => a.companyId === myCompanyId.value && !isCompletedAssessment(a.status)).length)
const myDisputeCount = computed(() => (dataStore.disputes || []).filter(d => d.companyId === myCompanyId.value && d.status === 'PENDING').length)
const myArchives = computed(() => (dataStore.archives || []).filter(a => a.companyId === myCompanyId.value))

// ===== 动态趋势 =====
const trends = computed(() => {
  const now = new Date()
  const thisMonth = String(now.getFullYear()) + '-' + String(now.getMonth()+1).padStart(2,'0')
  const lastMonth = String(now.getFullYear()) + '-' + String(now.getMonth()).padStart(2,'0')
  const hired = myInternships.value.filter(i => i.status === 'ACTIVE' || i.agreementSigned)
  const hiredThis = hired.filter(h => h.startDate && h.startDate.startsWith(thisMonth)).length
  const hiredLast = hired.filter(h => h.startDate && h.startDate.startsWith(lastMonth)).length
  const archs = (dataStore.archives || []).filter(a => a.companyId === myCompanyId.value)
  const archThis = archs.filter(a => a.time && a.time.startsWith(thisMonth)).length
  const archLast = archs.filter(a => a.time && a.time.startsWith(lastMonth)).length
  const calcTrend = (curr, prev) => prev > 0 ? Math.round((curr - prev) / prev * 100) : (curr > 0 ? 100 : 0)
  const jobs = (dataStore.jobs || []).filter(j => j.companyId === myCompanyId.value)
  return {
    interns: calcTrend(hiredThis, hiredLast),
    jobs: jobs.length > 0 ? Math.round(jobs.filter(j => j.status === 'OPEN').length / Math.max(jobs.length,1) * 100) - 50 : 0,
    assessments: calcTrend(myAssessmentCount.value, Math.max(myAssessmentCount.value - 1, 0)),
    archives: calcTrend(archThis, archLast)
  }
})

// ===== 统计卡片 =====
const remoteStats = ref(null)
const localStats = computed(() => [
  { label:'在岗实习生', value: hiredSignedCount.value, icon:User, color:'#2563EB', bg:'#EFF6FF', trend: trends.value.interns, blue:false },
  { label:'发布岗位',   value: myJobCount.value, icon:Briefcase, color:'#7C3AED', bg:'#F5F3FF', trend: trends.value.jobs,  blue:false },
  { label:'本月考核',   value: myAssessmentCount.value, icon:DataAnalysis, color:'#D97706', bg:'#FFFBEB', trend: trends.value.assessments, blue:false },
  { label:'存证单据',   value: myArchiveCount.value, icon:Document, color:'#FFF', bg:'rgba(255,255,255,0.2)', trend: trends.value.archives, blue:true, to:'/enterprise/archive' },
  { label:'待处理考核', value: myPendingAssessmentCount.value, icon:Warning, color:'#DC2626', bg:'#FEF2F2', trend: 0,  blue:false },
  { label:'待处理纠纷', value: myDisputeCount.value, icon:Bell, color:'#EA580C', bg:'#FFF7ED', trend: 0,  blue:false },
])
const stats = computed(() => remoteStats.value || localStats.value)

const openStat = (stat) => {
  if (stat?.to) router.push(stat.to)
}

// ===== 在岗实习生折线 (从 hiredStudents 真实计算) =====
const lineRange = ref('month')
const buildLineData = () => {
  const hired = myInternships.value.filter(i => i.status === 'ACTIVE' || i.agreementSigned)
  const now = new Date()
  if (lineRange.value === 'month') {
    const labels = []; const data = []
    for (let i = 11; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
      const key = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`
      labels.push(String(d.getMonth()+1) + '月')
      data.push(hired.filter(h => h.startDate && h.startDate <= key).length)
    }
    return { data, labels }
  } else if (lineRange.value === 'week') {
    const labels = []; const data = []
    for (let i = 7; i >= 0; i--) {
      const d = new Date(now); d.setDate(d.getDate() - i * 7)
      const key = d.toISOString().slice(0, 10)
      labels.push('W' + (8 - i))
      data.push(hired.filter(h => h.startDate && h.startDate <= key).length)
    }
    return { data, labels }
  } else {
    const labels = []; const data = []
    for (let i = 29; i >= 0; i--) {
      const d = new Date(now); d.setDate(d.getDate() - i)
      const key = d.toISOString().slice(0, 10)
      labels.push(i % 5 === 0 ? String(d.getDate()) : '')
      data.push(hired.filter(h => h.startDate && h.startDate <= key).length)
    }
    return { data, labels }
  }
}
const displayLineData = ref(buildLineData().data)
const lineLabels = ref(buildLineData().labels)
const lineScale = computed(() => { const m = Math.max(...displayLineData.value, 1); return 125 / m })
const lineCx = (i) => 32 + i * (456 / Math.max(displayLineData.value.length - 1, 1))
const lineShowLabel = (i) => lineRange.value === 'day' ? (i+1)%5===0 : lineRange.value === 'week' ? (i+1)%2===0 : true
const linePoints = computed(() => displayLineData.value.map((v,i) => `${lineCx(i)},${150 - v * lineScale.value}`).join(' '))
const lineAreaPoints = computed(() => `32,150 ${linePoints.value} ${lineCx(displayLineData.value.length-1)},150`)
const switchLine = (val) => triggerSwitch(() => { const d = buildLineData(); displayLineData.value = d.data; lineLabels.value = d.labels })

// ===== 岗位发布柱状 (从 jobs 真实计算) =====
// barRange removed - now always shows monthly
const buildBarData = () => {
  const jobs = (dataStore.jobs || []).filter(j => j.companyId === myCompanyId.value)
  const apps = (dataStore.applications || []).filter(a => a.companyId === myCompanyId.value)
  const now = new Date()
  const months = []
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const key = d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0')
    months.push({
      month: String(d.getMonth() + 1) + '月',
      key,
      new: jobs.filter(j => j.status === 'OPEN').length,
      apply: apps.filter(a => (a.applyDate || '').startsWith(key)).length,
      hired: myInternships.value.filter(h => (h.status === 'ACTIVE' || h.agreementSigned) && (h.startDate || '').startsWith(key)).length
    })
  }
  return months
}
const displayBarData = ref(buildBarData())
// Refresh bar data when jobs/apps change
const refreshBars = () => { displayBarData.value = buildBarData() }
const maxBar = computed(() => Math.max(...displayBarData.value.map(b => Math.max(b.apply, b.hired)), 1))
const barTotalApply = computed(() => (dataStore.applications || []).filter(a => a.companyId === myCompanyId.value).length)
const barTotalHired = computed(() => myInternships.value.filter(h => h.status === 'ACTIVE' || h.agreementSigned).length)


// ===== 考核等级饼图 (从 assessments 真实计算) =====
const pieRange = ref('month')
const buildPieData = () => {
  const assessments = (dataStore.assessments || []).filter(a => a.companyId === myCompanyId.value)
  const excellent = assessments.filter(a => a.score >= 90).length
  const good = assessments.filter(a => a.score >= 80 && a.score < 90).length
  const pass = assessments.filter(a => a.score >= 60 && a.score < 80).length
  const fail = assessments.filter(a => a.score < 60).length
  return [
    { label: '优秀', value: excellent, color: '#10B981' },
    { label: '良好', value: good, color: '#2563EB' },
    { label: '及格', value: pass, color: '#F99716' },
    { label: '不及格', value: fail, color: '#EF4444' },
  ]
}
const displayPieData = ref(buildPieData())
const pieTotal = computed(() => displayPieData.value.reduce((s,d) => s + d.value, 0) || 1)
const pieSegs = computed(() => { let o = 0; return displayPieData.value.map(d => { const p = d.value / pieTotal.value * 490; const seg = { color: d.color, dash: p, offset: -o }; o += p; return seg }) })
const switchPie = (val) => triggerSwitch(() => { displayPieData.value = buildPieData() })

// ===== 在岗学生专业分布 (从 applications 真实计算) =====
const majorData = computed(() => {
  const apps = (dataStore.applications || []).filter(a => a.companyId === myCompanyId.value)
  const majors = {}
  apps.forEach(a => { const m = a.major || '未知'; majors[m] = (majors[m] || 0) + 1 })
  return Object.entries(majors).map(([name, count]) => ({ name, count })).sort((a,b) => b.count - a.count).slice(0, 8)
})
const majorMax = computed(() => Math.max(...majorData.value.map(m => m.count), 1))

// ===== 存证趋势 (从 archives 真实计算) =====
const trendRange = ref('month')
const buildTrendData = () => {
  const archs = myArchives.value
  const now = new Date()
  if (trendRange.value === 'month') {
    const labels = []; const data = []
    for (let i = 11; i >= 0; i--) {
      const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
      const key = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`
      labels.push(String(d.getMonth()+1) + '月')
      data.push(archs.filter(a => a.time && a.time <= key + '-31').length)
    }
    return { data, labels }
  } else if (trendRange.value === 'week') {
    const labels = []; const data = []
    for (let i = 7; i >= 0; i--) {
      const d = new Date(now); d.setDate(d.getDate() - i * 7)
      const key = d.toISOString().slice(0, 10)
      labels.push('W' + (8 - i))
      data.push(archs.filter(a => a.time && a.time <= key).length)
    }
    return { data, labels }
  } else {
    const labels = []; const data = []
    for (let i = 29; i >= 0; i--) {
      const d = new Date(now); d.setDate(d.getDate() - i)
      const key = d.toISOString().slice(0, 10)
      labels.push(i % 5 === 0 ? String(d.getDate()) : '')
      data.push(archs.filter(a => a.time && a.time <= key).length)
    }
    return { data, labels }
  }
}
const displayTrendData = ref(buildTrendData().data)
const trendLabels = ref(buildTrendData().labels)
const trendScale = computed(() => { const m = Math.max(...displayTrendData.value, 1); return 125 / m })
const trendCx = (i) => 32 + i * (456 / Math.max(displayTrendData.value.length - 1, 1))
const trendPoints = computed(() => displayTrendData.value.map((v,i) => `${trendCx(i)},${150 - v * trendScale.value}`).join(' '))
const trendAreaPoints = computed(() => `32,150 ${trendPoints.value} ${trendCx(displayTrendData.value.length-1)},150`)
const switchTrend = (val) => triggerSwitch(() => { const d = buildTrendData(); displayTrendData.value = d.data; trendLabels.value = d.labels })

onMounted(() => {
  const d = new Date()
  now.value = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
  loadDashboard()
})
const loadDashboard = async () => {
  try {
    const r = await api.get('/enterprise/dashboard')
    if (r.data?.stats) {
      remoteStats.value = r.data.stats.map((s, i) => ({ ...localStats.value[i], value: s.value, trend: s.trend }))
    }
  } catch (e) {}
}
</script>

<style scoped>
.dashboard { width: 100%; animation: dashIn 0.5s ease-out; }
@keyframes dashIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }

/* 切换时的微闪效果 */
.is-switching .svg-morph,
.is-switching .svg-dot,
.is-switching .morph-bar { opacity: 0.35; transition: opacity 0.08s ease; }

/* ===== 顶部 ===== */
.dash-top { margin-bottom: 24px; }
.dash-title { font-size: 24px; font-weight: 700; color: #1E293B; letter-spacing: -0.5px; margin: 0; }
.dash-updated { font-size: 12px; color: #94A3B8; margin-top: 2px; display: block; }

/* ===== 统计卡片网格 ===== */
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(168px, 1fr)); gap: 14px; margin-bottom: 20px; }
.stat-card {
  background: #FFF; border-radius: 14px; padding: 18px 16px 14px;
  box-shadow: 0 2px 14px rgba(0,0,0,0.05); cursor: default;
  transition: all 0.2s; animation: cardIn 0.45s ease-out both; position: relative; overflow: hidden;
}
.stat-card.is-clickable { cursor: pointer; }
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(0,0,0,0.07); }
.stat-card.is-blue { background: #2563EB; }
.stat-card.is-blue .sc-label { color: rgba(255,255,255,0.75); }
.stat-card.is-blue .sc-num { color: #FFF; }
.stat-card.is-blue .sc-trend.flat { color: rgba(255,255,255,0.6); }

@keyframes cardIn { from { opacity: 0; transform: translateY(8px) scale(0.98); } to { opacity: 1; transform: translateY(0) scale(1); } }

.sc-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.sc-icon { width: 38px; height: 38px; border-radius: 50%; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.sc-trend { font-size: 12px; font-weight: 600; padding: 1px 6px; border-radius: 6px; }
.sc-trend.up { color: #10B981; background: #ECFDF5; }
.sc-trend.down { color: #EF4444; background: #FEF2F2; }
.sc-trend.flat { color: #94A3B8; font-size: 11px; background: #F8FAFC; }
.sc-num { font-size: 30px; font-weight: 700; color: #1E293B; line-height: 1; }
.sc-label { font-size: 12px; color: #94A3B8; margin-top: 4px; }

/* ===== 图表行 ===== */
.chart-row { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-bottom: 14px; }
.chart-card {
  background: #FFF; border-radius: 14px; padding: 20px 20px 16px;
  box-shadow: 0 2px 14px rgba(0,0,0,0.05);
}
.chart-top { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.chart-name { font-size: 13px; font-weight: 600; color: #334155; }
.chart-pick { width: 76px; }
.chart-pick :deep(.el-input__wrapper) { background: #F8FAFC !important; border: 1px solid #E2E8F0 !important; border-radius: 6px !important; height: 28px !important; font-size: 11px !important; box-shadow: none !important; }
.chart-canvas { overflow: hidden; }

/* SVG 平滑过渡 */
.svg-morph { transition: all 0.55s cubic-bezier(0.4, 0, 0.2, 1); }
.svg-dot { transition: cx 0.55s cubic-bezier(0.4, 0, 0.2, 1), cy 0.55s cubic-bezier(0.4, 0, 0.2, 1), opacity 0.2s; }
.svg-label { font-size: 9px; fill: #94A3B8; }

/* 柱状图 */
.bar-chart { display: flex; align-items: flex-end; gap: 12px; padding: 4px 0; height: 180px; position: relative; }
.bar-item { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; }
.bar-wrap { display: flex; align-items: flex-end; gap: 3px; height: 150px; width: 100%; justify-content: center; }
.bar { width: 12px; border-radius: 3px 3px 0 0; }
.bar.apply { background: #2563EB; }
.bar.hired { background: #059669; }
.morph-bar { transition: height 0.5s cubic-bezier(0.4, 0, 0.2, 1); }
.bar-val { font-size: 10px; font-weight: 600; color: #2563EB; margin-bottom: 3px; transition: all 0.4s; }
.bar-val-zero { color: transparent; }
.bar-label { font-size: 10px; color: #94A3B8; margin-top: 6px; }
.bar-legend { position: absolute; top: 0; right: 0; display: flex; align-items: center; gap: 8px; font-size: 10px; color: #94A3B8; }
.dot { width: 7px; height: 7px; border-radius: 2px; display: inline-block; margin-right: 3px; }
.apply-dot { background: #2563EB; }
.hired-dot { background: #059669; }

/* 饼图 */
.pie-wrap { display: flex; align-items: center; gap: 20px; padding: 4px 8px; }
.pie-legend { display: flex; flex-direction: column; gap: 10px; }
.legend-row { display: flex; align-items: center; gap: 8px; font-size: 12px; }
.lr-dot { width: 9px; height: 9px; border-radius: 3px; flex-shrink: 0; }
.lr-name { color: #64748B; width: 36px; }
.lr-pct { color: #94A3B8; font-size: 11px; width: 32px; text-align: right; }
.lr-val { color: #334155; font-weight: 500; }
/* 柱状图下方指标 */
.bar-metrics { display:flex; gap:16px; padding-top:14px; margin-top:14px; border-top:1px solid #F1F5F9; }
.bm-item { display:flex; flex-direction:column; gap:2px; flex:1; }
.bm-val { font-size:16px; font-weight:700; color:#334155; }
.bm-lbl { font-size:10px; color:#94A3B8; }

/* 专业分布横向柱状图 */
.major-bars { padding-top:14px; margin-top:14px; border-top:1px solid #F1F5F9; }
.mb-title { font-size:11px; font-weight:600; color:#94A3B8; margin-bottom:10px; }
.mb-row { display:flex; align-items:center; gap:10px; margin-bottom:8px; }
.mb-name { width:56px; font-size:11px; color:#64748B; text-align:right; flex-shrink:0; }
.mb-track { flex:1; height:8px; background:#F1F5F9; border-radius:4px; overflow:hidden; }
.mb-fill { height:100%; background:#2563EB; border-radius:4px; transition:width 0.5s cubic-bezier(0.4,0,0.2,1); }
.mb-num { width:30px; font-size:11px; color:#94A3B8; flex-shrink:0; }
</style>
