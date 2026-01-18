<script setup>
import { ref, computed } from 'vue'
import { Search, Key, MagicStick, Delete } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import axios from 'axios' 
import dayjs from 'dayjs' 

// --- 設定 ---
//  n8n Webhook URL
const N8N_WEBHOOK_URL = '/n8n-proxy/webhook/ec70ae29-f6a8-4320-884b-ab6ea96b0c2f'
const TOKEN = "Tm4NnPvnavNfvJOUP8uA6yJrWctC6fhKiY2q4sLbCKEbgyWzEH"
// --- 狀態變數 ---
const activeTab = ref('strict')
const loading = ref(false)
const logData = ref([])

// 專案名
const projectNames = ['tv', 'go-api', 'go-nuxt']

// Strict 模式表單
const strictForm = ref({
  projectName: '', 
  msg_body: '',
  dateRange: [] // [startDate, endDate]
})

// AI 模式輸入
const aiQueryText = ref('')
// 定義快捷選項
const quickOptions = {
  time: ['最近 10 分鐘', '最近 1 小時', '今天', '昨天'],
  project: ['TV', 'Go-API', 'Go-Nuxt'],
  status: ['發生 Error', '查詢 Timeout', '包含 Exception']
}

// 點擊標籤後，自動串接文字
const appendQuery = (text) => {
  if (aiQueryText.value) {
    aiQueryText.value += ` ${text}`
  } else {
    aiQueryText.value = text
  }
}

// 日期快捷鍵
const dateShortcuts = [
  { text: '今天', value: () => [new Date(), new Date()] },
  { text: '最近3天', value: () => { const end = new Date(); const start = new Date(); start.setTime(start.getTime() - 3600 * 1000 * 24 * 3); return [start, end] } }
]

// --- 核心查詢邏輯 ---
const handleSearch = async () => {
  loading.value = true
  logData.value = [] // 清空舊資料
  
  let payload = {}

  try {
    // 1. 根據 Tab 決定 Payload 結構
    if (activeTab.value === 'strict') {
      // 檢核
      if ( !strictForm.value.projectName && !strictForm.value.msg_body && (!strictForm.value.dateRange || strictForm.value.dateRange.length === 0)) {
        ElMessage.warning('缺少專案名稱、關鍵字或選擇日期範圍')
        loading.value = false
        return
      }
      
      payload = {
        type: 'strict',
        project: strictForm.value.projectName,
        msg_body: strictForm.value.msg_body,
        startDate: strictForm.value.dateRange?.[0] || null,
        endDate: strictForm.value.dateRange?.[1] || null
      }
    } else {
      // AI 模式
      if (!aiQueryText.value.trim()) {
        ElMessage.warning('請輸入查詢描述')
        loading.value = false
        return
      }

      payload = {
        type: 'ai',
        project: strictForm.value.projectName,
        query: aiQueryText.value
      }
    }

    // 2. 發送請求給 n8n
    // 注意: n8n 應該設定為回傳 JSON，且包含 hits 陣列
    const response = await axios.post(
      N8N_WEBHOOK_URL, 
      payload , 
      {
        headers: { 
          'Authorization': `Bearer ${TOKEN}`
        }
      }
    );
    
    // 3. 處理回傳資料
    // 假設 n8n 回傳結構為: { data: [ {_id:..., _source:...}, ... ] } 或直接是 ES 的 hits.hits
    // 這裡做一個兼容處理，將 _source 攤平以便表格顯示
    const rawHits = response.data.hits?.hits || response.data || []
    
    logData.value = rawHits.map(hit => {
        // 如果 n8n 已經幫你攤平了就直接用，如果是原始 ES 結構則取 _source
        const source = hit._source || hit
        return {
            _id: hit._id,
            ...source
        }
    })

    if (logData.value.length === 0) {
      ElMessage.info('查無符合條件的日誌')
    } else {
      ElMessage.success(`查詢成功，共 ${logData.value.length} 筆`)
    }

  } catch (error) {
    console.error(error)
    ElMessage.error('查詢失敗，請檢查 n8n 連線或 ES 狀態')
  } finally {
    loading.value = false
  }
}

// --- 輔助函數 ---
const formatDate = (isoStr) => {
  if (!isoStr) return '-'
  return dayjs(isoStr).format('YYYY-MM-DD HH:mm:ss')
}

const getLevelType = (level) => {
  switch (level?.toUpperCase()) {
    case 'ERROR': return 'danger'
    case 'WARN': return 'warning'
    case 'INFO': return 'success'
    default: return 'info'
  }
}

// 根據 Log Level 改變行樣式 (可選)
const tableRowClassName = ({ row }) => {
  if (row.log_level === 'ERROR') return 'error-row'
  if (row.log_level === 'WARN') return 'warning-row'
  return ''
}

// 嘗試格式化 JSON
const formatJavaLog = (text) => {
  if (!text) return '';
  
  // 1. 如果它是標準 JSON，還是嘗試用 JSON 處理 (相容性)
  try {
    const obj = JSON.parse(text);
    return JSON.stringify(obj, null, 2);
  } catch (e) {
    // 忽略錯誤，繼續往下執行自定義排版
  }

  // 2. 處理 Java toString 格式 [{key=value}, {key=value}]
  let formatted = text

    // 1. 針對 "文字 :[" 的情況，強制在 : 後面換行，讓 [ 獨佔一行
    .replace(/:\s*\[/g, ':\n[')
    
    // 2. 處理陣列開頭 "[{" -> 變成 "[\n  {"
    // 注意：因為上面已經加了換行，這裡的 \s* 會匹配到那個換行符，不影響結果
    .replace(/\[\s*\{/g, '[\n  {')
    
    // 3. 處理物件分隔 "}, {" -> 變成 "},\n  {"
    .replace(/\}\s*,\s*\{/g, '\n  },\n  {')
    
    // 4. 處理陣列結尾 "}]" -> 補回 "}" 並換行
    .replace(/\}\]/g, '\n  }\n]')
    
    // 5. 處理屬性逗號 ", KEY=" -> 換行 + 縮排
    .replace(/, (\w+=)/g, ',\n    $1');

  return formatted;
}


</script>

<template>
  <div class="log-query-container">
    
    <div class="page-header">
      <h2><el-icon><Search /></el-icon> 日誌檢索中心 (ES)</h2>
      <!-- <el-tag effect="dark" round>n8n Gateway</el-tag> -->
    </div>

    <div class="query-panel glass-card">
      <el-tabs v-model="activeTab" class="custom-tabs">
        
        <el-tab-pane label="精準查詢 (Strict)" name="strict">
          <el-form :inline="true" :model="strictForm" class="search-form">

            <el-form-item label="專案名稱">
              <el-select v-model="strictForm.projectName" placeholder="請選擇專案" clearable style="width: 140px;">
                <el-option v-for="name in projectNames" :key="name" :label="name" :value="name"/>
              </el-select>
            </el-form-item>

            <el-form-item label="訂單/關鍵字">
              <el-input v-model="strictForm.msg_body" placeholder="例如: 身分證字號/手機號碼" clearable style="width: 220px;">
                <template #prefix><el-icon><Key /></el-icon></template>
              </el-input>
            </el-form-item>

            <el-form-item label="日期範圍">
              <el-date-picker
                v-model="strictForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="開始日期"
                end-placeholder="結束日期"
                value-format="YYYY-MM-DD"
                :shortcuts="dateShortcuts"
                style="width: 260px;"
              />
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="loading" @click="handleSearch">
                <el-icon><Search /></el-icon> 查詢
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="AI 幫幫我" name="ai">
          <div class="ai-container">
            
            <el-form :inline="true" :model="strictForm" class="search-form">
              <el-form-item label="專案名稱">
                <el-select v-model="strictForm.projectName" placeholder="請選擇專案" clearable style="width: 140px;">
                  <el-option v-for="name in projectNames" :key="name" :label="name" :value="name"/>
                </el-select>
              </el-form-item>
            </el-form>
            
            <el-input
              v-model="aiQueryText"
              type="textarea"
              :rows="3"
              placeholder="請輸入需求，或點擊下方標籤組合..."
              class="ai-textarea"
            />

            <div class="quick-tags">
              <div class="tag-group">
                <span class="tag-label">時間:</span>
                <el-tag 
                  v-for="item in quickOptions.time" :key="item" 
                  class="clickable-tag" type="info" effect="plain"
                  @click="appendQuery(item)"
                >
                  {{ item }}
                </el-tag>
              </div>
              
              <!-- <div class="tag-group">
                <span class="tag-label">專案:</span>
                <el-tag 
                  v-for="item in quickOptions.project" :key="item" 
                  class="clickable-tag" type="primary" effect="plain"
                  @click="appendQuery(item)"
                >
                  {{ item }}
                </el-tag>
              </div> -->

              <div class="tag-group">
                <span class="tag-label">狀態:</span>
                <el-tag 
                  v-for="item in quickOptions.status" :key="item" 
                  class="clickable-tag" type="danger" effect="plain"
                  @click="appendQuery(item)"
                >
                  {{ item }}
                </el-tag>
              </div>
            </div>

            <div class="action-bar">
              <el-button type="primary" color="#6366f1" :loading="loading" @click="handleSearch" class="ai-btn">
                <el-icon><MagicStick /></el-icon> AI 分析與查詢
              </el-button>
              <el-button link type="info" @click="aiQueryText = ''">清空</el-button>
            </div>

          </div>
        </el-tab-pane>

      </el-tabs>
    </div>

    <div class="result-section" v-loading="loading" element-loading-text="正在從 ElasticSearch 撈取數據...">
      
      <div class="result-header" v-if="logData.length > 0">
        <span>共找到 {{ logData.length }} 筆資料</span>
        <el-button size="small" circle @click="logData = []"><el-icon><Delete /></el-icon></el-button>
      </div>

      <el-table 
        v-if="logData.length > 0" 
        :data="logData" 
        style="width: 100%" 
        class="log-table"
        :row-class-name="tableRowClassName"
      >
        <el-table-column type="expand">
          <template #default="props">
            <div class="expand-content">
              <p><strong>Full ID:</strong> {{ props.row._id }}</p>
              <p><strong>Logger:</strong> {{ props.row.logger_class }}</p>
              <p><strong>Raw Message:</strong></p>
              <pre class="code-block">{{ formatJavaLog(props.row.msg_body) }}</pre>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="@timestamp" label="時間" width="180">
          <template #default="scope">
            {{ formatDate(scope.row['@timestamp']) }}
          </template>
        </el-table-column>

        <el-table-column prop="log_level" label="Level" width="100" align="center">
          <template #default="scope">
            <el-tag :type="getLevelType(scope.row.log_level)" effect="dark">
              {{ scope.row.log_level }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="env" label="環境" width="120" />
        
        <el-table-column prop="site" label="Site" width="100">
           <template #default="scope">
             <el-tag size="small" effect="dark" :type="scope.row.site === 'blue' ? 'primary' : (scope.row.site === 'green' ? 'success' : 'info')"> 
                {{ scope.row.site ? scope.row.site.toUpperCase() : '-' }}
             </el-tag>
           </template> 
        </el-table-column>

        <el-table-column prop="msg_body" label="日誌內容" min-width="300">
           <template #default="scope">
              <div class="msg-preview">{{ formatJavaLog(scope.row.msg_body) }}</div>
              <!-- <div class="code-block">{{ formatJson(scope.row.msg_body) }}</div> -->
           </template>
        </el-table-column>
      </el-table>

      <el-empty v-else description="暫無數據，請發起查詢" :image-size="150" />
    </div>

  </div>
</template>

<style scoped>
.log-query-container { padding: 20px; color: #fff; }
.page-header { display: flex; align-items: center; gap: 10px; margin-bottom: 20px; }
.glass-card {
  background: rgba(30, 41, 59, 0.6);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.ai-input-group { display: flex; gap: 10px; align-items: flex-start; }
.ai-textarea { flex: 1; }
.ai-btn { height: 52px; /* 配合 textarea 高度 */ }

.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; color: #94a3b8; font-size: 14px;}


/* --- 列表中的預覽區塊 (Preview) --- */
.msg-preview {
  font-family: monospace;
  font-size: 12px;
  color: #e2e8f0;
  
  /* 限制顯示行數 (例如只顯示 3 行)，超過顯示 ... */
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;      /* 想看更多行可改為 5 或 10 */
  overflow: hidden;
  
  /* 如果希望列表內也全部展開不隱藏，請改用下面這行： */
  /* white-space: pre-wrap; word-break: break-all; */
}

.code-block {
  background: #1e1e20;        /* 深色背景 */
  border: 1px solid #4c4d4f;  /* 邊框顏色 */
  border-left: 5px solid #6366f1; /* 左側加粗邊框跳色，增加辨識度 */
  color: #a9b7c6;             /* 字體顏色 (仿 IDE) */
  
  padding: 15px;
  border-radius: 6px;
  margin-top: 8px;
  
  font-family: 'Consolas', 'Monaco', monospace; /* 等寬字體 */
  font-size: 14px;            /* 字體大小 */
  line-height: 1.6;           /* 行高 */

  /* --- 核心：自動換行與滾動設定 --- */
  white-space: pre-wrap;      /* 保留換行符號，但過長時自動折行 (關鍵!) */
  word-break: break-all;      /* 強制斷行，防止超長單字撐開 */
  max-height: 600px;          /* 限制最大高度 */
  overflow-y: auto;           /* 超過高度才顯示垂直捲軸 */
}

/* 針對 Element UI 的深色適配 */
:deep(.el-tabs__item) { color: #94a3b8; }
:deep(.el-tabs__item.is-active) { color: #fff; }
:deep(.el-table) { background-color: transparent; --el-table-tr-bg-color: transparent; --el-table-header-bg-color: rgba(255,255,255,0.05); color: #cbd5e1; }
:deep(.el-table--enable-row-hover .el-table__body tr:hover > td) { background-color: rgba(255,255,255,0.05) !important; }

/* 狀態行顏色 */
:deep(.el-table .error-row) { background: rgba(220, 38, 38, 0.15) !important; }
:deep(.el-table .warning-row) { background: rgba(217, 119, 6, 0.15) !important; }

/* --- 展開區域容器優化 --- */
.expand-content {
  padding: 24px; /* [新增] 增加整體內邊距，讓內容呼吸 */
  background: rgba(15, 23, 42, 0.5); /* [可選] 稍微加深背景色，與表格區隔 */
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.05); /* [新增] 微弱邊框增加層次 */
  font-size: 15px; /* [調整] 整體字體放大 */
  color: #e2e8f0;  /* 淺灰白色 */
  line-height: 1.8; /* [調整] 增加行高，閱讀更舒適 */
}

/* 個別資訊段落間隔 */
.expand-content p {
  margin-bottom: 16px; /* [調整] 增加段落間距 */
  border-bottom: 1px dashed rgba(255, 255, 255, 0.1); /* [可選] 加分隔線 */
  padding-bottom: 8px;
}

/* 最後一個段落不需要分隔線 */
.expand-content p:last-of-type {
    border-bottom: none;
    margin-bottom: 8px;
}

/* 強調標題 (例如 "Full ID:") */
.expand-content strong {
  color: #a5b4fc; /* [調整] 使用亮一點的藍紫色強調標題 */
  font-weight: 700;
  font-size: 14px;
  text-transform: uppercase; /* [可選] 轉大寫看起來更像標籤 */
  letter-spacing: 0.05em;
  display: block; /* [可選] 讓標題獨佔一行 */
  margin-bottom: 4px;
}

/* --- Raw Message 區塊核心樣式修復 --- */
.code-block {
  /* 視覺樣式加強 */
  background: #0b1120; /* 比背景更深的顏色 */
  border: 1px solid rgba(99, 102, 241, 0.3); /* [調整] 增加明顯的藍紫色邊框 */
  padding: 16px; /* 增加內邊距 */
  border-radius: 6px;
  color: #c7d2fe; /* [調整] 內容字體顏色偏亮 */
  font-family: 'JetBrains Mono', 'Fira Code', monospace; /* 等寬字體 */
  font-size: 14px;
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.3); /* [可選] 內部陰影增加立體感 */
  margin-top: 8px;

  /* --- 核心解決方案：防止水平滾動 --- */
  white-space: pre-wrap; /* 關鍵：保留換行符，但允許瀏覽器自動換行 */
  word-break: break-word; /* 關鍵：在長單詞或長字符串中間強制斷行 */
  overflow-wrap: break-word; /* 兼容性寫法 */
  overflow-x: hidden; /* 強制隱藏水平滾動條 */
  
  /* [建議新增] 限制最大高度，內容太多時出現垂直滾動條，避免撐破頁面 */
  max-height: 500px;
  overflow-y: auto;
}

/* 優化垂直滾動條樣式 (Webkit瀏覽器) */
.code-block::-webkit-scrollbar {
    width: 8px;
}
.code-block::-webkit-scrollbar-track {
    background: rgba(0,0,0,0.1);
    border-radius: 4px;
}
.code-block::-webkit-scrollbar-thumb {
    background: rgba(99, 102, 241, 0.5);
    border-radius: 4px;
}
.code-block::-webkit-scrollbar-thumb:hover {
    background: rgba(99, 102, 241, 0.8);
}

.ai-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-tags {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.tag-group {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.tag-label {
  font-size: 12px;
  color: #94a3b8;
  width: 40px; /* 固定寬度讓排版整齊 */
}

.clickable-tag {
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.clickable-tag:hover {
  transform: translateY(-1px);
  filter: brightness(1.2);
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

</style>