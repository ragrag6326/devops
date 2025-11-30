import { createApp } from 'vue'
import router from './router'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhtw from 'element-plus/es/locale/lang/zh-tw'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'


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

app.mount('#app')