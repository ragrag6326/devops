package com.tkb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.tkb.api.gitlab.GitlabApiClient;
import com.tkb.api.gitlab.dto.GitlabDto;
import com.tkb.config.GitlabConfig;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.mapper.GitlabMrMapper;
import com.tkb.service.GitlabMrService;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitlabMrServiceImpl extends ServiceImpl<GitlabMrMapper, GitlabMrEntity> implements GitlabMrService {

    private final GitlabApiClient gitlabApiClient;
    private final GitlabConfig gitlabConfig;



    @Override
    public List<GitlabMrEntity> listByProjectName(String projectName) {
        List<GitlabMrEntity> list = this.list(new LambdaQueryWrapper<GitlabMrEntity>()
                .eq(GitlabMrEntity::getProjectName, projectName));
        return list;
    }

    @Override
    public String  syncFromGitlab(String projectName) {

        // Token
        String token = gitlabConfig.getToken();

        // 找對應專案的 project ID
        Long projectId = gitlabConfig.getProjects()
                .stream()
                .filter(project -> project.getName().equals(projectName))
                .findFirst()
                .map(GitlabConfig.ProjectItem::getId)
                .orElse(-1L);

        if (projectId == -1) {
            return "projectId 獲取失敗";
        }

        List<GitlabDto> list  = gitlabApiClient.getMRInfo(projectId, token);
        for(GitlabDto mrDto : list) {
            GitlabMrEntity MrEntity = new GitlabMrEntity();

            MrEntity.setProjectName(projectName);
            MrEntity.setMrId(mrDto.getId());
            MrEntity.setIid(mrDto.getIid());
            MrEntity.setTitle(mrDto.getTitle());
            MrEntity.setDescription(mrDto.getDescription());
            MrEntity.setState(mrDto.getState());
            MrEntity.setTargetBranch(mrDto.getTarget_branch());
            MrEntity.setSourceBranch(mrDto.getSource_branch());
            MrEntity.setAuthorName(mrDto.getAuthor() != null ? mrDto.getAuthor().getName() : null);
            MrEntity.setMergedBy(mrDto.getMerged_by() != null ? mrDto.getMerged_by().getName() : null);
            MrEntity.setMergedAt(mrDto.getMerged_at() != null ? mrDto.getMerged_at().toLocalDateTime() : null);
            MrEntity.setCreatedAt(mrDto.getCreated_at().toLocalDateTime());
            MrEntity.setUpdatedAt(mrDto.getUpdated_at().toLocalDateTime());
            System.out.println(MrEntity);

            // 以 project_name 跟 mr_id 判斷避免重複寫入
            this.saveOrUpdate(MrEntity,
                    new LambdaQueryWrapper<GitlabMrEntity>()
                            .eq(GitlabMrEntity::getMrId, MrEntity.getMrId())
                            .eq(GitlabMrEntity::getProjectName, projectName)
            );

        }
        return "同步完成";
    }

    @Override
    public List<GitlabMrEntity> getMergedMrsBetween(String projectName, String targetBranch, LocalDateTime start, LocalDateTime end) {
        return this.lambdaQuery()
                .eq(GitlabMrEntity::getProjectName, projectName)
                .eq(GitlabMrEntity::getState, "merged") // 只找已合併的
                .eq(GitlabMrEntity::getTargetBranch, targetBranch)   // 動態傳入 develop 或 main
                .isNull(GitlabMrEntity::getVersion) // 只選取尚未被標記的 MR
                .between(GitlabMrEntity::getMergedAt, start, end)
                .orderByAsc(GitlabMrEntity::getMergedAt) // 依照合併時間排序
                .list();
    }

    @Override
    public void stampVersionForMrs(List<Long> mrEntityIds, String version) {
        if (mrEntityIds == null || mrEntityIds.isEmpty()) {
            return;

        }
        // 批量更新功能
        this.lambdaUpdate()
                .set(GitlabMrEntity::getVersion, version) // 設定版本號
                .in(GitlabMrEntity::getId, mrEntityIds)   // 根據 DB 的主鍵 ID 列表進行更新
                .update();
    }

    @Override
    public void unstampVersionForMrs(String projectName, String version) {
        // 將特定版本號的 MR 的 version 欄位設為 NULL
        this.lambdaUpdate()
                .set(GitlabMrEntity::getVersion, null) // 設為 NULL，解除綁定
                .eq(GitlabMrEntity::getProjectName, projectName)
                .eq(GitlabMrEntity::getVersion, version) // 鎖定目標版本
                .update();
    }

    @Override
    public PageBean page(Integer page, Integer pageSize, String projectName, String state, LocalDate mrStartDate, LocalDate mrEndDate) {


        PageHelper.startPage(page, pageSize);

        List<GitlabMrEntity> list = this.lambdaQuery()
                .like(projectName != null && !projectName.isEmpty(), GitlabMrEntity::getProjectName, projectName)
                .eq(state != null && !state.isEmpty(), GitlabMrEntity::getState, state)
                .between(mrStartDate != null && mrEndDate != null, GitlabMrEntity::getMergedAt, mrStartDate, mrEndDate)
                .orderByDesc(GitlabMrEntity::getMergedAt) // 依照合併時間排序
                .list();

        Page<GitlabMrEntity> pageList = (Page<GitlabMrEntity>) list;

        return new PageBean(pageList.getTotal(), pageList.getResult());

    }
}
