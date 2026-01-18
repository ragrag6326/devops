package com.tkb.controller;

import com.tkb.entity.UserEntity;
import com.tkb.service.UserService;
import com.tkb.utils.result.Result;
import com.tkb.vo.PageBean;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Tag(name = "用戶查詢", description = "提供用戶的查詢、新增、刪除、修改密碼")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "使用者分頁查詢", description = "根據條件篩選並分頁顯示使用者紀錄")
    @GetMapping
    public Result page(
            @Parameter(description = "頁碼 (預設 1)", example = "1")
            @RequestParam(defaultValue = "1") Integer page,

            @Parameter(description = "每頁筆數 (預設 10)", example = "10")
            @RequestParam(defaultValue = "10")Integer pageSize ,

            @Parameter(description = "使用者名稱", example = "admin")
            String name
    ) {
        log.info("分頁查詢 , 參數 {} , {}, {}" , page, pageSize , name );

        // 調用 userService 分頁查詢
        PageBean pageBean = userService.page(page,pageSize,name);

        return Result.success(pageBean);
    }

    @PostMapping
    public Result saveOrUpdateUser(@RequestBody UserEntity user) {

        String operator = "創建";
        UserEntity byId = userService.getById(user.getId());
        if (byId != null) {
            user.setUpdatedTime(LocalDateTime.now());
            operator = "修改";
        }

        boolean b = userService.saveOrUpdate(user);
        if (b) {
            return Result.success(operator + " 成功");
        } else {
            return Result.error(operator + "失敗");
        }
    }

    @PutMapping("/{id}")
    public Result updateUser(@RequestBody UserEntity user) {

        user.setUpdatedTime(LocalDateTime.now());
        boolean b = userService.updateById(user);

        if (b) {
            return Result.success( "修改成功");
        } else {
            return Result.error("修改失敗");
        }
    }




    /**
     * batchDeleteUser
     * @param ids 版本主鍵 ID
     */
    @Operation(summary = "批量刪除使用者也支援(單個)", description = "根據 ID 列表刪除多筆版本紀錄")
    @DeleteMapping("/{ids}")
    public Result<String> batchDeleteById(
            @Parameter(description = "ID 列表 (以逗號分隔)", required = true, example = "1,2,3")
            @PathVariable List<Long> ids
    ) {
        boolean b = userService.removeBatchByIds(ids);
        if ( b ) {
            return Result.success("刪除成功");
        }
        return Result.error("刪除失敗");
    }

}
