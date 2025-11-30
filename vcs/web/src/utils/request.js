import axios from "axios";
import { ElMessage , ElMessageBox } from 'element-plus'
import router from "@/router";

const request = axios.create({
    baseURL: '/api',
    timeout: 5000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// axios 請求(request) 攔截器 - 獲取 localstorage 中的 token 再請求 header 中增加 token
request.interceptors.request.use(
    (config) => {
        console.log(config);
        
        const token = localStorage.getItem("jwt_token");
        const username = localStorage.getItem("current_username");
        if (token && username) {
            config.headers.token = token ;
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

// axios 響應(respone)  攔截器
request.interceptors.response.use(
    (response) => {
        return response.data;
    },
    (error) => {
        // 處理錯誤
        if (error.response.status === 401) {
            ElMessage.error("登入超時,請重新登錄");
            localStorage.removeItem("jwt_token");
            localStorage.removeItem("current_username");
            router.push("/login");
        } else {
            ElMessage.error(error.response.data);
        }

        return Promise.reject(error);
    }
);

export default request;