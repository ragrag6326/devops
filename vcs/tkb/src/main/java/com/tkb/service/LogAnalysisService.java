package com.tkb.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.LogAnalysisEntity;
import com.tkb.entity.UserEntity;
import com.tkb.vo.PageBean;

public interface LogAnalysisService extends IService<LogAnalysisEntity> {

    /**
     * 分頁查詢用戶
     * @param page 頁碼
     * @param pageSize 每頁數量
     * @param serviceName 用戶名稱
     * @return 分頁查詢結果
     */
    PageBean page(Integer page, Integer pageSize, String serviceName);
}
