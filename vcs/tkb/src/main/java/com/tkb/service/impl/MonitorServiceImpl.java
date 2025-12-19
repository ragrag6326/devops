package com.tkb.service.impl;

import com.tkb.service.MonitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl implements MonitorService {

    @Override
    public int healthCheck(String projectName, String type) {

        // 健康檢查腳本位置
        String scriptPath = "/opt/vcs/tools/"+ projectName +"/healthcheck.sh";
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath, type);

        // 合併錯誤流，方便在日誌中看到腳本內的 echo 內容
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            // 讀取腳本的標準輸出 (Optional: 如果想知道腳本印了什麼)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Script Output: {}", line);
                }
            }

            // 等待腳本執行結束並取得返回值
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info("健康檢查通過");
                return 200;
            } else {
                log.error("健康檢查失敗，代碼: {}", exitCode);
            }

            return 404;

        } catch (IOException | InterruptedException e) {
            log.error("執行腳本時發生錯誤", e);
            // 發生異常時，視為檢查失敗返回 1
            return 1;
        }
    }

    @Override
    public String getTraffic(String projectName, String type) {

        // 取得流量腳本位置
        String scriptPath = "/opt/vcs/tools/"+ projectName +"/get_traffic.sh";
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath, type);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            // 讀取腳本的標準輸出 (Optional: 如果想知道腳本印了什麼)
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Script Output: {}", line);
                }
            }

            // 等待腳本執行結束並取得返回值
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info( type + "目前流量在正式機" );
                return "BLUE_ACTIVE";

            } else if (exitCode == 1) {
                log.info( type + "目前流量在備援機" );
                return "GREEN_ACTIVE";
            }

            return "獲取失敗";

        } catch (IOException | InterruptedException e) {
            log.error("執行腳本時發生錯誤", e);
            // 發生異常時，視為檢查失敗返回 1
            return "執行發生異常";
        }

    }

    @Override
    public String switchTraffic(String projectName ,String target, String mode) {
        // 取得流量腳本位置
        String scriptPath = "/opt/vcs/tools/"+ projectName +"/switch_traffic.sh";
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath, target , mode);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            // 讀取腳本的標準輸出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Script Output: {}", line);
                }
            }

            // 等待腳本執行結束並取得返回值
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.info( "切換完畢 目標:{}  header:{}" ,  target , mode );
                if (mode != null) { return target + " header切換完畢"; }
                return target + " 正式流量切換完畢"  ;

            } else if (exitCode == 10) {
                log.info( "無須切換" );
                return "無須切換";
            }

            return "切換失敗";

        } catch (IOException | InterruptedException e) {
            log.error("執行腳本時發生錯誤", e);
            // 發生異常時，視為檢查失敗返回 1
            return "執行發生異常";
        }

    }
}
