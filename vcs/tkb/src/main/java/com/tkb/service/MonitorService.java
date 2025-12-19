package com.tkb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.VersionEntity;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;


public interface MonitorService  {

    int healthCheck(String projectName, String type);

    String getTraffic(String projectName, String type);

    String switchTraffic(String projectName ,String target, String mode);
}
