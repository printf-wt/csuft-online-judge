<template>
  <section class="admin-users-page">
    <div class="surface-card admin-users-hero">
      <div>
        <span class="hero-kicker">{{ t('admin.usersKicker') }}</span>
        <h1>{{ t('admin.usersTitle') }}</h1>
        <p class="muted">{{ t('admin.usersDescription') }}</p>
      </div>
      <el-button type="primary" round @click="fetchUsers">{{ t('common.refresh') }}</el-button>
    </div>

    <el-card shadow="never" class="users-card">
      <div class="user-filters">
        <el-input
          v-model="filters.keyword"
          clearable
          :placeholder="t('admin.searchUsers')"
          @keyup.enter="applyFilters"
          @clear="applyFilters"
        />
        <el-select v-model="filters.role" clearable :placeholder="t('admin.filterRole')" @change="applyFilters">
          <el-option v-for="role in roles" :key="role" :label="role" :value="role" />
        </el-select>
        <el-select v-model="filters.status" clearable :placeholder="t('admin.filterStatus')" @change="applyFilters">
          <el-option :label="t('admin.active')" :value="1" />
          <el-option :label="t('admin.disabled')" :value="0" />
        </el-select>
        <el-button type="primary" @click="applyFilters">{{ t('admin.search') }}</el-button>
      </div>

      <el-table :data="users" row-key="id" v-loading="loading">
        <el-table-column prop="username" :label="t('auth.username')" min-width="150" />
        <el-table-column prop="nickname" :label="t('profile.nickname')" min-width="140" />
        <el-table-column prop="email" :label="t('profile.email')" min-width="210" />
        <el-table-column prop="role" :label="t('profile.role')" width="120">
          <template #default="{ row }"><el-tag effect="plain">{{ row.role }}</el-tag></template>
        </el-table-column>
        <el-table-column :label="t('common.status')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? t('admin.active') : t('admin.disabled') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="globalAcCount" :label="t('profile.globalAc')" width="110" />
        <el-table-column prop="submitCount" :label="t('profile.totalSubmits')" width="110" />
        <el-table-column :label="t('profile.joinedAt')" width="190">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column :label="t('common.action')" width="110" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEditor(row)">{{ t('admin.edit') }}</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="users-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="prev, pager, next, sizes, total"
          @current-change="fetchUsers"
          @size-change="applyFilters"
        />
      </div>
    </el-card>

    <el-dialog v-model="editorVisible" :title="t('admin.editUser')" width="min(460px, 92vw)">
      <el-descriptions :column="1" border class="user-summary">
        <el-descriptions-item :label="t('auth.username')">{{ editingUser?.username }}</el-descriptions-item>
        <el-descriptions-item :label="t('profile.email')">{{ editingUser?.email || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-position="top">
        <el-form-item :label="t('profile.role')">
          <el-select v-model="editForm.role" style="width: 100%">
            <el-option v-for="role in roles" :key="role" :label="role" :value="role" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('common.status')">
          <el-radio-group v-model="editForm.status">
            <el-radio-button :value="1">{{ t('admin.active') }}</el-radio-button>
            <el-radio-button :value="0">{{ t('admin.disabled') }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <el-alert :title="t('admin.sessionResetHint')" type="warning" show-icon :closable="false" />
      <template #footer>
        <el-button @click="editorVisible = false">{{ t('profile.cancel') }}</el-button>
        <el-button type="primary" :loading="saving" @click="saveUser">{{ t('profile.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { onMounted, reactive, ref } from 'vue'

import http from '@/api/http'
import { formatDateTime, useI18n } from '@/i18n'

const { t } = useI18n()
const roles = ['STUDENT', 'TEACHER', 'ADMIN']
const users = ref([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const editorVisible = ref(false)
const editingUser = ref(null)
const filters = reactive({ keyword: '', role: '', status: null })
const editForm = reactive({ role: 'STUDENT', status: 1 })

onMounted(fetchUsers)

async function fetchUsers() {
  loading.value = true
  try {
    const response = await http.get('/admin/users', {
      params: {
        page: page.value,
        size: size.value,
        keyword: filters.keyword || undefined,
        role: filters.role || undefined,
        status: filters.status ?? undefined,
      },
    })
    users.value = response.data?.records || []
    total.value = response.data?.total || 0
  } catch (error) {
    users.value = []
    ElMessage.error(error.response?.data?.message || t('admin.usersRequestFailed'))
  } finally {
    loading.value = false
  }
}

function applyFilters() {
  page.value = 1
  fetchUsers()
}

function openEditor(user) {
  editingUser.value = user
  Object.assign(editForm, { role: user.role, status: user.status })
  editorVisible.value = true
}

async function saveUser() {
  saving.value = true
  try {
    const response = await http.put(`/admin/users/${editingUser.value.id}`, editForm)
    const index = users.value.findIndex((item) => item.id === editingUser.value.id)
    if (index >= 0) users.value.splice(index, 1, response.data)
    editorVisible.value = false
    ElMessage.success(t('admin.userSaved'))
  } catch (error) {
    ElMessage.error(error.response?.data?.message || t('admin.userSaveFailed'))
  } finally {
    saving.value = false
  }
}
</script>
