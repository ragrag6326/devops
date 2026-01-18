package com.tkb.controller;

import com.tkb.entity.VersionEntity;
import com.tkb.utils.result.Result;
import com.tkb.service.VersionService;
import com.tkb.vo.PageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "版本控制管理", description = "提供版本的查詢、生成、檢核與狀態維護")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/version")
public class VersionController {

    private  final VersionService versionService;

    /**
     * 分業查詢
     * @param page
     * @param pageSize
     * @param name
     * @param env
     * @param state
     * @return
     */
    @Operation(summary = "分頁查詢版本列表", description = "根據條件篩選並分頁顯示版本紀錄")
    @GetMapping("/list")
    public Result listPage(
            @Parameter(description = "頁碼 (預設 1)", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數 (預設 10)", example = "10")
            @RequestParam(defaultValue = "10")Integer pageSize ,

            @Parameter(description = "專案名稱 (模糊查詢)", example = "tkb")
            String name ,

            @Parameter(description = "環境 (dev / prod)", example = "dev")
            String env ,

            @Parameter(description = "狀態 (字串格式)", example = "成功")
            String state
    ) {
        log.info("VersionController 分頁查詢 , 參數 {} , {}, {} , {} , {} , {}" , page, pageSize , name , env , state );
        PageBean pageBean = versionService.page(page,pageSize,name,env,state);
        return Result.success(pageBean);
    }

    /**
     * 取得下一個要更新的版本號
     * @param projectName
     * @param env
     * @return
     */
    @Operation(summary = "取得下一個版本號", description = "根據目前版號規則自動生成下一個版號 (例如 v1.0.0 -> v1.0.1)")
    @GetMapping("/next")
    public Result<String> nextVersion(
            @Parameter(description = "專案名稱", required = true, example = "tkbtv") String projectName,
            @Parameter(description = "環境", required = true, example = "dev") String env) {
        log.info("取得下個版號 專案 {} , 環境 {}" ,projectName , env);

        return versionService.getNextVersion(projectName , env);
    }


    /**
     * Prod 用查詢 dev 更新最後成功的版號
     * @param projectName
     * @return void
     */
    @Operation(summary = "取得最後一次成功的版本", description = "通常用於 Prod 環境查詢 Dev 環境最後一個穩定的版本")
    @GetMapping("/latest-success")
    public Result<String> latestSuccess(
            @Parameter(description = "專案名稱", required = true) String projectName ,
            @Parameter(description = "環境", required = true) String env
    ) {
        log.info("取得dev最後一個成功的版本: 專案名: {}" ,projectName );
        String version = versionService.getLastSuccessVersion(projectName, env);

        // 查不到會回傳 null
        return Result.success(version);
    }

    @Operation(summary = "檢查是否可部屬", description = "檢核目標版本是否符合部屬規則")
    @GetMapping("/check-deployable")
    public Result<String> checkDeployable(
            @Parameter(description = "專案名稱", required = true) @RequestParam String projectName,
            @Parameter(description = "環境", required = true) @RequestParam String env,
            @Parameter(description = "目標版本 (若不填則檢查最新)", required = false) @RequestParam(required = false) String targetVersion
    ) {
        return versionService.checkdeployable(projectName, env, targetVersion);

    }



    /**
     * 取得此次部屬成功 gitlab 上 mr 的 release Note
     * @param projectName
     * @param env
     * @return
     */
    @Operation(summary = "取得 Release Note", description = "查詢該版本在 GitLab MR 中的 Release Note")
    @GetMapping("/getReleaseNote")
        public Result<String> getReleaseNote(
            @Parameter(description = "專案名稱") String projectName,
            @Parameter(description = "環境") String env
    ) {
        String releaseNote = versionService.getReleaseNote(projectName , env);
        return Result.success(releaseNote);
    }

    /**
     * 發版 - 備註修改
     * @param versionEntity
     * @return
     */
    @Operation(summary = "修改版本備註", description = "針對既有版本更新備註欄位")
    @PatchMapping("/editRemark")
    public Result<String> editRemark(@RequestBody VersionEntity versionEntity) {

        Boolean result = versionService.editRemark(versionEntity);

        if ( result ) {
            return Result.success("備註更新成功");
        }
        return Result.error("備註更新失敗");
    }

    /**
     * queryVersionById
     * @param id 版本主鍵 ID
     */
    @Operation(summary = "根據 ID 查詢詳情")
    @GetMapping("/{id}")
    public Result<VersionEntity> queryById(
            @Parameter(description = "版本主鍵 ID", required = true) @PathVariable Long id
    ) {
        return Result.success(versionService.getById(id));
    }

    /**
     * batchDeleteVersionById
     * @param ids 版本主鍵 ID
     */
    @Operation(summary = "批量刪除版本", description = "根據 ID 列表刪除多筆版本紀錄")
    @DeleteMapping("/{ids}")
    public Result<String> batchDeleteById(
            @Parameter(description = "ID 列表 (以逗號分隔)", required = true, example = "1,2,3")
            @PathVariable List<Long> ids
    ) {
        boolean b = versionService.removeBatchByIds(ids);
        if ( b ) {
            return Result.success("刪除成功");
        }
        return Result.error("刪除失敗");
    }

    /**
     * updateBuildId
     * @param versionEntity
     */
    @Operation(summary = "新增JenkinsBuildId ", description = "儲存 JenkinsBuildId")
    @PostMapping("/updateBuildId")
    public Result<String> updateJenkinsBuildId(
            @RequestBody VersionEntity versionEntity
    ) {

        Boolean b = versionService.updateJenkinsBuildById(versionEntity);
        if (b) {
            return Result.success("新增成功");
        }
        return Result.success("新增失敗");
    }
}
