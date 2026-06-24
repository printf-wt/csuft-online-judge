<template>
  <section class="contest-page">
    <div class="surface-card contest-hero">
      <div>
        <span class="hero-kicker">{{ t('contest.arena') }}</span>
        <h1>{{ t('contest.title') }}</h1>
        <p class="muted">{{ t('contest.description') }}</p>
      </div>
      <el-button type="primary" size="large" round @click="fetchContests">{{ t('common.refresh') }}</el-button>
    </div>

    <div class="contest-grid" v-loading="loading">
      <router-link v-for="contest in contests" :key="contest.id" class="contest-card surface-card" :to="`/contests/${contest.id}`">
        <div class="contest-card-head">
          <el-tag :type="statusMeta(contest).type" effect="dark" round>{{ statusMeta(contest).label }}</el-tag>
          <span class="contest-rule">{{ contest.ruleType }}</span>
        </div>
        <h2>{{ contest.title }}</h2>
        <p class="muted">{{ contest.description || t('contest.fallbackDescription') }}</p>
        <div class="contest-time">
          <span>{{ formatDateTime(contest.startTime) }}</span>
          <i />
          <span>{{ formatDateTime(contest.endTime) }}</span>
        </div>
        <div v-if="statusMeta(contest).key === 'running'" class="countdown-pill">
          {{ t('contest.remaining', { time: countdown(contest.endTime) }) }}
        </div>
      </router-link>
      <el-empty v-if="!loading && contests.length === 0" :description="t('contest.loadFailed')" />
    </div>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { onBeforeUnmount, onMounted, ref } from 'vue'

import http from '@/api/http'
import { formatDateTime, useI18n } from '@/i18n'

const { t } = useI18n()

const loading = ref(false)
const contests = ref([])
const now = ref(Date.now())
let timer = null

onMounted(() => {
  fetchContests()
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
})

async function fetchContests() {
  loading.value = true
  try {
    const response = await http.get('/contests', { params: { page: 1, size: 50 } })
    contests.value = response.data?.records || response.data || []
  } catch {
    contests.value = []
    ElMessage.error(t('contest.loadFailed'))
  } finally {
    loading.value = false
  }
}

function statusMeta(contest) {
  const start = new Date(contest.startTime).getTime()
  const end = new Date(contest.endTime).getTime()
  if (now.value < start) return { key: 'upcoming', label: t('common.upcoming'), type: 'primary' }
  if (now.value <= end) return { key: 'running', label: t('common.running'), type: 'success' }
  return { key: 'ended', label: t('common.ended'), type: 'info' }
}

function countdown(endTime) {
  const ms = Math.max(0, new Date(endTime).getTime() - now.value)
  const totalSeconds = Math.floor(ms / 1000)
  const hours = String(Math.floor(totalSeconds / 3600)).padStart(2, '0')
  const minutes = String(Math.floor((totalSeconds % 3600) / 60)).padStart(2, '0')
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${hours}:${minutes}:${seconds}`
}

</script>
