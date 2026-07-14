package com.tkb.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tkb.dto.InitProjectDTO;
import com.tkb.service.DeployGitConfigService;
import com.tkb.utils.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * deploy.git 內 project_deploy.json 以外的設定檔管理。
 * 注意：與 DeployRegistryController 共用 /api/deploy-registry 前綴，
 * Spring 對 literal 路徑（gitlab-repo/templates/vm-ip/init-project）的比對優先於 {projectName} 路徑變數。
 */
@Tag(name = "7.1.0 部署設定檔管理 (deploy.git)",
     description = "remote_gitlab_repo.json（repo 對照）、template/{ENV}/{NAME}_Dockerfile（模板）、vmIP.json，以及新增/移除專案的單一 commit 原子操作")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/deploy-registry")
public class DeployGitConfigController {

    private final DeployGitConfigService deployGitConfigService;

    // ── remote_gitlab_repo.json ─────────────────────────────────────

    @Operation(summary = "7.1.1 讀取 GitLab repo 對照表", description = "回傳 config/remote_gitlab_repo.json 原始結構（backend/frontend 各一個陣列）")
    @GetMapping("/gitlab-repo")
    public Result<JsonNode> readGitlabRepo() {
        try {
            return Result.success(deployGitConfigService.readGitlabRepo());
        } catch (Exception e) {
            log.error("[DeployGitConfig] 讀取 repo 對照失敗: {}", e.getMessage());
            return Result.error("讀取失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.1.2 新增/更新 repo 對照", description = "body: {\"repository_url\": \"ssh://git@...\"}。寫回後直接 commit + push")
    @PutMapping("/gitlab-repo/{type}/{projectName}")
    public Result<JsonNode> upsertGitlabRepo(
            @Parameter(description = "backend / frontend，對應 Jenkins 的 TYPE", required = true) @PathVariable String type,
            @Parameter(description = "PROJECT_NAME", required = true) @PathVariable String projectName,
            @RequestParam(required = false) String commitMessage,
            @RequestBody JsonNode body) {
        try {
            String url = body.path("repository_url").asText(null);
            return Result.success(deployGitConfigService.upsertGitlabRepo(type, projectName, url, commitMessage));
        } catch (Exception e) {
            log.error("[DeployGitConfig] 更新 repo 對照失敗: {}", e.getMessage());
            return Result.error("寫入失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.1.3 移除 repo 對照")
    @DeleteMapping("/gitlab-repo/{type}/{projectName}")
    public Result<JsonNode> deleteGitlabRepo(
            @PathVariable String type,
            @PathVariable String projectName,
            @RequestParam(required = false) String commitMessage) {
        try {
            return Result.success(deployGitConfigService.deleteGitlabRepo(type, projectName, commitMessage));
        } catch (Exception e) {
            log.error("[DeployGitConfig] 移除 repo 對照失敗: {}", e.getMessage());
            return Result.error("刪除失敗：" + e.getMessage());
        }
    }

    // ── Dockerfile 模板 ─────────────────────────────────────────────

    @Operation(summary = "7.1.4 列出所有 Dockerfile 模板", description = "掃描 template/{dev|local|prod|admin}/*_Dockerfile，回傳 {env, projectName, path} 清單")
    @GetMapping("/templates")
    public Result<List<Map<String, String>>> listTemplates() {
        try {
            return Result.success(deployGitConfigService.listTemplates());
        } catch (Exception e) {
            log.error("[DeployGitConfig] 列出模板失敗: {}", e.getMessage());
            return Result.error("讀取失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.1.5 讀取 Dockerfile 模板內容", description = "檔案不存在回傳空字串（配合前端新增流程）")
    @GetMapping("/templates/{env}/{projectName}")
    public Result<String> readTemplate(
            @Parameter(description = "dev / local / prod / admin，對應 PROJECT_ENV", required = true) @PathVariable String env,
            @PathVariable String projectName) {
        try {
            String content = deployGitConfigService.readTemplate(env, projectName);
            return Result.success(content != null ? content : "");
        } catch (Exception e) {
            log.error("[DeployGitConfig] 讀取模板失敗: {}", e.getMessage());
            return Result.error("讀取失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.1.6 新增/更新 Dockerfile 模板", description = "body 為 Dockerfile 純文字內容。BuildUtil.sh 會 cp template/{ENV}/{NAME}_Dockerfile 使用")
    @PutMapping(value = "/templates/{env}/{projectName}", consumes = "text/plain")
    public Result<String> writeTemplate(
            @PathVariable String env,
            @PathVariable String projectName,
            @RequestParam(required = false) String commitMessage,
            @RequestBody String content) {
        try {
            deployGitConfigService.writeTemplate(env, projectName, content, commitMessage);
            return Result.success("已儲存並 push 回 deploy.git");
        } catch (Exception e) {
            log.error("[DeployGitConfig] 寫入模板失敗: {}", e.getMessage());
            return Result.error("寫入失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.1.7 移除 Dockerfile 模板")
    @DeleteMapping("/templates/{env}/{projectName}")
    public Result<String> deleteTemplate(
            @PathVariable String env,
            @PathVariable String projectName,
            @RequestParam(required = false) String commitMessage) {
        try {
            deployGitConfigService.deleteTemplate(env, projectName, commitMessage);
            return Result.success("已移除並 push 回 deploy.git");
        } catch (Exception e) {
            log.error("[DeployGitConfig] 移除模板失敗: {}", e.getMessage());
            return Result.error("刪除失敗：" + e.getMessage());
        }
    }

    // ── vmIP.json ───────────────────────────────────────────────────

    @Operation(summary = "7.1.8 讀取 vmIP.json", description = "各環境的預設目標機器 IP。專案特例請改在 project_deploy.json 的 envs.{ENV}.vmIP 設定")
    @GetMapping("/vm-ip")
    public Result<JsonNode> readVmIp() {
        try {
            return Result.success(deployGitConfigService.readVmIp());
        } catch (Exception e) {
            log.error("[DeployGitConfig] 讀取 vmIP 失敗: {}", e.getMessage());
            return Result.error("讀取失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.1.9 覆寫 vmIP.json", description = "body 為完整的環境→IP 扁平 object，值必須是非空字串")
    @PutMapping("/vm-ip")
    public Result<JsonNode> writeVmIp(
            @RequestParam(required = false) String commitMessage,
            @RequestBody JsonNode vmIp) {
        try {
            return Result.success(deployGitConfigService.writeVmIp(vmIp, commitMessage));
        } catch (Exception e) {
            log.error("[DeployGitConfig] 寫入 vmIP 失敗: {}", e.getMessage());
            return Result.error("寫入失敗：" + e.getMessage());
        }
    }

    // ── 新增專案精靈 ─────────────────────────────────────────────────

    @Operation(summary = "7.1.10 新增專案（單一 commit 原子寫入）",
               description = "同時寫入 project_deploy.json + remote_gitlab_repo.json + 各環境 Dockerfile 模板。全部成功或全部不寫入。專案已存在時拒絕")
    @PostMapping("/init-project")
    public Result<JsonNode> initProject(@RequestBody InitProjectDTO dto) {
        try {
            return Result.success(deployGitConfigService.initProject(dto));
        } catch (Exception e) {
            log.error("[DeployGitConfig] 新增專案失敗: {}", e.getMessage());
            return Result.error("新增專案失敗：" + e.getMessage());
        }
    }
}
