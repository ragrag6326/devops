package com.tkb.controller;

import com.tkb.entity.UserEntity;
import com.tkb.utils.result.Result;
import com.tkb.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class LoginController {

    private final UserService userService;

    @PostMapping("/login")
    public Result login(@RequestBody UserEntity user) {
        log.info("/login 用戶: {} , 嘗試登入" , user.getUsername());

        if ( user.getUsername() == null || user.getPassword() == null ) {
            return Result.error("參數無效");
        }

        String authUser = userService.login(user);

        if (authUser == null) {
            return Result.error("登入失敗");
        }

        // 登入成功 生成JWT 下發令牌
        return Result.success(authUser);
    }
}
