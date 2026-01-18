package com.tkb.controller;

import com.tkb.dto.DeployDTO;
import com.tkb.service.DeployService;
import com.tkb.service.LogAnalysisService;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;



@Tag(name = "LOG分析", description = "獲取分析完成 LOG 數據")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/log_analysis")
public class LogAnalysisController {

    private final LogAnalysisService logAnalysisService;

    @Operation(summary = "LOG分頁查詢", description = "根據條件篩選並分頁顯示LOG紀錄")
    @GetMapping
    public Result page(
            @Parameter(description = "頁碼 (預設 1)", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數 (預設 10)", example = "10")
            @RequestParam(defaultValue = "10")Integer pageSize ,

            @Parameter(description = "該服務的LOG名稱", example = "vcs")
            String serverName
    ) {
        log.info("分頁查詢 , 參數 {} , {}, {}" , page, pageSize , serverName );

        // 調用 logAnalysisService 分頁查詢
        PageBean pageBean = logAnalysisService.page(page,pageSize,serverName);

        return Result.success(pageBean);
    }

}
