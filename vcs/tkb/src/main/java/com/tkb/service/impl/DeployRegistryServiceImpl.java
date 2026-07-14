package com.tkb.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tkb.api.gitlab.DeployGitFileClient;
import com.tkb.api.gitlab.dto.GitlabCommitRequest;
import com.tkb.api.gitlab.dto.GitlabFileDto;
import com.tkb.service.DeployRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 讀寫 deploy.git 內 config/project_deploy.json 的實作。
 * GitLab API 存取細節（%2F 編碼、base64、commit）統一在 {@link DeployGitFileClient}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeployRegistryServiceImpl implements DeployRegistryService {

    private static final String FILE_PATH = DeployGitFileClient.PROJECT_DEPLOY_PATH;

    private final DeployGitFileClient deployGitFileClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public JsonNode readAll() {
        String content = deployGitFileClient.readFile(FILE_PATH);
        if (content == null) {
            throw new RuntimeException(FILE_PATH + " 不存在於 deploy.git，請確認分支設定");
        }
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new RuntimeException("解析 " + FILE_PATH + " 失敗: " + e.getMessage(), e);
        }
    }

    @Override
    public String lastCommitId() {
        GitlabFileDto meta = deployGitFileClient.readFileMeta(FILE_PATH);
        if (meta == null) {
            throw new RuntimeException(FILE_PATH + " 不存在於 deploy.git，請確認分支設定");
        }
        return meta.getLast_commit_id();
    }

    @Override
    public JsonNode readProject(String projectName) {
        JsonNode root = readAll();
        JsonNode node = root.get(projectName);
        return node != null ? node : objectMapper.createObjectNode();
    }

    @Override
    public JsonNode writeProject(String projectName, JsonNode projectConfig, String commitMessage, String lastCommitId) {
        if (projectConfig == null || !projectConfig.isObject()) {
            throw new RuntimeException("專案設定必須是 JSON object");
        }
        ObjectNode root = (ObjectNode) readAll();
        root.set(projectName, projectConfig);
        String msg = (commitMessage != null && !commitMessage.isBlank())
                ? commitMessage
                : "[deploy-registry] 更新 " + projectName + " 設定";
        return commit(root, msg, lastCommitId);
    }

    @Override
    public JsonNode deleteProject(String projectName, String commitMessage) {
        ObjectNode root = (ObjectNode) readAll();
        if (!root.has(projectName)) {
            throw new RuntimeException("專案 " + projectName + " 不存在於 " + FILE_PATH + "，未執行任何變更");
        }
        root.remove(projectName);
        String msg = (commitMessage != null && !commitMessage.isBlank())
                ? commitMessage
                : "[deploy-registry] 移除 " + projectName + " 設定";
        return commit(root, msg, null);
    }

    /** 序列化整份 JSON 並透過 GitLab Commit API 直接 commit + push 回 deploy.git */
    private JsonNode commit(ObjectNode root, String message, String lastCommitId) {
        try {
            String content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            GitlabCommitRequest.Action action = new GitlabCommitRequest.Action(
                    "update", FILE_PATH, content,
                    (lastCommitId != null && !lastCommitId.isBlank()) ? lastCommitId : null);
            deployGitFileClient.commit(message, List.of(action));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("寫回 deploy.git 失敗: " + e.getMessage(), e);
        }
        return root;
    }
}
