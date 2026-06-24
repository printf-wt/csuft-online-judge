<template>
  <section class="profile-page" v-loading="loading">
    <div class="surface-card profile-hero">
      <div class="profile-avatar">{{ avatarText }}</div>
      <div class="profile-heading">
        <span class="hero-kicker">{{ t('profile.center') }}</span>
        <h1>{{ displayName }}</h1>
        <p class="muted">{{ t('profile.description') }}</p>
      </div>
      <div class="profile-actions">
        <el-button round @click="passwordVisible = true">{{ t('profile.changePassword') }}</el-button>
        <el-button type="primary" plain round @click="openEditor">{{ t('profile.edit') }}</el-button>
      </div>
    </div>

    <div class="profile-grid">
      <el-card class="profile-card" shadow="never">
        <template #header><strong>{{ t('profile.account') }}</strong></template>
        <div class="info-list">
          <div><span>{{ t('profile.studentNo') }}</span><strong>{{ profile.username || '-' }}</strong></div>
          <div><span>{{ t('profile.email') }}</span><strong>{{ profile.email || t('common.notProvided') }}</strong></div>
          <div><span>{{ t('profile.role') }}</span><el-tag effect="dark">{{ profile.role || 'STUDENT' }}</el-tag></div>
          <div><span>{{ t('profile.joinedAt') }}</span><strong>{{ formatDateTime(profile.createdAt) }}</strong></div>
        </div>
      </el-card>

      <div class="metric-grid">
        <div class="stat-card profile-metric"><span class="muted">{{ t('profile.globalAc') }}</span><strong>{{ profile.globalAcCount ?? 0 }}</strong></div>
        <div class="stat-card profile-metric"><span class="muted">{{ t('profile.totalSubmits') }}</span><strong>{{ profile.submitCount ?? 0 }}</strong></div>
      </div>
    </div>

    <el-card class="heatmap-card" shadow="never">
      <template #header>
        <div class="heatmap-head">
          <div><strong>{{ t('profile.heatmap') }}</strong><p class="muted">{{ t('profile.heatmapDescription') }}</p></div>
          <div class="heatmap-legend"><span>{{ t('common.less') }}</span><i v-for="level in 5" :key="level" :style="{ backgroundColor: heatColor(level - 1) }" /><span>{{ t('common.more') }}</span></div>
        </div>
      </template>
      <div class="heatmap-scroll">
        <svg class="heatmap-svg" :width="svgWidth" :height="svgHeight" role="img" :aria-label="t('profile.heatmapAria')">
          <g :transform="`translate(${leftPad}, ${topPad})`">
            <rect v-for="day in heatmapDays" :key="day.date" class="heat-cell" :x="day.week * cellGap" :y="day.weekday * cellGap" :width="cellSize" :height="cellSize" :rx="3" :fill="heatColor(day.level)">
              <title>{{ t('profile.submissionsCount', { date: day.date, count: day.count }) }}</title>
            </rect>
          </g>
        </svg>
      </div>
    </el-card>

    <el-dialog v-model="editorVisible" :title="t('profile.edit')" width="min(480px, 92vw)">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('profile.nickname')" prop="nickname"><el-input v-model="form.nickname" maxlength="64" show-word-limit /></el-form-item>
        <el-form-item :label="t('profile.email')" prop="email"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ t('profile.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveProfile">{{ t('profile.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordVisible" :title="t('profile.changePassword')" width="min(480px, 92vw)">
      <el-alert :title="t('profile.passwordSecurityHint')" type="info" show-icon :closable="false" />
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules" label-position="top" class="password-form">
        <el-form-item :label="t('profile.currentPassword')" prop="currentPassword">
          <el-input v-model="passwordForm.currentPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item :label="t('profile.newPassword')" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item :label="t('auth.confirmPassword')" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password autocomplete="new-password" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordVisible = false">{{ t('profile.cancel') }}</el-button>
        <el-button type="primary" :loading="passwordSaving" @click="changePassword">{{ t('profile.confirmChange') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { formatDateTime, useI18n } from '@/i18n'

const authStore = useAuthStore()
const router = useRouter()
const { t } = useI18n()
const profile = reactive({ submissionActivity: [] })
const loading = ref(false)
const saving = ref(false)
const editorVisible = ref(false)
const passwordVisible = ref(false)
const passwordSaving = ref(false)
const passwordFormRef = ref()
const formRef = ref()
const form = reactive({ nickname: '', email: '' })
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
const rules = computed(() => ({
  nickname: [{ required: true, message: t('profile.nicknameRequired'), trigger: 'blur' }],
  email: [
    { required: true, message: t('auth.enterEmail'), trigger: 'blur' },
    { type: 'email', message: t('auth.validEmail'), trigger: ['blur', 'change'] },
  ],
}))
const passwordRules = computed(() => ({
  currentPassword: [{ required: true, message: t('profile.currentPasswordRequired'), trigger: 'blur' }],
  newPassword: [
    { required: true, message: t('profile.newPasswordRequired'), trigger: 'blur' },
    { min: 8, max: 128, message: t('auth.passwordLength'), trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: t('auth.confirmPasswordRequired'), trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== passwordForm.newPassword) callback(new Error(t('auth.passwordMismatch')))
        else callback()
      },
      trigger: ['blur', 'change'],
    },
  ],
}))

const displayName = computed(() => profile.nickname || profile.username || t('profile.defaultCoder'))
const avatarText = computed(() => displayName.value.slice(0, 1).toUpperCase())
const cellSize = 12
const cellGap = 16
const leftPad = 10
const topPad = 10
const weeks = 53
const svgWidth = leftPad * 2 + weeks * cellGap
const svgHeight = topPad * 2 + 7 * cellGap

const heatmapDays = computed(() => {
  const counts = new Map((profile.submissionActivity || []).map((item) => [item.date, item.count]))
  const today = new Date()
  return Array.from({ length: 365 }, (_, index) => {
    const date = new Date(today)
    date.setDate(today.getDate() - (364 - index))
    const dateKey = localDateKey(date)
    const count = counts.get(dateKey) || 0
    return { date: dateKey, weekday: date.getDay(), week: Math.floor(index / 7), count, level: levelFromCount(count) }
  })
})

onMounted(fetchProfile)

async function fetchProfile() {
  loading.value = true
  try {
    const response = await http.get('/users/me')
    Object.assign(profile, response.data)
    syncAuthUser(response.data)
  } finally {
    loading.value = false
  }
}

function openEditor() {
  form.nickname = profile.nickname || ''
  form.email = profile.email || ''
  editorVisible.value = true
}

async function saveProfile() {
  await formRef.value.validate()
  saving.value = true
  try {
    const response = await http.put('/users/me', form)
    Object.assign(profile, response.data)
    syncAuthUser(response.data)
    editorVisible.value = false
    ElMessage.success(t('profile.saved'))
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  await passwordFormRef.value.validate()
  passwordSaving.value = true
  try {
    await http.put('/users/me/password', {
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })
    try {
      await http.post('/auth/logout')
    } catch {
      // The password change already revoked every refresh token on the server.
    } finally {
      authStore.logout()
    }
    ElMessage.success(t('profile.passwordChanged'))
    passwordVisible.value = false
    Object.assign(passwordForm, { currentPassword: '', newPassword: '', confirmPassword: '' })
    await router.replace('/login')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || t('profile.passwordChangeFailed'))
  } finally {
    passwordSaving.value = false
  }
}

function syncAuthUser(value) {
  authStore.setAuth({ token: authStore.token, user: { ...authStore.user, ...value } })
}

function localDateKey(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function levelFromCount(count) {
  if (count <= 0) return 0
  if (count <= 1) return 1
  if (count <= 3) return 2
  if (count <= 6) return 3
  return 4
}

function heatColor(level) {
  return ['rgba(101, 113, 137, 0.16)', '#b7eadf', '#62cfbc', '#1f9e92', '#096c72'][level] || 'rgba(101, 113, 137, 0.16)'
}
</script>
