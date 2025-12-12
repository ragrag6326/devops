package com.tkb.controller;

import com.tkb.entity.VersionEntity;
import com.tkb.utils.result.Result;
import com.tkb.service.VersionService;
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
@RequestMapping("/api/version")
public class VersionController {

    private  final VersionService versionService;

    /**
     * 分業查詢
     * @param page
     * @param pageSize
     * @param name
     * @param env
     * @param state
     * @return
     */
    @GetMapping("/list")
    public Result listPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10")Integer pageSize ,
            String name , String env ,String state
    ) {
        log.info("VersionController 分頁查詢 , 參數 {} , {}, {} , {} , {} , {}" , page, pageSize , name , env , state );
        PageBean pageBean = versionService.page(page,pageSize,name,env,state);
        return Result.success(pageBean);
    }

    /**
     * 取得下一個要更新的版本號
     * @param projectName
     * @param env
     * @return
     */
    @GetMapping("/next")
    public Result<String> nextVersion(String projectName, String env) {
        log.info("取得下個版號 專案 {} , 環境 {}" ,projectName , env);

        String next = versionService.getNextVersion(projectName , env);
        return Result.success(next);
    }


    /**
     * Prod 用查詢 dev 更新最後成功的版號
     * @param projectName
     * @return void
     */
    @GetMapping("/latest-success")
    public Result<String> latestSuccess(String projectName , String env) {
        log.info("取得dev最後一個成功的版本: 專案名: {}" ,projectName );
        String version = versionService.getLastSuccessVersion(projectName, env);

        // 查不到會回傳 null
        return Result.success(version);
    }


    /**
     * 取得此次部屬成功 gitlab 上 mr 的 release Note
     * @param projectName
     * @param env
     * @return
     */
    @GetMapping("/getReleaseNote")
        public Result<String> getReleaseNote(String projectName, String env) {

        String releaseNote = versionService.getReleaseNote(projectName , env);
        return Result.success(releaseNote);
    }

    /**
     * 發版 - 備註修改
     * @param versionEntity
     * @return
     */
    @PatchMapping("/editRemark")
    public Result<String> editRemark(@RequestBody VersionEntity versionEntity) {

        Boolean result = versionService.editRemark(versionEntity);

        if ( result ) {
            return Result.success("備註更新成功");
        }
        return Result.error("備註更新失敗");
    }

    /**
     * queryVersionById
     * @param id 版本主鍵 ID
     */
    @GetMapping("/{id}")
    public Result<VersionEntity> queryById(@PathVariable Long id ) {
        return Result.success(versionService.getById(id));
    }

    /**
     * batchDeleteVersionById
     * @param ids 版本主鍵 ID
     */
    @DeleteMapping("/{ids}")
    public Result<String> batchDeleteById(@PathVariable List<Long> ids ) {
        boolean b = versionService.removeBatchByIds(ids);
        if ( b ) {
            return Result.success("刪除成功");
        }
        return Result.error("刪除失敗");
    }


}
