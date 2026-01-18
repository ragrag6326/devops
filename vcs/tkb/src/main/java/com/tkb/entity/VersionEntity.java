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
@TableName("project_versions")
@Schema(description = "版本資訊實體類")
public class VersionEntity {

    @Schema(description = "主鍵 ID", example = "100")
    @TableId(value = "id" , type = IdType.AUTO)
    private Long id;

    @Schema(description = "專案名稱", example = "tkbtv")
    private String projectName;

    @Schema(description = "專案環境 (dev / prod)", example = "dev")
    private String projectEnv;

    @Schema(description = "版本號", example = "1.0.5")
    private String version;

    /** 0=DEPLOYING, 1=SUCCESS, 2=FAILED, 3=ROLLED_BACK */
    @Schema(description = "狀態 (0=DEPLOYING, 1=SUCCESS, 2=FAILED, 3=ROLLED_BACK)", example = "1")
    private int state;

    @Schema(description = "備註說明", example = "緊急修復登入 Bug")
    private String remark;

    @Schema(description = "GitLab Release Note 內容", example = "Fix: login issue...")
    private String releaseNote;

    @Schema(description = "建立人員", example = "admin")
    private String createdBy;

    @Schema(description = "Jenkins BuildID")
    private String jenkinsBuildId;

    @Schema(description = "建立時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;

    @Schema(description = "完成/結束時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    @Schema(description = "更新時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedTime;


}
