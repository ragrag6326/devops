package com.tkb.controller;

import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.tkb.dto.DeployDTO;
import com.tkb.service.DeployService;
import com.tkb.utils.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "部屬狀態標記", description = "提供部屬中、成功、失敗、回滾的標記")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/deploy")
public class DeployController {

    private final DeployService service;

    @Operation(summary = "部屬中", description = "將該版號印上部屬標示 (Deploying)")
    @PostMapping("/deploying")
    public Result<Long> deploying(
            @Parameter(description = "部屬版號", required = true, example = "1001")
            @RequestBody DeployDTO dto) {
        Long id = service.markDeploying(dto);
        return Result.success(id);
    }

    @Operation(summary = "部屬成功", description = "將該版號標記為成功 (Success)")
    @PostMapping("/success")
    public Result success(@RequestBody DeployDTO dto){
        service.markSuccess(dto);
        return Result.success();
    }

    @Operation(summary = "部屬失敗", description = "將該版號標記為失敗 (Fail)，通常由 Jenkins Pipeline 觸發")
    @PostMapping("/fail")
    public Result fail(@RequestBody DeployDTO dto){
        service.markFail(dto);
        return Result.success();
    }

    @Operation(summary = "版本回滾", description = "標記該版號為已回滾 (Rollback)")
    @PostMapping("/rollback")
    public Result rollback(@RequestBody DeployDTO dto){
        service.markRollback(dto);
        return Result.success();
    }

}
