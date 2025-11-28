package com.tkb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tkb.entity.UserEntity;
import com.tkb.mapper.UserMapper;
import com.tkb.result.Result;
import com.tkb.service.UserService;
import com.tkb.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public Result login(UserEntity user) {
        UserEntity User = this.lambdaQuery()
                .eq(UserEntity::getUsername, user.getUsername())
                .eq(UserEntity::getPassword, user.getPassword())
                .one();

        if (User != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", User.getId());
            map.put("name", User.getName());
            map.put("username", User.getUsername());

            String jwt = JwtUtils.generateJWT(map); // jwt 包含當前員工登入的訊息
            return Result.success(jwt);
        }

        return Result.error("登入失敗");
    }
}
