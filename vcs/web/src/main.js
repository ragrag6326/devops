// === 清除儲存的主題偏好 ===
//localStorage.removeItem('theme');

import { createApp } from 'vue'
import router from './router'

// Element
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhtw from 'element-plus/es/locale/lang/zh-tw'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 主題初始化函數放在這裡
import { initTheme } from '@/utils/theme';

import '@/assets/style/theme-color.css'
import '@/assets/style/el-dialog.css'
import '@/assets/style/el-message.css'

import App from './App.vue'

const app = createApp(App)

app.use(router)
app.use(ElementPlus , {locale: zhtw})
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component)
}
// 確保在 mount 之前或之後立即執行初始化
initTheme();

app.mount('#app')