<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage , ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'


// 當前登入的員工
const currentUser = ref('');

// 路由
const router = useRouter();

const logout = () => {
    // 清空當前登入的員工
    
    ElMessageBox.confirm('確定要登出嗎 ?', '提示', {
        cancelButtonText: '取消',
        confirmButtonText: '登出',
        type: 'warning' // 跳出的樣式
    }).then(async ()  => { // 點擊 confirmButtonText 確定時觸發 

        localStorage.removeItem('current_username'); 
        localStorage.removeItem('jwt_token'); 
        ElMessage.success('退出成功');

        // 跳轉到 登入頁面
        router.push("/login");

    }) .catch(() => { // 點擊 cancelButtonText 取消時觸發 
        ElMessage.info('已取消退出登入'); 
    })
}


onMounted ( () => {

    // 當頁面加載完成後, 獲取當前登入的員工
    currentUser.value = localStorage.getItem('current_username');
})


</script>

<template>
 <div class="common-layout">
    <el-container>
     <!-- Header 區域 -->
      <el-header class="header">
        <spen class="title">魔鬼投顧後台系統</spen>
        <span class="right_tool">
            <a href="">
                <el-icon><EditPen/></el-icon> 修改密碼 &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;
            </a>
            <a href="javascrpit:;" @click="logout">
                <el-icon><SwitchButton/></el-icon> 退出登入 【 {{currentUser}} 】
            </a>
        </span>
      </el-header>


      <el-container class="main-container">

        <!-- 左側菜單 -->
        <el-aside width="200px" class="left-menu">
          <el-menu router>

              <!-- 首頁菜單 -->
              <el-menu-item index="/homepage">
                <el-icon><Promotion /></el-icon> 首頁
              </el-menu-item>
              
              <el-sub-menu index="/manage">
                  <template #title>
                      <el-icon><message /></el-icon> 班級學員管理
                  </template>
                  <el-menu-item index="/clazz"><el-icon><User /></el-icon>班級管理</el-menu-item>
                  <el-menu-item index="/stu"><el-icon><HomeFilled /></el-icon>學員管理</el-menu-item>
              </el-sub-menu>

              <el-sub-menu index="/system">
                  <template #title>
                      <el-icon><Avatar /></el-icon> 系統訊息管理
                  </template>
                  <el-menu-item index="/dept"><el-icon><Money /></el-icon>部門管理</el-menu-item>
                  <el-menu-item index="/emp"><el-icon><User /></el-icon>員工管理</el-menu-item>
              </el-sub-menu>

              <el-sub-menu index="static">
                  <template #title>
                      <el-icon><TrendCharts /></el-icon> 數據統計管理
                  </template>
                  <el-menu-item index="/empReport"><el-icon><User /></el-icon>員工訊息統計</el-menu-item>
                  <el-menu-item index="/stuReport"><el-icon><HomeFilled /></el-icon>會員訊息統計</el-menu-item>
                  <el-menu-item index="/log"><el-icon><HomeFilled /></el-icon>日誌訊息統計</el-menu-item>
              </el-sub-menu>

          </el-menu>
        </el-aside>

        <el-container class="right-container">
            
        <!-- 右側核心展示區 -->
          <el-main class="main-content">
            <router-view></router-view>
          </el-main>

          <el-footer>
             <div class="login-footer">
                © 2025 魔鬼投顧 - 版權所有
            </div>
          </el-footer>

        </el-container>

      </el-container>
    </el-container>
  </div>
</template>

<style scoped>

.header {
  background-image: linear-gradient(to right, #00547d, #007fa4, #00aaa0, #00d072, #a8eb12);
  padding: 0 20px; /* 增加左右填充*/
  display: flex; 
  justify-content: space-between; 
  align-items: center; /* 垂直居中*/
  height: 60px; /* 設置高度*/
}

.title { 
  color: white; 
  font-size: 24px; 
  font-family: "楷体", "KaiTi", sans-serif; 
  line-height: 60px; 
  font-weight: bold;
}

.right_tool a {
  color: white;
  text-decoration: none;
  margin-left: 15px;    /* 調整連結之間的間距*/
  font-size: 14px;      /* 調整字體大小*/
  display: inline-flex; /* 讓圖標和文字在同一行*/
  align-items: center;  /* 垂直居中圖標和文字 */
}

.right_tool .el-icon {
  margin-right: 5px;
}

/* 主容器樣式 */
.main-container {
  border-top: 1px solid #e6e6e6; /* 頂部邊框 */
  height: calc(100vh - 60px); /* 減去 header 高度 */
}

/* 左側菜單樣式 */
.left-menu {
  border-right: 1px solid #e6e6e6; /* 右側邊框 */
  background-color: #f5f5f5;
  height: 100%;
  overflow-y: auto; /* 如果內容過長可滾動 */
}

/* 右側容器樣式 */
.right-container {
  border-left: 1px solid #e6e6e6; /* 左側邊框 */
}

/* 主內容區域樣式 */
.main-content {
  padding: 20px;
  background-color: #fff;
  min-height: calc(100vh - 120px); /* 減去 header 和 footer 高度 */
}

.login-footer {
    margin-top: 20px;
    text-align: center;
    font-size: 14px;
    color: #666;
}


</style>