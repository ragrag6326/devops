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

    /**
     * vcs 後台 、Jenkins 自動觸發 都會呼叫此方法
     * vcs 後台 ： 如果前端指定了版本號會先呼叫此方法，將狀態設為 DEPLOYING (0)
     * Jenkins 自動觸發 ：若版號已存在且狀態為 DEPLOYING (0)，則直接回傳，更新該本版
     * @param dto
     */
    @Override
    public Long markDeploying(DeployDTO dto) {

        String version = dto.getVersion();
        String projectName = dto.getProjectName();
        String projectEnv = dto.getProjectEnv();

        // 檢查是否已有一筆該版號且狀態為 0 的資料 (前端寫入的那筆)
        VersionEntity one = VersionService.lambdaQuery()
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, projectEnv)
                .eq(VersionEntity::getVersion, version)
                .eq(VersionEntity::getState, DeployState.DEPLOYING.getCode())
                .one();

        if (one != null) {
            // 如果資料已存在 ( 是由後台系統先寫了 )
            // 且狀態為 0 (DEPLOYING)，則直接回傳
            if (one.getState() == DeployState.DEPLOYING.getCode()) {
                log.info("部署版號已存在且狀態為 DEPLOYING：{}-{}-{}", projectName, projectEnv, version);
                return one.getId();
            }
        }

        /**
         * 部署版號不存在則標記的兩種途徑
         * 1. 前端手動指定
         * 2. Jenkins 自動觸發，沒經過前端手動指定
         */
        VersionEntity versionEntity = new VersionEntity();
        versionEntity.setProjectName(dto.getProjectName());
        versionEntity.setVersion(dto.getVersion());
        versionEntity.setProjectEnv(dto.getProjectEnv());
        versionEntity.setState(DeployState.DEPLOYING.getCode()); // 0 = DEPLOYING
        versionEntity.setCreatedBy(dto.getUser());
        versionEntity.setCreatedTime(LocalDateTime.now());
        VersionService.save(versionEntity);

        log.info("➡ 部署開始紀錄：{}-{} v{}", projectName, projectEnv, version);

        // MyBatis Plus save 後會自動將 ID 回填到 entity 中
        return versionEntity.getId();
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
