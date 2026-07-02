package com.tkb.controller;

import com.tkb.utils.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "6.0.0 系統資訊", description = "提供當前部署環境等系統基本資訊")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/system")
public class SystemController {

    /**
     * 由 application.yml 的 app.env 注入，測試機填 dev，正式機填 prod
     */
    @Value("${app.env:dev}")
    private String appEnv;

    @Operation(
        summary = "6.0.1 取得當前環境",
        description = "前端呼叫此 API 判斷目前連線的是測試機 (dev) 還是正式機 (prod)"
    )
    @GetMapping("/env")
    public Result<Map<String, String>> getEnv() {
        return Result.success(Map.of("env", appEnv));
    }
}
