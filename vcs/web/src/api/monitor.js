import request from "@/utils/request";


/** 
 * getCurrentTraffic
 * @param {string} projectName 
  * @param {string} trafficType     live / header
  * @returns   projectName=tv type=header  "data": "GREEN_ACTIVE"
 */
export function getCurrentTraffic(projectName, trafficType) {
  return request.get(`/monitor/traffic/${projectName}/${trafficType}`);
}

/** 
 * 服務健康檢查
 * @param {string} projectName 專案名稱
  * @param {string} nodeType 節點類型 (blue / green)
  * @returns
 */
export function healthCheck(projectName, nodeType) {
  return request.get(`/monitor/health/${projectName}/${nodeType}`);
}

/** 
 * 切換流量指向 (藍綠切換)
 * @param {string} opertaionName 操作人員
  * @param {string} projectName 專案名稱
  * @param {string} nodeType 節點類型 (通常填 blue 或 green)
  * @param {string} mode 切換正式或header ( 正式為空即可 )
  * @returns
 */
export function switchTraffic(opertaionName, projectName, nodeType, mode) {
  return request.patch(`/monitor/switchTraffic?opertaionName=${opertaionName}&projectName=${projectName}&nodeType=${nodeType}&mode=${mode}`);
}


/** 
 * 重啟服務節點
 * @param {string} opertaionName 操作人員
  * @param {string} projectName 專案名稱
  * @param {string} target 重啟目標 (blue / green)
  * @returns
 */
export function restartService(opertaionName, projectName, target) {
  return request.post(
    `/monitor/restart?opertaionName=${opertaionName}&projectName=${projectName}&target=${target}`,
    {},
    {
      timeout: 30000 //  設定 30 秒超時 (預設通常是 5 或 10 秒)
    }
  );
}

/** 
 * 稽核日誌分頁查詢
 * @param {string} page 頁碼 (預設 1)
  * @param {string} pageSize 每頁筆數 (預設 10)
  * @param {string} projectName 專案名稱
  * @param {string} status 狀態
  * @param {string} StartDate 查詢開始時間
  * @param {string} EndDate 狀態
  * @returns
 */
export function getAudLogPage(page, pageSize, projectName, status, StartDate, EndDate) {
  return request.get(`/monitor/list?page=${page}&pageSize=${pageSize}&projectName=${projectName}&status=${status}&StartDate=${StartDate}&EndDate=${EndDate}`);
}