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

            localStorage.setItem('current_username', loginForm.value.username); // 儲存用戶名
            localStorage.setItem('jwt_token', result.data.token);
            localStorage.setItem('current_id', result.data.id)      // 當前 ID
            localStorage.setItem('current_role', result.data.role)  // 連 Role 一起存

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
                © 2026 版本控制管理系統 V2 - 🤖 版權所有
            </div>
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
    z-index: 10;
    
    /* 這裡不再使用傳統白色背景，讓內容由內部的 .el-form 承載玻璃效果 */
    background: rgba(0, 0, 0, 0.3); /* 輕微深色疊加，增加層次感 */
    border-radius: 24px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
    padding: 0; /* 讓 el-form 內部控制內邊距 */
}

.login-header {
    text-align: center;
    padding: 30px 30px 10px 30px; /* 調整內邊距 */
}
.login-header h2 {
    font-size: 26px;
    font-weight: 700;
    /* 標題漸層 */
    background: linear-gradient(to right, #fff, #cbd5e1);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    margin-bottom: 5px;
}
.login-header p {
    color: var(--text-sub);
    font-size: 14px;
}

.login-footer {
    margin-top: 20px;
    text-align: center;
    font-size: 14px;
    color: #666;
}



/* --- Element Plus 表單/輸入框樣式覆蓋  --- */
/* 1. 調整整個表單容器的外觀 */
.el-form {
    padding: 30px;
    /* 玻璃效果應用在表單本身 */
    background: var(--glass-bg) !important; 
    backdrop-filter: blur(12px) !important;
    border-top: 1px solid var(--glass-border) !important; /* 視覺區隔 */
    border-radius: 0 0 24px 24px; /* 只有底部圓角 */
}

/* 2. 輸入框整體樣式 (Input Wrapper) */
:deep(.el-input__wrapper) {
    background-color: rgba(0, 0, 0, 0.2) !important; 
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.15) inset !important; 
    border-radius: 12px !important; 
    padding: 10px 15px !important; 
    transition: all 0.3s ease;
}

/* 3. 輸入框聚焦 (Focus) 時的樣式 */
:deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 2px var(--brand, #00f5d4) inset,
                0 0 15px color-mix(in srgb, var(--brand, #00f5d4) 30%, transparent) !important;
}

/* 4. 調整輸入框內的文字和圖標顏色 */
:deep(.el-input__inner) {
    color: var(--text-main, #f4f1f9) !important; /* 文字顏色為淺色 */
    font-size: 16px !important;
}

/* 5. 調整輸入框的 Icon 顏色 */
:deep(.el-input__prefix) {
    color: var(--text-sub, #94a3b8) !important; /* 圖標顏色為柔和的灰色 */
    font-size: 28px;
    margin-right: 15px; /* 調整圖標與文字的間距 */
}


/* 6. 按鈕樣式 (使用前一輪定義的漸層風格) */

/* 登入按鈕 (Primary) */
:deep(.el-button--primary) {
    background: var(--brand, #00f5d4) !important;
    border: none !important;
    color: #000 !important;
    box-shadow: 0 4px 14px 0 color-mix(in srgb, var(--brand, #00f5d4) 40%, transparent) !important;
    font-weight: 600;
    height: 48px;
}
:deep(.el-button--primary:hover) { transform: translateY(-2px); opacity: 0.9; }


/* 清除按鈕 (Info - 玻璃風格) */
:deep(.el-button--info) {
    background: rgba(255, 255, 255, 0.1) !important;
    border: 1px solid rgba(255, 255, 255, 0.2) !important;
    color: #fff !important;
    transition: all 0.3s ease !important;
    height: 48px;
}

:deep(.el-button--info:hover) {
    background: rgba(255, 255, 255, 0.2) !important;
    transform: translateY(4px); opacity: 0.8;
}

/* --- 頁面底部 --- */
.login-footer {
    padding: 0px 0px;
    font-size: 15px;
    color: rgba(255, 255, 255, 0.999);
}


</style>