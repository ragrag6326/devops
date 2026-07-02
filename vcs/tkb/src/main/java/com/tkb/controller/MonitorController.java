package com.tkb.controller;

import com.tkb.dto.ImageInfoDTO;
import com.tkb.dto.RenewImageDTO;
import com.tkb.service.MonitorService;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "6.0.0 監控服務狀態", description = "健康檢查、當前流量查詢、流量切換、服務重啟")
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService MonitorService;

    @Operation(summary = "6.0.1 服務健康檢查", description = "檢測指定專案與節點的 HTTP 狀態 (返回 200 代表正常)")
    @GetMapping("/health/{projectName}/{nodeType}")
    public Result healthCheck(
            @Parameter(description = "環境 prod 或 dev", required = true, example = "prod")
            @RequestParam String env,

            @Parameter(description = "專案名稱", required = true, example = "tkbtv")
            @PathVariable String projectName,

            @Parameter(description = "節點類型 (blue / green)", required = true, example = "blue")
            @PathVariable String nodeType
    ){
        int healthCode = MonitorService.healthCheck(env, projectName, nodeType);
        return Result.success(healthCode);
    }

    @Operation(summary = "6.0.2 查詢當前流量狀態", description = "查詢 Nginx 目前將流量導向哪個環境 (Blue 或 Green)")
    @GetMapping("/traffic/{projectName}/{trafficType}")
    public Result<String> getCurrentTraffic(
            @Parameter(description = "環境 prod 或 dev", required = true, example = "prod")
            @RequestParam String env,

            @Parameter(description = "專案名稱", required = true, example = "tkbtv")
            @PathVariable String projectName,

            @Parameter(description = "流量類型 (live (正式) | header (header測試) )", required = true, example = "live")
            @PathVariable String trafficType
    ) {
        String traffic = MonitorService.getTraffic(env, projectName, trafficType);
        return Result.success(traffic);
    }

    /**
     *
     * @param projectName
     * @param target 重啟目標 blue | green
     * @return
     */
    @Operation(summary = "6.0.3 重啟服務節點", description = "觸發 Shell 腳本重啟指定的 Docker 容器 (Blue 或 Green)")
    @PostMapping("/restart")
    public Result<String> restartService(
            @Parameter(description = "環境 prod 或 dev", required = true, example = "prod")
            @RequestParam String env,

            @Parameter(description = "操作人員", required = true, example = "admin")
            @RequestParam String opertaionName,

            @Parameter(description = "專案名稱", required = true, example = "tkbtv")
            @RequestParam String projectName,

            @Parameter(description = "重啟目標 (blue / green)", required = true, example = "blue")
            @RequestParam String target
    ) {
        String result = MonitorService.restartService(env, opertaionName, projectName, target);
        return Result.success(result);
    }

    @Operation(summary = "6.0.4 切換流量指向 (藍綠切換)", description = "修改 Nginx 配置，將流量切換至指定顏色環境")
    @PatchMapping("/switchTraffic")
    public Result<String> switchTraffic(
            @Parameter(description = "環境 prod 或 dev", required = true, example = "prod")
            @RequestParam String env,

            @Parameter(description = "操作人員", required = true, example = "admin")
            @RequestParam String opertaionName,

            @Parameter(description = "專案名稱", required = true, example = "tv")
            @RequestParam String projectName,

            @Parameter(description = "節點類型 (通常填 blue 或 green)", required = true, example = "blue")
            @RequestParam("nodeType") String nodeType,

            @Parameter(description = "切換正式或header ( 正式為空即可 )", required = true, example = " ")
            @RequestParam(value = "mode") String mode
    ) {
        String traffic = MonitorService.switchTraffic(env, opertaionName, projectName, nodeType, mode);
        return Result.success(traffic);
    }

    @Operation(
        summary = "6.0.5 取得目前/歷史 image 清單",
        description = "腳本：manage_images.sh {env} {type}。env=prod|dev，type=current|history"
    )
    @GetMapping("/getImageVersion/{type}")
    public Result<List<String>> getImageVersionByType(
            @Parameter(description = "current 或 history", required = true, example = "current")
            @PathVariable String type,
            @Parameter(description = "環境 prod 或 dev", required = true, example = "prod")
            @RequestParam String env
    ) {
        return Result.success(MonitorService.getDockerImageVersion(env, type));
    }

    @Operation(summary = "6.0.5b 退版可選版本", description = "取得專案在指定環境退版用版本清單")
    @GetMapping("/getRollBackImageVersion")
    public Result<List<ImageInfoDTO>> getRollBackImageVersion(
            @Parameter(description = "環境 prod 或 dev", required = true, example = "prod")
            @RequestParam String env,
            @Parameter(description = "專案名稱", required = true, example = "tkbtv")
            @RequestParam String projectName
    ) {
        return Result.success(MonitorService.getRollBackImageVersions(env, projectName));
    }

    @Operation(summary = "6.0.5c 移除 Docker image", description = "腳本：manage_images.sh {env} delete {imageName}")
    @GetMapping("/deleteImage")
    public Result<String> deleteImage(
            @Parameter(description = "環境 prod 或 dev", required = true, example = "prod")
            @RequestParam String env,
            @Parameter(description = "image 完整名稱", required = true, example = "backend-prod/tkbtv:1.0.5")
            @RequestParam String imageName
    ) {
        return Result.success(MonitorService.deleteImage(env, imageName));
    }

    @Operation(summary = "6.0.6 版本號更新", description = "退版")
    @PostMapping("/renewImage")
    public Result<String> renewimage (@RequestBody RenewImageDTO dto) {
        String result = MonitorService.renewImage(
                dto.getEnv(), dto.getOpertaionName(),
                dto.getProjectName(), dto.getNodeType(), dto.getVersion());
        return Result.success(result);
    }

    @Operation(summary = "6.0.7 稽核日誌分頁查詢", description = "根據條件篩選並分頁顯示操作日誌")
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
