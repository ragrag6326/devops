package com.tkb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("project_config")
@Schema(description = "專案設定實體類")
public class ProjectEntity {

    @Schema(description = "主鍵 ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Schema(description = "系統名稱 (對應 project_versions.project_name)", example = "tkbtv")
    private String name;

    @Schema(description = "前端顯示名稱", example = "TKB TV")
    private String displayName;

    @Schema(description = "專案描述", example = "TKB 電視台後端服務")
    private String description;

    @Schema(description = "GitLab Project ID", example = "3")
    private Long gitlabProjectId;

    @Schema(description = "分類: frontend / backend", example = "backend")
    private String category;

    @Schema(description = "是否啟用 (1=啟用, 0=停用)", example = "1")
    private Integer isActive;

    @Schema(description = "排序 (數字越小越前)", example = "1")
    private Integer sortOrder;

    @Schema(description = "Shell 腳本目錄名稱 (tools/ 下的目錄名，null 時使用 name)", example = "tv")
    private String scriptName;

    @Schema(description = "是否部署於正式機 (1=是, 0=否)", example = "1")
    private Integer hasProd;

    @Schema(description = "是否部署於測試機 (1=是, 0=否)", example = "1")
    private Integer hasDev;

    @Schema(description = "Docker image 過濾關鍵字 (對應 config.sh IMAGE_KEYWORD，null 時使用 name)", example = "goapi")
    @com.baomidou.mybatisplus.annotation.TableField("image_keyword")
    private String imageKeyword;

    @Schema(description = "正式部署 Jenkins env 名稱 (null 時預設 prod)", example = "admin")
    @com.baomidou.mybatisplus.annotation.TableField("prod_env")
    private String prodEnv;

    @Schema(description = "測試部署 Jenkins env 名稱 (null 時預設 dev)", example = "dev")
    @com.baomidou.mybatisplus.annotation.TableField("dev_env")
    private String devEnv;

    @Schema(description = "PROD Job 名稱（正式 blue，null 時用 {type}-prod）", example = "frontend-prod")
    @com.baomidou.mybatisplus.annotation.TableField("jenkins_job_name_prod")
    private String jenkinsJobNameProd;

    @Schema(description = "Backup Job 名稱（備援 green，前端才有，null 則不觸發）", example = "frontend-prod-backup")
    @com.baomidou.mybatisplus.annotation.TableField("jenkins_job_name_backup")
    private String jenkinsJobNameBackup;

    @Schema(description = "DEV Job 名稱（測試，null 時用 {type}-dev）", example = "frontend-dev")
    @com.baomidou.mybatisplus.annotation.TableField("jenkins_job_name_dev")
    private String jenkinsJobNameDev;

    @Schema(description = "PROD Jenkins Token（正式 blue）")
    @com.baomidou.mybatisplus.annotation.TableField("jenkins_token_prod")
    private String jenkinsTokenProd;

    @Schema(description = "Backup Jenkins Token（備援 green，前端才有）")
    @com.baomidou.mybatisplus.annotation.TableField("jenkins_token_backup")
    private String jenkinsTokenBackup;

    @Schema(description = "DEV Jenkins Token（測試）")
    @com.baomidou.mybatisplus.annotation.TableField("jenkins_token_dev")
    private String jenkinsTokenDev;

    @Schema(description = "Jenkins Pipeline Job 名稱 (null 時用 {type}-pipeline)", example = "form-service-pipeline")
    @com.baomidou.mybatisplus.annotation.TableField("jenkins_pipeline_name")
    private String jenkinsPipelineName;

    @Schema(description = "預設部署分支 (null 時用 master)", example = "main")
    @com.baomidou.mybatisplus.annotation.TableField("default_branch")
    private String defaultBranch;

    @Schema(description = "PROD 操作實際 SSH 的機器 (null=prod, 填 dev=走 dev 機)", example = "dev")
    @com.baomidou.mybatisplus.annotation.TableField("prod_ssh_env")
    private String prodSshEnv;

    @Schema(description = "DEV 操作實際 SSH 的機器 (null=dev, 填 prod=走 prod 機)", example = "prod")
    @com.baomidou.mybatisplus.annotation.TableField("dev_ssh_env")
    private String devSshEnv;

    @Schema(description = "建立時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdTime;

    @Schema(description = "更新時間")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedTime;
}
