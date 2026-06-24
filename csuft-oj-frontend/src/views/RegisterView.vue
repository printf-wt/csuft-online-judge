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
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import http from '@/api/http'
import { useI18n } from '@/i18n'

const router = useRouter()
const { t } = useI18n()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', email: '', password: '', confirmPassword: '' })

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
  password: [
    { required: true, message: t('auth.enterPassword'), trigger: 'blur' },
    { min: 8, max: 128, message: t('auth.passwordLength'), trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: ['blur', 'change'] }],
}))

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await http.post('/auth/register', { username: form.username, email: form.email, password: form.password })
    ElMessage.success(t('auth.registerSuccess'))
    router.push('/login')
  } catch (error) {
    const msg = error.response?.data?.message || t('auth.registerFailed')
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>
