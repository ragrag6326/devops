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

export {
    queryMRPage ,
    queryByProjectName,
}