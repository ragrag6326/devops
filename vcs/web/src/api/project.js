import request from "@/utils/request";

/**
 * 取得啟用中的專案清單（前端版本歷史頁用）
 */
export const getProjectList = () =>
    request.get('/project/list');

/**
 * 取得全部專案（含停用，管理介面用）
 */
export const getProjectListAll = () =>
    request.get('/project/list/all');

/**
 * 新增專案
 * @param {Object} data - { name, displayName, description, gitlabProjectId, category, isActive, sortOrder }
 */
export const addProject = (data) =>
    request.post('/project', data);

/**
 * 修改專案
 * @param {Object} data - { id, name, displayName, description, gitlabProjectId, category, isActive, sortOrder }
 */
export const updateProject = (data) =>
    request.put('/project', data);

/**
 * 刪除專案
 * @param {Number} id
 */
export const deleteProject = (id) =>
    request.delete(`/project/${id}`);

/**
 * 取得當前機器環境 (dev / prod)
 * @returns {Promise<{env: string}>}
 */
export const getSystemEnv = () =>
    request.get('/system/env');

/**
 * 取得可用 SSH 機器清單（來源：tools/common/ssh_hosts.json，
 * 與 sshToolUtil.sh 查表用同一份，新增機器兩邊自動一致）
 * @returns {Promise<[{env, host, label}]>}
 */
export const getSshHosts = () =>
    request.get('/system/ssh-hosts');

/** 讀取 ssh_hosts.json 原始內容（含 user/key/label），供同步合併用 */
export const getSshHostsRaw = () =>
    request.get('/system/ssh-hosts/raw');

/** 覆寫 ssh_hosts.json（VCS 主機本機檔案，sshToolUtil.sh 查表用） */
export const saveSshHosts = (data) =>
    request.put('/system/ssh-hosts', data);

/**
 * 初始化專案環境（建立遠端目錄、腳本、docker-compose）
 * @param {string} projectName - DB project_config.name
 * @returns {Promise<{ config, prod, dev }>}
 */
export const initProject = (projectName) =>
    request.post(`/project/${projectName}/init`);
