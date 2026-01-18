package com.tkb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.VersionEntity;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;


public interface VersionService extends IService<VersionEntity> {


    Result<String> saveNewVersion(VersionEntity version);

    Result<String> getVersion(String projectName, String projectEnv);

    /**
     * 升版並回傳 Release Note
     */
    Result<String> upgradeAndGenerateNote(VersionEntity versionRequest);

    /**
     *  處理 rollback ：刪除版本紀錄並解除 MR 的版本標記
     */
    Result<String> rollbackVersion(String projectName, String projectEnv, String version);

    /**
     *
     * @param page
     * @param pageSize
     * @param name
     * @param env
     * @param state
     * @return
     */
    PageBean page(Integer page, Integer pageSize, String name, String env, String state);

    /**
     * 取的下一個版本
     * @param projectName
     * @param env
     * @return
     */
    Result<String> getNextVersion(String projectName, String env);

    /**
     * 取得 dev環境 最後一次成功的 版號
     * @param projectName
     * @param env
     * @return
     */
    String getLastSuccessVersion(String projectName, String env);

    /**
     * 獲取發版成功的 releaseNote
     * @param projectName
     * @param env
     * @return
     */
    String getReleaseNote(String projectName, String env);

    /**
     * 修改發版的備註
     * @param versionEntity
     */
    Boolean editRemark(VersionEntity versionEntity);


    /**
     * 檢測 prod 環境是否能夠部屬
     * @param projectName
     * @param env
     * @param targetVersion
     * @return
     */
    Result<String> checkdeployable(String projectName, String env , String targetVersion);

    Boolean  updateJenkinsBuildById(VersionEntity versionEntity);
}
