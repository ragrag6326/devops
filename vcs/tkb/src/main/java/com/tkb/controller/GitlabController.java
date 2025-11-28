package com.tkb.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tkb.api.gitlab.GitlabApiClient;
import com.tkb.config.GitlabConfig;
import com.tkb.api.gitlab.dto.GitlabDto;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.result.Result;
import com.tkb.service.GitlabMrService;
import com.tkb.service.VersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/gitlab")
public class GitlabController {

    private final GitlabMrService gitlabMrService;

    @GetMapping("/sync/{projectName}")
    public Result<String> getGitlabInfo (@PathVariable String projectName) {
        String result = gitlabMrService.syncFromGitlab(projectName);
        return Result.success(result);
    }

    @GetMapping("/list/{projectName}")
    public Result list (@PathVariable String projectName) {

        List<GitlabMrEntity> gitlabMrEntities = gitlabMrService.listByProjectName(projectName);
        return Result.success(gitlabMrEntities);
    }
}
