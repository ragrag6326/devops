package com.tkb.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.tkb.api.gitlab.GitlabApiClient;
import com.tkb.api.gitlab.dto.GitlabDto;
import com.tkb.config.GitlabConfig;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.mapper.GitlabMrMapper;
import com.tkb.service.GitlabMrService;
import com.tkb.utils.Constant.GitlabReleaseState;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

        // 找 application 中對應專案的 project ID
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
    public List<GitlabMrEntity> getMergedMrsBetween(String projectName, String targetBranch, String env ,LocalDateTime start, LocalDateTime end) {

        LambdaQueryChainWrapper<GitlabMrEntity> qw = this.lambdaQuery()
                .eq(GitlabMrEntity::getProjectName, projectName)
                .eq(GitlabMrEntity::getState, "merged")          // 只找已合併的
                //.eq(GitlabMrEntity::getTargetBranch, targetBranch)   // 動態傳入 develop 或 main
                //.between(GitlabMrEntity::getMergedAt, start, end)
                .orderByAsc(GitlabMrEntity::getMergedAt); // 依照合併時間排序

        if ("dev".equalsIgnoreCase(env)) {
            // DEV：只挑還沒發過 DEV 的 MR
            qw.eq(GitlabMrEntity::getReleasedDev, GitlabReleaseState.FALSE.getCode());
        } else if ("prod".equalsIgnoreCase(env)) {
            // PROD：只挑「已發 DEV、尚未發 PROD」的 MR
            qw.eq(GitlabMrEntity::getReleasedDev, GitlabReleaseState.TRUE.getCode())
              .eq(GitlabMrEntity::getReleasedProd, GitlabReleaseState.FALSE.getCode());
        }

        return qw.list();

    }

    @Override
    public void stampVersionForMrs(
            String projectName,
            String env,
            List<Long> mrEntityIds,
            String version
    ) {
        if (mrEntityIds == null || mrEntityIds.isEmpty()) {
            return;
        }

        LambdaUpdateWrapper<GitlabMrEntity> uw = Wrappers.lambdaUpdate();

        uw.eq(GitlabMrEntity::getProjectName, projectName)
                .in(GitlabMrEntity::getId, mrEntityIds);

        if ("dev".equalsIgnoreCase(env)) {
            uw.set(GitlabMrEntity::getVersionDev, version)
                    .set(GitlabMrEntity::getReleasedDev, GitlabReleaseState.TRUE.getCode());
        } else if ("prod".equalsIgnoreCase(env)) {
            uw.set(GitlabMrEntity::getVersionProd, version)
                    .set(GitlabMrEntity::getReleasedProd, GitlabReleaseState.TRUE.getCode());
        }

        this.update(uw);
    }

    @Override
    public void unstampVersionForMrs(String projectName, String env, String version) {

        LambdaUpdateChainWrapper<GitlabMrEntity> uw = this.lambdaUpdate()
                .eq(GitlabMrEntity::getProjectName, projectName);

        if ("dev".equalsIgnoreCase(env)) {
            uw.eq(GitlabMrEntity::getVersionDev, version)
                    .set(GitlabMrEntity::getVersionDev, null)
                    .set(GitlabMrEntity::getReleasedDev, GitlabReleaseState.FALSE.getCode());

        } else if ("prod".equalsIgnoreCase(env)) {
            uw.eq(GitlabMrEntity::getVersionProd, version)
                    .set(GitlabMrEntity::getVersionProd, null)
                    .set(GitlabMrEntity::getReleasedProd, GitlabReleaseState.FALSE.getCode());
        }

        this.update(uw);
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

    /**
     * UPDATE () SET released_dev = TRUE, version_dev = #{version} WHERE project_name = #{projectName} AND released_dev = False AND state = merged"
     */
    @Override
    public Boolean markReleaseDev(String projectName, String version) {
        // 0 = True

        return this.lambdaUpdate()
                .set(GitlabMrEntity::getVersionDev, version)
                .set(GitlabMrEntity::getReleasedDev, GitlabReleaseState.TRUE.getCode()) //  1 = True
                .eq(GitlabMrEntity::getProjectName, projectName)
                .eq(GitlabMrEntity::getReleasedDev, GitlabReleaseState.FALSE.getCode())
                .eq(GitlabMrEntity::getState, "merged")
                .update();
    }
    /**
     * UPDATE () SET released_prod = TRUE, version_prod = #{version} WHERE project_name = #{projectName} AND released_dev = TRUE"
     */
    @Override
    public Boolean markReleaseProd(String projectName, String version) {
        return this.lambdaUpdate()
                .set(GitlabMrEntity::getVersionProd, version)
                .set(GitlabMrEntity::getReleasedProd, GitlabReleaseState.TRUE.getCode())  // 0 = True
                .eq(GitlabMrEntity::getProjectName, projectName)
                .eq(GitlabMrEntity::getReleasedDev, GitlabReleaseState.TRUE.getCode())
                .eq(GitlabMrEntity::getReleasedProd, GitlabReleaseState.FALSE.getCode())
                .update();
    }

    @Override
    public List<GitlabMrEntity> getMrsPending(String projectName) {
        // Token
        String token = gitlabConfig.getToken();

        // 找 application 中對應專案的 project ID
        Long projectId = gitlabConfig.getProjects()
                .stream()
                .filter(project -> project.getName().equals(projectName))
                .findFirst()
                .map(GitlabConfig.ProjectItem::getId)
                .orElseThrow(() -> new RuntimeException("找不到對應專案: " + projectName));

        // 2. 呼叫 API 取得列表
        List<GitlabDto> dtoList  = gitlabApiClient.getMRInfo(projectId, token);

        //
        //
        //
        //
        // System.out.println(dtoList);

        // 3. 轉換與過濾
        return dtoList.stream()
                .filter(dto -> "opened".equals(dto.getState())) // GitLab API 的開啟狀態通常是 "opened"
                .map(dto -> convertToEntity(dto, projectName))
                .collect(Collectors.toList());

    }


    private GitlabMrEntity convertToEntity(GitlabDto dto, String projectName) {
        GitlabMrEntity entity = new GitlabMrEntity();
        entity.setProjectName(projectName);
        entity.setMrId(dto.getId());
        entity.setIid(dto.getIid());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setState(dto.getState());
        entity.setTargetBranch(dto.getTarget_branch());
        entity.setSourceBranch(dto.getSource_branch());

        // Null Safety 處理
        if (dto.getAuthor() != null) entity.setAuthorName(dto.getAuthor().getName());
        if (dto.getMerged_by() != null) entity.setMergedBy(dto.getMerged_by().getName());

        // 時間轉換處理 (確保 dto 內的值不為 null)
        if (dto.getMerged_at() != null) entity.setMergedAt(dto.getMerged_at().toLocalDateTime());
        if (dto.getCreated_at() != null) entity.setCreatedAt(dto.getCreated_at().toLocalDateTime());
        if (dto.getUpdated_at() != null) entity.setUpdatedAt(dto.getUpdated_at().toLocalDateTime());

        return entity;
    }
}
