package com.tkb.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.tkb.entity.VersionEntity;
import com.tkb.result.Result;
import com.tkb.service.VersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/version")
public class VersionController {

    private  final VersionService versionService;

    @GetMapping("{env}")
    public String version(@PathVariable String env) {
        return env;
    }

    @GetMapping("/list")
    public Result<List<VersionEntity>> list() {
        List<VersionEntity> list = versionService.list();
        log.info(list.toString());
        return Result.success(list);
    }

    @GetMapping("/{ProjectName}/{ProjectEnv}")
    public Result<String> getVersion(@PathVariable String ProjectName, @PathVariable String ProjectEnv) {
        log.info("取得 {} 環境 {} 版本號" ,ProjectName ,ProjectEnv);

        return versionService.getVersion(ProjectName , ProjectEnv);

    }

    @PostMapping("/upgrade")
    public Result<String> upgrade(@RequestBody VersionEntity version) {
        log.info("執行升版程序: {} -> {}", version.getProjectName(), version.getVersion());
        return versionService.upgradeAndGenerateNote(version);
    }

    @DeleteMapping("/rollback/{ProjectName}/{ProjectEnv}/{Version}")
    public Result<String> rollback(@PathVariable String ProjectName,
                                   @PathVariable String ProjectEnv,
                                   @PathVariable String Version) {
        log.warn("收到版本回溯請求: 專案 {} 環境 {} 版本 {}", ProjectName, ProjectEnv, Version);
        return versionService.rollbackVersion(ProjectName, ProjectEnv, Version);
    }
}
