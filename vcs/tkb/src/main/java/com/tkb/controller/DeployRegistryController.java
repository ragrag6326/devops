package com.tkb.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.tkb.service.DeployGitConfigService;
import com.tkb.service.DeployRegistryService;
import com.tkb.utils.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "7.0.0 部署對照表管理 (deploy.git)",
     description = "讀取/寫入 deploy.git 內 config/project_deploy.json，取代 core/deploy/SSHUtil.sh、core/deploy/BuildUtil.sh 內硬編碼的 PROJECT_NAME 對照表")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/deploy-registry")
public class DeployRegistryController {

    private final DeployRegistryService deployRegistryService;
    private final DeployGitConfigService deployGitConfigService;

    @Operation(summary = "7.0.1 讀取完整對照表", description = "回傳 deploy.git config/project_deploy.json 目前完整內容")
    @GetMapping
    public Result<JsonNode> readAll() {
        try {
            return Result.success(deployRegistryService.readAll());
        } catch (Exception e) {
            log.error("[DeployRegistryController] 讀取失敗: {}", e.getMessage());
            return Result.error("讀取部署對照表失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.0.5 讀取檔案版本 (樂觀鎖)",
               description = "回傳 project_deploy.json 目前的 last_commit_id。前端編輯前取得，儲存時帶回 lastCommitId 參數，期間被別人改過會拒絕寫入")
    @GetMapping("/meta")
    public Result<Map<String, String>> meta() {
        try {
            return Result.success(Map.of("lastCommitId", deployRegistryService.lastCommitId()));
        } catch (Exception e) {
            log.error("[DeployRegistryController] 讀取 meta 失敗: {}", e.getMessage());
            return Result.error("讀取失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.0.2 讀取單一專案設定", description = "找不到時回傳空物件，方便前端新增流程")
    @GetMapping("/{projectName}")
    public Result<JsonNode> readProject(
            @Parameter(description = "PROJECT_NAME，需與 Jenkins 參數值一致", required = true)
            @PathVariable String projectName) {
        try {
            return Result.success(deployRegistryService.readProject(projectName));
        } catch (Exception e) {
            log.error("[DeployRegistryController] 讀取失敗: {}", e.getMessage());
            return Result.error("讀取失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.0.3 新增/更新單一專案設定",
               description = "body 為該專案節點的原始 JSON（純 JSON 編輯）。寫回後直接透過 GitLab Commit API commit + push 回 deploy.git，Jenkins 下次執行即可套用")
    @PutMapping("/{projectName}")
    public Result<JsonNode> writeProject(
            @Parameter(description = "PROJECT_NAME，需與 Jenkins 參數值一致", required = true)
            @PathVariable String projectName,
            @Parameter(description = "自訂 commit message，留空則使用預設訊息")
            @RequestParam(required = false) String commitMessage,
            @Parameter(description = "樂觀鎖：編輯前 GET /meta 取得的 last_commit_id，期間被別人改過會拒絕寫入")
            @RequestParam(required = false) String lastCommitId,
            @RequestBody JsonNode projectConfig) {
        try {
            return Result.success(deployRegistryService.writeProject(projectName, projectConfig, commitMessage, lastCommitId));
        } catch (Exception e) {
            log.error("[DeployRegistryController] 寫入失敗: {}", e.getMessage());
            return Result.error("寫入失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "7.0.4 移除單一專案設定",
               description = "可選擇一併移除該專案的 Dockerfile 模板與 remote_gitlab_repo.json 對照（單一 commit）。寫回後直接 commit + push 回 deploy.git")
    @DeleteMapping("/{projectName}")
    public Result<JsonNode> deleteProject(
            @Parameter(description = "PROJECT_NAME，需與 Jenkins 參數值一致", required = true)
            @PathVariable String projectName,
            @Parameter(description = "一併移除 template/{env}/{PROJECT_NAME}_Dockerfile")
            @RequestParam(required = false, defaultValue = "false") boolean removeTemplates,
            @Parameter(description = "一併移除 remote_gitlab_repo.json 內的對照")
            @RequestParam(required = false, defaultValue = "false") boolean removeRepo,
            @Parameter(description = "自訂 commit message，留空則使用預設訊息")
            @RequestParam(required = false) String commitMessage) {
        try {
            if (removeTemplates || removeRepo) {
                return Result.success(deployGitConfigService.removeProject(projectName, removeTemplates, removeRepo, commitMessage));
            }
            return Result.success(deployRegistryService.deleteProject(projectName, commitMessage));
        } catch (Exception e) {
            log.error("[DeployRegistryController] 刪除失敗: {}", e.getMessage());
            return Result.error("刪除失敗：" + e.getMessage());
        }
    }
}
