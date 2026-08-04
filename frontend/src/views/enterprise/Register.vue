<template>
  <div class="page">
    <div class="page-head"><h1>企业资质管理</h1><span class="status-tag" :class="currentStatusClass">{{ currentStatusLabel }}</span></div>

    <div class="register-layout">
      <!-- Left: Form -->
      <div class="form-card">
        <el-form :model="form" label-width="150px" label-position="left">
          <div class="section-title">基础信息</div>
          <el-row :gutter="24">
            <el-col :span="12"><el-form-item label="企业名称"><el-input v-model="form.name" placeholder="请输入企业全称" size="large"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="统一社会信用代码"><el-input v-model="form.code" placeholder="18位统一社会信用代码" maxlength="18" show-word-limit size="large"/></el-form-item></el-col>
          </el-row>
          <el-row :gutter="24">
            <el-col :span="12"><el-form-item label="联系人"><el-input v-model="form.contact" placeholder="请输入联系人姓名" size="large"/></el-form-item></el-col>
            <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" placeholder="11位手机号码" maxlength="11" show-word-limit size="large"/></el-form-item></el-col>
          </el-row>
          <div class="section-title" style="margin-top:24px">资质文件</div>
          <el-form-item label="营业执照">
            <el-upload drag action="#" :auto-upload="false" :limit="1" class="upload-wide">
              <el-icon :size="40" color="#93C5FD"><UploadFilled /></el-icon>
              <div class="upload-text"><span class="upload-main">点击或拖拽文件至此处上传</span><span class="upload-hint">支持 JPG、PNG、PDF，不超过 10MB</span></div>
            </el-upload>
          </el-form-item>
          <el-form-item label="用工资质">
            <el-upload drag action="#" :auto-upload="false" :limit="5" class="upload-wide">
              <el-icon :size="40" color="#93C5FD"><UploadFilled /></el-icon>
              <div class="upload-text"><span class="upload-main">上传用工资质证明（可选）</span><span class="upload-hint">如劳务派遣许可证、行业资质证书等</span></div>
            </el-upload>
          </el-form-item>
          <div class="form-actions">
            <el-button size="large" class="btn-reset" @click="reset">重置</el-button>
            <el-button type="primary" size="large" @click="submit" :loading="loading" :disabled="!canSubmit">{{ submitButtonText }}</el-button>
          </div>
        </el-form>
      </div>

      <!-- Right: AI Assistant Cards -->
            <!-- Right: Progress + History Cards -->
      <div class="right-cards">
        <!-- Top Card: Review Progress -->
        <div class="prog-card">
          <div class="prog-card-head">
            <span class="prog-title">资质审核进度</span>
            <span class="status-tag-sm" :class="progressStatusClass">{{ progressStatusLabel }}</span>
          </div>
          <div class="prog-steps">
            <div class="prog-step" :class="{ done: qualificationStep >= 1 }">
              <div class="prog-dot"><el-icon :size="12"><Check /></el-icon></div>
              <span class="prog-label">企业提交资料</span>
            </div>
            <div class="prog-line" :class="{ active: qualificationStep >= 2 }"></div>
            <div class="prog-step" :class="{ active: qualificationStep === 2, done: qualificationStep > 2 }">
              <div class="prog-dot">2</div>
              <span class="prog-label">平台资质审核</span>
            </div>
            <div class="prog-line" :class="{ active: qualificationStep >= 3 }"></div>
            <div class="prog-step" :class="{ active: qualificationStep >= 3 }">
              <div class="prog-dot">3</div>
              <span class="prog-label">审核完成</span>
            </div>
          </div>
          <div class="prog-info">
            <div class="prog-info-item"><span class="prog-info-dot blue"></span>当前状态：{{ progressDescription }}</div>
            <div class="prog-info-item"><span class="prog-info-dot gray"></span>审核驳回后会附带平台备注，可修改资料重新提交</div>
          </div>
        </div>

        <!-- Bottom Card: Submission History -->
        <div class="hist-card">
          <div class="hist-card-head">
            <span class="hist-title">提交履历记录</span>
          </div>
          <div class="hist-body">
            <template v-if="historyList.length > 0">
              <div class="hist-item" v-for="(h, i) in historyList" :key="i">
                <div class="hist-row">
                  <span class="hist-time">{{ h.time }}</span>
                  <span class="hist-tag" :class="'tag-' + h.statusColor">{{ h.statusLabel }}</span>
                </div>
                <div class="hist-row sec">
                  <span class="hist-ver">版本 v{{ h.version }}</span>
                  <span class="hist-note" v-if="h.note">{{ h.note }}</span>
                </div>
              </div>
            </template>
            <div v-else class="hist-empty">暂无历史提交/驳回记录</div>
          </div>
        </div></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Check } from '@element-plus/icons-vue'
import { useUserStore } from '../../stores/userStore'
import api from '../../api/request'

const loading = ref(false)
const form = ref({ name:'', code:'', contact:'', phone:'' })
const currentEnterprise = ref(null)
const latestApproval = ref(null)

// Current enterprise status lookup
const currentStatus = computed(() => {
  const userStore = useUserStore()
  return latestApproval.value?.status || currentEnterprise.value?.qualificationStatus || 'UNSUBMITTED'
})
const hasPendingApproval = computed(() => latestApproval.value?.status === 'PENDING')
const canSubmit = computed(() => latestApproval.value?.status !== 'APPROVED' && !hasPendingApproval.value)
const submitButtonText = computed(() => {
  if (currentStatus.value === 'APPROVED') return '资质已通过'
  if (hasPendingApproval.value) return '审核中'
  return '提交审核'
})
const currentStatusLabel = computed(() => {
  const s = currentStatus.value
  if (s === 'APPROVED') return '已入驻'
  if (s === 'REJECTED') return '已驳回'
  if (s === 'PENDING') return '待审核'
  return '待提交'
})
const currentStatusClass = computed(() => {
  const s = currentStatus.value
  if (s === 'APPROVED') return 'green'
  if (s === 'REJECTED') return 'red'
  return 'orange'
})

const progressStatusLabel = computed(() => {
  const s = currentStatus.value
  if (s === 'APPROVED') return '已通过'
  if (s === 'REJECTED') return '已驳回'
  return '待审核'
})
const progressStatusClass = computed(() => {
  const s = currentStatus.value
  if (s === 'APPROVED') return 'green'
  if (s === 'REJECTED') return 'red'
  return 'orange'
})
const qualificationStep = computed(() => {
  const s = currentStatus.value
  if (s === 'APPROVED' || s === 'REJECTED') return 3
  if (s === 'PENDING' || hasPendingApproval.value) return 2
  return 1
})
const progressDescription = computed(() => {
  const s = currentStatus.value
  if (s === 'APPROVED') return '审核完成，企业已入驻'
  if (s === 'REJECTED') return '审核已驳回，请修改资料后重新提交'
  if (s !== 'PENDING') return '请提交企业资质资料，提交后由平台审核员审核'
  return '平台审核中，常规审核时效1~3个工作日'
})

// Pre-fill form from existing enterprise data if already submitted
const loadQualification = async () => {
  const userStore = useUserStore()
  try {
    const res = await api.get('/data/enterprise-qualification/current')
    currentEnterprise.value = res.data?.enterprise || null
    latestApproval.value = res.data?.approval || null
    if (currentEnterprise.value) {
      form.value.name = currentEnterprise.value.name || ''
      form.value.code = currentEnterprise.value.code || ''
      form.value.contact = currentEnterprise.value.contact || ''
      form.value.phone = currentEnterprise.value.phone || ''
    }
  } catch (e) {
    form.value.name = userStore.userInfo?.organizationName || ''
    form.value.contact = userStore.userInfo?.realName || ''
    form.value.phone = userStore.userInfo?.phone || ''
  }
}

onMounted(loadQualification)

const submit = async () => {
  if (!canSubmit.value) {
    ElMessage.warning(currentStatus.value === 'APPROVED' ? '企业资质已通过，不能重复提交' : '企业资质正在审核中，请勿重复提交')
    return
  }
  // Validation
  if (!form.value.name || !form.value.name.trim()) {
    ElMessage.warning('请填写企业名称')
    return
  }
  if (!form.value.code || !form.value.code.trim()) {
    ElMessage.warning('请填写统一社会信用代码')
    return
  }
  if (form.value.code.trim().length !== 18) {
    ElMessage.warning('统一社会信用代码必须为18位')
    return
  }
  if (!form.value.contact || !form.value.contact.trim()) {
    ElMessage.warning('请填写联系人')
    return
  }
  if (!form.value.phone || !form.value.phone.trim()) {
    ElMessage.warning('请填写联系电话')
    return
  }
  if (!/^\d{11}$/.test(form.value.phone.trim())) {
    ElMessage.warning('联系电话必须为11位数字')
    return
  }
  loading.value = true
  try {
    const payload = {
      name: form.value.name.trim(),
      code: form.value.code.trim(),
      contact: form.value.contact.trim(),
      phone: form.value.phone.trim()
    }
    const res = await api.post('/data/enterprise-qualification', payload)
    currentEnterprise.value = res.data?.enterprise || currentEnterprise.value
    latestApproval.value = res.data?.approval || latestApproval.value
    const userStore = useUserStore()
    if (currentEnterprise.value) {
      userStore.updateUserInfo({
        organizationName: currentEnterprise.value.name,
        realName: currentEnterprise.value.contact,
        phone: currentEnterprise.value.phone,
        status: currentEnterprise.value.status
      })
    }
    ElMessage.success('资质信息已提交，等待审核')
    const histEntry = {
      time: new Date().toISOString().slice(0, 19).replace('T', ' '),
      version: historyList.value.length + 1,
      statusLabel: '待审核',
      statusColor: 'orange',
      note: ''
    }
    historyList.value.unshift(histEntry)
    // Also save history to localStorage for persistence
    try {
      const saved = JSON.parse(localStorage.getItem('enterprise_reg_history') || '[]')
      saved.unshift(histEntry)
      localStorage.setItem('enterprise_reg_history', JSON.stringify(saved.slice(0, 20)))
    } catch (e) {}
  } catch (e) {
    ElMessage.error('提交失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const reset = () => { form.value = { name:'', code:'', contact:'', phone:'' } }

// Load history from localStorage cache
const loadHistory = () => {
  try {
    const saved = JSON.parse(localStorage.getItem('enterprise_reg_history') || 'null')
    if (saved && Array.isArray(saved) && saved.length > 0) return saved
  } catch (e) {}
  return []
}
const historyList = ref(loadHistory())</script>

<style scoped>
.page { width:100%; }
.page-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:20px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; letter-spacing:-0.5px; }
.status-tag { display:inline-block; padding:3px 12px; border-radius:14px; font-size:12px; font-weight:500; }
.status-tag.orange { background:#FFFBEB; color:#D97706; }
.status-tag.blue { background:#EFF6FF; color:#2563EB; }
.status-tag.green { background:#ECFDF5; color:#059669; }
.status-tag.red { background:#FEF2F2; color:#DC2626; }

/* Two-column layout */
.register-layout { display:flex; gap:20px; align-items:flex-start; }
.form-card { flex:1; background:#FFF; border:none; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:32px; min-width:0; }
.right-cards { width:340px; flex-shrink:0; display:flex; flex-direction:column; gap:16px; }

/* Section titles */
.section-title { font-size:14px; font-weight:600; color:#334155; margin-bottom:16px; padding-bottom:8px; border-bottom:1px solid #F1F5F9; }

/* Upload */
.upload-wide { width:100% !important; }
.upload-wide :deep(.el-upload-dragger) { min-height:140px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;background:#F8FAFC;border:2px dashed #CBD5E1;border-radius:10px;transition:all 0.2s; }
.upload-wide :deep(.el-upload-dragger:hover) { border-color:#2563EB;background:#EFF6FF; }
.upload-text { display:flex;flex-direction:column;align-items:center;gap:4px; }
.upload-main { color:#64748B;font-size:13px; }
.upload-hint { color:#94A3B8;font-size:11px; }

/* Form actions */
.form-actions { display:flex;justify-content:flex-end;gap:12px;margin-top:32px;padding-top:20px;border-top:1px solid #F1F5F9; }
.btn-reset { background:#FFF !important;border:1px solid #E2E8F0 !important;color:#64748B !important; }
.btn-reset:hover { background:#F8FAFC !important;border-color:#CBD5E1 !important;color:#334155 !important; }

/* Progress Card */
.prog-card { background:#FFF; border:none; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:20px; }
.prog-card-head { display:flex; justify-content:space-between; align-items:center; margin-bottom:18px; }
.prog-title { font-size:15px; font-weight:600; color:#1E293B; }
.status-tag-sm { display:inline-block; padding:2px 10px; border-radius:10px; font-size:11px; font-weight:500; }
.status-tag-sm.orange { background:#FFF7ED; color:#EA580C; }
.status-tag-sm.green { background:#ECFDF5; color:#059669; }
.status-tag-sm.red { background:#FEF2F2; color:#DC2626; }

/* Progress Steps */
.prog-steps { display:flex; align-items:center; margin-bottom:16px; }
.prog-step { display:flex; flex-direction:column; align-items:center; gap:6px; flex-shrink:0; }
.prog-dot { width:28px; height:28px; border-radius:50%; display:flex; align-items:center; justify-content:center; font-size:12px; font-weight:600; color:#94A3B8; background:#F1F5F9; transition:all 0.3s; }
.prog-step.done .prog-dot { background:#2563EB; color:#FFF; }
.prog-step.active .prog-dot { background:#2563EB; color:#FFF; box-shadow:0 0 0 4px rgba(37,99,235,0.15); }
.prog-label { font-size:11px; color:#94A3B8; text-align:center; white-space:nowrap; }
.prog-step.done .prog-label,
.prog-step.active .prog-label { color:#334155; font-weight:500; }
.prog-line { flex:1; height:2px; background:#F1F5F9; margin:0 6px; margin-bottom:20px; border-radius:1px; transition:background 0.3s; }
.prog-line.active { background:#2563EB; }

/* Progress Info */
.prog-info { display:flex; flex-direction:column; gap:8px; }
.prog-info-item { font-size:11px; color:#64748B; line-height:1.5; display:flex; align-items:flex-start; gap:6px; }
.prog-info-dot { width:6px; height:6px; border-radius:50%; margin-top:5px; flex-shrink:0; }
.prog-info-dot.blue { background:#2563EB; }
.prog-info-dot.gray { background:#CBD5E1; }

/* History Card */
.hist-card { background:#FFF; border:none; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:20px; flex:1; display:flex; flex-direction:column; }
.hist-card-head { margin-bottom:14px; }
.hist-title { font-size:15px; font-weight:600; color:#1E293B; }
.hist-body { flex:1; overflow-y:auto; }
.hist-empty { color:#94A3B8; font-size:12px; text-align:center; padding:24px 0; }
.hist-item { padding:10px 0; border-bottom:1px solid #F1F5F9; }
.hist-item:last-child { border-bottom:none; }
.hist-row { display:flex; justify-content:space-between; align-items:center; }
.hist-row.sec { margin-top:4px; }
.hist-time { font-size:12px; color:#64748B; }
.hist-tag { display:inline-block; padding:1px 8px; border-radius:8px; font-size:11px; font-weight:500; }
.hist-tag.tag-orange { background:#FFF7ED; color:#EA580C; }
.hist-tag.tag-green { background:#ECFDF5; color:#059669; }
.hist-tag.tag-red { background:#FEF2F2; color:#DC2626; }
.hist-ver { font-size:11px; color:#94A3B8; }
.hist-note { font-size:11px; color:#64748B; max-width:180px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
</style>
