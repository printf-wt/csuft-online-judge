<template>
  <section class="management-page">
    <div class="surface-card management-hero">
      <div>
        <span class="hero-kicker">{{ t('management.contestKicker') }}</span>
        <h1>{{ t('management.contestTitle') }}</h1>
        <p class="muted">{{ t('management.contestDescription') }}</p>
      </div>
      <el-button type="primary" round @click="openEditor()">{{ t('management.createContest') }}</el-button>
    </div>

    <el-card shadow="never" class="management-card">
      <el-table :data="contests" row-key="id" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="title" :label="t('management.contestName')" min-width="230" show-overflow-tooltip />
        <el-table-column prop="ruleType" :label="t('management.rule')" width="90" />
        <el-table-column :label="t('management.period')" min-width="330">
          <template #default="{ row }">
            {{ formatDateTime(row.startTime) }} - {{ formatDateTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column :label="t('management.visibility')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isPublic === 1 ? 'success' : 'info'">
              {{ row.isPublic === 1 ? t('management.public') : t('management.private') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? t('admin.active') : t('admin.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="280" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditor(row)">{{ t('admin.edit') }}</el-button>
            <el-button link type="success" @click="openBindings(row)">{{ t('management.bindProblems') }}</el-button>
            <el-button link @click="$router.push(`/contests/${row.id}`)">{{ t('management.preview') }}</el-button>
            <el-button link type="danger" @click="deleteContest(row)">{{ t('admin.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="management-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="prev, pager, next, sizes, total"
          @current-change="fetchContests"
          @size-change="resetPage"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="editorVisible"
      :title="form.id ? t('management.editContest') : t('management.createContest')"
      width="min(720px, 95vw)"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item :label="t('management.contestName')" prop="title">
          <el-input v-model="form.title" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item :label="t('management.contestIntro')">
          <el-input v-model="form.description" type="textarea" :rows="5" maxlength="65535" />
        </el-form-item>
        <div class="form-grid two-columns">
          <el-form-item :label="t('management.rule')" prop="ruleType">
            <el-radio-group v-model="form.ruleType">
              <el-radio-button value="ACM">ACM</el-radio-button>
              <el-radio-button value="IOI">IOI</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item :label="t('management.visibility')">
            <el-switch
              v-model="form.isPublic"
              :active-value="1"
              :inactive-value="0"
              :active-text="t('management.public')"
              :inactive-text="t('management.private')"
            />
          </el-form-item>
          <el-form-item :label="t('management.startTime')" prop="startTime">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :placeholder="t('management.selectTime')"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item :label="t('management.endTime')" prop="endTime">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              :placeholder="t('management.selectTime')"
              style="width: 100%"
            />
          </el-form-item>
        </div>
        <el-form-item v-if="form.id" :label="t('common.status')">
          <el-switch
            v-model="form.status"
            :active-value="1"
            :inactive-value="0"
            :active-text="t('admin.active')"
            :inactive-text="t('admin.disabled')"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">{{ t('profile.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveContest">{{ t('profile.save') }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="bindingsVisible"
      :title="t('management.bindTitle', { title: bindingContest?.title || '' })"
      width="min(900px, 96vw)"
    >
      <el-alert :title="t('management.bindingHint')" type="info" show-icon :closable="false" />
      <div class="binding-list">
        <div v-for="(item, index) in bindings" :key="item.key" class="binding-row">
          <el-select v-model="item.problemId" filterable :placeholder="t('management.selectProblem')">
            <el-option
              v-for="problem in availableProblems"
              :key="problem.id"
              :label="`#${problem.id} ${problem.title}`"
              :value="problem.id"
            />
          </el-select>
          <el-input v-model="item.alias" maxlength="16" :placeholder="t('management.alias')" />
          <el-input-number v-model="item.score" :min="1" :max="100000" :placeholder="t('common.score')" />
          <el-button type="danger" plain @click="removeBinding(index)">{{ t('admin.delete') }}</el-button>
        </div>
      </div>
      <el-button class="add-binding" plain type="primary" @click="addBinding">{{ t('management.addProblem') }}</el-button>
      <template #footer>
        <el-button @click="bindingsVisible = false">{{ t('profile.cancel') }}</el-button>
        <el-button type="primary" :loading="bindingSaving" @click="saveBindings">{{ t('management.saveBindings') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, onMounted, reactive, ref } from 'vue'

import http from '@/api/http'
import { formatDateTime, useI18n } from '@/i18n'

const { t } = useI18n()
const contests = ref([])
const availableProblems = ref([])
const bindings = ref([])
const loading = ref(false)
const saving = ref(false)
const bindingSaving = ref(false)
const editorVisible = ref(false)
const bindingsVisible = ref(false)
const bindingContest = ref(null)
const formRef = ref()
const page = ref(1)
const size = ref(20)
const total = ref(0)
const form = reactive(emptyContest())

const rules = computed(() => ({
  title: [{ required: true, message: t('management.contestTitleRequired'), trigger: 'blur' }],
  ruleType: [{ required: true, message: t('management.ruleRequired'), trigger: 'change' }],
  startTime: [{ required: true, message: t('management.startRequired'), trigger: 'change' }],
  endTime: [
    { required: true, message: t('management.endRequired'), trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (form.startTime && value && new Date(value) <= new Date(form.startTime)) {
          callback(new Error(t('management.endAfterStart')))
        } else callback()
      },
      trigger: 'change',
    },
  ],
}))

onMounted(fetchContests)

function emptyContest() {
  const start = new Date(Date.now() + 3600000)
  const end = new Date(Date.now() + 3 * 3600000)
  return {
    id: null,
    title: '',
    description: '',
    ruleType: 'ACM',
    startTime: localDateTime(start),
    endTime: localDateTime(end),
    isPublic: 1,
    status: 1,
  }
}

async function fetchContests() {
  loading.value = true
  try {
    const response = await http.get('/contests', { params: { page: page.value, size: size.value } })
    contests.value = response.data?.records || []
    total.value = response.data?.total || 0
  } catch (error) {
    contests.value = []
    ElMessage.error(error.response?.data?.message || t('management.contestLoadFailed'))
  } finally {
    loading.value = false
  }
}

function resetPage() {
  page.value = 1
  fetchContests()
}

function openEditor(contest) {
  Object.assign(form, emptyContest(), contest || {})
  editorVisible.value = true
}

async function saveContest() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = {
      title: form.title,
      description: form.description,
      ruleType: form.ruleType,
      startTime: form.startTime,
      endTime: form.endTime,
      isPublic: form.isPublic,
    }
    if (form.id) {
      payload.status = form.status
      await http.put(`/teacher/contests/${form.id}`, payload)
    } else {
      await http.post('/teacher/contests', payload)
    }
    editorVisible.value = false
    ElMessage.success(t('management.contestSaved'))
    await fetchContests()
  } catch (error) {
    ElMessage.error(error.response?.data?.message || t('management.contestSaveFailed'))
  } finally {
    saving.value = false
  }
}

async function deleteContest(contest) {
  try {
    await ElMessageBox.confirm(
      t('management.deleteContestConfirm', { title: contest.title }),
      t('management.deleteContest'),
      { type: 'warning', confirmButtonText: t('admin.delete'), cancelButtonText: t('profile.cancel') },
    )
    await http.delete(`/teacher/contests/${contest.id}`)
    ElMessage.success(t('management.contestDeleted'))
    await fetchContests()
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.response?.data?.message || t('management.contestDeleteFailed'))
  }
}

async function openBindings(contest) {
  bindingContest.value = contest
  try {
    const [problemResponse, bindingResponse] = await Promise.all([
      http.get('/problems', { params: { page: 1, size: 100 } }),
      http.get(`/contests/${contest.id}/problems`),
    ])
    availableProblems.value = problemResponse.data?.records || []
    bindings.value = (bindingResponse.data || []).map((item, index) => ({
      key: `${item.id || 'binding'}-${index}`,
      problemId: item.problemId,
      alias: item.alias,
      score: item.score || 100,
    }))
    if (!bindings.value.length) addBinding()
    bindingsVisible.value = true
  } catch (error) {
    ElMessage.error(error.response?.data?.message || t('management.bindingLoadFailed'))
  }
}

function addBinding() {
  const index = bindings.value.length
  bindings.value.push({
    key: `new-${Date.now()}-${index}`,
    problemId: null,
    alias: aliasFor(index),
    score: 100,
  })
}

function removeBinding(index) {
  bindings.value.splice(index, 1)
}

async function saveBindings() {
  if (!bindings.value.length || bindings.value.some((item) => !item.problemId || !item.alias?.trim())) {
    ElMessage.warning(t('management.bindingRequired'))
    return
  }
  const aliases = bindings.value.map((item) => item.alias.trim().toUpperCase())
  const problemIds = bindings.value.map((item) => item.problemId)
  if (new Set(aliases).size !== aliases.length || new Set(problemIds).size !== problemIds.length) {
    ElMessage.warning(t('management.bindingDuplicate'))
    return
  }
  bindingSaving.value = true
  try {
    await http.post(`/teacher/contests/${bindingContest.value.id}/problems`, {
      problems: bindings.value.map((item, index) => ({
        problemId: item.problemId,
        alias: item.alias.trim().toUpperCase(),
        sortOrder: index,
        score: item.score || 100,
      })),
    })
    bindingsVisible.value = false
    ElMessage.success(t('management.bindingsSaved'))
  } catch (error) {
    ElMessage.error(error.response?.data?.message || t('management.bindingSaveFailed'))
  } finally {
    bindingSaving.value = false
  }
}

function aliasFor(index) {
  return index < 26 ? String.fromCharCode(65 + index) : `P${index + 1}`
}

function localDateTime(date) {
  const offset = date.getTimezoneOffset() * 60000
  return new Date(date.getTime() - offset).toISOString().slice(0, 19)
}
</script>
