import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/homepage/index.vue'
import VersionHistoryView from '../views/version/history/index.vue'
import MRHistoryView from '../views/mr/history/index.vue'
import MRReviewView from '../views/mr/review/index.vue'
import UserListView from '../views/user/list/index.vue'
import LoginView from '../views/login/index.vue'

// 監控模塊
import MonitorOverview from '../views/system/monitor/MonitorOverview.vue'
import MonitorView from '../views/system/monitor/MonitorDetail.vue'
import ProjectManage from '../views/system/project/index.vue'
import DeployRegistryView from '../views/system/deploy-registry/index.vue'
// log 分析
import LogAnalysisView from '../views/system/log/index.vue'
import LogQueryView from '../views/system/log_query/index.vue'
import RelaxView from '../views/relax/index.vue'

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
        {path: '/homepage',name: 'homepage',component: HomeView, meta: { title: '版本控制平台' }},
        {path: '/version/history',name: 'VersionHistory',component: VersionHistoryView , meta: { title: '版本歷史查詢' , keepAlive: true}},
        {path: '/mr/history',name: 'MRHistory',component: MRHistoryView , meta: { title: 'MR歷史查詢' , keepAlive: true }},
        {path: '/mr/review',name: 'MRReview',component: MRReviewView , meta: { title: 'MR AI 程式碼審核' , keepAlive: true }},
        {path: '/relax',name: 'Relax',component: RelaxView , meta: { title: '上班累了看這邊' }},
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
        {
          path: '/system/log_query', 
          name: 'LogQueryView',
          component: LogQueryView, 
          meta: { title: '日誌查詢' }
        },
        {
          path: '/system/log',
          name: 'LogAnalysisView',
          component: LogAnalysisView,
          meta: { title: '日誌智能分析' }
        },
        {
          path: '/system/project',
          name: 'ProjectManage',
          component: ProjectManage,
          meta: { title: '專案管理' }
        },
        {
          path: '/system/deploy-registry',
          name: 'DeployRegistryView',
          component: DeployRegistryView,
          meta: { title: '部署對照表管理', keepAlive: true }
        }
      ]
    },

    { path: '/login', name: 'login', component: LoginView },
  ]
})

export default router
