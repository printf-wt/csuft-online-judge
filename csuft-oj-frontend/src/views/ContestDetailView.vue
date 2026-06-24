<template>
  <section class="contest-detail-page">
    <div class="surface-card contest-detail-hero">
      <div>
        <span class="hero-kicker">{{ contest.ruleType || t('common.contests') }}</span>
        <h1>{{ contest.title }}</h1>
        <p class="muted">{{ contest.description || t('contest.liveDescription') }}</p>
      </div>
      <div class="contest-status-panel">
        <el-tag :type="statusMeta.type" effect="dark" round>{{ statusMeta.label }}</el-tag>
        <strong v-if="statusMeta.key === 'running'">{{ countdown(contest.endTime) }}</strong>
        <span class="muted">{{ formatDateTime(contest.startTime) }} - {{ formatDateTime(contest.endTime) }}</span>
        <el-button
          v-if="statusMeta.key !== 'ended'"
          type="primary"
          :loading="registering"
          @click="registerContest"
        >
          {{ registered ? t('contest.registered') : t('contest.register') }}
        </el-button>
      </div>
    </div>

    <el-card class="contest-tabs-card" shadow="never">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane :label="t('contest.problems')" name="problems">
          <el-table :data="contestProblems" row-key="id">
            <el-table-column :label="t('contest.alias')" width="90">
              <template #default="{ row }"><span class="alias-badge">{{ row.alias }}</span></template>
            </el-table-column>
            <el-table-column :label="t('contest.problemId')" prop="problemId" width="130" />
            <el-table-column :label="t('common.score')" prop="score" width="100" />
            <el-table-column :label="t('common.action')" width="160">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="$router.push(`/contests/${contestId}/problems/${row.problemId}/solve`)">
                  {{ t('contest.solveAlias', { alias: row.alias }) }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('common.submissions')" name="submissions">
          <el-table :data="submissions" row-key="id" v-loading="submissionsLoading">
            <el-table-column prop="id" label="#" width="90" />
            <el-table-column prop="problemId" :label="t('contest.problem')" width="120" />
            <el-table-column prop="language" :label="t('common.language')" width="120" />
            <el-table-column :label="t('common.status')" width="180">
              <template #default="{ row }"><el-tag :type="submissionType(row.status)" effect="dark">{{ row.status }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="score" :label="t('common.score')" width="100" />
            <el-table-column :label="t('contest.submittedAt')">
              <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="t('contest.standings')" name="standings">
          <div class="standings-scroll" v-loading="rankLoading">
            <el-table :data="ranklist.rows" row-key="userId" class="standings-table">
              <el-table-column :label="t('common.rank')" width="90" fixed="left">
                <template #default="{ row }">#{{ row.rank }}</template>
              </el-table-column>
              <el-table-column :label="t('common.user')" min-width="180" fixed="left">
                <template #default="{ row }">
                  <div class="user-cell">
                    <strong>{{ row.nickname || row.username || `User ${row.userId}` }}</strong>
                    <p class="muted">{{ row.username }}</p>
                  </div>
                </template>
              </el-table-column>
              <el-table-column v-if="isAcm" prop="acceptedCount" :label="t('contest.accepted')" width="110" />
              <el-table-column v-if="isAcm" prop="totalPenaltyMinutes" :label="t('contest.penalty')" width="110" />
              <el-table-column v-if="!isAcm" prop="totalScore" :label="t('common.total')" width="100" />
              <el-table-column
                v-for="(problem, index) in ranklist.problems"
                :key="problem.id"
                :label="problem.alias"
                width="110"
                align="center"
              >
                <template #default="{ row }">
                  <span v-if="isAcm" class="acm-cell" :class="acmCellClass(row.problems[index] || {})">
                    {{ acmCellText(row.problems[index] || {}) }}
                  </span>
                  <span
                    v-else
                    class="ioi-cell"
                    :style="{ background: ioiCellColor(row.problems[index] || {}, fullScore(problem.id)) }"
                  >
                    {{ row.problems[index]?.score ?? 0 }}
                  </span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

import http from '@/api/http'
import { formatDateTime, useI18n } from '@/i18n'

const route = useRoute()
const { t } = useI18n()
const contestId = computed(() => route.params.id)
const activeTab = ref('problems')
const contestProblems = ref([])
const submissions = ref([])
const submissionsLoading = ref(false)
const rankLoading = ref(false)
const registering = ref(false)
const registered = ref(false)
const now = ref(Date.now())
let timer = null

const contest = reactive({
  id: contestId.value,
  title: t('contest.loading'),
  description: '',
  ruleType: '',
  startTime: null,
  endTime: null,
})

const ranklist = reactive({
  problems: [],
  rows: [],
})

const isAcm = computed(() => (ranklist.ruleType || contest.ruleType || 'ACM') === 'ACM')
const statusMeta = computed(() => {
  const start = new Date(contest.startTime).getTime()
  const end = new Date(contest.endTime).getTime()
  if (now.value < start) return { key: 'upcoming', label: t('common.upcoming'), type: 'primary' }
  if (now.value <= end) return { key: 'running', label: t('common.running'), type: 'success' }
  return { key: 'ended', label: t('common.ended'), type: 'info' }
})

onMounted(async () => {
  await Promise.all([fetchContest(), fetchProblems(), fetchRanklist()])
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onBeforeUnmount(() => window.clearInterval(timer))

async function fetchContest() {
  try {
    const response = await http.get(`/contests/${contestId.value}`)
    Object.assign(contest, response.data)
  } catch {
    contest.title = t('contest.loadFailed')
    ElMessage.error(t('contest.loadFailed'))
  }
}

async function registerContest() {
  registering.value = true
  try {
    await http.post(`/contests/${contestId.value}/register`)
    registered.value = true
    ElMessage.success(t('contest.registerSuccess'))
  } catch (error) {
    ElMessage.error(error.response?.data?.message || t('contest.registerFailed'))
  } finally {
    registering.value = false
  }
}

async function fetchProblems() {
  try {
    const response = await http.get(`/contests/${contestId.value}/problems`)
    contestProblems.value = response.data || []
  } catch {
    contestProblems.value = []
  }
}

async function fetchSubmissions() {
  submissionsLoading.value = true
  try {
    const response = await http.get('/submissions', { params: { contestId: contestId.value, page: 1, size: 50 } })
    submissions.value = response.data?.records || []
  } catch {
    submissions.value = []
  } finally {
    submissionsLoading.value = false
  }
}

async function fetchRanklist() {
  rankLoading.value = true
  try {
    const response = await http.get(`/contests/${contestId.value}/ranklist`)
    Object.assign(ranklist, response.data)
  } catch {
    Object.assign(ranklist, { problems: [], rows: [] })
  } finally {
    rankLoading.value = false
  }
}

function handleTabChange(name) {
  if (name === 'submissions') fetchSubmissions()
  if (name === 'standings') fetchRanklist()
}

function acmCellText(problem) {
  if (problem.accepted) return `+${problem.wrongAttemptsBeforeAc || 0} (${formatPenalty(problem.penaltyMinutes)})`
  if (problem.wrongAttemptsBeforeAc > 0) return `-${problem.wrongAttemptsBeforeAc}`
  return ''
}

function acmCellClass(problem) {
  if (problem.accepted) return 'is-solved'
  if (problem.wrongAttemptsBeforeAc > 0) return 'is-failed'
  return 'is-empty'
}

function ioiCellColor(problem, maxScore) {
  const ratio = maxScore ? problem.score / maxScore : 0
  if (ratio >= 0.8) return '#dff3e8'
  if (ratio >= 0.4) return '#f8edce'
  if (ratio > 0) return '#f8ddda'
  return 'rgba(64, 82, 75, 0.12)'
}

function fullScore(problemId) {
  return ranklist.problems.find((problem) => problem.problemId === problemId)?.score || 100
}

function formatPenalty(minutes) {
  const safe = Math.max(0, minutes || 0)
  const h = Math.floor(safe / 60)
  const m = String(safe % 60).padStart(2, '0')
  return `${h}:${m}`
}

function submissionType(status) {
  if (status === 'ACCEPTED') return 'success'
  if (status === 'WRONG_ANSWER') return 'warning'
  if (status === 'COMPILE_ERROR') return 'danger'
  return 'info'
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
