<template>
  <section class="auth-stage">
    <div class="auth-copy">
      <span class="hero-kicker">{{ t('auth.welcomeBack') }}</span>
      <h1>{{ t('auth.loginTitle') }}</h1>
      <p>{{ t('auth.loginDescription') }}</p>
    </div>

    <el-card class="auth-panel" shadow="never">
      <template #header>
        <h2>{{ t('common.login') }}</h2>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
        <el-form-item :label="t('auth.username')" prop="username">
          <el-input v-model="form.username" size="large" :placeholder="t('auth.usernamePlaceholder')" clearable />
        </el-form-item>
        <el-form-item :label="t('auth.password')" prop="password">
          <el-input v-model="form.password" size="large" type="password" show-password />
        </el-form-item>
        <el-button class="auth-submit" type="primary" size="large" round :loading="loading" @click="handleLogin">{{ t('common.login') }}</el-button>
      </el-form>
      <p class="auth-switch">{{ t('auth.noAccount') }} <router-link to="/register">{{ t('auth.createOne') }}</router-link></p>
    </el-card>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from '@/i18n'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { t } = useI18n()
const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const rules = computed(() => ({
  username: [
    { required: true, message: t('auth.enterUsername'), trigger: 'blur' },
    { min: 3, max: 32, message: t('auth.usernameLength'), trigger: 'blur' },
  ],
  password: [
    { required: true, message: t('auth.enterPassword'), trigger: 'blur' },
    { min: 8, max: 128, message: t('auth.passwordLength'), trigger: 'blur' },
  ],
}))

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const response = await http.post('/auth/login', form)
    authStore.setAuth(response.data)
    ElMessage.success(t('auth.loginSuccess'))
    router.push(route.query.redirect || '/')
  } catch (error) {
    const msg = error.response?.data?.message || t('auth.loginFailed')
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>
