package com.tkb.controller;

import com.tkb.dto.LoginDTO;
import com.tkb.utils.result.Result;
import com.tkb.service.UserService;
import com.tkb.vo.LoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "登入", description = "取得JWT TOKEN")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class LoginController {

    private final UserService userService;

    @Operation(summary = "登入", description = "使用者身分驗證")
    @PostMapping("/login")
    public Result login(@RequestBody LoginDTO Login) {
        String username = Login.getUsername();
        String password = Login.getPassword();

        log.info("/login 用戶: {} , 嘗試登入" , username);

        if ( username == null || password == null ) {
            return Result.error("參數無效");
        }

        LoginVO loginVO = userService.login(username, password);

        if (loginVO.getUsername() == null && loginVO.getUsername().isEmpty()) {
            return Result.error("登入失敗");
        }

        // 登入成功 生成JWT 下發令牌
        return Result.success(loginVO);
    }
}
