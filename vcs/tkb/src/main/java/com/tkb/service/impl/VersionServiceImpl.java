package com.tkb.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tkb.entity.GitlabMrEntity;
import com.tkb.entity.VersionEntity;
import com.tkb.mapper.VersionMapper;
import com.tkb.utils.result.Result;
import com.tkb.service.GitlabMrService;
import com.tkb.service.VersionService;
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

    @Override
    public Result<String> saveVersion(VersionEntity version) {

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

        // 1. 找出同環境的，且(上一個版本更新成功的版本)
        // select * from project_versions where project_env = '?' and project_name = '?' and state = 0 order by created_time desc limit 1
        VersionEntity lastVersion = this.lambdaQuery()
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, projectEnv) // prod / dev 但通常都是 dev
                .eq(VersionEntity::getState, 0) // 0 = 成功
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

        // 5. 【注意】設定要查詢的 Target Branc 不管是測試還是正式，功能說明都在 'develop'
        String targetBranchToQuery = "develop";
        // 只有 Hotfix 的情況才會進 main，才需要額外處理 main分支 ，但一般正常流程，查詢 develop 區間即可涵蓋 Release 分支的內容。

        // 6. 查詢 MR
        // select * from <gitlab_merge_requests> where projectName = ? and projectEnv = ? and merged_at > start and merged_at < end order by merged_at
        List<GitlabMrEntity> mergedMrs = gitlabMrService.getMergedMrsBetween(
                projectName,
                targetBranchToQuery, // 查看 develop
                startTime,
                now
        );

        log.info("查詢 MR : {} ", mergedMrs);

        // 7. 存檔新版本
        versionRequest.setCreatedTime(now);
        versionRequest.setUpdatedTime(now);
        versionRequest.setState(0); // 0 success
        this.save(versionRequest);

        // 8. MR 版標標記 (Stamping)
        if (!mergedMrs.isEmpty()) {
            // 取得所有 MR 實體在 DB 中的 ID
            List<Long> mrIdsToStamp = mergedMrs.stream()
                    .map(GitlabMrEntity::getId)
                    .collect(Collectors.toList());

            // 標記服務
            // update <gitlab_merge_requests> set version = '??' where id in (1 ,2 ,3)
            gitlabMrService.stampVersionForMrs(mrIdsToStamp, newVersion);
        }

        // 8. 生成 Note (加入環境標示)
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

        //2. Version 表的 state 改成 1=rollback
        boolean VersionUpdate = this.lambdaUpdate()
                .set(VersionEntity::getState, 1)
                .eq(VersionEntity::getProjectName, projectName)
                .eq(VersionEntity::getProjectEnv, projectEnv)
                .eq(VersionEntity::getVersion, version)
                .update();

        if (!VersionUpdate) {
            log.warn("版本狀態修改失敗 {} - {}", projectName, version);
            return Result.error("版本 : "+ version + "RollBack 標記失敗");
        }

        // 3. 將特定版本號的 MR表 的 version 欄位設為 NULL
        gitlabMrService.unstampVersionForMrs(projectName, version);

        log.info("版本 RollBack 成功：版本紀錄 {}  MR 已解除標記。", version);
        return Result.error("版本 RollBack 成功：版本紀錄" + version + "MR 已解除標記。");
    }


    /**
     * 比較兩個 x.x.x 格式的版本號。
     * @return > 0 (v1 > v2), < 0 (v1 < v2), = 0 (v1 = v2)
     */
    private static int compareVersions(String version1, String version2) {
        // 假設版本格式已在前面檢查過 (x.x.x)
        String[] parts1 = version1.split("\\.");
        String[] parts2 = version2.split("\\.");

        // 只比較 Major, Minor, Patch 三個部分
        int length = Math.min(parts1.length, parts2.length);

        for (int i = 0; i < length; i++) {
            int v1 = Integer.parseInt(parts1[i]);
            int v2 = Integer.parseInt(parts2[i]);

            if (v1 != v2) {
                return v1 - v2;
            }
        }
        // 如果前面部分都相同 (例如 1.0.0 vs 1.0.0.1)
        // 根據您的需求，我們假設都是 x.x.x 格式，此處回傳 0
        return 0;
    }
}
