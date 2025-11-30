import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/homepage/index.vue'
import VersionHistoryView from '../views/version/history/index.vue'
import MRHistoryView from '../views/mr/history/index.vue'
import UserListView from '../views/user/list/index.vue'
import LoginView from '../views/login/index.vue'
import LayoutView from '../views/layout/index.vue'


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [

    { path: '/', 
      name: '', 
      component: LayoutView ,
      redirect:'/homepage',
      children:[
        {path: 'homepage',name: 'homepage',component: HomeView},
        {path: 'version/history',name: 'VersionHistory',component: VersionHistoryView , meta: { title: '版本歷史查詢' , keepAlive: true}},
        {path: 'mr/history',name: 'MRHistory',component: MRHistoryView , meta: { title: 'MR歷史查詢' , keepAlive: true }},
        {path: 'user/list',name: 'UserList',component: UserListView , meta: { title: '用戶查詢' , keepAlive: true }},
      ]
    },

    { path: '/login',name: 'login',component: LoginView},
  ]
})

export default router
