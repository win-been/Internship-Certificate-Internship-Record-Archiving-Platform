import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/userStore'

const routes = [
  { path: '/login', name: 'Login', component: () => import('../views/Login.vue'), meta: { requiresAuth: false } },
  { path: '/register', name: 'Register', component: () => import('../views/Register.vue'), meta: { requiresAuth: false } },
  { path: '/', redirect: '/login' },

  // Student
  { path: '/student/dashboard', name: 'StudentDashboard', component: () => import('../views/student/Dashboard.vue'), meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/student/identity', name: 'StudentIdentity', component: () => import('../views/student/Identity.vue'), meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/student/jobs', name: 'StudentJobs', component: () => import('../views/student/Jobs.vue'), meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/student/internship', name: 'StudentInternship', component: () => import('../views/student/Internship.vue'), meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/student/reports', name: 'StudentReports', component: () => import('../views/student/Reports.vue'), meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/student/assessment', name: 'StudentAssessment', component: () => import('../views/student/Assessment.vue'), meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/student/graduate', name: 'StudentGraduate', component: () => import('../views/student/Graduate.vue'), meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/student/evidence', name: 'StudentEvidence', component: () => import('../views/student/Evidence.vue'), meta: { requiresAuth: true, role: 'STUDENT' } },
  { path: '/student/ai', name: 'StudentAI', component: () => import('../views/student/AI.vue'), meta: { requiresAuth: true, role: 'STUDENT' } },

  // School admin
  { path: '/school/dashboard', name: 'SchoolDashboard', component: () => import('../views/school/Dashboard.vue'), meta: { requiresAuth: true, role: 'SCHOOL_ADMIN' } },
  { path: '/school/students', name: 'SchoolStudents', component: () => import('../views/school/Students.vue'), meta: { requiresAuth: true, role: 'SCHOOL_ADMIN' } },
  { path: '/school/enterprises', name: 'SchoolEnterprises', component: () => import('../views/school/Enterprises.vue'), meta: { requiresAuth: true, role: 'SCHOOL_ADMIN' } },
  { path: '/school/approvals', name: 'SchoolApprovals', component: () => import('../views/platform/Approvals.vue'), meta: { requiresAuth: true, role: 'SCHOOL_ADMIN' } },
  { path: '/school/inspection', name: 'SchoolInspection', component: () => import('../views/school/Inspection.vue'), meta: { requiresAuth: true, role: 'SCHOOL_ADMIN' } },
  { path: '/school/verify', name: 'SchoolVerify', component: () => import('../views/school/Verify.vue'), meta: { requiresAuth: true, role: 'SCHOOL_ADMIN' } },
  { path: '/school/notices', name: 'SchoolNotices', component: () => import('../views/school/Notices.vue'), meta: { requiresAuth: true, role: 'SCHOOL_ADMIN' } },
  { path: '/school/ai', name: 'SchoolAI', component: () => import('../views/school/AI.vue'), meta: { requiresAuth: true, role: 'SCHOOL_ADMIN' } },

  // Enterprise
  { path: '/enterprise/dashboard', name: 'EnterpriseDashboard', component: () => import('../views/enterprise/Dashboard.vue'), meta: { requiresAuth: true, role: ['ENTERPRISE_HR', 'ENTERPRISE_MENTOR'] } },
  { path: '/enterprise/register', name: 'EnterpriseRegister', component: () => import('../views/enterprise/Register.vue'), meta: { requiresAuth: true, role: ['ENTERPRISE_HR', 'ENTERPRISE_MENTOR'] } },
  { path: '/enterprise/jobs', name: 'EnterpriseJobs', component: () => import('../views/enterprise/Jobs.vue'), meta: { requiresAuth: true, role: ['ENTERPRISE_HR', 'ENTERPRISE_MENTOR'] } },
  { path: '/enterprise/recruit', name: 'EnterpriseRecruit', component: () => import('../views/enterprise/Recruit.vue'), meta: { requiresAuth: true, role: ['ENTERPRISE_HR', 'ENTERPRISE_MENTOR'] } },
  { path: '/enterprise/assessment', name: 'EnterpriseAssessment', component: () => import('../views/enterprise/Assessment.vue'), meta: { requiresAuth: true, role: ['ENTERPRISE_HR', 'ENTERPRISE_MENTOR'] } },
  { path: '/enterprise/disputes', name: 'EnterpriseDisputes', component: () => import('../views/enterprise/Disputes.vue'), meta: { requiresAuth: true, role: ['ENTERPRISE_HR', 'ENTERPRISE_MENTOR'] } },
  { path: '/enterprise/archive', name: 'EnterpriseArchive', component: () => import('../views/enterprise/Archive.vue'), meta: { requiresAuth: true, role: ['ENTERPRISE_HR', 'ENTERPRISE_MENTOR'] } },
  { path: '/enterprise/ai', name: 'EnterpriseAI', component: () => import('../views/enterprise/AI.vue'), meta: { requiresAuth: true, role: ['ENTERPRISE_HR', 'ENTERPRISE_MENTOR'] } },

  // Platform admin
  { path: '/platform/dashboard', name: 'PlatformDashboard', component: () => import('../views/platform/Dashboard.vue'), meta: { requiresAuth: true, role: 'PLATFORM_ADMIN' } },
  { path: '/platform/permissions', name: 'PlatformPermissions', component: () => import('../views/platform/Permissions.vue'), meta: { requiresAuth: true, role: 'PLATFORM_ADMIN' } },
  { path: '/platform/approvals', name: 'PlatformApprovals', component: () => import('../views/platform/Approvals.vue'), meta: { requiresAuth: true, role: 'PLATFORM_ADMIN' } },
  { path: '/platform/chain', name: 'PlatformChain', component: () => import('../views/platform/Chain.vue'), meta: { requiresAuth: true, role: 'PLATFORM_ADMIN' } },
  { path: '/platform/config', name: 'PlatformConfig', component: () => import('../views/platform/Config.vue'), meta: { requiresAuth: true, role: 'PLATFORM_ADMIN' } },
  { path: '/platform/verify', name: 'PlatformVerify', component: () => import('../views/platform/Verify.vue'), meta: { requiresAuth: true, role: 'PLATFORM_ADMIN' } },
  { path: '/platform/ai', name: 'PlatformAI', component: () => import('../views/platform/AI.vue'), meta: { requiresAuth: true, role: 'PLATFORM_ADMIN' } },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const isAuth = userStore.isLoggedIn

  if (to.meta.requiresAuth && !isAuth) {
    return next('/login')
  }

  if (isAuth && to.meta.role) {
    const userRole = userStore.userInfo?.role
    const required = to.meta.role
    const ok = Array.isArray(required) ? required.includes(userRole) : required === userRole
    if (!ok) {
      if (userRole === 'STUDENT') return next('/student/dashboard')
      if (userRole === 'SCHOOL_ADMIN') return next('/school/dashboard')
      if (userRole === 'ENTERPRISE_HR' || userRole === 'ENTERPRISE_MENTOR') return next('/enterprise/dashboard')
      if (userRole === 'PLATFORM_ADMIN') return next('/platform/dashboard')
      userStore.logout()
      return next('/login')
    }
  }

  next()
})

export default router
