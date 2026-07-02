package com.tkb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.dto.ImageInfoDTO;
import com.tkb.entity.LogAnalysisEntity;
import com.tkb.entity.SystemAudLogEntity;
import com.tkb.entity.VersionEntity;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;

import java.time.LocalDate;
import java.util.List;


public interface MonitorService extends IService<SystemAudLogEntity> {

    /** 映射 0/1 為 200/404 */
    int healthCheck(String env, String projectName, String nodeType);

    String getTraffic(String env, String projectName, String trafficType);

    String switchTraffic(String env, String opertaionName, String projectName, String nodeType, String mode);

    String restartService(String env, String opertaionName, String projectName, String target);

    PageBean page(Integer page, Integer pageSize, String projectName, String status, LocalDate startDate, LocalDate endDate);

    /**
     * 退版用：取得指定專案在指定環境可選的版本清單
     * 腳本：manage_images.sh {env} history
     */
    List<ImageInfoDTO> getDockerImageVersions(String env, String projectName);

    /**
     * 取得指定環境目前運行中或歷史的 image 清單
     * 腳本：manage_images.sh {env} {type}
     *
     * @param env  prod 或 dev
     * @param type current 或 history
     */
    List<String> getDockerImageVersion(String env, String type);

    /**
     * 退版可選版本，委派 getDockerImageVersions
     */
    List<ImageInfoDTO> getRollBackImageVersions(String env, String projectName);

    /**
     * 刪除指定環境上的 image
     * 腳本：manage_images.sh {env} delete {imageName}
     */
    String deleteImage(String env, String imageName);

    String renewImage(String env, String opertaionName, String projectName, String nodeType, String version);
}
