<template>
  <section class="surface-card hero">
    <div>
      <div class="hero-kicker">CSUFT OJ</div>
      <h1>{{ t('home.title') }}</h1>
      <p>{{ t('home.description') }}</p>
      <div class="stat-grid">
        <div class="stat-card">
          <el-icon class="stat-card-icon"><Clock /></el-icon>
          <strong>24/7</strong>
          <span class="muted">{{ t('home.asyncJudge') }}</span>
        </div>
        <div class="stat-card">
          <el-icon class="stat-card-icon"><Trophy /></el-icon>
          <strong>ACM</strong>
          <span class="muted">{{ t('home.contestMode') }}</span>
        </div>
        <div class="stat-card">
          <el-icon class="stat-card-icon"><DataAnalysis /></el-icon>
          <strong>IOI</strong>
          <span class="muted">{{ t('home.scoreMode') }}</span>
        </div>
      </div>
    </div>
    <div class="hero-panel">
      <div class="mini-card notice-card">
        <div class="notice-heading">
          <h3>{{ t('home.pinnedNotice') }}</h3>
          <el-icon v-if="noticeLoading" class="is-loading"><Loading /></el-icon>
        </div>
        <template v-if="notices.length">
          <article v-for="notice in notices" :key="notice.id" class="notice-item">
            <strong>{{ notice.title }}</strong>
            <p class="muted">{{ notice.content }}</p>
            <time>{{ formatDateTime(notice.createdAt) }}</time>
          </article>
        </template>
        <p v-else class="muted">{{ t('home.noNotices') }}</p>
      </div>
      <div class="mini-card"><h3>{{ t('home.today') }}</h3><p class="muted">{{ t('home.todayHint') }}</p></div>
    </div>
  </section>
</template>

<script setup>
import { Clock, DataAnalysis, Loading, Trophy } from '@element-plus/icons-vue'
import { onMounted, ref } from 'vue'

import http from '@/api/http'
import { formatDateTime, useI18n } from '@/i18n'

const { t } = useI18n()
const notices = ref([])
const noticeLoading = ref(false)

onMounted(fetchNotices)

async function fetchNotices() {
  noticeLoading.value = true
  try {
    const response = await http.get('/notices', { params: { page: 1, size: 3 } })
    notices.value = response.data?.records || []
  } catch {
    notices.value = []
  } finally {
    noticeLoading.value = false
  }
}
</script>
