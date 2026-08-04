<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>全局大盘</h1>
        <span class="page-sub">全平台运营数据概览</span>
      </div>
    </div>

    <!-- 8 stat cards -->
    <div class="stat-grid">
      <div class="stat-card" v-for="s in stats" :key="s.label" :class="{ 'is-clickable': s.to }" @click="openStat(s)">
        <div class="sc-top">
          <div class="sc-icon" :style="{background:s.bg}"><el-icon :size="18" :color="s.color"><component :is="s.icon"/></el-icon></div>
          <span class="sc-trend" v-if="s.trend!==0" :class="s.trend>0?'up':'down'">{{s.trend>0?'+':''}}{{s.trend}}</span>
        </div>
        <div class="sc-num">{{s.value}}</div>
        <div class="sc-label">{{s.label}}</div>
      </div>
    </div>

    <!-- Charts row 1 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-head">存证上链趋势</div>
        <div class="chart-body">
          <div class="mini-line">
            <div class="ml-bar" v-for="(m,i) in archiveMonths" :key="m.month" :title="m.month+': '+m.count+'份'">
              <div class="ml-fill" :style="{height:(m.count/maxArchive*100)+'%'}"></div>
              <span class="ml-label">{{m.month.slice(5)}}月</span>
              <span class="ml-val" v-if="m.count>0">{{m.count}}</span>
            </div>
          </div>
          <div class="chart-summary">
            <span class="cs-item">本月存证: <b>{{archiveMonths[archiveMonths.length-1]?.count||0}} 份</b></span>
            <span class="cs-item">总计: <b>{{archives.length}} 份</b></span>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-head">企业入驻状态分布</div>
        <div class="chart-body">
          <div class="pie-wrap">
            <svg viewBox="0 0 120 120" width="120" height="120">
              <circle cx="60" cy="60" r="50" fill="none" stroke="#F1F5F9" stroke-width="12"/>
              <circle v-for="(seg,i) in enterprisePie" :key="i" cx="60" cy="60" r="50" fill="none"
                :stroke="seg.color" stroke-width="12"
                :stroke-dasharray="seg.len+' '+(314-seg.len)"
                :stroke-dashoffset="seg.offset"
                transform="rotate(-90 60 60)" style="transition:all 0.6s"/>
              <text x="60" y="57" text-anchor="middle" font-size="20" font-weight="700" fill="#1E293B">{{enterprisePie[0]?.pct||0}}%</text>
              <text x="60" y="73" text-anchor="middle" font-size="10" fill="#94A3B8">已入驻</text>
            </svg>
            <div class="pie-legend">
              <div class="pl-row" v-for="s in enterprisePie" :key="s.label">
                <span class="pl-dot" :style="{background:s.color}"></span>
                <span class="pl-name">{{s.label}}</span>
                <span class="pl-num">{{s.value}}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Charts row 2 -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-head">实习岗位发布统计</div>
        <div class="chart-body">
          <div class="bar-group">
            <div class="bg-item" v-for="b in jobStats" :key="b.label">
              <span class="bg-label">{{b.label}}</span>
              <div class="bg-track"><div class="bg-fill" :style="{width:b.pct+'%',background:b.color}"></div></div>
              <span class="bg-val">{{b.value}}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <div class="chart-head">最新存证记录</div>
        <div class="chart-body" style="max-height:180px;overflow-y:auto">
          <div class="log-item is-clickable" v-for="(a,i) in archives.slice(0,6)" :key="i" @click="openArchive(a)">
            <span class="log-type" :style="{color:archiveColor(a.type)}">{{ archiveTypeText(a.type) }}</span>
            <span class="log-name" :title="archiveTitle(a)">{{ archiveNameText(a) }}</span>
            <span class="log-time">{{(a.time||'').slice(0,10)}}</span>
          </div>
          <div v-if="archives.length===0" class="log-empty">暂无存证记录</div>
        </div>
      </div>
    </div>

    <!-- Charts row 3 -->
    <div class="chart-row">
      <div class="chart-card full">
        <div class="chart-head">近期已入驻企业</div>
        <div class="chart-body">
          <div class="ent-list">
            <div class="el-item" v-for="e in approvedEnterprises.slice(0,5)" :key="e.id">
              <span class="el-name">{{e.name}}</span>
              <span class="el-code">{{e.code}}</span>
              <span class="el-industry">{{e.industry||'未填写'}}</span>
              <span class="el-tag green">已入驻</span>
            </div>
            <div v-if="approvedEnterprises.length===0" class="log-empty">暂无已入驻企业</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { OfficeBuilding, School, UserFilled, Stamp, Briefcase, Checked, DataAnalysis, Clock } from '@element-plus/icons-vue'
import { useDataStore } from '../../stores/dataStore'
import api from '../../api/request'

const dataStore = useDataStore()
const router = useRouter()
const archives = computed(() => dataStore.archives || [])
const enterpriseList = computed(() => dataStore.enterpriseList || [])
const schoolStudents = computed(() => dataStore.schoolStudents || [])
const jobs = computed(() => dataStore.jobs || [])
const internships = computed(() => dataStore.internships || [])
const applications = computed(() => dataStore.applications || [])
const assessments = computed(() => dataStore.assessments || [])
const disputes = computed(() => dataStore.disputes || [])
const schoolCount = ref(0)

// Stats cards
const approvedCount = computed(() => enterpriseList.value.filter(e => e.status === 'APPROVED').length)
const pendingCount = computed(() => enterpriseList.value.filter(e => e.status === 'PENDING' || e.status === 'SCHOOL_APPROVED').length)
const activeStudents = computed(() => internships.value.filter(i => i.status === 'ACTIVE').length)

const stats = computed(() => [
  { label:'入驻企业', value: approvedCount.value, icon:OfficeBuilding, color:'#2563EB', bg:'#EFF6FF', trend: pendingCount.value },
  { label:'实习学生', value: activeStudents.value, icon:UserFilled, color:'#059669', bg:'#ECFDF5', trend: schoolStudents.value.length },
  { label:'开放岗位', value: jobs.value.filter(j=>j.status==='OPEN').length, icon:Briefcase, color:'#7C3AED', bg:'#F5F3FF', trend: jobs.value.length },
  { label:'存证单据', value: archives.value.length, icon:Stamp, color:'#D97706', bg:'#FFFBEB', trend: archives.value.length, to:'/platform/chain' },
  { label:'待审企业', value: pendingCount.value, icon:Clock, color:'#EA580C', bg:'#FFF7ED', trend: 0 },
  { label:'本月考核', value: assessments.value.length, icon:DataAnalysis, color:'#DC2626', bg:'#FEF2F2', trend: 0 },
  { label:'岗位申请', value: applications.value.length, icon:Checked, color:'#06B6D4', bg:'#ECFEFF', trend: 0 },
  { label:'入驻院校', value: schoolCount.value, icon:School, color:'#8B5CF6', bg:'#F5F3FF', trend: 0 },
])

const openStat = (stat) => {
  if (stat?.to) router.push(stat.to)
}

const openArchive = () => {
  router.push('/platform/chain')
}

onMounted(async () => {
  try {
    const res = await api.get('/auth/users')
    schoolCount.value = (res.data || []).filter(u => u.role === 'SCHOOL_ADMIN').length
  } catch (e) {
    schoolCount.value = 0
  }
})

// Archive monthly trend
const archiveMonths = computed(() => {
  const months = []
  const now = new Date()
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const key = d.toISOString().slice(0,7)
    const count = archives.value.filter(a => (a.time||'').startsWith(key)).length
    months.push({ month: key, count })
  }
  return months
})
const maxArchive = computed(() => Math.max(...archiveMonths.value.map(m=>m.count), 1))

const archiveTypeText = (type) => ({
  USER_PROFILE: '用户档案快照',
  USER_REGISTER: '用户注册记录',
  USER_PROFILE_UPDATE: '资料修改记录',
  USER_PASSWORD_CHANGE: '密码修改记录',
  USER_PASSWORD_RESET: '密码重置记录',
  USER_PHONE_BACKFILL: '手机号补全记录',
  USER_NAME_BACKFILL: '姓名修复记录',
  USER_APPROVE: '用户审核通过',
  USER_REJECT: '用户审核驳回',
  USER_ENABLE: '用户启用记录',
  USER_DISABLE: '用户禁用记录',
  USER_IDENTITY_SUBMIT: '实名认证提交',
  USER_ENTERPRISE_QUALIFICATION_SUBMIT: '企业资质提交',
  USER_ENTERPRISE_APPROVE: '企业入驻审核',
  USER_ENTERPRISE_ROSTER_CREATE: '企业名单录入',
  '岗位发布': '岗位发布',
  '简历投递': '简历投递',
  '录用处理': '录用处理',
  '实习登记': '实习备案',
  '考核提交': '过程考核',
  '纠纷发起': '纠纷申诉',
  '纠纷处理': '纠纷处理',
  '协议签署': '协议签署',
  '企业资质提交': '企业资质提交',
  '企业资质审核通过': '企业入驻审核',
  '实名认证提交': '实名认证提交'
}[type] || type || '存证记录')

const archiveColor = (type) => {
  if ((type || '').startsWith('USER_')) return '#D97706'
  if (['岗位发布', '简历投递', '录用处理'].includes(type)) return '#2563EB'
  if (['实习登记', '考核提交', '协议签署'].includes(type)) return '#059669'
  return '#D97706'
}

const archiveNameText = (archive) => {
  const name = archive?.name || ''
  const userMatch = name.match(/^user:(\d+):(.+)$/)
  if (userMatch) {
    const action = userMatch[2] === 'profile' ? '档案快照' : archiveTypeText(archive.type)
    return `用户#${userMatch[1]} · ${action}`
  }
  return name || archiveTypeText(archive?.type)
}

const archiveTitle = (archive) => {
  const status = archive?.chainStatus === 'ON_CHAIN' ? '已上链' : '本地存证'
  return `${archiveTypeText(archive?.type)}｜${archiveNameText(archive)}｜${status}${archive?.txHash ? '｜交易哈希：' + archive.txHash : ''}`
}

// Enterprise status pie
const enterprisePie = computed(() => {
  const approved = enterpriseList.value.filter(e=>e.status==='APPROVED').length
  const pending = enterpriseList.value.filter(e=>e.status==='PENDING'||e.status==='SCHOOL_APPROVED').length
  const rejected = enterpriseList.value.filter(e=>e.status==='REJECTED').length
  const total = approved + pending + rejected || 1
  const pct = Math.round(approved/total*100)
  const len = Math.round(pct/100*314)
  return [
    { label:'已入驻', value:approved, pct, color:'#059669', len, offset:0 },
    { label:'待审核', value:pending, color:'#D97706', len:Math.round(pending/total*314), offset: -len },
    { label:'已驳回', value:rejected, color:'#DC2626', len:Math.round(rejected/total*314), offset: -(len+Math.round(pending/total*314)) },
  ]
})

// Job stats bars
const jobStats = computed(() => {
  const open = jobs.value.filter(j=>j.status==='OPEN').length
  const closed = jobs.value.filter(j=>j.status==='CLOSED').length
  const max = Math.max(open, closed, 1)
  return [
    { label:'开放中', value:open, pct:Math.round(open/max*100), color:'#059669' },
    { label:'已关闭', value:closed, pct:Math.round(closed/max*100), color:'#94A3B8' },
    { label:'总申请', value:applications.value.length, pct:Math.round(applications.value.length/Math.max(applications.value.length,1)*100), color:'#2563EB' },
  ]
})

const approvedEnterprises = computed(() => enterpriseList.value.filter(e=>e.status==='APPROVED'))
</script>

<style scoped>
.page { width:100%; }
.page-head { margin-bottom:20px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.page-sub { font-size:13px; color:#94A3B8; display:block; margin-top:2px; }

/* 8 stat cards */
.stat-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:14px; margin-bottom:20px; }
.stat-card {
  background:#FFF; border-radius:14px; padding:18px 16px;
  box-shadow:0 2px 14px rgba(0,0,0,0.05); transition:all 0.2s; cursor:default;
}
.stat-card.is-clickable { cursor:pointer; }
.stat-card:hover { transform:translateY(-2px); box-shadow:0 6px 20px rgba(0,0,0,0.07); }
.sc-top { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
.sc-icon { width:36px; height:36px; border-radius:50%; display:flex; align-items:center; justify-content:center; }
.sc-trend { font-size:12px; font-weight:600; color:#94A3B8; }
.sc-num { font-size:28px; font-weight:700; color:#1E293B; line-height:1; }
.sc-label { font-size:12px; color:#94A3B8; margin-top:4px; }

/* Chart rows */
.chart-row { display:grid; grid-template-columns:1fr 1fr; gap:14px; margin-bottom:14px; }
.chart-card {
  background:#FFF; border-radius:14px; padding:20px;
  box-shadow:0 2px 14px rgba(0,0,0,0.05);
}
.chart-card.full { grid-column:1/-1; }
.chart-head { font-size:14px; font-weight:600; color:#334155; margin-bottom:14px; }
.chart-body { min-height:120px; }

/* Mini bar chart (archive trend) */
.mini-line { display:flex; align-items:flex-end; gap:12px; height:140px; padding:0 8px; }
.ml-bar { flex:1; display:flex; flex-direction:column; align-items:center; height:100%; justify-content:flex-end; }
.ml-fill { width:24px; background:#2563EB; border-radius:4px 4px 0 0; min-height:2px; transition:height 0.5s; }
.ml-label { font-size:10px; color:#94A3B8; margin-top:6px; }
.ml-val { font-size:11px; font-weight:600; color:#2563EB; margin-bottom:4px; }
.chart-summary { display:flex; gap:24px; margin-top:16px; padding-top:12px; border-top:1px solid #F1F5F9; }
.cs-item { font-size:12px; color:#64748B; }
.cs-item b { color:#334155; }

/* Pie chart */
.pie-wrap { display:flex; align-items:center; gap:24px; }
.pie-legend { display:flex; flex-direction:column; gap:8px; }
.pl-row { display:flex; align-items:center; gap:8px; font-size:12px; }
.pl-dot { width:8px; height:8px; border-radius:2px; flex-shrink:0; }
.pl-name { color:#64748B; width:48px; }
.pl-num { color:#334155; font-weight:500; }

/* Bar group */
.bar-group { display:flex; flex-direction:column; gap:16px; }
.bg-item { display:flex; align-items:center; gap:10px; }
.bg-label { width:56px; font-size:12px; color:#64748B; text-align:right; flex-shrink:0; }
.bg-track { flex:1; height:10px; background:#F1F5F9; border-radius:5px; overflow:hidden; }
.bg-fill { height:100%; border-radius:5px; transition:width 0.5s; }
.bg-val { width:32px; font-size:12px; color:#334155; font-weight:500; }

/* Log list */
.log-item {
  display:grid;
  grid-template-columns:minmax(96px, 140px) minmax(0, 1fr) 86px;
  align-items:center;
  gap:12px;
  padding:8px 0;
  border-bottom:1px solid #F8FAFC;
  font-size:12px;
}
.log-item.is-clickable { cursor:pointer; }
.log-item.is-clickable:hover .log-name { color:#2563EB; }
.log-type { font-weight:600; min-width:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.log-name { min-width:0; color:#334155; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
.log-time { color:#94A3B8; text-align:right; white-space:nowrap; }
.log-empty { color:#94A3B8; font-size:13px; text-align:center; padding:30px 0; }

/* Enterprise list */
.ent-list { display:flex; flex-direction:column; }
.el-item { display:flex; align-items:center; gap:14px; padding:10px 0; border-bottom:1px solid #F8FAFC; font-size:13px; }
.el-name { font-weight:600; color:#334155; width:120px; }
.el-code { font-family:monospace; font-size:11px; color:#94A3B8; flex:1; }
.el-industry { color:#64748B; font-size:12px; }
.el-tag { padding:1px 8px; border-radius:6px; font-size:11px; font-weight:500; }
.el-tag.green { background:#ECFDF5; color:#059669; }
</style>
