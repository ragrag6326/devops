<script setup>
import { onMounted, ref, computed , onActivated } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus' 
import { useRouter, useRoute } from 'vue-router'
import { toggleTheme } from '@/utils/theme';
import { 
    EditPen, SwitchButton, Promotion, Message, 
    User, HomeFilled, Avatar, Money, TrendCharts, Menu,
    ArrowRight, ArrowDown, Sunny, Moon // 確保導入主題圖示
} from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute(); 
const currentUser = ref('');
const currentRole = ref('');

const fetchData = () => {
    console.log('--- 數據正在被請求/刷新中 ---');
};

const menuItems = [
    { name: '首頁', path: '/homepage', icon: Promotion },
    { 
        name: '版本管理', path: '/version', icon: Message, children: [
            { name: '版本紀錄', path: '/version/history', icon: User },
        ]
    },
    { 
        name: 'MR/程式碼審核', path: '/mr', icon: Avatar, children: [
            { name: '歷史紀錄查詢', path: '/mr/history', icon: Money }, 
        ]
    },
    { 
        name: '系統', path: '/system', icon: Menu, children: [
            { name: '狀態監控', path: '/system/monitor', icon: TrendCharts },
            { name: '日誌查詢', path: '/system/log_query', icon: TrendCharts },
            { name: '日誌智能分析', path: '/system/log', icon: TrendCharts }, 
        ]
    },
    { 
        name: '用戶管理', path: '/user', icon: User, children: [
            { name: '用戶查詢', path: '/user/list', icon: User }
        ]
    }
];

const expandedMenus = ref({});

const toggleMenu = (path) => {
    expandedMenus.value[path] = !expandedMenus.value[path];
};

const logout = () => {
    ElMessageBox.confirm('確定要登出系統嗎?', '登出確認', {
        confirmButtonText: '確認登出',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'glass-confirm'
    }).then(()  => { 
        localStorage.removeItem('current_username'); 
        localStorage.removeItem('current_id'); 
        localStorage.removeItem('current_role'); 
        localStorage.removeItem('jwt_token'); 
        ElMessage.success('您已安全登出');
        router.push("/login");
    }).catch(() => { })
}

const navigate = (path) => {
    router.push(path);
}

const isActive = (path) => {
    return route.path === path || route.path.startsWith(path);
}

const isExactActive = (path) => {
    return route.path === path;
}

const isDark = ref(localStorage.getItem('theme') === 'dark');
const handleToggle = () => {
  isDark.value = !isDark.value;
  // 假設 toggleTheme 會處理 document.documentElement.setAttribute('data-theme', ...)
  toggleTheme(isDark.value);
  // 同步更新 HTML tag 屬性以符合 CSS 選擇器
  document.documentElement.setAttribute('data-theme', isDark.value ? 'dark' : 'light');
};

onMounted(() => {
    currentUser.value = localStorage.getItem('current_username') || 'Admin';
    currentRole.value = localStorage.getItem('current_role') ;
    // 初始化主題屬性
    const initialTheme = localStorage.getItem('theme') || 'dark';
    document.documentElement.setAttribute('data-theme', initialTheme);
    
    menuItems.forEach(item => {
        if (item.children && isActive(item.path)) {
            expandedMenus.value[item.path] = true;
        }
    });
})
</script>

<template>
  <div class="app-container">
    <!-- 頂部英雄裝飾光暈 -->
    <div class="hero-glow"></div>

    <aside class="sidebar">
      <div class="logo-area">
        <div class="logo-icon">V</div>
        <h1 class="app-title gradient-text">Version Control</h1>
      </div>

      <nav class="nav-menu">
        <div v-for="item in menuItems" :key="item.path" class="menu-group">
          
          <div v-if="!item.children" 
               class="menu-item single-item" 
               :class="{ 'active': isActive(item.path) }"
               @click="navigate(item.path)">
            <el-icon class="icon"><component :is="item.icon" /></el-icon>
            <span class="label">{{ item.name }}</span>
          </div>

          <div v-else class="submenu-wrapper">
            <div class="menu-item parent-item" 
                 :class="{ 'active': isActive(item.path) }"
                 @click="toggleMenu(item.path)">
              <div class="left-content">
                <el-icon class="icon"><component :is="item.icon" /></el-icon>
                <span class="label">{{ item.name }}</span>
              </div>
              <el-icon class="arrow" :class="{ 'rotated': expandedMenus[item.path] }">
                <ArrowRight />
              </el-icon>
            </div>

            <div class="submenu-list" :class="{ 'expanded': expandedMenus[item.path] }">
              <div v-for="child in item.children" 
                   :key="child.path"
                   class="menu-item child-item"
                   :class="{ 'active': isExactActive(child.path) }"
                   @click="navigate(child.path)">
                <span class="dot"></span>
                <span class="label">{{ child.name }}</span>
              </div>
            </div>
          </div>

        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="user-card custom-card">
            <el-avatar :size="32" class="user-avatar">{{ currentUser.charAt(0).toUpperCase() }}</el-avatar>
            <div class="user-info">
                <span class="username">{{ currentUser }}</span>
                <span class="role">{{ currentRole == 'ADMIN' ? "管理員" : "使用者"}}</span>
            </div>
        </div>
      </div>
    </aside>

    <div class="main-wrapper">
      <header class="top-header navbar">
        <div class="header-left">
          <h2 class="page-title gradient-text">版本控制平台</h2>
        </div>
        <div class="header-right">
            <button @click="handleToggle" class="theme-toggle-btn-custom">
                <el-icon v-if="isDark"><Moon /></el-icon>
                <el-icon v-else><Sunny /></el-icon>
                <span>{{ isDark ? '黑曜模式' : '紙墨模式' }}</span>
            </button>

          <div class="divider"></div>
          
          <button class="action-btn logout-btn" @click="logout">
            <el-icon><SwitchButton/></el-icon>
            <a>退出</a>
          </button>
        </div>
      </header>

        <div class="content-area">
            <div class="glass-panel custom-card">
                <KeepAlive include="UserList">
                    <router-view />
                </KeepAlive>
            </div>
        </div>

      <footer class="app-footer">
        © 2026 版本控制管理系統 · <span class="gradient-text">Obsidian Edition</span>
      </footer>
    </div>
  </div>
</template>

<style scoped>
/* --- 主題變量定義 --- */
:root {
    /* 深色模式變數 - 黑曜石與螢光綠 */
    --bg-color: #050505;
    --panel-color: #121212;
    --text-primary: #ffffff;
    --text-secondary: #a0a0a0;
    --brand-color: #00f5d4; /* 螢光綠 */
    --brand-glow: rgba(0, 245, 212, 0.4);
    --accent-gold: linear-gradient(to right, #d4af37, #f3e5ab, #d4af37);
    --border-color: #222222;
    --card-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
    
    --sidebar-width: 260px;
    --header-height: 70px;
    --transition-speed: 0.3s;
}

/* 淺色模式變數 - 紙墨風格 */
:root[data-theme='light'] {
    --bg-color: #fcfcfc;
    --panel-color: #ffffff;
    --text-primary: #1a1a1a;
    --text-secondary: #4b5563;
    --brand-color: #006b5e; /* 深墨綠 */
    --brand-glow: rgba(0, 107, 94, 0.2);
    --accent-gold: linear-gradient(to right, #b8860b, #daa520, #b8860b);
    --border-color: #e5e7eb;
    --card-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.app-container {
    display: flex;
    width: 100vw;
    height: 100vh;
    background-color: var(--bg-color);
    color: var(--text-primary);
    font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
    overflow: hidden;
    position: relative;
    transition: background-color var(--transition-speed);
}

/* 金色漸層標題 */
.gradient-text {
    background: var(--accent-gold);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    font-weight: 800;
}

/* 英雄裝飾光暈 */
.hero-glow {
    position: absolute;
    top: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 100%;
    height: 500px;
    background: radial-gradient(circle at 50% 0%, var(--brand-glow) 0%, transparent 70%);
    pointer-events: none;
    z-index: 1;
}

/* --- 側邊欄 --- */
.sidebar {
    width: var(--sidebar-width);
    height: 100%;
    background: var(--panel-color);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    z-index: 10;
    flex-shrink: 0;
    transition: all var(--transition-speed);
}

.logo-area {
    height: var(--header-height);
    display: flex;
    align-items: center;
    padding: 0 24px;
    border-bottom: 1px solid var(--border-color);
}

.logo-icon {
    width: 34px;
    height: 34px;
    background: var(--brand-color);
    color: #000;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 900;
    font-size: 20px;
    margin-right: 12px;
    box-shadow: 0 0 15px var(--brand-glow);
}

.app-title {
    font-size: 17px;
    letter-spacing: -0.5px;
}

/* --- 導航菜單 --- */
.nav-menu {
    flex: 1;
    overflow-y: auto;
    padding: 24px 12px;
}

.menu-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.2s ease;
    color: var(--text-secondary);
    margin-bottom: 4px;
}

.menu-item:hover {
    background: rgba(128, 128, 128, 0.08);
    color: var(--text-primary);
}

.menu-item.active {
    background: var(--brand-glow);
    color: var(--brand-color);
    font-weight: 600;
}

.menu-item .icon {
    font-size: 18px;
    margin-right: 12px;
}

.arrow {
    font-size: 12px;
    transition: transform 0.3s;
}
.arrow.rotated {
    transform: rotate(90deg);
}

.submenu-list {
    max-height: 0;
    overflow: hidden;
    transition: max-height 0.3s ease-out;
    padding-left: 20px;
}
.submenu-list.expanded {
    max-height: 500px;
}

.child-item {
    padding: 10px 16px;
    font-size: 14px;
}
.child-item .dot {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background-color: currentColor;
    margin-right: 10px;
    opacity: 0.5;
}

/* --- 側邊欄底部 --- */
.sidebar-footer {
    padding: 16px;
    border-top: 1px solid var(--border-color);
}
.user-card {
    display: flex;
    align-items: center;
    padding: 12px;
}
.user-avatar {
    background: var(--accent-gold);
    color: #fff;
    margin-right: 10px;
    border: 1px solid rgba(255,255,255,0.1);
}
.username {
    font-size: 14px;
    font-weight: 600;
    display: block;
}
.role {
    font-size: 11px;
    color: var(--text-secondary);
    text-transform: uppercase;
    letter-spacing: 1px;
}

/* --- 主區域 --- */
.main-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    height: 100vh;
    z-index: 5;
}

.top-header {
    height: var(--header-height);
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 32px;
    background: rgba(18, 18, 18, 0.8);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid var(--border-color);
}

:root[data-theme='light'] .top-header {
    background: rgba(255, 255, 255, 0.8);
}

.header-right {
    display: flex;
    align-items: center;
    gap: 20px;
}

.theme-toggle-btn-custom {
    background: transparent;
    border: 1px solid var(--border-color);
    color: var(--text-primary);
    padding: 8px 16px;
    border-radius: 20px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    transition: all 0.2s;
}
.theme-toggle-btn-custom:hover {
    border-color: var(--brand-color);
    box-shadow: 0 0 10px var(--brand-glow);
}

.action-btn {
    background: transparent;
    border: none;
    color: var(--text-secondary);
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
}
.action-btn:hover { color: var(--text-primary); }
.logout-btn:hover { color: #ff4d4d; }

.divider {
    width: 1px;
    height: 16px;
    background: var(--border-color);
}

/* --- 內容區域 --- */
.content-area {
    flex: 1;
    padding: 24px;
    overflow-y: auto;
}

.custom-card {
    background-color: var(--panel-color) !important;
    border: 1px solid var(--border-color) !important;
    border-radius: 12px !important;
    box-shadow: var(--card-shadow) !important;
    color: var(--text-primary) !important;
}

.glass-panel {
    min-height: 100%;
    padding: 30px;
}

.app-footer {
    padding: 16px 32px;
    font-size: 12px;
    color: var(--text-secondary);
    text-align: center;
    border-top: 1px solid var(--border-color);
    background: var(--panel-color);
}

/* 滾動條美化 */
::-webkit-scrollbar {
    width: 6px;
}
::-webkit-scrollbar-track {
    background: transparent;
}
::-webkit-scrollbar-thumb {
    background: var(--border-color);
    border-radius: 10px;
}
::-webkit-scrollbar-thumb:hover {
    background: var(--text-secondary);
}

/* Element Plus 覆寫確保表格在深色模式下正常 */
:deep(.el-table) {
    --el-table-bg-color: var(--panel-color);
    --el-table-tr-bg-color: var(--panel-color);
    --el-table-header-bg-color: var(--bg-color);
    --el-table-border-color: var(--border-color);
    --el-table-text-color: var(--text-primary);
}
</style>