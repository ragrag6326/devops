import request from "@/utils/request";

// 查詢員工列表數據 加上 {} 要用 return {} 才有返回值
const queryEmpPage = (name, gender , begin , end ,page , pageSize) => 
    request.get(`/emps?name=${name}&gender=${gender}&begin=${begin}&end=${end}&page=${page}&pageSize=${pageSize}`)


// 新增
const addempApi = (data) => request.post('/emps', data)

// 根據 ID 查詢部門
const queryEmpByIdApi = (id) => request.get(`/emps/${id}`)

// 修改
const updateEmpApi = (emp) => request.put(`/emps/${emp.id}`, emp)


// 刪除
const deleteEmpApi = (ids) => request.delete(`/emps/${ids}`)


export {
    queryEmpPage ,
    addempApi,
    queryEmpByIdApi,
    updateEmpApi,
    deleteEmpApi
}