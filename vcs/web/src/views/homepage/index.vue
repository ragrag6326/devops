<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
// 引入需要的圖標
import { 
  Promotion, Check, CircleClose, FolderOpened, 
  Bell, Timer, DataLine, MagicStick
} from '@element-plus/icons-vue';
import request from '@/utils/request';
import { getMrReviewDetail } from '@/api/mrReview';

const router = useRouter(); 

// --- 1. 原本的儀表板數據 ---
const stats = ref([
  { title: '總專案數', value: '18', icon: FolderOpened, color: '#3b82f6' },
  { title: '待處理 MRs', value: '0', icon: Promotion, color: '#f59e0b' },
  { title: '今日部署成功', value: '12', icon: Check, color: '#10b981' },
  { title: '版本衝突', value: '0', icon: CircleClose, color: '#ef4444' }
]);

const recentActivities = ref([
  { time: '1 分鐘前', description: '專案 A 部署至 Production 環境成功。', type: 'success' },
  { time: '1 小時前', description: '使用者 Peter 提交了 MR #1024 (功能優化)。', type: 'info' },
  { time: '昨天 15:30', description: '專案 B 版本更新至 2.1.0。', type: 'primary' },
  { time: '2 天前', description: '專案 C 部署至 Staging 環境失敗，請檢查。', type: 'danger' }
]);

// ------------------------------
// 歡迎卡片右側：assets 圖片隨機輪播/刷新隨機
// ------------------------------
// 註：此實作會抓取 `src/assets` 底下符合 `bg*.png` 的檔案。
const enableBgCarousel = true; // 若只要刷新隨機一張，改成 false
const carouselIntervalMs = 4500;

// 每張圖片底下的說明文字：用「檔名」對應
// 你之後只要新增圖片檔案並補上對應說明即可
const bgCaptionsByFile = {
  '1760253304510.gif' : '本來應該從從容容 游刃有餘..',
  'swimming.png' : '沒游泳的 賴祥德',
  'kp.png': '我是不會投降的。',
  '昌.png': '太離譜了，實在太離譜了',
  'liar.png': '不是喔! 不是這樣喔。',
  'koreafish.png': '我跟你談大海，你跟談我漱口杯',
};

const bgImageModules = import.meta.glob('../../assets/*.{gif,png}', {
  eager: true,
  import: 'default',
});

const bgSlides = Object.entries(bgImageModules)
  .map(([filePath, src]) => {
    const filename = filePath.split('/').pop() || filePath;
    const caption = bgCaptionsByFile[filename] || `（${filename}）圖片說明待補上`;
    return { id: filename, src, filename, caption };
  })
  .sort((a, b) => a.filename.localeCompare(b.filename));

const slideOrder = ref([]);
const slidePointer = ref(0);
const isBgPaused = ref(false);
const bgTimeoutId = ref(null);

const shuffleInPlace = (arr) => {
  // Fisher-Yates shuffle
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
};

const clearBgTimer = () => {
  if (bgTimeoutId.value) {
    window.clearTimeout(bgTimeoutId.value);
    bgTimeoutId.value = null;
  }
};

const initBgCarousel = () => {
  if (!bgSlides.length) return;
  slideOrder.value = bgSlides.map((_, i) => i);
  shuffleInPlace(slideOrder.value);
  slidePointer.value = 0;
};

const currentBgSlide = computed(() => {
  if (!bgSlides.length) return { id: '', src: '', filename: '', caption: '' };
  const idx = slideOrder.value[slidePointer.value];
  return bgSlides[idx ?? 0] ?? bgSlides[0];
});

const scheduleNextBg = () => {
  clearBgTimer();
  if (isBgPaused.value) return;
  if (!enableBgCarousel) return;
  if (bgSlides.length <= 1) return;

  bgTimeoutId.value = window.setTimeout(() => {
    slidePointer.value = (slidePointer.value + 1) % slideOrder.value.length;
    scheduleNextBg();
  }, carouselIntervalMs);
};

const pauseBgCarousel = () => {
  isBgPaused.value = true;
  clearBgTimer();
};

const resumeBgCarousel = () => {
  isBgPaused.value = false;
  scheduleNextBg();
};


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
  { key: 'tkbgoapi_dev', name: 'tkbgoapi', env: 'dev' , url: '/version/getReleaseNote', params: { projectName: 'tkbgoapi', env: 'dev' } },
  { key: 'tkbgoapi_prod', name: 'tkbgoapi' , env: 'prod' , url: '/version/getReleaseNote', params: { projectName: 'tkbgoapi', env: 'prod' } },
  { key: 'tkbtv_dev', name: 'tkbtv', env: 'dev' ,url: '/version/getReleaseNote', params: { projectName: 'tkbtv', env: 'dev' } },
  { key: 'tkbtv_prod', name: 'tkbtv', env: 'prod' ,url: '/version/getReleaseNote', params: { projectName: 'tkbtv', env: 'prod' } }
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

const mrCountsMap = ref({}); // 紀錄各專案的 MR 數量
const mrDataMap = ref({}); // 結構：{ tkbtv: { count: 1, list: [...] } }
const mrReviewMap = ref({}); // key: projectName-iid -> review entity

const reviewStatusLabel = (status) => {
  const map = {
    PENDING: { type: 'warning', text: 'AI 審核中' },
    COMPLETED: { type: 'success', text: 'AI 已審核' },
    FAILED: { type: 'danger', text: 'AI 審核失敗' }
  };
  return map[status] || { type: 'info', text: '未審核' };
};

const loadReviewsForPendingMrs = async () => {
  const tasks = [];
  Object.entries(mrDataMap.value).forEach(([projectName, data]) => {
    (data.list || []).forEach((mr) => {
      tasks.push(
        getMrReviewDetail(projectName, mr.iid)
          .then((res) => {
            if (res.code === 1 && res.data) {
              mrReviewMap.value[`${projectName}-${mr.iid}`] = res.data;
            }
          })
          .catch(() => {})
      );
    });
  });
  await Promise.all(tasks);
};

const goMrReviewPage = () => {
  router.push('/mr/review');
};

// --- 獲取待處理 MR 數量 ---
const fetchPendingMRs = async () => {
  const uniqueProjects = [...new Set(sources.map(s => s.name))];
  
  console.log(`uniqueProjects` , uniqueProjects);
  
  try {
    const promises = uniqueProjects.map(projectName => 
      request.get(`/gitlab/projects/${projectName}/mrs/pending`)
        .then(res => {
          // 根據您提供的結構：res.code === 1 且資料在 res.data
          const list = res.data || []; 
          return {
            projectName,
            count: (res.code === 1 && Array.isArray(res.data)) ? res.data.length : 0,
            list: list
          };
        })
        .catch(err => {
          console.error(`Fetch MRs for ${projectName} failed:`, err);
          return { projectName, count: 0};
        })
    );

    const results = await Promise.all(promises);
    
    console.log(`mrDataMap= ` , results);
    

    let totalCount = 0;
    const details = [];

    results.forEach(item => {
      mrDataMap.value[item.projectName] = { count: item.count, list: item.list }; // 供下方 Tab 使用
      totalCount += item.count;
      // 存入 details 供上方 Card 使用
      details.push({ name: item.projectName, count: item.count });
    });

    // 更新統計卡片
    const mrStat = stats.value.find(s => s.title === '待處理 MRs');
    if (mrStat) {
      mrStat.value = totalCount.toString();
      mrStat.details = details; // 新增這行
    }

    await loadReviewsForPendingMrs();

  } catch (error) {
    console.error('Fetch all pending MRs error', error);
  }
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

const refreshData = async () => {
  loading.value = true;
  await Promise.all([fetchReleaseNotes(), fetchPendingMRs()]);
  loading.value = false;
};

onMounted(() => {
  console.log('Homepage Dashboard loaded.');

  initBgCarousel();
  scheduleNextBg();

  fetchReleaseNotes(); // 載入時觸發
  fetchPendingMRs(); // 初始載入
});

onUnmounted(() => {
  clearBgTimer();
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
          <div
            class="bg-carousel"
            @mouseenter="pauseBgCarousel"
            @mouseleave="resumeBgCarousel"
          >
            <transition name="carousel-fade" mode="out-in">
              <img
                v-if="currentBgSlide.src"
                :key="currentBgSlide.id"
                class="carousel-img"
                :src="currentBgSlide.src"
                :alt="currentBgSlide.filename || 'carousel image'"
              />
            </transition>

            <div class="carousel-dots" aria-hidden="true" v-if="slideOrder.length > 1">
              <span
                v-for="(slideIdx, i) in slideOrder"
                :key="bgSlides[slideIdx]?.id || i"
                class="dot"
                :class="{ active: i === slidePointer }"
              />
            </div>
          </div>
          <p class="bg-caption">{{ currentBgSlide.caption }}</p>
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
              
              <el-popover
                v-if="stat.title === '待處理 MRs' && stat.details"
                placement="bottom"
                :width="200"
                trigger="hover"
                popper-class="mr-popper"
              >
                <template #reference>
                  <p class="stat-value" :style="{ color: stat.color, cursor: 'pointer' }">
                    {{ stat.value }}
                  </p>
                </template>
                
                <div class="mr-detail-list">
                  <div v-for="item in stat.details" :key="item.name" class="mr-detail-item">
                    <span class="detail-name">{{ item.name }}</span>
                    <el-tag size="small" :type="item.count > 0 ? 'danger' : 'info'">
                      {{ item.count }}
                    </el-tag>
                  </div>
                </div>
              </el-popover>

              <p v-else class="stat-value" :style="{ color: stat.color }">{{ stat.value }}</p>
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
              <el-button link type="primary" :loading="loading" @click="refreshData">
                <el-icon><DataLine /></el-icon> 重新整理
              </el-button>
            </div>
          </template>

          <el-tabs v-model="activeTab" class="custom-tabs">
            <el-tab-pane 
              v-for="source in sources" 
              :key="source.key" 
              :name="source.key"
            >
              <template #label>
                <span>{{ source.name }} ({{ source.env }})</span>
                <el-badge 
                  v-if="mrDataMap[source.name]?.count > 0" 
                  :value="mrDataMap[source.name].count" 
                  class="tab-badge"
                />
              </template>

              <div class="release-content-box custom-scrollbar">
                <div v-html="formatMarkdown(releaseNotes[source.key])" class="markdown-body"></div>
                <div v-if="mrDataMap[source.name] && mrDataMap[source.name].count > 0" class="mr-section">
                    <div class="mr-section-head">
                      <h4 class="mr-section-title">
                        <el-icon style="margin-right: 6px;"><Promotion /></el-icon>
                        待處理 Merge Requests
                      </h4>
                      <el-button link type="primary" size="small" @click="goMrReviewPage">
                        <el-icon><MagicStick /></el-icon> AI 審核中心
                      </el-button>
                    </div>
                    <div v-for="mr in mrDataMap[source.name].list" :key="mr.iid" class="mr-item">
                      <div class="mr-item-header">
                        <span class="mr-iid">!{{ mr.iid }}</span>
                        <span class="mr-title">{{ mr.title }}</span>
                        <el-tag
                          v-if="mrReviewMap[`${source.name}-${mr.iid}`]"
                          size="small"
                          :type="reviewStatusLabel(mrReviewMap[`${source.name}-${mr.iid}`].reviewStatus).type"
                        >
                          {{ reviewStatusLabel(mrReviewMap[`${source.name}-${mr.iid}`].reviewStatus).text }}
                        </el-tag>
                      </div>
                      <div class="mr-item-footer">
                        <span>提交者：<strong>{{ mr.authorName }}</strong></span>
                        <span><el-icon><Timer /></el-icon> {{ mr.createdAt }}</span>
                      </div>
                      <p
                        v-if="mrReviewMap[`${source.name}-${mr.iid}`]?.summary"
                        class="mr-review-summary"
                      >
                        {{ mrReviewMap[`${source.name}-${mr.iid}`].summary }}
                      </p>
                    </div>
                  </div>
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
  display: flex;
  flex-direction: column;
  background: var(--panel);
  border: 1px solid var(--border-color);
  overflow: hidden;
}
.card-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex: 1;
  height: auto;
}
.text-area { flex: 2; padding-right: 30px; }
.image-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  gap: 10px;
}
.dashboard-img {
  max-width: 100%;
  height: auto;
  max-height: 200px;
  border-radius: var(--radius);
  opacity: 0.85;
}

/* --- 背景圖片輪播 --- */
.bg-carousel {
  position: relative;
  width: 240px;
  height: 150px;
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  background: var(--panel);
}

.carousel-img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.92;
  transform: scale(1.02);
}

/* 淡入淡出動畫（切換背景圖） */
.carousel-fade-enter-active,
.carousel-fade-leave-active {
  transition: opacity 0.45s ease, transform 0.45s ease;
}
.carousel-fade-enter-from,
.carousel-fade-leave-to {
  opacity: 0;
  transform: scale(1.03);
}
.carousel-fade-enter-to,
.carousel-fade-leave-from {
  opacity: 1;
  transform: scale(1.02);
}

.bg-caption {
  margin: 0;
  max-width: 240px;
  font-size: 12px;
  line-height: 1.4;
  color: var(--muted);
  padding: 0 4px;
}

.carousel-dots {
  position: absolute;
  top: 10px;
  right: 10px;
  display: flex;
  gap: 6px;
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.35);
  border: 1px solid rgba(255, 255, 255, 0.25);
}
.dot.active {
  background: rgba(255, 255, 255, 0.95);
  border-color: rgba(255, 255, 255, 0.95);
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


/* --- MR 區塊樣式修正 --- */
.mr-section {
  margin-top: 20px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05); /* 輕微透明背景，區隔 Markdown 內容 */
  border-radius: 8px;
  border: 1px solid var(--border-color);
}

.mr-section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.mr-section-title {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
  color: #60a5fa !important;
  display: flex;
  align-items: center;
}

.mr-review-summary {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.mr-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.mr-item {
  background: var(--panel); /* 確保背景與卡片一致 */
  border: 1px solid var(--border-color);
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 6px;
}

/* 針對標題文字的顏色修正 */
.mr-title {
  color: #e5e7eb !important; /* 淺灰色/白色，確保在深色背景可見 */
  font-weight: 500;
  font-size: 14px;
}

.mr-iid {
  color: #fb7185; /* 亮粉色/紅色顯示 iid */
  font-weight: bold;
  margin-right: 8px;
}

.mr-item-footer {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  /* 輔助文字使用較淡的顏色 */
  color: #9ca3af !important; 
  font-size: 12px;
}

.mr-author {
  color: #fbbf24; /* 琥珀色強調提交者 */
  font-weight: bold;
}


/* 統計細節彈窗樣式 */
.mr-detail-list {
  padding: 5px 0;
}

.mr-detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}

.mr-detail-item:last-child {
  border-bottom: none;
}

.detail-name {
  font-size: 13px;
  color: var(--text); /* 跟隨主題文字顏色 */
  font-weight: 500;
}

/* 讓 Popover 背景符合主題 */
:deep(.el-popper.mr-popper) {
  background: var(--panel) !important;
  border: 1px solid var(--border-color) !important;
  color: var(--text) !important;
}

</style>