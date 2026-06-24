<template>
  <section class="problem-page">
    <div class="surface-card problem-hero">
      <div>
        <span class="hero-kicker">{{ t('problem.archive') }}</span>
        <h1>{{ t('problem.title') }}</h1>
        <p class="muted">{{ t('problem.description') }}</p>
      </div>
      <div class="problem-hero-orb">
        <strong>{{ filteredProblems.length }}</strong>
        <span>{{ t('problem.matched') }}</span>
      </div>
    </div>

    <el-card class="problem-board" shadow="never">
      <div class="problem-toolbar">
        <el-input
          v-model="filters.keyword"
          class="problem-search"
          clearable
          size="large"
          :placeholder="t('problem.search')"
        />
        <el-select v-model="filters.difficulty" clearable size="large" :placeholder="t('problem.difficulty')">
          <el-option :label="t('common.easy')" value="EASY" />
          <el-option :label="t('common.medium')" value="MEDIUM" />
          <el-option :label="t('common.hard')" value="HARD" />
        </el-select>
        <el-select
          v-model="filters.tags"
          multiple
          collapse-tags
          collapse-tags-tooltip
          clearable
          size="large"
          :placeholder="t('problem.tags')"
        >
          <el-option v-for="tag in availableTags" :key="tag" :label="tag" :value="tag" />
        </el-select>
      </div>

      <el-table :data="filteredProblems" class="problem-table" v-loading="loading" row-key="id">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column :label="t('problem.titleColumn')" min-width="240">
          <template #default="{ row }">
            <router-link class="problem-title" :to="`/problems/${row.id}`">{{ row.title }}</router-link>
            <p class="muted table-subtitle">{{ row.timeLimitMs || 1000 }} ms · {{ row.memoryLimitKb || 262144 }} KB</p>
          </template>
        </el-table-column>
        <el-table-column :label="t('problem.difficulty')" width="130">
          <template #default="{ row }">
            <el-tag :type="difficultyType(row.difficulty)" effect="dark" round>
              {{ difficultyLabel(row.difficulty) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="130">
          <template #default="{ row }">
            <el-tag :type="submissionStatusType(row.submissionStatus)" effect="plain" round>
              {{ submissionStatusLabel(row.submissionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('problem.passRate')" width="190">
          <template #default="{ row }">
            <div class="rate-cell">
              <el-progress :percentage="passRate(row)" :stroke-width="9" :show-text="false" />
              <span>{{ passRate(row) }}%</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('problem.tags')" min-width="220">
          <template #default="{ row }">
            <div class="tag-cloud">
              <el-tag v-for="tag in row.tags" :key="tag" size="small" round>{{ tag }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="130" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="$router.push(`/problems/${row.id}/solve`)">
              {{ t('problem.solve') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import http from '@/api/http'
import { useI18n } from '@/i18n'

const { t, locale } = useI18n()

const loading = ref(false)
const problems = ref([])
const filters = reactive({
  keyword: '',
  difficulty: '',
  tags: [],
})

const availableTags = computed(() => {
  const tags = new Set()
  problems.value.forEach((problem) => problem.tags?.forEach((tag) => tags.add(tag)))
  return [...tags].sort()
})

const filteredProblems = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return problems.value.filter((problem) => {
    const matchesKeyword = !keyword || String(problem.id).includes(keyword) || problem.title.toLowerCase().includes(keyword)
    const matchesDifficulty = !filters.difficulty || problem.difficulty === filters.difficulty
    const matchesTags = filters.tags.length === 0 || filters.tags.every((tag) => problem.tags?.includes(tag))
    return matchesKeyword && matchesDifficulty && matchesTags
  })
})

onMounted(fetchProblems)

async function fetchProblems() {
  loading.value = true
  try {
    const response = await http.get('/problems', { params: { page: 1, size: 100 } })
    const records = response.data?.records || response.data || []
    problems.value = records.map(normalizeProblem)
  } catch (error) {
    problems.value = []
    ElMessage.error(error.response?.data?.message || t('problem.loadFailed'))
  } finally {
    loading.value = false
  }
}

function normalizeProblem(problem) {
  return {
    ...problem,
    difficulty: (problem.difficulty || 'EASY').toUpperCase(),
    acceptedCount: problem.acceptedCount || 0,
    submitCount: problem.submitCount || 0,
    submissionStatus: problem.submissionStatus || 'NOT_SUBMITTED',
    tags: problem.tags?.length ? problem.tags : guessTags(problem),
  }
}

function guessTags(problem) {
  const title = problem.title?.toLowerCase() || ''
  if (title.includes('path') || title.includes('graph')) return ['Graph']
  if (title.includes('string')) return ['String']
  if (title.includes('dp')) return ['DP']
  return ['Implementation']
}

function passRate(problem) {
  if (!problem.submitCount) return 0
  return Math.round((problem.acceptedCount / problem.submitCount) * 100)
}

function difficultyType(difficulty) {
  return {
    EASY: 'success',
    MEDIUM: 'warning',
    HARD: 'danger',
  }[difficulty] || 'info'
}

function difficultyLabel(difficulty) {
  return {
    EASY: t('common.easy'),
    MEDIUM: t('common.medium'),
    HARD: t('common.hard'),
  }[difficulty] || difficulty
}

function submissionStatusType(status) {
  return {
    ACCEPTED: 'success',
    WRONG_ANSWER: 'danger',
    NOT_SUBMITTED: 'info',
  }[status] || 'info'
}

function submissionStatusLabel(status) {
  const labels = locale.value === 'zh-CN'
    ? {
        ACCEPTED: '已通过',
        WRONG_ANSWER: '答案错误',
        NOT_SUBMITTED: '未通过',
      }
    : {
        ACCEPTED: 'Accepted',
        WRONG_ANSWER: 'Wrong answer',
        NOT_SUBMITTED: 'Not submitted',
      }
  return labels[status] || labels.NOT_SUBMITTED
}
</script>
