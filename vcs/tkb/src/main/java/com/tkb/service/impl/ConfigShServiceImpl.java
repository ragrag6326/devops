package com.tkb.service.impl;

import com.tkb.dto.ConfigShDTO;
import com.tkb.dto.ConfigSyncResult;
import com.tkb.entity.ProjectEntity;
import com.tkb.service.ConfigShService;
import com.tkb.service.ProjectService;
import com.tkb.utils.ShellExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigShServiceImpl implements ConfigShService {

    @Value("${app.tools-base-path:/opt/vcs/tools}")
    private String toolsBasePath;

    private final ProjectService projectService;

    // PROD_* 已知欄位（保持前端表單順序）
    private static final List<String> PROD_KEYS = List.of(
            "PROD_BLUE_CONTAINERS", "PROD_GREEN_CONTAINERS",
            "PROD_BLUE_CHECK_PORTS", "PROD_GREEN_CHECK_PORTS",
            "PROD_NGINX_CONF", "PROD_LIVE_UPSTREAM", "PROD_HEADER_UPSTREAM",
            "PROD_TRAFFIC_BLUE_PORT",
            "PROD_DEPLOY_BASE", "PROD_SWITCH_SCRIPT",
            "PROD_HEALTH_HOST", "PROD_HEALTH_SCHEME", "PROD_HEALTH_PATH",
            "PROD_IMAGE_REPO"
    );

    // DEV_* 已知欄位
    private static final List<String> DEV_KEYS = List.of(
            "DEV_BLUE_CONTAINERS", "DEV_GREEN_CONTAINERS",
            "DEV_BLUE_CHECK_PORTS", "DEV_GREEN_CHECK_PORTS",
            "DEV_NGINX_CONF", "DEV_LIVE_UPSTREAM", "DEV_HEADER_UPSTREAM",
            "DEV_TRAFFIC_BLUE_PORT",
            "DEV_DEPLOY_BASE", "DEV_SWITCH_SCRIPT",
            "DEV_HEALTH_HOST", "DEV_HEALTH_SCHEME", "DEV_HEALTH_PATH",
            "DEV_IMAGE_REPO"
    );

    // 共用欄位
    private static final List<String> SHARED_KEYS = List.of(
            "IMAGE_KEYWORD", "BLUE_ENV_KEY", "GREEN_ENV_KEY"
    );

    // bash array 語法的 key（寫回時包 ()）
    private static final Set<String> ARRAY_KEYS = Set.of(
            "PROD_BLUE_CHECK_PORTS", "PROD_GREEN_CHECK_PORTS",
            "DEV_BLUE_CHECK_PORTS",  "DEV_GREEN_CHECK_PORTS"
    );

    // ── parse regex：支援 KEY="val" / KEY=val / KEY=(v1 v2) ──────────────────
    private static final Pattern LINE_PATTERN =
            Pattern.compile("^([A-Z_][A-Z0-9_]*)=(.*)$");

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public ConfigShDTO read(String projectName) {
        String scriptName = resolveScriptName(projectName);
        Path configPath = Paths.get(toolsBasePath, scriptName, "config.sh");

        ConfigShDTO dto = new ConfigShDTO();
        dto.setProjectName(projectName);
        dto.setScriptName(scriptName);

        // 預填空值，確保前端表單欄位齊全
        PROD_KEYS.forEach(k  -> dto.getProd().put(k, ""));
        DEV_KEYS.forEach(k   -> dto.getDev().put(k, ""));
        SHARED_KEYS.forEach(k -> dto.getShared().put(k, ""));

        if (!Files.exists(configPath)) {
            log.warn("[ConfigSh] 檔案不存在: {}", configPath);
            return dto;
        }

        try {
            List<String> lines = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#") || trimmed.isEmpty()) continue;

                Matcher m = LINE_PATTERN.matcher(trimmed);
                if (!m.matches()) continue;

                String key   = m.group(1);
                String value = parseValue(key, m.group(2));

                if (PROD_KEYS.contains(key))    dto.getProd().put(key, value);
                else if (DEV_KEYS.contains(key)) dto.getDev().put(key, value);
                else if (SHARED_KEYS.contains(key)) dto.getShared().put(key, value);
            }
        } catch (IOException e) {
            throw new RuntimeException("讀取 config.sh 失敗: " + configPath, e);
        }

        return dto;
    }

    @Override
    public void write(String projectName, ConfigShDTO dto) {
        String scriptName = resolveScriptName(projectName);
        Path configPath   = Paths.get(toolsBasePath, scriptName, "config.sh");

        // 合併所有待寫入的 key→value
        Map<String, String> allValues = new LinkedHashMap<>();
        dto.getProd().forEach(allValues::put);
        dto.getDev().forEach(allValues::put);
        dto.getShared().forEach(allValues::put);

        try {
            List<String> original;
            if (Files.exists(configPath)) {
                original = Files.readAllLines(configPath, StandardCharsets.UTF_8);
            } else {
                // 目錄不存在時自動建立
                Files.createDirectories(configPath.getParent());
                original = new ArrayList<>();
                original.add("#!/bin/bash");
                original.add("# " + projectName + " 專案配置  (由 common/init.sh source)");
                original.add("");
            }

            List<String> result = new ArrayList<>();
            Set<String> written = new HashSet<>();

            for (String line : original) {
                String trimmed = line.trim();
                Matcher m = LINE_PATTERN.matcher(trimmed);

                if (!trimmed.startsWith("#") && m.matches()) {
                    String key = m.group(1);
                    if (allValues.containsKey(key)) {
                        result.add(formatLine(key, allValues.get(key)));
                        written.add(key);
                        continue;
                    }
                }
                result.add(line);
            }

            // 將原始檔中沒有但 DTO 有的 key 附加到末尾
            for (String key : allValues.keySet()) {
                if (!written.contains(key) && !allValues.get(key).isBlank()) {
                    result.add(formatLine(key, allValues.get(key)));
                }
            }

            Files.write(configPath, result, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("[ConfigSh] 寫入成功: {}", configPath);

        } catch (IOException e) {
            log.error("[ConfigSh] 寫入失敗 path={} cause={} msg={}",
                    configPath, e.getClass().getSimpleName(), e.getMessage());
            throw new RuntimeException(
                    "寫入 config.sh 失敗: " + configPath
                    + " | " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    /** 解析 config.sh 的值（去掉引號 / bash array 括弧） */
    private String parseValue(String key, String raw) {
        raw = raw.trim();
        if (raw.startsWith("(") && raw.endsWith(")")) {
            // bash array: (8091 8094) → "8091 8094"
            return raw.substring(1, raw.length() - 1).trim();
        }
        if ((raw.startsWith("\"") && raw.endsWith("\"")) ||
            (raw.startsWith("'")  && raw.endsWith("'"))) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    /** 根據 key 類型格式化成 bash 行 */
    private String formatLine(String key, String value) {
        if (ARRAY_KEYS.contains(key)) {
            // 空值寫 ()，有值寫 (val1 val2)
            String inner = (value == null) ? "" : value.trim();
            return key + "=(" + inner + ")";
        }
        // 其餘統一用雙引號
        String v = (value == null) ? "" : value;
        return key + "=\"" + v + "\"";
    }

    // ── PROD 必填欄位 → 顯示名稱 ─────────────────────────────────────────────
    private static final Map<String, String> PROD_REQUIRED;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("PROD_DEPLOY_BASE",      "PROD 部署根目錄（PROD_DEPLOY_BASE）");
        m.put("PROD_BLUE_CHECK_PORTS", "PROD Blue 埠號（PROD_BLUE_CHECK_PORTS）");
        m.put("PROD_GREEN_CHECK_PORTS","PROD Green 埠號（PROD_GREEN_CHECK_PORTS）");
        m.put("PROD_HEALTH_HOST",      "健康檢查主機（PROD_HEALTH_HOST）");
        m.put("PROD_HEALTH_SCHEME",    "健康檢查協議（PROD_HEALTH_SCHEME）");
        m.put("PROD_HEALTH_PATH",      "健康檢查路徑（PROD_HEALTH_PATH）");
        m.put("PROD_NGINX_CONF",       "Nginx 設定檔路徑（PROD_NGINX_CONF）");
        m.put("PROD_LIVE_UPSTREAM",    "正式流量 upstream（PROD_LIVE_UPSTREAM）");
        m.put("PROD_HEADER_UPSTREAM",  "Header 流量 upstream（PROD_HEADER_UPSTREAM）");
        PROD_REQUIRED = Collections.unmodifiableMap(m);
    }
    @Override
    public ConfigSyncResult checkSync(String projectName, ConfigShDTO dto) {
        ConfigSyncResult result = new ConfigSyncResult();
        ProjectEntity proj = projectService.findByName(projectName);
        if (proj == null) { result.addError("找不到專案：" + projectName); return result; }

        // PROD 必填驗證
        if (proj.getHasProd() != null && proj.getHasProd() == 1) {
            Map<String, String> prod = dto.getProd() != null ? dto.getProd() : Map.of();
            PROD_REQUIRED.forEach((key, label) -> {
                String val = prod.getOrDefault(key, "").trim();
                if (val.isBlank()) result.addError("PROD 必填：" + label);
            });

            // nginx conf 格式基本驗證
            String nginxConf = prod.getOrDefault("PROD_NGINX_CONF", "").trim();
            if (!nginxConf.isBlank() && !nginxConf.startsWith("/")) {
                result.addError("PROD_NGINX_CONF 必須是絕對路徑（以 / 開頭）");
            }

            // 埠號格式驗證
            for (String portKey : List.of("PROD_BLUE_CHECK_PORTS", "PROD_GREEN_CHECK_PORTS")) {
                String ports = prod.getOrDefault(portKey, "").trim();
                if (!ports.isBlank()) {
                    for (String p : ports.split("\\s+")) {
                        try { Integer.parseInt(p.trim()); }
                        catch (NumberFormatException e) {
                            result.addError(portKey + " 含非數字值：\"" + p + "\"");
                        }
                    }
                }
            }
        }

        // DEV 基本驗證（寬鬆）
        if (proj.getHasDev() != null && proj.getHasDev() == 1) {
            Map<String, String> dev = dto.getDev() != null ? dto.getDev() : Map.of();
            if (dev.getOrDefault("DEV_HEALTH_HOST", "").isBlank())
                result.addWarning("DEV 建議填寫 DEV_HEALTH_HOST（健康檢查主機）");
        }

        return result;
    }

    @Override
    public ConfigSyncResult syncToRemote(String projectName) {
        ConfigSyncResult result = new ConfigSyncResult();
        ProjectEntity proj = projectService.findByName(projectName);
        if (proj == null) { result.addError("找不到專案：" + projectName); return result; }

        String scriptName = resolveScriptName(projectName);
        String syncScript = toolsBasePath + "/common/sync_config.sh";
        String checkScript = toolsBasePath + "/common/check_remote_file.sh";

        // ── PROD 同步 ──────────────────────────────────────────────────────
        if (proj.getHasProd() != null && proj.getHasProd() == 1) {
            String sshEnv = (proj.getProdSshEnv() != null && !proj.getProdSshEnv().isBlank())
                    ? proj.getProdSshEnv() : "prod";
            ShellExecutor.ExecResult r = ShellExecutor.execMerged(syncScript, sshEnv, scriptName);
            result.putSync("prod", r.isSuccess() ? r.output() : "❌ " + r.output());

            // 遠端 nginx conf 路徑警告
            ConfigShDTO saved;
            try { saved = read(projectName); } catch (Exception ignore) { saved = null; }
            if (saved != null) {
                String nginxConf = saved.getProd().getOrDefault("PROD_NGINX_CONF", "").trim();
                if (!nginxConf.isBlank()) {
                    ShellExecutor.ExecResult chk = ShellExecutor.execMerged(
                            checkScript, sshEnv, "file", nginxConf);
                    if (chk.isSuccess() && chk.output().trim().equals("not_found")) {
                        result.addWarning("[PROD] Nginx 設定檔不存在於遠端：" + nginxConf);
                    }
                }
                String deployBase = saved.getProd().getOrDefault("PROD_DEPLOY_BASE", "").trim();
                if (!deployBase.isBlank()) {
                    ShellExecutor.ExecResult chk = ShellExecutor.execMerged(
                            checkScript, sshEnv, "dir", deployBase);
                    if (chk.isSuccess() && chk.output().trim().equals("not_found")) {
                        result.addWarning("[PROD] 部署根目錄不存在於遠端：" + deployBase);
                    }
                }
            }
        }

        // ── DEV 同步 ───────────────────────────────────────────────────────
        if (proj.getHasDev() != null && proj.getHasDev() == 1) {
            String sshEnv = (proj.getDevSshEnv() != null && !proj.getDevSshEnv().isBlank())
                    ? proj.getDevSshEnv() : "dev";
            ShellExecutor.ExecResult r = ShellExecutor.execMerged(syncScript, sshEnv, scriptName);
            result.putSync("dev", r.isSuccess() ? r.output() : "❌ " + r.output());
        }

        return result;
    }

    /** 取得專案對應的 tools 目錄名稱 */
    private String resolveScriptName(String projectName) {
        ProjectEntity proj = projectService.findByName(projectName);
        if (proj != null && proj.getScriptName() != null && !proj.getScriptName().isBlank()) {
            return proj.getScriptName();
        }
        return projectName;
    }
}
