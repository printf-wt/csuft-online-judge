<template>
  <section class="playground-page">
    <div class="playground-shell">
      <aside class="problem-pane surface-card">
        <div class="pane-head">
          <div>
            <span class="hero-kicker">{{ contestId ? t('problem.contestNumber', { id: contestId }) : t('problem.problemNumber', { id: problem.id }) }}</span>
            <h1>{{ problem.title }}</h1>
          </div>
          <el-tag :type="difficultyType(problem.difficulty)" effect="dark" round>{{ difficultyLabel(problem.difficulty) }}</el-tag>
        </div>

        <div class="limit-grid">
          <div><span>{{ t('common.time') }}</span><strong>{{ problem.timeLimitMs }} ms</strong></div>
          <div><span>{{ t('common.memory') }}</span><strong>{{ problem.memoryLimitKb }} KB</strong></div>
        </div>

        <article class="markdown-body" v-html="renderMarkdown(problem.description)" />

        <section class="statement-block">
          <h3>{{ t('problem.input') }}</h3>
          <p>{{ problem.inputDescription }}</p>
        </section>
        <section class="statement-block">
          <h3>{{ t('problem.output') }}</h3>
          <p>{{ problem.outputDescription }}</p>
        </section>

        <section class="sample-grid">
          <div class="sample-card">
            <div class="sample-head">
              <strong>{{ t('problem.sampleInput') }}</strong>
              <el-button size="small" text @click="copyText(problem.sampleInput)">{{ t('common.copy') }}</el-button>
            </div>
            <pre>{{ problem.sampleInput }}</pre>
          </div>
          <div class="sample-card">
            <div class="sample-head">
              <strong>{{ t('problem.sampleOutput') }}</strong>
              <el-button size="small" text @click="copyText(problem.sampleOutput)">{{ t('common.copy') }}</el-button>
            </div>
            <pre>{{ problem.sampleOutput }}</pre>
          </div>
        </section>
      </aside>

      <main class="editor-pane surface-card">
        <div class="editor-toolbar">
          <el-select v-model="selectedLanguage" size="large" class="language-select" @change="resetCode">
            <el-option v-for="item in languages" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <el-input-number v-model="fontSize" :min="12" :max="24" size="large" controls-position="right" />
          <el-button size="large" @click="resetCode">{{ t('common.reset') }}</el-button>
          <el-button size="large" type="primary" :loading="submitting || judging" @click="submitCode">
            {{ judging ? t('problem.judging') : t('problem.submitCode') }}
          </el-button>
        </div>

        <div ref="editorEl" class="monaco-host" />

        <el-card v-if="resultVisible" class="judge-card" :class="resultClass" shadow="never">
          <template #header>
            <div class="judge-head">
              <span class="judge-badge">{{ result.status }}</span>
              <span class="muted">{{ t('problem.submission', { id: submissionId }) }}</span>
            </div>
          </template>
          <p>{{ result.judgeMessage || statusMessage }}</p>
          <div class="judge-meta">
            <span>{{ t('common.time') }}: {{ result.timeUsedMs ?? '-' }} ms</span>
            <span>{{ t('common.memory') }}: {{ result.memoryUsedKb ?? '-' }} KB</span>
          </div>
          <pre v-if="result.status === 'COMPILE_ERROR' && result.errorLog" class="compile-log">{{ result.errorLog }}</pre>
        </el-card>
      </main>
    </div>
  </section>
</template>

<script setup>
import loader from '@monaco-editor/loader'
import { ElMessage } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef, watch } from 'vue'
import { useRoute } from 'vue-router'

import http from '@/api/http'
import { useI18n } from '@/i18n'

const route = useRoute()
const { t } = useI18n()
const contestId = computed(() => route.params.contestId || null)
const editorEl = ref()
const editor = shallowRef(null)
const monacoInstance = shallowRef(null)
const submitting = ref(false)
const judging = ref(false)
const submissionId = ref(null)
const result = ref(null)
const pollTimer = ref(null)
let pollRetryCount = 0
const MAX_POLL_RETRIES = 120
const selectedLanguage = ref('C++')
const fontSize = ref(15)

const problem = reactive({
  id: route.params.id,
  title: t('problem.loadingTitle'),
  difficulty: 'EASY',
  description: t('problem.loadingStatement'),
  inputDescription: t('problem.inputDescription'),
  outputDescription: t('problem.outputDescription'),
  sampleInput: '1 2',
  sampleOutput: '3',
  timeLimitMs: 1000,
  memoryLimitKb: 262144,
})

const languages = [
  { label: 'C++17', value: 'C++', monaco: 'cpp' },
  { label: 'Java 17', value: 'JAVA', monaco: 'java' },
  { label: 'Python 3', value: 'PYTHON', monaco: 'python' },
  { label: 'Go', value: 'GO', monaco: 'go' },
]

const templates = {
  'C++': '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n\n    return 0;\n}\n',
  JAVA: 'import java.io.*;\nimport java.util.*;\n\npublic class Main {\n    public static void main(String[] args) throws Exception {\n        Scanner sc = new Scanner(System.in);\n    }\n}\n',
  PYTHON: 'import sys\n\n\ndef main():\n    data = sys.stdin.read().strip().split()\n\n\nif __name__ == "__main__":\n    main()\n',
  GO: 'package main\n\nimport "fmt"\n\nfunc main() {\n    fmt.Println("Hello, CSUFT OJ")\n}\n',
}

const resultVisible = computed(() => Boolean(result.value || judging.value))
const statusMessage = computed(() => (judging.value ? t('problem.judgeRunning') : t('problem.judgeCompleted')))
const resultClass = computed(() => {
  const status = result.value?.status
  if (status === 'ACCEPTED') return 'is-accepted'
  if (status === 'COMPILE_ERROR') return 'is-compile-error'
  if (status === 'WRONG_ANSWER') return 'is-wrong'
  return 'is-pending'
})

onMounted(async () => {
  await fetchProblem()
  await initEditor()
})

onBeforeUnmount(() => {
  stopPolling()
  editor.value?.dispose()
})

watch(fontSize, (value) => {
  editor.value?.updateOptions({ fontSize: value })
})

async function fetchProblem() {
  try {
    const response = await http.get(`/problems/${route.params.id}`)
    Object.assign(problem, normalizeProblem(response.data))
  } catch {
    Object.assign(problem, normalizeProblem({
      id: route.params.id,
      title: 'A+B Problem',
      difficulty: 'EASY',
      description: 'Given two integers `a` and `b`, output their sum.\n\nThis is the classic warm-up ritual.',
      inputDescription: 'Two integers a and b.',
      outputDescription: 'One integer, the sum of a and b.',
      sampleInput: '1 2',
      sampleOutput: '3',
      timeLimitMs: 1000,
      memoryLimitKb: 262144,
    }))
  }
}

async function initEditor() {
  await nextTick()
  const monaco = await loader.init()
  monacoInstance.value = monaco
  editor.value = monaco.editor.create(editorEl.value, {
    value: templates[selectedLanguage.value],
    language: currentMonacoLanguage(),
    theme: document.documentElement.classList.contains('dark') ? 'vs-dark' : 'vs',
    fontSize: fontSize.value,
    minimap: { enabled: false },
    automaticLayout: true,
    roundedSelection: true,
    scrollBeyondLastLine: false,
    padding: { top: 18, bottom: 18 },
  })
}

function resetCode() {
  if (monacoInstance.value && editor.value) {
    monacoInstance.value.editor.setModelLanguage(editor.value.getModel(), currentMonacoLanguage())
  }
  editor.value?.setValue(templates[selectedLanguage.value])
}

async function submitCode() {
  const code = editor.value?.getValue() || ''
  if (!code.trim()) {
    ElMessage.warning(t('problem.codeEmpty'))
    return
  }

  submitting.value = true
  result.value = null
  try {
    const response = await http.post('/submissions', {
      problemId: Number(route.params.id),
      code,
      language: selectedLanguage.value,
      contestId: contestId.value ? Number(contestId.value) : null,
    })
    submissionId.value = response.data.submissionId
    result.value = { status: 'PENDING' }
    judging.value = true
    ElMessage.success(t('problem.submitted', { id: submissionId.value }))
    startPolling()
  } catch (error) {
    const msg = error.response?.data?.message || t('problem.submitFailed')
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

function startPolling() {
  stopPolling()
  pollRetryCount = 0
  pollTimer.value = window.setInterval(fetchSubmissionResult, 1000)
  fetchSubmissionResult()
}

function stopPolling() {
  if (pollTimer.value) {
    window.clearInterval(pollTimer.value)
    pollTimer.value = null
  }
}

async function fetchSubmissionResult() {
  if (!submissionId.value) return
  pollRetryCount++
  if (pollRetryCount > MAX_POLL_RETRIES) {
    judging.value = false
    stopPolling()
    ElMessage.warning(t('problem.judgeTimeout'))
    return
  }
  try {
    const response = await http.get(`/submissions/${submissionId.value}`)
    const submission = response.data
    result.value = submission

    if (!['PENDING', 'COMPILING', 'RUNNING', 'JUDGING'].includes(submission.status)) {
      judging.value = false
      stopPolling()
    }
  } catch {
    // silently retry on network errors
  }
}

function normalizeProblem(data) {
  return {
    ...data,
    difficulty: (data.difficulty || 'EASY').toUpperCase(),
    timeLimitMs: data.timeLimitMs || 1000,
    memoryLimitKb: data.memoryLimitKb || 262144,
    description: data.description || t('problem.noStatement'),
    inputDescription: data.inputDescription || t('problem.noInput'),
    outputDescription: data.outputDescription || t('problem.noOutput'),
    sampleInput: data.sampleInput || '',
    sampleOutput: data.sampleOutput || '',
  }
}

function currentMonacoLanguage() {
  return languages.find((item) => item.value === selectedLanguage.value)?.monaco || 'cpp'
}

function renderMarkdown(markdown) {
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

async function copyText(value) {
  await navigator.clipboard.writeText(value || '')
  ElMessage.success(t('common.copied'))
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
</script>
