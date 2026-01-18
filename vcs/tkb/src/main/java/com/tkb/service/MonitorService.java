package com.tkb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.LogAnalysisEntity;
import com.tkb.entity.SystemAudLogEntity;
import com.tkb.entity.VersionEntity;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;

import java.time.LocalDate;


public interface MonitorService extends IService<SystemAudLogEntity> {

    /** 映射 0/1 為 200/404 */
    int healthCheck(String projectName, String nodeType);

    String getTraffic(String projectName, String trafficType);

    String switchTraffic(String opertaionName, String projectName ,String nodeType, String mode);

    String restartService(String opertaionName, String projectName, String target);

    PageBean page(Integer page, Integer pageSize, String projectName, String status, LocalDate startDate, LocalDate endDate);
}
