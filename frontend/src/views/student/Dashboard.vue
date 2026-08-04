<template>
  <div class="page student-dashboard">
    <div class="student-overview">
      <div class="overview-copy">
        <span class="overview-kicker">Student command center</span>
        <h2>{{ myName }}，实习档案状态已汇总</h2>
        <p>投递、备案、考核与消息在同一条可信记录里流转，便于后续归档和链上核验。</p>
        <div class="overview-tags">
          <span>链上核验就绪</span>
          <span>学校监管同步</span>
          <span>档案留痕</span>
        </div>
      </div>
      <div class="overview-orbit" aria-hidden="true">
        <i></i><i></i><i></i>
      </div>
      <div class="overview-ledger">
        <span>可信档案</span>
        <strong>{{ myInternships.length + myAssessments.length + recentMessages.length }}</strong>
        <em>Archive items</em>
      </div>
    </div>

    <div class="stat-row">
      <div class="student-stat-card" v-for="s in stats" :key="s.label" :class="{ 'is-clickable': s.to }" :style="{'--accent':s.color,'--wash':s.bg}" @click="openStat(s)">
        <div class="student-stat-icon"><el-icon :size="20"><component :is="s.icon"/></el-icon></div>
        <div>
          <div class="student-stat-num">{{ s.value }}</div>
          <div class="student-stat-label">{{ s.label }}</div>
          <small>{{ s.hint }}</small>
        </div>
      </div>
    </div>

    <div class="dashboard-grid">
      <section class="student-panel wide">
        <div class="panel-head">
          <div>
            <span>Internship</span>
            <h3>实习动态</h3>
          </div>
          <small>{{ myInternships.length }} 条记录</small>
        </div>
        <div class="student-table-shell">
          <el-table :data="myInternships" size="small" empty-text="暂无实习记录，请前往实习择业投递岗位">
            <el-table-column prop="enterpriseName" label="企业" min-width="120"/>
            <el-table-column prop="position" label="岗位" min-width="120"/>
            <el-table-column prop="startDate" label="开始日期" width="110"/>
            <el-table-column prop="endDate" label="结束日期" width="110"/>
            <el-table-column prop="status" label="状态" width="90" align="center">
              <template #default="{row}"><span class="st" :class="row.status==='ACTIVE'?'st-ok':'st-done'">{{ row.status==='ACTIVE'?'进行中':'已结束' }}</span></template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section class="student-panel">
        <div class="panel-head">
          <div>
            <span>Assessment</span>
            <h3>我的考核</h3>
          </div>
          <small>{{ myAssessments.length }} 条</small>
        </div>
        <div class="student-table-shell">
          <el-table :data="myAssessments" size="small" empty-text="暂无考核，企业发布考核后将在此显示">
            <el-table-column prop="month" label="月份" width="100"/>
            <el-table-column label="出勤" width="120">
              <template #default="{ row }">{{ attendanceText(row.attendance) }}</template>
            </el-table-column>
            <el-table-column prop="score" label="评分" width="80">
              <template #default="{row}"><span :style="{color:row.score>=80?'#059669':row.score>=60?'#D97706':'#DC2626',fontWeight:'600'}">{{row.score}}</span></template>
            </el-table-column>
            <el-table-column prop="comment" label="评语" min-width="160" show-overflow-tooltip/>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{row}"><span class="st" :class="row.status==='已完成'?'st-ok':'st-wait'">{{row.status}}</span></template>
            </el-table-column>
          </el-table>
        </div>
      </section>

      <section class="student-panel">
        <div class="panel-head">
          <div>
            <span>Notice</span>
            <h3>最近消息</h3>
          </div>
          <small>{{ recentMessages.length }} 条</small>
        </div>
        <div class="student-table-shell">
          <el-table :data="recentMessages" size="small" empty-text="暂无消息" @row-click="openMsg">
            <el-table-column label="" width="40" align="center">
              <template #default="{row}"><span v-if="!row.read" class="unread-dot"></span></template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="160"/>
            <el-table-column prop="from" label="来源" width="100"/>
            <el-table-column prop="date" label="日期" width="110"/>
          </el-table>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Notebook, Stamp, Bell } from '@element-plus/icons-vue'
import { useDataStore } from '../../stores/dataStore'
import { useUserStore } from '../../stores/userStore'

const dataStore = useDataStore()
const userStore = useUserStore()
const router = useRouter()
const myName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '张同学')
const myId = computed(() => userStore.userInfo?.userId || null)

const myInternships = computed(() =>
  (dataStore.internships || []).filter(i => i.studentId === myId.value)
)

const myAssessments = computed(() =>
  (dataStore.assessments || []).filter(a => a.studentId === myId.value)
)
const isCompletedAssessment = (status) => status === 'COMPLETED' || status === 'Completed' || status === '已完成'
const attendanceText = (value) => {
  const text = String(value || '').trim()
  if (!text || text.includes('?')) return '全勤'
  return text
}

const recentMessages = computed(() =>
  (dataStore.messages || []).filter(m => !m.to || m.to === myName.value).slice(0, 5)
)

const stats = computed(() => [
  { label:'在投岗位', hint:'等待企业反馈', value: (dataStore.applications || []).filter(a=>a.studentId===myId.value&&a.status==='pending').length, icon:User, color:'#315DFF', bg:'#EDF2FF' },
  { label:'在岗实习', hint:'当前履约记录', value: (dataStore.internships || []).filter(i=>i.studentId===myId.value&&i.status==='ACTIVE').length, icon:Notebook, color:'#0F8A68', bg:'#EAF8F2' },
  { label:'考核完成', hint:'已形成过程材料', value: (dataStore.assessments || []).filter(a=>a.studentId===myId.value&&isCompletedAssessment(a.status)).length, icon:Stamp, color:'#B7791F', bg:'#FFF7E6', to:'/student/evidence' },
  { label:'未读消息', hint:'需要及时查看', value: (dataStore.messages || []).filter(m=>!m.read && (!m.to || m.to === myName.value)).length, icon:Bell, color:'#7C3AED', bg:'#F3EDFF' },
])

const openStat = (stat) => {
  if (stat?.to) router.push(stat.to)
}

const openMsg = (row) => {
  if (!row.read) dataStore.markMessageRead(row.id)
  ElMessage.info(row.title + ': ' + row.content)
}
</script>

<style scoped>
.student-dashboard {
  position: relative;
  width: 100%;
  color: #202721;
}

.student-dashboard::before {
  content: '';
  position: absolute;
  inset: -18px -12px auto;
  height: 260px;
  z-index: -1;
  border-radius: 28px;
  background:
    radial-gradient(circle at 18% 18%, rgba(208, 176, 108, .18), transparent 28%),
    radial-gradient(circle at 82% 20%, rgba(95, 168, 142, .18), transparent 30%),
    linear-gradient(180deg, rgba(255,255,255,.36), transparent);
  pointer-events: none;
}

.student-overview {
  position: relative;
  overflow: hidden;
  display:flex;
  align-items:center;
  justify-content:space-between;
  gap:22px;
  margin-bottom:16px;
  padding:26px 28px;
  min-height:178px;
  border:1px solid rgba(255,255,255,.18);
  border-radius:26px;
  background:
    radial-gradient(circle at 86% 22%, rgba(113, 204, 169, .24), transparent 24%),
    radial-gradient(circle at 10% 0%, rgba(228, 185, 98, .2), transparent 30%),
    linear-gradient(135deg, #17231d 0%, #26342c 54%, #eef3ea 148%);
  box-shadow:
    0 28px 72px rgba(32,39,33,.16),
    inset 0 1px 0 rgba(255,255,255,.16);
}

.student-overview::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(115deg, rgba(255,255,255,.14), transparent 34%),
    repeating-linear-gradient(90deg, rgba(255,255,255,.05) 0 1px, transparent 1px 78px);
  opacity: .78;
  pointer-events: none;
}

.student-overview::after {
  content: '';
  position: absolute;
  inset: auto -22% 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(214, 184, 111, .9), rgba(122, 217, 183, .8), transparent);
  animation: overview-scan 4.8s ease-in-out infinite;
}

.overview-copy,
.overview-ledger,
.overview-orbit {
  position: relative;
  z-index: 1;
}

.overview-copy {
  min-width: 0;
  max-width: 760px;
}

.overview-kicker {
  display:block;
  color:rgba(239,247,239,.62);
  font-size:12px;
  font-weight:900;
  letter-spacing:.12em;
  text-transform:uppercase;
}

.student-overview h2 {
  max-width:760px;
  margin:8px 0 0;
  color:#fff;
  font-size:30px;
  font-weight:950;
  line-height:1.18;
}

.student-overview p {
  max-width: 640px;
  margin: 12px 0 0;
  color: rgba(239,247,239,.68);
  font-size: 14px;
  line-height: 1.7;
}

.overview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.overview-tags span {
  padding: 7px 10px;
  border: 1px solid rgba(255,255,255,.14);
  border-radius: 999px;
  background: rgba(255,255,255,.08);
  color: rgba(246,250,244,.78);
  font-size: 12px;
  font-weight: 800;
}

.overview-orbit {
  flex: 0 0 auto;
  width: 142px;
  height: 142px;
  border: 1px solid rgba(255,255,255,.14);
  border-radius: 50%;
  background:
    radial-gradient(circle, rgba(255,255,255,.18) 0 6px, transparent 7px),
    radial-gradient(circle, rgba(255,255,255,.06), transparent 62%);
  animation: orbit-breathe 5.6s ease-in-out infinite;
}

.overview-orbit::before,
.overview-orbit::after,
.overview-orbit i {
  content: '';
  position: absolute;
  border-radius: 50%;
}

.overview-orbit::before {
  inset: 24px;
  border: 1px dashed rgba(255,255,255,.18);
}

.overview-orbit::after {
  width: 12px;
  height: 12px;
  left: 22px;
  top: 42px;
  background: #d9b86f;
  box-shadow: 58px 58px 0 rgba(119,213,180,.9), 84px 14px 0 rgba(255,255,255,.58);
}

.overview-orbit i {
  inset: 48px;
  border: 1px solid rgba(255,255,255,.2);
}

.overview-ledger {
  min-width:142px;
  padding:18px;
  border:1px solid rgba(255,255,255,.16);
  border-radius:20px;
  background:rgba(255,255,255,.1);
  color: rgba(246,250,244,.82);
  text-align:center;
  box-shadow: inset 0 1px 0 rgba(255,255,255,.16);
  backdrop-filter: blur(14px);
}

.overview-ledger span {
  display:block;
  color:rgba(239,247,239,.62);
  font-size:12px;
  font-weight:900;
}

.overview-ledger strong {
  display:block;
  margin-top:8px;
  color:#fff;
  font-size:36px;
  line-height:1;
}

.overview-ledger em {
  display: block;
  margin-top: 8px;
  color: rgba(239,247,239,.48);
  font-size: 11px;
  font-style: normal;
  letter-spacing: .08em;
  text-transform: uppercase;
}

.stat-row {
  display:grid;
  grid-template-columns:repeat(4,1fr);
  gap:14px;
  margin-bottom:16px;
}

.student-stat-card {
  position:relative;
  overflow:hidden;
  display:flex;
  align-items:flex-start;
  gap:16px;
  min-height:132px;
  padding:22px;
  border:1px solid rgba(32,39,33,.08);
  border-radius:22px;
  background:
    radial-gradient(circle at 14% 12%, var(--wash), transparent 34%),
    linear-gradient(145deg, rgba(255,255,255,.9), rgba(240,245,238,.62));
  box-shadow:
    0 18px 48px rgba(32,39,33,.08),
    inset 0 1px 0 rgba(255,255,255,.9);
  transition:transform .2s, box-shadow .2s;
}

.student-stat-card::before {
  content:'';
  position:absolute;
  left: 22px;
  right: 22px;
  bottom: 18px;
  height: 3px;
  border-radius: 999px;
  background: linear-gradient(90deg, var(--accent), transparent);
  opacity: .38;
}

.student-stat-card::after {
  content:'';
  position:absolute;
  right:-42px;
  top:-48px;
  width:130px;
  height:130px;
  border-radius:50%;
  background:radial-gradient(circle, color-mix(in srgb, var(--accent) 16%, transparent), transparent 66%);
}

.student-stat-card:hover {
  transform:translateY(-3px);
  box-shadow:
    0 26px 66px rgba(32,39,33,.12),
    inset 0 1px 0 rgba(255,255,255,.95);
}

.student-stat-card.is-clickable {
  cursor: pointer;
}

.student-stat-icon {
  position: relative;
  z-index: 1;
  flex:0 0 auto;
  width:48px;
  height:48px;
  border: 1px solid rgba(255,255,255,.72);
  border-radius:16px;
  display:flex;
  align-items:center;
  justify-content:center;
  background: color-mix(in srgb, var(--wash) 72%, #fff);
  color: var(--accent);
  box-shadow:inset 0 1px 0 rgba(255,255,255,.86);
}

.student-stat-num {
  position: relative;
  z-index: 1;
  font-size:36px;
  font-weight:950;
  color:#202721;
  line-height:1;
}

.student-stat-label {
  position: relative;
  z-index: 1;
  font-size:13px;
  color:rgba(32,39,33,.68);
  margin-top:9px;
  font-weight:900;
}

.student-stat-card small {
  position: relative;
  z-index: 1;
  display:block;
  margin-top:6px;
  color:rgba(32,39,33,.42);
  font-size:12px;
  font-weight:700;
}

.dashboard-grid {
  display:grid;
  grid-template-columns:1.08fr .92fr;
  gap:16px;
}

.student-panel {
  position: relative;
  overflow: hidden;
  padding:20px;
  border:1px solid rgba(32,39,33,.08);
  border-radius:24px;
  background:
    linear-gradient(145deg, rgba(255,255,255,.86), rgba(237,243,235,.58)),
    rgba(255,255,255,.72);
  box-shadow:
    0 20px 54px rgba(32,39,33,.07),
    inset 0 1px 0 rgba(255,255,255,.86);
}

.student-panel::before {
  content: '';
  position: absolute;
  inset: 0 0 auto;
  height: 3px;
  background: linear-gradient(90deg, rgba(32,39,33,.74), rgba(203,166,86,.74), transparent);
  opacity: .64;
}

.student-panel.wide {
  grid-column:1/-1;
}

.panel-head {
  position: relative;
  z-index: 1;
  display:flex;
  align-items:flex-end;
  justify-content:space-between;
  gap:12px;
  margin-bottom:15px;
}

.panel-head span {
  display:block;
  color:rgba(32,39,33,.45);
  font-size:11px;
  font-weight:950;
  letter-spacing:.1em;
  text-transform:uppercase;
}

.panel-head h3 {
  margin:5px 0 0;
  color:#202721;
  font-size:18px;
  font-weight:950;
}

.panel-head small {
  padding: 5px 9px;
  border: 1px solid rgba(32,39,33,.08);
  border-radius: 999px;
  background: rgba(255,255,255,.62);
  color:rgba(32,39,33,.52);
  font-size:12px;
  font-weight:800;
}

.student-table-shell {
  position: relative;
  z-index: 1;
  overflow:hidden;
  border: 1px solid rgba(32,39,33,.07);
  border-radius:18px;
  background: rgba(255,255,255,.52);
}

.student-table-shell :deep(.el-table) {
  --el-table-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: rgba(32,39,33,.035);
  --el-table-border-color: rgba(32,39,33,.07);
  --el-table-row-hover-bg-color: rgba(32,39,33,.035);
  color: rgba(32,39,33,.78);
  background: transparent;
}

.student-table-shell :deep(.el-table th.el-table__cell) {
  color: rgba(32,39,33,.6);
  font-weight: 900;
}

.student-table-shell :deep(.el-table td.el-table__cell),
.student-table-shell :deep(.el-table th.el-table__cell) {
  background: transparent;
}

.student-table-shell :deep(.el-table__empty-text) {
  color: rgba(32,39,33,.42);
}

.st {
  display:inline-block;
  padding:3px 9px;
  border:1px solid rgba(32,39,33,.07);
  border-radius:999px;
  font-size:11px;
  font-weight:800;
}

.st-ok { background:#ECFDF5; color:#057857; }
.st-wait { background:#FFFBEB; color:#A16207; }
.st-done { background:#F1F5F9; color:#64748B; }
.unread-dot { width:7px; height:7px; background:#DC2626; border-radius:50%; display:inline-block; }

@keyframes overview-scan {
  0%, 100% { transform: translateX(-18%); opacity: .2; }
  50% { transform: translateX(18%); opacity: .92; }
}

@keyframes orbit-breathe {
  0%, 100% { transform: translateY(0) scale(1); opacity: .82; }
  50% { transform: translateY(-4px) scale(1.03); opacity: 1; }
}

@media (max-width:1180px) {
  .stat-row,
  .dashboard-grid { grid-template-columns:1fr 1fr; }
  .overview-orbit { display: none; }
}

@media (max-width:720px) {
  .student-overview { align-items:flex-start; flex-direction:column; padding:22px; }
  .student-overview h2 { font-size:22px; }
  .overview-ledger { width: 100%; }
  .stat-row,
  .dashboard-grid { grid-template-columns:1fr; }
  .student-panel.wide { grid-column:auto; }
}

@media (prefers-reduced-motion: reduce) {
  .student-overview::after,
  .overview-orbit {
    animation: none;
  }
}
</style>
