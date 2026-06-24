<template>
  <section class="management-page">
    <div class="surface-card management-hero">
      <div>
        <span class="hero-kicker">{{ t('management.problemKicker') }}</span>
        <h1>{{ t('management.problemTitle') }}</h1>
        <p class="muted">{{ t('management.problemDescription') }}</p>
      </div>
      <div class="management-actions">
        <input ref="importInput" hidden type="file" accept=".json,application/json" @change="importProblems" />
        <el-button round :loading="importing" @click="importInput?.click()">{{ t('management.importJson') }}</el-button>
        <el-button type="primary" round @click="openEditor()">{{ t('management.createProblem') }}</el-button>
      </div>
    </div>

    <el-card shadow="never" class="management-card">
      <div class="management-filters">
        <el-input
          v-model="filters.keyword"
          clearable
          :placeholder="t('problem.search')"
          @keyup.enter="applyFilters"
          @clear="applyFilters"
        />
        <el-select v-model="filters.visible" clearable :placeholder="t('management.visibility')" @change="applyFilters">
          <el-option :label="t('management.public')" :value="1" />
          <el-option :label="t('management.hidden')" :value="0" />
        </el-select>
        <el-button type="primary" @click="applyFilters">{{ t('admin.search') }}</el-button>
      </div>

      <el-table :data="problems" row-key="id" v-loading="loading">
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="title" :label="t('problem.titleColumn')" min-width="240" show-overflow-tooltip />
        <el-table-column :label="t('problem.difficulty')" width="120">
          <template #default="{ row }">
            <el-tag :type="difficultyType(row.difficulty)" effect="plain">{{ row.difficulty }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('management.limits')" width="190">
          <template #default="{ row }">{{ row.timeLimitMs }} ms / {{ formatMemory(row.memoryLimitKb) }}</template>
        </el-table-column>
        <el-table-column :label="t('management.visibility')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.isVisible === 1 ? 'success' : 'info'">
              {{ row.isVisible === 1 ? t('management.public') : t('management.hidden') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('management.submissionStats')" width="140">
          <template #default="{ row }">{{ row.acceptedCount || 0 }} / {{ row.submitCount || 0 }}</template>
        </el-table-column>
        <el-table-column :label="t('common.time')" width="190">
          <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="270" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditor(row)">{{ t('admin.edit') }}</el-button>
            <el-button link type="success" @click="openUpload(row)">{{ t('management.testcases') }}</el-button>
            <el-button link @click="$router.push(`/problems/${row.id}`)">{{ t('management.preview') }}</el-button>
            <el-button link type="danger" @click="deleteProblem(row)">{{ t('admin.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="management-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="prev, pager, next, sizes, total"
          @current-change="fetchProblems"
          @size-change="applyFilters"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="editorVisible"
      :title="form.id ? t('management.editProblem') : t('management.createProblem')"
      width="min(900px, 96vw)"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="form-grid two-columns">
          <el-form-item :label="t('management.problemName')" prop="title">
            <el-input v-model="form.title" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item :label="t('problem.difficulty')" prop="difficulty">
            <el-select v-model="form.difficulty" style="width: 100%">
              <el-option label="EASY" value="EASY" />
              <el-option label="MEDIUM" value="MEDIUM" />
              <el-option label="HARD" value="HARD" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item :label="t('management.statement')" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="8" />
        </el-form-item>
        <div class="form-grid two-columns">
          <el-form-item :label="t('problem.input')">
            <el-input v-model="form.inputDescription" type="textarea" :rows="4" />
          </el-form-item>
          <el-form-item :label="t('problem.output')">
            <el-input v-model="form.outputDescription" type="textarea" :rows="4" />
          </el-form-item>
          <el-form-item :label="t('problem.sampleInput')">
            <el-input v-model="form.sampleInput" type="textarea" :rows="4" class="code-textarea" />
          </el-form-item>
          <el-form-item :label="t('problem.sampleOutput')">
            <el-input v-model="form.sampleOutput" type="textarea" :rows="4" class="code-textarea" />
          </el-form-item>
        </div>
        <div class="form-grid three-columns">
          <el-form-item :label="t('management.timeLimit')" prop="timeLimitMs">
            <el-input-number v-model="form.timeLimitMs" :min="100" :max="60000" :step="100" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('management.memoryLimit')" prop="memoryLimitKb">
            <el-input-number v-model="form.memoryLimitKb" :min="16384" :max="2097152" :step="16384" style="width: 100%" />
          </el-form-item>
          <el-form-item :label="t('management.visibility')">
            <el-switch
              v-model="form.isVisible"
              :active-value="1"
              :inactive-value="0"
              :active-text="t('management.public')"
              :inactive-text="t('management.hidden')"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ t('profile.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveProblem">{{ t('profile.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="uploadVisible" :title="t('management.uploadTitle', { id: uploadProblem?.id || '' })" width="min(560px, 94vw)">
      <el-alert :title="t('management.zipHint')" type="info" show-icon :closable="false" />
      <el-upload
        class="testcase-upload"
        drag
        accept=".zip,application/zip"
        :auto-upload="false"
        :limit="1"
        :on-change="selectTestcaseFile"
        :on-remove="() => { testcaseFile = null }"
      >
        <div class="el-upload__text">{{ t('management.dropZip') }}</div>
      </el-upload>
      <template #footer>
        <el-button @click="uploadVisible = false">{{ t('profile.cancel') }}</el-button>
        <el-button type="primary" :loading="uploading" :disabled="!testcaseFile" @click="uploadTestcases">
          {{ t('management.upload') }}
        </el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, reactive, ref } from 'vue'
import { onMounted } from 'vue'

import http from '@/api/http'
import { formatDateTime, useI18n } from '@/i18n'

const { t } = useI18n()
const problems = ref([])
const loading = ref(false)
const saving = ref(false)
const importing = ref(false)
const uploading = ref(false)
const editorVisible = ref(false)
const uploadVisible = ref(false)
const formRef = ref()
const importInput = ref()
const uploadProblem = ref(null)
const testcaseFile = ref(null)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filters = reactive({ keyword: '', visible: null })
const form = reactive(emptyProblem())

const rules = computed(() => ({
  title: [{ required: true, message: t('management.titleRequired'), trigger: 'blur' }],
  description: [{ required: true, message: t('management.statementRequired'), trigger: 'blur' }],
  difficulty: [{ required: true, message: t('management.difficultyRequired'), trigger: 'change' }],
}))

onMounted(fetchProblems)

function emptyProblem() {
  return {
    id: null,
    title: '',
    description: '',
    inputDescription: '',
    outputDescription: '',
    sampleInput: '',
    sampleOutput: '',
    difficulty: 'EASY',
    timeLimitMs: 1000,
    memoryLimitKb: 262144,
    isVisible: 0,
  }
}

async function fetchProblems() {
  loading.value = true
  try {
    const response = await http.get('/problems', {
      params: {
        page: page.value,
        size: size.value,
        keyword: filters.keyword || undefined,
        visible: filters.visible ?? undefined,
      },
    })
    problems.value = response.data?.records || []
    total.value = response.data?.total || 0
  } catch (error) {
    problems.value = []
    ElMessage.error(error.response?.data?.message || t('management.problemLoadFailed'))
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  fetchProblems()
}

async function openEditor(problem) {
  if (!problem) {
    Object.assign(form, emptyProblem())
    editorVisible.value = true
    return
  }
  try {
    const response = await http.get(`/problems/${problem.id}`)
    Object.assign(form, emptyProblem(), response.data)
    editorVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || t('management.problemLoadFailed'))
  }
}

async function saveProblem() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    delete payload.id
    if (form.id) {
      await http.put(`/teacher/problems/${form.id}`, payload)
    } else {
      await http.post('/teacher/problems', payload)
    }
    editorVisible.value = false
    ElMessage.success(t('management.problemSaved'))
    await fetchProblems()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || t('management.problemSaveFailed'))
  } finally {
    saving.value = false
  }
}

async function deleteProblem(problem) {
  try {
    await ElMessageBox.confirm(
      t('management.deleteProblemConfirm', { title: problem.title }),
      t('management.deleteProblem'),
      { type: 'warning', confirmButtonText: t('admin.delete'), cancelButtonText: t('profile.cancel') },
    )
    await http.delete(`/teacher/problems/${problem.id}`)
    ElMessage.success(t('management.problemDeleted'))
    await fetchProblems()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.response?.data?.message || t('management.problemDeleteFailed'))
  }
}

function openUpload(problem) {
  uploadProblem.value = problem
  testcaseFile.value = null
  uploadVisible.value = true
}

function selectTestcaseFile(file) {
  testcaseFile.value = file.raw
}

async function uploadTestcases() {
  if (!testcaseFile.value) return
  uploading.value = true
  try {
    const data = new FormData()
    data.append('file', testcaseFile.value)
    const response = await http.post(`/teacher/problems/${uploadProblem.value.id}/testcases`, data)
    ElMessage.success(t('management.testcasesUploaded', { count: response.data?.count || 0 }))
    uploadVisible.value = false
  } catch (error) {
    ElMessage.error(error.response?.data?.message || t('management.testcaseUploadFailed'))
  } finally {
    uploading.value = false
  }
}

async function importProblems(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  importing.value = true
  let succeeded = 0
  let failed = 0
  try {
    const parsed = JSON.parse(await file.text())
    const items = Array.isArray(parsed) ? parsed : (Array.isArray(parsed.problems) ? parsed.problems : [parsed])
    if (!items.length) throw new Error(t('management.emptyImport'))
    for (const item of items) {
      try {
        await http.post('/teacher/problems', normalizeImportedProblem(item))
        succeeded++
      } catch {
        failed++
      }
    }
    ElMessage.success(t('management.importResult', { succeeded, failed }))
    await fetchProblems()
  } catch (error) {
    ElMessage.error(error.message || t('management.importFailed'))
  } finally {
    importing.value = false
  }
}

function normalizeImportedProblem(item) {
  return {
    title: item.title,
    description: item.description,
    inputDescription: item.inputDescription || '',
    outputDescription: item.outputDescription || '',
    sampleInput: item.sampleInput || '',
    sampleOutput: item.sampleOutput || '',
    difficulty: String(item.difficulty || 'EASY').toUpperCase(),
    timeLimitMs: Number(item.timeLimitMs || 1000),
    memoryLimitKb: Number(item.memoryLimitKb || 262144),
    isVisible: Number(item.isVisible ?? 0),
  }
}

function difficultyType(value) {
  return { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' }[value] || 'info'
}

function formatMemory(value) {
  return value >= 1024 ? `${Math.round(value / 1024)} MB` : `${value} KB`
}
</script>
