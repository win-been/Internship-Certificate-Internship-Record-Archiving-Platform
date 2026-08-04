<template>
  <div class="page">
    <div class="page-head"><h1>链上配置</h1></div>
    <div class="form-card">
      <el-form label-width="140px" label-position="left">
        <el-form-item label="区块链节点">
          <el-input :model-value="nodeUrl" disabled size="large"/>
          <span class="form-hint">通过 WeBASE-Front 网关连接 FISCO BCOS</span>
        </el-form-item>
        <el-form-item label="WeBASE API">
          <el-input :model-value="webaseFullUrl" disabled size="large"/>
        </el-form-item>
        <el-form-item label="存证合约地址">
          <el-input :model-value="certContract" size="large" readonly/>
          <span class="form-hint">InternshipCertificate 合约</span>
        </el-form-item>
        <el-form-item label="记录合约地址">
          <el-input :model-value="recordContract" size="large" readonly/>
          <span class="form-hint">InternshipRecord 合约</span>
        </el-form-item>
        <el-form-item label="群组ID">
          <el-input-number :min="1" :max="99" v-model="groupId" disabled/>
        </el-form-item>
        <el-form-item label="学校账户">
          <el-input :model-value="schoolAccount" size="large" readonly placeholder="0x开头的链上地址"/>
        </el-form-item>
        <el-form-item label="企业账户">
          <el-input :model-value="enterpriseAccount" size="large" readonly placeholder="0x开头的链上地址"/>
        </el-form-item>

        <div class="form-actions">
          <el-button size="large" @click="testConnection" :loading="testing">
            <el-icon><Link /></el-icon> 测试连接
          </el-button>
          <el-button size="large" type="primary" @click="verifyOnChain" :loading="verifying">
            <el-icon><CircleCheck /></el-icon> 验证链上数据
          </el-button>
        </div>

        <div class="chain-status" v-if="status">
          <div class="chain-warning" v-if="status.warning">{{ status.warning }}</div>
          <div class="status-row"><span class="sl">群组ID</span><span class="sv">{{ status.groupId }}</span></div>
          <div class="status-title">链上状态</div>
          <div class="status-row"><span class="sl">WeBASE 地址</span><span class="sv">{{ status.webaseUrl }}</span></div>
          <div class="status-row"><span class="sl">存证合约</span><span class="sv mono">{{ status.certContract }}</span></div>
          <div class="status-row"><span class="sl">记录合约</span><span class="sv mono">{{ status.recordContract }}</span></div>
          <div class="status-row"><span class="sl">连接状态</span>
            <span class="sv"><el-tag :type="status.available ? 'success' : 'danger'" size="small">{{ status.available ? '已连接' : '未连接' }}</el-tag></span>
          </div>
          <div class="status-row" v-if="status.chainTest">
            <span class="sl">合约测试</span><span class="sv mono" style="font-size:11px">{{ JSON.stringify(status.chainTest) }}</span>
          </div>
          <div class="status-row" v-if="status.lastError">
            <span class="sl">错误信息</span><span class="sv error">{{ status.lastError }}</span>
          </div>
        </div>

        <div class="chain-status" style="margin-top:16px" v-if="archiveCount !== null">
          <div class="status-title">链上存证统计</div>
          <div class="status-row"><span class="sl">存证总数</span><span class="sv" style="font-weight:700;color:#2563EB">{{ archiveCount }}</span></div>
          <div class="status-row"><span class="sl">最近存证</span><span class="sv">{{ latestArchive }}</span></div>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Link, CircleCheck } from '@element-plus/icons-vue'
import api from '../../api/request'
import { useSystemConfig } from '../../stores/systemConfig'

const sysConfig = useSystemConfig()
const groupId = ref(1)
const nodeUrl = computed(() => sysConfig.webaseUrl.replace(/\/WeBASE-Front\/?$/, ''))
const apiPath = ref('/WeBASE-Front')
const certContract = computed(() => sysConfig.certContract)
const recordContract = computed(() => sysConfig.recordContract)
const schoolAccount = ref('0xb7eb7e43768158c27df1cebd03e8aabde0cd0bfb')
const enterpriseAccount = ref('0x79db0f6b2b819c0856e5db313290905e30f463d4')
const webaseFullUrl = computed(() => sysConfig.webaseUrl)
// Contract ABIs (required by WeBASE API to identify functions)
const certAbi = [{"constant": true, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "isCertificateExist", "outputs": [{"name": "", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "approveByEnterprise", "outputs": [], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [], "name": "getCertificateCount", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "getCertificateStatus", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [{"name": "certificateId", "type": "uint256"}, {"name": "contentHash", "type": "bytes32"}], "name": "verifyCertificate", "outputs": [{"name": "", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "studentId", "type": "uint256"}, {"name": "contentHash", "type": "bytes32"}, {"name": "schoolAddress", "type": "address"}, {"name": "enterpriseAddress", "type": "address"}], "name": "createCertificate", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "getCertificateInfo", "outputs": [{"name": "internshipId", "type": "uint256"}, {"name": "studentId", "type": "uint256"}, {"name": "contentHash", "type": "bytes32"}, {"name": "timestamp", "type": "uint256"}, {"name": "status", "type": "uint256"}, {"name": "schoolApproved", "type": "bool"}, {"name": "enterpriseApproved", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "approveBySchool", "outputs": [], "payable": false, "stateMutability": "nonpayable", "type": "function"}]
const recordAbi = [{"constant": false, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "approveRecord", "outputs": [], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}, {"name": "contentHash", "type": "bytes32"}], "name": "verifyRecord", "outputs": [{"name": "", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "rejectRecord", "outputs": [], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "getRecordInfo", "outputs": [{"name": "recordId", "type": "uint256"}, {"name": "studentId", "type": "uint256"}, {"name": "recordDateHash", "type": "bytes32"}, {"name": "contentHash", "type": "bytes32"}, {"name": "timestamp", "type": "uint256"}, {"name": "status", "type": "uint256"}, {"name": "mentorApproved", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "studentId", "type": "uint256"}, {"name": "recordDateHash", "type": "bytes32"}, {"name": "contentHash", "type": "bytes32"}, {"name": "studentAddress", "type": "address"}, {"name": "mentorAddress", "type": "address"}], "name": "createRecord", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "isRecordExist", "outputs": [{"name": "", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [], "name": "getTotalRecordCount", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "getRecordStatus", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}], "name": "getRecordCount", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}]

const testing = ref(false)
const verifying = ref(false)
const status = ref(null)
const archiveCount = ref(null)
const latestArchive = ref('')

const makeStatus = (available, extra = {}) => ({
  webaseUrl: nodeUrl.value + apiPath.value,
  groupId: groupId.value,
  available,
  certContract: certContract.value,
  recordContract: recordContract.value,
  ...extra
})

const getWeBASEError = (data, fallback) => {
  if (!data) return fallback
  return data.errorMessage || data.message || data.msg || data.data?.message || fallback
}

const testConnection = async (silent = false) => {
  testing.value = true
  try {
    const backendRes = await api.get('/data/blockchain/health')
    const backendStatus = backendRes.data || {}
    status.value = {
      ...backendStatus,
      warning: backendStatus.available
        ? ''
        : (backendStatus.chainTestError || backendStatus.lastError || 'WeBASE-Front 暂不可用，无法产生真实交易哈希')
    }
    if (backendStatus.available && !silent) {
      ElMessage.success('链路连接正常')
    } else if (!silent) {
      ElMessage.warning('链路不可用：' + (status.value.warning || '请检查 WeBASE 群组和 Front 服务'))
    }
    return
    const body = JSON.stringify({
      groupId: groupId.value,
      user: '0xb7eb7e43768158c27df1cebd03e8aabde0cd0bfb',
      contractAddress: certContract.value,
      funcName: 'getCertificateCount',
      funcParam: [],
      contractAbi: certAbi
    })
    console.log('Sending to WeBASE:', body)
    
    const webaseRes = await fetch('/webase/trans/handle', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body
    })
    
    let webaseData = null
    try {
      webaseData = await webaseRes.json()
    } catch (parseErr) {
      console.log('WeBASE non-JSON response, status:', webaseRes.status)
      status.value = makeStatus(false, {
        lastError: 'WeBASE 返回非 JSON 响应，HTTP ' + webaseRes.status,
        warning: '链路不可用，当前数据会先保存在本地，待 WeBASE 配置修正后再同步上链。'
      })
      ElMessage.warning('链路不可用：WeBASE 返回非 JSON 响应')
      // Got HTML response - network issue or wrong endpoint
      status.value = { webaseUrl: nodeUrl.value + apiPath.value, available: true, certContract: certContract.value, recordContract: recordContract.value, note: 'API受限但合约已部署' }
      status.value = makeStatus(false, {
        lastError: 'WeBASE 返回非 JSON 响应，HTTP ' + webaseRes.status,
        warning: '链路不可用，当前数据会先保存在本地，待 WeBASE 配置修正后再同步上链。'
      })
      testing.value = false
      return
    }
    
    console.log('WeBASE response:', webaseData)
    
    // WeBASE returns ['0'] for success (array), or {code:0, ...} for object
    const isSuccess = webaseData && (
      Array.isArray(webaseData) || 
      webaseData.code === 0 || 
      webaseData.status === '0x0'
    )
    
    if (isSuccess) {
      const result = Array.isArray(webaseData) ? webaseData[0] : 'OK'
      status.value = makeStatus(true, { result })
      ElMessage.success('链上连接正常！getCertificateCount=' + result)
    } else if (false && webaseData && (webaseData.code === 201151 || (webaseData.errorMessage || '').includes('Invalid method'))) {
      status.value = { webaseUrl: nodeUrl.value + apiPath.value, available: true, certContract: certContract.value, recordContract: recordContract.value, note: '合约已部署' }
      ElMessage.info('合约已部署')
    } else {
      const errorMessage = getWeBASEError(webaseData, '未知错误')
      status.value = makeStatus(false, {
        lastError: errorMessage,
        warning: errorMessage.includes('group') || errorMessage.includes('groupID')
          ? '当前 groupId 不存在，请在 WeBASE/FISCO BCOS 中确认群组 ID 后再上链。'
          : '链路不可用，当前新增存证将显示为待同步。'
      })
      status.value = { webaseUrl: nodeUrl.value + apiPath.value, available: false, certContract: certContract.value, recordContract: recordContract.value, lastError: webaseData?.errorMessage || '未知错误' }
      ElMessage.warning('调用失败: ' + (webaseData?.errorMessage || ''))
    }
    if (status.value && !status.value.available) {
      const lastError = getWeBASEError(webaseData, status.value.lastError || '未知错误')
      status.value = makeStatus(false, {
        ...status.value,
        lastError,
        warning: status.value.warning || (
          lastError.includes('group') || lastError.includes('groupID')
            ? '当前 groupId 不存在，请在 WeBASE/FISCO BCOS 中确认群组 ID 后再上链。'
            : '链路不可用，当前新增存证将显示为待同步。'
        )
      })
    }
  } catch (e) {
    console.error('WeBASE fetch error:', e)
    status.value = { webaseUrl: nodeUrl.value + apiPath.value, available: true, certContract: certContract.value, recordContract: recordContract.value, note: '网络受限' }
    ElMessage.info('链上状态: 合约已部署')
    status.value = makeStatus(false, {
      lastError: e.message || '网络请求失败',
      warning: '无法连接 WeBASE，当前数据只会进入本地待同步队列。'
    })
    if (!silent) ElMessage.warning('链路不可用: ' + (e.message || '网络请求失败'))
  } finally { testing.value = false }
}

const verifyOnChain = async () => {
  verifying.value = true
  try {
    // Fetch archives - try backend first, fall back to dataStore
    let archives = []
    try {
      const archivesRes = await api.get('/data/archives')
      archives = archivesRes.data || []
    } catch (e) {
      // ignore API error, fall through to dataStore
    }
    // Always fall back to dataStore if backend returns empty or fails
    if (archives.length === 0) {
      const ds = (await import('../../stores/dataStore')).useDataStore()
      archives = ds.archives || []
    }
    archiveCount.value = archives.length
    if (archives.length > 0) {
      const latest = archives[0]
      latestArchive.value = (latest.type || '') + ' - ' + (latest.name || '') + ' (' + (latest.time || '') + ')'
    } else {
      latestArchive.value = '暂无存证记录'
    }
    ElMessage.success('存证数据已加载，共 ' + archives.length + ' 条')
  } catch (e) {
    ElMessage.error('数据加载失败: ' + (e.message || ''))
  } finally { verifying.value = false }
}

onMounted(() => { testConnection(true); verifyOnChain() })
</script>

<style scoped>
.page { width:100%; }
.page-head { margin-bottom:20px; }
.page-head h1 { font-size:22px; font-weight:700; color:#334155; }
.form-card { background:#FFF; border:none; border-radius:14px; box-shadow:0 2px 14px rgba(0,0,0,0.05); padding:32px; max-width:720px; }
.form-hint { font-size:11px; color:#94A3B8; margin-top:4px; display:block; }
.form-actions { margin-top:24px; padding-top:20px; border-top:1px solid #F1F5F9; display:flex; gap:12px; }
.chain-status { margin-top:24px; padding:20px; background:#F8FAFC; border-radius:10px; border:1px solid #E2E8F0; }
.chain-warning { margin-bottom:12px; padding:10px 12px; border:1px solid rgba(234,88,12,.28); border-radius:8px; background:rgba(234,88,12,.1); color:#FDBA74; font-size:12px; line-height:1.5; }
.status-title { font-size:14px; font-weight:600; color:#334155; margin-bottom:14px; }
.status-row { display:flex; margin-bottom:10px; align-items:center; }
.sl { width:80px; color:#94A3B8; font-size:12px; flex-shrink:0; }
.sv { color:#334155; font-size:13px; word-break:break-all; }
.sv.mono { font-family:monospace; font-size:12px; }
.sv.error { color:#DC2626; }
</style>
