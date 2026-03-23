<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Refresh, Timer, List, Search, Delete } from '@element-plus/icons-vue' // 新增 icon
import { healthCheck, getCurrentTraffic, getAudLogPage } from '@/api/monitor'
import { ElMessage } from 'element-plus'

const router = useRouter()

// --- 專案與卡片狀態 ---
const projectNames = ['tv', 'go-api', 'go_nuxt', 'player' ,'test']
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
.overview-container { padding: 10px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
.header-right { display: flex; align-items: center; gap: 15px; }
.poll-select { display: flex; align-items: center; gap: 8px; color: var(--muted); font-size: 13px; }
.poll-icon { font-size: 16px; color: var(--primary-color); }
.title {
    font-size: 22px;
    font-weight: 600;
    color: var(--text);
}

/* --- 玻璃卡片（自適應深淺主題）--- */
.glass-card {
    background: var(--panel);
    backdrop-filter: blur(12px);
    border-radius: 20px;
    padding: 24px;
    cursor: pointer;
    transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    border: 1.5px solid var(--border-color);
    box-shadow: var(--shadow-md);
}

.glass-card:hover, .glass-card.is-active {
    transform: translateY(-5px);
    background: var(--panel-alt);
    border-color: var(--brand);
    box-shadow: 0 0 20px var(--brand-muted), var(--shadow-md);
}

/* --- 異常狀態 --- */
.glass-card.is-error {
    background: color-mix(in srgb, var(--danger) 8%, var(--panel)) !important;
    border-color: var(--danger) !important;
    box-shadow: 0 0 15px color-mix(in srgb, var(--danger) 30%, transparent);
    animation: error-breathe 2s infinite ease-in-out;
}
.glass-card.is-error:hover {
    transform: translateY(-5px);
    box-shadow: 0 0 30px color-mix(in srgb, var(--danger) 50%, transparent);
}

@keyframes error-breathe {
    0%   { box-shadow: 0 0 10px color-mix(in srgb, var(--danger) 20%, transparent); border-color: color-mix(in srgb, var(--danger) 50%, transparent); }
    50%  { box-shadow: 0 0 25px color-mix(in srgb, var(--danger) 60%, transparent); border-color: var(--danger); }
    100% { box-shadow: 0 0 10px color-mix(in srgb, var(--danger) 20%, transparent); border-color: color-mix(in srgb, var(--danger) 50%, transparent); }
}

.project-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.proj-name { font-weight: 700; font-size: 18px; color: var(--text); }
.status-group { display: flex; gap: 6px; }

.node-dot {
    width: 20px; height: 20px; border-radius: 4px; font-size: 10px;
    display: flex; align-items: center; justify-content: center; font-weight: bold;
}
.node-dot.online { background: #4ade80; color: #064e3b; box-shadow: 0 0 8px rgba(74, 222, 128, 0.4); }
.node-dot.offline { background: #f87171; color: #450a0a; animation: flash 2s infinite; }

.info-row { display: flex; justify-content: space-between; margin-bottom: 10px; font-size: 14px; }
.label { color: var(--muted); }
.text-ok { color: #4ade80; }
.text-err { color: #f87171; }

.card-footer {
    margin-top: 15px; padding-top: 12px; border-top: 1px solid var(--border-color);
    display: flex; justify-content: space-between; font-size: 12px; color: var(--muted);
}

.log-section { margin-top: 40px; }
.section-title {
    font-size: 16px; font-weight: 600; color: var(--text);
    margin-bottom: 15px; display: flex; align-items: center; gap: 8px;
}
.table-container {
    background: var(--panel);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    padding: 10px;
}
.code-text { background: var(--panel-alt); padding: 2px 6px; border-radius: 4px; font-family: monospace; color: var(--brand); }

@keyframes flash { 0%, 100% { opacity: 1; } 50% { opacity: 0.5; } }

:deep(.el-table) {
    --el-table-bg-color: transparent !important;
    --el-table-tr-bg-color: transparent !important;
    --el-table-header-bg-color: var(--table-header-bg) !important;
    --el-table-border-color: var(--table-border-color) !important;
    --el-table-text-color: var(--table-text-color) !important;
    background-color: transparent !important;
}

.search-bar {
    margin-bottom: 20px;
    padding: 18px 18px 0 18px;
}

.glass-panel {
    background: var(--panel);
    border: 1px solid var(--border-color);
    border-radius: 12px;
    box-shadow: var(--shadow);
}

:deep(.el-form-item__label) {
    color: var(--text);
}

:deep(.el-tag--dark) {
    border: none;
}
</style>


<style scoped>
.page-container {
    padding: 16px 0;
    display: flex;
    justify-content: center;
}

:deep(.custom-pagination) {
    --el-text-color-regular: var(--muted);
    --el-pagination-bg-color: transparent !important;
    padding: 15px 0;
}

:deep(.custom-pagination .btn-prev),
:deep(.custom-pagination .btn-next),
:deep(.custom-pagination .el-pager li),
:deep(.custom-pagination .el-select .el-input__wrapper) {
    background-color: var(--panel) !important;
    border: 1px solid var(--border-color);
    color: var(--text) !important;
    border-radius: 6px;
    transition: all 0.3s;
}

:deep(.custom-pagination .el-pager li.is-active) {
    background: var(--brand) !important;
    border-color: var(--brand) !important;
    color: #000 !important;
    font-weight: bold;
    transform: scale(1.05);
}

:deep(.custom-pagination .el-pager li:hover:not(.is-active)) {
    background-color: var(--panel-alt) !important;
    border-color: var(--brand);
    color: var(--brand) !important;
}

:deep(.custom-pagination .el-pagination__total) {
    color: var(--muted);
}

:deep(.custom-pagination .el-input.el-pagination__editor.is-in-pagination .el-input__wrapper) {
    background-color: var(--panel) !important;
    border: 1px solid var(--border-color) !important;
    box-shadow: none !important;
}

:deep(.el-button--danger) {
    background: var(--danger) !important;
    border: none;
    box-shadow: 0 2px 6px color-mix(in srgb, var(--danger) 40%, transparent);
}
:deep(.el-button--danger:hover) {
    opacity: 0.9;
    transform: translateY(-1px);
}

:deep(.el-table .el-button) {
    padding: 6px 12px;
    height: auto;
    font-size: 12px;
}
</style>
