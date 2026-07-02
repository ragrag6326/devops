package com.tkb.controller;

import com.tkb.dto.ConfigShDTO;
import com.tkb.dto.ConfigSyncResult;
import com.tkb.service.ConfigShService;
import com.tkb.utils.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "6.0.0 Config.sh 管理", description = "讀取與寫入 /opt/vcs/tools/{project}/config.sh")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/config")
public class ConfigController {

    private final ConfigShService configShService;

    @Operation(summary = "6.0.1 讀取 config.sh",
               description = "回傳指定專案的 config.sh 解析結果（JSON）")
    @GetMapping("/{projectName}")
    public Result<ConfigShDTO> read(
            @Parameter(description = "DB project_config.name", required = true)
            @PathVariable String projectName) {
        try {
            return Result.success(configShService.read(projectName));
        } catch (Exception e) {
            log.error("[ConfigController] 讀取失敗: {}", e.getMessage());
            return Result.error("讀取 config.sh 失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "6.0.2 寫入 config.sh",
               description = "將修改後的 JSON 寫回本機 config.sh（保留原始註解與未知欄位）")
    @PostMapping("/{projectName}")
    public Result<String> write(
            @Parameter(description = "DB project_config.name", required = true)
            @PathVariable String projectName,
            @RequestBody ConfigShDTO dto) {
        try {
            configShService.write(projectName, dto);
            return Result.success("config.sh 更新成功");
        } catch (Exception e) {
            // 印完整 root cause，方便排查權限問題
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            log.error("[ConfigController] 寫入失敗 project={} error={}: {}",
                    projectName, cause.getClass().getSimpleName(), cause.getMessage());
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "6.0.3 同步前欄位驗證",
               description = "本機驗證 DTO 必填欄位與格式（不 SSH），快速回應是否可同步")
    @PostMapping("/{projectName}/check")
    public Result<ConfigSyncResult> checkSync(
            @Parameter(description = "DB project_config.name", required = true)
            @PathVariable String projectName,
            @RequestBody ConfigShDTO dto) {
        try {
            return Result.success(configShService.checkSync(projectName, dto));
        } catch (Exception e) {
            log.error("[ConfigController] checkSync 失敗: {}", e.getMessage());
            return Result.error("驗證失敗：" + e.getMessage());
        }
    }

    @Operation(summary = "6.0.4 同步 config.sh 至遠端",
               description = "SSH 將已存檔的 config.sh 同步至 PROD/DEV 機器，並做遠端路徑警告檢查")
    @PostMapping("/{projectName}/sync")
    public Result<ConfigSyncResult> syncToRemote(
            @Parameter(description = "DB project_config.name", required = true)
            @PathVariable String projectName) {
        try {
            ConfigSyncResult r = configShService.syncToRemote(projectName);
            return Result.success(r);
        } catch (Exception e) {
            log.error("[ConfigController] syncToRemote 失敗: {}", e.getMessage());
            return Result.error("同步失敗：" + e.getMessage());
        }
    }
}
