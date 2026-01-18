package com.tkb.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.UserEntity;
import com.tkb.vo.LoginVO;
import com.tkb.vo.PageBean;

public interface UserService extends IService<UserEntity> {
    LoginVO login(String username, String password);

    /**
     * 分頁查詢用戶
     * @param page 頁碼
     * @param pageSize 每頁數量
     * @param name 用戶名稱
     * @return 分頁查詢結果
     */
    PageBean page(Integer page, Integer pageSize, String name);
}
