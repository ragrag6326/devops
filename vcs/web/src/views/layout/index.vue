<script setup>
import { onMounted, ref, computed , onActivated } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus' 
import { useRouter, useRoute } from 'vue-router'
import { toggleTheme } from '@/utils/theme';
import { 
    EditPen, SwitchButton, Promotion, Message, 
    User, HomeFilled, Avatar, Money, TrendCharts, Menu,
    ArrowRight, ArrowDown, Sunny, Moon
} from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute(); // 用於監聽路由變化
const currentUser = ref('');
const currentRole = ref('');
// 請求部門列表或版本歷史
const fetchData = () => {
    console.log('--- 數據正在被請求/刷新中 ---');
};

// 菜單數據
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

// 簡單的展開/收合狀態管理 (可選優化)
const expandedMenus = ref({});

const toggleMenu = (path) => {
    expandedMenus.value[path] = !expandedMenus.value[path];
};

// 側邊欄展開 / 收合
const isCollapsed = ref(false);
const toggleSidebar = () => {
    isCollapsed.value = !isCollapsed.value;
};


// 密碼修改
// const changePassword = () => {

// }

const logout = () => {
    ElMessageBox.confirm('確定要登出系統嗎?', '登出確認', {
        confirmButtonText: '確認登出',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'glass-confirm' // 自定義樣式類名
    }).then(()  => { 
        localStorage.removeItem('current_username'); 
        localStorage.removeItem('current_id'); 
        localStorage.removeItem('current_role'); 
        localStorage.removeItem('jwt_token'); 
        ElMessage.success('您已安全登出');
        router.push("/login");
    }).catch(() => { 
        // 取消操作
    })
}

const navigate = (path) => {
    router.push(path);
}

// 判斷是否激活 (包含父級高亮邏輯)
const isActive = (path) => {
    return route.path === path || route.path.startsWith(path);
}

// 判斷是否是當前精確路由 (用於子菜單)
const isExactActive = (path) => {
    return route.path === path;
}

// 狀態控制
const isDark = ref(localStorage.getItem('theme') === 'dark');
const handleToggle = () => {
  isDark.value = !isDark.value;
  toggleTheme(isDark.value);
};




onMounted(() => {
    console.log('--- 組件首次創建完成 ---');
    currentUser.value = localStorage.getItem('current_username') || 'Admin';
    currentRole.value = localStorage.getItem('current_role') ;
    // 預設展開當前激活的父菜單
    menuItems.forEach(item => {
        if (item.children && isActive(item.path)) {
            expandedMenus.value[item.path] = true;
        }
    });
})


</script>

<template>
  <div class="app-container">
    <div class="bg-glow bg-glow-1"></div>
    <div class="bg-glow bg-glow-2"></div>

    <aside class="sidebar" :class="{ 'sidebar-collapsed': isCollapsed }">
      <div class="logo-area">
        <div class="logo-main">
          <div class="logo-icon">V</div>
          <h1 class="app-title">Version Control</h1>
        </div>
        <button
          class="sidebar-toggle-btn"
          @click="toggleSidebar"
          :title="isCollapsed ? '展開側邊欄' : '收合側邊欄'"
        >
          <el-icon class="toggle-icon"><Menu /></el-icon>
          <span class="toggle-text">{{ isCollapsed ? '展開' : '收合' }}</span>
        </button>
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
        <div class="user-card">
            <el-avatar :size="36" class="user-avatar">{{ currentUser.charAt(0).toUpperCase() }}</el-avatar>
            <div class="user-info">
                <span class="username">{{ currentUser }}</span>
                <span class="role">{{ currentRole == 'ADMIN' ? "管理員" : "一般使用者"}}</span>
            </div>
        </div>
      </div>
    </aside>

    <div class="main-wrapper">
      
      <header class="top-header">
        <div class="header-left">
          <h2 class="page-title">{{ '版本控制平台' }}</h2>
        </div>
        <div class="header-right">
            <button @click="handleToggle" class="header-btn theme-btn" :title="isDark ? '切換至淺色' : '切換至深色'">
                <el-icon class="header-btn-icon"><Moon v-if="isDark" /><Sunny v-else /></el-icon>
                <span class="header-btn-text">切換主題</span>
            </button>
            
          <div class="header-divider"></div>
          <button class="header-btn logout-btn" @click="logout">
            <el-icon class="header-btn-icon"><SwitchButton /></el-icon>
            <span class="header-btn-text">退出</span>
          </button>
        </div>
      </header>

        <div class="content-area">
            <KeepAlive include="UserList">
                <router-view />
            </KeepAlive>
        </div>

      <footer class="app-footer">
        © 2026 版本控制管理系統 v2
      </footer>
    </div>
  </div>
</template>

<style scoped>
/* 使用主題變數，不覆寫 :root，以支援深淺主題 */
.app-container {
    --sidebar-width: 260px;
    --header-height: 70px;
    --transition-speed: 0.3s;
    display: flex;
    width: 100vw;
    height: 100vh;
    background-color: var(--bg);
    color: var(--text);
    font-family: 'Inter', 'Helvetica Neue', Arial, sans-serif;
    overflow: hidden;
    position: relative;
}

/* --- 背景光暈效果 --- */
.bg-glow {
    position: absolute;
    width: 600px;
    height: 600px;
    border-radius: 50%;
    filter: blur(100px);
    opacity: 0.15;
    z-index: 0;
    pointer-events: none;
}
.bg-glow-1 { top: -100px; left: -100px; background: var(--primary-color); }
.bg-glow-2 { bottom: -100px; right: -100px; background: var(--secondary-color); }

/* --- 側邊欄樣式 --- */
.sidebar {
    width: var(--sidebar-width);
    height: 100%;
    background: var(--panel);
    backdrop-filter: blur(12px);
    border-right: 1px solid var(--border-color);
    display: flex;
    flex-direction: column;
    z-index: 10;
    flex-shrink: 0;
    transition: all var(--transition-speed);
}

/* 收合狀態：縮小寬度並隱藏文字，只留圖示 */
.sidebar-collapsed {
    width: 72px;
}
.sidebar-collapsed .logo-area .app-title {
    display: none;
}
.sidebar-collapsed .menu-item .label {
    display: none;
}
.sidebar-collapsed .menu-item {
    justify-content: center;
}
.sidebar-collapsed .menu-item .icon {
    margin-right: 0;
}
.sidebar-collapsed .submenu-list {
    display: none;
}
.sidebar-collapsed .user-info {
    display: none;
}
.sidebar-collapsed .user-card {
    justify-content: center;
}

.logo-area {
    height: var(--header-height);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 16px 0 24px;
    border-bottom: 1px solid var(--border-color);
}

.logo-main {
    display: flex;
    align-items: center;
}

.logo-icon {
    width: 32px;
    height: 32px;
    background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    font-size: 18px;
    margin-right: 12px;
    box-shadow: 0 4px 12px color-mix(in srgb, var(--brand) 35%, transparent);
}

.app-title {
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 0.5px;
    color: var(--muted);
}

/* --- 導航菜單 --- */
.nav-menu {
    flex: 1;
    overflow-y: auto;
    padding: 20px 16px;
}

.menu-group {
    margin-bottom: 4px;
}

.menu-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border-radius: 12px;
    cursor: pointer;
    transition: all 0.2s ease;
    color: var(--muted);
    position: relative;
    user-select: none;
}

.menu-item:hover {
    background: var(--panel-alt);
    color: var(--text);
}

.menu-item.active {
    background: var(--brand-muted, rgba(99, 102, 241, 0.15));
    color: var(--primary-color);
    font-weight: 600;
}

.menu-item.active::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    height: 20px;
    width: 3px;
    background: var(--primary-color);
    border-radius: 0 4px 4px 0;
}

.menu-item .icon {
    font-size: 18px;
    margin-right: 12px;
}

/* 子菜單特定樣式 */
.parent-item {
    justify-content: space-between;
}
.left-content {
    display: flex;
    align-items: center;
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
    transition: max-height 0.4s ease-in-out;
    padding-left: 12px; /* 縮進 */
}
.submenu-list.expanded {
    max-height: 500px; /* 足夠大的高度以容納子菜單 */
}

.child-item {
    padding: 10px 16px 10px 38px; /* 增加左內邊距 */
    font-size: 14px;
}
.child-item .dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background-color: var(--muted);
    margin-right: 10px;
    opacity: 0.6;
    transition: all 0.2s;
}
.child-item:hover .dot, .child-item.active .dot {
    background-color: var(--primary-color);
    opacity: 1;
    box-shadow: 0 0 8px color-mix(in srgb, var(--primary-color) 50%, transparent);
}

/* 側邊欄底部 */
.sidebar-footer {
    padding: 20px;
    border-top: 1px solid var(--border-color);
}
.user-card {
    display: flex;
    align-items: center;
    background: var(--panel-alt);
    padding: 10px;
    border-radius: 12px;
}
.user-avatar {
    background: var(--primary-color);
    margin-right: 12px;
}
.user-info {
    display: flex;
    flex-direction: column;
}
.username {
    font-size: 14px;
    font-weight: 600;
    color: var(--text);
}
.role {
    font-size: 12px;
    color: var(--muted);
}

/* --- 主區域 --- */
.main-wrapper {
    flex: 1;
    display: flex;
    flex-direction: column;
    height: 100vh;
    overflow: hidden;
    position: relative;
    z-index: 5;
    /* 這裡使用 Flex 讓 Footer 沉底，Content 自適應 */
    
}

.top-header {
    height: var(--header-height);
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 32px;
    background: var(--navbar-bg);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid var(--border-color);
    position: sticky;
    top: 0;
    z-index: 50;
}

.page-title {
    font-size: 20px;
    font-weight: 600;
    color: var(--text);
}

.header-right {
    display: flex;
    align-items: center;
    gap: 12px;
}

/* 側邊欄收合按鈕樣式（logo 右側） */
.sidebar-toggle-btn {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    padding: 6px 10px;
    border-radius: 999px;
    border: 1px solid var(--border-color);
    background: var(--panel-alt);
    color: var(--muted);
    font-size: 12px;
    cursor: pointer;
    transition: all 0.2s ease;
}
.sidebar-toggle-btn:hover {
    border-color: var(--primary-color);
    color: var(--primary-color);
    box-shadow: 0 0 10px color-mix(in srgb, var(--primary-color) 30%, transparent);
}
.toggle-icon {
    font-size: 16px;
}
.toggle-text {
    line-height: 1;
}
/* 收合狀態：只顯示圖示，文字隱藏，仍保留提示 title */
.sidebar-collapsed .sidebar-toggle-btn .toggle-text {
    display: none;
}

/* 頂部按鈕：切換主題、退出 */
.header-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 10px 16px;
    border-radius: 10px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.2s ease;
    border: 1px solid var(--border-color);
    background: var(--panel);
    color: var(--text);
}
.header-btn-icon {
    font-size: 18px;
}
.header-btn-text {
    line-height: 1;
}
.theme-btn:hover {
    border-color: var(--primary-color);
    color: var(--primary-color);
    background: var(--brand-muted, var(--panel-alt));
    box-shadow: 0 2px 8px color-mix(in srgb, var(--primary-color) 18%, transparent);
}
.logout-btn:hover {
    border-color: var(--danger);
    color: var(--danger);
    background: color-mix(in srgb, var(--danger) 10%, transparent);
    box-shadow: 0 2px 8px color-mix(in srgb, var(--danger) 18%, transparent);
}
.header-divider {
    width: 1px;
    height: 20px;
    background: var(--border-color);
    flex-shrink: 0;
}

/* --- 內容與 Footer --- */
.content-area {
    flex: 1;            /* 佔滿所有剩餘空間，將 Footer 推到底部 */
    padding: 24px 32px;
    overflow-y: auto; /* 內容區滾動 */
    /* 背景使用與整體一致的深色漸層，讓中間內容區更有層次感 */
    background:
        radial-gradient(circle at 0% 0%, color-mix(in srgb, var(--brand) 12%, transparent) 0%, transparent 55%),
        radial-gradient(circle at 100% 100%, color-mix(in srgb, var(--secondary-color) 14%, transparent) 0%, transparent 55%),
        var(--bg);
}


/* 自定義滾動條 (Chrome/Safari) */
.content-area::-webkit-scrollbar {
    width: 6px;
}
.content-area::-webkit-scrollbar-track {
    background: transparent;
}
.content-area::-webkit-scrollbar-thumb {
    background-color: var(--border-color);
    border-radius: 20px;
}

/* 玻璃面板容器 (給 router-view 內的頁面使用) */
.glass-panel {
    background: var(--panel);
    border: 1px solid var(--border-color);
    border-radius: 16px;
    padding: 30px;
    min-height: 100%;
    box-shadow: var(--shadow-md);
}

.app-footer {
    flex-shrink: 0;
    border-top: 1px solid var(--border-color);
    background: var(--panel);
    color: var(--muted);
    font-size: 13px;
}

/* --- 頁面切換動畫 --- */
.fade-slide-enter-active,
.fade-slide-leave-active {
    transition: opacity 0.3s ease, transform 0.3s ease;
}

.fade-slide-enter-from {
    opacity: 0;
    transform: translateY(10px);
}

.fade-slide-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}


</style>