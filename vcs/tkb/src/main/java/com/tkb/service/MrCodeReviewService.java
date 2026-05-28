package com.tkb.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tkb.dto.GitlabMrWebhookDTO;
import com.tkb.dto.MrReviewCallbackDTO;
import com.tkb.dto.MrReviewScanResultDTO;
import com.tkb.entity.MrCodeReviewEntity;
import com.tkb.vo.PageBean;

public interface MrCodeReviewService extends IService<MrCodeReviewEntity> {

    MrReviewScanResultDTO scanProject(String projectName);

    MrReviewScanResultDTO scanAllProjects();

    void handleCallback(MrReviewCallbackDTO callback);

    /** GitLab MR Webhook：有新 MR / 更新 push 時即時觸發審核 */
    String handleGitlabWebhook(GitlabMrWebhookDTO payload, String gitlabToken);

    /** 手動對單一 MR 觸發審核 */
    boolean triggerReviewForMr(String projectName, Integer mrIid);

    MrCodeReviewEntity getByProjectAndIid(String projectName, Integer mrIid);

    PageBean page(Integer page, Integer pageSize, String projectName, String reviewStatus, String state);
}
