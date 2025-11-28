package com.tkb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.result.Result;

import java.time.LocalDateTime;
import java.util.List;


public interface GitlabMrService extends IService<GitlabMrEntity> {

    List<GitlabMrEntity> listByProjectName(String projectName);

    String syncFromGitlab(String projectName);

    /**
     * 查詢特定時間區間內已合併的 MR
     * @param projectName
     * @param targetBranch
     * @param start  MR起始時間
     * @param end   現在時間 now()
     * @return
     */
    List<GitlabMrEntity> getMergedMrsBetween(String projectName, String targetBranch, LocalDateTime start, LocalDateTime end);

    /**
     * 標記版本
     * @param mrEntityIds
     * @param version
     */
    void stampVersionForMrs(List<Long> mrEntityIds, String version);

    /**
     * 標記退版
     * @param projectName
     * @param version
     */
    void unstampVersionForMrs(String projectName, String version);
}
