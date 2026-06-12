USE `vcs`;

CREATE TABLE IF NOT EXISTS `mr_code_reviews` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(50) NOT NULL,
  `mr_id` bigint NOT NULL,
  `mr_iid` int NOT NULL,
  `title` varchar(255) DEFAULT NULL,
  `author_name` varchar(100) DEFAULT NULL,
  `state` varchar(20) DEFAULT NULL,
  `diff_hash` varchar(64) DEFAULT NULL,
  `review_status` varchar(20) NOT NULL DEFAULT 'PENDING',
  `summary` text,
  `suggestions` mediumtext,
  `full_review` mediumtext,
  `severity` tinyint DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `reviewed_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_project_mr` (`project_name`, `mr_id`),
  KEY `idx_status` (`review_status`),
  KEY `idx_project_state` (`project_name`, `state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MR AI Code Review 紀錄';
