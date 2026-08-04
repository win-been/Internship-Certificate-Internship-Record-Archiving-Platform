<template>
  <div class="app-layout theme-archive" v-if="showWorkspaceLayout">
    <div class="archive-motion" aria-hidden="true">
      <div class="archive-star-stream"></div>
      <div class="archive-ledger-plane"></div>
      <ParticleText :words="particleWords" />
      <div class="archive-chain-orbit">
        <span></span>
        <span></span>
        <span></span>
        <span></span>
      </div>
    </div>
    <aside class="sidebar-wrap">
      <div class="sidebar-brand"><div class="brand-icon"><el-icon :size="20"><DocumentChecked /></el-icon></div><span class="brand-text">实习存证平台</span></div>
      <nav class="sidebar-nav">
        <template v-if="role === 'STUDENT'">
          <router-link to="/student/dashboard" class="nav-item" :class="{ active: isActive('/student/dashboard') }"><el-icon><Odometer /></el-icon><span>我的仪表盘</span></router-link>
          <router-link to="/student/identity" class="nav-item" :class="{ active: isActive('/student/identity') }"><el-icon><UserFilled /></el-icon><span>实名认证</span></router-link>
          <router-link to="/student/jobs" class="nav-item" :class="{ active: isActive('/student/jobs') }"><el-icon><Briefcase /></el-icon><span>实习择业</span></router-link>
          <router-link to="/student/internship" class="nav-item" :class="{ active: isActive('/student/internship') }"><el-icon><Document /></el-icon><span>实习备案</span></router-link>
          <router-link to="/student/reports" class="nav-item" :class="{ active: isActive('/student/reports') }"><el-icon><Notebook /></el-icon><span>日常填报</span></router-link>
          <router-link to="/student/assessment" class="nav-item" :class="{ active: isActive('/student/assessment') }"><el-icon><DataAnalysis /></el-icon><span>考核查看</span></router-link>
          <router-link to="/student/graduate" class="nav-item" :class="{ active: isActive('/student/graduate') }"><el-icon><Stamp /></el-icon><span>毕业申领</span></router-link>
          <router-link to="/student/evidence" class="nav-item" :class="{ active: isActive('/student/evidence') }"><el-icon><CircleCheck /></el-icon><span>存证中心</span></router-link>
          <router-link to="/student/ai" class="nav-item" :class="{ active: isActive('/student/ai') }"><el-icon><ChatDotRound /></el-icon><span>AI 助手</span></router-link>
        </template>
        <template v-else-if="isSchool">
          <router-link to="/school/dashboard" class="nav-item" :class="{ active: isActive('/school/dashboard') }"><el-icon><Odometer /></el-icon><span>数据看板</span></router-link>
          <router-link to="/school/students" class="nav-item" :class="{ active: isActive('/school/students') }"><el-icon><User /></el-icon><span>学生名单</span></router-link>
          <router-link to="/school/enterprises" class="nav-item" :class="{ active: isActive('/school/enterprises') }"><el-icon><OfficeBuilding /></el-icon><span>校企管控</span></router-link>
          <router-link to="/school/approvals" class="nav-item" :class="{ active: isActive('/school/approvals') }"><el-icon><Checked /></el-icon><span>审核中心</span></router-link>
          <router-link to="/school/inspection" class="nav-item" :class="{ active: isActive('/school/inspection') }"><el-icon><Search /></el-icon><span>过程巡检</span></router-link>
          <router-link to="/school/verify" class="nav-item" :class="{ active: isActive('/school/verify') }"><el-icon><CircleCheck /></el-icon><span>存证核验</span></router-link>
          <router-link to="/school/notices" class="nav-item" :class="{ active: isActive('/school/notices') }"><el-icon><Notification /></el-icon><span>公告下发</span></router-link>
          <router-link to="/school/ai" class="nav-item" :class="{ active: isActive('/school/ai') }"><el-icon><ChatDotRound /></el-icon><span>AI 助手</span></router-link>
        </template>
        <template v-else-if="isEnterprise">
          <router-link to="/enterprise/dashboard" class="nav-item" :class="{ active: isActive('/enterprise/dashboard') }"><el-icon><Odometer /></el-icon><span>数据看板</span></router-link>
          <router-link to="/enterprise/register" class="nav-item" :class="{ active: isActive('/enterprise/register') }"><el-icon><OfficeBuilding /></el-icon><span>企业资质管理</span></router-link>
          <router-link to="/enterprise/jobs" class="nav-item" :class="{ active: isActive('/enterprise/jobs') }"><el-icon><Briefcase /></el-icon><span>岗位管理</span></router-link>
          <router-link to="/enterprise/recruit" class="nav-item" :class="{ active: isActive('/enterprise/recruit') }"><el-icon><User /></el-icon><span>录用管理</span></router-link>
          <router-link to="/enterprise/assessment" class="nav-item" :class="{ active: isActive('/enterprise/assessment') }"><el-icon><DataAnalysis /></el-icon><span>过程考核</span></router-link>
          <router-link to="/enterprise/disputes" class="nav-item" :class="{ active: isActive('/enterprise/disputes') }"><el-icon><Warning /></el-icon><span>纠纷处理</span><span class="nav-dot" v-if="badges.disputes>0">{{ badges.disputes }}</span></router-link>
          <router-link to="/enterprise/archive" class="nav-item" :class="{ active: isActive('/enterprise/archive') }"><el-icon><FolderOpened /></el-icon><span>存证档案管理</span></router-link>
          <router-link to="/enterprise/ai" class="nav-item" :class="{ active: isActive('/enterprise/ai') }"><el-icon><ChatDotRound /></el-icon><span>AI 助手</span></router-link>
        </template>
        <template v-else-if="role === 'PLATFORM_ADMIN'">
          <router-link to="/platform/dashboard" class="nav-item" :class="{ active: isActive('/platform/dashboard') }"><el-icon><Odometer /></el-icon><span>全局大盘</span></router-link>
          <router-link to="/platform/permissions" class="nav-item" :class="{ active: isActive('/platform/permissions') }"><el-icon><Lock /></el-icon><span>权限管控</span></router-link>
          <router-link to="/platform/approvals" class="nav-item" :class="{ active: isActive('/platform/approvals') }"><el-icon><Checked /></el-icon><span>企业资质审核</span></router-link>
          <router-link to="/platform/chain" class="nav-item" :class="{ active: isActive('/platform/chain') }"><el-icon><Link /></el-icon><span>链上配置</span></router-link>
          <router-link to="/platform/config" class="nav-item" :class="{ active: isActive('/platform/config') }"><el-icon><Setting /></el-icon><span>系统配置</span></router-link>
          <router-link to="/platform/verify" class="nav-item" :class="{ active: isActive('/platform/verify') }"><el-icon><CircleCheck /></el-icon><span>公共核验</span></router-link>
          <router-link to="/platform/ai" class="nav-item" :class="{ active: isActive('/platform/ai') }"><el-icon><ChatDotRound /></el-icon><span>AI 助手</span></router-link>
        </template>
      </nav>
      <div class="sidebar-user">
        <div class="s-user-avatar"><el-icon :size="18"><User /></el-icon></div>
        <div class="s-user-info"><span class="s-user-name">{{ currentUserName }}</span><span class="s-user-role">{{ roleLabel }}</span></div>
        <el-dropdown trigger="click" @command="handleUserCmd">
          <span class="s-user-caret"><el-icon :size="14"><ArrowDown /></el-icon></span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人信息</el-dropdown-item>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </aside>

    <main class="main-wrap">
      <header class="top-bar">
        <div v-if="backendOnline" class="sync-hint" @click="syncToBackend" :title="syncing?'同步中...':'点击同步本地数据到后端'">
          <el-icon :size="14" :class="{spin:syncing}"><Promotion /></el-icon>
          <span v-if="!syncing">已连接后端</span>
          <span v-else>同步中...</span>
        </div>
        <div class="top-left"><span class="top-logo">实习存证平台</span></div>
        <div class="top-center"><el-input v-model="globalSearch" placeholder="检索学生 / 岗位 / 存证哈希..." prefix-icon="Search" clearable class="global-search"/></div>
        <div class="top-right">
          <span class="top-bell" @click="goMessages"><el-icon :size="20"><Bell /></el-icon><span class="bell-dot" v-if="notifyCount>0">{{ notifyCount }}</span></span>
          <span class="top-avatar"><el-icon :size="22"><User /></el-icon></span>
        </div>
      </header>
      <section class="workspace-hero" aria-label="可信工作区状态">
        <div class="workspace-copy">
          <p><span></span> 可信档案工作区</p>
          <h1>{{ workspaceTitle }}</h1>
          <small>当前角色：{{ roleLabel }} · 备案、审核、归档与链上核验保持同一条可信记录。</small>
        </div>
        <div class="workspace-proof">
          <div class="proof-main">
            <div class="proof-icon"><el-icon><DocumentChecked /></el-icon></div>
            <div>
              <strong>98%</strong>
              <span>归档可靠性</span>
            </div>
          </div>
          <div class="proof-track"><span></span></div>
          <div class="proof-pills">
            <span><i></i> 运行中</span>
            <span>链上待核验</span>
          </div>
        </div>
      </section>
      <div class="breadcrumb-row"><span v-for="(bc,i) in breadcrumbs" :key="i"><span v-if="i>0" class="bc-sep">/</span><span class="bc-item" :class="{bcLast:i===breadcrumbs.length-1}">{{bc.label}}</span></span></div>
      <div class="page-container">
        <router-view />
      </div>
    </main>

    <el-drawer v-model="showAi" direction="rtl" size="400px" title="AI 助手">
      <div class="ai-panel">
        <div class="ai-msgs"><div v-for="(m,i) in aiMsgs" :key="i" class="ai-msg" :class="'ai-'+m.role"><span>{{ m.text }}</span></div></div>
        <div class="ai-input-row"><el-input v-model="aiInput" placeholder="输入问题..." @keyup.enter="sendAi"/><el-button type="primary" size="small" @click="sendAi">发送</el-button></div>
      </div>
    </el-drawer>

    <el-dialog v-model="profileDialogVisible" title="个人信息" width="560px" class="theme-archive-dialog account-dialog" append-to-body>
      <div class="account-hero" v-loading="profileLoading">
        <div class="account-avatar">{{ profileInitial }}</div>
        <div class="account-head">
          <strong>{{ profileForm.realName || userStore.userInfo?.realName || '用户' }}</strong>
          <span>{{ roleLabel }} · {{ profileForm.organizationName || '实习存证平台' }}</span>
        </div>
        <div class="account-status"><i></i>{{ profileForm.status || 'APPROVED' }}</div>
      </div>
      <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="86px" class="account-form">
        <el-form-item label="用户名">
          <el-input v-model="profileForm.username" disabled />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="profileForm.realName" maxlength="30" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :model-value="roleLabel" disabled />
        </el-form-item>
        <el-form-item label="单位名称">
          <el-input v-model="profileForm.organizationName" maxlength="80" placeholder="请输入学校或企业名称" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="profileForm.phone" maxlength="20" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="profileForm.email" maxlength="100" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="钱包地址">
          <el-input v-model="profileForm.walletAddress" disabled placeholder="暂无链上地址" />
        </el-form-item>
      </el-form>
      <div class="account-note"><el-icon><CircleCheck /></el-icon><span>资料保存后会同步更新用户画像存证，并提交链上归档记录。</span></div>
      <template #footer>
        <el-button @click="profileDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存信息</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="500px" class="theme-archive-dialog account-dialog" append-to-body @closed="resetPasswordForm">
      <div class="password-hero">
        <div class="password-lock"><el-icon><Lock /></el-icon></div>
        <div>
          <strong>账户安全校验</strong>
          <span>修改成功后需要重新登录，以刷新本次会话。</span>
        </div>
      </div>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-width="92px" class="account-form">
        <el-form-item label="当前密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password autocomplete="current-password" placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" placeholder="6-20位新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" placeholder="再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible=false">取消</el-button>
        <el-button type="primary" :loading="changingPassword" @click="changePassword">确认修改</el-button>
      </template>
    </el-dialog>

    <div class="float-ai" @click="showAi=true"><el-icon :size="24"><ChatDotRound /></el-icon></div>
  </div>
  <router-view v-else />
</template>

<script setup>
import { ref, computed, onMounted, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from './stores/userStore'
import { useDataStore } from './stores/dataStore'
import ParticleText from './components/ParticleText.vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DocumentChecked, Odometer, UserFilled, Briefcase, Document, Notebook, DataAnalysis, Stamp, CircleCheck, Bell, ChatDotRound, User, OfficeBuilding, Checked, Search, Notification, Lock, Link, Setting, FolderOpened, ArrowDown, SwitchButton, Close, Warning, Promotion, MoreFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const dataStore = useDataStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const showWorkspaceLayout = computed(() => isLoggedIn.value && route.meta.requiresAuth)
const role = computed(() => userStore.userInfo?.role || '')
const isSchool = computed(() => role.value === 'SCHOOL_ADMIN')
const isEnterprise = computed(() => role.value === 'ENTERPRISE_HR' || role.value === 'ENTERPRISE_MENTOR')
const roleLabel = computed(() => {
  const r = role.value
  if (r === 'STUDENT') return '学生'
  if (r === 'SCHOOL_ADMIN') return '学校管理'
  if (r === 'ENTERPRISE_HR') return '企业HR'
  if (r === 'ENTERPRISE_MENTOR') return '企业导师'
  if (r === 'PLATFORM_ADMIN') return '平台管理'
  return '用户'
})
const badText = (value) => !value || /^\?+$/.test(String(value).trim())
const currentUserName = computed(() => {
  const user = userStore.userInfo || {}
  return badText(user.realName) ? (user.organizationName || user.username || '用户') : user.realName
})

const isActive = (path) => route.path === path
const globalSearch = ref('')
const notifyCount = computed(() => (dataStore.messages || []).filter(m => !m.read).length)
const showAi = ref(false); const aiInput = ref(''); const aiMsgs = ref([])
const particleWords = ['ARCHIVE', 'VERIFY', 'ON CHAIN', 'CERTIFY']
const goMessages = () => { 
  const r = userStore.userInfo?.role
  if (r === 'STUDENT') router.push('/student/dashboard')
  else if (r === 'SCHOOL_ADMIN') router.push('/school/notices')
  else if (r === 'ENTERPRISE_HR' || r === 'ENTERPRISE_MENTOR') router.push('/enterprise/dashboard')
  else ElMessage.info('暂无消息中心')
}

const sendAi = () => {
  if (!aiInput.value.trim()) return
  aiMsgs.value.push({ role: 'user', text: aiInput.value })
  setTimeout(() => { aiMsgs.value.push({ role: 'bot', text: '正在查询相关信息，请稍候...' }) }, 400)
  aiInput.value = ''
}

const breadcrumbMap = {
  '/student/dashboard':[{label:'学生端'},{label:'我的仪表盘'}],'/student/identity':[{label:'学生端'},{label:'实名认证'}],'/student/jobs':[{label:'学生端'},{label:'实习择业'}],'/student/internship':[{label:'学生端'},{label:'实习备案'}],'/student/reports':[{label:'学生端'},{label:'日常填报'}],'/student/assessment':[{label:'学生端'},{label:'考核查看'}],'/student/graduate':[{label:'学生端'},{label:'毕业申领'}],'/student/evidence':[{label:'学生端'},{label:'存证中心'}],'/student/messages':[{label:'学生端'},{label:'系统消息'}],'/student/ai':[{label:'学生端'},{label:'AI 助手'}],
  '/school/dashboard':[{label:'学校管理'},{label:'数据看板'}],'/school/students':[{label:'学校管理'},{label:'学生名单'}],'/school/enterprises':[{label:'学校管理'},{label:'校企管控'}],'/school/approvals':[{label:'学校管理'},{label:'审核中心'}],'/school/inspection':[{label:'学校管理'},{label:'过程巡检'}],'/school/verify':[{label:'学校管理'},{label:'存证核验'}],'/school/notices':[{label:'学校管理'},{label:'公告下发'}],'/school/ai':[{label:'学校管理'},{label:'AI 助手'}],
  '/enterprise/dashboard':[{label:'企业管理'},{label:'数据看板'}],'/enterprise/register':[{label:'企业管理'},{label:'企业资质管理'}],'/enterprise/jobs':[{label:'企业管理'},{label:'岗位管理'}],'/enterprise/recruit':[{label:'企业管理'},{label:'录用管理'}],'/enterprise/assessment':[{label:'企业管理'},{label:'过程考核'}],'/enterprise/disputes':[{label:'企业管理'},{label:'纠纷处理'}],'/enterprise/archive':[{label:'企业管理'},{label:'存证档案管理'}],'/enterprise/ai':[{label:'企业管理'},{label:'AI 助手'}],
  '/platform/dashboard':[{label:'平台管理'},{label:'全局大盘'}],'/platform/permissions':[{label:'平台管理'},{label:'权限管控'}],'/platform/approvals':[{label:'平台管理'},{label:'企业资质审核'}],'/platform/chain':[{label:'平台管理'},{label:'链上配置'}],'/platform/config':[{label:'平台管理'},{label:'系统配置'}],'/platform/verify':[{label:'平台管理'},{label:'公共核验'}],'/platform/ai':[{label:'平台管理'},{label:'AI 助手'}]
}
const breadcrumbs = computed(() => breadcrumbMap[route.path] || [])
const workspaceTitle = computed(() => breadcrumbs.value.at(-1)?.label || '可信工作台')

const profileDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const profileLoading = ref(false)
const savingProfile = ref(false)
const changingPassword = ref(false)
const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const profileForm = reactive({
  username: '',
  realName: '',
  role: '',
  organizationName: '',
  phone: '',
  email: '',
  walletAddress: '',
  status: ''
})
const passwordForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })
const profileInitial = computed(() => (profileForm.realName || userStore.userInfo?.realName || userStore.userInfo?.username || '用').slice(0, 1))

const profileRules = {
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  phone: [{ pattern: /^$|^1\d{10}$|^[0-9+\-\s()]{6,20}$/, message: '请输入有效手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入有效邮箱', trigger: 'blur' }]
}
const validateConfirmPassword = (rule, value, callback) => {
  if (!value) callback(new Error('请再次输入新密码'))
  else if (value !== passwordForm.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为6-20位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

const fillProfileForm = (data = {}) => {
  const read = (key, fallback = '') =>
    Object.prototype.hasOwnProperty.call(data, key) ? (data[key] ?? '') : (fallback ?? '')
  profileForm.username = read('username', userStore.userInfo?.username)
  profileForm.realName = read('realName', userStore.userInfo?.realName)
  profileForm.role = read('role', userStore.userInfo?.role)
  profileForm.organizationName = read('organizationName', userStore.userInfo?.organizationName)
  profileForm.phone = read('phone', userStore.userInfo?.phone)
  profileForm.email = read('email', userStore.userInfo?.email)
  profileForm.walletAddress = read('walletAddress', userStore.userInfo?.walletAddress)
  profileForm.status = read('status', userStore.userInfo?.status || 'APPROVED')
}

const openProfileDialog = async () => {
  fillProfileForm()
  profileDialogVisible.value = true
  profileLoading.value = true
  try {
    const api = (await import('./api/request')).default
    const res = await api.get('/auth/me')
    fillProfileForm(res.data || {})
    userStore.updateUserInfo(res.data || {})
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '个人信息加载失败')
  } finally {
    profileLoading.value = false
  }
}

const saveProfile = async () => {
  await profileFormRef.value?.validate()
  savingProfile.value = true
  try {
    const payload = {
      realName: profileForm.realName,
      organizationName: profileForm.organizationName,
      phone: profileForm.phone,
      email: profileForm.email
    }
    const api = (await import('./api/request')).default
    const res = await api.put('/auth/me', payload)
    userStore.updateUserInfo(res.data || payload)
    fillProfileForm(res.data || payload)
    profileDialogVisible.value = false
    ElMessage.success('个人信息已保存')
  } catch (e) {
    if (e !== false) ElMessage.error(e?.response?.data?.message || e?.message || '保存失败')
  } finally {
    savingProfile.value = false
  }
}

const resetPasswordForm = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

const performLogout = async (message = '已退出') => {
  showAi.value = false
  profileDialogVisible.value = false
  passwordDialogVisible.value = false
  userStore.logout()
  try {
    await router.replace('/login')
  } catch (e) {
    // ignore duplicate navigation while already on login page
  }
  ElMessage.closeAll()
  ElMessage.success(message)
}

const changePassword = async () => {
  await passwordFormRef.value?.validate()
  changingPassword.value = true
  try {
    const api = (await import('./api/request')).default
    await api.put('/auth/me/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    passwordDialogVisible.value = false
    await performLogout('密码已修改，请重新登录')
  } catch (e) {
    if (e !== false) ElMessage.error(e?.response?.data?.message || e?.message || '修改失败')
  } finally {
    changingPassword.value = false
  }
}

const handleUserCmd = (cmd) => {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？','提示',{confirmButtonText:'确定',cancelButtonText:'取消',type:'warning'}).then(()=>{
      performLogout()
    }).catch(()=>{})
  } else if (cmd === 'password') {
    passwordDialogVisible.value = true
  } else if (cmd === 'profile') {
    openProfileDialog()
  }
}

const badges = computed(() => {
  const companyId = userStore.userInfo?.userId || null
  return {
    assessment: (dataStore.assessments || []).filter(a => a.companyId === companyId && a.status !== 'Completed').length,
    disputes: (dataStore.disputes || []).filter(d => d.companyId === companyId && d.status === 'PENDING').length
  }
})
const backendOnline = ref(false)
const syncing = ref(false)

// Detect backend availability
const checkBackend = async () => {
  if (!userStore.token) {
    backendOnline.value = false
    return false
  }
  try {
    const api = (await import('./api/request')).default
    const res = await api.get('/data/jobs', { timeout: 3000 })
    backendOnline.value = true
    return true
  } catch (e) {
    backendOnline.value = false
    return false
  }
}

// Sync all localStorage data to backend
const syncToBackend = async () => {
  syncing.value = true
  try {
    const raw = localStorage.getItem('platform_data')
    if (!raw) { ElMessage.info('暂无本地数据需要同步'); return }
    const data = JSON.parse(raw)
    const api = (await import('./api/request')).default
    let count = 0
    
    // Sync jobs
    if (data.jobs) for (const j of data.jobs) {
      try { await api.post('/data/jobs', j); count++ } catch(e) {}
    }
    // Sync applications
    if (data.applications) for (const a of data.applications) {
      try { await api.post('/data/applications', a); count++ } catch(e) {}
    }
    // Sync assessments
    if (data.assessments) for (const a of data.assessments) {
      try { await api.post('/data/assessments', a); count++ } catch(e) {}
    }
    // Sync archives
    if (data.archives) for (const a of data.archives) {
      try { await api.post('/data/archives', a); count++ } catch(e) {}
    }
    // Sync disputes
    if (data.disputes) for (const d of data.disputes) {
      try { await api.post('/data/disputes', d); count++ } catch(e) {}
    }
    ElMessage.success('已同步 ' + count + ' 条数据到后端')
  } catch (e) {
    ElMessage.error('同步失败: ' + (e.message || '后端不可用'))
  } finally { syncing.value = false }
}

onMounted(() => { checkBackend() })

</script>

<style scoped>
.app-layout {
  position: relative;
  display: flex;
  height: 100vh;
  overflow: hidden;
  isolation: isolate;
  color: #1f2923;
  background:
    radial-gradient(circle at 52% 76%, rgba(190, 203, 185, 0.5), transparent 20%),
    linear-gradient(180deg, #fbfbfa 0%, #f5f5f3 58%, #ecefea 100%);
}

.app-layout::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: -2;
  background:
    radial-gradient(circle at 48% 18%, rgba(222, 226, 222, 0.78), transparent 18%),
    linear-gradient(rgba(30, 36, 32, 0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(30, 36, 32, 0.026) 1px, transparent 1px);
  background-size: auto, 68px 68px, 68px 68px;
}

.app-layout::after {
  content: '';
  position: absolute;
  inset: auto 0 0;
  z-index: -1;
  height: 34vh;
  background: linear-gradient(180deg, transparent, rgba(225, 230, 224, 0.72));
}

.archive-motion {
  position: absolute;
  inset: 0;
  z-index: 0;
  overflow: hidden;
  pointer-events: none;
  perspective: 900px;
}

.archive-star-stream,
.archive-ledger-plane,
.archive-chain-orbit,
.particle-text-canvas {
  position: absolute;
  inset: 0;
}

.particle-text-canvas {
  inset: 7% 4% auto auto;
  width: min(58vw, 860px);
  height: min(28vh, 290px);
  opacity: 0.1;
  mix-blend-mode: multiply;
  filter: grayscale(1) contrast(0.6);
  mask-image: linear-gradient(90deg, transparent, #000 18%, #000 82%, transparent);
}

.archive-star-stream {
  opacity: 0.18;
  background-image:
    radial-gradient(circle, rgba(46, 55, 49, 0.2) 0 1px, transparent 1.8px),
    radial-gradient(circle, rgba(46, 55, 49, 0.12) 0 1px, transparent 2px);
  background-position: 0 0, 42px 58px;
  background-size: 150px 150px, 230px 230px;
  animation: archiveDrift 28s linear infinite;
}

.archive-ledger-plane {
  inset: auto -12% -18%;
  height: 52vh;
  opacity: 0.16;
  background:
    linear-gradient(rgba(46, 55, 49, 0.16) 1px, transparent 1px),
    linear-gradient(90deg, rgba(46, 55, 49, 0.12) 1px, transparent 1px);
  background-size: 86px 34px;
  transform: rotateX(64deg) translateY(8%);
  transform-origin: 50% 100%;
  animation: ledgerFloat 12s ease-in-out infinite alternate;
}

.archive-chain-orbit {
  inset: 10% 6% auto auto;
  width: min(36vw, 520px);
  height: min(36vw, 520px);
  border: 1px solid rgba(46, 55, 49, 0.08);
  border-radius: 50%;
  opacity: 0.36;
  transform-style: preserve-3d;
  animation: chainOrbit 22s linear infinite;
}

.archive-chain-orbit span {
  position: absolute;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a49a;
  box-shadow: 0 0 18px rgba(46, 55, 49, 0.16);
}

.archive-chain-orbit span:nth-child(1) { top: 6%; left: 49%; }
.archive-chain-orbit span:nth-child(2) { top: 49%; right: 6%; background: #c9d2c9; }
.archive-chain-orbit span:nth-child(3) { bottom: 8%; left: 48%; }
.archive-chain-orbit span:nth-child(4) { top: 48%; left: 6%; background: #c9d2c9; }

.sidebar-wrap {
  width: 246px;
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  z-index: 10;
  margin: 18px 0 18px 18px;
  border: 1px solid rgba(32, 39, 33, 0.09);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.82), rgba(255, 255, 255, 0.56)),
    rgba(236, 242, 235, 0.72);
  box-shadow: 0 24px 70px rgba(32, 39, 33, 0.1);
  backdrop-filter: blur(24px) saturate(1.04);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 22px 18px 18px;
  border-bottom: 1px solid rgba(32, 39, 33, 0.08);
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(32, 39, 33, 0.1);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  color: #2b342e;
  box-shadow: 0 14px 26px rgba(32, 39, 33, 0.08);
}

.brand-text {
  color: #202721;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0;
  text-shadow: none;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 12px;
  overflow-y: auto;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 42px;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 14px;
  color: rgba(32, 39, 33, 0.62);
  font-size: 14px;
  text-decoration: none;
  transition: border-color 0.18s, background 0.18s, color 0.18s, transform 0.18s;
}

.nav-item:hover {
  border-color: rgba(32, 39, 33, 0.08);
  background: rgba(255, 255, 255, 0.68);
  color: #202721;
  transform: translateX(2px);
}

.nav-item.active {
  border-color: rgba(32, 39, 33, 0.1);
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.86), rgba(236, 242, 235, 0.74)),
    rgba(255, 255, 255, 0.68);
  color: #202721;
  font-weight: 700;
  box-shadow:
    0 12px 28px rgba(32, 39, 33, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.nav-item .el-icon {
  flex-shrink: 0;
  color: currentColor;
  font-size: 18px;
}

.nav-dot {
  position: absolute;
  right: 12px;
  top: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #202721;
  color: #fff;
  font-size: 11px;
  font-weight: 800;
  transform: translateY(-50%);
}

.sidebar-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px 18px;
  border-top: 1px solid rgba(32, 39, 33, 0.08);
}

.s-user-avatar,
.top-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid rgba(32, 39, 33, 0.1);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.72);
  color: #202721;
}

.s-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.s-user-name {
  color: #202721;
  font-size: 13px;
  font-weight: 700;
}

.s-user-role {
  color: rgba(32, 39, 33, 0.52);
  font-size: 11px;
}

.s-user-caret {
  color: rgba(32, 39, 33, 0.58);
  cursor: pointer;
  padding: 4px;
}

.main-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
  z-index: 2;
  background: transparent !important;
}

.top-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 64px;
  margin: 16px 18px 0;
  padding: 12px 18px;
  border: 1px solid rgba(32, 39, 33, 0.09);
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.84), rgba(255, 255, 255, 0.55)),
    rgba(236, 242, 235, 0.68);
  box-shadow: 0 18px 48px rgba(32, 39, 33, 0.08);
  backdrop-filter: blur(22px) saturate(1.04);
}

.top-left {
  display: flex;
  align-items: center;
}

.top-logo {
  color: #202721;
  font-size: 18px;
  font-weight: 800;
}

.top-center {
  flex: 1;
  max-width: 480px;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-left: auto;
}

.top-bell {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid rgba(32, 39, 33, 0.1);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.68);
  color: rgba(32, 39, 33, 0.74);
  cursor: pointer;
}

.bell-dot {
  position: absolute;
  top: -4px;
  right: -6px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #202721;
  color: #fff;
  font-size: 10px;
  font-weight: 800;
}

.global-search :deep(.el-input__wrapper) {
  height: 40px;
  border-color: rgba(32, 39, 33, 0.09) !important;
  border-radius: 14px !important;
  background: rgba(255, 255, 255, 0.68) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.78) !important;
}

.global-search :deep(.el-input__wrapper:hover),
.global-search :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(32, 39, 33, 0.2) !important;
  box-shadow: 0 0 0 3px rgba(32, 39, 33, 0.08) !important;
}

.global-search :deep(.el-input__inner) {
  color: #202721 !important;
}

.global-search :deep(.el-input__inner::placeholder) {
  color: rgba(32, 39, 33, 0.42) !important;
}

.breadcrumb-row {
  padding: 10px 32px 0;
  color: rgba(32, 39, 33, 0.52);
  font-size: 12px;
}

.bc-sep {
  margin: 0 7px;
  color: rgba(32, 39, 33, 0.28);
}

.bcLast {
  color: #202721;
  font-weight: 700;
}

.workspace-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 310px);
  gap: 14px;
  margin: 12px 18px 0;
  padding: 16px 18px;
  overflow: hidden;
  border: 1px solid rgba(32, 39, 33, 0.09);
  border-radius: 24px;
  background:
    radial-gradient(circle at 70% 86%, rgba(190, 203, 185, 0.28), transparent 24%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(255, 255, 255, 0.54)),
    rgba(236, 242, 235, 0.76);
  box-shadow:
    0 18px 48px rgba(32, 39, 33, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(24px) saturate(1.04);
}

.workspace-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(115deg, rgba(255, 255, 255, 0.72), transparent 36%),
    linear-gradient(90deg, transparent, rgba(32, 39, 33, 0.04), transparent);
  pointer-events: none;
}

.workspace-copy,
.workspace-proof {
  position: relative;
  z-index: 1;
}

.workspace-copy {
  min-width: 0;
}

.workspace-copy p {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 7px;
  color: rgba(32, 39, 33, 0.52);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.workspace-copy p span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #202721;
  box-shadow: 0 0 14px rgba(32, 39, 33, 0.18);
}

.workspace-copy h1 {
  margin: 0;
  color: #202721;
  font-family: Georgia, 'Times New Roman', serif;
  font-size: clamp(24px, 2vw, 34px);
  font-weight: 500;
  line-height: 1;
  letter-spacing: 0;
  text-shadow: none;
}

.workspace-copy small {
  display: block;
  margin-top: 8px;
  color: rgba(32, 39, 33, 0.58);
  font-size: 12px;
  line-height: 1.6;
}

.workspace-proof {
  align-self: stretch;
  padding: 13px;
  border: 1px solid rgba(32, 39, 33, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.64);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.8);
}

.proof-main {
  display: flex;
  align-items: center;
  gap: 12px;
}

.proof-icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 1px solid rgba(32, 39, 33, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  color: #202721;
}

.proof-main strong {
  display: block;
  color: #202721;
  font-size: 26px;
  font-weight: 950;
  line-height: 1;
}

.proof-main span {
  display: block;
  margin-top: 4px;
  color: rgba(32, 39, 33, 0.52);
  font-size: 12px;
  font-weight: 700;
}

.proof-track {
  height: 7px;
  margin-top: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: rgba(32, 39, 33, 0.1);
}

.proof-track span {
  display: block;
  width: 98%;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #202721, rgba(32, 39, 33, 0.46));
  animation: proofLoad 1s ease-out both;
}

.proof-pills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.proof-pills span {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 24px;
  padding: 0 10px;
  border: 1px solid rgba(32, 39, 33, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  color: rgba(32, 39, 33, 0.72);
  font-size: 11px;
  font-weight: 900;
}

.proof-pills i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #202721;
  box-shadow: 0 0 12px rgba(32, 39, 33, 0.18);
}

.page-container {
  flex: 1;
  overflow-y: auto;
  margin: 12px 18px 18px;
  padding: 20px 24px 28px;
  border: 1px solid rgba(32, 39, 33, 0.06);
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.38), rgba(236, 242, 235, 0.22)),
    rgba(255, 255, 255, 0.18) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.56);
  backdrop-filter: blur(12px);
  scrollbar-color: rgba(255, 255, 255, 0.22) transparent;
}

.page-container::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.22);
}

.float-ai {
  position: fixed;
  right: 28px;
  bottom: 28px;
  z-index: 100;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border: 1px solid rgba(255, 214, 120, 0.42);
  border-radius: 50%;
  background: linear-gradient(135deg, #f6c869, #f9973e);
  color: #1c160c;
  box-shadow: 0 18px 42px rgba(249, 151, 62, 0.34);
  cursor: pointer;
  transition: transform 0.18s, box-shadow 0.18s;
}

.float-ai:hover {
  transform: translateY(-2px) scale(1.04);
  box-shadow: 0 24px 52px rgba(249, 151, 62, 0.42);
}

.account-hero,
.password-hero {
  position: relative;
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 86px;
  margin-bottom: 20px;
  padding: 18px;
  overflow: hidden;
  border: 1px solid rgba(32, 39, 33, 0.08);
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(236, 242, 235, 0.62)),
    radial-gradient(circle at 84% 24%, rgba(255, 214, 120, 0.28), transparent 32%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.account-hero::after,
.password-hero::after {
  content: '';
  position: absolute;
  inset: auto -18% -52% 35%;
  height: 120px;
  background:
    linear-gradient(rgba(32, 39, 33, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(32, 39, 33, 0.06) 1px, transparent 1px);
  background-size: 26px 18px;
  transform: rotateX(62deg) rotateZ(-5deg);
  opacity: 0.45;
}

.account-avatar,
.password-lock {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  border: 1px solid rgba(32, 39, 33, 0.1);
  border-radius: 18px;
  background: #202721;
  color: #fff;
  font-size: 22px;
  font-weight: 800;
  box-shadow: 0 16px 34px rgba(32, 39, 33, 0.16);
}

.account-head,
.password-hero > div:last-child {
  position: relative;
  z-index: 1;
  display: flex;
  flex: 1;
  min-width: 0;
  flex-direction: column;
}

.account-head strong,
.password-hero strong {
  color: #202721;
  font-size: 18px;
  font-weight: 800;
}

.account-head span,
.password-hero span {
  margin-top: 4px;
  color: rgba(32, 39, 33, 0.58);
  font-size: 12px;
}

.account-status {
  position: relative;
  z-index: 1;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border: 1px solid rgba(32, 39, 33, 0.08);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.7);
  color: rgba(32, 39, 33, 0.72);
  font-size: 12px;
  font-weight: 700;
}

.account-status i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #16a34a;
  box-shadow: 0 0 0 4px rgba(22, 163, 74, 0.12);
}

.account-form {
  padding: 2px 4px 0;
}

.account-note {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 2px;
  padding: 12px 14px;
  border: 1px solid rgba(32, 39, 33, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.56);
  color: rgba(32, 39, 33, 0.62);
  font-size: 12px;
}

.account-note .el-icon {
  color: #202721;
}

.sync-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border: 1px solid rgba(92, 255, 189, 0.22);
  border-radius: 999px;
  background: rgba(92, 255, 189, 0.1);
  color: #a9ffd8;
  cursor: pointer;
  font-size: 12px;
}

.sync-hint:hover {
  background: rgba(92, 255, 189, 0.16);
}

.ai-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.ai-msgs {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.ai-msg {
  margin-bottom: 10px;
  padding: 9px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
}

.ai-user {
  background: rgba(37, 99, 235, 0.12);
  color: #2563eb;
  text-align: right;
}

.ai-bot {
  background: #f8fafc;
  color: #334155;
}

.ai-input-row {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.spin {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

@keyframes archiveDrift {
  from { background-position: 0 0, 42px 58px; }
  to { background-position: 150px 300px, -188px 288px; }
}

@keyframes ledgerFloat {
  from { transform: rotateX(64deg) translateY(8%); }
  to { transform: rotateX(61deg) translateY(3%); }
}

@keyframes chainOrbit {
  from { transform: rotateX(58deg) rotateZ(0deg); }
  to { transform: rotateX(58deg) rotateZ(360deg); }
}

@keyframes archiveSheen {
  from { transform: translateX(-36%); }
  to { transform: translateX(36%); }
}

@keyframes chainPulse {
  0%, 100% { box-shadow: 0 0 0 rgba(255, 214, 120, 0); }
  50% { box-shadow: 0 0 18px rgba(255, 214, 120, 0.26); }
}

@keyframes proofLoad {
  from { width: 0; }
  to { width: 98%; }
}

.theme-archive :deep(.page),
.theme-archive :deep(.dashboard) {
  color: rgba(237, 247, 255, 0.88);
}

.theme-archive :deep(.page-head h1),
.theme-archive :deep(.dash-title) {
  margin: 0;
  color: #fff !important;
  font-size: 26px !important;
  letter-spacing: 0 !important;
  text-shadow: 0 10px 34px rgba(0, 0, 0, 0.32);
}

.theme-archive :deep(.page-sub),
.theme-archive :deep(.page-desc),
.theme-archive :deep(.dash-updated),
.theme-archive :deep(.section-title),
.theme-archive :deep(.chart-name),
.theme-archive :deep(.chart-head),
.theme-archive :deep(.card-head) {
  color: rgba(226, 242, 250, 0.72) !important;
}

.theme-archive :deep(.stat-card),
.theme-archive :deep(.sc),
.theme-archive :deep(.card),
.theme-archive :deep(.chart-card),
.theme-archive :deep(.table-card),
.theme-archive :deep(.form-card),
.theme-archive :deep(.gen-result) {
  border: 1px solid rgba(255, 255, 255, 0.16) !important;
  border-radius: 18px !important;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.16), rgba(255, 255, 255, 0.04)),
    rgba(6, 20, 32, 0.58) !important;
  color: rgba(237, 247, 255, 0.9) !important;
  box-shadow:
    0 22px 70px rgba(0, 0, 0, 0.26),
    inset 0 1px 0 rgba(255, 255, 255, 0.14) !important;
  backdrop-filter: blur(22px) saturate(1.1);
}

.theme-archive :deep(.page-head),
.theme-archive :deep(.dash-top) {
  padding: 8px 0 2px;
}

.theme-archive :deep(.stat-card:hover),
.theme-archive :deep(.sc:hover),
.theme-archive :deep(.chart-card:hover),
.theme-archive :deep(.card:hover) {
  box-shadow:
    0 28px 86px rgba(0, 0, 0, 0.32),
    inset 0 1px 0 rgba(255, 255, 255, 0.18) !important;
  transform: translateY(-2px);
}

.theme-archive :deep(.table-card),
.theme-archive :deep(.form-card),
.theme-archive :deep(.ai-card),
.theme-archive :deep(.ai-left-card),
.theme-archive :deep(.ai-right-card),
.theme-archive :deep(.job-card),
.theme-archive :deep(.info-card) {
  position: relative;
}

.theme-archive :deep(.table-card)::before,
.theme-archive :deep(.form-card)::before,
.theme-archive :deep(.ai-card)::before,
.theme-archive :deep(.ai-left-card)::before,
.theme-archive :deep(.ai-right-card)::before,
.theme-archive :deep(.job-card)::before,
.theme-archive :deep(.info-card)::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  border-radius: inherit;
  background:
    linear-gradient(115deg, transparent 12%, rgba(255, 214, 120, 0.13) 34%, transparent 56%),
    linear-gradient(90deg, transparent, rgba(126, 212, 255, 0.08), transparent);
  opacity: 0;
  pointer-events: none;
  transform: translateX(-26%);
  transition: opacity 0.22s ease;
}

.theme-archive :deep(.table-card:hover)::before,
.theme-archive :deep(.form-card:hover)::before,
.theme-archive :deep(.ai-card:hover)::before,
.theme-archive :deep(.ai-left-card:hover)::before,
.theme-archive :deep(.ai-right-card:hover)::before,
.theme-archive :deep(.job-card:hover)::before,
.theme-archive :deep(.info-card:hover)::before {
  opacity: 1;
  animation: archiveSheen 1.35s ease-out;
}

.theme-archive :deep(.table-card > *),
.theme-archive :deep(.form-card > *),
.theme-archive :deep(.ai-card > *),
.theme-archive :deep(.ai-left-card > *),
.theme-archive :deep(.ai-right-card > *),
.theme-archive :deep(.job-card > *),
.theme-archive :deep(.info-card > *) {
  position: relative;
  z-index: 1;
}

.theme-archive :deep(.chain-state),
.theme-archive :deep(.chain-badge) {
  box-shadow: 0 0 0 rgba(255, 214, 120, 0);
}

.theme-archive :deep(.chain-on-chain),
.theme-archive :deep(.chain-badge.on-chain) {
  animation: chainPulse 2.6s ease-in-out infinite;
}

.theme-archive :deep(.stat-card.is-blue),
.theme-archive :deep(.stat-card.blue-fill) {
  background:
    linear-gradient(145deg, rgba(255, 214, 120, 0.24), rgba(68, 183, 255, 0.14)),
    rgba(6, 20, 32, 0.7) !important;
}

.theme-archive :deep(.sc-num),
.theme-archive :deep(.stat-num),
.theme-archive :deep(.bm-val),
.theme-archive :deep(.lr-val),
.theme-archive :deep(.sc-label),
.theme-archive :deep(.stat-label),
.theme-archive :deep(.bm-lbl),
.theme-archive :deep(.lr-name),
.theme-archive :deep(.lr-pct),
.theme-archive :deep(.bar-label),
.theme-archive :deep(.bar-val),
.theme-archive :deep(.mb-name),
.theme-archive :deep(.mb-num),
.theme-archive :deep(.log-name),
.theme-archive :deep(.log-time),
.theme-archive :deep(.el-name),
.theme-archive :deep(.el-industry),
.theme-archive :deep(.el-code) {
  color: rgba(237, 247, 255, 0.82) !important;
}

.theme-archive :deep(.sc-num),
.theme-archive :deep(.stat-num),
.theme-archive :deep(.bm-val) {
  color: #fff !important;
}

.theme-archive :deep(.el-table) {
  --el-table-bg-color: transparent !important;
  --el-table-tr-bg-color: transparent !important;
  --el-table-header-bg-color: rgba(255, 255, 255, 0.08) !important;
  --el-table-border-color: rgba(255, 255, 255, 0.08) !important;
  --el-table-text-color: rgba(237, 247, 255, 0.86) !important;
  --el-table-header-text-color: rgba(226, 242, 250, 0.7) !important;
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.08) !important;
  background: transparent !important;
  color: rgba(237, 247, 255, 0.86) !important;
}

.theme-archive :deep(.el-table th.el-table__cell),
.theme-archive :deep(.el-table td.el-table__cell) {
  border-bottom-color: rgba(255, 255, 255, 0.08) !important;
  background: transparent !important;
  color: rgba(237, 247, 255, 0.86) !important;
}

.theme-archive :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: rgba(255, 255, 255, 0.08) !important;
}

.theme-archive :deep(.el-table__empty-text) {
  color: rgba(226, 242, 250, 0.5) !important;
}

.theme-archive :deep(.el-input__wrapper),
.theme-archive :deep(.el-select .el-input__wrapper),
.theme-archive :deep(.chart-pick .el-input__wrapper),
.theme-archive :deep(.el-date-editor .el-input__wrapper),
.theme-archive :deep(.el-textarea__inner) {
  border-color: rgba(255, 255, 255, 0.16) !important;
  background: rgba(3, 13, 23, 0.54) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08) !important;
}

.theme-archive :deep(.el-input__wrapper:hover),
.theme-archive :deep(.el-input__wrapper.is-focus),
.theme-archive :deep(.el-textarea__inner:focus) {
  border-color: rgba(255, 214, 120, 0.48) !important;
  box-shadow: 0 0 0 3px rgba(255, 214, 120, 0.12) !important;
}

.theme-archive :deep(.el-input__inner),
.theme-archive :deep(.el-select__placeholder),
.theme-archive :deep(.el-select .el-input__inner),
.theme-archive :deep(.el-textarea__inner) {
  color: #fff !important;
}

.theme-archive :deep(.el-input__inner::placeholder),
.theme-archive :deep(.el-textarea__inner::placeholder) {
  color: rgba(226, 242, 250, 0.46) !important;
}

.theme-archive :deep(.el-button--primary) {
  --el-button-bg-color: transparent !important;
  --el-button-border-color: transparent !important;
  --el-button-hover-bg-color: transparent !important;
  --el-button-hover-border-color: transparent !important;
  --el-button-active-bg-color: transparent !important;
  --el-button-active-border-color: transparent !important;
  border: none !important;
  background: linear-gradient(135deg, #f6c869, #f9973e) !important;
  color: #1c160c !important;
  font-weight: 800 !important;
  box-shadow: 0 14px 30px rgba(249, 151, 62, 0.26);
}

.theme-archive :deep(.el-button--default),
.theme-archive :deep(.btn-sm),
.theme-archive :deep(.btn-blue),
.theme-archive :deep(.btn-gray),
.theme-archive :deep(.btn-green) {
  border-color: rgba(255, 255, 255, 0.16) !important;
  background: rgba(255, 255, 255, 0.08) !important;
  color: rgba(237, 247, 255, 0.86) !important;
}

.theme-archive :deep(.el-button.is-link),
.theme-archive :deep(.el-button--primary.is-link),
.theme-archive :deep(.el-button--primary.el-button--text) {
  background: transparent !important;
  box-shadow: none !important;
  color: #ffd678 !important;
}

.theme-archive :deep(.el-tag),
.theme-archive :deep(.sc-trend),
.theme-archive :deep(.sc-trend.up),
.theme-archive :deep(.sc-trend.down),
.theme-archive :deep(.sc-trend.flat),
.theme-archive :deep(.st),
.theme-archive :deep(.el-tag.green),
.theme-archive :deep(.st-open),
.theme-archive :deep(.st-ok),
.theme-archive :deep(.st-wait),
.theme-archive :deep(.st-closed),
.theme-archive :deep(.st-done) {
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  background: rgba(255, 255, 255, 0.08) !important;
  color: rgba(237, 247, 255, 0.88) !important;
}

.theme-archive :deep(.chart-canvas svg text),
.theme-archive :deep(.pie-wrap svg text),
.theme-archive :deep(.chart-card svg text) {
  fill: rgba(237, 247, 255, 0.86) !important;
}

.theme-archive :deep(.chart-canvas svg line) {
  stroke: rgba(255, 255, 255, 0.1) !important;
}

.theme-archive :deep(.bar-track),
.theme-archive :deep(.bg-track),
.theme-archive :deep(.mb-track) {
  background: rgba(255, 255, 255, 0.1) !important;
}

.theme-archive :deep(.bar-metrics),
.theme-archive :deep(.major-bars),
.theme-archive :deep(.log-item),
.theme-archive :deep(.el-item) {
  border-color: rgba(255, 255, 255, 0.1) !important;
}

.theme-archive :deep(.ai-left-card),
.theme-archive :deep(.ai-right-card),
.theme-archive :deep(.ai-card),
.theme-archive :deep(.stat-item),
.theme-archive :deep(.mini-stat),
.theme-archive :deep(.job-card),
.theme-archive :deep(.info-card),
.theme-archive :deep(.result-card),
.theme-archive :deep(.prog-card),
.theme-archive :deep(.hist-card),
.theme-archive :deep(.chain-status) {
  border: 1px solid rgba(255, 255, 255, 0.16) !important;
  border-radius: 18px !important;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.15), rgba(255, 255, 255, 0.04)),
    rgba(5, 17, 28, 0.62) !important;
  color: rgba(237, 247, 255, 0.9) !important;
  box-shadow:
    0 22px 70px rgba(0, 0, 0, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.14) !important;
  backdrop-filter: blur(22px) saturate(1.1);
}

.theme-archive :deep(.ai-left-card),
.theme-archive :deep(.ai-right-card) {
  min-height: 0;
}

.theme-archive :deep(.left-head),
.theme-archive :deep(.student-bar),
.theme-archive :deep(.bottom-input),
.theme-archive :deep(.form-actions) {
  border-color: rgba(255, 255, 255, 0.1) !important;
  background: rgba(255, 255, 255, 0.03) !important;
}

.theme-archive :deep(.history-item),
.theme-archive :deep(.msg-bubble),
.theme-archive :deep(.ai-result),
.theme-archive :deep(.chain-status .vh),
.theme-archive :deep(.hash-text),
.theme-archive :deep(.vh) {
  border-color: rgba(255, 255, 255, 0.1) !important;
  background: rgba(3, 13, 23, 0.5) !important;
  color: rgba(237, 247, 255, 0.86) !important;
}

.theme-archive :deep(.history-item:hover),
.theme-archive :deep(.history-item.active),
.theme-archive :deep(.month-tabs span.active) {
  border-color: rgba(255, 214, 120, 0.32) !important;
  background:
    linear-gradient(135deg, rgba(255, 214, 120, 0.18), rgba(126, 212, 255, 0.1)),
    rgba(255, 255, 255, 0.08) !important;
}

.theme-archive :deep(.empty-icon),
.theme-archive :deep(.msg-avatar),
.theme-archive :deep(.hi-icon),
.theme-archive :deep(.sb-avatar),
.theme-archive :deep(.stat-icon) {
  border: 1px solid rgba(255, 214, 120, 0.22) !important;
  background: rgba(255, 214, 120, 0.12) !important;
  color: #ffd678 !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.theme-archive :deep(.left-title),
.theme-archive :deep(.hi-title),
.theme-archive :deep(.empty-state h3),
.theme-archive :deep(.msg-bubble),
.theme-archive :deep(.sb-name),
.theme-archive :deep(.status-title),
.theme-archive :deep(.stat-val),
.theme-archive :deep(.stu-name),
.theme-archive :deep(.job-card h3),
.theme-archive :deep(.vv),
.theme-archive :deep(.sv) {
  color: rgba(255, 255, 255, 0.94) !important;
}

.theme-archive :deep(.hi-meta),
.theme-archive :deep(.empty-state p),
.theme-archive :deep(.sb-meta),
.theme-archive :deep(.upload-text),
.theme-archive :deep(.sl),
.theme-archive :deep(.vl),
.theme-archive :deep(.table-foot),
.theme-archive :deep(.table-footer),
.theme-archive :deep(.jf-count),
.theme-archive :deep(.desc),
.theme-archive :deep(.stat-lbl),
.theme-archive :deep(.stat-val small),
.theme-archive :deep(.head-sub) {
  color: rgba(226, 242, 250, 0.62) !important;
}

.theme-archive :deep(.chat-input .el-input__wrapper),
.theme-archive :deep(.month-tabs) {
  border: 1px solid rgba(255, 255, 255, 0.12) !important;
  background: rgba(3, 13, 23, 0.54) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.08) !important;
}

.theme-archive :deep(.upload-wide .el-upload-dragger) {
  border-color: rgba(255, 214, 120, 0.26) !important;
  background: rgba(3, 13, 23, 0.42) !important;
  color: rgba(237, 247, 255, 0.86) !important;
}

.theme-archive :deep(.upload-wide .el-upload-dragger:hover) {
  border-color: rgba(255, 214, 120, 0.52) !important;
  background: rgba(255, 214, 120, 0.08) !important;
}

.theme-archive :deep(.el-tabs__item) {
  color: rgba(226, 242, 250, 0.64) !important;
}

.theme-archive :deep(.el-tabs__item.is-active),
.theme-archive :deep(.el-tabs__item:hover),
.theme-archive :deep(.company),
.theme-archive :deep(.history-item.active .hi-title) {
  color: #ffd678 !important;
}

.theme-archive :deep(.el-tabs__active-bar) {
  background-color: #ffd678 !important;
}

.theme-archive :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(255, 255, 255, 0.1) !important;
}

.theme-archive :deep(.vok) {
  border: 1px solid rgba(92, 255, 189, 0.24) !important;
  background: rgba(92, 255, 189, 0.1) !important;
  color: #a9ffd8 !important;
}

.theme-archive :deep(.page),
.theme-archive :deep(.dashboard) {
  color: rgba(32, 39, 33, 0.82) !important;
}

.theme-archive :deep(.page-head h1),
.theme-archive :deep(.dash-title),
.theme-archive :deep(.left-title),
.theme-archive :deep(.hi-title),
.theme-archive :deep(.empty-state h3),
.theme-archive :deep(.sb-name),
.theme-archive :deep(.status-title),
.theme-archive :deep(.stat-val),
.theme-archive :deep(.stu-name),
.theme-archive :deep(.job-card h3),
.theme-archive :deep(.vv),
.theme-archive :deep(.sv),
.theme-archive :deep(.sc-num),
.theme-archive :deep(.stat-num),
.theme-archive :deep(.bm-val) {
  color: #202721 !important;
  text-shadow: none !important;
}

.theme-archive :deep(.page-sub),
.theme-archive :deep(.page-desc),
.theme-archive :deep(.dash-updated),
.theme-archive :deep(.section-title),
.theme-archive :deep(.chart-name),
.theme-archive :deep(.chart-head),
.theme-archive :deep(.card-head),
.theme-archive :deep(.sc-label),
.theme-archive :deep(.stat-label),
.theme-archive :deep(.bm-lbl),
.theme-archive :deep(.lr-name),
.theme-archive :deep(.lr-pct),
.theme-archive :deep(.bar-label),
.theme-archive :deep(.bar-val),
.theme-archive :deep(.mb-name),
.theme-archive :deep(.mb-num),
.theme-archive :deep(.log-name),
.theme-archive :deep(.log-time),
.theme-archive :deep(.el-name),
.theme-archive :deep(.el-industry),
.theme-archive :deep(.el-code),
.theme-archive :deep(.hi-meta),
.theme-archive :deep(.empty-state p),
.theme-archive :deep(.sb-meta),
.theme-archive :deep(.upload-text),
.theme-archive :deep(.sl),
.theme-archive :deep(.vl),
.theme-archive :deep(.table-foot),
.theme-archive :deep(.table-footer),
.theme-archive :deep(.jf-count),
.theme-archive :deep(.desc),
.theme-archive :deep(.stat-lbl),
.theme-archive :deep(.stat-val small),
.theme-archive :deep(.head-sub) {
  color: rgba(32, 39, 33, 0.58) !important;
}

.theme-archive :deep(.stat-card),
.theme-archive :deep(.sc),
.theme-archive :deep(.card),
.theme-archive :deep(.chart-card),
.theme-archive :deep(.table-card),
.theme-archive :deep(.form-card),
.theme-archive :deep(.gen-result),
.theme-archive :deep(.ai-left-card),
.theme-archive :deep(.ai-right-card),
.theme-archive :deep(.ai-card),
.theme-archive :deep(.stat-item),
.theme-archive :deep(.mini-stat),
.theme-archive :deep(.job-card),
.theme-archive :deep(.info-card),
.theme-archive :deep(.result-card),
.theme-archive :deep(.prog-card),
.theme-archive :deep(.hist-card),
.theme-archive :deep(.chain-status) {
  border: 1px solid rgba(32, 39, 33, 0.08) !important;
  border-radius: 20px !important;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.54)),
    rgba(236, 242, 235, 0.72) !important;
  color: rgba(32, 39, 33, 0.82) !important;
  box-shadow:
    0 20px 54px rgba(32, 39, 33, 0.08),
    inset 0 1px 0 rgba(255, 255, 255, 0.86) !important;
  backdrop-filter: blur(18px) saturate(1.02);
}

.theme-archive :deep(.stat-card:hover),
.theme-archive :deep(.sc:hover),
.theme-archive :deep(.chart-card:hover),
.theme-archive :deep(.card:hover) {
  box-shadow:
    0 24px 62px rgba(32, 39, 33, 0.12),
    inset 0 1px 0 rgba(255, 255, 255, 0.9) !important;
}

.theme-archive :deep(.table-card)::before,
.theme-archive :deep(.form-card)::before,
.theme-archive :deep(.ai-card)::before,
.theme-archive :deep(.ai-left-card)::before,
.theme-archive :deep(.ai-right-card)::before,
.theme-archive :deep(.job-card)::before,
.theme-archive :deep(.info-card)::before {
  background:
    linear-gradient(115deg, transparent 12%, rgba(255, 255, 255, 0.72) 34%, transparent 56%),
    linear-gradient(90deg, transparent, rgba(32, 39, 33, 0.04), transparent);
}

.theme-archive :deep(.el-table) {
  --el-table-bg-color: transparent !important;
  --el-table-tr-bg-color: transparent !important;
  --el-table-header-bg-color: rgba(32, 39, 33, 0.04) !important;
  --el-table-border-color: rgba(32, 39, 33, 0.08) !important;
  --el-table-text-color: rgba(32, 39, 33, 0.78) !important;
  --el-table-header-text-color: rgba(32, 39, 33, 0.52) !important;
  --el-table-row-hover-bg-color: rgba(32, 39, 33, 0.04) !important;
  background: transparent !important;
  color: rgba(32, 39, 33, 0.78) !important;
}

.theme-archive :deep(.el-table th.el-table__cell),
.theme-archive :deep(.el-table td.el-table__cell) {
  border-bottom-color: rgba(32, 39, 33, 0.08) !important;
  background: transparent !important;
  color: rgba(32, 39, 33, 0.78) !important;
}

.theme-archive :deep(.el-table__body tr:hover > td.el-table__cell) {
  background: rgba(32, 39, 33, 0.04) !important;
}

.theme-archive :deep(.el-table__empty-text) {
  color: rgba(32, 39, 33, 0.45) !important;
}

.theme-archive :deep(.el-input__wrapper),
.theme-archive :deep(.el-select .el-input__wrapper),
.theme-archive :deep(.chart-pick .el-input__wrapper),
.theme-archive :deep(.el-date-editor .el-input__wrapper),
.theme-archive :deep(.el-textarea__inner),
.theme-archive :deep(.chat-input .el-input__wrapper),
.theme-archive :deep(.month-tabs) {
  border-color: rgba(32, 39, 33, 0.08) !important;
  background: rgba(255, 255, 255, 0.68) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82) !important;
}

.theme-archive :deep(.el-input__wrapper:hover),
.theme-archive :deep(.el-input__wrapper.is-focus),
.theme-archive :deep(.el-textarea__inner:focus) {
  border-color: rgba(32, 39, 33, 0.2) !important;
  box-shadow: 0 0 0 3px rgba(32, 39, 33, 0.08) !important;
}

.theme-archive :deep(.el-input__inner),
.theme-archive :deep(.el-select__placeholder),
.theme-archive :deep(.el-select .el-input__inner),
.theme-archive :deep(.el-textarea__inner) {
  color: #202721 !important;
}

.theme-archive :deep(.el-input__inner::placeholder),
.theme-archive :deep(.el-textarea__inner::placeholder) {
  color: rgba(32, 39, 33, 0.38) !important;
}

.theme-archive :deep(.el-button--primary) {
  background: #202721 !important;
  color: #fff !important;
  box-shadow: 0 14px 30px rgba(32, 39, 33, 0.16) !important;
}

.theme-archive :deep(.el-button--default),
.theme-archive :deep(.btn-sm),
.theme-archive :deep(.btn-blue),
.theme-archive :deep(.btn-gray),
.theme-archive :deep(.btn-green) {
  border-color: rgba(32, 39, 33, 0.08) !important;
  background: rgba(255, 255, 255, 0.68) !important;
  color: rgba(32, 39, 33, 0.76) !important;
}

.theme-archive :deep(.el-button.is-link),
.theme-archive :deep(.el-button--primary.is-link),
.theme-archive :deep(.el-button--primary.el-button--text) {
  color: #202721 !important;
}

.theme-archive :deep(.el-tag),
.theme-archive :deep(.sc-trend),
.theme-archive :deep(.sc-trend.up),
.theme-archive :deep(.sc-trend.down),
.theme-archive :deep(.sc-trend.flat),
.theme-archive :deep(.st),
.theme-archive :deep(.el-tag.green),
.theme-archive :deep(.st-open),
.theme-archive :deep(.st-ok),
.theme-archive :deep(.st-wait),
.theme-archive :deep(.st-closed),
.theme-archive :deep(.st-done),
.theme-archive :deep(.vok) {
  border: 1px solid rgba(32, 39, 33, 0.08) !important;
  background: rgba(255, 255, 255, 0.68) !important;
  color: rgba(32, 39, 33, 0.78) !important;
}

.theme-archive :deep(.chart-canvas svg text),
.theme-archive :deep(.pie-wrap svg text),
.theme-archive :deep(.chart-card svg text) {
  fill: rgba(32, 39, 33, 0.72) !important;
}

.theme-archive :deep(.chart-canvas svg line) {
  stroke: rgba(32, 39, 33, 0.1) !important;
}

.theme-archive :deep(.bar-track),
.theme-archive :deep(.bg-track),
.theme-archive :deep(.mb-track) {
  background: rgba(32, 39, 33, 0.08) !important;
}

.theme-archive :deep(.bar-metrics),
.theme-archive :deep(.major-bars),
.theme-archive :deep(.log-item),
.theme-archive :deep(.el-item),
.theme-archive :deep(.left-head),
.theme-archive :deep(.student-bar),
.theme-archive :deep(.bottom-input),
.theme-archive :deep(.form-actions) {
  border-color: rgba(32, 39, 33, 0.08) !important;
  background: rgba(255, 255, 255, 0.42) !important;
}

.theme-archive :deep(.history-item),
.theme-archive :deep(.msg-bubble),
.theme-archive :deep(.ai-result),
.theme-archive :deep(.chain-status .vh),
.theme-archive :deep(.hash-text),
.theme-archive :deep(.vh) {
  border-color: rgba(32, 39, 33, 0.08) !important;
  background: rgba(255, 255, 255, 0.56) !important;
  color: rgba(32, 39, 33, 0.78) !important;
}

.theme-archive :deep(.history-item:hover),
.theme-archive :deep(.history-item.active),
.theme-archive :deep(.month-tabs span.active) {
  border-color: rgba(32, 39, 33, 0.12) !important;
  background: rgba(255, 255, 255, 0.82) !important;
}

.theme-archive :deep(.empty-icon),
.theme-archive :deep(.msg-avatar),
.theme-archive :deep(.hi-icon),
.theme-archive :deep(.sb-avatar),
.theme-archive :deep(.stat-icon) {
  border: 1px solid rgba(32, 39, 33, 0.08) !important;
  background: rgba(255, 255, 255, 0.72) !important;
  color: #202721 !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82);
}

.theme-archive :deep(.upload-wide .el-upload-dragger) {
  border-color: rgba(32, 39, 33, 0.1) !important;
  background: rgba(255, 255, 255, 0.56) !important;
  color: rgba(32, 39, 33, 0.72) !important;
}

.theme-archive :deep(.upload-wide .el-upload-dragger:hover) {
  border-color: rgba(32, 39, 33, 0.2) !important;
  background: rgba(255, 255, 255, 0.82) !important;
}

.theme-archive :deep(.el-tabs__item) {
  color: rgba(32, 39, 33, 0.56) !important;
}

.theme-archive :deep(.el-tabs__item.is-active),
.theme-archive :deep(.el-tabs__item:hover),
.theme-archive :deep(.company),
.theme-archive :deep(.history-item.active .hi-title) {
  color: #202721 !important;
}

.theme-archive :deep(.el-tabs__active-bar) {
  background-color: #202721 !important;
}

.theme-archive :deep(.el-tabs__nav-wrap::after) {
  background-color: rgba(32, 39, 33, 0.08) !important;
}

:global(.theme-archive-dialog.el-dialog),
:global(.el-dialog) {
  border: 1px solid rgba(32, 39, 33, 0.08);
  border-radius: 18px;
  background:
    linear-gradient(145deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.68)),
    rgba(236, 242, 235, 0.9);
  color: rgba(32, 39, 33, 0.86);
  box-shadow: 0 28px 90px rgba(32, 39, 33, 0.16);
  backdrop-filter: blur(24px) saturate(1.04);
}

:global(.el-dialog__title),
:global(.el-form-item__label) {
  color: rgba(32, 39, 33, 0.86) !important;
}

:global(.el-dialog__body) {
  color: rgba(32, 39, 33, 0.78) !important;
}

.plain-layout,
.auth-layout {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8fafc;
}

@media (prefers-reduced-motion: reduce) {
  .archive-star-stream,
  .archive-ledger-plane,
  .archive-chain-orbit,
  .proof-track span,
  .theme-archive :deep(.chain-on-chain),
  .theme-archive :deep(.chain-badge.on-chain) {
    animation: none !important;
  }

  .theme-archive :deep(.table-card:hover)::before,
  .theme-archive :deep(.form-card:hover)::before,
  .theme-archive :deep(.ai-card:hover)::before,
  .theme-archive :deep(.ai-left-card:hover)::before,
  .theme-archive :deep(.ai-right-card:hover)::before,
  .theme-archive :deep(.job-card:hover)::before,
  .theme-archive :deep(.info-card:hover)::before {
    animation: none !important;
  }
}

@media (max-width: 980px) {
  .sidebar-wrap {
    width: 86px;
    margin-left: 12px;
  }

  .brand-text,
  .sidebar-nav .nav-item span:not(.nav-dot),
  .s-user-info,
  .top-logo {
    display: none;
  }

  .sidebar-brand,
  .nav-item,
  .sidebar-user {
    justify-content: center;
  }

  .top-bar {
    margin-right: 12px;
  }

  .workspace-hero {
    grid-template-columns: 1fr;
    margin: 14px 12px 0;
    padding: 22px;
  }

  .page-container {
    padding: 16px 18px 28px;
  }
}
</style>
