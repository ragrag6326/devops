package com.tkb.controller;

import com.tkb.dto.GitlabMrWebhookDTO;
import com.tkb.dto.MrReviewCallbackDTO;
import com.tkb.dto.MrReviewScanResultDTO;
import com.tkb.entity.MrCodeReviewEntity;
import com.tkb.service.MrCodeReviewService;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "3.1.0 MR AI Code Review", description = "GitLab MR 自動 AI 程式碼審核")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/mr-review")
public class MrCodeReviewController {

    private final MrCodeReviewService mrCodeReviewService;

    @Operation(summary = "掃描單一專案 opened MR 並送出 AI 審核")
    @PostMapping("/scan/{projectName}")
    public Result<MrReviewScanResultDTO> scanProject(@PathVariable String projectName) {
        return Result.success(mrCodeReviewService.scanProject(projectName));
    }

    @Operation(summary = "掃描所有設定專案的 opened MR")
    @PostMapping("/scan")
    public Result<MrReviewScanResultDTO> scanAll() {
        return Result.success(mrCodeReviewService.scanAllProjects());
    }

    @Operation(summary = "N8N 回寫審核結果")
    @PostMapping("/callback")
    public Result<String> callback(@RequestBody MrReviewCallbackDTO callback) {
        mrCodeReviewService.handleCallback(callback);
        return Result.success("ok");
    }

    @Operation(summary = "GitLab MR Webhook 即時觸發")
    @PostMapping("/gitlab-webhook")
    public Result<String> gitlabWebhook(
            @RequestBody GitlabMrWebhookDTO payload,
            @RequestHeader(value = "X-Gitlab-Token", required = false) String gitlabToken
    ) {
        return Result.success(mrCodeReviewService.handleGitlabWebhook(payload, gitlabToken));
    }

    @Operation(summary = "手動觸發單一 MR 審核")
    @PostMapping("/trigger")
    public Result<String> trigger(@RequestParam String projectName, @RequestParam Integer mrIid) {
        boolean submitted = mrCodeReviewService.triggerReviewForMr(projectName, mrIid);
        return Result.success(submitted ? "submitted" : "skipped");
    }

    @Operation(summary = "分頁查詢 AI 審核紀錄")
    @GetMapping
    public Result<PageBean> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            String projectName,
            String reviewStatus,
            String state
    ) {
        return Result.success(mrCodeReviewService.page(page, pageSize, projectName, reviewStatus, state));
    }

    @Operation(summary = "依專案與 MR IID 查詢審核結果")
    @GetMapping("/detail")
    public Result<MrCodeReviewEntity> detail(
            @RequestParam String projectName,
            @RequestParam Integer mrIid
    ) {
        return Result.success(mrCodeReviewService.getByProjectAndIid(projectName, mrIid));
    }
}
