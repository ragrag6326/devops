package com.tkb.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Map;

/**
 * 新增專案精靈請求：以單一 GitLab commit 同時寫入
 * config/project_deploy.json + config/remote_gitlab_repo.json + template/{env}/{name}_Dockerfile。
 * 全部成功或全部不寫入，不會出現「對照表有了但模板沒有」的半套狀態。
 */
@Data
public class InitProjectDTO {

    @Schema(description = "PROJECT_NAME，需與 Jenkins 參數值一致", example = "new_project")
    private String projectName;

    @Schema(description = "remote_gitlab_repo.json 的分類，對應 Jenkins 的 TYPE", allowableValues = {"backend", "frontend"})
    private String type;

    @Schema(description = "GitLab 倉庫位置", example = "ssh://git@192.168.1.35:2224/tkb/xxx.git")
    private String repositoryUrl;

    @Schema(description = "project_deploy.json 的專案節點內容（原始 JSON，至少要有 envs.{ENV}.buildType / sshUser）")
    private JsonNode deployConfig;

    @Schema(description = "各環境的 Dockerfile 模板內容，key 為 PROJECT_ENV（dev/local/prod/admin）")
    private Map<String, String> dockerfiles;
}
