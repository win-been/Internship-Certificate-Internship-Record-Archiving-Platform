<template>
  <div
    class="auth-page"
    :style="{
      '--scroll-progress': scrollProgress,
      '--landing-image': `url(${landingImage})`,
      '--carousel-shift': `${carouselShift}px`,
      '--carousel-tilt': `${carouselTilt}deg`,
      '--carousel-card-width': `${carouselCardWidth}px`,
      '--carousel-card-height': `${carouselCardHeight}px`,
      '--carousel-height': `${carouselHeight}px`,
      '--carousel-focus': carouselFocus
    }"
  >
    <header class="auth-nav">
      <router-link to="/login" class="brand" aria-label="实习存证平台">
        <span class="brand-mark">C</span>
        <span class="brand-text">实习存证平台</span>
      </router-link>
      <nav class="nav-links" aria-label="入口导航">
        <a href="#login" @click.prevent="focusUsername">登录</a>
        <router-link to="/register">注册</router-link>
      </nav>
    </header>

    <main class="scroll-hero" id="vision">
      <div class="particle-field" aria-hidden="true">
        <span
          v-for="particle in particles"
          :key="particle.id"
          :style="{
            '--x': `${particle.x}%`,
            '--y': `${particle.y}%`,
            '--s': `${particle.size}px`,
            '--delay': `${particle.delay}s`
          }"
        ></span>
      </div>

      <section class="vision-copy" aria-label="平台愿景">
        <p class="eyebrow">可信实习档案</p>
        <h1>把每段实习，沉淀为可信记录</h1>
        <p>
          备案、日报、考核、证明与链上核验在同一条成长时间线上汇合。
        </p>
      </section>

      <section id="login" class="workspace-entry" :class="{ 'is-open': loginVisible }" aria-label="进入工作台">
        <div class="entry-glow" aria-hidden="true"></div>
        <div class="panel-head">
          <div class="panel-icon">
            <span class="panel-mark">C</span>
          </div>
          <div>
            <p class="panel-kicker">身份入口</p>
            <h2>进入工作台</h2>
          </div>
        </div>

        <p class="panel-lead">
          学生、学校、企业和平台管理员从这里进入各自的档案空间。
        </p>

        <div v-if="!loginVisible" class="entry-actions">
          <button class="start-button" type="button" @click="openLoginPanel">
            <span>进入工作台</span>
            <el-icon><ArrowRight /></el-icon>
          </button>
        </div>

        <transition name="panel-unfold">
          <div v-show="loginVisible" class="login-area">
            <el-form :model="form" :rules="rules" class="login-form">
              <el-form-item prop="username">
                <el-input
                  ref="usernameInput"
                  v-model="form.username"
                  placeholder="用户名"
                  size="large"
                  :prefix-icon="User"
                />
              </el-form-item>
              <el-form-item prop="password">
                <el-input
                  v-model="form.password"
                  type="password"
                  show-password
                  placeholder="密码"
                  size="large"
                  :prefix-icon="Lock"
                  @keyup.enter="handleLogin"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" :loading="loading" class="submit-button" @click="handleLogin">
                  登录
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </el-form-item>
            </el-form>

            <div class="panel-foot">
              <span>请使用已注册并审核通过的真实账号</span>
              <router-link to="/register">创建账号</router-link>
            </div>
          </div>
        </transition>
      </section>

      <section
        ref="carouselEl"
        id="archive-flow"
        class="flow-carousel"
        aria-label="实习档案流程"
        @pointermove="steerCarousel"
        @pointerleave="resetCarousel"
      >
        <div class="arc-guide" aria-hidden="true"></div>
        <div class="carousel-window">
          <div class="carousel-track">
            <article
              v-for="card in carouselCards"
              :key="card.key"
              class="morph-card"
              :style="cardStyle(card)"
            >
              <div class="morph-card-shell">
                <div class="card-face card-front">
                  <div class="morph-card-inner">
                    <span>{{ card.kicker }}</span>
                    <strong>{{ card.title }}</strong>
                  </div>
                </div>
                <div class="card-face card-back">
                  <span>{{ card.kicker }}</span>
                  <strong>查看详情</strong>
                  <small>{{ card.title }}</small>
                </div>
              </div>
            </article>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/userStore'
import landingImage from '../assets/landing-alpine-meadow.jpg'
import flow01 from '../assets/flow/flow-01.jpg'
import flow02 from '../assets/flow/flow-02.jpg'
import flow03 from '../assets/flow/flow-03.jpg'
import flow04 from '../assets/flow/flow-04.jpg'
import flow05 from '../assets/flow/flow-05.jpg'
import flow06 from '../assets/flow/flow-06.jpg'
import flow07 from '../assets/flow/flow-07.jpg'
import flow08 from '../assets/flow/flow-08.jpg'
import flow09 from '../assets/flow/flow-09.jpg'
import flow10 from '../assets/flow/flow-10.jpg'
import flow11 from '../assets/flow/flow-11.jpg'
import flow12 from '../assets/flow/flow-12.jpg'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Lock,
  User
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const loginVisible = ref(false)
const usernameInput = ref(null)
const form = ref({ username: '', password: '' })
const scrollProgress = ref(0)
const carouselEl = ref(null)
const carouselWidth = ref(1180)
const carouselShift = ref(0)
const carouselTilt = ref(0)
const carouselFocus = ref(0)
const carouselProgress = ref(0)
const carouselSpeed = ref(0.2)
const targetCarouselSpeed = ref(0.2)
let carouselFrame = null
let carouselResizeObserver = null
let lastCarouselTime = 0

const flowCards = [
  { kicker: '01', title: '实名认证', tint: 'rgba(38, 123, 110, 0.32)', position: '16% 44%', image: flow01 },
  { kicker: '02', title: '实习备案', tint: 'rgba(41, 139, 193, 0.28)', position: '36% 40%', image: flow02 },
  { kicker: '03', title: '日报填报', tint: 'rgba(238, 156, 62, 0.24)', position: '52% 42%', image: flow03 },
  { kicker: '04', title: '过程考核', tint: 'rgba(75, 88, 154, 0.28)', position: '64% 46%', image: flow04 },
  { kicker: '05', title: '企业签章', tint: 'rgba(28, 42, 62, 0.34)', position: '72% 38%', image: flow05 },
  { kicker: '06', title: '链上核验', tint: 'rgba(98, 130, 93, 0.28)', position: '68% 56%', image: flow06 },
  { kicker: '07', title: '毕业归档', tint: 'rgba(198, 132, 66, 0.3)', position: '48% 58%', image: flow07 },
  { kicker: '08', title: '三方确认', tint: 'rgba(67, 112, 138, 0.3)', position: '28% 62%', image: flow08 },
  { kicker: '09', title: '材料补全', tint: 'rgba(194, 101, 77, 0.26)', position: '58% 34%', image: flow09 },
  { kicker: '10', title: '学校复核', tint: 'rgba(54, 86, 151, 0.3)', position: '78% 48%', image: flow10 },
  { kicker: '11', title: '实习评价', tint: 'rgba(124, 149, 77, 0.26)', position: '42% 52%', image: flow11 },
  { kicker: '12', title: '档案查询', tint: 'rgba(36, 58, 76, 0.34)', position: '22% 38%', image: flow12 }
]

const wrapArcOffset = (value, total) => {
  const half = total / 2
  return ((((value + half) % total) + total) % total) - half
}

const clampValue = (value, min, max) => Math.min(Math.max(value, min), max)
const carouselCardWidth = computed(() => Number(clampValue(carouselWidth.value * 0.112, 112, 184).toFixed(2)))
const carouselCardHeight = computed(() => Number((carouselCardWidth.value * 1.318).toFixed(2)))
const carouselHeight = computed(() => Number(clampValue(carouselWidth.value * 0.225, 276, 370).toFixed(2)))

const carouselCards = computed(() => {
  const total = flowCards.length
  const width = carouselWidth.value || 1180
  const arcStep = clampValue(width * 0.105, 92, 168)
  const arcDepth = clampValue(width / 390, 2.1, 4.2)
  const frontZ = clampValue(width / 18, 54, 88)
  const zStep = clampValue(width / 52, 18, 30)
  return flowCards.map((flow, index) => {
    const offset = wrapArcOffset(index - carouselProgress.value, total)
    const distance = Math.abs(offset)
    return {
      ...flow,
      key: flow.kicker,
      rotate: Number((offset * 8.2).toFixed(2)),
      x: Number((offset * arcStep).toFixed(2)),
      y: Number((arcDepth * distance * distance + 2).toFixed(2)),
      z: Number((frontZ - distance * zStep).toFixed(2)),
      scale: Number(Math.max(0.72, 1.08 - distance * 0.055).toFixed(3)),
      opacity: Number(Math.max(0.28, 1 - Math.max(0, distance - 4.6) * 0.34).toFixed(3))
    }
  })
})

const particles = [
  { id: 1, x: 12, y: 28, size: 4, delay: -1.2 },
  { id: 2, x: 18, y: 66, size: 3, delay: -4.1 },
  { id: 3, x: 28, y: 18, size: 5, delay: -2.7 },
  { id: 4, x: 36, y: 72, size: 3, delay: -6.2 },
  { id: 5, x: 44, y: 31, size: 4, delay: -3.6 },
  { id: 6, x: 52, y: 59, size: 5, delay: -5.4 },
  { id: 7, x: 61, y: 22, size: 3, delay: -7.1 },
  { id: 8, x: 68, y: 76, size: 4, delay: -2.2 },
  { id: 9, x: 77, y: 37, size: 5, delay: -8.3 },
  { id: 10, x: 86, y: 62, size: 3, delay: -4.9 },
  { id: 11, x: 91, y: 18, size: 4, delay: -6.8 },
  { id: 12, x: 8, y: 82, size: 5, delay: -3.1 }
]

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const cardStyle = (card) => ({
  '--card-rotate': `${card.rotate}deg`,
  '--card-x': `${card.x}px`,
  '--card-y': `${card.y}px`,
  '--card-z': `${card.z}px`,
  '--card-scale': card.scale,
  '--card-hover-scale': card.scale + 0.05,
  '--card-delay': `${Math.abs(card.x) / -180}s`,
  opacity: card.opacity,
  zIndex: Math.round(card.z + 200),
  backgroundImage: `linear-gradient(145deg, ${card.tint}, rgba(255,255,255,0.14)), url(${card.image || flow01})`,
  backgroundPosition: card.position,
  backgroundSize: 'cover'
})

const updateScroll = () => {
  scrollProgress.value = Math.min(window.scrollY / 420, 1).toFixed(3)
}

const updateCarouselSize = () => {
  const width = carouselEl.value?.getBoundingClientRect().width || window.innerWidth
  carouselWidth.value = Math.max(320, width)
}

const steerCarousel = (event) => {
  const box = event.currentTarget.getBoundingClientRect()
  const pointerRatio = (event.clientX - box.left) / box.width
  const centered = Math.max(-1, Math.min(1, (pointerRatio - 0.5) * 2))
  carouselShift.value = Math.round(centered * 42)
  carouselTilt.value = Number((centered * -2.8).toFixed(2))
  carouselFocus.value = Number(centered.toFixed(3))
  targetCarouselSpeed.value = Number((centered * -1.22 || 0.2).toFixed(3))
}

const resetCarousel = () => {
  carouselShift.value = 0
  carouselTilt.value = 0
  carouselFocus.value = 0
  targetCarouselSpeed.value = 0.2
}

const startCarousel = () => {
  const tick = (time) => {
    if (!lastCarouselTime) lastCarouselTime = time
    const delta = Math.min((time - lastCarouselTime) / 1000, 0.05)
    lastCarouselTime = time
    carouselSpeed.value += (targetCarouselSpeed.value - carouselSpeed.value) * Math.min(1, delta * 5)
    const nextProgress = carouselProgress.value + carouselSpeed.value * delta
    carouselProgress.value = ((nextProgress % flowCards.length) + flowCards.length) % flowCards.length
    carouselFrame = window.requestAnimationFrame(tick)
  }
  carouselFrame = window.requestAnimationFrame(tick)
}

const openLoginPanel = async () => {
  loginVisible.value = true
  await nextTick()
  usernameInput.value?.focus?.()
}

const focusUsername = async () => {
  document.getElementById('login')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  await openLoginPanel()
}

const goByRole = (role) => {
  if (role === 'STUDENT') return router.push('/student/dashboard')
  if (role === 'SCHOOL_ADMIN') return router.push('/school/dashboard')
  if (role === 'ENTERPRISE_HR' || role === 'ENTERPRISE_MENTOR') return router.push('/enterprise/dashboard')
  if (role === 'PLATFORM_ADMIN') return router.push('/platform/dashboard')
  return router.push('/login')
}

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    const data = await userStore.login(form.value.username, form.value.password)
    await goByRole(data.role)
    ElMessage.success('登录成功')
  } catch (e) {
    ElMessage.error(e.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  updateScroll()
  nextTick(updateCarouselSize)
  startCarousel()
  if (window.ResizeObserver) {
    carouselResizeObserver = new ResizeObserver(updateCarouselSize)
    if (carouselEl.value) carouselResizeObserver.observe(carouselEl.value)
  }
  window.addEventListener('scroll', updateScroll, { passive: true })
  window.addEventListener('resize', updateCarouselSize, { passive: true })
})

onBeforeUnmount(() => {
  if (carouselFrame) window.cancelAnimationFrame(carouselFrame)
  carouselResizeObserver?.disconnect()
  window.removeEventListener('scroll', updateScroll)
  window.removeEventListener('resize', updateCarouselSize)
})
</script>

<style scoped>
.auth-page {
  position: relative;
  min-height: 100svh;
  overflow-x: hidden;
  color: #17231c;
  background: #eef5ed;
  font-family: Inter, -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  isolation: isolate;
}

.auth-page::before {
  content: '';
  position: fixed;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(110deg, rgba(248, 252, 247, 0.93) 0%, rgba(248, 252, 247, 0.74) 42%, rgba(29, 48, 42, 0.22) 100%),
    var(--landing-image) center 42% / cover no-repeat;
  transform: scale(calc(1.02 + var(--scroll-progress) * 0.03));
  transform-origin: center;
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
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.8), transparent 82%);
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

.nav-links a:hover {
  background: rgba(19, 34, 25, 0.07);
  color: #132219;
}

.scroll-hero {
  position: relative;
  z-index: 1;
  width: min(1560px, calc(100% - 72px));
  min-height: calc(100svh - 118px);
  margin: 0 auto 40px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.58);
  border-radius: 30px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.74), rgba(248, 252, 247, 0.34) 44%, rgba(35, 54, 45, 0.26) 100%),
    radial-gradient(circle at 23% 18%, rgba(255, 255, 255, 0.88), transparent 28%);
  box-shadow:
    0 36px 100px rgba(19, 34, 25, 0.2),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
}

.scroll-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.08), rgba(251, 254, 250, 0.62) 76%, rgba(247, 251, 246, 0.34) 100%),
    var(--landing-image) center 44% / cover no-repeat;
  opacity: 0.42;
  filter: saturate(1.12);
  transform: translateY(calc(var(--scroll-progress) * -18px)) scale(1.04);
}

.scroll-hero::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 1;
  background:
    radial-gradient(circle at 72% 24%, rgba(255, 255, 255, 0.72), transparent 22%),
    radial-gradient(ellipse at 50% 112%, rgba(8, 20, 18, 0.46) 0%, rgba(39, 61, 51, 0.28) 31%, transparent 58%),
    linear-gradient(180deg, transparent 0%, rgba(236, 244, 234, 0.28) 70%, rgba(21, 35, 29, 0.24) 100%);
  pointer-events: none;
}

.particle-field {
  position: absolute;
  inset: 0;
  z-index: 2;
  overflow: hidden;
  pointer-events: none;
}

.particle-field span {
  position: absolute;
  left: var(--x);
  top: var(--y);
  width: var(--s);
  height: var(--s);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.84);
  box-shadow:
    0 0 18px rgba(122, 201, 180, 0.55),
    0 0 34px rgba(255, 215, 137, 0.24);
  animation: particleFloat 11s ease-in-out infinite;
  animation-delay: var(--delay);
}

.vision-copy {
  position: relative;
  z-index: 4;
  max-width: 760px;
  padding: 118px 0 0 72px;
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

.vision-copy h1 {
  max-width: 700px;
  margin: 0;
  color: #142119;
  font-size: clamp(54px, 5.4vw, 92px);
  font-weight: 950;
  line-height: 0.98;
  letter-spacing: 0;
  text-shadow: 0 22px 70px rgba(255, 255, 255, 0.72);
}

.vision-copy p:not(.eyebrow) {
  max-width: 570px;
  margin: 24px 0 0;
  color: rgba(20, 33, 25, 0.66);
  font-size: 21px;
  font-weight: 800;
  line-height: 1.65;
}

.workspace-entry {
  position: absolute;
  top: 96px;
  right: 72px;
  z-index: 7;
  width: min(420px, calc(100% - 96px));
  min-height: 292px;
  padding: 24px;
  border: 1px solid rgba(255, 255, 255, 0.68);
  border-radius: 28px;
  background: rgba(243, 250, 242, 0.68);
  box-shadow:
    0 28px 80px rgba(19, 34, 25, 0.18),
    inset 0 1px 0 rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(24px);
  overflow: hidden;
}

.workspace-entry.is-open {
  background: rgba(244, 250, 242, 0.82);
}

.entry-glow {
  position: absolute;
  top: -80px;
  right: -70px;
  width: 210px;
  height: 210px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(138, 199, 178, 0.44), transparent 68%);
  animation: glowDrift 8s ease-in-out infinite;
  pointer-events: none;
}

.panel-head {
  position: relative;
  display: flex;
  gap: 14px;
  align-items: center;
}

.panel-icon {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border: 1px solid rgba(19, 34, 25, 0.15);
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.78);
  color: #1a3325;
  box-shadow: 0 14px 34px rgba(19, 34, 25, 0.1);
}

.panel-mark {
  font-family: Georgia, 'Times New Roman', serif;
  font-size: 22px;
  line-height: 1;
}

.panel-kicker {
  margin: 0 0 4px;
  color: rgba(25, 48, 35, 0.52);
  font-size: 12px;
  font-weight: 950;
  letter-spacing: 0.08em;
}

.panel-head h2 {
  margin: 0;
  color: #142119;
  font-size: 31px;
  font-weight: 950;
  line-height: 1;
}

.panel-lead {
  position: relative;
  margin: 20px 0 0;
  color: rgba(20, 33, 25, 0.68);
  font-size: 15px;
  font-weight: 800;
  line-height: 1.7;
}

.entry-actions {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  margin-top: 28px;
}

.start-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 54px;
  border-radius: 999px;
  cursor: pointer;
  font-size: 15px;
  font-weight: 950;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.start-button {
  border: none;
  padding: 0 22px;
  background: #15231a;
  color: #fff;
  box-shadow: 0 18px 36px rgba(21, 35, 26, 0.26);
}

.start-button:hover {
  transform: translateY(-2px);
}

.login-area {
  position: relative;
  margin-top: 22px;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 12px;
}

.login-form :deep(.el-input__wrapper) {
  height: 48px;
  border-color: rgba(20, 33, 25, 0.08) !important;
  border-radius: 16px !important;
  background: rgba(255, 255, 255, 0.72) !important;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.82) !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(20, 33, 25, 0.24) !important;
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.82),
    0 0 0 3px rgba(68, 119, 91, 0.12) !important;
}

.submit-button {
  --el-button-bg-color: transparent !important;
  --el-button-border-color: transparent !important;
  --el-button-hover-bg-color: transparent !important;
  --el-button-hover-border-color: transparent !important;
  --el-button-active-bg-color: transparent !important;
  --el-button-active-border-color: transparent !important;
  width: 100%;
  height: 48px;
  border: none !important;
  border-radius: 16px !important;
  background: #15231a !important;
  color: #fff !important;
  font-weight: 950;
  box-shadow: 0 18px 34px rgba(21, 35, 26, 0.24);
}

.submit-button :deep(span) {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.panel-foot {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-top: 15px;
  color: rgba(20, 33, 25, 0.52);
  font-size: 12px;
  font-weight: 800;
}

.panel-foot a {
  color: #142119;
  font-weight: 950;
  text-decoration: none;
}

.flow-carousel {
  position: absolute;
  left: 0;
  right: 0;
  bottom: -12px;
  z-index: 6;
  height: var(--carousel-height);
  pointer-events: auto;
  perspective: 1700px;
  transform-style: preserve-3d;
}

.flow-carousel::before {
  content: '';
  position: absolute;
  left: 50%;
  bottom: calc(var(--carousel-height) * -0.34);
  width: min(1380px, 92%);
  height: calc(var(--carousel-height) * 0.65);
  border-radius: 50%;
  background:
    radial-gradient(ellipse at center, rgba(255, 255, 255, 0.34) 0%, rgba(205, 224, 208, 0.2) 31%, transparent 68%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.18), rgba(24, 43, 34, 0.2));
  filter: blur(8px);
  transform: translateX(-50%) rotateX(62deg);
  pointer-events: none;
}

.arc-guide {
  position: absolute;
  left: 50%;
  top: calc(var(--carousel-height) * 0.13);
  width: min(1060px, 76%);
  height: calc(var(--carousel-height) * 0.68);
  border-bottom: 1px solid rgba(20, 33, 25, 0.12);
  border-radius: 50%;
  transform: translateX(-50%) rotateX(58deg);
  opacity: 0.52;
  pointer-events: none;
}

.carousel-window {
  position: absolute;
  inset: 0;
  overflow: visible;
  padding: 0;
  mask-image: linear-gradient(90deg, transparent 0%, #000 8%, #000 92%, transparent 100%);
}

.carousel-track {
  position: absolute;
  left: 50%;
  top: calc(var(--carousel-height) * 0.12);
  width: 0;
  height: 0;
  transform-style: preserve-3d;
  transform: translateX(var(--carousel-shift)) rotateZ(var(--carousel-tilt)) rotateX(0deg);
  transition: transform 0.42s cubic-bezier(0.22, 1, 0.36, 1);
  will-change: transform;
}

.flow-carousel:hover .carousel-track,
.carousel-window:hover .carousel-track {
  transform: translateX(var(--carousel-shift)) rotateZ(var(--carousel-tilt)) rotateX(2deg);
}

.morph-card {
  position: absolute;
  left: calc(var(--carousel-card-width) / -2);
  top: 0;
  width: var(--carousel-card-width);
  height: var(--carousel-card-height);
  border-radius: 26px;
  background-color: transparent;
  transform:
    translate3d(var(--card-x), var(--card-y), var(--card-z))
    rotate(var(--card-rotate))
    scale(var(--card-scale));
  transform-origin: center bottom;
  transition: filter 0.28s ease;
  pointer-events: auto;
  perspective: 960px;
  animation:
    cardSettle 0.58s cubic-bezier(0.22, 1, 0.36, 1) both,
    cardFloat 8s ease-in-out infinite;
  animation-delay: 0s, var(--card-delay);
}

.morph-card:hover {
  z-index: 12;
  filter: saturate(1.08);
  transition: transform 0.28s cubic-bezier(0.22, 1, 0.36, 1), filter 0.28s ease;
  transform:
    translate3d(var(--card-x), calc(var(--card-y) - 22px), calc(var(--card-z) + 96px))
    rotate(0deg)
    scale(var(--card-hover-scale));
}

.morph-card-shell {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 26px;
  background-image: inherit;
  background-position: inherit;
  background-size: inherit;
  transform-style: preserve-3d;
  transition: transform 0.52s cubic-bezier(0.22, 1, 0.36, 1), box-shadow 0.22s ease;
  box-shadow:
    0 28px 58px rgba(17, 28, 22, 0.22),
    0 1px 0 rgba(255, 255, 255, 0.72) inset;
}

.morph-card:hover .morph-card-shell {
  transform: rotateY(180deg);
  box-shadow:
    0 36px 76px rgba(17, 28, 22, 0.3),
    0 1px 0 rgba(255, 255, 255, 0.74) inset;
}

.card-face {
  position: absolute;
  inset: 0;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: 26px;
  backface-visibility: hidden;
}

.card-front {
  background-image: inherit;
  background-position: inherit;
  background-size: inherit;
}

.card-back {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  background:
    radial-gradient(circle at 30% 20%, rgba(96, 162, 194, 0.24), transparent 36%),
    linear-gradient(145deg, rgba(12, 22, 33, 0.96), rgba(18, 40, 33, 0.94));
  color: #fff;
  text-align: center;
  transform: rotateY(180deg);
}

.card-back span {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  font-weight: 950;
}

.card-back strong {
  color: #ffffff;
  font-size: 25px;
  font-weight: 950;
}

.card-back small {
  color: rgba(255, 255, 255, 0.66);
  font-size: 13px;
  font-weight: 900;
}

.morph-card-inner {
  position: relative;
  display: flex;
  height: 100%;
  flex-direction: column;
  justify-content: flex-end;
  padding: 17px;
  color: #fff;
  background:
    linear-gradient(180deg, rgba(0, 0, 0, 0.02) 22%, rgba(0, 0, 0, 0.56) 100%),
    radial-gradient(circle at 58% 72%, rgba(255, 190, 87, 0.28), transparent 26%);
}

.morph-card-inner::before {
  content: '';
  position: absolute;
  left: 17px;
  bottom: 58px;
  width: 30px;
  height: 30px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.68);
  box-shadow:
    38px 8px 0 rgba(255, 211, 104, 0.48),
    15px -27px 0 rgba(23, 34, 43, 0.6);
  opacity: 0.9;
}

.morph-card-inner span {
  position: relative;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
  font-weight: 950;
}

.morph-card-inner strong {
  position: relative;
  margin-top: 8px;
  font-size: 21px;
  font-weight: 950;
  line-height: 1.05;
  text-shadow: 0 3px 18px rgba(0, 0, 0, 0.28);
}

.play-mark {
  width: 0;
  height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 12px solid currentColor;
}

.panel-unfold-enter-active,
.panel-unfold-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
}

.panel-unfold-enter-from,
.panel-unfold-leave-to {
  opacity: 0;
  transform: translateY(12px);
}

@keyframes carouselDrift {
  from { transform: translateX(var(--carousel-shift)) rotateZ(var(--carousel-tilt)); }
  to { transform: translateX(var(--carousel-shift)) rotateZ(var(--carousel-tilt)); }
}

@keyframes cardFloat {
  0%, 100% {
    margin-top: 0;
  }
  50% {
    margin-top: -9px;
  }
}

@keyframes cardSettle {
  from {
    opacity: 0;
    filter: blur(5px) saturate(0.92);
  }
  to {
    opacity: 1;
    filter: blur(0) saturate(1);
  }
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

@media (prefers-reduced-motion: reduce) {
  .carousel-track,
  .morph-card,
  .morph-card:hover .morph-card-shell,
  .particle-field span,
  .entry-glow {
    animation: none !important;
  }
}

@media (max-width: 1180px) {
  .auth-nav,
  .scroll-hero {
    width: calc(100% - 40px);
    max-width: 1120px;
  }

  .scroll-hero {
    min-height: 1080px;
  }

  .vision-copy {
    max-width: 660px;
    padding: 76px 36px 0;
  }

  .workspace-entry {
    top: 390px;
    right: 36px;
    left: 36px;
    width: auto;
  }

  .flow-carousel {
    bottom: 12px;
  }
}

@media (max-width: 980px) {
  .auth-nav,
  .scroll-hero {
    width: min(100% - 32px, 900px);
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

  .scroll-hero {
    min-height: 1040px;
    border-radius: 22px;
  }

  .vision-copy {
    padding: 52px 22px 0;
  }

  .vision-copy h1 {
    font-size: clamp(42px, 12vw, 58px);
  }

  .vision-copy p:not(.eyebrow) {
    font-size: 16px;
  }

  .workspace-entry {
    top: 336px;
    right: 18px;
    left: 18px;
    padding: 20px;
    border-radius: 22px;
  }

  .entry-actions {
    grid-template-columns: 1fr;
  }

  .flow-carousel {
    bottom: 8px;
  }
}
</style>
