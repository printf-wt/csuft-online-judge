<template>
  <section class="surface-card view-card">
    <h1 class="view-title">{{ t('submissions.title') }}</h1>
    <p class="muted">{{ t('submissions.description') }}</p>

    <div class="filter-bar">
      <el-select v-model="filters.status" :placeholder="t('submissions.filterStatus')" clearable @change="fetchSubmissions">
        <el-option v-for="s in statusOptions" :key="s" :label="s" :value="s" />
      </el-select>
      <el-select v-model="filters.language" :placeholder="t('submissions.filterLanguage')" clearable @change="fetchSubmissions">
        <el-option label="C++" value="C++" />
        <el-option label="Java" value="JAVA" />
        <el-option label="Python" value="PYTHON" />
        <el-option label="Go" value="GO" />
      </el-select>
    </div>

    <el-table class="content-table" :data="submissions" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="problemId" :label="t('submissions.problem')" width="100">
        <template #default="{ row }">
          <router-link :to="`/problems/${row.problemId}/solve`">{{ row.problemId }}</router-link>
        </template>
      </el-table-column>
      <el-table-column prop="language" :label="t('submissions.language')" width="100" />
      <el-table-column prop="status" :label="t('submissions.status')" width="180">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" effect="dark" round>{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="timeUsedMs" :label="t('common.time')" width="100">
        <template #default="{ row }">{{ row.timeUsedMs ?? '-' }} ms</template>
      </el-table-column>
      <el-table-column prop="memoryUsedKb" :label="t('common.memory')" width="120">
        <template #default="{ row }">{{ row.memoryUsedKb ?? '-' }} KB</template>
      </el-table-column>
      <el-table-column prop="createdAt" :label="t('submissions.time')">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
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
import { onMounted, reactive, ref } from 'vue'

import http from '@/api/http'
import { formatDateTime, useI18n } from '@/i18n'

const { t } = useI18n()
const submissions = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const filters = reactive({ status: '', language: '' })

const statusOptions = ['ACCEPTED', 'WRONG_ANSWER', 'TIME_LIMIT_EXCEEDED', 'RUNTIME_ERROR', 'COMPILE_ERROR', 'PENDING', 'JUDGING']

onMounted(() => fetchSubmissions())

async function fetchSubmissions() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (filters.status) params.status = filters.status
    if (filters.language) params.language = filters.language
    const response = await http.get('/submissions', { params })
    submissions.value = response.data?.records || []
    total.value = response.data?.total || 0
  } catch {
    submissions.value = []
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  currentPage.value = page
  fetchSubmissions()
}

function statusType(status) {
  if (status === 'ACCEPTED') return 'success'
  if (status === 'WRONG_ANSWER') return 'danger'
  if (status === 'PENDING' || status === 'JUDGING') return 'warning'
  return 'info'
}
</script>
