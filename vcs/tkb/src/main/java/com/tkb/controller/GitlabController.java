package com.tkb.controller;

import com.tkb.entity.GitlabMrEntity;
import com.tkb.utils.result.Result;
import com.tkb.service.GitlabMrService;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/gitlab")
public class GitlabController {

    private final GitlabMrService gitlabMrService;

    /**
     * 資料同步
     * @param projectName
     * @return
     */
    @GetMapping("/sync/{projectName}")
    public Result<String> getGitlabInfo (@PathVariable String projectName) {
        String result = gitlabMrService.syncFromGitlab(projectName);
        return Result.success(result);
    }

     /**
      * 分頁查詢
      * @param page
      * @param pageSize
      * @param name
      * @param state
      * @param start
      * @param end
      * @return
      */
    @GetMapping("/list")
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10")Integer pageSize ,
                       String name , String state ,
                       @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start ,
                       @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    )
    {
        log.info("GitlabController 分頁查詢 , 參數 {} , {}, {} , {} , {} , {}" , page, pageSize , name , state , start , end);

        // 調用 userService 分頁查詢
        PageBean pageBean = gitlabMrService.page(page,pageSize,name,state,start,end);

        return Result.success(pageBean);
    }

    @GetMapping("/list/{projectName}")
    public Result listByProjectName (@PathVariable String projectName) {

        List<GitlabMrEntity> gitlabMrEntities = gitlabMrService.listByProjectName(projectName);
        return Result.success(gitlabMrEntities);
    }
}
