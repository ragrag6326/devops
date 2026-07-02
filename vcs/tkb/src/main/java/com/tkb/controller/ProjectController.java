package com.tkb.controller;

import com.tkb.entity.ProjectEntity;
import com.tkb.service.ProjectInitService;
import com.tkb.service.ProjectService;

import java.util.Map;
import com.tkb.utils.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "5.0.0 專案設定管理", description = "管理版本歷史頁面顯示的專案清單")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectInitService projectInitService;

    @Operation(summary = "5.0.1 取得啟用中的專案清單", description = "前端版本歷史頁面用，僅回傳 is_active=1 的專案")
    @GetMapping("/list")
    public Result<List<ProjectEntity>> listActive() {
        return Result.success(projectService.listActive());
    }

    @Operation(summary = "5.0.2 取得全部專案（含停用）", description = "管理介面用")
    @GetMapping("/list/all")
    public Result<List<ProjectEntity>> listAll() {
        return Result.success(projectService.listAll());
    }

    @Operation(summary = "5.0.3 新增專案")
    @PostMapping
    public Result<String> add(@RequestBody ProjectEntity project) {
        log.info("新增專案: {}", project.getName());
        return projectService.addProject(project);
    }

    @Operation(summary = "5.0.4 修改專案")
    @PutMapping
    public Result<String> update(@RequestBody ProjectEntity project) {
        log.info("修改專案 ID: {}", project.getId());
        return projectService.updateProject(project);
    }

    @Operation(summary = "5.0.5 刪除專案")
    @DeleteMapping("/{id}")
    public Result<String> delete(
            @Parameter(description = "專案 ID", required = true) @PathVariable Long id) {
        log.info("刪除專案 ID: {}", id);
        return projectService.deleteProject(id);
    }

    @Operation(summary = "5.0.6 初始化專案環境",
               description = "在 PROD/DEV 機器建立目錄、生成腳本（deploy/rollback/switch_traffic）、確認 config.sh")
    @PostMapping("/{name}/init")
    public Result<Map<String, String>> init(
            @Parameter(description = "DB project_config.name", required = true) @PathVariable String name) {
        log.info("初始化專案: {}", name);
        try {
            return Result.success(projectInitService.initProject(name));
        } catch (Exception e) {
            log.error("[ProjectInit] 失敗: {}", e.getMessage());
            return Result.error("初始化失敗：" + e.getMessage());
        }
    }
}