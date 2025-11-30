import request from "@/utils/request";

// 查詢員工列表數據 加上 {} 要用 return {} 才有返回值
const queryMRPage = ( page , pageSize , name , state ,start , end) => 
    request.get(`/gitlab/list` , {
        params : {
            page,
            pageSize,
            name,
            state,
            start,
            end
        }
    }
)

const queryByProjectName = (Projectname) => 
    request.get(`/gitlab/list/${Projectname}`)

// 新增
const addUserApi = (data) => request.post('/users', data)

// 修改
const updateUserApi = (emp) => request.put(`/users/${emp.id}`, emp)

// 刪除
const deleteUserApi = (ids) => request.delete(`/users/${ids}`)


export {
    queryMRPage ,
    queryByProjectName,
}