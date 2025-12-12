<script setup>
import { ref, onMounted } from 'vue';
import { Promotion, Check, CircleClose, FolderOpened } from '@element-plus/icons-vue';

// 假設的儀表板數據 (這裡使用靜態數據，您可以替換成從後端 API 獲取的數據)
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

onMounted(() => {
  // 可以在這裡呼叫 API 獲取真實數據
  console.log('Homepage Dashboard loaded.');
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
        <!-- <div class="image-area">
          <img src="@/assets/bg.png" alt="Dashboard Illustration" class="dashboard-img">
        </div> -->
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

    <el-row :gutter="20">
      <el-col :span="24">
        <el-card header="最新活動總覽" shadow="hover">
          <el-timeline style="padding-left: 10px;">
            <el-timeline-item
              v-for="(activity, index) in recentActivities"
              :key="index"
              :timestamp="activity.time"
              :type="activity.type"
              placement="top"
            >
              {{ activity.description }}
            </el-timeline-item>
          </el-timeline>
          <el-button link type="primary" style="margin-top: 15px;">查看更多日誌</el-button>
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

/* --- 1. 歡迎卡片 (Welcome Card) --- */
.welcome-card {
  height: 250px;
  /* 確保 welcome-card 顯得更大氣 */
  background: var(--panel);
  border: 1px solid var(--border-color); 
}

/* 卡片內容排版：文字和圖片並排 */
.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.text-area {
  flex: 2; /* 佔用較多空間 */
  padding-right: 30px;
}

.image-area {
  flex: 1; /* 佔用較少空間 */
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.dashboard-img {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius);
  /* 添加輕微陰影讓圖片更立體 */
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1); 
  opacity: 0.85; /* 略微降低透明度，更好地融入背景 */
}

.welcome-title {
  font-size: 28px;
  margin-top: 0;
  color: var(--text);
}

.welcome-subtitle {
  color: var(--muted);
  line-height: 1.6;
  max-width: 800px;
}

.quick-actions {
  margin-top: 20px;
}

/* --- 2. 數據指標 (Stat Cards) --- */
.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  height: 100px;
  transition: all 0.3s ease;
}

.stat-box {
  display: flex;
  align-items: center;
}

.stat-icon {
  font-size: 28px;
  padding: 12px;
  border-radius: 8px;
  margin-right: 15px;
  min-width: 28px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-title {
  margin: 0;
  font-size: 14px;
  color: var(--muted);
}

.stat-value {
  margin: 5px 0 0 0;
  font-size: 24px;
  font-weight: bold;
}
</style>