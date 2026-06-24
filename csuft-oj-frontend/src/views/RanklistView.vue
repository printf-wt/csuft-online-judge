<template>
  <section class="surface-card view-card">
    <h1 class="view-title">{{ t('ranklist.title') }}</h1>
    <p class="muted">{{ t('ranklist.description') }}</p>

    <el-table class="content-table" :data="ranklist" v-loading="loading" stripe>
      <el-table-column prop="rank" :label="t('ranklist.rank')" width="80" />
      <el-table-column prop="username" :label="t('auth.username')" />
      <el-table-column prop="nickname" :label="t('auth.nickname')" />
      <el-table-column prop="solvedCount" :label="t('ranklist.solved')" width="120" />
      <el-table-column prop="submitCount" :label="t('ranklist.submissions')" width="120" />
    </el-table>

    <div v-if="total > pageSize" class="table-pagination">
      <el-pagination
        layout="prev, pager, next"
        :total="total"
        :page-size="pageSize"
        :current-page="currentPage"
        @current-change="handlePageChange"
      />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'

import http from '@/api/http'
import { useI18n } from '@/i18n'

const { t } = useI18n()
const ranklist = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(50)
const total = ref(0)

onMounted(() => fetchRanklist())

async function fetchRanklist() {
  loading.value = true
  try {
    const response = await http.get('/users/ranklist', {
      params: { page: currentPage.value, size: pageSize.value },
    })
    ranklist.value = response.data?.records || []
    total.value = response.data?.total || 0
  } catch {
    ranklist.value = []
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  currentPage.value = page
  fetchRanklist()
}
</script>
