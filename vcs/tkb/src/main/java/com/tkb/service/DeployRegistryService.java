package com.tkb.service;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 讀寫 deploy.git（Jenkins buildWithParameters 用的部署腳本倉庫）內
 * config/project_deploy.json 的服務。
 * <p>
 * 這個檔案取代原本寫死在 core/deploy/SSHUtil.sh、core/deploy/BuildUtil.sh 裡的
 * PROJECT_NAME case/if 判斷（container 名稱、部署路徑、npm build 差異設定等）。
 * 新增一個部署專案，只需要在這裡新增一筆設定，不用改 shell 腳本邏輯。
 */
public interface DeployRegistryService {

    /** 讀取整份 config/project_deploy.json */
    JsonNode readAll();

    /** 取得 config/project_deploy.json 目前的 last_commit_id（前端編輯前取得，儲存時帶回做樂觀鎖） */
    String lastCommitId();

    /** 讀取單一專案（PROJECT_NAME）的設定，找不到回傳空物件 */
    JsonNode readProject(String projectName);

    /**
     * 新增或更新單一專案設定，寫回後直接 commit + push 回 deploy.git。
     *
     * @param lastCommitId 選填。帶入編輯前取得的 last_commit_id，
     *                     若期間檔案已被別人改過則拒絕寫入（避免互相覆蓋）
     */
    JsonNode writeProject(String projectName, JsonNode projectConfig, String commitMessage, String lastCommitId);

    /** 移除單一專案設定（不存在時拒絕，避免產生空 commit），寫回後直接 commit + push 回 deploy.git */
    JsonNode deleteProject(String projectName, String commitMessage);
}
