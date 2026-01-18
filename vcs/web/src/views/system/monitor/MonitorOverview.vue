<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Refresh, Timer, List, Search, Delete } from '@element-plus/icons-vue' // 新增 icon
import { healthCheck, getCurrentTraffic, getAudLogPage } from '@/api/monitor'
import { ElMessage } from 'element-plus'

const router = useRouter()

// --- 專案與卡片狀態 ---
const projectNames = ['tv', 'go-api', 'go-nuxt', 'player' ,'test']
const projects = ref([])
const dashboardLoading = ref(false) // 專給卡片區塊用的 loading

// --- 輪詢設定 ---
const pollInterval = ref(30000)
let pollTimer = null

// --- 日誌與分頁數據 ---
const recentAudLogs = ref([])
const tableLoading = ref(false) // 專給表格用的 loading
const totalPage = ref(0)
const currentPage = ref(1)
const pageSize = ref(10) // 建議預設 10 筆較為適當
const background = ref(true)

// --- 搜尋表單 ---
const searchForm = ref({
    name: "",
    status: "", // 對應 el-select 的 value
    date: [],
    start: "",
    end: ""
})

const statusOptions = [
    { label: "全部", value: "" }, // 建議加一個空值選項
    { label: "成功", value: 0 },
    { label: "失敗", value: 1 },
]

// 監聽日期變動，拆解成 start/end
watch(() => searchForm.value.date, (val) => {
    if (val && val.length === 2) {
        searchForm.value.start = val[0]
        searchForm.value.end = val[1]
    } else {
        searchForm.value.start = ""
        searchForm.value.end = ""
    }
})

// --- 1. 核心邏輯分離：只抓取 Dashboard 卡片狀態 ---
const fetchDashboardStatus = async () => {
    // 如果是自動輪詢，通常不顯示 loading 遮罩以免畫面閃爍；手動刷新時才顯示
    // 這裡設為 true 會讓卡片區塊變灰
    // dashboardLoading.value = true 
    
    try {
        const projectPromises = projectNames.map(async (name) => {
            // 使用 allSettled 避免其中一個 API 掛掉導致全部畫面空白
            const results = await Promise.allSettled([
                healthCheck(name, 'blue'),
                healthCheck(name, 'green'),
                getCurrentTraffic(name, 'live')
            ])

            // 解析結果，若失敗給予預設值
            const blueRes = results[0].status === 'fulfilled' ? results[0].value : { data: 500 }
            const greenRes = results[1].status === 'fulfilled' ? results[1].value : { data: 500 }
            const trafficRes = results[2].status === 'fulfilled' ? results[2].value : { data: 'UNKNOWN' }

            return {
                name: name,
                blueStatus: blueRes.data,
                greenStatus: greenRes.data,
                activeTraffic: trafficRes.data
            }
        })
        projects.value = await Promise.all(projectPromises)
    } catch (error) {
        console.error('監控數據抓取失敗:', error)
    } finally {
        dashboardLoading.value = false
    }
}

// --- 2. 核心邏輯分離：只抓取日誌列表 ---
const fetchLogs = async () => {
    tableLoading.value = true
    try {
        // 若 API 支援 projectName 參數，傳入 searchForm.name，否則傳 null
        // 注意：這裡假設 getAudLogPage 的參數順序，請依照您實際 API 調整
        const res = await getAudLogPage(
            currentPage.value,
            pageSize.value,
            searchForm.value.name,   // 專案名稱
            searchForm.value.status, // 狀態
            searchForm.value.start,  // 開始時間
            searchForm.value.end     // 結束時間
        )
        
        // 假設後端回傳結構為 { rows: [], total: 100 }
        recentAudLogs.value = res.data.rows
        totalPage.value = res.data.total 
    } catch (error) {
        console.error('日誌抓取失敗:', error)
        recentAudLogs.value = []
        totalPage.value = 0
    } finally {
        tableLoading.value = false
    }
}

// --- 搜尋功能 ---
const handleSearch = () => {
    currentPage.value = 1 // 搜尋時重置回第一頁
    fetchLogs()
}

// --- 清空重置 ---
const handleReset = () => {
    searchForm.value = {
        name: "",
        status: "",
        date: [],
        start: "",
        end: ""
    }
    handleSearch()
}

// --- 分頁控制 ---
const handleSizeChange = (val) => {
    pageSize.value = val
    currentPage.value = 1 // 改變筆數通常重回第一頁
    fetchLogs()
}

const handleCurrentChange = (val) => {
    currentPage.value = val
    fetchLogs()
}

// --- 路由跳轉 ---
const goToDetail = (name) => {
    router.push(`/system/monitor/${name}`)
}

// --- 輪詢控制 (只輪詢 Dashboard 狀態，不輪詢日誌) ---
const stopPolling = () => {
    if (pollTimer) {
        clearInterval(pollTimer)
        pollTimer = null
    }
}

const startPolling = () => {
    stopPolling()
    if (pollInterval.value > 0) {
        pollTimer = setInterval(fetchDashboardStatus, pollInterval.value)
    }
}

// 手動刷新按鈕 (同時刷新兩者)
const handleManualRefresh = () => {
    dashboardLoading.value = true
    fetchDashboardStatus()
    fetchLogs()
}

watch(pollInterval, (newVal) => {
    startPolling()
    if (newVal === 0) {
        ElMessage.info('已關閉自動刷新')
    } else {
        ElMessage.success(`已設定為每 ${newVal / 1000} 秒自動刷新`)
    }
})

// --- 生命週期 ---
onMounted(() => {
    fetchDashboardStatus() // 抓卡片
    fetchLogs()            // 抓日誌
    startPolling()         // 啟動卡片輪詢
})

onUnmounted(() => {
    stopPolling()
})
</script>

<template>
  <div class="overview-container">
    <div class="page-header">
      <div class="header-left">
        <h2 class="title">服務監控中心</h2>
      </div>
      
      <div class="header-right">
        <div class="poll-select">
            <el-icon class="poll-icon"><Timer /></el-icon>
            <el-select v-model="pollInterval" size="small" style="width: 110px">
                <el-option :value="0" label="不刷新" />
                <el-option :value="30000" label="30 秒" />
                <el-option :value="60000" label="1 分鐘" />
            </el-select>
        </div>
        <el-button type="primary" plain size="small" :icon="Refresh" @click="handleManualRefresh">
            手動刷新
        </el-button>
      </div>
    </div>

    <div class="project-grid" v-loading="dashboardLoading" element-loading-background="rgba(0, 0, 0, 0.3)">
      <div 
        v-for="proj in projects" 
        :key="proj.name" 
        class="glass-card project-card"
        :class="{ 'is-error': proj.blueStatus !== 200 || proj.greenStatus !== 200 }"
        @click="goToDetail(proj.name)"
      >
        <div class="card-header">
          <span class="proj-name">{{ proj.name }}</span>
          <div class="status-group">
            <div class="node-dot" :class="proj.blueStatus === 200 ? 'online' : 'offline'">B</div>
            <div class="node-dot" :class="proj.greenStatus === 200 ? 'online' : 'offline'">G</div>
          </div>
        </div>
        
        <div class="card-body">
          <div class="info-row">
            <span class="label">當前流量指向</span>
            <el-tag :type="proj.activeTraffic === 'BLUE_ACTIVE' ? 'primary' : 'success'" size="small" effect="dark">
              {{ proj.activeTraffic === 'BLUE_ACTIVE' ? 'BLUE' : (proj.activeTraffic === 'GREEN_ACTIVE' ? 'GREEN' : 'UNKNOWN') }}
            </el-tag>
          </div>
          <div class="info-row">
            <span class="label">節點狀態摘要</span>
            <span class="summary">
                <span :class="proj.blueStatus === 200 ? 'text-ok' : 'text-err'">B</span> / 
                <span :class="proj.greenStatus === 200 ? 'text-ok' : 'text-err'">G</span>
            </span>
          </div>
        </div>

        <div class="card-footer">
          <span>進入控制面板</span>
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>
    </div>

    <div class="log-section">
        <div class="section-title">
            <el-icon><List /></el-icon> 最近流量切換日誌
        </div>

        <div class="search-bar glass-panel">
            <el-form :inline="true" :model="searchForm" class="search-form-inline">
                <el-form-item label="專案名稱">
                    <el-select v-model="searchForm.name" placeholder="全部專案" clearable style="width:140px">
                        <el-option label="全部" value="" />
                        <el-option v-for="name in projectNames" :key="name" :label="name" :value="name" />
                    </el-select>
                </el-form-item>

                <el-form-item label="狀態">
                    <el-select v-model="searchForm.status" placeholder="全部狀態" clearable style="width:120px">
                        <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                </el-form-item>

                <el-form-item label="操作日期">
                    <el-date-picker
                        v-model="searchForm.date"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="開始日期"
                        end-placeholder="結束日期"
                        value-format="YYYY-MM-DD"
                        style="width: 260px"
                    />
                </el-form-item>
                
                <el-form-item>
                    <el-button type="primary" :icon="Search" @click="handleSearch">查詢</el-button>
                    <el-button :icon="Delete" @click="handleReset">重置</el-button>
                </el-form-item>
            </el-form>
        </div>

        <div class="table-container" v-loading="tableLoading">
            <el-table :data="recentAudLogs" size="large" style="width: 100%">
                <el-table-column prop="projectName" label="專案名稱" width="150" />
                <el-table-column prop="action" label="切換動作" width="600" >
                    <template #default="scope">
                        <code class="code-text">{{ scope.row.action }}</code>
                    </template>
                </el-table-column>
                <el-table-column prop="operator" label="操作者" width="150" />
                <el-table-column prop="status" label="狀態" width="120" align="center">
                    <template #default="scope">
                        <el-tag v-if="scope.row.status === 0" type="success" effect="dark">成功</el-tag>
                        <el-tag v-else-if="scope.row.status === 1" type="danger" effect="dark">失敗</el-tag>
                        <el-tag v-else type="info">未知</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="operationTime" label="操作時間" min-width="180" />
            </el-table>
        </div>

        <div class="page-container">
            <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50]"
                :background="background"
                layout="total, sizes, prev, pager, next, jumper"
                :total="totalPage"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
                class="custom-pagination"
            />
        </div>
    </div>
  </div>
</template>

<style scoped>
/* 樣式部分保持不變，直接沿用您原本的 CSS */
.overview-container { padding: 10px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-right { display: flex; align-items: center; gap: 15px; }
.poll-select { display: flex; align-items: center; gap: 8px; color: var(--text-sub); font-size: 13px; }
.poll-icon { font-size: 16px; color: var(--primary-color); }
.title { 
    font-size: 22px; 
    font-weight: 600; 
    background: linear-gradient(to right, #fff, #94a3b8); 
    -webkit-background-clip: text; 
    -webkit-text-fill-color: transparent; 
}

/* --- 強化版玻璃卡片外框 --- */
.glass-card {
    background: rgba(30, 41, 59, 0.6); 
    backdrop-filter: blur(12px);
    border-radius: 20px;
    padding: 24px;
    cursor: pointer;
    transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    border: 1.5px solid rgba(255, 255, 255, 0.12);
    box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.1), 0 8px 32px 0 rgba(0, 0, 0, 0.3);
}

.glass-card:hover, .glass-card.is-active {
    transform: translateY(-5px);
    background: rgba(30, 41, 59, 0.8);
    border-color: rgba(99, 102, 241, 0.6); 
    box-shadow: 0 0 20px rgba(99, 102, 241, 0.3), inset 0 0 12px rgba(99, 102, 241, 0.15);
}

/* --- 🚨 異常狀態 (Is Error) --- */
.glass-card.is-error {
    /* 1. 背景帶有淡淡的危險紅 */
    background: rgba(69, 10, 10, 0.6) !important; 
    
    /* 2. 邊框變成警示紅 */
    border-color: #ef4444 !important; /* Tailwind Red-500 */
    
    /* 3. 初始陰影：紅色光暈 */
    box-shadow: 0 0 15px rgba(239, 68, 68, 0.3), inset 0 0 10px rgba(239, 68, 68, 0.1);
    
    /* 4. 加入呼吸燈動畫 */
    animation: error-breathe 2s infinite ease-in-out;
}

/* 異常狀態下的 Hover 效果 */
.glass-card.is-error:hover {
    transform: translateY(-5px);
    background: rgba(69, 10, 10, 0.8) !important;
    box-shadow: 0 0 30px rgba(239, 68, 68, 0.5), inset 0 0 20px rgba(239, 68, 68, 0.2);
}

/* --- 呼吸燈動畫 Keyframes --- */
@keyframes error-breathe {
    0% {
        box-shadow: 0 0 10px rgba(239, 68, 68, 0.2), inset 0 0 5px rgba(239, 68, 68, 0.1);
        border-color: rgba(239, 68, 68, 0.5);
    }
    50% {
        box-shadow: 0 0 25px rgba(239, 68, 68, 0.6), inset 0 0 15px rgba(239, 68, 68, 0.2);
        border-color: rgba(239, 68, 68, 1);
    }
    100% {
        box-shadow: 0 0 10px rgba(239, 68, 68, 0.2), inset 0 0 5px rgba(239, 68, 68, 0.1);
        border-color: rgba(239, 68, 68, 0.5);
    }
}


.project-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.proj-name { font-weight: 700; font-size: 18px; color: #fff; }
.status-group { display: flex; gap: 6px; }

.node-dot {
    width: 20px; height: 20px; border-radius: 4px; font-size: 10px;
    display: flex; align-items: center; justify-content: center; font-weight: bold;
}
.node-dot.online { background: #4ade80; color: #064e3b; box-shadow: 0 0 8px rgba(74, 222, 128, 0.4); }
.node-dot.offline { background: #f87171; color: #450a0a; animation: flash 2s infinite; }

.info-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; }
.label { color: var(--text-sub); }
.text-ok { color: #4ade80; }
.text-err { color: #f87171; }

.card-footer {
    margin-top: 15px; padding-top: 12px; border-top: 1px solid rgba(255, 255, 255, 0.05);
    display: flex; justify-content: space-between; font-size: 12px; color: var(--text-sub);
}

.log-section { margin-top: 40px; }
.section-title { 
    font-size: 16px; font-weight: 600; color: #fff; 
    margin-bottom: 15px; display: flex; align-items: center; gap: 8px;
}
.table-container {
    background: var(--glass-bg);
    border: 1px solid var(--glass-border);
    border-radius: 12px;
    padding: 10px;
}
.code-text { background: rgba(0,0,0,0.3); padding: 2px 6px; border-radius: 4px; font-family: monospace; color: var(--primary-color); }

@keyframes flash { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

:deep(.el-table) {
    --el-table-bg-color: transparent !important;
    --el-table-tr-bg-color: transparent !important;
    --el-table-header-bg-color: rgba(255, 255, 255, 0.03) !important;
    --el-table-border-color: rgba(255, 255, 255, 0.08) !important;
    --el-table-text-color: #cbd5e1 !important;
    background-color: transparent !important;
}

.search-bar {
    margin-bottom: 20px;
    padding: 18px 18px 0 18px; /* 調整 padding 讓 form-item 不會太貼邊 */
}

/* 讓搜尋列也有玻璃擬態效果 */
.glass-panel {
    background: rgba(30, 41, 59, 0.4);
    backdrop-filter: blur(8px);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 12px;
}

/* 調整 Element Form 在深色模式下的細節 */
:deep(.el-form-item__label) {
    color: #cbd5e1;
}

/* 輸入框背景 */
:deep(.el-input__wrapper), 
:deep(.el-range-editor.el-input__wrapper) {
    background-color: rgba(15, 23, 42, 0.6) !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
    color: white;
}

/* 讓表格內的狀態標籤置中並好看一點 */
:deep(.el-tag--dark) {
    border: none;
}
</style>


<style scoped>
/* =============================== */
/* 分頁組件樣式 (Pagination) */
/* =============================== */

.page-container {
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

/* =============================== */
/* 按鈕樣式加強 (如果全局沒生效) */
/* =============================== */

/* 主按鈕 (新增/編輯) */
:deep(.el-button--primary) {
    background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
    border: none;
    box-shadow: 0 2px 6px rgba(99, 102, 241, 0.4);
    color: white;
}
:deep(.el-button--primary:hover) {
    opacity: 0.9;
    transform: translateY(-1px);
}

/* 危險按鈕 (刪除) */
:deep(.el-button--danger) {
    background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);
    border: none;
    box-shadow: 0 2px 6px rgba(239, 68, 68, 0.4);
}
:deep(.el-button--danger:hover) {
    opacity: 0.9;
    transform: translateY(-1px);
}

/* 表格內的小按鈕調整 */
:deep(.el-table .el-button) {
    padding: 6px 12px;
    height: auto;
    font-size: 12px;
}

</style>