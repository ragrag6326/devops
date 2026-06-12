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

    String handleGitlabWebhook(GitlabMrWebhookDTO payload, String gitlabToken);

    boolean triggerReviewForMr(String projectName, Integer mrIid);

    MrCodeReviewEntity getByProjectAndIid(String projectName, Integer mrIid);

    PageBean page(Integer page, Integer pageSize, String projectName, String reviewStatus, String state);
}
