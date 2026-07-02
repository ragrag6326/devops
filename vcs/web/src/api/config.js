import request from "@/utils/request";

/**
 * 讀取指定專案的 config.sh（解析為 JSON）
 * @param {string} projectName - DB project_config.name
 * @returns {{ projectName, scriptName, prod, dev, shared }}
 */
export const getProjectConfig = (projectName) =>
    request.get(`/config/${projectName}`);

/**
 * 將修改後的 config 寫回本機 config.sh
 * @param {string} projectName - DB project_config.name
 * @param {{ prod, dev, shared }} data
 */
export const saveProjectConfig = (projectName, data) =>
    request.post(`/config/${projectName}`, data);

/**
 * 同步前本機欄位驗證（不 SSH，快速）
 * @returns {{ errors: string[], warnings: string[], canSync: boolean }}
 */
export const checkConfigSync = (projectName, dto) =>
    request.post(`/config/${projectName}/check`, dto);

/**
 * 同步已存檔的 config.sh 至遠端 PROD/DEV 機器（SSH）
 * @returns {{ errors: string[], warnings: string[], syncResult: { prod?, dev? } }}
 */
export const syncConfigToRemote = (projectName) =>
    request.post(`/config/${projectName}/sync`);

/**
 * 從其他專案複製 config.sh 內容，複製後回傳目標專案最新 DTO
 * @param {string} targetProject  目標專案 DB name
 * @param {string} sourceProject  來源專案 DB name
 * @returns {ConfigShDTO}
 */
export const copyConfigFrom = (targetProject, sourceProject) =>
    request.post(`/config/${targetProject}/copy-from/${sourceProject}`);
