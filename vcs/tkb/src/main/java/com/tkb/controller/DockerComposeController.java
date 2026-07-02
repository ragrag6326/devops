package com.tkb.controller;

import com.tkb.entity.ProjectEntity;
import com.tkb.service.ProjectService;
import com.tkb.utils.ShellExecutor;
import com.tkb.utils.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@Tag(name = "7.0.0 Docker Compose 管理", description = "透過 SSH 讀取/寫入遠端 docker-compose.yml")
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/docker-compose")
public class DockerComposeController {

    @Value("${app.tools-base-path:/opt/vcs/tools}")
    private String toolsBasePath;

    private final ProjectService projectService;

    // ── 讀取 ────────────────────────────────────────────────────────────────

    @Operation(summary = "7.0.1 讀取 docker-compose.yml",
               description = "PROD: /opt/docker_image/{project}/docker-compose.yml  " +
                             "DEV: /opt/docker_image/docker-compose.yml")
    @GetMapping("/{projectName}")
    public Result<Map<String, String>> read(
            @Parameter(description = "DB project_config.name", required = true)
            @PathVariable String projectName,
            @Parameter(description = "prod | dev", required = true)
            @RequestParam String env) {

        ProjectEntity proj = projectService.findByName(projectName);
        if (proj == null) return Result.error("找不到專案：" + projectName);

        String sshEnv      = resolveSshEnv(proj, env);
        String composePath = resolveComposePath(proj, env, projectName);
        String script      = toolsBasePath + "/common/read_docker_compose.sh";

        log.info("[DockerCompose] read sshEnv={} path={}", sshEnv, composePath);
        ShellExecutor.ExecResult r = ShellExecutor.execMerged(script, sshEnv, composePath);
        if (!r.isSuccess()) {
            log.error("[DockerCompose] 讀取失敗 env={} project={}: {}", env, projectName, r.output());
            return Result.error("讀取失敗：" + r.output());
        }

        String content = r.output();
        if (content.trim().equals("__NOT_FOUND__")) {
            return Result.success(Map.of("content", "", "exists", "false"));
        }
        return Result.success(Map.of("content", content, "exists", "true"));
    }

    // ── 寫入 ────────────────────────────────────────────────────────────────

    @Operation(summary = "7.0.2 寫入 docker-compose.yml",
               description = "透過 SSH 將內容寫回遠端 docker-compose.yml（無則建立）")
    @PostMapping("/{projectName}")
    public Result<String> write(
            @Parameter(description = "DB project_config.name", required = true)
            @PathVariable String projectName,
            @Parameter(description = "prod | dev", required = true)
            @RequestParam String env,
            @RequestBody Map<String, String> body) {

        String content = body.getOrDefault("content", "");
        if (content.isBlank()) return Result.error("內容不可為空");

        ProjectEntity proj = projectService.findByName(projectName);
        if (proj == null) return Result.error("找不到專案：" + projectName);

        // 寫入暫存檔
        String tmpPath = "/tmp/dc-" + UUID.randomUUID() + ".yml";
        try {
            Files.writeString(Paths.get(tmpPath), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Result.error("暫存檔建立失敗：" + e.getMessage());
        }

        String sshEnv      = resolveSshEnv(proj, env);
        String composePath = resolveComposePath(proj, env, projectName);
        String script      = toolsBasePath + "/common/write_docker_compose.sh";

        log.info("[DockerCompose] write sshEnv={} path={}", sshEnv, composePath);
        try {
            ShellExecutor.ExecResult r = ShellExecutor.execMerged(script, sshEnv, composePath, tmpPath);
            if (!r.isSuccess()) {
                log.error("[DockerCompose] 寫入失敗 env={} project={}: {}", env, projectName, r.output());
                return Result.error("寫入失敗：" + r.output());
            }
            log.info("[DockerCompose] 寫入成功 env={} project={}", env, projectName);
            return Result.success(r.output());
        } finally {
            try { Files.deleteIfExists(Paths.get(tmpPath)); } catch (IOException ignore) {}
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    /**
     * 決定實際 SSH 目標機器（prod_ssh_env / dev_ssh_env 覆蓋）
     * form-service 的 prod_ssh_env="dev" → SSH 到 dev 機器，但路徑仍為 per-project
     */
    private String resolveSshEnv(ProjectEntity proj, String env) {
        if ("prod".equals(env)) {
            return (proj.getProdSshEnv() != null && !proj.getProdSshEnv().isBlank())
                    ? proj.getProdSshEnv() : "prod";
        }
        return (proj.getDevSshEnv() != null && !proj.getDevSshEnv().isBlank())
                ? proj.getDevSshEnv() : "dev";
    }

    /**
     * 決定遠端 docker-compose.yml 路徑：
     * <ul>
     *   <li>env=prod（含 prod_ssh_env 覆蓋至 dev）→ per-project /opt/docker_image/{name}/docker-compose.yml</li>
     *   <li>env=dev（純 DEV 配置）→ 共用 /opt/docker_image/docker-compose.yml</li>
     * </ul>
     *
     * 這樣 form-service-frontend（prod_ssh_env=dev）的 PROD compose 路徑正確：
     * SSH=dev, PATH=/opt/docker_image/form-service-frontend/docker-compose.yml
     */
    private String resolveComposePath(ProjectEntity proj, String env, String projectName) {
        if ("prod".equals(env)) {
            return "/opt/docker_image/" + projectName + "/docker-compose.yml";
        }
        return "/opt/docker_image/docker-compose.yml";
    }
}
