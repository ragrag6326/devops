<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import {
  Promotion, Check, CircleClose, FolderOpened,
  Bell, Timer, DataLine, MagicStick
} from '@element-plus/icons-vue';
import request from '@/utils/request';
import { getMrReviewDetail } from '@/api/mrReview';
import { getProjectList } from '@/api/project';

const router = useRouter();

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

// --- 背景圖輪播 ---
const enableBgCarousel = true;
const carouselIntervalMs = 4500;

const bgCaptionsByFile = {
  '1760253304510.gif': '本來應該從從容容 游刃有餘..',
  'swimming.png': '沒游泳的 賴祥德',
  'kp.png': '我是不會投降的。',
  '昌.png': '太離譜了，實在太離譜了',
  'liar.png': '不是喔! 不是這樣喔。',
  'koreafish.png': '我跟你談大海，你跟談我漱口杯',
};

const makeSlideSvg = (color) => {
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="240" height="150" viewBox="0 0 240 150"><rect width="240" height="150" fill="${color}"/><rect x="8" y="8" width="224" height="134" rx="10" fill="none" stroke="rgba(255,255,255,0.25)" stroke-width="2"/></svg>`;
  return `data:image/svg+xml,${encodeURIComponent(svg)}`;
};

const slideColors = {
  '1760253304510.gif': '#5a7ab8',
  'swimming.png': '#38bdf8',
  'kp.png': '#ef4444',
  '昌.png': '#fbbf24',
  'liar.png': '#a855f7',
  'koreafish.png': '#34d399',
};

const bgImageModules = import.meta.glob('../../assets/*.{gif,png,jpg,jpeg,webp}', {
  eager: true,
  import: 'default',
});

const bgSlides = Object.keys(bgCaptionsByFile)
  .map((filename) => {
    const matched = Object.entries(bgImageModules).find(([path]) => path.endsWith(`/${filename}`));
    const src = matched ? matched[1] : makeSlideSvg(slideColors[filename] || '#6366f1');
    const caption = bgCaptionsByFile[filename] || `（${filename}）圖片說明待補上`;
    return { id: filename, src, filename, caption };
  })
  .sort((a, b) => a.filename.localeCompare(b.filename));

const slideOrder = ref([]);
const slidePointer = ref(0);
const isBgPaused = ref(false);
const bgTimeoutId = ref(null);

const shuffleInPlace = (arr) => {
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
  if (isBgPaused.value || !enableBgCarousel || bgSlides.length <= 1) return;
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

// --- Release Note ---
const allProjects = ref([])
// sources 由 DB 動態產生：hasProd=1 加 prod tab，hasDev=1 加 dev tab
const sources = computed(() => {
  const list = []
  allProjects.value.forEach(p => {
    if (p.hasDev)  list.push({ key: `${p.name}_dev`,  name: p.name, env: 'dev',  label: p.displayName || p.name, url: '/version/getReleaseNote', params: { projectName: p.name, env: 'dev'  } })
    if (p.hasProd) list.push({ key: `${p.name}_prod`, name: p.name, env: 'prod', label: p.displayName || p.name, url: '/version/getReleaseNote', params: { projectName: p.name, env: 'prod' } })
  })
  return list
})
const activeTab = ref('');
const releaseNotes = ref({});
const loading = ref(false);

// sources 載入後初始化 activeTab 與 releaseNotes
watch(sources, (list) => {
  if (list.length && !activeTab.value) {
    activeTab.value = list[0].key
  }
  list.forEach(s => {
    if (!(s.key in releaseNotes.value)) releaseNotes.value[s.key] = ''
  })
}, { immediate: true })

const formatMarkdown = (text) => {
  if (!text) return '<div class="empty-note">尚無發布紀錄</div>';
  return text
    .replace(/^## (.*$)/gim, '<h3 class="rn-title">$1</h3>')
    .replace(/\*\*(.*?)\*\*/gim, '<strong class="rn-bold">$1</strong>')
    .replace(/`([^`]+)`/gim, '<code class="rn-code">$1</code>')
    .replace(/^\- (.*$)/gim, '<li class="rn-list-item">$1</li>')
    .replace(/\n/gim, '<br>');
};

const mrDataMap = ref({});
const mrReviewMap = ref({});

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

const goMrReviewPage = () => router.push('/mr/review');

const fetchPendingMRs = async () => {
  const uniqueProjects = [...new Set(sources.map(s => s.name))];
  try {
    const results = await Promise.all(
      uniqueProjects.map(projectName =>
        request.get(`/gitlab/projects/${projectName}/mrs/pending`)
          .then(res => ({
            projectName,
            count: (res.code === 1 && Array.isArray(res.data)) ? res.data.length : 0,
            list: res.data || []
          }))
          .catch(() => ({ projectName, count: 0, list: [] }))
      )
    );

    let totalCount = 0;
    const details = [];
    results.forEach(item => {
      mrDataMap.value[item.projectName] = { count: item.count, list: item.list };
      totalCount += item.count;
      details.push({ name: item.projectName, count: item.count });
    });

    const mrStat = stats.value.find(s => s.title === '待處理 MRs');
    if (mrStat) {
      mrStat.value = totalCount.toString();
      mrStat.details = details;
    }

    await loadReviewsForPendingMrs();
  } catch (error) {
    console.error('Fetch all pending MRs error', error);
  }
};

const fetchReleaseNotes = async () => {
  loading.value = true;
  try {
    const results = await Promise.all(
      sources.map(source =>
        request.get(source.url, { params: source.params })
          .then(res => ({ key: source.key, data: res.data || res }))
          .catch(() => ({ key: source.key, data: '> ⚠ 無法獲取資料' }))
      )
    );
    results.forEach(item => {
      releaseNotes.value[item.key] = typeof item.data === 'string' ? item.data : (item.data.data || '');
    });
  } finally {
    loading.value = false;
  }
};

const refreshData = async () => {
  loading.value = true;
  await Promise.all([fetchReleaseNotes(), fetchPendingMRs()]);
  loading.value = false;
};

onMounted(async () => {
  initBgCarousel();
  scheduleNextBg();
  // 先載入專案清單，sources computed 才有資料
  const res = await getProjectList()
  if (res.code === 1) allProjects.value = res.data || []
  fetchReleaseNotes();
  fetchPendingMRs();
});

onUnmounted(() => clearBgTimer());
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
        <div
          class="image-area"
          @mouseenter="pauseBgCarousel"
          @mouseleave="resumeBgCarousel"
        >
          <template v-if="bgSlides.length">
            <div class="bg-carousel">
              <Transition name="carousel-fade" mode="out-in">
                <img
                  :key="currentBgSlide.id"
                  :src="currentBgSlide.src"
                  :alt="currentBgSlide.filename"
                  class="carousel-img"
                />
              </Transition>
              <div v-if="bgSlides.length > 1" class="carousel-dots">
                <span
                  v-for="(_, idx) in slideOrder"
                  :key="idx"
                  class="dot"
                  :class="{ active: idx === slidePointer }"
                />
              </div>
            </div>
            <p class="bg-caption">{{ currentBgSlide.caption }}</p>
          </template>
          <img v-else src="@/assets/bg.png" alt="Dashboard Illustration" class="dashboard-img">
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
                  <p class="stat-value" :style="{ color: stat.color, cursor: 'pointer' }">{{ stat.value }}</p>
                </template>
                <div class="mr-detail-list">
                  <div v-for="item in stat.details" :key="item.name" class="mr-detail-item">
                    <span class="detail-name">{{ item.name }}</span>
                    <el-tag size="small" :type="item.count > 0 ? 'danger' : 'info'">{{ item.count }}</el-tag>
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
            <el-tab-pane v-for="source in sources" :key="source.key" :name="source.key">
              <template #label>
                <span>{{ source.label }} ({{ source.env }})</span>
                <el-badge
                  v-if="mrDataMap[source.name]?.count > 0"
                  :value="mrDataMap[source.name].count"
                  class="tab-badge"
                />
              </template>

              <div class="release-content-box custom-scrollbar">
                <div v-html="formatMarkdown(releaseNotes[source.key])" class="markdown-body" />
                <div v-if="mrDataMap[source.name]?.count > 0" class="mr-section">
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
.homepage-dashboard {
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.h-full { height: 100%; }

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

.bg-carousel {
  position: relative;
  width: 240px;
  height: 150px;
  border-radius: var(--radius);
  overflow: hidden;
  border: 1px solid var(--border-color);
  box-shadow: 0 10px 30px #00000026;
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

.stat-row { margin-bottom: 0; }
.stat-card {
  height: 100px;
  transition: all 0.3s ease;
  border: 1px solid var(--border-color);
  background: var(--panel);
}
.stat-box { display: flex; align-items: center; }
.stat-icon {
  font-size: 28px;
  padding: 12px;
  border-radius: 8px;
  margin-right: 15px;
  min-width: 28px;
}
.stat-info { display: flex; flex-direction: column; }
.stat-title { margin: 0; font-size: 14px; color: var(--muted); }
.stat-value { margin: 5px 0 0; font-size: 24px; font-weight: 700; }

.content-row { display: flex; align-items: stretch; }
.release-card, .activity-card {
  background: var(--panel);
  border: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
}
:deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-title {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}
.header-icon { margin-right: 8px; color: var(--primary-color); }

.release-content-box {
  height: 400px;
  overflow-y: auto;
  padding: 15px;
  background: var(--table-hover-bg);
  border-radius: 8px;
  border: 1px solid var(--border-color);
}
.activity-box { height: 360px; overflow-y: auto; padding-right: 10px; }
.activity-text { color: var(--text); font-size: 14px; }
.card-footer { margin-top: auto; text-align: center; padding-top: 10px; }

:deep(.markdown-body) { color: var(--text); line-height: 1.7; font-size: 14px; }
:deep(.rn-title) {
  font-size: 18px;
  color: var(--primary-color);
  margin-bottom: 15px;
  margin-top: 5px;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 8px;
}
:deep(.rn-bold) { color: var(--secondary-color); font-weight: 700; }
:deep(.rn-code) {
  background: rgba(255, 255, 255, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: monospace;
  color: #fbbf24;
}
:deep(.rn-list-item) {
  margin-left: 20px;
  margin-bottom: 6px;
  list-style-type: disc;
  display: list-item;
}
:deep(.empty-note) { text-align: center; padding: 60px; color: var(--muted); }

:deep(.el-tabs__item) { color: var(--muted); }
:deep(.el-tabs__item.is-active) { color: var(--primary-color); font-weight: bold; }
:deep(.el-tabs__nav-wrap::after) { background-color: var(--border-color); }
.tab-badge { margin-left: 6px; }

.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: var(--border-color); border-radius: 3px; }
.custom-scrollbar::-webkit-scrollbar-track { background: transparent; }

.mr-section {
  margin-top: 20px;
  padding: 15px;
  background: rgba(255, 255, 255, 0.05);
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
  font-weight: 700;
  color: #60a5fa !important;
  display: flex;
  align-items: center;
}
.mr-item {
  background: var(--panel);
  border: 1px solid var(--border-color);
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 6px;
}
.mr-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.mr-title {
  color: #e5e7eb !important;
  font-weight: 500;
  font-size: 14px;
}
.mr-iid {
  color: #fb7185;
  font-weight: 700;
  margin-right: 8px;
}
.mr-item-footer {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  color: #9ca3af !important;
  font-size: 12px;
}
.mr-review-summary {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.5;
}

.mr-detail-list { padding: 5px 0; }
.mr-detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-color);
}
.detail-name { font-size: 13px; color: var(--text); font-weight: 500; }
.mr-author { color: #fbbf24; font-weight: 700; }
:deep(.el-popper.mr-popper) {
  background: var(--panel) !important;
  border: 1px solid var(--border-color) !important;
  color: var(--text) !important;
}
</style>
