package com.tkb.service.impl;

import com.tkb.entity.ProjectEntity;
import com.tkb.service.ConfigShService;
import com.tkb.service.ProjectInitService;
import com.tkb.service.ProjectService;
import com.tkb.utils.ShellExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectInitServiceImpl implements ProjectInitService {

    @Value("${app.tools-base-path:/opt/vcs/tools}")
    private String toolsBasePath;

    private final ProjectService  projectService;
    private final ConfigShService configShService;

    @Override
    public Map<String, String> initProject(String projectName) {
        ProjectEntity proj = projectService.findByName(projectName);
        if (proj == null) throw new IllegalArgumentException("找不到專案：" + projectName);

        String scriptName = (proj.getScriptName() != null && !proj.getScriptName().isBlank())
                ? proj.getScriptName() : projectName;

        Map<String, String> result = new LinkedHashMap<>();

        // ── 1. 確保本機 config.sh 存在 ────────────────────────────────────
        try {
            configShService.read(projectName);
            log.info("[ProjectInit] config.sh ready for {}", projectName);
            result.put("config", "✅ config.sh 就緒：" + toolsBasePath + "/" + scriptName + "/config.sh");
        } catch (Exception e) {
            log.warn("[ProjectInit] config.sh init 失敗: {}", e.getMessage());
            result.put("config", "⚠️  config.sh：" + e.getMessage());
        }

        // ── 2. PROD 機器初始化 ─────────────────────────────────────────────
        // form-service 等特殊專案：prod_ssh_env=dev → SSH 打到 dev 機器
        String prodSsh = null;
        if (proj.getHasProd() != null && proj.getHasProd() == 1) {
            prodSsh = resolveSshEnv(proj, "prod");
            // initType 固定傳 "prod"，sshEnv 可能是 "dev"（form-service 特殊配置）
            result.put("prod", runInitAndSync(prodSsh, "prod", projectName, scriptName));
        } else {
            result.put("prod", "⏭  hasProd=0，跳過 PROD 初始化");
        }

        // ── 3. DEV 機器初始化 ──────────────────────────────────────────────
        if (proj.getHasDev() != null && proj.getHasDev() == 1) {
            String devSsh = resolveSshEnv(proj, "dev");
            // 若 prod/dev 最終 SSH 目標相同（form-service prod_ssh_env=dev），只跑一次
            if (devSsh.equals(prodSsh)) {
                log.info("[ProjectInit] prod_ssh_env == dev_ssh_env ({})，跳過重複執行", devSsh);
                result.put("dev", "⏭  SSH 目標與 PROD 相同（" + devSsh + "），init + config.sh 同步已於 PROD 階段完成");
            } else {
                result.put("dev", runInitAndSync(devSsh, "dev", projectName, scriptName));
            }
        } else {
            result.put("dev", "⏭  hasDev=0，跳過 DEV 初始化");
        }

        return result;
    }

    /**
     * init_project.sh + sync_config.sh 一起跑。
     * init 建立遠端目錄與 deploy/rollback/switch_traffic scripts；
     * sync 把本機 config.sh 推過去，讓遠端 scripts source 到正確設定。
     *
     * @param sshEnv    實際 SSH 目標（prod|dev，含 override）
     * @param initType  初始化邏輯（prod|dev，固定傳呼叫方語意）
     */
    private String runInitAndSync(String sshEnv, String initType, String projectName, String scriptName) {
        StringBuilder sb = new StringBuilder();

        // 1. init_project.sh <sshEnv> <project> <scriptName> <initType>
        String initScript = toolsBasePath + "/common/init_project.sh";
        log.info("[ProjectInit] init_project.sh sshEnv={} initType={} project={} script={}",
                sshEnv, initType, projectName, scriptName);
        ShellExecutor.ExecResult initR = ShellExecutor.execMerged(initScript, sshEnv, projectName, scriptName, initType);
        if (initR.isSuccess()) {
            log.info("[ProjectInit] init OK env={}\n{}", sshEnv, initR.output());
            sb.append("✅ init:\n").append(initR.output().trim());
        } else {
            log.error("[ProjectInit] init FAILED env={} exit={}\n{}", sshEnv, initR.exitCode(), initR.output());
            sb.append("❌ init (exit=").append(initR.exitCode()).append("):\n").append(initR.output().trim());
        }

        // 2. sync_config.sh（無論 init 成功與否都嘗試同步）
        String syncScript = toolsBasePath + "/common/sync_config.sh";
        log.info("[ProjectInit] sync_config.sh {} {}", sshEnv, scriptName);
        ShellExecutor.ExecResult syncR = ShellExecutor.execMerged(syncScript, sshEnv, scriptName);
        sb.append("\n");
        if (syncR.isSuccess()) {
            log.info("[ProjectInit] sync_config OK env={}\n{}", sshEnv, syncR.output());
            sb.append("✅ config.sh 同步:\n").append(syncR.output().trim());
        } else {
            log.error("[ProjectInit] sync_config FAILED env={} exit={}\n{}", sshEnv, syncR.exitCode(), syncR.output());
            sb.append("⚠️  config.sh 同步失敗 (exit=").append(syncR.exitCode()).append("):\n").append(syncR.output().trim());
        }

        return sb.toString();
    }

    /** prod_ssh_env / dev_ssh_env override，未設定時使用預設值 */
    private String resolveSshEnv(ProjectEntity proj, String env) {
        if ("prod".equals(env)) {
            return (proj.getProdSshEnv() != null && !proj.getProdSshEnv().isBlank())
                    ? proj.getProdSshEnv() : "prod";
        }
        return (proj.getDevSshEnv() != null && !proj.getDevSshEnv().isBlank())
                ? proj.getDevSshEnv() : "dev";
    }
}
