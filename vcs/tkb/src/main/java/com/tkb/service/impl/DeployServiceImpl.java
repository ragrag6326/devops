package com.tkb.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tkb.dto.DeployDTO;
import com.tkb.entity.VersionEntity;
import com.tkb.mapper.DeployMapper;
import com.tkb.service.DeployService;
import com.tkb.service.GitlabMrService;
import com.tkb.service.VersionService;
import com.tkb.utils.Constant.DeployState;
import com.tkb.utils.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeployServiceImpl implements DeployService  {

    private final VersionService VersionService;
    private final GitlabMrService GitlabMrService;

    @Override
    public void markDeploying(DeployDTO dto) {

        String version = dto.getVersion();
        String projectName = dto.getProjectName();
        String projectEnv = dto.getProjectEnv();

        VersionEntity one = VersionService.lambdaQuery()
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, projectEnv)
                .eq(VersionEntity::getVersion, version)
                .one();

        if (one != null) {
            log.info("部署版號已存在：{}-{}-{}", projectName, projectEnv, version);
            return;
        }

        VersionEntity versionEntity = new VersionEntity();
        versionEntity.setProjectName(dto.getProjectName());
        versionEntity.setVersion(dto.getVersion());
        versionEntity.setProjectEnv(dto.getProjectEnv());
        versionEntity.setState(DeployState.DEPLOYING.getCode()); // 0 = DEPLOYING
        versionEntity.setCreatedBy(dto.getUser());
        versionEntity.setCreatedTime(LocalDateTime.now());
        VersionService.save(versionEntity);

        log.info("➡ 部署開始紀錄：{}-{} v{}", projectName, projectEnv, version);
    }

    @Override
    public void markSuccess(DeployDTO dto) {

        String projectName = dto.getProjectName();
        String env = dto.getProjectEnv();
        String version = dto.getVersion();

        // 1. 先產生 Release Note + MR stamping（VersionService）
        VersionEntity req = new VersionEntity();
        req.setProjectName(projectName);
        req.setProjectEnv(env);
        req.setVersion(version);

        Result<String> result = VersionService.upgradeAndGenerateNote(req);

        if ( result.getCode() != 1) {  // 1 成功 0失敗
            log.error("upgradeAndGenerateNote 失敗: {}", result.getMsg());
            throw new RuntimeException("產生 Release Note 失敗：" + result.getMsg());
        }

        // 2. 更新版本 state 成 SUCCESS
        boolean ok = VersionService.lambdaUpdate()
                .set(VersionEntity::getState, DeployState.SUCCESS.getCode())
                .set(VersionEntity::getFinishedTime, LocalDateTime.now())
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, env)
                .eq(VersionEntity::getVersion, version)
                .update();

        if (!ok) {
            throw new RuntimeException("更新版本狀態失敗");
        }

        if ("dev".equals(dto.getProjectEnv())) {
            if( GitlabMrService.markReleaseDev(projectName, version )) {
                log.info("標記 markReleaseDev , {} , {} , {}", projectName, env, version);
            };
        } else if ("prod".equals(dto.getProjectEnv())) {
            if (GitlabMrService.markReleaseProd(projectName, version)) {
                log.info("標記 markReleaseProd , {} , {} , {}", projectName, env , version);
            };
        }
    }

    @Override
    public void markFail(DeployDTO dto) {
        updateState(dto , DeployState.FAILED.getCode()); // 2 = Faild
    }

    @Override
    public void markRollback(DeployDTO dto) {
        updateState(dto , DeployState.ROLLED_BACK.getCode()); // 3 = Rolled back
    }

    /**
     * 1. select * from () where project_name = #{projectName} AND project_Env = #{projectEnv} AND version = #{version}
     * 2. UPDATE () SET setState = #{setState}  where id = #{id}
     * @param dto
     * @param state
     */
    private void updateState(DeployDTO dto, int state) {
        VersionEntity one = VersionService.lambdaQuery()
                .eq(VersionEntity::getProjectName, dto.getProjectName())
                .eq(VersionEntity::getProjectEnv, dto.getProjectEnv())
                .eq(VersionEntity::getVersion, dto.getVersion())
                .one();
        if (one == null) return;


        one.setState(state);
        one.setFinishedTime(LocalDateTime.now());
        VersionService.updateById(one);

    }
}
