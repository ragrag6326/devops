import request from "@/utils/request";

// 新增
const loginApi = (data) => request.post('/login', data)


export {
    loginApi
}