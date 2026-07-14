package com.tkb.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tkb.utils.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Tag(name = "6.0.0 系統資訊", description = "提供當前部署環境等系統基本資訊")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/system")
public class SystemController {

    /**
     * 由 application.yml 的 app.env 注入，測試機填 dev，正式機填 prod
     */
    @Value("${app.env:dev}")
    private String appEnv;

    /**
     * 機器對照表位置（sshToolUtil.sh 讀同一份）。
     * 新增遠端機器只需在此 JSON 加一筆＋放金鑰，前端下拉自動連動。
     */
    @Value("${app.ssh-hosts-file:/opt/vcs/tools/common/ssh_hosts.json}")
    private String sshHostsFile;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(
        summary = "6.0.1 取得當前環境",
        description = "前端呼叫此 API 判斷目前連線的是測試機 (dev) 還是正式機 (prod)"
    )
    @GetMapping("/env")
    public Result<Map<String, String>> getEnv() {
        return Result.success(Map.of("env", appEnv));
    }

    @Operation(
        summary = "6.0.3 讀取 ssh_hosts.json 原始內容",
        description = "回傳完整 JSON（含 user/key/label 與 _comment），供前端「機器 IP 同步」合併使用。檔案不存在回傳空物件"
    )
    @GetMapping("/ssh-hosts/raw")
    public Result<JsonNode> getSshHostsRaw() {
        try {
            File f = new File(sshHostsFile);
            if (!f.isFile()) {
                return Result.success(objectMapper.createObjectNode());
            }
            return Result.success(objectMapper.readTree(f));
        } catch (Exception e) {
            log.error("[SystemController] 讀取 {} 失敗: {}", sshHostsFile, e.getMessage());
            return Result.error("讀取 ssh_hosts.json 失敗：" + e.getMessage());
        }
    }

    @Operation(
        summary = "6.0.4 覆寫 ssh_hosts.json",
        description = "寫入本機 " + "tools/common/ssh_hosts.json（sshToolUtil.sh 查表用同一份）。"
                + "每筆需含非空 host；金鑰檔仍需手動放置到對應路徑"
    )
    @PutMapping("/ssh-hosts")
    public Result<JsonNode> writeSshHosts(@RequestBody JsonNode body) {
        try {
            if (body == null || !body.isObject() || body.isEmpty()) {
                return Result.error("內容必須是非空的 JSON object");
            }
            for (Iterator<Map.Entry<String, JsonNode>> it = body.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> e = it.next();
                if (e.getKey().startsWith("_")) {
                    continue; // _comment 等說明欄位
                }
                if (!e.getValue().isObject() || e.getValue().path("host").asText("").isBlank()) {
                    return Result.error("環境 " + e.getKey() + " 缺少 host");
                }
            }
            File f = new File(sshHostsFile);
            if (f.getParentFile() != null) {
                f.getParentFile().mkdirs();
            }
            java.nio.file.Files.writeString(f.toPath(),
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(body));
            log.info("[SystemController] 已更新 {}", sshHostsFile);
            return Result.success(body);
        } catch (Exception e) {
            log.error("[SystemController] 寫入 {} 失敗: {}", sshHostsFile, e.getMessage());
            return Result.error("寫入 ssh_hosts.json 失敗：" + e.getMessage());
        }
    }

    @Operation(
        summary = "6.0.2 取得可用 SSH 機器清單",
        description = "讀取 tools/common/ssh_hosts.json（sshToolUtil.sh 用同一份查 host/key）。"
                + "回傳 [{env, host, label}]，供 /system/project 的 SSH 目標下拉動態顯示。"
                + "檔案不存在時回傳內建 prod/dev 預設值（與 sshToolUtil.sh 的 fallback 一致）"
    )
    @GetMapping("/ssh-hosts")
    public Result<List<Map<String, String>>> getSshHosts() {
        List<Map<String, String>> hosts = new ArrayList<>();
        try {
            File f = new File(sshHostsFile);
            if (f.isFile()) {
                JsonNode root = objectMapper.readTree(f);
                for (Iterator<Map.Entry<String, JsonNode>> it = root.fields(); it.hasNext(); ) {
                    Map.Entry<String, JsonNode> e = it.next();
                    if (e.getKey().startsWith("_") || !e.getValue().isObject()) {
                        continue; // 跳過 _comment 等說明欄位
                    }
                    Map<String, String> item = new LinkedHashMap<>();
                    item.put("env", e.getKey());
                    item.put("host", e.getValue().path("host").asText(""));
                    item.put("label", e.getValue().path("label").asText(e.getKey()));
                    hosts.add(item);
                }
            }
        } catch (Exception ex) {
            log.warn("[SystemController] 讀取 {} 失敗，改用內建預設: {}", sshHostsFile, ex.getMessage());
        }
        if (hosts.isEmpty()) {
            // 與 sshToolUtil.sh 內建 case 一致的 fallback
            hosts.add(Map.of("env", "prod", "host", "132.145.125.250", "label", "正式機"));
            hosts.add(Map.of("env", "dev", "host", "131.186.44.40", "label", "測試機"));
        }
        return Result.success(hosts);
    }
}
