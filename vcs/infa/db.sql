

CREATE DATABASE IF NOT EXISTS `vcs` 
USE `vcs`;


-- vcs.gitlab_merge_requests definition
CREATE TABLE `gitlab_merge_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '專案名稱',
  `mr_id` bigint NOT NULL COMMENT 'GitLab MR 的 id',
  `iid` int NOT NULL COMMENT '專案內 MR 序號',
  `title` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MR標題',
  `description` mediumtext COLLATE utf8mb4_unicode_ci COMMENT 'MR說明',
  `state` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '狀態 merged(完成MR) | opened(尚未MR) | closed(取消MR) ',
  `version_dev` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MR 首次釋出至 DEV 時的版本號',
  `version_prod` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'MR 首次釋出至 PROD 時的版本號',
  `released_dev` tinyint(1) DEFAULT '0' COMMENT '是否已經釋出到 DEV            0 = false 尚未釋出到該環境 1 = true 已釋出到該環境',
  `released_prod` tinyint(1) DEFAULT '0' COMMENT '是否已經釋出到 PROD         0 = false 尚未釋出到該環境 1 = true 已釋出到該環境',
  `target_branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `source_branch` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `author_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `merged_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `merged_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mr_id` (`mr_id`)
) ENGINE=InnoDB AUTO_INCREMENT=337 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- vcs.log_analysis definition
CREATE TABLE `log_analysis` (
  `id` int NOT NULL AUTO_INCREMENT,
  `log_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `service_name` varchar(255) DEFAULT NULL,
  `logger_class` varchar(255) DEFAULT NULL,
  `error_reason` text,
  `ai_summary` varchar(255) DEFAULT NULL,
  `ai_root_cause` text,
  `ai_solution` text,
  `severity` int DEFAULT NULL,
  `ai_provider` varchar(50) DEFAULT 'Unknown',
  `ai_model` varchar(50) DEFAULT 'Unknown',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb3;



-- vcs.mr_code_reviews definition
CREATE TABLE `mr_code_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(50) NOT NULL COMMENT '專案名稱（對應 application.yml gitlab.projects.name）',
  `mr_id` bigint NOT NULL COMMENT 'GitLab 全域 MR id',
  `mr_iid` int NOT NULL COMMENT '專案內 MR 序號',
  `title` varchar(255) DEFAULT NULL,
  `author_name` varchar(100) DEFAULT NULL,
  `state` varchar(20) DEFAULT NULL COMMENT 'opened | merged | closed',
  `diff_hash` varchar(64) DEFAULT NULL COMMENT 'diff 內容 SHA-256，用於判斷是否需要重新審核',
  `review_status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | COMPLETED | FAILED',
  `summary` text COMMENT 'AI 審核摘要',
  `suggestions` mediumtext COMMENT 'AI 具體建議（Markdown）',
  `full_review` mediumtext COMMENT 'AI 完整回覆',
  `severity` tinyint DEFAULT NULL COMMENT '嚴重度 1-5',
  `error_message` varchar(500) DEFAULT NULL COMMENT '失敗原因',
  `reviewed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_mr` (`project_name`,`mr_id`),
  KEY `idx_status` (`review_status`),
  KEY `idx_project_state` (`project_name`,`state`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='MR AI Code Review 紀錄';


-- vcs.project_versions definition
CREATE TABLE `project_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `project_env` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '部屬環境',
  `version` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '記錄版號:如 1.0.3',
  `state` tinyint NOT NULL DEFAULT '0' COMMENT '狀態：0=DEPLOYING, 1=SUCCESS, 2=FAILED, 3=ROLLED_BACK',
  `node_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `remark` mediumtext COLLATE utf8mb4_unicode_ci COMMENT '對該版本的描述',
  `release_note` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci,
  `jenkins_build_id` bigint DEFAULT NULL,
  `created_by` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '觸發者 (GitLab / Jenkins / RD)',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `finished_time` datetime DEFAULT NULL COMMENT '成功部署或 rollback 發生時間',
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=516 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- vcs.system_aud_log definition

CREATE TABLE `system_aud_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主鍵 ID',
  `project_name` varchar(50) NOT NULL COMMENT '專案名稱',
  `action` varchar(100) NOT NULL COMMENT '操作動作',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人員',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '狀態：0=SUCCESS, 1=FAILED',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=66 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系統操作日誌紀錄';


-- vcs.`user` definition
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主鍵 ID',
  `username` varchar(50) NOT NULL COMMENT '用戶名/登入帳號',
  `password` varchar(100) NOT NULL COMMENT '加密後的密碼',
  `name` varchar(50) DEFAULT NULL COMMENT '用戶真實姓名',
  `role` varchar(20) NOT NULL DEFAULT 'USER' COMMENT '角色：例如 ADMIN, USER',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系統用戶表';


CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主鍵 ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用戶名/登入帳號',
    `password` VARCHAR(100) NOT NULL COMMENT '加密後的密碼',
    `name` VARCHAR(50) NULL COMMENT '用戶真實姓名',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：例如 ADMIN, USER',
    `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '創建時間',
    `updated_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新時間',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系統用戶表';

INSERT INTO `user` (`username`, `password`, `name`, `role`) VALUES
('admin', 'admin', '系統管理員', 'ADMIN');