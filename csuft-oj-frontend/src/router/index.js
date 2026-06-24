import { createRouter, createWebHistory } from 'vue-router'

import { restoreSession } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { t } from '@/i18n'

const AdminDashboardView = () => import('@/views/AdminDashboardView.vue')
const AdminUserManagementView = () => import('@/views/AdminUserManagementView.vue')
const ContestDetailView = () => import('@/views/ContestDetailView.vue')
const ContestListView = () => import('@/views/ContestListView.vue')
const HomeView = () => import('@/views/HomeView.vue')
const LoginView = () => import('@/views/LoginView.vue')
const ProblemDetailView = () => import('@/views/ProblemDetailView.vue')
const ProblemListView = () => import('@/views/ProblemListView.vue')
const ProblemManagementView = () => import('@/views/ProblemManagementView.vue')
const ProfileView = () => import('@/views/ProfileView.vue')
const RanklistView = () => import('@/views/RanklistView.vue')
const RegisterView = () => import('@/views/RegisterView.vue')
const SolveView = () => import('@/views/SolveView.vue')
const SubmissionHistoryView = () => import('@/views/SubmissionHistoryView.vue')
const ContestManagementView = () => import('@/views/ContestManagementView.vue')

const routes = [
  { path: '/', name: 'home', component: HomeView, meta: { titleKey: 'route.home' } },
  { path: '/problems', name: 'problems', component: ProblemListView, meta: { titleKey: 'route.problems' } },
  { path: '/ranklist', name: 'ranklist', component: RanklistView, meta: { titleKey: 'route.ranklist' } },
  { path: '/login', name: 'login', component: LoginView, meta: { titleKey: 'route.login', guestOnly: true } },
  { path: '/register', name: 'register', component: RegisterView, meta: { titleKey: 'route.register', guestOnly: true } },
  { path: '/profile', name: 'profile', component: ProfileView, meta: { titleKey: 'route.profile', requiresAuth: true } },
  { path: '/admin/monitor', name: 'admin-monitor', component: AdminDashboardView, meta: { titleKey: 'route.admin', requiresAuth: true, requiresAdmin: true } },
  { path: '/admin/users', name: 'admin-users', component: AdminUserManagementView, meta: { titleKey: 'route.adminUsers', requiresAuth: true, requiresAdmin: true } },
  { path: '/manage/problems', name: 'manage-problems', component: ProblemManagementView, meta: { titleKey: 'route.manageProblems', requiresAuth: true, requiresTeacher: true } },
  { path: '/manage/contests', name: 'manage-contests', component: ContestManagementView, meta: { titleKey: 'route.manageContests', requiresAuth: true, requiresTeacher: true } },
  { path: '/problems/:id', name: 'problem-detail', component: ProblemDetailView, meta: { titleKey: 'route.problemDetail', requiresAuth: true } },
  { path: '/problems/:id/solve', name: 'solve', component: SolveView, meta: { titleKey: 'route.solve', requiresAuth: true } },
  { path: '/submissions', name: 'submissions', component: SubmissionHistoryView, meta: { titleKey: 'route.submissions', requiresAuth: true } },
  { path: '/contests', name: 'contests', component: ContestListView, meta: { titleKey: 'route.contests', requiresAuth: true } },
  { path: '/contests/:id', name: 'contest-detail', component: ContestDetailView, meta: { titleKey: 'route.contestDetail', requiresAuth: true } },
  { path: '/contests/:contestId/problems/:id/solve', name: 'contest-solve', component: SolveView, meta: { titleKey: 'route.contestPlayground', requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

let sessionRestoreAttempted = false

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  if (!authStore.token
      && !sessionRestoreAttempted
      && (to.meta.requiresAuth || to.meta.guestOnly)) {
    sessionRestoreAttempted = true
    await restoreSession()
  }
  if (to.meta.requiresAuth && !authStore.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    return { name: 'home' }
  }
  if (to.meta.requiresTeacher && !authStore.isTeacherOrAdmin) {
    return { name: 'home' }
  }
  if (to.meta.guestOnly && authStore.token) {
    return { name: 'home' }
  }
  document.title = `${t(to.meta.titleKey || 'route.home')} - CSUFT OJ`
  return true
})

export default router
