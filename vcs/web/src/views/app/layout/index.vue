<script setup>
import { onMounted, ref, computed , onActivated } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus' 
import { useRouter, useRoute } from 'vue-router'
import { toggleTheme } from '@/utils/theme';
import { 
    EditPen, SwitchButton, Promotion, Message, 
    User, HomeFilled, Avatar, Money, TrendCharts,
    ArrowRight, ArrowDown
} from '@element-plus/icons-vue';

const router = useRouter();
const route = useRoute(); // 用於監聽路由變化
const currentUser = ref('');
// 假設您在這裡請求部門列表或版本歷史
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

const logout = () => {
    ElMessageBox.confirm('確定要登出系統嗎?', '登出確認', {
        confirmButtonText: '確認登出',
        cancelButtonText: '取消',
        type: 'warning',
        customClass: 'glass-confirm' // 自定義樣式類名
    }).then(()  => { 
        localStorage.removeItem('current_username'); 
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

    <aside class="sidebar">
      <div class="logo-area">
        <div class="logo-icon">V</div>
        <h1 class="app-title">Version Control</h1>
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
                <span class="role">管理員</span>
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
            
            <button @click="handleToggle" class="theme-btn">
                <el-icon v-if="isDark"><Moon /></el-icon>
                <el-icon v-else><Sunny /></el-icon>
                    切換主題
            </button>

          <button class="action-btn">
            <el-icon><EditPen /></el-icon>
            <a>修改密碼</a>
          </button>
          <div class="divider"></div>
          <button class="action-btn logout-btn" @click="logout">
            <el-icon><SwitchButton/></el-icon>
            <a>退出</a>
          </button>
        </div>
      </header>

        <div class="content-area">
            <KeepAlive include="UserList">
                <router-view />
            </KeepAlive>
        </div>

      <footer class="app-footer">
        © 2025 版本控制管理系統
      </footer>
    </div>
  </div>
</template>

<style scoped>
/* --- 全局變量定義 --- */
:root {
    --primary-color: #6366f1; /* Indigo */
    --secondary-color: #ec4899; /* Pink */
    --bg-dark: #0f172a;
    --glass-bg: rgba(30, 41, 59, 0.7);
    --glass-border: rgba(255, 255, 255, 0.08);
    --text-main: #f1f5f9;
    --text-sub: #94a3b8;
    --sidebar-width: 260px;
    --header-height: 70px;
    --transition-speed: 0.3s;
}

/* 確保全屏且無滾動條 (內容區內部滾動) */
.app-container {
    display: flex;
    width: 100vw;
    height: 100vh;
    background-color: var(--bg-dark);
    color: var(--text-main);
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
    background: var(--glass-bg);
    backdrop-filter: blur(12px);
    border-right: 1px solid var(--glass-border);
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
    border-bottom: 1px solid var(--glass-border);
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
    box-shadow: 0 4px 12px rgba(99, 102, 241, 0.3);
}

.app-title {
    font-size: 16px;
    font-weight: 600;
    letter-spacing: 0.5px;
    background: linear-gradient(to right, #fff, #cbd5e1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
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
    color: var(--text-sub);
    position: relative;
    user-select: none;
}

.menu-item:hover {
    background: rgba(255, 255, 255, 0.05);
    color: #fff;
}

.menu-item.active {
    background: rgba(99, 102, 241, 0.15); /* Primary color low opacity */
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
    background-color: var(--text-sub);
    margin-right: 10px;
    opacity: 0.5;
    transition: all 0.2s;
}
.child-item:hover .dot, .child-item.active .dot {
    background-color: var(--secondary-color);
    opacity: 1;
    box-shadow: 0 0 8px var(--secondary-color);
}

/* 側邊欄底部 */
.sidebar-footer {
    padding: 20px;
    border-top: 1px solid var(--glass-border);
}
.user-card {
    display: flex;
    align-items: center;
    background: rgba(0,0,0,0.2);
    padding: 10px;
    border-radius: 12px;
}
.user-avatar {
    background: var(--secondary-color);
    margin-right: 12px;
}
.user-info {
    display: flex;
    flex-direction: column;
}
.username {
    font-size: 14px;
    font-weight: 600;
    color: #fff;
}
.role {
    font-size: 12px;
    color: var(--text-sub);
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
    /* 讓 Header 懸浮 */
    background: rgba(15, 23, 42, 0.5); 
    backdrop-filter: blur(8px);
    position: sticky;
    top: 0;
    z-index: 50;
}

.page-title {
    font-size: 20px;
    font-weight: 500;
}

.header-right {
    display: flex;
    align-items: center;
    gap: 16px;
}

.action-btn {
    background: transparent;
    border: none;
    color: var(--text-sub);
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    border-radius: 8px;
    transition: all 0.2s;
    font-size: 14px;
}
.action-btn:hover {
    background: rgba(255,255,255,0.05);
    color: #fff;
}
.logout-btn:hover {
    background: rgba(239, 68, 68, 0.15); /* Red hint */
    color: #ef4444;
}

.divider {
    width: 1px;
    height: 16px;
    background: var(--glass-border);
}

/* --- 內容與 Footer --- */
.content-area {
    flex: 1;            /* 佔滿所有剩餘空間，將 Footer 推到底部 */
    padding: 24px 32px;
    overflow-y: auto; /* 內容區滾動 */

}


/* 自定義滾動條 (Chrome/Safari) */
.content-area::-webkit-scrollbar {
    width: 6px;
}
.content-area::-webkit-scrollbar-track {
    background: transparent;
}
.content-area::-webkit-scrollbar-thumb {
    background-color: rgba(255,255,255,0.2);
    border-radius: 20px;
}

/* 玻璃面板容器 (給 router-view 內的頁面使用) */
.glass-panel {
    background: var(--glass-bg);
    border: 1px solid var(--glass-border);
    border-radius: 16px;
    padding: 30px;
    min-height: 100%; /* 撐開高度 */
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
}

.app-footer {
    flex-shrink: 0; /* 防止 Footer 被壓縮 */
    /* ... 其他樣式保持不變 */
    border-top: 1px solid var(--glass-border); /* 確保邊框顏色正確 */
    background: rgba(15, 23, 42, 0.8); /* 稍微加深背景色 */
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