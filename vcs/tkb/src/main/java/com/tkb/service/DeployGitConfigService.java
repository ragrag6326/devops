package com.tkb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tkb.dto.InitProjectDTO;

import java.util.List;
import java.util.Map;

/**
 * deploy.git 內「project_deploy.json 以外」設定檔的管理服務：
 * config/remote_gitlab_repo.json、config/vmIP.json、template/{ENV}/{PROJECT_NAME}_Dockerfile，
 * 以及新增/移除專案時跨檔案的單一 commit 原子操作。
 */
public interface DeployGitConfigService {

    // ── remote_gitlab_repo.json ─────────────────────────────────────

    /** 讀取整份 config/remote_gitlab_repo.json（維持 shell jq 讀取的原始結構） */
    JsonNode readGitlabRepo();

    /** 新增/更新 {type}.{projectName}.repository_url，type 僅允許 backend/frontend */
    JsonNode upsertGitlabRepo(String type, String projectName, String repositoryUrl, String commitMessage);

    /** 移除 {type}.{projectName}，找不到時拒絕（避免空 commit） */
    JsonNode deleteGitlabRepo(String type, String projectName, String commitMessage);

    // ── template/{ENV}/{PROJECT_NAME}_Dockerfile ────────────────────

    /** 列出所有 Dockerfile 模板，每筆為 {env, projectName, path} */
    List<Map<String, String>> listTemplates();

    /** 讀取模板內容，不存在回傳 null（配合前端新增流程） */
    String readTemplate(String env, String projectName);

    /** 新增或更新模板（依檔案是否存在自動選 create/update action） */
    void writeTemplate(String env, String projectName, String content, String commitMessage);

    /** 刪除模板 */
    void deleteTemplate(String env, String projectName, String commitMessage);

    // ── vmIP.json ───────────────────────────────────────────────────

    /** 讀取整份 config/vmIP.json（環境 → 預設 IP） */
    JsonNode readVmIp();

    /** 覆寫整份 config/vmIP.json，僅接受「字串 → 字串」的扁平 object */
    JsonNode writeVmIp(JsonNode vmIp, String commitMessage);

    // ── 跨檔案原子操作 ───────────────────────────────────────────────

    /**
     * 新增專案精靈：單一 commit 同時寫入
     * project_deploy.json（新增節點）+ remote_gitlab_repo.json（upsert repo URL）
     * + 各環境 Dockerfile 模板（create）。
     * 專案已存在於 project_deploy.json 時拒絕（請改用編輯功能）。
     */
    JsonNode initProject(InitProjectDTO dto);

    /**
     * 移除專案：單一 commit 移除 project_deploy.json 節點，
     * 並可選擇一併移除該專案的所有 Dockerfile 模板與 remote_gitlab_repo.json 對照。
     */
    JsonNode removeProject(String projectName, boolean removeTemplates, boolean removeRepo, String commitMessage);
}
