<template>
  <el-config-provider :namespace="'el'" :locale="elementLocale">
    <div class="app-shell" :class="{ 'is-dark': isDark }">
      <header class="topbar">
        <router-link class="brand" to="/">
          <img class="brand-mark" :src="csuftLogo" alt="CSUFT" />
          <span>
            <strong>CSUFT OJ</strong>
            <small>{{ t('layout.slogan') }}</small>
          </span>
        </router-link>

        <nav class="nav-links">
          <router-link v-for="item in navItems" :key="item.path" :to="item.path">{{ item.label }}</router-link>
        </nav>

        <div class="top-actions">
          <el-dropdown trigger="click" @command="setLocale">
            <el-button class="language-toggle" round>
              <el-icon><Connection /></el-icon>
              {{ isChinese ? '中文' : 'EN' }}
              <el-icon><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="zh-CN" :disabled="isChinese">{{ t('layout.chinese') }}</el-dropdown-item>
                <el-dropdown-item command="en" :disabled="!isChinese">{{ t('layout.english') }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <el-button circle class="theme-toggle" @click="toggleTheme">
            <el-icon><Moon v-if="!isDark" /><Sunny v-else /></el-icon>
          </el-button>

          <template v-if="authStore.isLoggedIn">
            <el-dropdown trigger="click" @command="handleUserCommand">
              <button class="avatar-button">
                <span class="avatar">{{ avatarText }}</span>
                <span class="user-name">{{ authStore.user?.nickname || authStore.user?.username }}</span>
                <el-icon><ArrowDown /></el-icon>
              </button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">{{ t('layout.profile') }}</el-dropdown-item>
                  <el-dropdown-item command="submissions">{{ t('layout.mySubmissions') }}</el-dropdown-item>
                  <el-dropdown-item v-if="authStore.isTeacherOrAdmin" command="manage-problems">{{ t('layout.problemManagement') }}</el-dropdown-item>
                  <el-dropdown-item v-if="authStore.isTeacherOrAdmin" command="manage-contests">{{ t('layout.contestManagement') }}</el-dropdown-item>
                  <el-dropdown-item v-if="authStore.isAdmin" command="admin">{{ t('layout.admin') }}</el-dropdown-item>
                  <el-dropdown-item v-if="authStore.isAdmin" command="admin-users">{{ t('layout.userManagement') }}</el-dropdown-item>
                  <el-dropdown-item divided command="logout">{{ t('common.logout') }}</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <router-link class="ghost-link" to="/login">{{ t('common.login') }}</router-link>
            <router-link class="solid-link" to="/register">{{ t('common.register') }}</router-link>
          </template>
        </div>
      </header>

      <main class="page-main">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>

      <footer class="footer">
        <span>{{ t('layout.footer') }}</span>
        <span>Java 17 + Spring Boot 3 + Vue 3</span>
      </footer>
    </div>
  </el-config-provider>
</template>

<script setup>
import { ArrowDown, Connection, Moon, Sunny } from '@element-plus/icons-vue'
import en from 'element-plus/es/locale/lang/en'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import { computed, ref, watch, watchEffect } from 'vue'
import { useRouter } from 'vue-router'

import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { useI18n } from '@/i18n'
import csuftLogo from '@/assets/csuft-logo.webp'

const router = useRouter()
const authStore = useAuthStore()
const { isChinese, locale, setLocale, t } = useI18n()
const isDark = ref(localStorage.getItem('csuft_oj_theme') === 'dark')

const elementLocale = computed(() => (isChinese.value ? zhCn : en))
const navItems = computed(() => [
  { label: t('common.home'), path: '/' },
  { label: t('common.problems'), path: '/problems' },
  { label: t('common.ranklist'), path: '/ranklist' },
  { label: t('common.contests'), path: '/contests' },
  { label: t('common.submissions'), path: '/submissions' },
])

const avatarText = computed(() => {
  const name = authStore.user?.nickname || authStore.user?.username || 'U'
  return name.slice(0, 1).toUpperCase()
})

watchEffect(() => {
  document.documentElement.classList.toggle('dark', isDark.value)
  localStorage.setItem('csuft_oj_theme', isDark.value ? 'dark' : 'light')
})

watch(locale, () => {
  document.title = `${t(router.currentRoute.value.meta.titleKey || 'route.home')} - CSUFT OJ`
})

function toggleTheme() {
  isDark.value = !isDark.value
}

async function handleUserCommand(command) {
  if (command === 'logout') {
    try {
      await http.post('/auth/logout')
    } finally {
      authStore.logout()
      router.push('/login')
    }
    return
  }
  if (command === 'admin') {
    router.push('/admin/monitor')
    return
  }
  if (command === 'admin-users') {
    router.push('/admin/users')
    return
  }
  if (command === 'manage-problems') {
    router.push('/manage/problems')
    return
  }
  if (command === 'manage-contests') {
    router.push('/manage/contests')
    return
  }
  router.push(command === 'profile' ? '/profile' : '/submissions')
}
</script>
