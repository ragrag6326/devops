import request from "@/utils/request";

// 查詢
const queryDeptsApi = () => request.get('/depts')

// 新增
const addDeptApi = (data) => request.post('/depts', data)

// 根據 ID 查詢部門
const queryDeptByIdApi = (id) => request.get(`/depts/${id}`)

// 修改
const updateDeptApi = (data) => request.put(`/depts/${data.id}`, data)

// 刪除
const deleteDeptApi = (id) => request.delete(`/depts/${id}`)


export {
    queryDeptsApi,
    addDeptApi,
    queryDeptByIdApi,
    updateDeptApi,
    deleteDeptApi
}