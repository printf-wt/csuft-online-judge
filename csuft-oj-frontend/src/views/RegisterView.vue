<template>
  <section class="auth-stage">
    <div class="auth-copy">
      <span class="hero-kicker">{{ t('auth.startTraining') }}</span>
      <h1>{{ t('auth.registerTitle') }}</h1>
      <p>{{ t('auth.registerDescription') }}</p>
    </div>

    <el-card class="auth-panel" shadow="never">
      <template #header>
        <h2>{{ t('common.register') }}</h2>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item :label="t('auth.username')" prop="username">
          <el-input v-model="form.username" size="large" :placeholder="t('auth.usernamePlaceholder')" clearable />
        </el-form-item>
        <el-form-item :label="t('auth.email')" prop="email">
          <el-input v-model="form.email" size="large" placeholder="name@example.com" clearable />
        </el-form-item>
        <el-form-item :label="t('auth.emailCode')" prop="emailCode">
          <el-input v-model="form.emailCode" size="large" maxlength="6" :placeholder="t('auth.emailCodePlaceholder')" clearable>
            <template #append>
              <el-button :loading="sendingCode" :disabled="countdown > 0" @click="sendEmailCode">
                {{ countdown > 0 ? t('auth.resendCodeIn', { seconds: countdown }) : t('auth.sendCode') }}
              </el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item :label="t('auth.password')" prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password />
        </el-form-item>
        <el-form-item :label="t('auth.confirmPassword')" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" size="large" type="password" show-password />
        </el-form-item>
        <el-button class="auth-submit" type="primary" size="large" round :loading="loading" @click="handleRegister">{{ t('auth.createAccount') }}</el-button>
      </el-form>
      <p class="auth-switch">{{ t('auth.hasAccount') }} <router-link to="/login">{{ t('common.login') }}</router-link></p>
    </el-card>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import http from '@/api/http'
import { useI18n } from '@/i18n'

const router = useRouter()
const { t } = useI18n()
const formRef = ref()
const loading = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
let countdownTimer = null
const form = reactive({ username: '', email: '', emailCode: '', password: '', confirmPassword: '' })

function validateConfirmPassword(rule, value, callback) {
  if (!value) return callback(new Error(t('auth.confirmPasswordRequired')))
  if (value !== form.password) return callback(new Error(t('auth.passwordMismatch')))
  return callback()
}

const rules = computed(() => ({
  username: [
    { required: true, message: t('auth.enterUsername'), trigger: 'blur' },
    { min: 3, max: 32, message: t('auth.usernameLength'), trigger: 'blur' },
  ],
  email: [
    { required: true, message: t('auth.enterEmail'), trigger: 'blur' },
    { type: 'email', message: t('auth.validEmail'), trigger: ['blur', 'change'] },
  ],
  emailCode: [
    { required: true, message: t('auth.enterEmailCode'), trigger: 'blur' },
    { pattern: /^\d{6}$/, message: t('auth.emailCodeLength'), trigger: ['blur', 'change'] },
  ],
  password: [
    { required: true, message: t('auth.enterPassword'), trigger: 'blur' },
    { min: 8, max: 128, message: t('auth.passwordLength'), trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: ['blur', 'change'] }],
}))

function startCountdown() {
  countdown.value = 60
  if (countdownTimer) window.clearInterval(countdownTimer)
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      window.clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function sendEmailCode() {
  await formRef.value.validateField('email')
  sendingCode.value = true
  try {
    await http.post('/auth/register-code', { email: form.email })
    ElMessage.success(t('auth.emailCodeSent'))
    startCountdown()
  } catch (error) {
    const msg = error.response?.data?.message || t('auth.emailCodeSendFailed')
    ElMessage.error(msg)
  } finally {
    sendingCode.value = false
  }
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await http.post('/auth/register', {
      username: form.username,
      email: form.email,
      emailCode: form.emailCode,
      password: form.password,
    })
    ElMessage.success(t('auth.registerSuccess'))
    router.push('/login')
  } catch (error) {
    const msg = error.response?.data?.message || t('auth.registerFailed')
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => {
  if (countdownTimer) window.clearInterval(countdownTimer)
})
</script>
