<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1>权限管控</h1>
        <span class="page-sub">管理系统用户账户与角色权限</span>
      </div>
    </div>

    <!-- Summary Stats -->
    <div class="stat-mini-row">
      <div class="mini-stat" v-for="s in summary" :key="s.label">
        <span class="mini-num" :style="{color:s.color}">{{ s.value }}</span>
        <span class="mini-label">{{ s.label }}</span>
      </div>
    </div>

    <!-- Table -->
    <el-card class="content-card" shadow="never">
      <div class="card-toolbar">
        <el-input v-model="search" placeholder="搜索用户名 / 姓名..." clearable style="width:220px"/>
        <el-select v-model="roleFilter" placeholder="角色筛选" clearable style="width:130px">
          <el-option label="学生" value="STUDENT"/>
          <el-option label="学校管理" value="SCHOOL_ADMIN"/>
          <el-option label="企业HR" value="ENTERPRISE_HR"/>
          <el-option label="平台管理" value="PLATFORM_ADMIN"/>
        </el-select>
        <span class="toolbar-hint">共 {{ filteredUsers.length }} 名用户</span>
      </div>

      <el-table :data="filteredUsers" empty-text="暂无用户数据" size="default" v-loading="loading">
        <el-table-column prop="username" label="用户名" width="130"/>
        <el-table-column label="姓名" width="100">
          <template #default="{row}">{{ displayName(row) }}</template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{row}"><span class="tt" :class="'tt-'+row.roleColor">{{row.roleLabel}}</span></template>
        </el-table-column>
        <el-table-column label="所属学校/机构" min-width="180">
          <template #default="{row}">{{ affiliationName(row) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="140"/>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{row}">
            <span class="st" :class="row.enabled?'st-ok':'st-off'">{{row.enabled?'正常':'已禁用'}}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{row}">
            <el-button link type="primary" size="small" @click="toggleUser(row)">{{row.enabled?'禁用':'启用'}}</el-button>
            <el-button link type="warning" size="small" @click="resetPwd(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import api from '../../api/request'
import { useUserStore } from '../../stores/userStore'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const search = ref('')
const roleFilter = ref('')
const users = ref([])

const roleMap = {
  STUDENT: { label: '学生', color: 'blue' },
  SCHOOL_ADMIN: { label: '学校管理', color: 'purple' },
  ENTERPRISE_HR: { label: '企业HR', color: 'green' },
  PLATFORM_ADMIN: { label: '平台管理', color: 'orange' },
}

const isBadText = (value) => !value || /^\?+$/.test(String(value).trim())
const firstGoodText = (...values) => values.find(value => !isBadText(value)) || '-'
const displayName = (row) => firstGoodText(row.realName, row.organizationName, row.username)
const affiliationName = (row) => firstGoodText(row.affiliationName, row.schoolName, row.organizationName)

onMounted(async () => {
  loading.value = true
  try {
    const res = await api.get('/auth/users')
    users.value = (res.data || []).map(u => ({
      ...u,
      roleLabel: roleMap[u.role]?.label || u.role,
      roleColor: roleMap[u.role]?.color || 'gray',
      enabled: u.enabled !== false,
    }))
  } catch (e) {
    users.value = []
    if (userStore.isLoggedIn && route.path === '/platform/permissions') {
      ElMessage.error('用户数据加载失败')
    }
  } finally {
    loading.value = false
  }
})

const filteredUsers = computed(() => {
  let list = users.value
  if (search.value) {
    const kw = search.value.toLowerCase()
    list = list.filter(u =>
      (u.username || '').toLowerCase().includes(kw)
      || displayName(u).toLowerCase().includes(kw)
      || affiliationName(u).toLowerCase().includes(kw)
    )
  }
  if (roleFilter.value) list = list.filter(u => u.role === roleFilter.value)
  return list
})

const summary = computed(() => [
  { label: '总用户', value: users.value.length, color: '#334155' },
  { label: '已启用', value: users.value.filter(u => u.enabled).length, color: '#059669' },
  { label: '已禁用', value: users.value.filter(u => !u.enabled).length, color: '#DC2626' },
  { label: '平台管理', value: users.value.filter(u => u.role === 'PLATFORM_ADMIN').length, color: '#7C3AED' },
])

const toggleUser = (row) => {
  const action = row.enabled ? '禁用' : '启用'
  ElMessageBox.confirm('确定' + action + '用户 ' + displayName(row) + '？', '提示', { type: 'warning' }).then(async () => {
    const enabled = !row.enabled
    const res = await api.put('/auth/users/' + row.id + '/enabled', { enabled })
    row.enabled = res.data?.enabled !== false
    ElMessage.success('已' + action + '，并已写入链上归档')
  }).catch(() => {})
}

const resetPwd = (row) => {
  ElMessageBox.confirm('确定重置 ' + displayName(row) + ' 的密码为 123456？', '重置密码', { type: 'warning' }).then(async () => {
    try {
      await api.put('/auth/users/' + row.id + '/reset-pwd', { password: '123456' })
    } catch(e) {
      ElMessage.error(e?.response?.data?.message || '密码重置失败')
      return
    }
    ElMessage.success(displayName(row) + ' 密码已重置为 123456，并已写入链上归档')
  }).catch(() => {})
}
</script>

<style scoped>
.page { width: 100%; }
.page-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.page-head h1 { font-size: 22px; font-weight: 700; color: #334155; }
.page-sub { font-size: 13px; color: #94A3B8; display: block; margin-top: 2px; }
.stat-mini-row { display: flex; gap: 16px; margin-bottom: 16px; }
.mini-stat { flex: 1; background: #FFF; border-radius: 12px; padding: 14px 16px; text-align: center; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.mini-num { font-size: 24px; font-weight: 700; display: block; }
.mini-label { font-size: 12px; color: #94A3B8; margin-top: 2px; display: block; }
.content-card { border-radius: 14px; border: none; box-shadow: 0 2px 14px rgba(0,0,0,0.05); }
.card-toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.toolbar-hint { font-size: 12px; color: #94A3B8; }
.tt { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.tt-blue { background: #EFF6FF; color: #2563EB; }
.tt-purple { background: #F5F3FF; color: #7C3AED; }
.tt-green { background: #ECFDF5; color: #059669; }
.tt-orange { background: #FFF7ED; color: #EA580C; }
.st { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.st-ok { background: #ECFDF5; color: #059669; }
.st-off { background: #FEF2F2; color: #DC2626; }
.st-pend { background: #FFF7ED; color: #EA580C; }
</style>
