package com.tkb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.vo.PageBean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


public interface GitlabMrService extends IService<GitlabMrEntity> {

    List<GitlabMrEntity> listByProjectName(String projectName);

    /**
     * 分頁查詢用戶
     * @param page 頁碼
     * @param pageSize 每頁數量
     * @param projectName 專案名
     * @param state 專案MR狀態
     * @param mrStartDate MR起始時間
     * @param mrEndDate MR結束時間
     * @return 分頁查詢結果
     */
    PageBean page(Integer page, Integer pageSize, String projectName, String state , LocalDate mrStartDate , LocalDate mrEndDate);


    /** gitlab 資料同步用 */
    String syncFromGitlab(String projectName);


    /**
     * 依環境查「這次要收進 Release Note 的 MR」
     * dev：尚未釋出到 dev 的 merged MR
     * prod：已釋出 dev、但尚未釋出 prod 的 merged MR
     */
    List<GitlabMrEntity> getMergedMrsBetween(
            String projectName,
            String targetBranch,
            String env ,
            LocalDateTime start,
            LocalDateTime end
    );

    /**
     * 針對這次版本，標記 MR 的 version_dev / version_prod & released_xxx
     */
    void stampVersionForMrs(String projectName, String env, List<Long> mrEntityIds, String version);

    /**
     * 退版時解除 MR 與版本關聯（只針對當前 env）
     */
    void unstampVersionForMrs(String projectName, String env ,String version);


    /**
     * UPDATE () SET released_dev = TRUE, version_dev = #{version} WHERE project_name = #{projectName} AND released_dev = False AND state = merged"
     * @param projectName
     * @param version
     */
    Boolean markReleaseDev(String projectName, String version);

    /**
     * UPDATE () SET released_prod = TRUE, version_prod = #{version} WHERE project_name = #{projectName} AND released_dev = TRUE"
     * @param projectName
     * @param version
     */
    Boolean markReleaseProd(String projectName, String version);


    List<GitlabMrEntity> getMrsPending(String projectName);
}
