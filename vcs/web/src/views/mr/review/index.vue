<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Search, View, Refresh, MagicStick } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import {
  getMrReviewPage,
  getMrReviewDetail,
  scanMrReviewProject,
  scanMrReviewAll
} from '@/api/mrReview'
import { getProjectList } from '@/api/project'

const loading = ref(false)
const scanning = ref(false)
const tableData = ref([])
const total = ref(0)
const projectOptions = ref([])  // 從 DB 動態載入

const queryParams = reactive({
  page: 1,
  pageSize: 10,
  projectName: '',
  reviewStatus: '',
  state: 'opened'
})

const drawerVisible = ref(false)
const currentReview = ref({})

const statusTag = (status) => {
  const map = {
    PENDING: { type: 'warning', label: '審核中' },
    COMPLETED: { type: 'success', label: '已完成' },
    FAILED: { type: 'danger', label: '失敗' }
  }
  return map[status] || { type: 'info', label: status || '未知' }
}

const severityTag = (level) => {
  const map = {
    5: { type: 'danger', label: 'Critical' },
    4: { type: 'danger', label: 'High' },
    3: { type: 'warning', label: 'Medium' },
    2: { type: 'info', label: 'Low' },
    1: { type: 'success', label: 'Info' }
  }
  return map[level] || { type: 'info', label: '-' }
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await getMrReviewPage(queryParams)
    if (res.code === 1) {
      tableData.value = res.data.rows || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res.msg || '查詢失敗')
    }
  } catch {
    ElMessage.error('查詢 AI 審核紀錄失敗')
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  try {
    const res = await getMrReviewDetail(row.projectName, row.mrIid)
    currentReview.value = (res.code === 1 && res.data) ? res.data : row
  } catch {
    currentReview.value = row
  }
  drawerVisible.value = true
}

const handleScan = async (projectName) => {
  scanning.value = true
  try {
    const res = projectName ? await scanMrReviewProject(projectName) : await scanMrReviewAll()
    if (res.code === 1) {
      ElMessage.success(res.data?.message || '掃描已送出')
      fetchData()
    } else {
      ElMessage.error(res.msg || '掃描失敗')
    }
  } catch {
    ElMessage.error('掃描請求失敗')
  } finally {
    scanning.value = false
  }
}

const renderText = (text) => {
  if (!text) return '<p class="empty-note">尚無內容</p>'
  return text
    .replace(/^### (.*$)/gim, '<h4>$1</h4>')
    .replace(/^## (.*$)/gim, '<h3>$1</h3>')
    .replace(/\*\*(.*?)\*\*/gim, '<strong>$1</strong>')
    .replace(/`([^`]+)`/gim, '<code>$1</code>')
    .replace(/^\- (.*$)/gim, '<li>$1</li>')
    .replace(/\n/gim, '<br>')
}

onMounted(async () => {
  const res = await getProjectList()
  if (res.code === 1) {
    projectOptions.value = (res.data || []).map(p => ({ label: p.displayName || p.name, value: p.name }))
  }
  fetchData()
})
</script>

<template>
  <div class="mr-review-page">
    <div class="header-section">
      <div class="title-box">
        <el-icon class="page-icon"><MagicStick /></el-icon>
        <h2>MR AI Code Review</h2>
      </div>
      <div class="action-box">
        <el-select v-model="queryParams.projectName" placeholder="專案" clearable style="width: 140px">
          <el-option v-for="p in projectOptions" :key="p.value" :label="p.label" :value="p.value" />
        </el-select>
        <el-select v-model="queryParams.reviewStatus" placeholder="審核狀態" clearable style="width: 130px">
          <el-option label="審核中" value="PENDING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="失敗" value="FAILED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="fetchData">查詢</el-button>
        <el-button type="success" :icon="Refresh" :loading="scanning" @click="handleScan(queryParams.projectName)">
          掃描專案
        </el-button>
        <el-button :loading="scanning" @click="handleScan()">掃描全部</el-button>
      </div>
    </div>

    <div class="table-container">
    <el-table :data="tableData" v-loading="loading" border stripe table-layout="auto">
      <el-table-column prop="mrIid" label="MR" width="80" align="center">
        <template #default="{ row }">!{{ row.mrIid }}</template>
      </el-table-column>
      <el-table-column prop="projectName" label="專案" width="110" />
      <el-table-column prop="title" label="標題" min-width="220" show-overflow-tooltip />
      <el-table-column prop="authorName" label="作者" width="100" />
      <el-table-column prop="reviewStatus" label="狀態" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.reviewStatus).type" size="small">
            {{ statusTag(row.reviewStatus).label }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="severity" label="嚴重度" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.severity" :type="severityTag(row.severity).type" size="small">
            {{ severityTag(row.severity).label }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="summary" label="摘要" min-width="200" show-overflow-tooltip />
      <el-table-column prop="reviewedAt" label="審核時間" width="170" />
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" :icon="View" @click="openDetail(row)">詳情</el-button>
        </template>
      </el-table-column>
    </el-table>
    </div>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="queryParams.page"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[5, 10, 20]"
        @size-change="fetchData"
        @current-change="fetchData"
      />
    </div>
  </div>

  <el-drawer v-model="drawerVisible" title="AI Code Review 詳情" size="50%" class="mr-review-drawer">
    <div class="detail-block" v-if="currentReview">
      <p><strong>專案：</strong>{{ currentReview.projectName }} · !{{ currentReview.mrIid }}</p>
      <p><strong>標題：</strong>{{ currentReview.title }}</p>
      <p>
        <strong>狀態：</strong>
        <el-tag :type="statusTag(currentReview.reviewStatus).type" size="small">
          {{ statusTag(currentReview.reviewStatus).label }}
        </el-tag>
      </p>
      <el-divider />
      <h4>摘要</h4>
      <p>{{ currentReview.summary || '（尚無）' }}</p>
      <h4>建議</h4>
      <div class="markdown-body" v-html="renderText(currentReview.suggestions)" />
      <h4 v-if="currentReview.fullReview">完整審核</h4>
      <div v-if="currentReview.fullReview" class="markdown-body" v-html="renderText(currentReview.fullReview)" />
      <el-alert
        v-if="currentReview.reviewStatus === 'FAILED' && currentReview.errorMessage"
        type="error"
        :title="currentReview.errorMessage"
        show-icon
        :closable="false"
        style="margin-top: 16px"
      />
    </div>
  </el-drawer>
</template>

<style scoped>
.mr-review-page {
  padding: 8px 4px;
}
.header-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}
.title-box {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-box h2 {
  margin: 0;
  font-size: 1.25rem;
  color: var(--text);
}
.page-icon {
  font-size: 22px;
  color: var(--brand);
}
.action-box {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.table-container {
  border-radius: 15px;
  overflow-x: auto;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
}

:deep(.table-container .el-table) {
  font-size: 13px;
  line-height: 1.6;
  min-width: 1100px;
  --el-table-bg-color: var(--table-bg-color) !important;
  --el-table-tr-bg-color: var(--table-bg-color) !important;
  --el-table-header-bg-color: var(--table-header-bg) !important;
  --el-table-border-color: var(--table-border-color) !important;
  --el-table-text-color: var(--table-text-color) !important;
  --el-table-header-text-color: var(--table-header-text-color) !important;
  --el-table-row-hover-bg-color: var(--table-hover-bg) !important;
  background-color: transparent !important;
}

:deep(.el-table th.el-table__cell),
:deep(.el-table td.el-table__cell) {
  background-color: var(--table-bg-color) !important;
  border-bottom: 1px solid var(--table-border-color) !important;
  padding: 10px 12px !important;
}

:deep(.el-table__inner-wrapper) {
  background-color: transparent !important;
}

:deep(.el-table::before) {
  height: 0 !important;
}

:deep(.action-box .el-select .el-input__wrapper) {
  background-color: var(--panel-alt) !important;
  border: 1px solid var(--border-color) !important;
  box-shadow: none !important;
}

:deep(.action-box .el-input__inner) {
  color: var(--text) !important;
}

:deep(.el-button--primary) {
  background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
  border: none;
  color: #fff;
}

:deep(.el-button--success) {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
  border: none;
  color: #fff;
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.pagination-wrap .el-pagination) {
  --el-text-color-regular: var(--muted);
  --el-pagination-bg-color: transparent !important;
}

:deep(.pagination-wrap .btn-prev),
:deep(.pagination-wrap .btn-next),
:deep(.pagination-wrap .el-pager li) {
  background-color: var(--panel-alt) !important;
  border: 1px solid var(--border-color) !important;
  color: var(--text) !important;
}

:deep(.pagination-wrap .el-pager li.is-active) {
  background: linear-gradient(45deg, #6366f1 0%, #8b5cf6 100%) !important;
  color: #fff !important;
}

.detail-block {
  color: var(--text);
}

.detail-block p {
  color: var(--text);
  line-height: 1.6;
}

.detail-block h4 {
  margin: 16px 0 8px;
  font-size: 14px;
  color: var(--muted);
}

.markdown-body {
  line-height: 1.6;
  font-size: 14px;
  color: var(--text);
}

.markdown-body code {
  background: rgba(255, 255, 255, 0.08);
  padding: 2px 6px;
  border-radius: 4px;
  color: #fbbf24;
}

.empty-note {
  color: var(--muted);
}
</style>

<style>
.el-drawer.mr-review-drawer {
  background: var(--panel) !important;
}

.el-drawer.mr-review-drawer .el-drawer__header {
  color: var(--text);
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 0;
  padding-bottom: 16px;
}

.el-drawer.mr-review-drawer .el-drawer__body {
  color: var(--text);
}
</style>
