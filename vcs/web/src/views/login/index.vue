<script setup>
import { ref } from 'vue'; // 移除 onMounted，因為目前沒有用到
import { loginApi } from '@/api/login';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
// 導入 Element Plus Icon
import { User, Lock } from '@element-plus/icons-vue'; 

const router = useRouter();

// 定義表單數據
const loginForm = ref({
    username: '',
    password: ''
});

// 定義表單驗證規則
const loginRules = {
    username: [
        { required: true, message: '請輸入用戶名', trigger: 'blur' }
    ],
    password: [
        { required: true, message: '請輸入密碼', trigger: 'blur' }
    ]
};

// 定義一個 ref 來訪問 el-form 組件實例，用於觸發表單驗證
const loginFormRef = ref(null); 
// 定義 loading 狀態，控制按鈕的禁用和視覺效果
const loading = ref(false);

const handleLogin = async () => {
    // 1. 【加強】觸發表單驗證
    const valid = await loginFormRef.value.validate().catch(() => false); // 捕獲驗證失敗的 promise
    if (!valid) {
        ElMessage.error('請檢查表單輸入！');
        return; // 如果驗證失敗，直接返回
    }

    loading.value = true; // 開始登入，顯示 loading
    try {
        const result = await loginApi(loginForm.value);

        if (result.code === 1) { // 建議明確比較 code 值
            ElMessage.success("登入成功");

            localStorage.setItem('jwt_token', result.data);
            localStorage.setItem('current_username', loginForm.value.username); // 儲存用戶名

            router.push('/homepage');
        } else {
            // 後端返回的錯誤訊息
            ElMessage.error(result.msg || '登入失敗，請檢查帳號密碼');
        }
    } catch (error) {
        // 【加強】錯誤處理：例如網路錯誤、API 請求失敗等
        console.error('登入請求失敗:', error);
        ElMessage.error('網路或伺服器錯誤，請稍後再試。');
    } finally {
        loading.value = false; // 無論成功失敗，結束 loading
    }
};

const handleClear = () => {
    loginFormRef.value.resetFields(); // 【加強】使用 Element Plus 的 resetFields 方法清空並重置驗證狀態
    // 或者 if you want to clear and not reset validation:
    // loginForm.value = { username: '', password: '' }; 
};
</script>

<template>
    <body>
        <div class="login-container">
            <div class="login-header">
                <h2>版本控制管理系统</h2> </div>

            <el-form :model="loginForm" :rules="loginRules" ref="loginFormRef" @keyup.enter="handleLogin"> 
                <el-form-item prop="username">
                    <el-input v-model="loginForm.username" placeholder="請輸入用戶帳號">
                        <template #prefix>
                            <el-icon><User /></el-icon>
                        </template>
                    </el-input>
                </el-form-item>

                <el-form-item prop="password">
                    <el-input v-model="loginForm.password" placeholder="請輸入密碼" show-password>
                        <template #prefix>
                            <el-icon><Lock /></el-icon>
                        </template>
                    </el-input>
                </el-form-item>

                <el-form-item>
                    <el-row :gutter="24" style="width: 100%;"> <el-col :span="12">
                            <el-button type="primary" style="width: 100%;" @click="handleLogin" :loading="loading">登入</el-button>
                        </el-col>
                        <el-col :span="12">
                            <el-button type="info" style="width: 100%;" @click="handleClear" :loading="loading">清除</el-button>
                        </el-col>
                    </el-row>
                </el-form-item>
            </el-form>

            <div class="login-footer">
                © 2025 版本控制管理系统 - 👶版權所有 </div>
        </div>
    </body>
</template>

<style scoped>
body {
    margin: 0;
    padding: 0;
    font-family: Arial, sans-serif;
    /* 【優化】使用 background-image 代替 background url()，並配合 background-size */
    background-image: url('@/assets/bg.png'); 
    background-repeat: no-repeat;
    background-position: center center;
    background-attachment: fixed; /* 讓背景圖固定不動 */
    background-size: cover; /* 讓背景圖覆蓋整個區域 */
    
    /* 【優化】疊加一個半透明的深色層，增加內容可讀性 */
    background-color: #333; /* 備用背景色或調整背景圖的底色 */
    
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    
    /* 增加一個漸變遮罩層，提升可讀性 */
    position: relative;
    z-index: 0; /* 確保 body 在最底層 */
}

/* 【加強】為 body 添加一個偽元素作為半透明疊加層 */
body::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.5); /* 黑色半透明疊加層，調整透明度 */
    z-index: -1; /* 讓疊加層在內容下方，在背景圖上方 */
}


.login-container {
    width: 400px;
    padding: 30px;
    /* 背景色可以稍微調整透明度，讓背景圖若隱若現 */
    background-color: rgba(255, 255, 255, 0.95); 
    border-radius: 8px;
    box-shadow: 0 0 20px rgba(0, 0, 0, 0.1);
    z-index: 1; /* 確保登入框在疊加層上方 */
}

.login-header {
    text-align: center;
    margin-bottom: 30px;
}

.login-header h2 {
    color: #333;
    font-size: 24px;
}

.login-footer {
    margin-top: 20px;
    text-align: center;
    font-size: 14px;
    color: #666;
}

/* 【加強】為 ElMessage 和 ElMessageBox 引入樣式，確保它們正常顯示 */
/* 如果您在 main.js 中已全局引入，則此處不需要 */
/* @import 'element-plus/dist/index.css'; */
</style>