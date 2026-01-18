package com.tkb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.entity.SystemAudLogEntity;
import com.tkb.entity.UserEntity;
import com.tkb.mapper.SystemAudLogMapper;
import com.tkb.service.MonitorService;
import com.tkb.utils.Constant.SystemAudLogState;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl extends ServiceImpl<SystemAudLogMapper, SystemAudLogEntity>  implements MonitorService  {

    @Override
    public int healthCheck(String projectName, String nodeType) {

        // 健康檢查腳本位置
        String scriptPath = "/opt/vcs/tools/"+ projectName +"/healthcheck.sh";
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath, nodeType);

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
                log.info("{} 健康檢查通過" , projectName);

                return 200;
            } else {
                log.info("{} 健康檢查失敗，代碼: {}", projectName , exitCode);
            }

            return 404;

        } catch (IOException | InterruptedException e) {
            log.error("執行腳本時發生錯誤", e);
            // 發生異常時，視為檢查失敗返回 1
            return 1;
        }
    }

    @Override
    public String getTraffic(String projectName, String trafficType) {

        // 取得流量腳本位置
        String scriptPath = "/opt/vcs/tools/"+ projectName +"/get_traffic.sh";
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath, trafficType);
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
                log.info( "{} 目前 {} 流量在正式機" , projectName , trafficType );
                return "BLUE_ACTIVE".trim();

            } else if (exitCode == 1) {
                log.info( trafficType + "{} 目前 {} 流量在備援機" , projectName , trafficType );
                return "GREEN_ACTIVE".trim();
            }

            return "獲取失敗";

        } catch (IOException | InterruptedException e) {
            log.error("執行腳本時發生錯誤", e);
            // 發生異常時，視為檢查失敗返回 1
            return projectName + "執行發生異常";
        }

    }

    @Override
    public String switchTraffic(String opertaionName ,String projectName ,String nodeType, String mode) {
        // 取得流量腳本位置
        String scriptPath = "/opt/vcs/tools/"+ projectName +"/switch_traffic.sh";
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath, nodeType , mode);
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

                if (mode == null || !mode.equals("header")) {
                    mode = "正式";
                }

                // 1. 建立 Entity 物件
                SystemAudLogEntity audLog = new SystemAudLogEntity();
                audLog.setProjectName(projectName);
                audLog.setAction("將 (" + mode + ") 流量切換至" + nodeType);
                audLog.setOperator(opertaionName);
                audLog.setStatus(SystemAudLogState.SUCCESS.getCode());
                // 2. 設定時間：使用 .withNano(0) 去除毫秒，讓 DB 存入整數秒
                audLog.setOperationTime(LocalDateTime.now().withNano(0));
                // 3. 執行新增 (Insert)
                boolean success = this.save(audLog);

                if (success) {
                    log.info( "{} 切換並記錄完畢 目標:{}  header:{}" , projectName , nodeType , mode );
                }
                return projectName + " " + nodeType + " " + mode + "流量切換完畢"  ;

            } else if (exitCode == 10) {
                log.info( "{} {} 流量無須切換" , projectName , nodeType );
                return projectName + " " + nodeType + "無須切換";
            }

            // 1. 建立 Entity 物件
            SystemAudLogEntity audLog = new SystemAudLogEntity();
            audLog.setProjectName(projectName);
            audLog.setAction("將 (" + mode + ") 流量切換至" + nodeType);
            audLog.setOperator(opertaionName);
            audLog.setStatus(SystemAudLogState.FAILED.getCode());
            // 2. 設定時間：使用 .withNano(0) 去除毫秒，讓 DB 存入整數秒
            audLog.setOperationTime(LocalDateTime.now().withNano(0));
            // 3. 執行新增 (Insert)
            boolean success = this.save(audLog);

            if (success) {
                log.info( "{} 切換並記錄失敗 目標:{}  header:{}" , projectName , nodeType , mode );
            }

            return "切換失敗";

        } catch (IOException | InterruptedException e) {
            log.error("執行腳本時發生錯誤", e);
            // 發生異常時，視為檢查失敗返回 1
            return "執行發生異常";
        }

    }

    @Override
    public String restartService(String opertaionName ,String projectName, String target) {

        // 取得流量腳本位置
        String scriptPath = "/opt/vcs/tools/"+ projectName +"/restartContainer.sh";
        ProcessBuilder pb = new ProcessBuilder("bash", scriptPath , target);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();

            // 讀取腳本的 echo 輸出 (即 result)
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();

            // 等待腳本執行結束並取得返回值
            int exitCode = process.waitFor();
            if (exitCode == 0) {

                // 1. 建立 Entity 物件
                SystemAudLogEntity audLog = new SystemAudLogEntity();
                audLog.setProjectName(projectName);
                audLog.setAction("重啟 (" + target + ") container");
                audLog.setOperator(opertaionName);
                audLog.setStatus(SystemAudLogState.SUCCESS.getCode());
                // 2. 設定時間：使用 .withNano(0) 去除毫秒，讓 DB 存入整數秒
                audLog.setOperationTime(LocalDateTime.now().withNano(0));
                // 3. 執行新增 (Insert)
                boolean success = this.save(audLog);

                if ( success ) {
                    log.info("{} 重啟成功並紀錄成功: {}", projectName ,output);
                }
                return output;
            } else  {
                SystemAudLogEntity audLog = new SystemAudLogEntity();
                audLog.setProjectName(projectName);
                audLog.setAction("重啟 (" + target + ") container");
                audLog.setOperator(opertaionName);
                audLog.setStatus(SystemAudLogState.FAILED.getCode());
                audLog.setOperationTime(LocalDateTime.now().withNano(0));
                boolean success = this.save(audLog);

                if ( success ) {
                    log.info("重啟失敗紀錄到日誌中, Code: {}, Msg: {}", exitCode, output);
                }
                throw new RuntimeException("重啟失敗: " + output);
            }

        } catch (Exception e ) {
            throw new RuntimeException("執行 Shell 發生錯誤", e);
        }
    }

    @Override
    public PageBean page(Integer page, Integer pageSize, String projectName, String status, LocalDate startDate, LocalDate endDate) {
        PageHelper.startPage(page, pageSize);

        List<SystemAudLogEntity> list = this.lambdaQuery()
                .like(projectName != null && !projectName.isEmpty() ,SystemAudLogEntity::getProjectName, projectName)
                .eq(status != null && !status.isEmpty(), SystemAudLogEntity::getStatus, status)
                .between(startDate != null && endDate != null, SystemAudLogEntity::getOperationTime, startDate, endDate)
                .orderByDesc(SystemAudLogEntity::getOperationTime)
                .list();

        Page<SystemAudLogEntity> pageList = (Page<SystemAudLogEntity>) list;

        // 3. 封裝 pageBean 對象
        return new PageBean(pageList.getTotal(), pageList.getResult());
    }
}
