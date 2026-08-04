<template>
  <canvas ref="canvasRef" class="particle-text-canvas" aria-hidden="true"></canvas>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  words: {
    type: Array,
    default: () => ['ARCHIVE', 'ON CHAIN', 'VERIFY', 'CERTIFY']
  }
})

const canvasRef = ref(null)
let ctx = null
let particles = []
let animationId = 0
let frame = 0
let wordIndex = 0
let width = 0
let height = 0
let pixelStep = 7
let resizeObserver = null

class Particle {
  constructor(x, y) {
    this.x = x
    this.y = y
    this.vx = 0
    this.vy = 0
    this.tx = x
    this.ty = y
    this.life = 1
    this.killed = false
    this.speed = 0.075 + Math.random() * 0.05
    this.friction = 0.78 + Math.random() * 0.08
    this.size = 1.1 + Math.random() * 1.4
    this.hue = 42 + Math.random() * 28
  }

  move() {
    const dx = this.tx - this.x
    const dy = this.ty - this.y
    this.vx = (this.vx + dx * this.speed) * this.friction
    this.vy = (this.vy + dy * this.speed) * this.friction
    this.x += this.vx
    this.y += this.vy
    if (this.killed) this.life *= 0.985
  }

  draw(context) {
    const glow = this.killed ? this.life * 0.35 : 0.78
    context.fillStyle = `hsla(${this.hue}, 92%, ${this.killed ? 62 : 72}%, ${glow})`
    context.fillRect(this.x, this.y, this.size, this.size)
  }
}

const randomEdgePoint = () => {
  const side = Math.floor(Math.random() * 4)
  if (side === 0) return { x: Math.random() * width, y: -40 }
  if (side === 1) return { x: width + 40, y: Math.random() * height }
  if (side === 2) return { x: Math.random() * width, y: height + 40 }
  return { x: -40, y: Math.random() * height }
}

const collectTextPoints = (word) => {
  const offscreen = document.createElement('canvas')
  offscreen.width = width
  offscreen.height = height
  const offCtx = offscreen.getContext('2d')
  if (!offCtx) return []

  const fontSize = Math.max(42, Math.min(width / Math.max(word.length * 0.68, 5), height * 0.34))
  offCtx.clearRect(0, 0, width, height)
  offCtx.fillStyle = '#fff'
  offCtx.textAlign = 'center'
  offCtx.textBaseline = 'middle'
  offCtx.font = `800 ${fontSize}px Inter, Arial, sans-serif`
  offCtx.fillText(word, width * 0.5, height * 0.5)

  const imageData = offCtx.getImageData(0, 0, width, height).data
  const points = []
  for (let y = 0; y < height; y += pixelStep) {
    for (let x = 0; x < width; x += pixelStep) {
      const alpha = imageData[(y * width + x) * 4 + 3]
      if (alpha > 20) points.push({ x, y })
    }
  }

  for (let i = points.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[points[i], points[j]] = [points[j], points[i]]
  }
  return points
}

const setWord = (word) => {
  const points = collectTextPoints(word)
  const maxParticles = Math.min(points.length, 1300)

  for (let i = 0; i < maxParticles; i++) {
    const point = points[i]
    let particle = particles[i]
    if (!particle) {
      const start = randomEdgePoint()
      particle = new Particle(start.x, start.y)
      particles[i] = particle
    }
    particle.tx = point.x
    particle.ty = point.y
    particle.killed = false
    particle.life = 1
    particle.hue = 38 + Math.random() * 34
  }

  for (let i = maxParticles; i < particles.length; i++) {
    const exit = randomEdgePoint()
    particles[i].tx = exit.x
    particles[i].ty = exit.y
    particles[i].killed = true
  }
}

const resize = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  const rect = canvas.getBoundingClientRect()
  const dpr = Math.min(window.devicePixelRatio || 1, 2)
  width = Math.max(320, Math.floor(rect.width * dpr))
  height = Math.max(160, Math.floor(rect.height * dpr))
  canvas.width = width
  canvas.height = height
  pixelStep = width < 900 ? 8 : 7
  setWord(props.words[wordIndex % props.words.length])
}

const animate = () => {
  if (!ctx) return
  ctx.fillStyle = 'rgba(2, 10, 18, 0.18)'
  ctx.fillRect(0, 0, width, height)
  ctx.save()
  ctx.shadowColor = 'rgba(255, 214, 120, 0.42)'
  ctx.shadowBlur = 12

  for (let i = particles.length - 1; i >= 0; i--) {
    const particle = particles[i]
    particle.move()
    particle.draw(ctx)
    if (particle.killed && particle.life < 0.05) particles.splice(i, 1)
  }

  ctx.restore()
  frame++
  if (frame % 260 === 0) {
    wordIndex = (wordIndex + 1) % props.words.length
    setWord(props.words[wordIndex])
  }
  animationId = requestAnimationFrame(animate)
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  ctx = canvas.getContext('2d')
  resizeObserver = new ResizeObserver(resize)
  resizeObserver.observe(canvas)
  resize()
  animate()
})

onBeforeUnmount(() => {
  if (animationId) cancelAnimationFrame(animationId)
  if (resizeObserver) resizeObserver.disconnect()
})
</script>
