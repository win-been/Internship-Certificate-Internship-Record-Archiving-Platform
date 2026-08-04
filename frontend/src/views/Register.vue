<template>
  <div class="auth-page" :style="{ '--landing-image': `url(${landingImage})` }">
    <header class="auth-nav">
      <router-link to="/login" class="brand" aria-label="实习存证平台">
        <span class="brand-mark">C</span>
        <span class="brand-text">实习存证平台</span>
      </router-link>
      <nav class="nav-links" aria-label="入口导航">
        <router-link to="/login">首页</router-link>
        <router-link to="/login">登录</router-link>
        <router-link to="/register" class="active">注册</router-link>
      </nav>
    </header>

    <main class="register-shell">
      <div class="particle-field" aria-hidden="true">
        <span v-for="particle in particles" :key="particle.id" :style="particleStyle(particle)"></span>
      </div>

      <section class="hero-copy">
        <p class="eyebrow">账号注册</p>
        <h1>
          <span>创建可信身份</span>
          <span>名单核验后自动开通</span>
        </h1>
        <p class="hero-desc">
          系统会用身份证号或企业信用代码匹配预录入名单；命中后账号自动启用，可直接用于实习备案、过程记录、证明签发与存证核验。
        </p>
        <div class="status-rail" aria-label="注册状态">
          <article>
            <span>01</span>
            <small>Identity</small>
            <strong>提交身份</strong>
          </article>
          <article>
            <span>02</span>
            <small>Auto check</small>
            <strong>名单核验</strong>
          </article>
          <article>
            <span>03</span>
            <small>Workspace</small>
            <strong>自动开通</strong>
          </article>
        </div>
        <div class="identity-pass" aria-hidden="true">
          <div>
            <span>Trusted ID</span>
            <strong>{{ identityPassLabel }}</strong>
          </div>
          <i></i>
        </div>
      </section>

      <section class="register-card" aria-label="注册表单">
        <div class="card-glow" aria-hidden="true"></div>
        <div class="form-orbit" aria-hidden="true"></div>
        <div class="login-brand">
          <div class="brand-icon"><el-icon :size="22"><DocumentChecked /></el-icon></div>
          <div>
            <span>实习存证平台</span>
            <small>名单命中后自动开通账号</small>
          </div>
        </div>

        <div class="verify-strip" aria-label="自动核验状态">
          <strong>Auto verify</strong>
          <span>{{ verifyTargetLabel }}</span>
          <i></i>
          <em>自动通过</em>
        </div>

        <el-form :model="form" :rules="rules" ref="formRef" label-position="top" size="default">
          <div class="form-grid">
            <el-form-item label="用户名" prop="username"><el-input v-model.trim="form.username" placeholder="4-20位字母数字" /></el-form-item>
            <el-form-item label="角色" prop="role">
              <el-select v-model="form.role">
                <el-option label="学生" value="STUDENT"/>
                <el-option label="企业HR" value="ENTERPRISE_HR"/>
                <el-option label="学校管理员" value="SCHOOL_ADMIN"/>
              </el-select>
            </el-form-item>
          </div>
          <div class="form-grid">
            <el-form-item label="密码" prop="password"><el-input v-model="form.password" type="password" show-password placeholder="6-20位密码" /></el-form-item>
            <el-form-item label="确认密码" prop="password2"><el-input v-model="form.password2" type="password" show-password placeholder="再次输入密码" /></el-form-item>
          </div>
          <div class="form-grid">
            <el-form-item label="真实姓名" prop="realName"><el-input v-model.trim="form.realName" :placeholder="isEnterpriseRole(form.role) ? '请输入联系人姓名' : '请输入真实姓名'" /></el-form-item>
            <el-form-item label="手机号" prop="phone"><el-input v-model.trim="form.phone" placeholder="11位手机号" /></el-form-item>
          </div>
          <div class="form-grid" v-if="form.role==='STUDENT'">
            <el-form-item label="所属学校" prop="schoolId">
              <el-select v-model="form.schoolId" filterable placeholder="请选择学校">
                <el-option v-for="school in schools" :key="school.id" :label="school.name" :value="school.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="身份证号" prop="idCard"><el-input v-model.trim="form.idCard" placeholder="学生需与所选学校名单一致" /></el-form-item>
          </div>
          <el-form-item label="邮箱" prop="email"><el-input v-model.trim="form.email" placeholder="可选，用于接收开通通知" /></el-form-item>
          <div class="form-grid" v-if="isEnterpriseRole(form.role)">
            <el-form-item label="企业名称" prop="organizationName"><el-input v-model.trim="form.organizationName" placeholder="请输入企业名称" /></el-form-item>
            <el-form-item label="信用代码" prop="organizationCode"><el-input v-model.trim="form.organizationCode" placeholder="需与企业名单编码一致" /></el-form-item>
          </div>
          <div class="form-grid" v-if="isSchoolRole(form.role)">
            <el-form-item label="学校名称" prop="organizationName"><el-input v-model.trim="form.organizationName" placeholder="请输入学校全称" /></el-form-item>
            <el-form-item label="学校代码" prop="organizationCode"><el-input v-model.trim="form.organizationCode" placeholder="可填学校统一编码，便于区分多学校" /></el-form-item>
          </div>
          <div class="auto-verify-note">
            <span></span>
            <p>{{ verifyNote }}</p>
          </div>
          <el-form-item>
            <el-button type="primary" :loading="loading" class="submit-button" @click="handleRegister">
              {{ submitButtonText }}
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-form-item>
        </el-form>
        <div class="login-footer">已有账号？<router-link to="/login">立即登录</router-link></div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import landingImage from '../assets/landing-alpine-meadow.jpg'
import { ElMessage } from 'element-plus'
import { ArrowRight, DocumentChecked } from '@element-plus/icons-vue'
import api from '../api/request'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref(null)
const schools = ref([])
const form = reactive({ username:'', password:'', password2:'', realName:'', idCard:'', schoolId:null, phone:'', email:'', role:'STUDENT', organizationName:'', organizationCode:'' })
const isEnterpriseRole = (role) => role === 'ENTERPRISE_HR' || role === 'ENTERPRISE_MENTOR'
const isSchoolRole = (role) => role === 'SCHOOL_ADMIN'
const identityPassLabel = computed(() => {
  if (isEnterpriseRole(form.role)) return 'Enterprise'
  if (isSchoolRole(form.role)) return 'School'
  return 'Student'
})
const verifyTargetLabel = computed(() => {
  if (isEnterpriseRole(form.role)) return '企业名单'
  if (isSchoolRole(form.role)) return '学校工作区'
  return '学生名单'
})
const verifyNote = computed(() => {
  if (isEnterpriseRole(form.role)) return '系统将校验企业名称和信用代码是否已录入企业名单，命中后自动开通。'
  if (isSchoolRole(form.role)) return '学校管理员注册后会自动创建独立学校工作区，后续学生实名认证、通知和业务数据按该学校隔离。'
  return '系统将校验所选学校的学生名单；名单命中会直接开通，未命中会提交学校审核，通过后自动加入学生名单。'
})
const submitButtonText = computed(() => isSchoolRole(form.role) ? '创建学校工作区' : (form.role === 'STUDENT' ? '提交注册' : '核验名单并开通'))
const particles = [
  { id: 1, x: 10, y: 24, s: 4, d: -2.2 },
  { id: 2, x: 22, y: 70, s: 3, d: -4.1 },
  { id: 3, x: 38, y: 18, s: 5, d: -1.4 },
  { id: 4, x: 56, y: 76, s: 4, d: -5.5 },
  { id: 5, x: 72, y: 30, s: 3, d: -3.1 },
  { id: 6, x: 88, y: 58, s: 5, d: -6.2 }
]

const particleStyle = (particle) => ({
  '--x': `${particle.x}%`,
  '--y': `${particle.y}%`,
  '--s': `${particle.s}px`,
  '--delay': `${particle.d}s`
})

const validateIdCard = (rule, value, callback) => {
  if (form.role !== 'STUDENT') callback()
  else if (!value) callback(new Error('请输入身份证号'))
  else if (!/^\d{17}[\dXx]$/.test(value)) callback(new Error('身份证号须为18位数字或末位X'))
  else callback()
}
const validateSchool = (rule, value, callback) => {
  if (form.role !== 'STUDENT') callback()
  else if (!value) callback(new Error('请选择所属学校'))
  else callback()
}
const validatePhone = (rule, value, callback) => {
  if (!value) callback()
  else if (!/^1[3-9]\d{9}$/.test(value)) callback(new Error('手机号格式不正确'))
  else callback()
}
const validatePassword2 = (rule, value, callback) => {
  if (!value) callback(new Error('请再次输入密码'))
  else if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}
const validateOrgCode = (rule, value, callback) => {
  if (isEnterpriseRole(form.role) && !value) callback(new Error('请输入统一社会信用代码'))
  else if (isSchoolRole(form.role) && value && !/^[A-Za-z0-9]{2,30}$/.test(value)) callback(new Error('学校代码须为2-30位字母数字'))
  else if (!isSchoolRole(form.role) && value && !/^[A-Za-z0-9]{6,30}$/.test(value)) callback(new Error('信用代码须为6-30位字母数字'))
  else callback()
}
const validateOrganizationName = (rule, value, callback) => {
  if (isEnterpriseRole(form.role) && !value) callback(new Error('请输入企业名称'))
  else if (isSchoolRole(form.role) && !value) callback(new Error('请输入学校名称'))
  else callback()
}

const rules = {
  username: [{required:true,message:'请输入用户名',trigger:'blur'},{min:4,max:20,message:'4-20位',trigger:'blur'}],
  password: [{required:true,message:'请输入密码',trigger:'blur'},{min:6,max:20,message:'6-20位',trigger:'blur'}],
  password2: [{validator:validatePassword2,trigger:'blur'}],
  realName: [{required:true,message:'请输入真实姓名',trigger:'blur'}],
  idCard: [{validator:validateIdCard,trigger:'blur'}],
  phone: [{validator:validatePhone,trigger:'blur'}],
  email: [{type:'email',message:'邮箱格式不正确',trigger:'blur'}],
  role: [{required:true,message:'请选择角色',trigger:'change'}],
  schoolId: [{validator:validateSchool,trigger:'change'}],
  organizationName: [{validator:validateOrganizationName,trigger:'blur'}],
  organizationCode: [{validator:validateOrgCode,trigger:'blur'}],
}

const loadSchools = async () => {
  try {
    const res = await api.get('/auth/schools')
    schools.value = res.data || []
  } catch (e) {
    schools.value = []
  }
}

const registerErrorMessage = (message) => {
  if (!message) return '注册失败'
  if (message.includes('Enterprise is not in the roster') || message.includes('企业不在名单')) {
    return '企业不在名单中，请先由学校端录入企业名称和信用代码'
  }
  if (message.includes('Student is not in the roster') || message.includes('学生不在名单')) {
    return '学生不在所选学校名单中，请学校管理员进入「学生名单」录入后再注册'
  }
  if (message.includes('already been registered') || message.includes('账号已注册')) {
    return message
  }
  if (message.includes('school name')) return '请输入学校名称'
  if (message.includes('organization code')) return '请输入企业信用代码'
  if (message.includes('ID card')) return '请输入学生身份证号'
  return message
}

const handleRegister = async () => {
  if (loading.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const registered = await userStore.register({
      username: form.username.trim(),
      password: form.password,
      realName: form.realName.trim(),
      idCard: form.idCard.trim(),
      schoolId: form.role === 'STUDENT' ? form.schoolId : null,
      schoolName: schools.value.find(s => s.id === form.schoolId)?.name || null,
      phone: form.phone.trim(),
      email: form.email.trim() || null,
      role: form.role,
      organizationName: form.organizationName.trim() || null,
      organizationCode: form.organizationCode.trim() || null
    })
    ElMessage.success(registered?.status === 'PENDING' ? '注册已提交，请等待学校审核通过后登录' : '注册成功，账号已自动开通')
    router.push('/login')
  } catch(e) {
    ElMessage.closeAll()
    ElMessage.error(registerErrorMessage(e?.message))
  } finally {
    loading.value = false
  }
}

onMounted(loadSchools)
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100svh;
  overflow-x: hidden;
  background: #eef5ed;
  color: #17231c;
  font-family: Inter, -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  isolation: isolate;
}

.auth-page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(110deg, rgba(248, 252, 247, 0.93) 0%, rgba(248, 252, 247, 0.72) 42%, rgba(29, 48, 42, 0.22) 100%),
    var(--landing-image) center 42% / cover no-repeat;
  transform: scale(1.03);
  pointer-events: none;
}

.auth-page::after {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(rgba(35, 53, 45, 0.055) 1px, transparent 1px),
    linear-gradient(90deg, rgba(35, 53, 45, 0.045) 1px, transparent 1px);
  background-size: 68px 68px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.82), transparent 84%);
  pointer-events: none;
}

.auth-nav {
  position: relative;
  z-index: 9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(1560px, calc(100% - 72px));
  margin: 0 auto;
  padding: 24px 0;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #132219 !important;
  text-decoration: none !important;
}

.brand-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(19, 34, 25, 0.15);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: 0 14px 36px rgba(22, 42, 31, 0.14);
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 18px;
}

.brand-text {
  font-size: 22px;
  font-weight: 950;
  letter-spacing: 0;
}

.nav-links {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px;
  border: 1px solid rgba(19, 34, 25, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.68);
  box-shadow: 0 18px 46px rgba(28, 45, 35, 0.12);
  backdrop-filter: blur(18px);
}

.nav-links a {
  padding: 9px 16px;
  border-radius: 999px;
  color: rgba(19, 34, 25, 0.6);
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}

.nav-links a:hover,
.nav-links a.active {
  background: rgba(19, 34, 25, 0.07);
  color: #132219;
}

.register-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(480px, 560px);
  gap: 58px;
  align-items: center;
  width: min(1560px, calc(100% - 72px));
  min-height: calc(100svh - 118px);
  margin: 0 auto 40px;
  overflow: hidden;
  padding: 68px 72px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  border-radius: 30px;
  background:
    radial-gradient(circle at 18% 18%, rgba(255, 255, 255, 0.82), transparent 28%),
    radial-gradient(circle at 78% 22%, rgba(144, 199, 184, 0.32), transparent 26%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(247, 251, 246, 0.38) 55%, rgba(25, 42, 35, 0.26) 100%),
    var(--landing-image) center 44% / cover no-repeat;
  box-shadow:
    0 36px 100px rgba(19, 34, 25, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.register-shell::after {
  content: '';
  position: absolute;
  inset: auto -10% -18% -10%;
  height: 42%;
  z-index: 0;
  background:
    radial-gradient(ellipse at 50% 72%, rgba(12, 26, 22, 0.4), rgba(255, 255, 255, 0.08) 48%, transparent 72%),
    linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.22), transparent);
  pointer-events: none;
}

.particle-field {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.particle-field span {
  position: absolute;
  left: var(--x);
  top: var(--y);
  width: var(--s);
  height: var(--s);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.82);
  box-shadow:
    0 0 18px rgba(122, 201, 180, 0.55),
    0 0 34px rgba(255, 215, 137, 0.24);
  animation: particleFloat 11s ease-in-out infinite;
  animation-delay: var(--delay);
}

.hero-copy {
  position: relative;
  z-index: 2;
  max-width: 760px;
}

.eyebrow {
  width: fit-content;
  margin: 0 0 14px;
  padding: 9px 14px;
  border: 1px solid rgba(19, 34, 25, 0.1);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.5);
  color: rgba(19, 34, 25, 0.7);
  font-size: 13px;
  font-weight: 950;
  letter-spacing: 0.08em;
  backdrop-filter: blur(16px);
}

.hero-copy h1 {
  margin: 0;
  color: #142119;
  font-size: clamp(46px, 4.55vw, 74px);
  font-weight: 950;
  line-height: 0.98;
  letter-spacing: 0;
  text-shadow: 0 22px 70px rgba(255, 255, 255, 0.72);
}

.hero-copy h1 span {
  display: block;
}

.hero-desc {
  max-width: 560px;
  margin: 24px 0 0;
  color: rgba(20, 33, 25, 0.68);
  font-size: 19px;
  font-weight: 800;
  line-height: 1.7;
}

.status-rail {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 620px;
  margin-top: 42px;
}

.status-rail article {
  position: relative;
  overflow: hidden;
  min-height: 100px;
  padding: 18px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  border-radius: 18px;
  background:
    linear-gradient(145deg, rgba(247, 252, 246, 0.72), rgba(216, 230, 213, 0.46)),
    rgba(244, 250, 242, 0.58);
  box-shadow:
    0 20px 42px rgba(19, 34, 25, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(18px);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.status-rail article::after {
  content: '';
  position: absolute;
  right: -28px;
  bottom: -32px;
  width: 92px;
  height: 92px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(20, 33, 25, 0.12), transparent 66%);
}

.status-rail article:hover {
  transform: translateY(-4px);
  box-shadow:
    0 28px 54px rgba(19, 34, 25, 0.15),
    inset 0 1px 0 rgba(255, 255, 255, 0.74);
}

.status-rail span {
  color: rgba(20, 33, 25, 0.44);
  font-size: 12px;
  font-weight: 950;
}

.status-rail small {
  display: block;
  margin-top: 6px;
  color: rgba(20, 33, 25, 0.42);
  font-size: 11px;
  font-weight: 900;
  letter-spacing: 0;
}

.status-rail strong {
  display: block;
  margin-top: 16px;
  color: #142119;
  font-size: 18px;
  font-weight: 950;
}

.identity-pass {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: min(390px, 100%);
  min-height: 122px;
  margin-top: 22px;
  padding: 20px 22px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(20, 34, 28, 0.86), rgba(32, 57, 47, 0.72)),
    rgba(20, 34, 28, 0.78);
  box-shadow:
    0 28px 68px rgba(17, 28, 22, 0.24),
    inset 0 1px 0 rgba(255, 255, 255, 0.22);
  color: #fff;
  backdrop-filter: blur(18px);
}

.identity-pass::before {
  content: '';
  position: absolute;
  inset: 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 18px;
}

.identity-pass::after {
  content: '';
  position: absolute;
  top: -60px;
  right: -30px;
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(143, 209, 188, 0.48), transparent 66%);
  animation: glowDrift 9s ease-in-out infinite;
}

.identity-pass span,
.identity-pass strong {
  position: relative;
  z-index: 1;
  display: block;
}

.identity-pass span {
  color: rgba(255, 255, 255, 0.56);
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.identity-pass strong {
  margin-top: 12px;
  color: #fff;
  font-size: 30px;
  font-weight: 950;
  line-height: 1;
}

.identity-pass i {
  position: relative;
  z-index: 1;
  display: block;
  width: 58px;
  height: 58px;
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.88), rgba(255, 255, 255, 0.28)),
    radial-gradient(circle at 70% 30%, rgba(255, 214, 108, 0.76), transparent 34%);
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.16);
}

.register-card {
  position: relative;
  z-index: 2;
  width: 100%;
  box-sizing: border-box;
  max-height: calc(100svh - 184px);
  overflow-y: auto;
  overflow-x: hidden;
  padding: 34px;
  border: 1px solid rgba(255, 255, 255, 0.76);
  border-radius: 30px;
  background:
    linear-gradient(145deg, rgba(248, 253, 247, 0.86), rgba(224, 238, 221, 0.72)),
    rgba(244, 250, 242, 0.78);
  box-shadow:
    0 34px 92px rgba(19, 34, 25, 0.2),
    0 1px 0 rgba(255, 255, 255, 0.72) inset,
    0 -1px 0 rgba(20, 33, 25, 0.04) inset;
  backdrop-filter: blur(28px) saturate(1.08);
  scrollbar-width: thin;
  scrollbar-color: rgba(20, 33, 25, 0.18) transparent;
}

.register-card::-webkit-scrollbar {
  width: 8px;
}

.register-card::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(20, 33, 25, 0.18);
}

.register-card::before {
  content: '';
  position: absolute;
  inset: 1px;
  border-radius: 29px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.42), transparent 36%),
    linear-gradient(315deg, rgba(111, 169, 148, 0.16), transparent 32%);
  pointer-events: none;
}

.card-glow {
  position: absolute;
  top: -96px;
  right: -84px;
  width: 220px;
  height: 220px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(138, 199, 178, 0.42), transparent 68%);
  animation: glowDrift 8s ease-in-out infinite;
  pointer-events: none;
}

.form-orbit {
  position: absolute;
  top: 16px;
  right: 18px;
  width: 124px;
  height: 124px;
  border: 1px solid rgba(20, 33, 25, 0.08);
  border-radius: 50%;
  opacity: 0.72;
  pointer-events: none;
}

.form-orbit::before,
.form-orbit::after {
  content: '';
  position: absolute;
  border-radius: 50%;
}

.form-orbit::before {
  inset: 28px;
  border: 1px solid rgba(20, 33, 25, 0.07);
}

.form-orbit::after {
  top: 16px;
  right: 18px;
  width: 9px;
  height: 9px;
  background: rgba(20, 33, 25, 0.42);
  box-shadow: 0 0 0 8px rgba(20, 33, 25, 0.06);
  animation: orbitDot 7s linear infinite;
  transform-origin: -44px 45px;
}

.login-brand {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 26px;
}

.brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border: 1px solid rgba(19, 34, 25, 0.08);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.74);
  color: #1a3325;
  box-shadow: 0 14px 34px rgba(19, 34, 25, 0.1);
}

.login-brand span {
  display: block;
  color: #142119;
  font-size: 24px;
  font-weight: 950;
  line-height: 1;
}

.login-brand small {
  display: block;
  margin-top: 6px;
  color: rgba(20, 33, 25, 0.54);
  font-size: 12px;
  font-weight: 800;
}

.verify-strip {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: auto auto 1fr auto;
  align-items: center;
  gap: 10px;
  margin: -8px 0 22px;
  padding: 12px 14px;
  overflow: hidden;
  border: 1px solid rgba(20, 33, 25, 0.09);
  border-radius: 18px;
  background:
    linear-gradient(135deg, rgba(20, 33, 25, 0.92), rgba(28, 57, 44, 0.78)),
    rgba(20, 33, 25, 0.86);
  color: #fff;
  box-shadow:
    0 18px 40px rgba(20, 33, 25, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.18);
}

.verify-strip::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(110deg, transparent 0%, rgba(255, 255, 255, 0.18) 42%, transparent 58%);
  transform: translateX(-120%);
  animation: verifySweep 4.8s ease-in-out infinite;
}

.verify-strip strong,
.verify-strip span,
.verify-strip em {
  position: relative;
  z-index: 1;
}

.verify-strip strong {
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.verify-strip span,
.verify-strip em {
  color: rgba(255, 255, 255, 0.72);
  font-size: 12px;
  font-style: normal;
  font-weight: 850;
}

.verify-strip i {
  position: relative;
  z-index: 1;
  height: 1px;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.18), rgba(255, 255, 255, 0.62));
}

.form-grid {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  min-width: 0;
}

.form-grid > * {
  min-width: 0;
}

.register-card :deep(.el-form-item) {
  position: relative;
  z-index: 1;
  margin-bottom: 14px;
}

.register-card :deep(.el-form-item__label) {
  color: rgba(20, 33, 25, 0.68);
  font-weight: 900;
  line-height: 1.1;
}

.register-card :deep(.el-input__wrapper),
.register-card :deep(.el-select__wrapper) {
  min-height: 48px;
  border: 1px solid rgba(255, 255, 255, 0.76) !important;
  border-radius: 17px !important;
  background: rgba(255, 255, 255, 0.78) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.86),
    0 12px 26px rgba(19, 34, 25, 0.06) !important;
}

.register-card :deep(.el-input__wrapper.is-focus),
.register-card :deep(.el-select__wrapper.is-focused) {
  border-color: rgba(20, 33, 25, 0.24) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 0 0 3px rgba(68, 119, 91, 0.12) !important;
}

.auto-verify-note {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 2px 0 14px;
  padding: 12px 14px;
  border: 1px solid rgba(20, 33, 25, 0.08);
  border-radius: 16px;
  background:
    linear-gradient(135deg, rgba(255,255,255,.74), rgba(231,242,226,.54)),
    rgba(255,255,255,.56);
  color: rgba(20, 33, 25, 0.62);
  box-shadow: inset 0 1px 0 rgba(255,255,255,.76);
}

.auto-verify-note span {
  flex: 0 0 auto;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #1b392b;
  box-shadow: 0 0 0 6px rgba(27, 57, 43, 0.08), 0 0 18px rgba(93, 159, 127, 0.36);
}

.auto-verify-note p {
  margin: 0;
  font-size: 12px;
  font-weight: 850;
  line-height: 1.5;
}

.submit-button {
  --el-button-bg-color: transparent !important;
  --el-button-border-color: transparent !important;
  --el-button-hover-bg-color: transparent !important;
  --el-button-hover-border-color: transparent !important;
  --el-button-active-bg-color: transparent !important;
  --el-button-active-border-color: transparent !important;
  width: 100%;
  height: 52px;
  border: none !important;
  border-radius: 18px !important;
  background:
    linear-gradient(135deg, #102019, #172e22) !important;
  color: #fff !important;
  font-weight: 950;
  box-shadow:
    0 20px 38px rgba(21, 35, 26, 0.28),
    inset 0 1px 0 rgba(255, 255, 255, 0.14);
}

.submit-button :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.login-footer {
  margin-top: 12px;
  color: rgba(20, 33, 25, 0.56);
  text-align: center;
  font-size: 13px;
  font-weight: 800;
}

.login-footer a {
  color: #142119;
  font-weight: 950;
  text-decoration: none;
}

@keyframes particleFloat {
  0%, 100% {
    transform: translate3d(0, 0, 0);
    opacity: 0.34;
  }
  50% {
    transform: translate3d(18px, -28px, 0);
    opacity: 0.86;
  }
}

@keyframes glowDrift {
  0%, 100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(-28px, 32px, 0) scale(1.12);
  }
}

@keyframes orbitDot {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@keyframes verifySweep {
  0%, 42% {
    transform: translateX(-120%);
  }
  72%, 100% {
    transform: translateX(120%);
  }
}

@media (prefers-reduced-motion: reduce) {
  .particle-field span,
  .card-glow,
  .identity-pass::after,
  .form-orbit::after,
  .verify-strip::before {
    animation: none !important;
  }
}

@media (max-width: 1080px) {
  .auth-nav {
    width: min(100% - 32px, 900px);
  }

  .register-shell {
    grid-template-columns: 1fr;
    width: min(100% - 32px, 900px);
    min-height: auto;
    padding: 52px 36px;
  }

  .register-card {
    max-height: none;
  }
}

@media (max-width: 720px) {
  .auth-nav {
    padding: 16px 0;
  }

  .brand-text {
    font-size: 18px;
  }

  .nav-links {
    display: none;
  }

  .register-shell {
    padding: 42px 20px;
    border-radius: 22px;
  }

  .hero-copy h1 {
    font-size: clamp(32px, 8.8vw, 36px);
    line-height: 1.04;
  }

  .hero-desc {
    font-size: 16px;
  }

  .status-rail,
  .form-grid {
    grid-template-columns: 1fr;
  }

  .register-card {
    padding: 22px;
    border-radius: 22px;
  }
}
</style>
