import { useSystemConfig } from '../stores/systemConfig'

const WEBASE_PROXY = '/webase'
const DEFAULT_WEBASE_URL = 'http://175.178.120.23:5002/WeBASE-Front'

function resolveWeBASEEndpoint(webaseUrl) {
  const base = webaseUrl || WEBASE_PROXY
  const normalizedBase = base.replace(/\/$/, '')
  const normalizedDefault = DEFAULT_WEBASE_URL.replace(/\/$/, '')
  return normalizedBase === WEBASE_PROXY || normalizedBase === normalizedDefault
    ? WEBASE_PROXY + '/trans/handle'
    : normalizedBase + '/trans/handle'
}

const CERT_ABI = [{"constant": true, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "isCertificateExist", "outputs": [{"name": "", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "approveByEnterprise", "outputs": [], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [], "name": "getCertificateCount", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "getCertificateStatus", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [{"name": "certificateId", "type": "uint256"}, {"name": "contentHash", "type": "bytes32"}], "name": "verifyCertificate", "outputs": [{"name": "", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "studentId", "type": "uint256"}, {"name": "contentHash", "type": "bytes32"}, {"name": "schoolAddress", "type": "address"}, {"name": "enterpriseAddress", "type": "address"}], "name": "createCertificate", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "getCertificateInfo", "outputs": [{"name": "internshipId", "type": "uint256"}, {"name": "studentId", "type": "uint256"}, {"name": "contentHash", "type": "bytes32"}, {"name": "timestamp", "type": "uint256"}, {"name": "status", "type": "uint256"}, {"name": "schoolApproved", "type": "bool"}, {"name": "enterpriseApproved", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "certificateId", "type": "uint256"}], "name": "approveBySchool", "outputs": [], "payable": false, "stateMutability": "nonpayable", "type": "function"}]
const RECORD_ABI = [{"constant": false, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "approveRecord", "outputs": [], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}, {"name": "contentHash", "type": "bytes32"}], "name": "verifyRecord", "outputs": [{"name": "", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "rejectRecord", "outputs": [], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "getRecordInfo", "outputs": [{"name": "recordId", "type": "uint256"}, {"name": "studentId", "type": "uint256"}, {"name": "recordDateHash", "type": "bytes32"}, {"name": "contentHash", "type": "bytes32"}, {"name": "timestamp", "type": "uint256"}, {"name": "status", "type": "uint256"}, {"name": "mentorApproved", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": false, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "studentId", "type": "uint256"}, {"name": "recordDateHash", "type": "bytes32"}, {"name": "contentHash", "type": "bytes32"}, {"name": "studentAddress", "type": "address"}, {"name": "mentorAddress", "type": "address"}], "name": "createRecord", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "nonpayable", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "isRecordExist", "outputs": [{"name": "", "type": "bool"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [], "name": "getTotalRecordCount", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}, {"name": "recordIndex", "type": "uint256"}], "name": "getRecordStatus", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}, {"constant": true, "inputs": [{"name": "internshipId", "type": "uint256"}], "name": "getRecordCount", "outputs": [{"name": "", "type": "uint256"}], "payable": false, "stateMutability": "view", "type": "function"}]

const SCHOOL_ADDR = '0xb7eb7e43768158c27df1cebd03e8aabde0cd0bfb'
const ENTERPRISE_ADDR = '0x79db0f6b2b819c0856e5db313290905e30f463d4'

function requireChainId(value, label) {
  if (value === undefined || value === null || value === '') {
    throw new Error(label + '不能为空')
  }
  return String(value)
}

async function callWeBASE(contractAddress, abi, funcName, params, user) {
  const sysCfg = useSystemConfig()
  if (!sysCfg.autoChain) {
    console.log('[Chain] Auto-chain disabled, skip:', funcName)
    return null
  }
  try {
    const endpoint = resolveWeBASEEndpoint(sysCfg.webaseUrl)
    const res = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        groupId: 1,
        user: user || SCHOOL_ADDR,
        contractAddress: contractAddress,
        funcName: funcName,
        funcParam: params || [],
        contractAbi: abi
      })
    })
    const data = await res.json()
    if (Array.isArray(data)) {
      console.log('[Chain]', funcName, '=>', data[0])
      return data[0]
    }
    if (data && data.code === 0) {
      console.log('[Chain]', funcName, '=> success')
      return data.data || data
    }
    console.warn('[Chain]', funcName, 'error:', data)
    return null
  } catch (e) {
    console.warn('[Chain]', funcName, 'failed:', e.message)
    return null
  }
}

export async function createCertificate(internshipId, studentId, contentHash, schoolAddr, enterpriseAddr) {
  const sysCfg = useSystemConfig()
  return callWeBASE(sysCfg.certContract, CERT_ABI, 'createCertificate',
    [requireChainId(internshipId, '实习ID'), requireChainId(studentId, '学生ID'),
     contentHash || '0x' + '0'.repeat(64),
     schoolAddr || SCHOOL_ADDR, enterpriseAddr || ENTERPRISE_ADDR],
    enterpriseAddr || ENTERPRISE_ADDR)
}

export async function createRecord(internshipId, studentId, recordDateHash, contentHash, studentAddr, mentorAddr) {
  const sysCfg = useSystemConfig()
  return callWeBASE(sysCfg.recordContract, RECORD_ABI, 'createRecord',
    [requireChainId(internshipId, '实习ID'), requireChainId(studentId, '学生ID'),
     recordDateHash || '0x' + '0'.repeat(64),
     contentHash || '0x' + '0'.repeat(64),
     studentAddr || SCHOOL_ADDR, mentorAddr || ENTERPRISE_ADDR],
    mentorAddr || ENTERPRISE_ADDR)
}

export async function getCertificateCount() {
  const sysCfg = useSystemConfig()
  return callWeBASE(sysCfg.certContract, CERT_ABI, 'getCertificateCount', [], SCHOOL_ADDR)
}

export async function getTotalRecordCount() {
  const sysCfg = useSystemConfig()
  return callWeBASE(sysCfg.recordContract, RECORD_ABI, 'getTotalRecordCount', [], SCHOOL_ADDR)
}
