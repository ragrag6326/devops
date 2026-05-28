-- MR AI Code Review 結果表（在 vcs 資料庫執行）
USE `vcs`;

CREATE TABLE IF NOT EXISTS `mr_code_reviews` (
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
  UNIQUE KEY `uk_project_mr` (`project_name`, `mr_id`),
  KEY `idx_status` (`review_status`),
  KEY `idx_project_state` (`project_name`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MR AI Code Review 紀錄';
