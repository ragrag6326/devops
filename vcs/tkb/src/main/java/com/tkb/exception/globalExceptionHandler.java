package com.tkb.exception;

import com.tkb.utils.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局異常處理器
 */
@RestControllerAdvice
@Slf4j
public class globalExceptionHandler {

    @ExceptionHandler(Exception.class) // 捕獲所有的異常訊息
    public Result ex (Exception ex) {
        log.error("全局異常 -> {}", ex.getMessage(), ex);
        ex.printStackTrace();
        return Result.error(ex.getClass().getSimpleName() + ": " + ex.getMessage());
    }
}

