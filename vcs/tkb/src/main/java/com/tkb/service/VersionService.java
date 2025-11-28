package com.tkb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.VersionEntity;
import com.tkb.result.Result;


public interface VersionService extends IService<VersionEntity> {


    Result<String> saveVersion(VersionEntity version);

    Result<String> getVersion(String projectName, String projectEnv);

    /**
     * 升版並回傳 Release Note
     */
    Result<String> upgradeAndGenerateNote(VersionEntity versionRequest);

    /**
     *  處理 rollback ：刪除版本紀錄並解除 MR 的版本標記
     */
    Result<String> rollbackVersion(String projectName, String projectEnv, String version);
}
