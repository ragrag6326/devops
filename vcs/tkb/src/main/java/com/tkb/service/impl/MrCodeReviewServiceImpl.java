package com.tkb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.tkb.api.gitlab.GitlabApiClient;
import com.tkb.api.gitlab.dto.GitlabDto;
import com.tkb.api.gitlab.dto.GitlabMrChangeDto;
import com.tkb.api.gitlab.dto.GitlabMrNoteRequest;
import com.tkb.config.GitlabConfig;
import com.tkb.config.MrReviewConfig;
import com.tkb.config.N8nConfig;
import com.tkb.dto.GitlabMrWebhookDTO;
import com.tkb.dto.MrReviewCallbackDTO;
import com.tkb.dto.MrReviewScanResultDTO;
import com.tkb.entity.MrCodeReviewEntity;
import com.tkb.mapper.MrCodeReviewMapper;
import com.tkb.service.MrCodeReviewService;
import com.tkb.utils.AiReviewCallbackNormalizer;
import com.tkb.utils.DiffHashUtil;
import com.tkb.utils.GitlabMrCommentBuilder;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class MrCodeReviewServiceImpl extends ServiceImpl<MrCodeReviewMapper, MrCodeReviewEntity>
        implements MrCodeReviewService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_FAILED = "FAILED";

    private static final Set<String> WEBHOOK_TRIGGER_ACTIONS = Set.of("open", "update", "reopen");

    private final GitlabApiClient gitlabApiClient;
    private final GitlabConfig gitlabConfig;
    private final N8nConfig n8nConfig;
    private final MrReviewConfig mrReviewConfig;
    private final RestTemplate restTemplate;

    @Override
    public MrReviewScanResultDTO scanProject(String projectName) {
        Long projectId = resolveProjectId(projectName);
        String token = gitlabConfig.getToken();
        List<GitlabDto> openedMrs = gitlabApiClient.getMRInfo(projectId, token, "opened");

        MrReviewScanResultDTO result = new MrReviewScanResultDTO();
        result.setProjectName(projectName);
        result.setScanned(openedMrs.size());

        int submitted = 0;
        int skipped = 0;
        for (GitlabDto mr : openedMrs) {
            if (processOpenedMr(projectName, projectId, token, mr)) {
                submitted++;
            } else {
                skipped++;
            }
        }

        result.setSubmitted(submitted);
        result.setSkipped(skipped);
        result.setMessage(String.format("掃描 %d 筆 opened MR，送出 %d 筆，略過 %d 筆", openedMrs.size(), submitted, skipped));
        return result;
    }

    @Override
    public MrReviewScanResultDTO scanAllProjects() {
        int totalScanned = 0, totalSubmitted = 0, totalSkipped = 0;
        for (GitlabConfig.ProjectItem project : gitlabConfig.getProjects()) {
            MrReviewScanResultDTO one = scanProject(project.getName());
            totalScanned += one.getScanned();
            totalSubmitted += one.getSubmitted();
            totalSkipped += one.getSkipped();
        }
        MrReviewScanResultDTO result = new MrReviewScanResultDTO();
        result.setProjectName("ALL");
        result.setScanned(totalScanned);
        result.setSubmitted(totalSubmitted);
        result.setSkipped(totalSkipped);
        result.setMessage(String.format("全專案掃描完成：送出 %d 筆，略過 %d 筆", totalSubmitted, totalSkipped));
        return result;
    }

    @Override
    public void handleCallback(MrReviewCallbackDTO callback) {
        if (callback == null || callback.getReviewId() == null) {
            throw new IllegalArgumentException("reviewId 不可為空");
        }

        MrCodeReviewEntity entity = this.getById(callback.getReviewId());
        if (entity == null) {
            throw new IllegalArgumentException("找不到審核紀錄 id=" + callback.getReviewId());
        }

        AiReviewCallbackNormalizer.normalize(callback);

        String status = normalizeStatus(callback.getStatus());
        entity.setReviewStatus(status);
        entity.setSummary(callback.getSummary());
        entity.setSuggestions(callback.getSuggestions());
        entity.setFullReview(callback.getFullReview());
        entity.setSeverity(callback.getSeverity());

        if (STATUS_COMPLETED.equals(status)) {
            entity.setErrorMessage(null);
        } else if (STATUS_FAILED.equals(status)) {
            entity.setErrorMessage(callback.getErrorMessage());
        } else {
            entity.setErrorMessage(null);
        }

        entity.setReviewedAt(LocalDateTime.now());
        this.updateById(entity);

        if (STATUS_COMPLETED.equals(status) && mrReviewConfig.isPostGitlabComment()) {
            postCommentToGitlab(entity);
        }

        log.info("MR review callback 完成: project={}, mrIid={}, status={}",
                entity.getProjectName(), entity.getMrIid(), status);
    }

    @Override
    public String handleGitlabWebhook(GitlabMrWebhookDTO payload, String gitlabToken) {
        if (StringUtils.hasText(mrReviewConfig.getGitlabWebhookSecret())
                && !mrReviewConfig.getGitlabWebhookSecret().equals(gitlabToken)) {
            throw new IllegalArgumentException("GitLab Webhook Token 驗證失敗");
        }
        if (payload == null || !"merge_request".equals(payload.getObjectKind())) {
            return "ignored: not merge_request";
        }

        GitlabMrWebhookDTO.ObjectAttributes attrs = payload.getObjectAttributes();
        if (attrs == null || attrs.getIid() == null) {
            return "ignored: missing object_attributes";
        }
        if (attrs.getAction() != null && !WEBHOOK_TRIGGER_ACTIONS.contains(attrs.getAction())) {
            return "ignored: action=" + attrs.getAction();
        }
        if (!"opened".equalsIgnoreCase(attrs.getState())) {
            return "ignored: state=" + attrs.getState();
        }
        if (payload.getProject() == null || payload.getProject().getId() == null) {
            return "ignored: missing project.id";
        }

        String projectName = resolveProjectName(payload.getProject().getId());
        boolean submitted = triggerReviewForMr(projectName, attrs.getIid());
        return submitted
                ? "submitted: " + projectName + " !" + attrs.getIid()
                : "skipped: " + projectName + " !" + attrs.getIid();
    }

    @Override
    public boolean triggerReviewForMr(String projectName, Integer mrIid) {
        Long projectId = resolveProjectId(projectName);
        String token = gitlabConfig.getToken();
        GitlabDto mr = gitlabApiClient.getMrByIid(projectId, mrIid, token);
        if (!"opened".equalsIgnoreCase(mr.getState())) {
            return false;
        }
        return processOpenedMr(projectName, projectId, token, mr);
    }

    @Override
    public MrCodeReviewEntity getByProjectAndIid(String projectName, Integer mrIid) {
        return this.lambdaQuery()
                .eq(MrCodeReviewEntity::getProjectName, projectName)
                .eq(MrCodeReviewEntity::getMrIid, mrIid)
                .orderByDesc(MrCodeReviewEntity::getUpdatedAt)
                .last("LIMIT 1")
                .one();
    }

    @Override
    public PageBean page(Integer page, Integer pageSize, String projectName, String reviewStatus, String state) {
        PageHelper.startPage(page, pageSize);
        List<MrCodeReviewEntity> list = this.lambdaQuery()
                .eq(StringUtils.hasText(projectName), MrCodeReviewEntity::getProjectName, projectName)
                .eq(StringUtils.hasText(reviewStatus), MrCodeReviewEntity::getReviewStatus, reviewStatus)
                .eq(StringUtils.hasText(state), MrCodeReviewEntity::getState, state)
                .orderByDesc(MrCodeReviewEntity::getUpdatedAt)
                .list();
        Page<MrCodeReviewEntity> pageList = (Page<MrCodeReviewEntity>) list;
        return new PageBean(pageList.getTotal(), pageList.getResult());
    }

    private boolean processOpenedMr(String projectName, Long projectId, String token, GitlabDto mr) {
        GitlabMrChangeDto changesDto = gitlabApiClient.getMrChanges(projectId, mr.getIid(), token);
        String diffHash = DiffHashUtil.hashChanges(changesDto.getChanges());

        MrCodeReviewEntity existing = this.lambdaQuery()
                .eq(MrCodeReviewEntity::getProjectName, projectName)
                .eq(MrCodeReviewEntity::getMrId, mr.getId())
                .one();

        if (existing != null
                && STATUS_COMPLETED.equals(existing.getReviewStatus())
                && diffHash.equals(existing.getDiffHash())) {
            return false;
        }

        MrCodeReviewEntity entity = existing != null ? existing : new MrCodeReviewEntity();
        entity.setProjectName(projectName);
        entity.setMrId(mr.getId());
        entity.setMrIid(mr.getIid());
        entity.setTitle(mr.getTitle());
        entity.setAuthorName(mr.getAuthor() != null ? mr.getAuthor().getName() : null);
        entity.setState(mr.getState());
        entity.setDiffHash(diffHash);
        entity.setReviewStatus(STATUS_PENDING);
        entity.setSummary(null);
        entity.setSuggestions(null);
        entity.setFullReview(null);
        entity.setSeverity(null);
        entity.setErrorMessage(null);
        entity.setReviewedAt(null);

        if (existing == null) {
            this.save(entity);
        } else {
            this.updateById(entity);
        }

        dispatchToN8n(entity, changesDto.getChanges());
        return true;
    }

    private void dispatchToN8n(MrCodeReviewEntity entity, List<GitlabMrChangeDto.ChangeItem> changes) {
        if (!StringUtils.hasText(n8nConfig.getWebhookUrl())) {
            entity.setReviewStatus(STATUS_FAILED);
            entity.setErrorMessage("N8N webhook 未設定");
            this.updateById(entity);
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("reviewId", entity.getId());
        payload.put("projectName", entity.getProjectName());
        payload.put("mrId", entity.getMrId());
        payload.put("mrIid", entity.getMrIid());
        payload.put("title", entity.getTitle());
        payload.put("authorName", entity.getAuthorName());
        payload.put("state", entity.getState());
        payload.put("diffHash", entity.getDiffHash());
        payload.put("changes", changes);
        payload.put("callbackUrl", n8nConfig.getCallbackBaseUrl());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.hasText(n8nConfig.getAuthToken())) {
            headers.set(n8nConfig.getAuthHeader(), n8nConfig.getAuthToken());
        }

        try {
            restTemplate.postForEntity(n8nConfig.getWebhookUrl(), new HttpEntity<>(payload, headers), String.class);
            log.info("已送出 MR 至 N8N: {} !{}, reviewId={}", entity.getProjectName(), entity.getMrIid(), entity.getId());
        } catch (Exception e) {
            log.error("N8N webhook 呼叫失敗: reviewId={}", entity.getId(), e);
            entity.setReviewStatus(STATUS_FAILED);
            entity.setErrorMessage("N8N 呼叫失敗: " + e.getMessage());
            this.updateById(entity);
        }
    }

    private void postCommentToGitlab(MrCodeReviewEntity entity) {
        try {
            Long projectId = resolveProjectId(entity.getProjectName());
            gitlabApiClient.createMrNote(
                    projectId,
                    entity.getMrIid(),
                    gitlabConfig.getToken(),
                    new GitlabMrNoteRequest(GitlabMrCommentBuilder.build(entity))
            );
            log.info("已留言至 GitLab MR: {} !{}", entity.getProjectName(), entity.getMrIid());
        } catch (Exception e) {
            log.error("GitLab MR 留言失敗: {} !{}", entity.getProjectName(), entity.getMrIid(), e);
        }
    }

    private Long resolveProjectId(String projectName) {
        return gitlabConfig.getProjects().stream()
                .filter(p -> p.getName().equals(projectName))
                .findFirst()
                .map(GitlabConfig.ProjectItem::getId)
                .orElseThrow(() -> new IllegalArgumentException("找不到專案: " + projectName));
    }

    private String resolveProjectName(Long projectId) {
        return gitlabConfig.getProjects().stream()
                .filter(p -> projectId.equals(p.getId()))
                .findFirst()
                .map(GitlabConfig.ProjectItem::getName)
                .orElseThrow(() -> new IllegalArgumentException("Webhook 專案 id=" + projectId + " 未設定"));
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return STATUS_COMPLETED;
        }
        String upper = status.trim().toUpperCase();
        if (STATUS_FAILED.equals(upper) || STATUS_PENDING.equals(upper) || STATUS_COMPLETED.equals(upper)) {
            return upper;
        }
        return STATUS_COMPLETED;
    }
}
