<template>
  <div class="page identity-page">
    <div class="page-head">
      <div>
        <p class="eyebrow">Student Verification</p>
        <h1>实名认证</h1>
      </div>
      <span class="status-tag" :class="statusMeta.className">{{ statusMeta.text }}</span>
    </div>

    <div class="form-card">
      <div class="profile-strip">
        <div>
          <span>当前账号</span>
          <strong>{{ currentUser.username || '-' }}</strong>
        </div>
        <div>
          <span>库内姓名</span>
          <strong>{{ currentUser.realName || '待审核写入' }}</strong>
        </div>
        <div>
          <span>学校 / 专业</span>
          <strong>{{ schoolMajorText }}</strong>
        </div>
      </div>

      <el-alert
        v-if="identityStatus === 'PENDING'"
        title="认证资料已提交，正在等待所属学校审核。审核通过后才会写入个人信息并上链。"
        type="warning"
        show-icon
        :closable="false"
      />
      <el-alert
        v-else-if="identityStatus === 'APPROVED'"
        title="认证已通过，当前展示的是数据库中已持久化的实名信息。"
        type="success"
        show-icon
        :closable="false"
      />
      <el-alert
        v-else-if="identityStatus === 'REJECTED'"
        title="上次认证被驳回，可以修改资料后重新提交。"
        type="error"
        show-icon
        :closable="false"
      />

      <el-form :model="form" label-width="110px" label-position="left" class="identity-form">
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" placeholder="请输入真实姓名" size="large" />
        </el-form-item>
        <el-form-item label="身份证号">
          <el-input v-model="form.idCard" placeholder="请输入身份证号码" maxlength="18" show-word-limit size="large" />
        </el-form-item>
        <el-form-item label="学校">
          <el-input v-model="form.school" disabled placeholder="注册时绑定的学校" size="large" />
        </el-form-item>
        <el-form-item label="专业">
          <el-input v-model="form.major" placeholder="请输入专业" size="large" />
        </el-form-item>
        <el-form-item label="学生证">
          <el-upload drag action="#" :auto-upload="false">
            <el-icon :size="34" color="#64748b"><UploadFilled /></el-icon>
            <div class="up-text">点击或拖拽学生证照片</div>
          </el-upload>
        </el-form-item>
        <el-form-item label="身份证照片">
          <el-upload drag action="#" :auto-upload="false">
            <el-icon :size="34" color="#64748b"><UploadFilled /></el-icon>
            <div class="up-text">上传身份证正反面</div>
          </el-upload>
        </el-form-item>
        <div class="form-actions">
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            :disabled="identityStatus === 'PENDING' || identityStatus === 'APPROVED'"
            @click="submit"
          >
            {{ submitText }}
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import api from '../../api/request'
import { useUserStore } from '../../stores/userStore'

const userStore = useUserStore()
const loading = ref(false)
const currentUser = ref({})
const latestApproval = ref(null)
const identityStatus = ref('UNSUBMITTED')
const form = reactive({ realName: '', idCard: '', school: '', major: '' })

const statusMeta = computed(() => {
  const map = {
    APPROVED: { text: '已认证', className: 'tag-ok' },
    PENDING: { text: '待审核', className: 'tag-pend' },
    REJECTED: { text: '已驳回', className: 'tag-reject' },
    UNSUBMITTED: { text: '未提交', className: 'tag-idle' }
  }
  return map[identityStatus.value] || map.UNSUBMITTED
})

const schoolMajorText = computed(() => {
  const school = currentUser.value.organizationName || form.school || '-'
  return currentUser.value.major ? `${school} / ${currentUser.value.major}` : school
})

const submitText = computed(() => {
  if (identityStatus.value === 'APPROVED') return '已完成认证'
  if (identityStatus.value === 'PENDING') return '等待学校审核'
  return '提交认证'
})

const fillForm = () => {
  const approval = latestApproval.value
  if (approval && approval.status === 'PENDING') {
    Object.assign(form, {
      realName: approval.name || '',
      idCard: approval.code || '',
      school: approval.school || approval.contact || currentUser.value.organizationName || '',
      major: approval.major || ''
    })
    return
  }
  Object.assign(form, {
    realName: currentUser.value.realName || '',
    idCard: currentUser.value.idCard || '',
    school: currentUser.value.organizationName || '',
    major: currentUser.value.major || ''
  })
}

const loadState = async () => {
  loading.value = true
  try {
    const [meRes, approvalRes] = await Promise.all([
      api.get('/auth/me'),
      api.get('/data/identity-approval/current')
    ])
    currentUser.value = approvalRes.data?.user || meRes.data || {}
    userStore.updateUserInfo(currentUser.value)
    latestApproval.value = approvalRes.data?.approval || null
    identityStatus.value = approvalRes.data?.identityStatus || currentUser.value.identityStatus || 'UNSUBMITTED'
    fillForm()
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '认证状态加载失败')
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (identityStatus.value === 'APPROVED') {
    ElMessage.warning('实名认证已通过，不能重复提交')
    return
  }
  if (identityStatus.value === 'PENDING') {
    ElMessage.warning('实名认证正在审核中，请勿重复提交')
    return
  }
  if (!form.school) {
    ElMessage.warning('当前账号未绑定学校，请联系学校录入名单后重新注册')
    return
  }
  if (!form.realName || !form.idCard || !form.major) {
    ElMessage.warning('请填写完整认证信息')
    return
  }
  if (!/^\d{17}[\dXx]$/.test(form.idCard)) {
    ElMessage.warning('身份证号码格式不正确，需要 18 位')
    return
  }
  loading.value = true
  try {
    const res = await api.post('/data/identity-approvals', {
      realName: form.realName,
      idCard: form.idCard,
      major: form.major
    })
    latestApproval.value = res.data
    identityStatus.value = 'PENDING'
    userStore.updateUserInfo({ identityStatus: 'PENDING' })
    ElMessage.success('认证资料已提交，等待所属学校审核')
  } catch (error) {
    ElMessage.error(error?.response?.data?.message || '认证提交失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadState)
</script>

<style scoped>
.page { width: 100%; }
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.eyebrow { margin: 0 0 5px; font-size: 12px; color: #64748b; letter-spacing: 0; }
.page-head h1 { margin: 0; font-size: 24px; font-weight: 800; color: #111827; }
.status-tag { display: inline-block; padding: 5px 14px; border-radius: 999px; font-size: 12px; font-weight: 700; }
.tag-ok { background: #ecfdf5; color: #047857; }
.tag-pend { background: #fff7ed; color: #c2410c; }
.tag-reject { background: #fef2f2; color: #b91c1c; }
.tag-idle { background: #f1f5f9; color: #475569; }
.form-card { max-width: 760px; padding: 28px; background: rgba(255,255,255,0.9); border: 1px solid rgba(226,232,240,0.9); border-radius: 14px; box-shadow: 0 20px 70px rgba(15,23,42,0.08); }
.profile-strip { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 14px; margin-bottom: 18px; }
.profile-strip div { padding: 14px; border-radius: 10px; background: #f8fafc; border: 1px solid #edf2f7; }
.profile-strip span { display: block; margin-bottom: 6px; font-size: 12px; color: #64748b; }
.profile-strip strong { display: block; min-height: 20px; font-size: 14px; color: #0f172a; font-weight: 700; overflow-wrap: anywhere; }
.identity-form { margin-top: 18px; }
.up-text { color: #64748b; font-size: 12px; margin-top: 6px; }
.form-actions { margin-top: 24px; padding-top: 20px; border-top: 1px solid #eef2f7; }
@media (max-width: 760px) {
  .profile-strip { grid-template-columns: 1fr; }
  .form-card { padding: 20px; }
}
</style>
