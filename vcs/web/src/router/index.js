import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '../views/homepage/index.vue'
import ClassView from '../views/clazz/index.vue'
import DeptView from '../views/dept/index.vue'
import EmpView from '../views/emp/index.vue'
import LogView from '../views/log/index.vue'
import LoginView from '../views/login/index.vue'
import EmpReportView from '../views/report/emp/index.vue'
import stuReportView from '../views/report/stu/index.vue'
import stuView from '../views/stu/index.vue'
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
        {path: 'clazz',name: 'clazz',component: ClassView},
        {path: 'dept',name: 'dept',component: DeptView},
        {path: 'emp',name: 'emp',component: EmpView},
        {path: 'log',name: 'log',component: LogView},
        {path: 'empReport',name: 'empreport',component: EmpReportView},
        {path: 'stuReport',name:'stureport',component: stuReportView},
        {path: 'stu',name:'stu',component: stuView},
      ]
    },

    { path: '/login',name: 'login',component: LoginView},
  ]
})

export default router
