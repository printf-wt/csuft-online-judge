<template>
  <section class="admin-page">
    <div class="surface-card admin-hero">
      <div>
        <span class="hero-kicker">{{ t('admin.observatory') }}</span>
        <h1>{{ t('admin.title') }}</h1>
        <p class="muted">{{ t('admin.description') }}</p>
      </div>
      <el-button type="primary" size="large" round @click="refreshAll">{{ t('common.refreshNow') }}</el-button>
    </div>

    <el-alert
      v-if="monitorError"
      :title="t('admin.monitorUnavailable')"
      :description="monitorError"
      type="error"
      show-icon
      :closable="false"
    />

    <div class="monitor-grid">
      <div class="monitor-card">
        <el-icon class="monitor-icon"><Monitor /></el-icon>
        <span>{{ t('admin.jvmUsed') }}</span>
        <strong>{{ formatBytes(latest.jvmUsedMemory) }}</strong>
        <p class="muted">{{ t('admin.max', { value: formatBytes(latest.jvmMaxMemory) }) }}</p>
      </div>
      <div class="monitor-card">
        <el-icon class="monitor-icon"><TrendCharts /></el-icon>
        <span>{{ t('admin.systemCpu') }}</span>
        <strong>{{ percent(latest.systemCpuLoad) }}</strong>
        <p class="muted">{{ t('admin.process', { value: percent(latest.processCpuLoad) }) }}</p>
      </div>
      <div class="monitor-card">
        <el-icon class="monitor-icon"><List /></el-icon>
        <span>{{ t('admin.judgeQueue') }}</span>
        <strong>{{ latest.judgeQueueSize ?? '-' }} / {{ latest.judgeQueueCapacity ?? '-' }}</strong>
        <p class="muted">{{ t('admin.queueUsage', { value: percent(latest.judgeQueueUtilization) }) }}</p>
      </div>
      <div class="monitor-card">
        <el-icon class="monitor-icon"><Clock /></el-icon>
        <span>{{ t('admin.judgeWorkers') }}</span>
        <strong>{{ latest.judgeActiveWorkers ?? '-' }} / {{ latest.judgeWorkerCount ?? '-' }}</strong>
        <p class="muted">{{ t('admin.uptime', { value: formatDuration(latest.uptimeMs) }) }}</p>
      </div>
    </div>

    <div class="chart-grid">
      <el-card class="dashboard-card" shadow="never">
        <template #header><strong>{{ t('admin.memoryCurve') }}</strong></template>
        <div ref="memoryChartEl" class="chart-host" />
      </el-card>
      <el-card class="dashboard-card" shadow="never">
        <template #header><strong>{{ t('admin.queueBacklog') }}</strong></template>
        <div ref="queueChartEl" class="chart-host" />
      </el-card>
    </div>

    <el-card class="dashboard-card" shadow="never">
      <template #header>
        <div class="audit-head">
          <div><strong>{{ t('admin.noticeManagement') }}</strong><p class="muted">{{ t('admin.noticeManagementDescription') }}</p></div>
          <el-button type="primary" @click="openNoticeEditor()">{{ t('admin.createNotice') }}</el-button>
        </div>
      </template>
      <el-table :data="notices" v-loading="noticeLoading" row-key="id">
        <el-table-column prop="title" :label="t('admin.noticeTitle')" min-width="220" />
        <el-table-column :label="t('admin.pinned')" width="90"><template #default="{ row }"><el-tag :type="row.isPinned ? 'warning' : 'info'">{{ row.isPinned ? t('admin.yes') : t('admin.no') }}</el-tag></template></el-table-column>
        <el-table-column :label="t('admin.visible')" width="90"><template #default="{ row }"><el-tag :type="row.isVisible ? 'success' : 'danger'">{{ row.isVisible ? t('admin.yes') : t('admin.no') }}</el-tag></template></el-table-column>
        <el-table-column :label="t('common.time')" width="190"><template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template></el-table-column>
        <el-table-column :label="t('common.action')" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openNoticeEditor(row)">{{ t('admin.edit') }}</el-button>
            <el-button link type="danger" @click="deleteNotice(row)">{{ t('admin.delete') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card class="dashboard-card" shadow="never">
      <template #header>
        <div class="audit-head">
          <div>
            <strong>{{ t('admin.auditLog') }}</strong>
            <p class="muted">{{ t('admin.auditDescription') }}</p>
          </div>
          <el-input v-model="actionFilter" clearable :placeholder="t('admin.filterAction')" style="max-width: 240px" @change="fetchAuditLogs" />
        </div>
      </template>

      <el-table :data="auditLogs" v-loading="auditLoading" row-key="id">
        <el-table-column prop="id" label="#" width="80" />
        <el-table-column prop="action" :label="t('common.action')" width="190" />
        <el-table-column prop="operatorId" :label="t('admin.operator')" width="110" />
        <el-table-column prop="ipAddress" :label="t('admin.ip')" width="150" />
        <el-table-column prop="targetType" :label="t('admin.target')" width="180" />
        <el-table-column prop="detail" :label="t('admin.detail')" min-width="260" show-overflow-tooltip />
        <el-table-column :label="t('common.time')" width="190">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
      </el-table>

      <div class="audit-pagination">
        <el-pagination
          v-model:current-page="auditPage"
          v-model:page-size="auditSize"
          :total="auditTotal"
          layout="prev, pager, next, sizes, total"
          @current-change="fetchAuditLogs"
          @size-change="fetchAuditLogs"
        />
      </div>
    </el-card>

    <el-dialog v-model="noticeEditorVisible" :title="noticeForm.id ? t('admin.editNotice') : t('admin.createNotice')" width="min(620px, 94vw)">
      <el-form label-position="top">
        <el-form-item :label="t('admin.noticeTitle')"><el-input v-model="noticeForm.title" maxlength="255" show-word-limit /></el-form-item>
        <el-form-item :label="t('admin.noticeContent')"><el-input v-model="noticeForm.content" type="textarea" :rows="8" maxlength="20000" show-word-limit /></el-form-item>
        <el-form-item>
          <el-checkbox v-model="noticeForm.isPinned">{{ t('admin.pinned') }}</el-checkbox>
          <el-checkbox v-model="noticeForm.isVisible">{{ t('admin.visible') }}</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="noticeEditorVisible = false">{{ t('profile.cancel') }}</el-button>
        <el-button type="primary" :loading="noticeSaving" @click="saveNotice">{{ t('profile.save') }}</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { Clock, List, Monitor, TrendCharts } from '@element-plus/icons-vue'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import * as echarts from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { ElMessage, ElMessageBox } from 'element-plus'
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'

import http from '@/api/http'
import { formatDateTime, useI18n } from '@/i18n'

const { locale, t } = useI18n()

echarts.use([LineChart, GridComponent, TooltipComponent, CanvasRenderer])

const memoryChartEl = ref()
const queueChartEl = ref()
const memoryChart = ref(null)
const queueChart = ref(null)
const samples = ref([])
const auditLogs = ref([])
const auditLoading = ref(false)
const actionFilter = ref('')
const auditPage = ref(1)
const auditSize = ref(10)
const auditTotal = ref(0)
const monitorError = ref('')
const notices = ref([])
const noticeLoading = ref(false)
const noticeSaving = ref(false)
const noticeEditorVisible = ref(false)
const noticeForm = reactive({ id: null, title: '', content: '', isPinned: false, isVisible: true })
let timer = null

const latest = computed(() => samples.value.at(-1) || {})

watch(locale, renderCharts)

onMounted(async () => {
  await nextTick()
  memoryChart.value = echarts.init(memoryChartEl.value)
  queueChart.value = echarts.init(queueChartEl.value)
  await refreshAll()
  timer = window.setInterval(fetchMonitor, 3000)
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.clearInterval(timer)
  window.removeEventListener('resize', resizeCharts)
  memoryChart.value?.dispose()
  queueChart.value?.dispose()
})

async function refreshAll() {
  await Promise.all([fetchMonitor(), fetchAuditLogs(), fetchNotices()])
}

async function fetchNotices() {
  noticeLoading.value = true
  try {
    const response = await http.get('/admin/notices', { params: { page: 1, size: 100 } })
    notices.value = response.data?.records || []
  } catch (error) {
    notices.value = []
    ElMessage.error(error.response?.data?.message || t('admin.noticeRequestFailed'))
  } finally {
    noticeLoading.value = false
  }
}

function openNoticeEditor(notice) {
  Object.assign(noticeForm, {
    id: notice?.id || null,
    title: notice?.title || '',
    content: notice?.content || '',
    isPinned: Boolean(notice?.isPinned),
    isVisible: notice ? Boolean(notice.isVisible) : true,
  })
  noticeEditorVisible.value = true
}

async function saveNotice() {
  if (!noticeForm.title.trim() || !noticeForm.content.trim()) {
    ElMessage.warning(t('admin.noticeRequired'))
    return
  }
  noticeSaving.value = true
  const payload = {
    title: noticeForm.title,
    content: noticeForm.content,
    isPinned: noticeForm.isPinned ? 1 : 0,
    isVisible: noticeForm.isVisible ? 1 : 0,
  }
  try {
    if (noticeForm.id) await http.put(`/admin/notices/${noticeForm.id}`, payload)
    else await http.post('/admin/notices', payload)
    noticeEditorVisible.value = false
    ElMessage.success(t('admin.noticeSaved'))
    await fetchNotices()
  } finally {
    noticeSaving.value = false
  }
}

async function deleteNotice(notice) {
  await ElMessageBox.confirm(t('admin.deleteNoticeConfirm', { title: notice.title }), t('admin.deleteNotice'), { type: 'warning' })
  await http.delete(`/admin/notices/${notice.id}`)
  ElMessage.success(t('admin.noticeDeleted'))
  await fetchNotices()
}

async function fetchMonitor() {
  try {
    const response = await http.get('/admin/system/monitor')
    pushSample(response.data)
    monitorError.value = ''
  } catch (error) {
    monitorError.value = error.response?.data?.message || t('admin.monitorRequestFailed')
  }
  renderCharts()
}

async function fetchAuditLogs() {
  auditLoading.value = true
  try {
    const response = await http.get('/admin/audit-logs', {
      params: { page: auditPage.value, size: auditSize.value, action: actionFilter.value || undefined },
    })
    auditLogs.value = response.data?.records || []
    auditTotal.value = response.data?.total || 0
  } catch (error) {
    auditLogs.value = []
    auditTotal.value = 0
    ElMessage.error(error.response?.data?.message || t('admin.auditRequestFailed'))
  } finally {
    auditLoading.value = false
  }
}

function pushSample(sample) {
  samples.value.push({ ...sample, timeLabel: new Date(sample.timestamp || Date.now()).toLocaleTimeString(locale.value) })
  if (samples.value.length > 30) samples.value.shift()
}

function renderCharts() {
  const labels = samples.value.map((item) => item.timeLabel)
  memoryChart.value?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 42, right: 18, top: 28, bottom: 36 },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value', axisLabel: { formatter: (value) => `${Math.round(value)}MB` } },
    series: [{ name: t('admin.usedMemory'), type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: samples.value.map((item) => bytesToMb(item.jvmUsedMemory)), color: '#006b4f' }],
  })
  queueChart.value?.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 36, right: 18, top: 28, bottom: 36 },
    xAxis: { type: 'category', data: labels },
    yAxis: { type: 'value' },
    series: [{ name: t('admin.queueSize'), type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: samples.value.map((item) => item.judgeQueueSize || 0), color: '#f59e0b' }],
  })
}

function resizeCharts() {
  memoryChart.value?.resize()
  queueChart.value?.resize()
}

function formatBytes(value) {
  if (!value) return '0 MB'
  return `${bytesToMb(value).toFixed(1)} MB`
}

function bytesToMb(value) {
  return (value || 0) / 1024 / 1024
}

function percent(value) {
  if (value == null || value < 0) return '-'
  return `${Math.round(value * 100)}%`
}

function formatDuration(value) {
  if (value == null) return '-'
  const totalMinutes = Math.floor(value / 60000)
  const days = Math.floor(totalMinutes / 1440)
  const hours = Math.floor((totalMinutes % 1440) / 60)
  const minutes = totalMinutes % 60
  return days > 0 ? `${days}d ${hours}h` : `${hours}h ${minutes}m`
}
</script>
