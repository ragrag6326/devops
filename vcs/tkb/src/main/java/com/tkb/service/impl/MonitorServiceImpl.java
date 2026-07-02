package com.tkb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.tkb.dto.ImageInfoDTO;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.entity.SystemAudLogEntity;
import com.tkb.entity.UserEntity;
import com.tkb.mapper.SystemAudLogMapper;
import com.tkb.entity.ProjectEntity;
import com.tkb.service.MonitorService;
import com.tkb.service.ProjectService;
import com.tkb.utils.Constant.SystemAudLogState;
import com.tkb.utils.ShellExecutor;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorServiceImpl extends ServiceImpl<SystemAudLogMapper, SystemAudLogEntity>  implements MonitorService  {

    // 所有通用腳本集中於此目錄，新增專案只需建立 {project}/config.sh
    private static final String TOOLS_COMMON = "/opt/vcs/tools/common";
    private static final String MANAGE_IMAGES_SCRIPT = TOOLS_COMMON + "/manage_images.sh";

    private final ProjectService projectService;


    @Override
    public int healthCheck(String env, String projectName, String nodeType) {
        log.info("healthCheck env={} project={} node={}", env, projectName, nodeType);
        ShellExecutor.ExecResult result = ShellExecutor.execMerged(
                TOOLS_COMMON + "/healthcheck.sh", resolveSshEnv(env, projectName), projectName, nodeType);
        if (result.isSuccess()) {
            log.info("{} 健康檢查通過", projectName);
            return 200;
        } else {
            log.info("{} 健康檢查失敗，exitCode={}", projectName, result.exitCode());
            return 404;
        }
    }

    @Override
    public String getTraffic(String env, String projectName, String trafficType) {
        log.info("getTraffic env={} project={} type={}", env, projectName, trafficType);
        ShellExecutor.ExecResult result = ShellExecutor.execMerged(
                TOOLS_COMMON + "/get_traffic.sh", resolveSshEnv(env, projectName), projectName, trafficType);
        if (result.exitCode() == 0) {
            log.info("{} 目前 {} 流量在 blue", projectName, trafficType);
            return "BLUE_ACTIVE";
        } else if (result.exitCode() == 1) {
            log.info("{} 目前 {} 流量在 green", projectName, trafficType);
            return "GREEN_ACTIVE";
        }
        return "獲取失敗";
    }

    @Override
    public String switchTraffic(String env, String opertaionName, String projectName, String nodeType, String mode) {
        log.info("switchTraffic env={} project={} node={} mode={}", env, projectName, nodeType, mode);
        ShellExecutor.ExecResult result = ShellExecutor.execMerged(
                TOOLS_COMMON + "/switch_traffic.sh", resolveSshEnv(env, projectName), projectName, nodeType,
                (mode != null ? mode : ""));

        String modeLabel = (mode == null || !mode.equals("header")) ? "正式" : mode;

        if (result.exitCode() == 0) {
            saveAuditLog(opertaionName, projectName, "將 (" + modeLabel + ") 流量切換至" + nodeType,
                    SystemAudLogState.SUCCESS.getCode());
            log.info("{} 切換完畢 目標:{} mode:{}", projectName, nodeType, modeLabel);
            return projectName + " " + nodeType + " " + modeLabel + " 流量切換完畢";

        } else if (result.exitCode() == 10) {
            log.info("{} {} 流量無須切換", projectName, nodeType);
            return projectName + " " + nodeType + " 無須切換";
        }

        saveAuditLog(opertaionName, projectName, "將 (" + modeLabel + ") 流量切換至" + nodeType,
                SystemAudLogState.FAILED.getCode());
        return "切換失敗";
    }

    @Override
    public String restartService(String env, String opertaionName, String projectName, String target) {
        log.info("restartService env={} project={} target={}", env, projectName, target);
        ShellExecutor.ExecResult result = ShellExecutor.execMerged(
                TOOLS_COMMON + "/restartContainer.sh", resolveSshEnv(env, projectName), projectName, target);

        if (result.isSuccess()) {
            saveAuditLog(opertaionName, projectName, "重啟 (" + target + ") container",
                    SystemAudLogState.SUCCESS.getCode());
            log.info("{} 重啟成功: {}", projectName, result.output());
            return result.output();
        } else {
            saveAuditLog(opertaionName, projectName, "重啟 (" + target + ") container",
                    SystemAudLogState.FAILED.getCode());
            throw new RuntimeException("重啟失敗: " + result.output());
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


    /**
     * 取得退版可選版本（解析 manage_images.sh {env} history 的輸出）
     * 輸出格式：backend-prod/tkbtv:1.0.5  or  frontend-prod/go_nuxt-backup:1.0.6
     */
    @Override
    public List<ImageInfoDTO> getDockerImageVersions(String env, String projectName) {
        // 優先用 project_config.image_keyword 做比對；沒設定時才 fallback 到 projectName
        // 解決 prod 命名(tkbgoapi) 與 dev 命名(goapi) 不一致的問題
        ProjectEntity project = projectService.findByName(projectName);
        String keyword = (project != null && project.getImageKeyword() != null && !project.getImageKeyword().isBlank())
                ? project.getImageKeyword()
                : projectName;

        log.info("getDockerImageVersions env={} projectName={} keyword={}", env, projectName, keyword);

        Map<String, List<String>> versionMap = new LinkedHashMap<>();
        versionMap.put("prod",   new ArrayList<>());
        versionMap.put("backup", new ArrayList<>());

        List<String> lines = ShellExecutor.exec(MANAGE_IMAGES_SCRIPT, resolveSshEnv(env, projectName), "history");

        for (String line : lines) {
            if (!line.contains(keyword)) continue;
            String[] parts = line.split(":");
            if (parts.length < 2) continue;
            String repo    = parts[0]; // e.g. backend-prod/tkbgoapi  or  backend-dev/goapi
            String version = parts[1]; // e.g. 1.0.72
            if (repo.contains("-backup")) {
                versionMap.get("backup").add(version);
            } else if (repo.contains("-prod") || repo.contains("-dev")) {
                // dev 機器的 image repo 為 backend-dev / frontend-dev，同樣歸入 prod bucket
                versionMap.get("prod").add(version);
            }
        }

        List<ImageInfoDTO> result = new ArrayList<>();
        versionMap.forEach((type, versions) -> {
            if (!versions.isEmpty()) result.add(new ImageInfoDTO(type, versions));
        });
        return result;
    }

    /**
     * 取得指定環境的 image 清單
     * 腳本呼叫：manage_images.sh {env} {type}
     *   env  = prod | dev
     *   type = current | history
     */
    @Override
    public List<String> getDockerImageVersion(String env, String type) {
        if (!"current".equals(type) && !"history".equals(type)) {
            throw new IllegalArgumentException("type must be 'current' or 'history'");
        }
        if (!"prod".equals(env) && !"dev".equals(env)) {
            throw new IllegalArgumentException("env must be 'prod' or 'dev'");
        }
        // 正確呼叫：manage_images.sh {env} {type}
        log.info("getDockerImageVersion env={} type={}", env, type);
        return ShellExecutor.exec(MANAGE_IMAGES_SCRIPT, env, type);
    }

    @Override
    public List<ImageInfoDTO> getRollBackImageVersions(String env, String projectName) {
        return getDockerImageVersions(env, projectName);
    }

    /**
     * 刪除指定環境的 image
     * 腳本呼叫：manage_images.sh {env} delete {imageName}
     */
    @Override
    public String deleteImage(String env, String imageName) {
        log.info("deleteImage env={} image={}", env, imageName);
        ShellExecutor.ExecResult result = ShellExecutor.execMerged(MANAGE_IMAGES_SCRIPT, env, "delete", imageName);
        return result.isSuccess() ? "刪除成功" : "刪除失敗";
    }

    @Override
    public String renewImage(String env, String opertaionName, String projectName, String nodeType, String version) {
        if (!projectName.matches("^[a-zA-Z0-9_]+$")) {
            return "非法專案名稱";
        }

        log.info("renewImage env={} project={} node={} version={}", env, projectName, nodeType, version);
        ShellExecutor.ExecResult result = ShellExecutor.execMerged(
                TOOLS_COMMON + "/version_renew.sh", resolveSshEnv(env, projectName), projectName, nodeType, version);

        if (result.isSuccess()) {
            String successMsg = String.format("將 (%s) 更新版號為 %s", nodeType, version);
            saveAuditLog(opertaionName, projectName, successMsg, SystemAudLogState.SUCCESS.getCode());
            log.info("{} 退版完畢 目標:{} 版號:{}", projectName, nodeType, version);
            return projectName + " " + nodeType + " " + version + " 版本更新完成";
        } else {
            String errorMsg = String.format("(%s) 更新版號為 %s 失敗", nodeType, version);
            saveAuditLog(opertaionName, projectName, errorMsg, SystemAudLogState.FAILED.getCode());
            log.warn("{} 更新失敗\n輸出:{}", projectName, result.output());
            return "更新失敗 (ExitCode: " + result.exitCode() + ")";
        }
    }

    /**
     * 依 DB prod_ssh_env / dev_ssh_env 解析實際 SSH 目標機器。
     * 未設定時原樣回傳 env（向下相容）。
     */
    private String resolveSshEnv(String env, String projectName) {
        ProjectEntity proj = projectService.findByName(projectName);
        if (proj == null) return env;
        if ("prod".equals(env) && proj.getProdSshEnv() != null && !proj.getProdSshEnv().isBlank()) {
            log.debug("resolveSshEnv: {} prod → {}", projectName, proj.getProdSshEnv());
            return proj.getProdSshEnv();
        }
        if ("dev".equals(env) && proj.getDevSshEnv() != null && !proj.getDevSshEnv().isBlank()) {
            log.debug("resolveSshEnv: {} dev → {}", projectName, proj.getDevSshEnv());
            return proj.getDevSshEnv();
        }
        return env;
    }


    /**
     * 提取共用的 Log 寫入邏輯
     */
    private boolean saveAuditLog(String opertaionName , String projectName, String action, Integer status) {
        SystemAudLogEntity audLog = new SystemAudLogEntity();
        audLog.setProjectName(projectName);
        audLog.setAction(action);
        audLog.setStatus(status);

        // 設定時間：去除毫秒
        audLog.setOperationTime(LocalDateTime.now().withNano(0));

        //操作人員
        audLog.setOperator(opertaionName);

        return this.save(audLog);
    }
}

