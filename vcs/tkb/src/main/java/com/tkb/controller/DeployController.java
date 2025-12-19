package com.tkb.controller;

import com.tkb.dto.DeployDTO;
import com.tkb.service.DeployService;
import com.tkb.utils.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/deploy")
public class DeployController {

    private final DeployService service;

    @PostMapping("/deploying")
    public Result<Long> deploying(@RequestBody DeployDTO dto) {
        Long id = service.markDeploying(dto);
        return Result.success(id);
    }

    @PostMapping("/success")
    public Result success(@RequestBody DeployDTO dto){
        service.markSuccess(dto);
        return Result.success();
    }

    @PostMapping("/fail")
    public Result fail(@RequestBody DeployDTO dto){
        service.markFail(dto);
        return Result.success();
    }

    @PostMapping("/rollback")
    public Result rollback(@RequestBody DeployDTO dto){
        service.markRollback(dto);
        return Result.success();
    }

}
