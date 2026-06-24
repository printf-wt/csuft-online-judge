<template>
  <section class="surface-card view-card">
    <div class="problem-header">
      <div>
        <span class="hero-kicker">{{ t('problem.problemNumber', { id: problem.id }) }}</span>
        <h1 class="view-title">{{ problem.title }}</h1>
      </div>
      <div class="problem-status-tags">
        <el-tag :type="submissionStatusType(problem.submissionStatus)" effect="plain" round>
          {{ submissionStatusLabel(problem.submissionStatus) }}
        </el-tag>
        <el-tag :type="difficultyType(problem.difficulty)" effect="dark" round>{{ difficultyLabel(problem.difficulty) }}</el-tag>
      </div>
    </div>

    <div class="limit-grid">
      <div><span>{{ t('common.time') }}</span><strong>{{ problem.timeLimitMs }} ms</strong></div>
      <div><span>{{ t('common.memory') }}</span><strong>{{ problem.memoryLimitKb }} KB</strong></div>
    </div>

    <article class="markdown-body" v-html="renderMarkdown(problem.description)" />

    <section class="statement-block" v-if="problem.inputDescription">
      <h3>{{ t('problem.input') }}</h3>
      <p>{{ problem.inputDescription }}</p>
    </section>
    <section class="statement-block" v-if="problem.outputDescription">
      <h3>{{ t('problem.output') }}</h3>
      <p>{{ problem.outputDescription }}</p>
    </section>

    <section class="sample-grid" v-if="problem.sampleInput || problem.sampleOutput">
      <div class="sample-card surface-card">
        <div class="sample-head"><strong>{{ t('problem.sampleInput') }}</strong></div>
        <pre>{{ problem.sampleInput }}</pre>
      </div>
      <div class="sample-card surface-card">
        <div class="sample-head"><strong>{{ t('problem.sampleOutput') }}</strong></div>
        <pre>{{ problem.sampleOutput }}</pre>
      </div>
    </section>

    <div class="problem-actions">
      <router-link :to="`/problems/${route.params.id}/solve`">
        <el-button type="primary" size="large">{{ t('problem.startSolving') }}</el-button>
      </router-link>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'

import http from '@/api/http'
import { useI18n } from '@/i18n'

const route = useRoute()
const { t, locale } = useI18n()

const problem = reactive({
  id: route.params.id,
  title: '',
  difficulty: 'EASY',
  description: '',
  inputDescription: '',
  outputDescription: '',
  sampleInput: '',
  sampleOutput: '',
  timeLimitMs: 1000,
  memoryLimitKb: 262144,
  submissionStatus: 'NOT_SUBMITTED',
})

onMounted(() => fetchProblem())

async function fetchProblem() {
  try {
    const response = await http.get(`/problems/${route.params.id}`)
    Object.assign(problem, response.data)
  } catch {
    // error handled by http interceptor
  }
}

function renderMarkdown(markdown) {
  if (!markdown) return ''
  return markdown
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/\n/g, '<br>')
    .replace(/^/, '<p>')
    .replace(/$/, '</p>')
}

function difficultyType(difficulty) {
  return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[difficulty] || 'info'
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

<style scoped>
.problem-status-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}
</style>
