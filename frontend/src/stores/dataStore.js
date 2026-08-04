import { defineStore } from 'pinia'
import { ref, watch } from 'vue'
import { useUserStore } from './userStore'
import { generateHash } from '../utils/hash'

const STORAGE_KEY = 'platform_data'; const DATA_VERSION = 'v18'
const COUNTER_KEY = 'platform_job_counter'

function loadPersisted() {
  try {
    const version = localStorage.getItem('platform_data_version');
    if (version !== DATA_VERSION) {
      localStorage.removeItem(STORAGE_KEY);
      localStorage.setItem('platform_data_version', DATA_VERSION);
      return null;
    }
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) { return null; }
}

const defaults = {
  jobs: [],
  applications: [],
  hiredStudents: [],
  assessments: [],
  disputes: [],
  archives: [],
  internships: [],
  reports: [],
  notices: [],
  schoolStudents: [],
  enterpriseList: [],
  messages: [],
  approvals: [],
  identityVerified: false,
}
// Unified chain-first operations (backend anchors on chain, localStorage as cache)
import { chainCall } from './chainOps'
async function apiCall(method, url, body) {
  return await chainCall(method, url, body)
}

export const useDataStore = defineStore('data', () => {
  const persisted = loadPersisted()
  const init = persisted || defaults

  const jobs = ref(init.jobs || defaults.jobs)
  let jobIdCounter = parseInt(localStorage.getItem(COUNTER_KEY) || '6', 10)
  const applications = ref(init.applications || defaults.applications)
  const hiredStudents = ref(init.hiredStudents || defaults.hiredStudents)
  const assessments = ref(init.assessments || defaults.assessments)
  const disputes = ref(init.disputes || defaults.disputes)
  const archives = ref(init.archives || defaults.archives)
  const internships = ref(init.internships || defaults.internships)
  const reports = ref(init.reports || defaults.reports)
  const notices = ref(init.notices || defaults.notices)
  const schoolStudents = ref(init.schoolStudents || defaults.schoolStudents)
  const enterpriseList = ref(init.enterpriseList || defaults.enterpriseList)
  const messages = ref(init.messages || defaults.messages)
  const approvals = ref(init.approvals || defaults.approvals)
  const identityVerified = ref(localStorage.getItem('platform_identity_verified') === 'true')

  // Auto-persist to localStorage as fallback
  const persistLocal = () => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify({
        jobs: jobs.value, applications: applications.value,
        hiredStudents: hiredStudents.value, assessments: assessments.value,
        disputes: disputes.value, archives: archives.value,
        internships: internships.value, reports: reports.value, notices: notices.value, schoolStudents: schoolStudents.value, enterpriseList: enterpriseList.value, messages: messages.value, approvals: approvals.value,
      }))
      localStorage.setItem(COUNTER_KEY, String(jobIdCounter))
    } catch (e) {}
  }
  watch([jobs, applications, hiredStudents, assessments, disputes, archives, internships, reports, notices, schoolStudents, enterpriseList, messages, approvals], persistLocal, { deep: true })

  // Backend is authoritative. LocalStorage is only an offline cache.
  const syncFromBackend = async () => {
    const userStore = useUserStore()
    if (!userStore.token) return
    try {
      const api = (await import('../api/request')).default
      const [jobsRes, appsRes, assessRes, disputesRes, archivesRes,
             internsRes, reportsRes, noticesRes, studentsRes, enterRes, msgsRes, approvsRes] = await Promise.all([
        api.get('/data/jobs').catch(() => null),
        api.get('/data/applications').catch(() => null),
        api.get('/data/assessments').catch(() => null),
        api.get('/data/disputes').catch(() => null),
        api.get('/data/archives').catch(() => null),
        api.get('/data/internships').catch(() => null),
        api.get('/data/reports').catch(() => null),
        api.get('/data/notices').catch(() => null),
        api.get('/data/school-students').catch(() => null),
        api.get('/data/enterprise-list').catch(() => null),
        api.get('/data/messages').catch(() => null),
        api.get('/data/approvals').catch(() => null),
      ])
      if (jobsRes) { jobs.value = jobsRes.data || []; if (jobs.value.length) jobIdCounter = Math.max(jobIdCounter, ...jobs.value.map(j => j.id || 0)) + 1 }
      if (appsRes) { applications.value = appsRes.data || [] }
      if (assessRes) { assessments.value = assessRes.data || [] }
      if (disputesRes) { disputes.value = disputesRes.data || [] }
      if (archivesRes) { archives.value = archivesRes.data || [] }
      if (internsRes) { internships.value = internsRes.data || [] }
      if (reportsRes) { reports.value = reportsRes.data || [] }
      if (noticesRes) { notices.value = noticesRes.data || [] }
      if (studentsRes) { schoolStudents.value = studentsRes.data || [] }
      if (enterRes) { enterpriseList.value = enterRes.data || [] }
      if (msgsRes) { messages.value = msgsRes.data || [] }
      if (approvsRes) { approvals.value = approvsRes.data || [] }
      console.log('Data synced from backend: ' + (jobsRes?.data?.length || 0) + ' jobs, ' + (appsRes?.data?.length || 0) + ' apps, ...')
    } catch (e) { console.log('Backend sync skipped:', e.message) }
  }

  // Trigger initial sync
  syncFromBackend()

  // ===== Jobs =====
  const addJob = async (job) => {
    const result = await apiCall('POST', '/data/jobs', { ...job, status: job.status || 'OPEN', count: 0 })
    if (result) {
      jobs.value.push(result)
      return result
    }
    // Fallback local: keep provided id or generate one
    const newJob = { ...job, id: job.id || jobIdCounter++, count: job.count || 0, status: job.status || 'OPEN' }
    jobs.value.push(newJob)
    return newJob
  }

  const updateJob = async (id, data) => {
    await apiCall('PUT', '/data/jobs/' + id, data)
    const idx = jobs.value.findIndex(j => j.id === id)
    if (idx >= 0) Object.assign(jobs.value[idx], data)
  }

  const toggleJobStatus = async (id) => {
    const job = jobs.value.find(j => j.id === id)
    if (!job) return
    const newStatus = job.status === 'OPEN' ? 'CLOSED' : 'OPEN'
    await apiCall('PUT', '/data/jobs/' + id, { status: newStatus })
    job.status = newStatus
  }

  const deleteJob = async (id) => {
    await apiCall('DELETE', '/data/jobs/' + id)
    jobs.value = jobs.value.filter(j => j.id !== id)
  }

  // ===== Applications =====
  const applyJob = async (studentId, studentName, studentSchool, studentMajor, jobId, jobTitle) => {
    const job = jobs.value.find(j => j.id === jobId)
    const companyId = job ? job.companyId : null
    const appData = { 
      studentId, name: studentName, school: studentSchool, major: studentMajor, 
      jobId, jobTitle: jobTitle || job?.title || '', companyId,
      applyDate: new Date().toISOString().slice(0,10), status: 'pending' 
    }
    const result = await apiCall('POST', '/data/applications', appData)
    if (result) {
      applications.value.push(result)
      if (job) job.count++
      return result
    }
    if (job) job.count++
    const localApp = { ...appData, id: Date.now() }
    applications.value.push(localApp)
    return localApp
  }

  const rejectApplication = async (id) => {
    const result = await apiCall('PUT', '/data/applications/' + id, { status: 'rejected' })
    const app = applications.value.find(a => a.id === id)
    if (app) app.status = 'rejected'
    return result || app
  }

  // ===== Hiring =====
  const hireStudent = async (applicationId, position, startDate, salary) => {
    const app = applications.value.find(a => a.id === applicationId)
    if (!app) return
    const result = await apiCall('PUT', '/data/applications/' + applicationId, {
      status: 'accepted',
      position,
      startDate: startDate || new Date().toISOString().slice(0,10)
    })
    app.status = 'accepted'
    applications.value
      .filter(a => a.studentId === app.studentId && a.id !== applicationId && a.status === 'pending')
      .forEach(a => { a.status = 'rejected' })
    const hired = { 
      name: app.name, position, 
      startDate: startDate || new Date().toISOString().slice(0,10), 
      salary, signed: false,
      studentId: app.studentId,
      companyId: app.companyId
    }
    hiredStudents.value.push(hired)
    return result || hired
  }

  const signAgreement = async (target) => {
    const payload = typeof target === 'object' && target !== null ? { ...target } : {}
    const studentName = typeof target === 'string' ? target : target?.name
    const h = studentName ? hiredStudents.value.find(h => h.name === studentName && !h.signed) : null
    const internship = payload.internshipId
      ? internships.value.find(i => i.id === payload.internshipId)
      : studentName
        ? internships.value.find(i => i.studentName === studentName && i.status === 'ACTIVE')
        : null
    if (internship) {
      payload.internshipId = internship.id
      payload.studentId = internship.studentId
      payload.companyId = internship.enterpriseId || internship.companyId
    } else if (h) {
      payload.studentId = h.studentId
      payload.companyId = h.companyId
    }
    const result = await apiCall('PUT', '/data/hired/sign', payload)
    if (h) h.signed = true
    if (internship) {
      internship.agreementSigned = true
      internship.agreementSignedAt = result?.agreementSignedAt || new Date().toISOString().slice(0, 10)
    }
    return result
  }

  // ===== Assessments =====
  const saveAssessment = async (student, month, attendance, score, comment) => {
    const userStore = useUserStore()
    const companyId = userStore.userInfo?.userId || null
    const data = typeof student === 'object' && student !== null
      ? { ...student, companyId: student.companyId || companyId }
      : { student, month, attendance, score, comment, status: 'Completed', companyId }
    if (!data.internshipId && data.student) {
      const internship = internships.value.find(i => i.studentName === data.student && (i.enterpriseId || i.companyId) === data.companyId && i.status === 'ACTIVE')
      if (internship) {
        data.internshipId = internship.id
        data.studentId = internship.studentId
      }
    }
    const result = await apiCall('POST', '/data/assessments', data)
    if (result) {
      const idx = assessments.value.findIndex(a => a.id === result.id || (a.internshipId === result.internshipId && a.month === result.month))
      if (idx >= 0) assessments.value[idx] = result
      else assessments.value.push(result)
      return result
    }
    const a = assessments.value.find(a => a.student === data.student && a.month === data.month)
    if (a) {
      Object.assign(a, data, { status: 'Completed' })
    } else {
      // Create new assessment entry
      const userStore = useUserStore()
      assessments.value.push({
        id: Date.now(),
        ...data,
        status: 'Completed',
        companyId: userStore.userInfo?.userId || null
      })
    }
  }

  // ===== Disputes =====
  const resolveDispute = async (student, opinion) => {
    const d = disputes.value.find(d => d.student === student && d.status === 'PENDING')
    if (!d) return
    const data = { status: 'RESOLVED', opinion, resolvedDate: new Date().toISOString().slice(0,10) }
    await apiCall('PUT', '/data/disputes/' + d.id, data)
    d.status = 'RESOLVED'
    d.opinion = opinion
    d.resolvedDate = new Date().toISOString().slice(0,10)
  }

  // ===== Archives =====
  const addArchive = async (type, name, extra, meta = {}) => {
    const archiveMeta = meta && typeof meta === 'object' ? meta : {}
    const extraText = typeof extra === 'object' ? JSON.stringify(extra) : (extra || Date.now().toString())
    const hash = await generateHash(type + '|' + name + '|' + extraText)
    const blockNum = 18495000 + Math.floor(Date.now() / 1000) % 100000
    const userStore = useUserStore()
    const normalizeId = (value, fallback = null) => {
      if (value === undefined || value === null || value === '') return fallback
      const num = Number(value)
      return Number.isFinite(num) && num > 0 ? num : fallback
    }
    const currentUserId = normalizeId(userStore.userInfo?.userId)
    const currentRole = userStore.userInfo?.role
    const studentId = normalizeId(archiveMeta.studentId, currentRole === 'STUDENT' ? currentUserId : null)
    const companyId = normalizeId(
      archiveMeta.companyId,
      currentRole === 'ENTERPRISE_HR' || currentRole === 'ENTERPRISE_MENTOR' ? currentUserId : null
    )
    const sourceId = archiveMeta.sourceId ?? null
    const data = { type, name, hash, time: new Date().toISOString().replace('T',' ').slice(0,19), block: blockNum, studentId, companyId }
    if (archiveMeta.internshipId !== undefined) data.internshipId = normalizeId(archiveMeta.internshipId)
    if (sourceId !== null && sourceId !== undefined && sourceId !== '') data.sourceId = sourceId
    const normalizeChainState = (record) => {
      if (record?.chainStatus === 'ON_CHAIN') return 'on-chain'
      if (record?.chainStatus === 'LOCAL_FALLBACK') return 'local'
      if (record?.txHash) return 'on-chain'
      return 'local'
    }

    const result = await apiCall('POST', '/data/archives', data)
    const stored = { ...result, chainState: normalizeChainState(result) }
    archives.value.unshift(stored)
    return stored
  }

  const addInternship = async (data) => {
    const entry = { ...data, id: data.id || Date.now(), status: data.status || 'ACTIVE' }
    const result = await apiCall('POST', '/data/internships', entry)
    const stored = result || entry
    // Auto-close any previous active internship for the same student & sync related data
    if (stored.status === 'ACTIVE' && stored.studentName) {
      const prevActive = internships.value.filter(i => i.studentName === stored.studentName && i.status === 'ACTIVE' && i.id !== stored.id)
      prevActive.forEach(i => {
        i.status = 'COMPLETED'
        i.endDate = stored.startDate || new Date().toISOString().slice(0, 10)
      })
      // Sync hiredStudents: update position/company for this student
      const hs = hiredStudents.value.find(h => h.name === stored.studentName)
      if (hs) {
        hs.position = stored.position || hs.position
      }
      // Sync schoolStudents: update enterprise/position/status for this student
      const ss = schoolStudents.value.find(s => s.name === stored.studentName)
      if (ss) {
        ss.enterprise = stored.enterpriseName || ss.enterprise
        ss.position = stored.position || ss.position
        ss.status = 'ACTIVE'
        ss.statusLabel = '在岗'
        ss.statusColor = 'green'
      }
    }
    internships.value.unshift(stored)
    return stored
  }
  const addSchoolStudent = async (data) => {
    const entry = { ...data, status: data.status || 'INACTIVE', statusLabel: 'Off Duty', statusColor: 'orange', id: data.id || Date.now(), schoolId: data.schoolId || null, userId: data.userId || null }
    const result = await apiCall('POST', '/data/school-students', entry)
    schoolStudents.value.push(result || entry)
    return result || entry
  }
  const updateSchoolStudent = async (studentNo, data) => {
    const s = schoolStudents.value.find(s => s.studentNo === studentNo)
    if (s) {
      await apiCall('PUT', '/data/school-students/' + studentNo, data)
      Object.assign(s, data)
    }
  }
  const toggleStudentStatus = async (studentNo) => {
    const s = schoolStudents.value.find(s => s.studentNo === studentNo)
    if (!s) return
    const newStatus = s.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    const newLabel = newStatus === 'ACTIVE' ? 'On Duty' : 'Off Duty'
    const newColor = newStatus === 'ACTIVE' ? 'green' : 'orange'
    await apiCall('PUT', '/data/school-students/' + studentNo + '/status', { status: newStatus })
    s.status = newStatus; s.statusLabel = newLabel; s.statusColor = newColor
  }
  // Platform approval completes enterprise qualification.
  const approveEnterprise = async (id) => {
    const e = enterpriseList.value.find(e => e.id === id)
    if (!e) return
    const result = await apiCall('PUT', '/data/enterprise-list/' + id + '/approve')
    e.status = result?.status || 'APPROVED'; e.statusLabel = result?.statusLabel || '已入驻'; e.statusColor = 'green'
  }
  const rejectEnterprise = async (id) => {
    const e = enterpriseList.value.find(e => e.id === id)
    if (!e) return
    await apiCall('PUT', '/data/enterprise-list/' + id + '/reject')
    e.status = 'REJECTED'; e.statusLabel = 'Rejected'; e.statusColor = 'red'
  }
  const markMessageRead = async (id) => {
    const m = messages.value.find(m => m.id === id)
    if (!m) return
    await apiCall('PUT', '/data/messages/' + id + '/read')
    m.read = true
  }
  const approveItem = async (id) => {
    const a = approvals.value.find(a => a.id === id)
    if (!a) return
    await apiCall('PUT', '/data/approvals/' + id, { status: 'APPROVED' })
    a.status = 'APPROVED'
  }
  const rejectApprovalItem = async (id) => {
    const a = approvals.value.find(a => a.id === id)
    if (!a) return
    await apiCall('PUT', '/data/approvals/' + id, { status: 'REJECTED' })
    a.status = 'REJECTED'
  }
  const addReport = async (data) => {
    const entry = { ...data, id: data.id || Date.now(), submitted: data.submitted !== false }
    await apiCall('POST', '/data/reports', entry)
    reports.value.unshift(entry)
  }
  const addNotice = async (data) => {
    const entry = { ...data, id: data.id || Date.now() }
    const result = await apiCall('POST', '/data/notices', entry)
    notices.value.unshift(result || entry)
    return result || entry
  }

  return {
    jobs, applications, hiredStudents, assessments, disputes, archives, internships, reports, notices,
    addJob, updateJob, toggleJobStatus, deleteJob,
    applyJob, hireStudent, signAgreement, rejectApplication,
    saveAssessment, resolveDispute, addArchive,
    identityVerified,
    addInternship, addReport, addNotice,
    addSchoolStudent, updateSchoolStudent, toggleStudentStatus,
    approveEnterprise, rejectEnterprise,
    markMessageRead, messages, enterpriseList, schoolStudents,
    approvals, approveItem, rejectApprovalItem
  }
})
