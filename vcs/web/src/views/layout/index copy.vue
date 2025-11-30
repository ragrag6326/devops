<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
// 引入需要的 icon
import { 
  EditPen, SwitchButton, Promotion, Message, 
  User, HomeFilled, Avatar, Money, TrendCharts 
} from '@element-plus/icons-vue';

const currentUser = ref('');
const router = useRouter();

const logout = () => {
    ElMessageBox.confirm('確定要登出嗎 ?', '提示', {
        cancelButtonText: '取消',
        confirmButtonText: '登出',
        type: 'warning',
        background: true, // 讓彈窗稍微好看一點
        customClass: 'dark-message-box' // 如果你有設定 EP 的暗黑模式
    }).then(() => { 
        localStorage.removeItem('current_username'); 
        localStorage.removeItem('jwt_token'); 
        ElMessage.success('退出成功');
        router.push("/login");
    }).catch(() => { 
        ElMessage.info('已取消退出登入'); 
    })
}

onMounted(() => {
    currentUser.value = localStorage.getItem('current_username');
})
</script>

<template>
  <div class="min-h-screen bg-slate-900 text-slate-200 font-sans flex flex-col">
    
    <header class="fixed top-0 left-0 right-0 h-16 z-50 flex items-center justify-between px-6 
                   bg-slate-900/80 backdrop-blur-md border-b border-white/5 shadow-sm">
      
      <div class="flex items-center gap-3">
        <div class="w-8 h-8 rounded-lg bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg shadow-purple-500/20">
          <span class="text-white font-bold text-lg">V</span>
        </div>
        <span class="text-xl font-bold bg-gradient-to-r from-white to-slate-400 bg-clip-text text-transparent tracking-wide">
          版本控制系統
        </span>
      </div>

      <div class="flex items-center gap-6">
        <a href="#" class="flex items-center gap-2 text-sm text-slate-400 hover:text-white transition-colors duration-200">
          <el-icon><EditPen /></el-icon> 
          <span>修改密碼</span>
        </a>
        
        <div class="h-4 w-[1px] bg-slate-700"></div>

        <button @click="logout" class="flex items-center gap-2 text-sm text-slate-400 hover:text-purple-400 transition-colors duration-200 group">
          <div class="p-1.5 rounded-full bg-slate-800 group-hover:bg-purple-500/10 transition-colors">
             <el-icon class="group-hover:text-purple-400"><SwitchButton /></el-icon>
          </div>
          <span>登出 <span class="text-xs bg-slate-800 px-2 py-0.5 rounded-full border border-slate-700 ml-1 text-slate-300">{{ currentUser }}</span></span>
        </button>
      </div>
    </header>

    <div class="flex flex-1 pt-16">
      
      <aside class="w-64 fixed left-0 top-16 bottom-0 overflow-y-auto 
                    bg-slate-900 border-r border-white/5 custom-scrollbar">
        
        <el-menu 
          router
          default-active="/homepage"
          class="custom-menu border-none w-full"
          :unique-opened="true"
        >
            <el-menu-item index="/homepage">
              <el-icon><Promotion /></el-icon> <span>首頁</span>
            </el-menu-item>
            
            <el-sub-menu index="/manage">
                <template #title>
                    <el-icon><Message /></el-icon> <span>班級學員管理</span>
                </template>
                <el-menu-item index="/clazz">班級管理</el-menu-item>
                <el-menu-item index="/stu">學員管理</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="/system">
                <template #title>
                    <el-icon><Avatar /></el-icon> <span>系統訊息管理</span>
                </template>
                <el-menu-item index="/dept">部門管理</el-menu-item>
                <el-menu-item index="/emp">員工管理</el-menu-item>
            </el-sub-menu>

            <el-sub-menu index="static">
                <template #title>
                    <el-icon><TrendCharts /></el-icon> <span>數據統計管理</span>
                </template>
                <el-menu-item index="/empReport">員工訊息統計</el-menu-item>
                <el-menu-item index="/stuReport">會員訊息統計</el-menu-item>
                <el-menu-item index="/log">日誌訊息統計</el-menu-item>
            </el-sub-menu>
        </el-menu>
      </aside>

      <main class="flex-1 ml-64 p-8 bg-slate-900 min-h-[calc(100vh-64px)] relative">
        
        <div class="absolute top-0 left-0 w-full h-96 bg-purple-900/10 blur-[100px] pointer-events-none"></div>

        <div class="relative z-10">
            <div class="bg-slate-800/50 backdrop-blur-sm border border-white/5 rounded-2xl p-6 shadow-xl min-h-[500px]">
                <router-view></router-view>
            </div>
        </div>

        <footer class="mt-12 text-center text-xs text-slate-500 pb-4">
            © 2025 版本控制管理系統 - Designed for Excellence
        </footer>
      </main>

    </div>
  </div>
</template>

<style scoped>
/* 自定義滾動條樣式 
  讓側邊欄滾動條在深色模式下不突兀
*/
.custom-scrollbar::-webkit-scrollbar {
  width: 4px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: #334155; /* slate-700 */
  border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: #475569; /* slate-600 */
}

/* 強制覆蓋 Element Plus Menu 的顏色變數 
  讓它完美融入 Tailwind 的 Slate-900 背景
*/
:deep(.custom-menu) {
  --el-menu-bg-color: transparent; /* 背景透明，透出下方的 slate-900 */
  --el-menu-text-color: #94a3b8;   /* slate-400 */
  --el-menu-hover-bg-color: rgba(255, 255, 255, 0.05); /* hover 時微亮 */
  --el-menu-active-color: #c084fc; /* purple-400 (激活時的顏色) */
  --el-menu-border-color: transparent;
}

/* 處理 Submenu 的標題顏色 */
:deep(.el-sub-menu__title) {
  color: #94a3b8; /* slate-400 */
}
:deep(.el-sub-menu__title:hover) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  color: #f1f5f9 !important; /* slate-100 */
}

/* 處理 Menu Item 的激活狀態背景 */
:deep(.el-menu-item.is-active) {
  background-color: rgba(139, 92, 246, 0.15); /* purple-500 的低透明度背景 */
  border-right: 3px solid #c084fc; /* 右側亮條指示 */
  color: #ffffff;
}

/* 移除 Element Plus 預設的右邊框 */
:deep(.el-menu) {
  border-right: none;
}
</style>