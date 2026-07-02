package com.tkb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.entity.VersionEntity;
import com.tkb.mapper.VersionMapper;
import com.tkb.utils.Constant.DeployState;
import com.tkb.utils.Version.VersionUtil;
import com.tkb.utils.result.Result;
import com.tkb.entity.ProjectEntity;
import com.tkb.service.GitlabMrService;
import com.tkb.service.ProjectService;
import com.tkb.service.VersionService;
import com.tkb.vo.PageBean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VersionServiceImpl extends ServiceImpl<VersionMapper, VersionEntity> implements VersionService {

    private final GitlabMrService gitlabMrService;
    private final ProjectService projectService;

    @Override
    public Result<String> saveNewVersion(VersionEntity version) {

        // 1. 版本號定義
        if (version.getProjectName() == null || version.getProjectEnv() == null) {
            return Result.error("缺少 projectName 或 projectEnv 參數");
        }

        // 2. 檢查版本格式 x.x.x
        if (!version.getVersion().matches("^\\d+\\.\\d+\\.\\d+$")) {
            return Result.error("版本格式錯誤，請使用 [x].[x].[x] 格式，如 1.0.0");
        }
        // 3. 檢查版本是否已存在（避免重複新增）
        boolean exists = this.lambdaQuery()
                .eq(VersionEntity::getProjectName, version.getProjectName())
                .eq(VersionEntity::getProjectEnv, version.getProjectEnv())
                .eq(VersionEntity::getVersion, version.getVersion())
                .exists();

        if (exists) {
            return Result.error("版本已存在，無需重複新增: " + version.getVersion());
        }

        version.setCreatedTime(LocalDateTime.now());
        version.setUpdatedTime(LocalDateTime.now());
        return this.save(version) ? Result.success("版本新增成功") : Result.error("版本新增失敗");

    }

    @Override
    public Result<String> getVersion(String ProjectName , String ProjectEnv) {
        VersionEntity result = this.lambdaQuery()
                .eq(VersionEntity::getProjectName, ProjectName)
                .eq(VersionEntity::getProjectEnv, ProjectEnv)
                .eq(VersionEntity::getState, DeployState.SUCCESS.getCode()) // 1
                .orderByDesc(VersionEntity::getVersion)
                .last("LIMIT 1")
                .one();

        if (result == null) {
            return Result.error("參數有誤請重新查詢 ProjectName = tkbgoapi | tkbtv , ProjectEnv = prod | env" );
        } else {
            return Result.success(result.getVersion());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> upgradeAndGenerateNote(VersionEntity versionRequest) {

        String projectName = versionRequest.getProjectName();
        String projectEnv = versionRequest.getProjectEnv(); // "dev" 或 "prod"
        String newVersion = versionRequest.getVersion();
        LocalDateTime now = LocalDateTime.now();

        // 1. 找上一版 (成功的 dev/prod)
        // select * from project_versions where project_env = '?' and project_name = '?' and state = 0 order by created_time desc limit 1
        VersionEntity lastVersion = this.lambdaQuery()
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, projectEnv) // prod / dev 但通常都是 dev
                .eq(VersionEntity::getState, DeployState.SUCCESS.getCode()) // 1 = 成功
                .orderByDesc(VersionEntity::getCreatedTime)
                .last("LIMIT 1")
                .one();


        //log.info("找出同環境的「上一個版本」 : {} ", lastVersion);

        // 2. 版本號比較
        if (lastVersion != null) {
            String currentVersionStr = lastVersion.getVersion();
            // 檢查新版本是否大於舊版本  新1.0.6 舊1.0.5
            int comparisonResult = compareVersions(newVersion, currentVersionStr);

            /**
             * 非法的版本號 : 兩種情況不允許
             * comparisonResult = 0 新舊版本號相同 (重複)
             * comparisonResult < 0 新版本號小於舊版本號 (倒退)
             * 合法的版本號 :
             * comparisonResult > 0 (新版>舊版)
             */
            if (comparisonResult <= 0) {
                return Result.error("新版本號 " + newVersion + " 必須大於現有最新版本號 " + currentVersionStr);
            }
        }

        // 3. 版本查詢的 startTime 如果查不到就將時間點往前推一個月  如 11/11 -> 10/11 查詢gitlab的MR時間
        LocalDateTime startTime = (lastVersion != null) ? lastVersion.getCreatedTime() : now.minusMonths(1);
        log.info("startTime : {} ", startTime);

        // 4. 同步 GitLab 資料 (確保資料庫最新)
        gitlabMrService.syncFromGitlab(projectName);

        // 5. 【注意】設定要查詢的 Target Branch 不管是測試還是正式，功能說明都在 'develop'
        String targetBranchToQuery = "develop";
        // 只有 Hotfix 的情況才會進 main，才需要額外處理 main分支 ，但一般正常流程，查詢 develop 區間即可涵蓋 Release 分支的內容。

        // 6. 查詢 MR
        // select * from vcs.gitlab_merge_requests where project_name = 'tkbtv' and state = 'merged' and released_prod = false and released_dev = true order by merged_at
        // 已不判斷分支來源 指判斷，rel
        List<GitlabMrEntity> mergedMrs = gitlabMrService.getMergedMrsBetween(
                projectName,
                targetBranchToQuery, // 查看 develop
                projectEnv,          // dev / prod
                startTime,
                now
        );

        log.info("查詢 MR : {} ", mergedMrs);


        // 7. 生成 Note (加入環境標示)
        StringBuilder note = new StringBuilder();
        note.append("## 🚀 ").append(projectEnv.toUpperCase()).append(" Release Note: ").append(newVersion).append("\n");
        note.append("**區間:** `").append(lastVersion != null ? lastVersion.getVersion() : "Initial").append("` -> `").append(newVersion).append("`\n");
        note.append("**收錄時間:** ").append(startTime).append(" ~ ").append(now).append("\n");
        note.append("**來源分支:** ").append(targetBranchToQuery).append("\n\n");

        if (mergedMrs.isEmpty()) {
            note.append("> ⚠ 期間內無功能合併紀錄\n");
        } else {
            for (GitlabMrEntity mr : mergedMrs) {
                note.append(String.format("- %s (!%s) - @%s\n", mr.getTitle(), mr.getIid(), mr.getAuthorName()));
            }
        }

        // 8. 更新已存在的 version row（ deploy/deploying 新建的）
        boolean updated = this.lambdaUpdate()
                .set(VersionEntity::getReleaseNote, note.toString())
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, projectEnv)
                .eq(VersionEntity::getVersion, newVersion)
                .update();

        if (!updated) {
            return Result.error("找不到對應版本紀錄，請先呼叫 /deploy/deploying");
        }

        // 9. MR 版標標記 (Stamping)
        if (!mergedMrs.isEmpty()) {
            // 取得所有 MR 實體在 DB 中的 ID
            List<Long> mrIdsToStamp = mergedMrs.stream()
                    .map(GitlabMrEntity::getId)
                    .collect(Collectors.toList());

            // 標記服務
            // update <gitlab_merge_requests> set version = '??' where id in (1 ,2 ,3)
            gitlabMrService.stampVersionForMrs( projectName, projectEnv , mrIdsToStamp, newVersion);
        }


        return Result.success(note.toString());
    }

    /**
     * 處理退版
     * @param projectName
     * @param projectEnv
     * @param version
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> rollbackVersion(String projectName, String projectEnv, String version) {

        // 1. 檢查版本號格式 (可選，避免 SQL Injection)
        if (version == null || version.isEmpty()) {
            return Result.error("版本號不可為空");
        }

        //2. Version 表的 state 改成 3 = rollback
        boolean VersionUpdate = this.lambdaUpdate()
                .set(VersionEntity::getState, DeployState.ROLLED_BACK.getCode())
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, projectEnv)
                .eq(VersionEntity::getVersion, version)
                .update();

        if (!VersionUpdate) {
            log.warn("版本狀態修改失敗 {} - {}", projectName, version);
            return Result.error("版本 : "+ version + "RollBack 標記失敗");
        }

        // 3. 將特定版本號的 MR表 的 version 欄位設為 NULL
        gitlabMrService.unstampVersionForMrs(projectName, projectEnv , version);

        log.info("版本 RollBack 成功：版本紀錄 {}  MR 已解除標記。", version);
        return Result.error("版本 RollBack 成功：版本紀錄" + version + "MR 已解除標記。");
    }

    @Override
    public PageBean page(Integer page, Integer pageSize, String name, String env, String state) {
        PageHelper.startPage(page, pageSize);

        List<VersionEntity> list = this.lambdaQuery()
                .like(name != null && !name.isEmpty(), VersionEntity::getProjectName, name)
                .eq(env != null && !env.isEmpty(), VersionEntity::getProjectEnv, env)
                .eq(state != null && !state.isEmpty(), VersionEntity::getState, state)
                .orderByDesc(VersionEntity::getCreatedTime)
                .list();

        Page<VersionEntity> pageList = (Page<VersionEntity>) list;

        return new PageBean(pageList.getTotal() , pageList.getResult());

    }

    /**
     * 查詢下一個版本
     * @param projectName
     * @param env
     * @return
     */
    @Override
    public Result<String> getNextVersion(String projectName, String env) {

        // 1. 【優先檢查】是否有正在部屬中 (State = 0) 的紀錄
        //  State = 0 表前端可能剛寫入了一筆指定版號
        VersionEntity deployingVersion = this.lambdaQuery()
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, env)
                .eq(VersionEntity::getState, DeployState.DEPLOYING.getCode()) // 0 = Deploying
                .orderByDesc(VersionEntity::getCreatedTime)
                .last("LIMIT 1")
                .one();

        if (deployingVersion != null) {
            // 不做任何運算，直接回傳最新且 State 標記Deploying 的版號
            log.info("發現正在部屬中的版號，直接返回: {}", deployingVersion.getVersion());
            return Result.success(deployingVersion.getVersion());
        }

        // 2. 【原本邏輯】如果沒有 (State = 0 ) 等待建置的版本，就找最後一個成功的 (State = 1 ) 或失敗 (State = 2 ) 回滾 (State = 3 )
        VersionEntity lastSuccessVersion = this.lambdaQuery()
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, env)
                .in(VersionEntity::getState,
                        DeployState.SUCCESS.getCode() ,   // 1 = Success
                        DeployState.FAILED.getCode(),      // 2 = Failed
                        DeployState.ROLLED_BACK.getCode()  // 3 = roll back
                )
                .orderByDesc(VersionEntity::getCreatedTime)
                .last("LIMIT 1")
                .one();

        // 上個版本 build (成功或失敗) 都直接進入下一版
        if (lastSuccessVersion != null) {
            String newVersion = VersionUtil.plusOne(lastSuccessVersion.getVersion());
            return Result.success(newVersion);
        }

        // 3. 【新創建版本】state 都查詢不到表示該版號未收錄，做新增動作並且從 1.0.0 開始
        VersionEntity initVersion = new VersionEntity();
        initVersion.setProjectName(projectName);
        initVersion.setProjectEnv(env);
        initVersion.setVersion("1.0.0");

        boolean save = this.save(initVersion);
        if (save) {
            return Result.success(initVersion.getVersion());
        }
        return Result.error("版本號無法獲取");
    }

    /**
     * 取得 prod/dev環境 最後一次成功的 版號
     * @param projectName
     * @param env
     * @return
     */
    @Override
    @Deprecated
    public String getLastSuccessVersion(String projectName, String env) {

        VersionEntity lastSuccessVersion = this.lambdaQuery()
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, env)
                .eq(VersionEntity::getState, DeployState.SUCCESS.getCode())
                .orderByDesc(VersionEntity::getCreatedTime)
                .last("LIMIT 1")
                .one();

        if (lastSuccessVersion != null) {
            return  VersionUtil.plusOne(lastSuccessVersion.getVersion());
        }

        return null ;
    }

    @Override
    public String getReleaseNote(String projectName, String env) {
        VersionEntity one = this.lambdaQuery()
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, env)
                .eq(VersionEntity::getState, DeployState.SUCCESS.getCode())
                .orderByDesc(VersionEntity::getFinishedTime)
                .last("LIMIT 1")
                .one();
        if (one != null) {
            return one.getReleaseNote();
        } else
            return "RELEASE NOTE NOT FOUND";
    }

    /**
     * 備註修改
     * @param versionEntity
     */
    @Override
    public Boolean editRemark(VersionEntity versionEntity) {
        return this.lambdaUpdate()
                .set(VersionEntity::getRemark, versionEntity.getRemark())
                    .eq(VersionEntity::getProjectName, versionEntity.getProjectName())
                    .eq(VersionEntity::getProjectEnv, versionEntity.getProjectEnv())
                    .eq(VersionEntity::getId, versionEntity.getId())
                .update();
    }

    /**
     * 檢測 prod 環境是否能夠部屬
     * @param projectName
     * @param env
     * @param targetVersion
     * @return
     */
    @Override
    public Result<String> checkdeployable(String projectName, String env , String targetVersion) {

        // 解析該專案的實際 dev/prod env 名稱（例如 form-service 的 prod 實際是 "admin"）
        ProjectEntity project = projectService.findByName(projectName);
        String actualProdEnv = (project != null && project.getProdEnv() != null && !project.getProdEnv().isBlank())
                ? project.getProdEnv() : "prod";
        String actualDevEnv  = (project != null && project.getDevEnv()  != null && !project.getDevEnv().isBlank())
                ? project.getDevEnv()  : "dev";

        // 判斷傳入 env 屬於 prod-like 還是 dev-like
        boolean isProd = env.equals(actualProdEnv) || "prod".equals(env);
        boolean isDev  = env.equals(actualDevEnv)  || "dev".equals(env);

        // 用實際 env 名稱查各環境最新成功版號
        String lastDevVer  = this.getNextVersion(projectName, actualDevEnv).getData();
        String lastProdVer = this.getNextVersion(projectName, actualProdEnv).getData();

        log.info("checkdeployable project={} env={} actualProdEnv={} actualDevEnv={} lastDevVer={} lastProdVer={}",
                projectName, env, actualProdEnv, actualDevEnv, lastDevVer, lastProdVer);

        // =====================
        // 情境 A: 目標環境是 Dev
        // =====================
        if (isDev) {
            if (targetVersion != null && !targetVersion.isEmpty()) {
                int i = compareVersions(targetVersion, lastDevVer);
                if (i >= 0) {
                    return Result.success("Dev 環境允許更新");
                }
                return Result.error("非法操作: 目前 Dev 版號: " + lastDevVer + " 目標版本: " + targetVersion + " 不允許小於當前版本");
            }
        }

        // =======================
        // 情境 B: 目標環境是 Prod
        // ======================
        if (isProd) {

            // 1. 如果 Dev 尚未有版號，Prod 無法部屬
            if (lastDevVer == null) {
                return Result.error("禁止部署：Dev 環境尚未有任何成功版本，無法部署 Prod");
            }

            // 2. (如果有傳入 targetVersion) 檢查：Prod 必須是往前更新 (Target >= Prod)
            if (targetVersion != null && !targetVersion.isEmpty()) {
                if (lastProdVer != null && compareVersions(targetVersion, lastProdVer) >= 0) {
                    return Result.success("檢查通過：有新的 Dev 版本可供 Prod 更新");
                }
                // lastProdVer 為 null 代表 Prod 尚無版本，直接允許
                if (lastProdVer == null) {
                    return Result.success("檢查通過：Prod 尚無版本，允許首次部署");
                }
            }
            return Result.error("版本錯誤：目標版本: " + targetVersion + " 必須大於當前 Prod 版本 " + lastProdVer);
        }
        return Result.error("未知環境設定");
    }


    @Override
    public Boolean updateJenkinsBuildById(VersionEntity versionEntity) {

        return this.lambdaUpdate()
                .set(VersionEntity::getJenkinsBuildId, versionEntity.getJenkinsBuildId())
                .eq(VersionEntity::getId, versionEntity.getId())
                .update();
    }


    /**
     * 版本號比較工具
     * @param version1 版本1 (e.g. 1.0.1)
     * @param version2 版本2 (e.g. 1.1.2)
     * @return 正數: v1 > v2, 負數: v1 < v2, 0: 相等
     */
    private static int compareVersions(String version1, String version2) {
        // 假設版本格式已在前面檢查過 (x.x.x)
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        // 只比較 Major, Minor, Patch 三個部分
        int length = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int v1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int v2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (v1 != v2) {
                return v1 - v2;
            }
        }
        // 如果前面部分都相同 (例如 1.0.0 vs 1.0.0.1)
        // 假設都是 x.x.x 格式，此處回傳 0
        return 0;
    }
}
