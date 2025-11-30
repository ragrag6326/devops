package com.tkb.controller;

import com.tkb.entity.UserEntity;
import com.tkb.service.UserService;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;


@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result page(@RequestParam(defaultValue = "1") Integer page,
                       @RequestParam(defaultValue = "10")Integer pageSize ,
                       String name )
    {
        log.info("分頁查詢 , 參數 {} , {}, {}" , page, pageSize , name );

        // 調用 userService 分頁查詢
        PageBean pageBean = userService.page(page,pageSize,name);

        return Result.success(pageBean);
    }
}
