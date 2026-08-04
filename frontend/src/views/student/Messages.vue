<template>
  <div class="page">
    <div class="page-head">
      <h1>消息通知</h1>
      <div class="head-actions">
        <el-button v-if="unreadCount>0" size="small" text @click="markAllRead">全部标为已读</el-button>
      </div>
    </div>

    <el-card class="msg-card" shadow="never">
      <el-table
        :data="myMessages"
        style="width:100%"
        empty-text="暂无消息"
        @row-click="readMsg"
        highlight-current-row
      >
        <el-table-column label="" width="44" align="center">
          <template #default="{row}">
            <span v-if="!row.read" class="unread-dot"></span>
            <el-icon v-else :size="14" color="#CBD5E1"><CircleCheck /></el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{row}">
            <span class="msg-title" :class="{ unread: !row.read }">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="from" label="发送方" width="130"/>
        <el-table-column prop="date" label="日期" width="120" align="center">
          <template #default="{row}">
            <span class="msg-date">{{ row.date }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-footer">
        <span class="table-total">共 {{ dataStore.messages.length }} 条消息，{{ unreadCount }} 条未读</span>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="消息详情" width="520px" :close-on-click-modal="false">
      <div class="msg-detail" v-if="currentMsg">
        <div class="msg-head">
          <span class="msg-from">{{ currentMsg.from }}</span>
          <span class="msg-date">{{ currentMsg.date }}</span>
        </div>
        <h3>{{ currentMsg.title }}</h3>
        <p>{{ currentMsg.content }}</p>
      </div>
      <template #footer>
        <el-button @click="detailVisible=false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheck } from '@element-plus/icons-vue'
import { useDataStore } from '../../stores/dataStore'
import { useUserStore } from '../../stores/userStore'

const dataStore = useDataStore()
const userStore = useUserStore()
const myName = computed(() => userStore.userInfo?.realName || '')
const myMessages = computed(() => (dataStore.messages || []).filter(m => !m.to || m.to === myName.value || m.from === '系统'))

const detailVisible = ref(false)
const currentMsg = ref(null)

const unreadCount = computed(() => myMessages.value.filter(m => !m.read).length)

const readMsg = (row) => {
  currentMsg.value = row
  detailVisible.value = true
  if (!row.read) dataStore.markMessageRead(row.id)
}

const markAllRead = () => {
  myMessages.value.forEach(m => { m.read = true })
  ElMessage.success('已全部标为已读')
}
</script>

<style scoped>
.page {
  width: 100%;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-head h1 {
  font-size: 22px;
  font-weight: 700;
  color: #1E293B;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.msg-card {
  background: #FFFFFF;
  border: none;
  border-radius: 14px;
  box-shadow: 0 2px 14px rgba(0,0,0,0.05);
  overflow: hidden;
}

.msg-card :deep(.el-card__body) {
  padding: 0;
}

.msg-title {
  font-size: 13px;
  color: #334155;
}

.msg-title.unread {
  font-weight: 600;
  color: #1E293B;
}

.unread-dot {
  width: 8px;
  height: 8px;
  background: #DC2626;
  border-radius: 50%;
  display: inline-block;
}

.msg-date {
  font-size: 12px;
  color: #94A3B8;
}

.table-footer {
  padding: 12px 16px;
  border-top: 1px solid #F1F5F9;
}

.table-total {
  font-size: 12px;
  color: #94A3B8;
}

.msg-detail {
  padding: 4px 0;
}

.msg-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.msg-from {
  font-size: 13px;
  color: #64748B;
}

.msg-detail h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1E293B;
  margin-bottom: 14px;
  line-height: 1.5;
}

.msg-detail p {
  font-size: 14px;
  color: #475569;
  line-height: 1.8;
  white-space: pre-wrap;
}
</style>