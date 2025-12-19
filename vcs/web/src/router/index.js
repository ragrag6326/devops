import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/homepage/index.vue'
import VersionHistoryView from '../views/version/history/index.vue'
import MRHistoryView from '../views/mr/history/index.vue'
import UserListView from '../views/user/list/index.vue'
import LoginView from '../views/login/index.vue'

// 監控模塊
import MonitorOverview from '../views/system/monitor/MonitorOverview.vue'
import MonitorView from '../views/system/monitor/MonitorDetail.vue'

import LayoutView from '../views/layout/index.vue'



const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // root 直接導到 login
    { path: '/', redirect: '/login' },

    { path: '/', 
      name: '', 
      component: LayoutView ,
      children:[
        {path: '/homepage',name: 'homepage',component: HomeView},
        {path: '/version/history',name: 'VersionHistory',component: VersionHistoryView , meta: { title: '版本歷史查詢' , keepAlive: true}},
        {path: '/mr/history',name: 'MRHistory',component: MRHistoryView , meta: { title: 'MR歷史查詢' , keepAlive: true }},
        {path: '/user/list',name: 'UserList',component: UserListView , meta: { title: '用戶查詢' , keepAlive: true }},
      
        {
          path: 'system/monitor',
          name: 'MonitorOverview',
          component: MonitorOverview,
          meta: { title: '服務狀態總覽', keepAlive: true }
        },
        {
          path: '/system/monitor/:projectName', 
          name: 'MonitorDetail',
          component: MonitorView, 
          meta: { title: '服務詳情控制' }
        },
      ]
    },

    { path: '/login', name: 'login', component: LoginView },
  ]
})

export default router
