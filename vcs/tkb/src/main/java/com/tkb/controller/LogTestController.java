package com.tkb.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class LogTestController {

    @GetMapping("/trigger-error")
    public String triggerError(String orderNumber) {
        try {
            // 模擬：除以零的錯誤
            int result = 10 / 0;
        } catch (Exception e) {
            //  log.error 會被寫入到 ./logs/vcs-error.log
            log.error("計算訂單金額時發生異常，訂單ID: " + orderNumber +". 原因: {}", e.getMessage());
        }
        return  orderNumber + "Error Triggered! Check your log file.";
    }
}
