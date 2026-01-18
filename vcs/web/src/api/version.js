import request from "@/utils/request";


/**
 * 分頁查詢版本號碼
 * @param {Number|String} id - 版號 ID
 */
const queryVersionPage = ( page , pageSize , name , env ,state) => 
    request.get(`/version/list` , {
        params : {
            page,
            pageSize,
            name,
            env,
            state
        }
    }
)

/**
 * 根據 ID 查詢版號詳情
 * @param {Number|String} id - 版號 ID
 */
const queryVersionById = (id) =>
    request.get(`/version/${id}` , {
    }
)

/**
 * API 定義：接收 id 或 id 陣列
 * @param {Number|String} ids - 版號 ID
 */
const deleteVersionById = (ids) => {
    // 如果傳入的是array (批刪)，用逗號連接
    const idParam = Array.isArray(ids) ? ids.join(',') : ids;
    return request.delete(`/version/${idParam}`);
}


/**
 * 修改 remark 備註
 * @param data  需傳入 name env version
 */
const editRemark = ( data ) => 
    request.patch('/version/editRemark', data);


/**
 * 查詢最後更版版號
 * @param 
 */
const getLatestSuccessVersion = (name , env) => {
    // 這裡會把 env ('dev' 或 'prod') 傳給後端
    return request.get('/version/latest-success', { 
        params: { 
            projectName: name, 
            env: env
        } 
    });
}

/**
 * 查詢 dev 更新的下個版號
 * @param 
 */
const getNextVersion = (name , env) => {
    // 把 env ('') 傳給後端
    return request.get('/version/next', { 
        params: { 
            projectName: name, 
            env: env
        } 
    });
}

/**
 * 檢查是否能更新指定版號
 * @param 
 */
const checkDeployable = (projectName, env, targetVersion) => {
  return request.get(`/version/check-deployable`, {
    params: { 
        projectName: projectName, 
        env: env,
        targetVersion: targetVersion
    } 
  });
}

/** 
 * 新增JenkinsBuildId 
 * @param {object} params 版本資訊實體類
 * @param {number} params.id 主鍵 ID
 * @param {string} params.jenkinsBuildId Jenkins BuildID
 */
export function updateJenkinsBuildId(params) {
  return request.post(`/version/updateBuildId`, params);
}


export {
    queryVersionPage,
    queryVersionById,
    deleteVersionById,
    editRemark,
    getLatestSuccessVersion,
    getNextVersion,
    checkDeployable
}