package com.tkb.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tkb.api.gitlab.DeployGitFileClient;
import com.tkb.api.gitlab.dto.GitlabCommitRequest;
import com.tkb.dto.InitProjectDTO;
import com.tkb.service.DeployGitConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeployGitConfigServiceImpl implements DeployGitConfigService {

    /** remote_gitlab_repo.json 的分類，對應 Jenkins 的 TYPE 參數 */
    private static final Set<String> ALLOWED_TYPES = Set.of("backend", "frontend");
    /** Dockerfile 模板目錄，對應 PROJECT_ENV（template/tv 是舊 war 專案的雜項檔案，不在此管理） */
    private static final Set<String> ALLOWED_ENVS = Set.of("dev", "local", "prod", "admin");
    /** PROJECT_NAME 限制：避免路徑跳脫（../）或產生 shell 上難處理的檔名 */
    private static final Pattern PROJECT_NAME_PATTERN = Pattern.compile("^[A-Za-z0-9._-]+$");
    private static final Pattern TEMPLATE_PATH_PATTERN =
            Pattern.compile("^template/(dev|local|prod|admin)/(.+)_Dockerfile$");

    private final DeployGitFileClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── remote_gitlab_repo.json ─────────────────────────────────────
    // 結構特殊：{"backend":[{...專案們...}],"frontend":[{...}]}（陣列包一個物件），
    // shell 用 jq ".${TYPE}[][\"${PROJECT_NAME}\"].repository_url" 讀取，
    // 因此這裡「保留原結構」只操作內容，不需要動任何 shell。

    @Override
    public JsonNode readGitlabRepo() {
        return readJson(DeployGitFileClient.REMOTE_GITLAB_REPO_PATH);
    }

    @Override
    public JsonNode upsertGitlabRepo(String type, String projectName, String repositoryUrl, String commitMessage) {
        validateType(type);
        validateProjectName(projectName);
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new RuntimeException("repository_url 不可為空");
        }

        ObjectNode root = (ObjectNode) readGitlabRepo();
        upsertRepoEntry(root, type, projectName, repositoryUrl);

        String msg = defaultMsg(commitMessage,
                "[deploy-registry] 更新 repo 對照 " + type + "/" + projectName);
        client.commit(msg, List.of(updateAction(DeployGitFileClient.REMOTE_GITLAB_REPO_PATH, root)));
        return root;
    }

    @Override
    public JsonNode deleteGitlabRepo(String type, String projectName, String commitMessage) {
        validateType(type);
        ObjectNode root = (ObjectNode) readGitlabRepo();

        boolean removed = removeRepoEntry(root, type, projectName);
        if (!removed) {
            throw new RuntimeException("repo 對照 " + type + "/" + projectName + " 不存在，未執行任何變更");
        }

        String msg = defaultMsg(commitMessage,
                "[deploy-registry] 移除 repo 對照 " + type + "/" + projectName);
        client.commit(msg, List.of(updateAction(DeployGitFileClient.REMOTE_GITLAB_REPO_PATH, root)));
        return root;
    }

    // ── template/{ENV}/{PROJECT_NAME}_Dockerfile ────────────────────

    @Override
    public List<Map<String, String>> listTemplates() {
        List<Map<String, String>> result = new ArrayList<>();
        for (String path : client.listTree(DeployGitFileClient.TEMPLATE_DIR)) {
            Matcher m = TEMPLATE_PATH_PATTERN.matcher(path);
            if (m.matches()) {
                Map<String, String> item = new LinkedHashMap<>();
                item.put("env", m.group(1));
                item.put("projectName", m.group(2));
                item.put("path", path);
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public String readTemplate(String env, String projectName) {
        return client.readFile(templatePath(env, projectName));
    }

    @Override
    public void writeTemplate(String env, String projectName, String content, String commitMessage) {
        if (content == null || content.isBlank()) {
            throw new RuntimeException("Dockerfile 內容不可為空");
        }
        String path = templatePath(env, projectName);
        String action = client.fileExists(path) ? "update" : "create";
        String msg = defaultMsg(commitMessage,
                "[deploy-registry] " + ("create".equals(action) ? "新增" : "更新") + " Dockerfile 模板 " + path);
        client.commit(msg, List.of(new GitlabCommitRequest.Action(action, path, content)));
    }

    @Override
    public void deleteTemplate(String env, String projectName, String commitMessage) {
        String path = templatePath(env, projectName);
        if (!client.fileExists(path)) {
            throw new RuntimeException("模板 " + path + " 不存在，未執行任何變更");
        }
        String msg = defaultMsg(commitMessage, "[deploy-registry] 移除 Dockerfile 模板 " + path);
        client.commit(msg, List.of(new GitlabCommitRequest.Action("delete", path, null)));
    }

    // ── vmIP.json ───────────────────────────────────────────────────

    @Override
    public JsonNode readVmIp() {
        return readJson(DeployGitFileClient.VM_IP_PATH);
    }

    @Override
    public JsonNode writeVmIp(JsonNode vmIp, String commitMessage) {
        if (vmIp == null || !vmIp.isObject() || vmIp.isEmpty()) {
            throw new RuntimeException("vmIP.json 必須是非空的 JSON object");
        }
        // shell 的 getVMIP() 直接 jq -r .${PROJECT_ENV}，值必須是純字串
        for (Iterator<Map.Entry<String, JsonNode>> it = vmIp.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            if (!entry.getValue().isTextual() || entry.getValue().asText().isBlank()) {
                throw new RuntimeException("環境 " + entry.getKey() + " 的 IP 必須是非空字串");
            }
        }
        String msg = defaultMsg(commitMessage, "[deploy-registry] 更新 vmIP.json");
        client.commit(msg, List.of(updateAction(DeployGitFileClient.VM_IP_PATH, vmIp)));
        return vmIp;
    }

    // ── 跨檔案原子操作 ───────────────────────────────────────────────

    @Override
    public JsonNode initProject(InitProjectDTO dto) {
        // 1. 驗證
        validateProjectName(dto.getProjectName());
        validateType(dto.getType());
        if (dto.getRepositoryUrl() == null || dto.getRepositoryUrl().isBlank()) {
            throw new RuntimeException("repositoryUrl 不可為空");
        }
        if (dto.getDeployConfig() == null || !dto.getDeployConfig().isObject()) {
            throw new RuntimeException("deployConfig 必須是 JSON object");
        }
        if (!dto.getDeployConfig().has("envs") || !dto.getDeployConfig().get("envs").isObject()
                || dto.getDeployConfig().get("envs").isEmpty()) {
            throw new RuntimeException("deployConfig 至少要有一筆 envs.{ENV} 設定（genericDeploy.sh 依此取得 buildType/sshUser）");
        }
        if (dto.getDockerfiles() != null) {
            for (Map.Entry<String, String> e : dto.getDockerfiles().entrySet()) {
                validateEnv(e.getKey());
                if (e.getValue() == null || e.getValue().isBlank()) {
                    throw new RuntimeException("環境 " + e.getKey() + " 的 Dockerfile 內容不可為空");
                }
            }
        }

        String name = dto.getProjectName();
        List<GitlabCommitRequest.Action> actions = new ArrayList<>();

        // 2. project_deploy.json：新增節點（已存在則拒絕，請改用編輯功能）
        ObjectNode deployRoot = (ObjectNode) readJson(DeployGitFileClient.PROJECT_DEPLOY_PATH);
        if (deployRoot.has(name)) {
            throw new RuntimeException("專案 " + name + " 已存在於 project_deploy.json，請改用編輯功能");
        }
        deployRoot.set(name, dto.getDeployConfig());
        actions.add(updateAction(DeployGitFileClient.PROJECT_DEPLOY_PATH, deployRoot));

        // 3. remote_gitlab_repo.json：upsert repo URL
        ObjectNode repoRoot = (ObjectNode) readGitlabRepo();
        upsertRepoEntry(repoRoot, dto.getType(), name, dto.getRepositoryUrl());
        actions.add(updateAction(DeployGitFileClient.REMOTE_GITLAB_REPO_PATH, repoRoot));

        // 4. Dockerfile 模板（存在同名檔案時用 update，避免 create 撞名導致整個 commit 失敗）
        List<String> files = new ArrayList<>(List.of(
                DeployGitFileClient.PROJECT_DEPLOY_PATH,
                DeployGitFileClient.REMOTE_GITLAB_REPO_PATH));
        if (dto.getDockerfiles() != null) {
            for (Map.Entry<String, String> e : dto.getDockerfiles().entrySet()) {
                String path = templatePath(e.getKey(), name);
                String action = client.fileExists(path) ? "update" : "create";
                actions.add(new GitlabCommitRequest.Action(action, path, e.getValue()));
                files.add(path);
            }
        }

        // 5. 單一 commit：全部成功或全部不寫入
        client.commit("[deploy-registry] 新增專案 " + name + "（" + dto.getType() + "）", actions);

        ObjectNode result = objectMapper.createObjectNode();
        result.put("projectName", name);
        ArrayNode fileArr = result.putArray("committedFiles");
        files.forEach(fileArr::add);
        return result;
    }

    @Override
    public JsonNode removeProject(String projectName, boolean removeTemplates, boolean removeRepo, String commitMessage) {
        validateProjectName(projectName);
        List<GitlabCommitRequest.Action> actions = new ArrayList<>();

        // 1. project_deploy.json：移除節點
        ObjectNode deployRoot = (ObjectNode) readJson(DeployGitFileClient.PROJECT_DEPLOY_PATH);
        if (!deployRoot.has(projectName)) {
            throw new RuntimeException("專案 " + projectName + " 不存在於 project_deploy.json，未執行任何變更");
        }
        deployRoot.remove(projectName);
        actions.add(updateAction(DeployGitFileClient.PROJECT_DEPLOY_PATH, deployRoot));

        // 2. remote_gitlab_repo.json：兩個分類都找一遍
        if (removeRepo) {
            ObjectNode repoRoot = (ObjectNode) readGitlabRepo();
            boolean removed = false;
            for (String type : ALLOWED_TYPES) {
                removed |= removeRepoEntry(repoRoot, type, projectName);
            }
            if (removed) {
                actions.add(updateAction(DeployGitFileClient.REMOTE_GITLAB_REPO_PATH, repoRoot));
            }
        }

        // 3. Dockerfile 模板：把該專案在各環境的模板全部刪掉
        if (removeTemplates) {
            for (Map<String, String> tpl : listTemplates()) {
                if (projectName.equals(tpl.get("projectName"))) {
                    actions.add(new GitlabCommitRequest.Action("delete", tpl.get("path"), null));
                }
            }
        }

        String msg = defaultMsg(commitMessage, "[deploy-registry] 移除專案 " + projectName
                + (removeRepo ? "（含 repo 對照）" : "")
                + (removeTemplates ? "（含 Dockerfile 模板）" : ""));
        client.commit(msg, actions);
        return deployRoot;
    }

    // ─────────────────────────── private ───────────────────────────

    private JsonNode readJson(String filePath) {
        String content = client.readFile(filePath);
        if (content == null) {
            throw new RuntimeException(filePath + " 不存在於 deploy.git，請確認分支設定");
        }
        try {
            return objectMapper.readTree(content);
        } catch (Exception e) {
            throw new RuntimeException("解析 " + filePath + " 失敗: " + e.getMessage(), e);
        }
    }

    /** upsert {type}[0].{projectName}.repository_url，維持「陣列包一個物件」的原始結構 */
    private void upsertRepoEntry(ObjectNode root, String type, String projectName, String repositoryUrl) {
        ArrayNode arr = root.withArray(type);
        ObjectNode holder;
        if (arr.isEmpty()) {
            holder = arr.addObject();
        } else {
            holder = (ObjectNode) arr.get(0);
        }
        ObjectNode entry = objectMapper.createObjectNode();
        entry.put("repository_url", repositoryUrl);
        holder.set(projectName, entry);
    }

    /** 從 {type} 陣列所有元素中移除 {projectName}，回傳是否有移除 */
    private boolean removeRepoEntry(ObjectNode root, String type, String projectName) {
        JsonNode arr = root.get(type);
        if (arr == null || !arr.isArray()) {
            return false;
        }
        boolean removed = false;
        for (JsonNode holder : arr) {
            if (holder.isObject() && holder.has(projectName)) {
                ((ObjectNode) holder).remove(projectName);
                removed = true;
            }
        }
        return removed;
    }

    private GitlabCommitRequest.Action updateAction(String filePath, JsonNode content) {
        try {
            return new GitlabCommitRequest.Action("update", filePath,
                    objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(content));
        } catch (Exception e) {
            throw new RuntimeException("序列化 " + filePath + " 失敗: " + e.getMessage(), e);
        }
    }

    private String templatePath(String env, String projectName) {
        validateEnv(env);
        validateProjectName(projectName);
        return DeployGitFileClient.TEMPLATE_DIR + "/" + env + "/" + projectName + "_Dockerfile";
    }

    private void validateType(String type) {
        if (type == null || !ALLOWED_TYPES.contains(type)) {
            throw new RuntimeException("type 僅允許 backend / frontend（對應 Jenkins 的 TYPE 參數）");
        }
    }

    private void validateEnv(String env) {
        if (env == null || !ALLOWED_ENVS.contains(env)) {
            throw new RuntimeException("env 僅允許 dev / local / prod / admin");
        }
    }

    private void validateProjectName(String projectName) {
        if (projectName == null || !PROJECT_NAME_PATTERN.matcher(projectName).matches()) {
            throw new RuntimeException("PROJECT_NAME 僅允許英數字與 . _ -（需與 Jenkins 參數值一致）");
        }
    }

    private String defaultMsg(String commitMessage, String fallback) {
        return (commitMessage != null && !commitMessage.isBlank()) ? commitMessage : fallback;
    }
}
