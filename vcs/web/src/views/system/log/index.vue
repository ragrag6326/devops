<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { Search, View, WarningFilled } from '@element-plus/icons-vue'
import { getLogAnalysisPage } from '@/api/log_analysis'
import MarkdownIt from 'markdown-it' // 建議安裝 markdown-it 以完美呈現代碼塊

// --- Markdown 初始化 ---
const md = new MarkdownIt({ html: true, breaks: true })

// --- 數據定義 ---
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const queryParams = reactive({
  page: 1,
  pageSize: 10,
  serverName: ''
})

// --- 詳情抽屜控制 ---
const drawerVisible = ref(false)
const currentLog = ref({})

// --- 獲取數據 ---
const fetchData = async () => {
  loading.value = true
  try {
    const res = await getLogAnalysisPage(queryParams)
    if (res.code === 1) {
      tableData.value = res.data.rows
      total.value = res.data.total
    }
  } finally {
    loading.value = false
  }
}

// --- 處理嚴重度顏色 ---
const getSeverityTag = (level) => {
  const map = {
    5: { type: 'danger', label: 'Critical' },
    4: { type: 'danger', label: 'High' },
    3: { type: 'warning', label: 'Medium' },
    2: { type: 'info', label: 'Low' },
    1: { type: 'success', label: 'Info' }
  }
  return map[level] || { type: 'info', label: 'Unknown' }
}

// --- 打開詳情 ---
const openDetail = (row) => {
  currentLog.value = row
  drawerVisible.value = true
}

// --- Markdown 渲染函數 ---
const renderMarkdown = (text) => {
  if (!text) return '無內容'
  // 如果沒有安裝 markdown-it，可以使用簡單的 replace 處理換行
  // return text.replace(/\n/g, '<br>') 
  return md.render(text)
}

// --- 分頁處理 ---
const handleSizeChange = (val) => {
  queryParams.pageSize = val
  fetchData()
}
const handlePageChange = (val) => {
  queryParams.page = val
  fetchData()
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <div class="log-container">
    <div class="header-section">
      <div class="title-box">
        <el-icon class="page-icon"><magic-stick /></el-icon>
        <h2>AIOps 診斷中心</h2>
      </div>
      <div class="search-box">
        <el-input 
          v-model="queryParams.serverName" 
          placeholder="搜尋服務名稱 (Service Name)" 
          class="glass-input"
          clearable 
          @clear="fetchData"
          @keyup.enter="fetchData"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="fetchData" class="glass-btn">搜尋</el-button>
      </div>
    </div>

    <div class="table-wrapper">
      <el-table :data="tableData" v-loading="loading" style="width: 100%" row-key="id">
        <el-table-column label="嚴重度" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getSeverityTag(scope.row.severity).type" effect="dark" size="small">
              {{ getSeverityTag(scope.row.severity).label }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="logTime" label="發生時間" width="170" sortable />
        
        <el-table-column prop="serviceName" label="服務名稱" width="150">
          <template #default="scope">
            <span class="service-tag"># {{ scope.row.serviceName }}</span>
          </template>
        </el-table-column>

        <el-table-column label="錯誤摘要" min-width="160">
          <template #default="scope">
            <div class="error-cell">
              <div class="logger-class">{{ scope.row.loggerClass }}</div>
              <div class="error-msg">{{ scope.row.errorReason }}</div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="aiSummary" label="AI 簡評" min-width="250" show-overflow-tooltip>
          <template #default="scope">
            <span class="ai-summary-text">🤖 {{ scope.row.aiSummary }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="120" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">
              <el-icon><View /></el-icon> 診斷報告
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-box">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          class="custom-pagination"
        />
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      title="AI 分析報告"
      direction="rtl"
      size="50%"
      destroy-on-close
      class="glass-drawer"
    >
      <div class="drawer-content" v-if="currentLog.id">
        
        <div class="info-card">
          <div class="row">
            <span class="label">服務：</span>
            <span class="val">{{ currentLog.serviceName }}</span>
          </div>
          <div class="row">
            <span class="label">時間：</span>
            <span class="val">{{ currentLog.logTime }}</span>
          </div>
          <div class="row">
            <span class="label">類別：</span>
            <span class="val code-font">{{ currentLog.loggerClass }}</span>
          </div>
        </div>

        <div class="analysis-section">
          <h3><el-icon><WarningFilled /></el-icon> 根本原因分析 (Root Cause)</h3>
          <div class="glass-box root-cause">
            {{ currentLog.aiRootCause }}
          </div>
        </div>

        <div class="analysis-section">
          <h3><el-icon><magic-stick /></el-icon> 建議解決方案 (AI Solution)</h3>
          <div class="glass-box solution-box markdown-body" v-html="renderMarkdown(currentLog.aiSolution)"></div>
        </div>

        <div class="meta-footer">
          分析模型: {{ currentLog.aiProvider }} / {{ currentLog.aiModel }}
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
/* 頁面容器 */
.log-container { padding: 20px; color: #e2e8f0; }

/* 頂部 Header */
.header-section {
  display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px;
}
.title-box { display: flex; align-items: center; gap: 10px; }
.title-box h2 { font-size: 22px; background: linear-gradient(to right, #60a5fa, #a78bfa); -webkit-background-clip: text; -webkit-text-fill-color: transparent; }
.page-icon { font-size: 24px; color: #a78bfa; }

/* 搜索框 */
.search-box { display: flex; gap: 10px; }
.glass-input { width: 300px; --el-input-bg-color: rgba(255,255,255,0.05); --el-input-border-color: rgba(255,255,255,0.1); }
.glass-btn { background: linear-gradient(135deg, #6366f1, #8b5cf6); border: none; }

/* 表格樣式優化 */
.table-wrapper {
  background: var(--glass-bg); 
  border: 1px solid var(--glass-border);
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.2);
}

.service-tag { color: #94a3b8; font-family: monospace; background: rgba(255,255,255,0.05); padding: 2px 6px; border-radius: 4px; }
.error-cell { display: flex; flex-direction: column; gap: 4px; }
.logger-class { font-size: 12px; color: #64748b; }
.error-msg { color: #f87171; font-weight: 500; }
.ai-summary-text { color: #38bdf8; font-style: italic; }


/* 抽屜樣式 */
.drawer-content { padding: 10px; display: flex; flex-direction: column; gap: 24px; }

.info-card {
  background: rgba(255,255,255,0.03); border-radius: 8px; padding: 15px; border: 1px solid rgba(255,255,255,0.05);
}
.info-card .row { display: flex; margin-bottom: 8px; font-size: 14px; }
.info-card .label { width: 60px; color: #94a3b8; }
.info-card .val { color: #e2e8f0; }
.code-font { font-family: monospace; color: #fbbf24; }

.analysis-section h3 { margin-bottom: 12px; font-size: 16px; display: flex; align-items: center; gap: 8px; color: #e2e8f0; }

.glass-box {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 12px;
  padding: 20px;
  line-height: 1.6;
  font-size: 14px;
}

.root-cause { border-left: 4px solid #f87171; color: #fca5a5; }
.solution-box { border-left: 4px solid #4ade80; color: #cbd5e1; }



/* =============================== */
/* 分頁組件樣式 (Pagination) */
/* =============================== */

/* 分頁 */
/* .pagination-box { margin-top: 20px; display: flex; justify-content: flex-end; } */

.pagination-box {
    padding: 16px 0;
    display: flex;
    justify-content: center;
}

/* 核心：設定分頁組件的文字和背景基調 */
:deep(.custom-pagination) {
    /* 總體文字顏色 */
    --el-text-color-regular: #cbd5e1; /* slate-300 */
    /* 分頁背景透明 (讓它透出主內容區的背景) */
    --el-pagination-bg-color: transparent !important;
    padding: 15px 0; /* 增加上下間距 */

}

/* 針對 Prev/Next 按鈕和頁碼數字的背景/邊框/文字調整 */
:deep(.custom-pagination .btn-prev),
:deep(.custom-pagination .btn-next),
:deep(.custom-pagination .el-pager li),
:deep(.custom-pagination .el-select .el-input__wrapper) /* 調整 Size 選擇器的外觀 */
{
    /* 讓按鈕背景為極深藍 (slate-900)，比卡片背景 #1e293b 更深，以增加層次感 */
    background-color: #0f172a !important; 
    
    /* 邊框使用極淡的白色，達到科技感邊緣效果 */
    border: 1px solid rgba(255, 255, 255, 0.15); 
    color: #cbd5e1 !important;
    border-radius: 6px; /* 圓角調整 */
    transition: all 0.3s;
}

/* 活躍/選中頁碼的樣式 (核心高亮) */
:deep(.custom-pagination .el-pager li.is-active) {
    /* 漸層紫色高亮 */
    background: linear-gradient(45deg, #6366f1 0%, #8b5cf6 100%) !important; 
    border-color: #8b5cf6 !important; /* 邊框顏色與漸層呼應 */
    color: white !important;
    font-weight: bold;
    transform: scale(1.05); /* 輕微放大效果 */
} 

/* 頁碼懸停 (Hover) 效果 */
:deep(.custom-pagination .el-pager li:hover:not(.is-active)) {
    background-color: #1e293b !important; /* slate-800 hover */
    border-color: #681656; /* 懸停時邊框變成主題紫色 */
    color: #8b5cf6 !important;
}

/* 調整總數文字的顏色 */
:deep(.custom-pagination .el-pagination__total) {
    color: #94a3b8; /* slate-400 */
}

/* 調整跳頁輸入框的邊框 */
:deep(.custom-pagination .el-input.el-pagination__editor.is-in-pagination .el-input__wrapper) {
    background-color: #0f172a !important; 
    border: 1px solid rgba(255, 255, 255, 0.15) !important;
    box-shadow: none !important;
}



/* Markdown 樣式微調 (若有 markdown-it) */
:deep(.markdown-body pre) { background: #1e293b; padding: 10px; border-radius: 6px; overflow-x: auto; }
:deep(.markdown-body code) { color: #f472b6; background: rgba(255,255,255,0.1); padding: 2px 4px; border-radius: 4px; }

.meta-footer { text-align: right; font-size: 12px; color: #475569; margin-top: 20px; }

/* 深度覆蓋 Element 表格透明度 (配合您的全局主題) */
:deep(.el-table) { --el-table-bg-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: rgba(255,255,255,0.03); --el-table-text-color: #cbd5e1; --el-table-border-color: rgba(255,255,255,0.05); }
:deep(.el-table__row:hover) { background-color: rgba(255,255,255,0.05) !important; }
</style>

<style>
/* 針對自定義 class "glass-drawer" 的樣式覆蓋 */
.glass-drawer.el-drawer {
    /* --- 核心修復：強制覆蓋 Element Plus 的背景變數 --- */
    --el-drawer-bg-color: rgba(15, 23, 42, 0.95) !important;
    
    /* 1. 背景與毛玻璃 */
    background: var(--el-drawer-bg-color) !important;
    backdrop-filter: blur(12px) !important;
    
    /* 2. 邊框與陰影 */
    border-left: 1px solid rgba(255, 255, 255, 0.1) !important;
    box-shadow: -5px 0 30px rgba(0, 0, 0, 0.5) !important;
}

/* 3. 修正標題顏色 */
.glass-drawer .el-drawer__header {
    margin-bottom: 0 !important; /* 修正預設邊距 */
    padding: 20px !important;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.glass-drawer .el-drawer__title {
    color: #e2e8f0 !important; /* 亮灰白文字 */
    font-size: 18px !important;
    font-weight: 600 !important;
}

/* 4. 修正關閉按鈕顏色 */
.glass-drawer .el-drawer__close-btn {
    color: #94a3b8 !important;
    font-size: 18px !important;
}
.glass-drawer .el-drawer__close-btn:hover {
    color: #6366f1 !important; /* Hover 時變亮紫色 */
}

/* 5. 修正內容區域文字顏色 */
.glass-drawer .el-drawer__body {
    color: #cbd5e1 !important; /* 內容文字淺灰 */
    padding: 20px !important;
    /* 確保卷軸在深色模式下好看一點 (Webkit瀏覽器) */
    scrollbar-width: thin;
    scrollbar-color: rgba(255,255,255,0.2) transparent;
}
</style>