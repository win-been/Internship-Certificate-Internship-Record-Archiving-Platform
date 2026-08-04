<template>
  <div class="chain-badge" :class="{ off: !txHash }">
    <template v-if="txHash">
      <el-tooltip :content="tooltip" placement="top" :show-after="100">
        <span class="badge on" @click="copy">
          <span class="dot"><span class="ring"></span></span>
          <span class="label">已上链</span>
          <span class="hash">{{ shortHash }}</span>
          <el-icon class="copy-ico"><CopyDocument /></el-icon>
        </span>
      </el-tooltip>
      <span v-if="indexLabel !== null" class="idx">#{{ indexLabel }}</span>
    </template>
    <span v-else class="badge none">
      <span class="dot-off"></span>
      <span class="label">未上链</span>
    </span>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CopyDocument } from '@element-plus/icons-vue'

const props = defineProps({
  txHash: { type: String, default: '' },
  index: { type: [Number, String], default: null },
})

const shortHash = computed(() => {
  const h = props.txHash || ''
  if (h.length <= 14) return h
  return h.slice(0, 8) + '...' + h.slice(-6)
})

const indexLabel = computed(() => (props.index === null || props.index === undefined || props.index === '') ? null : props.index)

const tooltip = computed(() => `交易哈希：${props.txHash}\n点击复制`)

const copy = async () => {
  try {
    await navigator.clipboard.writeText(props.txHash)
    ElMessage.success('交易哈希已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动复制')
  }
}
</script>

<style scoped>
.chain-badge { display: inline-flex; align-items: center; gap: 6px; }

.badge {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 3px 10px; border-radius: 999px;
  font-size: 12px; line-height: 1; white-space: nowrap;
  animation: pop-in .35s cubic-bezier(.34, 1.56, .64, 1);
}

.badge.on {
  cursor: pointer;
  color: #0e7490;
  background: linear-gradient(135deg, rgba(6, 182, 212, .14), rgba(16, 185, 129, .14));
  border: 1px solid rgba(6, 182, 212, .35);
  transition: transform .18s ease, box-shadow .18s ease, background .18s ease;
}
.badge.on:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(6, 182, 212, .28);
  background: linear-gradient(135deg, rgba(6, 182, 212, .22), rgba(16, 185, 129, .22));
}
.badge.on:active { transform: scale(.96); }

.badge.none {
  color: #94a3b8;
  background: rgba(148, 163, 184, .12);
  border: 1px solid rgba(148, 163, 184, .25);
}

.label { font-weight: 600; }

.hash {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', monospace;
  font-size: 11px; opacity: .85; letter-spacing: .2px;
}

.copy-ico { font-size: 12px; opacity: .55; transition: opacity .18s ease; }
.badge.on:hover .copy-ico { opacity: 1; }

.idx {
  font-size: 11px; color: #64748b;
  font-family: 'SFMono-Regular', Consolas, monospace;
}

/* pulsing dot */
.dot {
  position: relative; width: 8px; height: 8px;
  border-radius: 50%; background: #10b981; flex: none;
}
.dot .ring {
  position: absolute; inset: 0; border-radius: 50%;
  background: #10b981; opacity: .6;
  animation: pulse 1.6s ease-out infinite;
}
.dot-off { width: 7px; height: 7px; border-radius: 50%; background: #94a3b8; flex: none; }

@keyframes pulse {
  0% { transform: scale(1); opacity: .6; }
  70% { transform: scale(2.6); opacity: 0; }
  100% { transform: scale(2.6); opacity: 0; }
}

@keyframes pop-in {
  0% { opacity: 0; transform: scale(.8); }
  100% { opacity: 1; transform: scale(1); }
}
</style>
