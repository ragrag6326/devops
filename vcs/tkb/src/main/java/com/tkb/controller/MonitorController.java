package com.tkb.controller;

import com.tkb.dto.DeployDTO;
import com.tkb.service.DeployService;
import com.tkb.service.MonitorService;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Update;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

@Tag(name = "監控服務狀態", description = "健康檢查、當前流量查詢、流量切換、服務重啟")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService MonitorService;

    @Operation(summary = "服務健康檢查", description = "檢測指定專案與節點的 HTTP 狀態 (返回 200 代表正常)")
    @GetMapping("/health/{projectName}/{nodeType}")
    public Result healthCheck(
            @Parameter(description = "專案名稱", required = true, example = "tkbtv")
            @PathVariable String projectName,

            @Parameter(description = "節點類型 (blue / green)", required = true, example = "blue")
            @PathVariable String nodeType
    ){
        int healthCode = MonitorService.healthCheck(projectName , nodeType);
        return Result.success(healthCode);
    }

    @Operation(summary = "查詢當前流量狀態", description = "查詢 Nginx 目前將流量導向哪個環境 (Blue 或 Green)")
    @GetMapping("/traffic/{projectName}/{trafficType}")
    public Result<String> getCurrentTraffic(
            @Parameter(description = "專案名稱", required = true, example = "tkbtv")
            @PathVariable String projectName,

            @Parameter(description = "流量類型 (live (正式) | header (header測試) )", required = true, example = "live")
            @PathVariable String trafficType
    ) {
        String traffic = MonitorService.getTraffic( projectName , trafficType);
        return Result.success(traffic);
    }

    /**
     *
     * @param projectName
     * @param target 重啟目標 blue | green
     * @return
     */
    @Operation(summary = "重啟服務節點", description = "觸發 Shell 腳本重啟指定的 Docker 容器 (Blue 或 Green)")
    @PostMapping("/restart")
    public Result<String> restartService(
            @Parameter(description = "操作人員", required = true, example = "admin")
            @RequestParam String opertaionName,

            @Parameter(description = "專案名稱", required = true, example = "tkbtv")
            @RequestParam String projectName,

            @Parameter(description = "重啟目標 (blue / green)", required = true, example = "blue")
            @RequestParam String target
    ) {
        // 這裡調用您之前寫好的 Shell 執行邏輯
        String result = MonitorService.restartService( opertaionName ,projectName, target);
        return Result.success(result);
    }

    @Operation(summary = "切換流量指向 (藍綠切換)", description = "修改 Nginx 配置，將流量切換至指定顏色環境")
    @PatchMapping("/switchTraffic")
    public Result<String> switchTraffic(
            @Parameter(description = "操作人員", required = true, example = "admin")
            @RequestParam String opertaionName,

            @Parameter(description = "專案名稱", required = true, example = "tv")
            @RequestParam String projectName,

            @Parameter(description = "節點類型 (通常填 blue 或 green)", required = true, example = "blue")
            @RequestParam("nodeType") String nodeType,

            @Parameter(description = "切換正式或header ( 正式為空即可 )", required = true, example = " ")
            @RequestParam(value = "mode") String mode
    ) {
        String traffic = MonitorService.switchTraffic( opertaionName , projectName, nodeType , mode);
        return Result.success(traffic);
    }


    @Operation(summary = "稽核日誌分頁查詢", description = "根據條件篩選並分頁顯示操作日誌")
    @GetMapping("/list")
    public Result getAudLogPage(
            @Parameter(description = "頁碼 (預設 1)", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數 (預設 10)", example = "10")
            @RequestParam(defaultValue = "10")Integer pageSize ,

            @Parameter(description = "專案名稱", example = "tv")
            String projectName,

            @Parameter(description = "狀態", example = "0=成功 1=失敗")
            String status,

            @Parameter(description = "查詢開始時間", example = "2025-12-20")
            LocalDate StartDate,

            @Parameter(description = "狀態", example = "2025-12-30")
            LocalDate EndDate
    ) {
        log.info("MonitorController 分頁查詢 , 參數 {} , {}, {} , {} , {} , {}" , page, pageSize , projectName ,status, StartDate , EndDate );

        // 調用 userService 分頁查詢
        PageBean pageBean = MonitorService.page(page,pageSize,projectName,status,StartDate,EndDate);

        return Result.success(pageBean);
    }

}
