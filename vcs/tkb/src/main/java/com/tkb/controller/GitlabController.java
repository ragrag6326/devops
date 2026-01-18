package com.tkb.controller;

import com.tkb.entity.GitlabMrEntity;
import com.tkb.utils.result.Result;
import com.tkb.service.GitlabMrService;
import com.tkb.vo.PageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Gitlab", description = "取得Gitlab 人員MR紀錄並整合至版號中")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/gitlab")
public class GitlabController {

    private final GitlabMrService gitlabMrService;

    /**
     * 資料同步
     * @param projectName
     * @return
     */
    @Operation(summary = "資料更新", description = "更新 gitlab 的 MR 數據")
    @GetMapping("/sync/{projectName}")
    public Result<String> getGitlabInfo (
            @Parameter(description = "同步的專案名稱，在application.yml修改", required = true, example = "tkbtv")
            @PathVariable String projectName
    ) {
        String result = gitlabMrService.syncFromGitlab(projectName);
        return Result.success(result);
    }

     /**
      * 分頁查詢
      * @param page
      * @param pageSize
      * @param name
      * @param state
      * @param start
      * @param end
      * @return
      */
    @Operation(summary = "MR紀錄分頁顯示", description = "MR紀錄整合後提供分頁查詢")
    @GetMapping("/list")
    public Result page(

            @Parameter(description = "頁碼 (預設 1)", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數 (預設 10)", example = "10")
            @RequestParam(defaultValue = "10")Integer pageSize ,

            @Parameter(description = "專案名稱", example = "tkbtv")
            String name ,

            @Parameter(description = "狀態", example = " 0 、 1 、 2")
            String state ,

            @Parameter(description = "查詢起始日期", example = "2025-12-24")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start ,

            @Parameter(description = "查詢結束日期", example = "2025-12-25")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ) {
        log.info("GitlabController 分頁查詢 , 參數 {} , {}, {} , {} , {} , {}" , page, pageSize , name , state , start , end);

        // 調用 userService 分頁查詢
        PageBean pageBean = gitlabMrService.page(page,pageSize,name,state,start,end);

        return Result.success(pageBean);
    }

    @Operation(summary = "查詢專案待處理 MR 列表", description = "根據專案名稱獲取所有狀態為 pending 的 Merge Requests")
    @GetMapping("/projects/{projectName}/mrs/pending")
    public Result<List<GitlabMrEntity>> listPendingMrs (
            @Parameter(description = "專案名稱", example = "tkbgoapi")
            @PathVariable("projectName") String projectName
    ) {
        List<GitlabMrEntity> mrsPending = gitlabMrService.getMrsPending(projectName);
        return Result.success(mrsPending);
    }
}
