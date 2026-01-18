package com.tkb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@TableName("gitlab_merge_requests")
@Schema(description = "GitLab Merge Request 紀錄")
public class GitlabMrEntity {

    @Schema(description = "主鍵 ID", example = "1")
    @TableId(value = "id" , type = IdType.AUTO)
    private Long id;

    @Schema(description = "專案名稱", example = "tkb-auth-service")
    private String projectName;

    @Schema(description = "GitLab 全域 MR ID", example = "56789")
    private Long mrId;

    @Schema(description = "專案內 MR ID (IID)", example = "12")
    private Integer iid;

    @Schema(description = "MR 標題", example = "Fix: Login bug")
    private String title;

    @Schema(description = "MR 描述", example = "修復了登入逾時的問題...")
    private String description;

    @Schema(description = "狀態 (opened, merged, closed)", example = "merged")
    private String state;

    @Schema(description = "關聯的 Dev 版本號", example = "1.0.1")
    private String versionDev;

    @Schema(description = "關聯的 Prod 版本號", example = "1.0.1")
    private String versionProd;

    @Schema(description = "是否已發布至 Dev", example = "true")
    private Boolean releasedDev;

    @Schema(description = "是否已發布至 Prod", example = "false")
    private Boolean releasedProd;

    @Schema(description = "目標分支", example = "master")
    private String targetBranch;

    @Schema(description = "來源分支", example = "feature/login-fix")
    private String sourceBranch;

    /**
     * 發起 MR 者 也就是 author
     */
    @Schema(description = "發起人 (Author)", example = "dev-user")
    private String authorName;

    /**
     * 最後由誰同意此次 MR 的
     */
    @Schema(description = "合併人 (Merger)", example = "lead-user")
    private String mergedBy;

    @Schema(description = "合併時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime mergedAt;

    @Schema(description = "建立時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @Schema(description = "更新時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
