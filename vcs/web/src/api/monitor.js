import request from "@/utils/request";


/**
 * getCurrentTraffic
 * @param {string} env           prod | dev
 * @param {string} projectName
 * @param {string} trafficType   live / header
 * @returns   "BLUE_ACTIVE" | "GREEN_ACTIVE"
 */
export function getCurrentTraffic(env, projectName, trafficType) {
  return request.get(`/monitor/traffic/${projectName}/${trafficType}`, { params: { env } });
}

/**
 * 服務健康檢查
 * @param {string} env      prod | dev
 * @param {string} projectName 專案名稱
 * @param {string} nodeType 節點類型 (blue / green)
 */
export function healthCheck(env, projectName, nodeType) {
  return request.get(`/monitor/health/${projectName}/${nodeType}`, {
    params: { env },
    timeout: 20000
  });
}

/**
 * 切換流量指向 (藍綠切換)
 * @param {string} env           prod | dev
 * @param {string} opertaionName 操作人員
 * @param {string} projectName   專案名稱
 * @param {string} nodeType      blue | green
 * @param {string} mode          header | '' (正式)
 */
export function switchTraffic(env, opertaionName, projectName, nodeType, mode) {
  return request.patch(
    `/monitor/switchTraffic?env=${env}&opertaionName=${opertaionName}&projectName=${projectName}&nodeType=${nodeType}&mode=${mode}`
  );
}

/**
 * 重啟服務節點
 * @param {string} env           prod | dev
 * @param {string} opertaionName 操作人員
 * @param {string} projectName   專案名稱
 * @param {string} target        blue | green
 */
export function restartService(env, opertaionName, projectName, target) {
  return request.post(
    `/monitor/restart?env=${env}&opertaionName=${opertaionName}&projectName=${projectName}&target=${target}`,
    {},
    { timeout: 30000 }
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

/** 
 * 6.0.5 取得機器上的版本號
 * @param {string} projectName 專案名稱
  * @returns
 */
export function getImageVersion(projectName) {
  return request.get(`/monitor/getImageVersion?projectName=${projectName}`);
}

/**
 * 取得目前運行中或歷史 image 清單
 * @param {string} env  prod | dev
 * @param {string} type current | history
 */
export function getImageVersionByType(env, type) {
  return request.get(`/monitor/getImageVersion/${type}`, { params: { env } });
}

/**
 * 退版用：取得專案可選版本清單
 * @param {string} env         prod | dev
 * @param {string} projectName 專案名稱
 */
export function getRollBackImageVersion(env, projectName) {
  return request.get('/monitor/getRollBackImageVersion', { params: { env, projectName } });
}

/**
 * 移除 Docker image
 * @param {string} env       prod | dev
 * @param {string} imageName 完整 image 路徑
 */
export function deleteImage(env, imageName) {
  return request.get('/monitor/deleteImage', { params: { env, imageName } });
}

/**
 * 6.0.6 版本號更新 (退版)
 * @param {object} params
 * @param {string} params.env          prod | dev
 * @param {string} params.opertaionName
 * @param {string} params.projectName
 * @param {string} params.nodeType     prod | backup
 * @param {string} params.version      1.0.0
 */
export function renewimage(params) {
  return request.post('/monitor/renewImage', params, { timeout: 10000 });
}