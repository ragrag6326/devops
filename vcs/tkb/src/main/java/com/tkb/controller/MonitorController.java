package com.tkb.controller;

import com.tkb.dto.DeployDTO;
import com.tkb.service.DeployService;
import com.tkb.service.MonitorService;
import com.tkb.utils.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Update;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    private final MonitorService MonitorService;

    @GetMapping("/health/{projectName}/{type}")
    public Result healthCheck(@PathVariable String projectName, @PathVariable String type){
        int healthCode = MonitorService.healthCheck(projectName , type);
        return Result.success(healthCode);
    }

    @GetMapping("/traffic/{projectName}/{type}")
    public Result<String> getCurrentTraffic(@PathVariable String projectName, @PathVariable String type) {
        String traffic = MonitorService.getTraffic( projectName , type);
        return Result.success(traffic);
    }

    @PatchMapping("/switchTraffic")
    public Result<String> switchTraffic(
            @RequestParam String projectName,
            @RequestParam("target") String target,
            @RequestParam(value = "mode", defaultValue = "header") String mode
    ) {
        String traffic = MonitorService.switchTraffic( projectName,target , mode);
        return Result.success(traffic);
    }

}
