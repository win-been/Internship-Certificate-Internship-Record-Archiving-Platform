<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>配置参数</h1>
        <span class="page-sub">维护审核策略、链上存证和智能助手开关</span>
      </div>
    </div>

    <div class="config-layout">
      <div class="form-card config-form">
        <el-form :model="config" label-width="150px" label-position="left">
          <el-form-item label="平台名称">
            <el-input v-model="config.platformName" size="large" placeholder="实习存证平台"/>
          </el-form-item>
          <el-form-item label="审核模式">
            <el-select v-model="config.reviewMode" size="large">
              <el-option label="人工审核" value="manual"/>
              <el-option label="自动通过" value="auto"/>
            </el-select>
          </el-form-item>
          <div class="switch-grid">
            <el-form-item label="存证自动上链">
              <el-switch v-model="config.autoChain"/>
              <span class="form-hint">{{ config.autoChain ? '操作后自动调用合约上链存证' : '仅本地存储，手动触发上链' }}</span>
            </el-form-item>
            <el-form-item label="AI助手">
              <el-switch v-model="config.aiEnabled"/>
              <span class="form-hint">{{ config.aiEnabled ? '已开启AI评语和智能分析' : 'AI功能已关闭' }}</span>
            </el-form-item>
          </div>
          <el-form-item label="WeBASE-Front地址">
            <el-input v-model="config.webaseUrl" size="large" placeholder="http://175.178.120.23:5002/WeBASE-Front"/>
          </el-form-item>
          <el-form-item label="证书合约地址">
            <el-input v-model="config.certContract" size="large" placeholder="0x..."/>
          </el-form-item>
          <el-form-item label="记录合约地址">
            <el-input v-model="config.recordContract" size="large" placeholder="0x..."/>
          </el-form-item>
          <el-divider/>
          <div class="form-actions">
            <el-button size="large" @click="resetConfig">恢复默认</el-button>
            <el-button type="primary" size="large" @click="save">保存配置</el-button>
          </div>
        </el-form>
      </div>

      <aside class="config-side" aria-label="配置状态">
        <section class="status-card primary">
          <span>当前策略</span>
          <strong>{{ config.reviewMode === 'manual' ? '人工审核' : '自动通过' }}</strong>
          <p>入驻、备案与归档流程将按该策略进入后续处理。</p>
        </section>
        <section class="status-card">
          <div class="side-row">
            <span>自动上链</span>
            <strong>{{ config.autoChain ? '已开启' : '未开启' }}</strong>
          </div>
          <div class="side-row">
            <span>AI助手</span>
            <strong>{{ config.aiEnabled ? '已开启' : '未开启' }}</strong>
          </div>
        </section>
        <section class="status-card contract-card">
          <span>链路摘要</span>
          <p>{{ config.webaseUrl || '未配置 WeBASE-Front 地址' }}</p>
          <small>证书合约：{{ shortAddress(config.certContract) }}</small>
          <small>记录合约：{{ shortAddress(config.recordContract) }}</small>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useSystemConfig } from '../../stores/systemConfig'

const sysConfig = useSystemConfig()

const config = reactive({
  platformName: sysConfig.platformName,
  reviewMode: sysConfig.reviewMode,
  autoChain: sysConfig.autoChain,
  aiEnabled: sysConfig.aiEnabled,
  webaseUrl: sysConfig.webaseUrl,
  certContract: sysConfig.certContract,
  recordContract: sysConfig.recordContract,
})

const save = () => {
  sysConfig.save({ ...config })
  ElMessage.success('配置已保存生效')
}

const resetConfig = () => {
  sysConfig.reset()
  Object.assign(config, {
    platformName: sysConfig.platformName,
    reviewMode: sysConfig.reviewMode,
    autoChain: sysConfig.autoChain,
    aiEnabled: sysConfig.aiEnabled,
    webaseUrl: sysConfig.webaseUrl,
    certContract: sysConfig.certContract,
    recordContract: sysConfig.recordContract,
  })
  ElMessage.info('已恢复默认')
}

const shortAddress = (address) => {
  if (!address) return '未配置'
  return address.length > 18 ? `${address.slice(0, 10)}...${address.slice(-6)}` : address
}
</script>

<style scoped>
.page { width:100%; }
.page-head { margin-bottom:18px; }
.page-head h1 { margin:0; font-size:24px; font-weight:800; color:#202721; }
.page-sub { display:block; margin-top:4px; color:rgba(32,39,33,.52); font-size:13px; }
.config-layout { display:grid; grid-template-columns:minmax(620px,1.25fr) minmax(300px,.75fr); gap:18px; align-items:start; }
.form-card { background:#FFF; border:none; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:32px; max-width:none; }
.config-form :deep(.el-form-item) { margin-bottom:20px; }
.switch-grid { display:grid; grid-template-columns:1fr 1fr; gap:10px 18px; }
.form-hint { display:inline-flex; margin-left:10px; color:#8A968F; font-size:12px; line-height:1.4; }
.form-actions { margin-top:24px; padding-top:20px; border-top:1px solid rgba(32,39,33,.08); display:flex; gap:12px; justify-content:flex-end; }
.config-side { display:grid; gap:14px; }
.status-card {
  overflow:hidden;
  position:relative;
  padding:22px;
  border:1px solid rgba(32,39,33,.08);
  border-radius:20px;
  background:linear-gradient(145deg,rgba(255,255,255,.9),rgba(238,245,236,.64));
  box-shadow:0 20px 54px rgba(32,39,33,.08), inset 0 1px 0 rgba(255,255,255,.86);
}
.status-card::after {
  content:'';
  position:absolute;
  right:-42px;
  bottom:-52px;
  width:150px;
  height:150px;
  border-radius:50%;
  background:radial-gradient(circle,rgba(32,39,33,.1),transparent 66%);
}
.status-card span { display:block; color:rgba(32,39,33,.5); font-size:12px; font-weight:900; }
.status-card strong { display:block; margin-top:10px; color:#202721; font-size:26px; font-weight:950; line-height:1; }
.status-card p { position:relative; z-index:1; margin:14px 0 0; color:rgba(32,39,33,.62); font-size:13px; line-height:1.7; word-break:break-all; }
.status-card small { position:relative; z-index:1; display:block; margin-top:10px; color:rgba(32,39,33,.52); font-size:12px; word-break:break-all; }
.status-card.primary { min-height:150px; background:linear-gradient(135deg,rgba(32,39,33,.92),rgba(46,68,56,.78)); color:#fff; }
.status-card.primary span,
.status-card.primary p { color:rgba(255,255,255,.66); }
.status-card.primary strong { color:#fff; }
.side-row { position:relative; z-index:1; display:flex; align-items:center; justify-content:space-between; gap:16px; padding:12px 0; }
.side-row + .side-row { border-top:1px solid rgba(32,39,33,.08); }
.side-row strong { margin:0; font-size:16px; }
.contract-card { min-height:170px; }
@media (max-width:1180px) {
  .config-layout { grid-template-columns:1fr; }
  .switch-grid { grid-template-columns:1fr; }
}
@media (max-width:720px) {
  .form-card,
  .status-card { padding:22px; border-radius:18px; }
  .config-form :deep(.el-form-item__label) { float:none; display:block; width:auto !important; text-align:left; }
  .config-form :deep(.el-form-item__content) { margin-left:0 !important; }
  .form-actions { flex-direction:column; }
}
</style>
