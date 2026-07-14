import request from "@/utils/request";

/**
 * deploy.git（Jenkins buildWithParameters 用的部署腳本倉庫）設定檔管理 API。
 *
 * - config/project_deploy.json：部署對照表（純 JSON 編輯）
 * - config/remote_gitlab_repo.json：TYPE/PROJECT_NAME → repo URL 對照
 * - template/{ENV}/{PROJECT_NAME}_Dockerfile：Dockerfile 模板
 * - config/vmIP.json：各環境預設目標機器 IP
 * - init-project：新增專案精靈（單一 commit 原子寫入多個檔案）
 *
 * 所有寫入都會直接透過 GitLab API commit + push 回 deploy.git，
 * Jenkins 下次 buildWithParameters 即套用。
 */

/**
 * query string 組裝。
 * 注意：axios 預設的 params 序列化「不會」編碼 [ ] 等字元，
 * commit message 內的 [deploy-registry] 會讓 Tomcat 直接回 400（RFC 3986 非法字元），
 * 改用 URLSearchParams 做完整 percent-encoding。
 */
const qs = (obj = {}) => {
    const p = new URLSearchParams()
    Object.entries(obj).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') p.append(k, v)
    })
    return p
}

// ── project_deploy.json ───────────────────────────────────────────

/** 讀取完整對照表 */
export const getDeployRegistry = () =>
    request.get('/deploy-registry');

/** 取得 project_deploy.json 的 last_commit_id（編輯前取得，儲存時帶回做樂觀鎖） */
export const getDeployRegistryMeta = () =>
    request.get('/deploy-registry/meta');

/** 讀取單一專案設定，找不到回傳空物件 */
export const getDeployRegistryProject = (projectName) =>
    request.get(`/deploy-registry/${projectName}`);

/**
 * 新增/更新單一專案設定（body 為該專案節點的原始 JSON）
 * @param {string} projectName
 * @param {Object} data - project_deploy.json 內該專案節點的完整 JSON
 * @param {string} [commitMessage]
 * @param {string} [lastCommitId] - 樂觀鎖，期間被別人改過會拒絕寫入
 */
export const saveDeployRegistryProject = (projectName, data, commitMessage, lastCommitId) =>
    request.put(`/deploy-registry/${projectName}`, data, {
        params: qs({ commitMessage, lastCommitId })
    });

/**
 * 移除單一專案設定，可一併移除 Dockerfile 模板與 repo 對照（單一 commit）
 */
export const deleteDeployRegistryProject = (projectName, { removeTemplates = false, removeRepo = false, commitMessage } = {}) =>
    request.delete(`/deploy-registry/${projectName}`, {
        params: qs({ removeTemplates, removeRepo, commitMessage })
    });

// ── remote_gitlab_repo.json ───────────────────────────────────────

/** 讀取 repo 對照表（原始結構：backend/frontend 各一個陣列） */
export const getGitlabRepoMap = () =>
    request.get('/deploy-registry/gitlab-repo');

/** 新增/更新 repo 對照 */
export const saveGitlabRepo = (type, projectName, repositoryUrl, commitMessage) =>
    request.put(`/deploy-registry/gitlab-repo/${type}/${projectName}`,
        { repository_url: repositoryUrl },
        { params: qs({ commitMessage }) });

/** 移除 repo 對照 */
export const deleteGitlabRepo = (type, projectName, commitMessage) =>
    request.delete(`/deploy-registry/gitlab-repo/${type}/${projectName}`, {
        params: qs({ commitMessage })
    });

// ── Dockerfile 模板 ───────────────────────────────────────────────

/** 列出所有 Dockerfile 模板 [{env, projectName, path}] */
export const listDockerTemplates = () =>
    request.get('/deploy-registry/templates');

/** 讀取模板內容（不存在回傳空字串） */
export const getDockerTemplate = (env, projectName) =>
    request.get(`/deploy-registry/templates/${env}/${projectName}`);

/** 新增/更新模板（body 為 Dockerfile 純文字） */
export const saveDockerTemplate = (env, projectName, content, commitMessage) =>
    request.put(`/deploy-registry/templates/${env}/${projectName}`, content, {
        params: qs({ commitMessage }),
        headers: { 'Content-Type': 'text/plain' },
    });

/** 移除模板 */
export const deleteDockerTemplate = (env, projectName, commitMessage) =>
    request.delete(`/deploy-registry/templates/${env}/${projectName}`, {
        params: qs({ commitMessage })
    });

// ── vmIP.json ─────────────────────────────────────────────────────

/** 讀取各環境預設目標機器 IP */
export const getVmIpMap = () =>
    request.get('/deploy-registry/vm-ip');

/** 覆寫 vmIP.json（完整的環境→IP object） */
export const saveVmIpMap = (data, commitMessage) =>
    request.put('/deploy-registry/vm-ip', data, {
        params: qs({ commitMessage })
    });

// ── 新增專案精靈 ──────────────────────────────────────────────────

/**
 * 單一 commit 同時寫入 project_deploy.json + remote_gitlab_repo.json + Dockerfile 模板
 * @param {Object} payload - { projectName, type, repositoryUrl, deployConfig, dockerfiles }
 */
export const initProject = (payload) =>
    request.post('/deploy-registry/init-project', payload);
