<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter, useRoute } from 'vue-router'
import { toggleTheme } from '@/utils/theme'
import { mailUnreadCount, refreshMailUnreadCount } from '@/utils/mailState'

const router = useRouter()
const route = useRoute()

const currentUser = ref('')
const currentRole = ref('')
const isCollapsed = ref(false)

const menuItems = [
  { name: '首頁', path: '/homepage', icon: '⌂' },
  {
    name: '版本管理', path: '/version', icon: '◈',
    children: [
      { name: '版本紀錄', path: '/version/history' },
    ]
  },
  {
    name: 'MR / 程式碼審核', path: '/mr', icon: '⎇',
    children: [
      { name: '歷史紀錄查詢', path: '/mr/history' },
      { name: 'AI 程式碼審核', path: '/mr/review' },
    ]
  },
  { name: '上班累了看這邊', path: '/relax', icon: '☕' },
  {
    name: '系統', path: '/system', icon: '⚙',
    children: [
      { name: '狀態監控', path: '/system/monitor' },
      { name: '日誌查詢', path: '/system/log_query' },
      { name: '日誌智能分析', path: '/system/log' },
      { name: '專案管理', path: '/system/project' },
      { name: '部署對照表管理', path: '/system/deploy-registry' },
    ]
  },
  {
    name: '用戶管理', path: '/user', icon: '◉',
    children: [
      { name: '用戶查詢', path: '/user/list' },
    ]
  }
]

const expandedMenus = ref({})

const pageTitle = computed(() => route.meta?.title || '版本控制平台')

const toggleMenu = (path) => {
  expandedMenus.value[path] = !expandedMenus.value[path]
}

const toggleSidebar = () => {
  isCollapsed.value = !isCollapsed.value
}

const logout = () => {
  ElMessageBox.confirm('確定要登出系統嗎？', '登出確認', {
    confirmButtonText: '確認登出',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    localStorage.removeItem('current_username')
    localStorage.removeItem('current_id')
    localStorage.removeItem('current_role')
    localStorage.removeItem('jwt_token')
    ElMessage.success('已安全登出')
    router.push('/login')
  }).catch(() => {})
}

const navigate = (path) => {
  router.push(path)
}

const isActive = (path) => {
  return route.path === path || route.path.startsWith(path + '/')
}

const isExactActive = (path) => route.path === path

const isDark = ref(localStorage.getItem('theme') !== 'light')

const handleToggle = () => {
  isDark.value = !isDark.value
  toggleTheme(isDark.value)
}

onMounted(() => {
  currentUser.value = localStorage.getItem('current_username') || 'Admin'
  currentRole.value = localStorage.getItem('current_role') || ''
  menuItems.forEach((item) => {
    if (item.children && isActive(item.path)) {
      expandedMenus.value[item.path] = true
    }
  })
  // 登入後立即算出信箱未讀數，sidebar badge 不需等到進入 /relax 才顯示
  refreshMailUnreadCount()
})
</script>

<template>
  <div
    class="app-shell"
    :class="{ 'theme-dark': isDark, 'sidebar-collapsed': isCollapsed }"
  >
    <div class="bg-orb bg-orb-1" aria-hidden="true"></div>
    <div class="bg-orb bg-orb-2" aria-hidden="true"></div>

    <aside class="sidebar">
      <div class="sidebar-header">
        <div class="logo-wrap">
          <span class="logo-mark">VC</span>
          <span class="logo-text">Version<br><strong>Control</strong></span>
        </div>
        <button
          class="collapse-btn"
          :title="isCollapsed ? '展開' : '收合'"
          @click="toggleSidebar"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
            <line x1="3" y1="6" x2="21" y2="6" />
            <line x1="3" y1="12" x2="21" y2="12" />
            <line x1="3" y1="18" x2="21" y2="18" />
          </svg>
        </button>
      </div>

      <nav class="nav-list">
        <template v-for="item in menuItems" :key="item.path">
          <div
            v-if="!item.children"
            class="nav-item"
            :class="{ active: isActive(item.path) }"
            @click="navigate(item.path)"
          >
            <span class="nav-icon">{{ item.icon }}</span>
            <span class="nav-label">{{ item.name }}</span>
            <span
              v-if="item.path === '/relax' && mailUnreadCount > 0"
              class="nav-badge"
            >{{ mailUnreadCount }}</span>
          </div>

          <div v-else class="nav-group">
            <div
              class="nav-item parent"
              :class="{ active: isActive(item.path) }"
              @click="toggleMenu(item.path)"
            >
              <span class="nav-icon">{{ item.icon }}</span>
              <span class="nav-label">{{ item.name }}</span>
              <span class="nav-arrow" :class="{ open: expandedMenus[item.path] }">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="9 18 15 12 9 6" />
                </svg>
              </span>
            </div>
            <div class="nav-children" :class="{ open: expandedMenus[item.path] }">
              <div
                v-for="child in item.children"
                :key="child.path"
                class="nav-child"
                :class="{ active: isExactActive(child.path) }"
                @click="navigate(child.path)"
              >
                <span class="child-dot"></span>
                <span>{{ child.name }}</span>
              </div>
            </div>
          </div>
        </template>
      </nav>

      <div class="sidebar-user">
        <div class="user-avatar">{{ currentUser.charAt(0).toUpperCase() }}</div>
        <div class="user-meta">
          <span class="user-name">{{ currentUser }}</span>
          <span class="user-role">{{ currentRole === 'ADMIN' ? '管理員' : '一般使用者' }}</span>
        </div>
      </div>
    </aside>

    <div class="main-area">
      <header class="top-bar">
        <div class="top-bar-left">
          <h2 class="page-heading">{{ pageTitle }}</h2>
        </div>
        <div class="top-bar-right">
          <button
            class="icon-btn theme-toggle"
            :title="isDark ? '切換至淺色模式' : '切換至深色模式'"
            @click="handleToggle"
          >
            <span class="toggle-track" :class="{ checked: isDark }">
              <span class="toggle-thumb">
                <svg v-if="isDark" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z" />
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                  <circle cx="12" cy="12" r="5" />
                  <line x1="12" y1="1" x2="12" y2="3" />
                  <line x1="12" y1="21" x2="12" y2="23" />
                  <line x1="4.22" y1="4.22" x2="5.64" y2="5.64" />
                  <line x1="18.36" y1="18.36" x2="19.78" y2="19.78" />
                  <line x1="1" y1="12" x2="3" y2="12" />
                  <line x1="21" y1="12" x2="23" y2="12" />
                  <line x1="4.22" y1="19.78" x2="5.64" y2="18.36" />
                  <line x1="18.36" y1="5.64" x2="19.78" y2="4.22" />
                </svg>
              </span>
            </span>
            <span class="icon-btn-label">{{ isDark ? '深色' : '淺色' }}</span>
          </button>

          <div class="divider-v"></div>

          <button class="icon-btn logout-btn" @click="logout">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
              <polyline points="16 17 21 12 16 7" />
              <line x1="21" y1="12" x2="9" y2="12" />
            </svg>
            <span class="icon-btn-label">退出</span>
          </button>
        </div>
      </header>

      <main class="content-view">
        <KeepAlive include="UserList">
          <router-view />
        </KeepAlive>
      </main>

      <footer class="app-footer">
        <span>© 2026 版本控制管理系統</span>
        <span class="footer-sep">·</span>
        <span class="footer-brand">DevOps Platform v2</span>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.app-shell {
  --sidebar-w: 248px;
  --header-h: 64px;
  --sidebar-collapsed-w: 72px;
  display: flex;
  width: 100vw;
  height: 100vh;
  background: var(--bg);
  color: var(--text);
  font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  overflow: hidden;
  position: relative;
}

.bg-orb {
  position: fixed;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.12;
  pointer-events: none;
  z-index: 0;
  transition: opacity 0.4s;
}

.bg-orb-1 {
  width: 480px;
  height: 480px;
  top: -120px;
  left: -80px;
  background: radial-gradient(circle, var(--brand), transparent 70%);
}

.bg-orb-2 {
  width: 400px;
  height: 400px;
  bottom: -80px;
  right: -60px;
  background: radial-gradient(circle, var(--secondary-color), transparent 70%);
}

.sidebar {
  width: var(--sidebar-w);
  height: 100%;
  flex-shrink: 0;
  background: var(--panel);
  border-right: 1px solid var(--border-color);
  display: flex;
  flex-direction: column;
  z-index: 20;
  transition: width 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.app-shell.sidebar-collapsed .sidebar {
  width: var(--sidebar-collapsed-w);
}

.sidebar-header {
  height: var(--header-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px 0 20px;
  border-bottom: 1px solid var(--border-color);
  flex-shrink: 0;
}

.logo-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  overflow: hidden;
  white-space: nowrap;
}

.logo-mark {
  width: 34px;
  height: 34px;
  background: linear-gradient(135deg, var(--brand), var(--secondary-color));
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  font-size: 13px;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 4px 12px var(--brand-glow, rgba(79, 70, 229, 0.3));
  letter-spacing: 0.02em;
}

.logo-text {
  font-size: 12px;
  color: var(--muted);
  line-height: 1.3;
  transition: opacity 0.2s;
}

.logo-text strong {
  color: var(--text);
  font-weight: 700;
}

.app-shell.sidebar-collapsed .logo-text {
  opacity: 0;
  width: 0;
}

.collapse-btn {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  border: 1px solid var(--border-color);
  background: transparent;
  color: var(--muted);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.collapse-btn:hover {
  border-color: var(--brand);
  color: var(--brand);
  background: var(--brand-muted);
}

.collapse-btn svg {
  width: 16px;
  height: 16px;
}

.nav-list {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 16px 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.nav-list::-webkit-scrollbar {
  width: 3px;
}

.nav-list::-webkit-scrollbar-thumb {
  background: var(--border-color);
  border-radius: 99px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.18s ease;
  color: var(--muted);
  font-size: 14px;
  font-weight: 500;
  position: relative;
  white-space: nowrap;
  overflow: visible;
  user-select: none;
}

.nav-item:hover {
  background: var(--panel-alt);
  color: var(--text);
}

.nav-item.active {
  background: var(--brand-muted);
  color: var(--brand);
  font-weight: 600;
}

.nav-icon {
  font-size: 16px;
  flex-shrink: 0;
  width: 22px;
  text-align: center;
}

.nav-label {
  flex: 1;
  transition: opacity 0.2s;
}

.nav-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 99px;
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  flex-shrink: 0;
  box-shadow: 0 0 6px rgba(239, 68, 68, 0.5);
}

.app-shell.sidebar-collapsed .nav-badge {
  position: absolute;
  top: 6px;
  right: 6px;
  min-width: 14px;
  height: 14px;
  padding: 0 3px;
  font-size: 9px;
}

.app-shell.sidebar-collapsed .nav-label {
  opacity: 0;
}

.app-shell.sidebar-collapsed .nav-arrow {
  display: none;
}

.nav-arrow {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.25s ease;
  flex-shrink: 0;
}

.nav-arrow svg {
  width: 14px;
  height: 14px;
}

.nav-arrow.open {
  transform: rotate(90deg);
}

.nav-children {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.nav-children.open {
  max-height: 300px;
}

.app-shell.sidebar-collapsed .nav-children {
  display: none;
}

.nav-child {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px 8px 38px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: var(--muted);
  transition: all 0.18s;
  white-space: nowrap;
  user-select: none;
}

.nav-child:hover {
  background: var(--panel-alt);
  color: var(--text);
}

.nav-child.active {
  color: var(--brand);
  background: var(--brand-muted);
  font-weight: 600;
}

.child-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--muted);
  flex-shrink: 0;
  transition: background 0.2s, box-shadow 0.2s;
}

.nav-child.active .child-dot,
.nav-child:hover .child-dot {
  background: var(--brand);
  box-shadow: 0 0 6px color-mix(in srgb, var(--brand) 50%, transparent);
}

.sidebar-user {
  padding: 14px;
  border-top: 1px solid var(--border-color);
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

.user-avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--brand), var(--secondary-color));
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 2px 8px var(--brand-glow, rgba(79, 70, 229, 0.25));
}

.user-meta {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  white-space: nowrap;
  transition: opacity 0.2s;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text);
}

.user-role {
  font-size: 11px;
  color: var(--muted);
  margin-top: 1px;
}

.app-shell.sidebar-collapsed .user-meta {
  opacity: 0;
  width: 0;
}

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  position: relative;
  z-index: 5;
}

.top-bar {
  height: var(--header-h);
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  background: var(--navbar-bg);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 30;
}

.page-heading {
  font-size: 17px;
  font-weight: 600;
  color: var(--text);
  margin: 0;
  letter-spacing: -0.01em;
}

.top-bar-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 13px;
  border-radius: 9px;
  border: 1px solid var(--border-color);
  background: var(--panel);
  color: var(--muted);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.icon-btn svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.icon-btn-label {
  line-height: 1;
  color: var(--muted);
  font-size: 12px;
}

.theme-toggle {
  gap: 10px;
  border: none;
  background: transparent;
  padding: 6px 8px;
}

.toggle-track {
  width: 42px;
  height: 22px;
  border-radius: 99px;
  background: var(--border-color);
  border: 1px solid var(--border-color);
  position: relative;
  display: flex;
  align-items: center;
  padding: 2px;
  transition: all 0.25s ease;
  cursor: pointer;
  flex-shrink: 0;
}

.toggle-track.checked {
  background: var(--brand-muted);
  border-color: var(--brand);
}

.toggle-thumb {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: var(--muted);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: absolute;
  left: 2px;
}

.toggle-track.checked .toggle-thumb {
  left: calc(100% - 18px);
  background: var(--brand);
  color: #fff;
}

.toggle-thumb svg {
  width: 10px;
  height: 10px;
}

.theme-toggle:hover .toggle-track {
  opacity: 0.8;
}

.logout-btn:hover {
  border-color: var(--danger);
  color: var(--danger);
  background: color-mix(in srgb, var(--danger) 8%, transparent);
}

.divider-v {
  width: 1px;
  height: 18px;
  background: var(--border-color);
  flex-shrink: 0;
}

.content-view {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 24px 28px;
  background:
    radial-gradient(ellipse at 0% 0%, color-mix(in srgb, var(--brand) 6%, transparent) 0%, transparent 50%),
    radial-gradient(ellipse at 100% 100%, color-mix(in srgb, var(--secondary-color) 5%, transparent) 0%, transparent 50%),
    var(--bg);
}

.app-footer {
  flex-shrink: 0;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border-top: 1px solid var(--border-color);
  background: var(--panel);
  color: var(--muted);
  font-size: 12px;
}

.footer-sep {
  opacity: 0.4;
}

.footer-brand {
  color: var(--brand);
  font-weight: 500;
}
</style>
