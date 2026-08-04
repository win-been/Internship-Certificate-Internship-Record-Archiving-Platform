<template>
  <div class="page">
    <div class="page-head">
      <h1>通知管理</h1>
      <el-button type="primary" @click="showDialog = true">发布通知</el-button>
    </div>

    <div class="table-card">
      <el-table :data="dataStore.notices" style="width: 100%" empty-text="暂无通知">
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="内容摘要" min-width="260">
          <template #default="{ row }">
            <span class="notice-summary">{{ noticeContent(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="120" align="center">
          <template #default="{ row }">
            <span class="st" :class="row.type === '考核通知' ? 'st-blue' : 'st-green'">{{ row.type }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="schoolName" label="可见学校" min-width="160">
          <template #default="{ row }">{{ row.schoolName || '本校' }}</template>
        </el-table-column>
        <el-table-column prop="date" label="日期" width="110" />
        <el-table-column label="操作" width="120" align="center">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewNotice(row)">查看</el-button>
            <el-button link type="danger" size="small" @click="deleteNotice(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="table-total">共 {{ dataStore.notices.length }} 条</span>
      </div>
    </div>

    <el-dialog v-model="showDialog" title="发布通知" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="通知标题" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="考核通知" value="考核通知" />
            <el-option label="安全通知" value="安全通知" />
            <el-option label="其他通知" value="其他通知" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="通知内容..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :loading="publishing" @click="publish">发布</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailDialog" title="通知详情" width="560px">
      <div v-if="selectedNotice" class="notice-detail">
        <div class="detail-head">
          <h3>{{ selectedNotice.title || '通知' }}</h3>
          <span class="st" :class="selectedNotice.type === '考核通知' ? 'st-blue' : 'st-green'">
            {{ selectedNotice.type || '其他通知' }}
          </span>
        </div>
        <div class="detail-meta">
          <span>发布方：{{ selectedNotice.from || '学校管理员' }}</span>
          <span>可见学校：{{ selectedNotice.schoolName || '本校' }}</span>
          <span>日期：{{ selectedNotice.date || '-' }}</span>
        </div>
        <div class="detail-content">{{ noticeContent(selectedNotice) }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useDataStore } from '../../stores/dataStore'

const dataStore = useDataStore()
const showDialog = ref(false)
const detailDialog = ref(false)
const selectedNotice = ref(null)
const publishing = ref(false)
const form = reactive({ title: '', type: '考核通知', content: '' })

const publish = async () => {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请输入通知内容')
    return
  }
  publishing.value = true
  try {
    const saved = await dataStore.addNotice({
      ...form,
      from: '学校管理员',
      date: new Date().toISOString().slice(0, 10)
    })
    ElMessage.success('通知已发布，本校学生可查看')
    showDialog.value = false
    Object.assign(form, { title: '', type: '考核通知', content: '' })
    return saved
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '通知发布失败')
  } finally {
    publishing.value = false
  }
}

const noticeContent = (row) => {
  return row?.content?.trim() || '该公告发布时未填写正文，请重新发布包含内容的通知。'
}

const viewNotice = (row) => {
  selectedNotice.value = row
  detailDialog.value = true
}

const deleteNotice = (row) => {
  ElMessageBox.confirm('确定删除？', '提示', { type: 'warning' }).then(() => {
    const idx = dataStore.notices.indexOf(row)
    if (idx >= 0) dataStore.notices.splice(idx, 1)
    ElMessage.success('已删除')
  }).catch(() => {})
}
</script>

<style scoped>
.page { width: 100%; }
.page-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-head h1 { font-size: 22px; font-weight: 700; color: #334155; }
.st { display: inline-block; padding: 2px 10px; border-radius: 10px; font-size: 12px; font-weight: 500; }
.st-blue { background: #EFF6FF; color: #2563EB; }
.st-green { background: #ECFDF5; color: #059669; }
.notice-summary {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #475569;
}
.notice-detail { display: flex; flex-direction: column; gap: 14px; }
.detail-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.detail-head h3 { margin: 0; color: #111827; font-size: 18px; font-weight: 800; }
.detail-meta { display: flex; flex-wrap: wrap; gap: 8px 16px; color: #64748b; font-size: 13px; }
.detail-content {
  min-height: 120px;
  padding: 14px 16px;
  border: 1px solid rgba(148, 163, 184, 0.24);
  border-radius: 8px;
  background: rgba(248, 250, 252, 0.78);
  color: #1f2937;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>
