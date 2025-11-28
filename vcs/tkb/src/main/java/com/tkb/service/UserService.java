package com.tkb.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.entity.UserEntity;

public interface UserService extends IService<UserEntity> {
    UserEntity login(UserEntity user);
}
