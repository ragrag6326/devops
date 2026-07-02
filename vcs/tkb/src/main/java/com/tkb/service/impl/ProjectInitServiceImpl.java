package com.tkb.service.impl;

import com.tkb.dto.ConfigShDTO;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectInitServiceImpl implements ProjectInitService {

    private static final String TOOLS_COMMON = "/opt/vcs/tools/common";
    private static final String INIT_SCRIPT   = TOOLS_COMMON + "/init_project.sh";

    private final ProjectService   projectService;
    private final ConfigShService  configShService;

    @Override
    public Map<String, String> initProject(String projectName) {
        ProjectEntity proj = projectService.findByName(projectName);
        if (proj == null) throw new IllegalArgumentException("找不到專案：" + projectName);

        String scriptName = (proj.getScriptName() != null && !proj.getScriptName().isBlank())
                ? proj.getScriptName() : projectName;

        Map<String, String> result = new LinkedHashMap<>();

        // ── 1. config.sh（若不存在則建立空白）─────────────────────────────
        try {
            configShService.read(projectName); // read 會自動建立空 DTO，write 若不存在則 createDirectories
            log.info("[ProjectInit] config.sh ready for {}", projectName);
            result.put("config", "✅ config.sh 就緒：/opt/vcs/tools/" + scriptName + "/config.sh");
        } catch (Exception e) {
            log.warn("[ProjectInit] config.sh init 失敗: {}", e.getMessage());
            result.put("config", "⚠️  config.sh：" + e.getMessage());
        }

        // ── 2. PROD 機器初始化 ──────────────────────────────────────────────
        if (proj.getHasProd() != null && proj.getHasProd() == 1) {
            String prodSsh = (proj.getProdSshEnv() != null && !proj.getProdSshEnv().isBlank())
                    ? proj.getProdSshEnv() : "prod";
            result.put("prod", runInitScript(prodSsh, projectName, scriptName));
        } else {
            result.put("prod", "⏭  hasProd=0，跳過 PROD 初始化");
        }

        // ── 3. DEV 機器初始化 ───────────────────────────────────────────────
        if (proj.getHasDev() != null && proj.getHasDev() == 1) {
            String devSsh = (proj.getDevSshEnv() != null && !proj.getDevSshEnv().isBlank())
                    ? proj.getDevSshEnv() : "dev";
            result.put("dev", runInitScript(devSsh, projectName, scriptName));
        } else {
            result.put("dev", "⏭  hasDev=0，跳過 DEV 初始化");
        }

        return result;
    }

    private String runInitScript(String env, String projectName, String scriptName) {
        log.info("[ProjectInit] init_project.sh {} {} {}", env, projectName, scriptName);
        ShellExecutor.ExecResult r = ShellExecutor.execMerged(
                INIT_SCRIPT, env, projectName, scriptName);
        if (r.isSuccess()) {
            log.info("[ProjectInit] {} init OK:\n{}", env, r.output());
            return "✅ " + r.output().trim();
        } else {
            log.error("[ProjectInit] {} init FAILED (exit={}):\n{}", env, r.exitCode(), r.output());
            return "❌ exitCode=" + r.exitCode() + "\n" + r.output().trim();
        }
    }
}
