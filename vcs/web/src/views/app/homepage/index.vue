<script setup>
import { ref, onMounted } from 'vue';
// 引入需要的圖標
import { 
  Promotion, Check, CircleClose, FolderOpened, 
  Bell, Timer, DataLine 
} from '@element-plus/icons-vue';
// 假設您有封裝好的 request，如果沒有請替換為 axios
import request from '@/utils/request'; 

// --- 1. 原本的儀表板數據 ---
const stats = ref([
  { title: '總專案數', value: '18', icon: FolderOpened, color: '#3b82f6' },
  { title: '待處理 MRs', value: '3', icon: Promotion, color: '#f59e0b' },
  { title: '今日部署成功', value: '12', icon: Check, color: '#10b981' },
  { title: '版本衝突', value: '0', icon: CircleClose, color: '#ef4444' }
]);

const recentActivities = ref([
  { time: '1 分鐘前', description: '專案 A 部署至 Production 環境成功。', type: 'success' },
  { time: '1 小時前', description: '使用者 Peter 提交了 MR #1024 (功能優化)。', type: 'info' },
  { time: '昨天 15:30', description: '專案 B 版本更新至 2.1.0。', type: 'primary' },
  { time: '2 天前', description: '專案 C 部署至 Staging 環境失敗，請檢查。', type: 'danger' }
]);

// --- 2. 新增：Release Note 相關邏輯 ---
const activeTab = ref('tkbgoapi_dev'); // 預設顯示的分頁
const releaseNotes = ref({
  tkbgoapi_dev: '',
  tkbgoapi_prod: '',
  tkbtv_dev: '',
  tkbtv_prod: ''
});
const loading = ref(false);

// 定義要請求的來源 API
const sources = [
  { key: 'tkbgoapi_dev', name: 'TKBGO API (Dev)', url: '/version/getReleaseNote', params: { projectName: 'tkbgoapi', env: 'dev' } },
  { key: 'tkbgoapi_prod', name: 'TKBGO API (Prod)', url: '/version/getReleaseNote', params: { projectName: 'tkbgoapi', env: 'prod' } },
  { key: 'tkbtv_dev', name: 'TKBTV (Dev)', url: '/version/getReleaseNote', params: { projectName: 'tkbtv', env: 'dev' } },
  { key: 'tkbtv_prod', name: 'TKBTV (Prod)', url: '/version/getReleaseNote', params: { projectName: 'tkbtv', env: 'prod' } }
];

// 簡易 Markdown 轉 HTML 工具
const formatMarkdown = (text) => {
  if (!text) return '<div class="empty-note">尚無發布紀錄</div>';
  return text
    .replace(/^## (.*$)/gim, '<h3 class="rn-title">$1</h3>')
    .replace(/\*\*(.*?)\*\*/gim, '<strong class="rn-bold">$1</strong>')
    .replace(/`([^`]+)`/gim, '<code class="rn-code">$1</code>')
    .replace(/^\- (.*$)/gim, '<li class="rn-list-item">$1</li>')
    .replace(/\n/gim, '<br>');
};

// 獲取所有 Release Notes
const fetchReleaseNotes = async () => {
  loading.value = true;
  try {
    const promises = sources.map(source => 
      request.get(source.url, { params: source.params })
        .then(res => ({ key: source.key, data: res.data || res })) 
        .catch(() => ({ key: source.key, data: '> ⚠ 無法獲取資料' }))
    );
    const results = await Promise.all(promises);
    results.forEach(item => {
      // 這裡假設後端回傳結構，請依實際情況調整 (例如 item.data.data)
      releaseNotes.value[item.key] = typeof item.data === 'string' ? item.data : (item.data.data || '');
    });
  } catch (error) {
    console.error('Fetch release notes error', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  console.log('Homepage Dashboard loaded.');
  fetchReleaseNotes(); // 載入時觸發
});
</script>

<template>
  <div class="homepage-dashboard">
    
    <el-card class="welcome-card" shadow="hover">
      <div class="card-content">
        <div class="text-area">
          <h2 class="welcome-title">歡迎回來，版本控制中心</h2>
          <p class="welcome-subtitle">
            隨時掌握各專案的部署狀態與合併請求 (MR) 進度。請查看下方重要指標與最新動態。
          </p>
          <div class="quick-actions">
            <el-button type="primary">新增版本</el-button>
            <el-button type="info" plain>查看部署日誌</el-button>
          </div>
        </div>
        <div class="image-area">
           <img src="@/assets/bg.png" alt="Dashboard Illustration" class="dashboard-img">
        </div>
      </div>
    </el-card>

    <el-row :gutter="20" class="stat-row">
      <el-col v-for="(stat, index) in stats" :key="index" :span="6">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-box">
            <el-icon :style="{ color: stat.color, background: stat.color + '33' }" class="stat-icon">
              <component :is="stat.icon" />
            </el-icon>
            <div class="stat-info">
              <p class="stat-title">{{ stat.title }}</p>
              <p class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="content-row">
      
      <el-col :span="16">
        <el-card shadow="hover" class="release-card h-full">
          <template #header>
            <div class="card-header">
              <span class="header-title">
                <el-icon class="header-icon"><Bell /></el-icon> 最新版本發布消息
              </span>
              <el-button link type="primary" :loading="loading" @click="fetchReleaseNotes">
                <el-icon><DataLine /></el-icon> 重新整理
              </el-button>
            </div>
          </template>

          <el-tabs v-model="activeTab" class="custom-tabs">
            <el-tab-pane 
              v-for="source in sources" 
              :key="source.key" 
              :label="source.name" 
              :name="source.key"
            >
              <div class="release-content-box custom-scrollbar">
                <div v-html="formatMarkdown(releaseNotes[source.key])" class="markdown-body"></div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="hover" class="activity-card h-full">
          <template #header>
            <div class="card-header">
              <span class="header-title">
                <el-icon class="header-icon"><Timer /></el-icon> 最新活動
              </span>
            </div>
          </template>
          
          <div class="activity-box custom-scrollbar">
            <el-timeline style="padding-left: 5px;">
              <el-timeline-item
                v-for="(activity, index) in recentActivities"
                :key="index"
                :timestamp="activity.time"
                :type="activity.type"
                placement="top"
                size="large"
              >
                <span class="activity-text">{{ activity.description }}</span>
              </el-timeline-item>
            </el-timeline>
          </div>
          
          <div class="card-footer">
             <el-button link type="primary">查看更多歷史</el-button>
          </div>
        </el-card>
      </el-col>

    </el-row>
  </div>
</template>

<style scoped>
/* 使用 Flexbox 確保在主題切換時，顏色變數能正確套用 */
.homepage-dashboard {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 實用類別 */
.h-full { height: 100%; }

/* --- 1. 歡迎卡片 --- */
.welcome-card {
  height: 250px;
  background: var(--panel);
  border: 1px solid var(--border-color);
  overflow: hidden;
}
.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}
.text-area { flex: 2; padding-right: 30px; }
.image-area { flex: 1; display: flex; justify-content: flex-end; align-items: center; }
.dashboard-img {
  max-width: 100%;
  height: auto;
  max-height: 200px;
  border-radius: var(--radius);
  opacity: 0.85;
}
.welcome-title { font-size: 28px; margin-top: 0; color: var(--text); }
.welcome-subtitle { color: var(--muted); line-height: 1.6; max-width: 800px; }
.quick-actions { margin-top: 20px; }

/* --- 2. 數據指標 --- */
.stat-row { margin-bottom: 0; }
.stat-card { height: 100px; transition: all 0.3s ease; border: 1px solid var(--border-color); background: var(--panel); }
.stat-box { display: flex; align-items: center; }
.stat-icon {
  font-size: 28px; padding: 12px; border-radius: 8px;
  margin-right: 15px; min-width: 28px;
}
.stat-info { display: flex; flex-direction: column; }
.stat-title { margin: 0; font-size: 14px; color: var(--muted); }
.stat-value { margin: 5px 0 0 0; font-size: 24px; font-weight: bold; }

/* --- 3. 內容區塊 (Release Note + Activity) --- */
.content-row { display: flex; align-items: stretch; } /* 讓左右高度一致 */

.release-card, .activity-card {
  background: var(--panel);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}

/* 確保卡片內容區填滿剩餘高度 */
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止撐開 */
}

.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-title { display: flex; align-items: center; font-size: 16px; font-weight: 600; color: var(--text); }
.header-icon { margin-right: 8px; color: var(--primary-color); }

/* Release Note 內容 */
.release-content-box {
  height: 400px; /* 固定高度 */
  overflow-y: auto;
  padding: 15px;
  background: var(--table-hover-bg); /* 使用變數 */
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

/* Activity 內容 */
.activity-box {
  height: 360px;
  overflow-y: auto;
  padding-right: 10px;
}
.activity-text { color: var(--text); font-size: 14px; }
.card-footer { margin-top: auto; text-align: center; padding-top: 10px; }

/* --- Markdown 美化 --- */
:deep(.markdown-body) { color: var(--text); line-height: 1.7; font-size: 14px; }
:deep(.rn-title) {
  font-size: 18px; color: var(--primary-color);
  margin-bottom: 15px; margin-top: 5px;
  border-bottom: 1px solid var(--border-color); padding-bottom: 8px;
}
:deep(.rn-bold) { color: var(--secondary-color); font-weight: 700; }
:deep(.rn-code) {
  background: rgba(255, 255, 255, 0.1); padding: 2px 6px;
  border-radius: 4px; font-family: monospace; color: #fbbf24;
}
:deep(.rn-list-item) { margin-left: 20px; margin-bottom: 6px; list-style-type: disc; display: list-item; }
:deep(.empty-note) { text-align: center; padding: 60px; color: var(--muted); }

/* --- Tabs 樣式 --- */
:deep(.el-tabs__item) { color: var(--muted); }
:deep(.el-tabs__item.is-active) { color: var(--primary-color); font-weight: bold; }
:deep(.el-tabs__nav-wrap::after) { background-color: var(--border-color); }

/* --- 滾動條美化 --- */
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 3px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }
</style>