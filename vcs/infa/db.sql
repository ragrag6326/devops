

CREATE DATABASE IF NOT EXISTS `vcs` 
USE `vcs`;


-- vcs.project_versions definition

CREATE TABLE `project_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(50) NOT NULL,
  `project_env` varchar(50) NOT NULL COMMENT '部屬環境',
  `version` varchar(50) NOT NULL COMMENT '記錄版號:如 1.0.3',
  `state` tinyint DEFAULT NULL COMMENT '0=success , 1=rollback , 2=??',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb3;

INSERT INTO vcs.project_versions (project_name,project_env,version,state,created_time,updated_time) VALUES
	 ('tkbgoapi','dev','1.0.17',0,'2025-11-16 15:13:50','2025-11-28 07:52:42'),
	 ('tkbgoapi','dev','1.0.18',0,'2025-11-28 16:15:45','2025-11-28 16:15:45'),
	 ('tkbgo-api','dev','1.0.18',1,'2025-11-28 16:17:57','2025-11-28 08:23:30'),
	 ('tkbgo-api','dev','1.0.18',0,'2025-11-28 16:24:22','2025-11-28 16:24:22');


-- vcs.gitlab_merge_requests definition

CREATE TABLE `gitlab_merge_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(50) DEFAULT NULL COMMENT '專案名稱',
  `version` varchar(50) DEFAULT NULL COMMENT '發布時所屬的版本號',
  `mr_id` bigint NOT NULL COMMENT 'GitLab MR 的 id',
  `iid` int NOT NULL COMMENT '專案內 MR 序號',
  `title` varchar(255) DEFAULT NULL COMMENT 'MR標題',
  `description` text COMMENT 'MR說明',
  `state` varchar(20) DEFAULT NULL COMMENT '狀態 merged(完成MR) | opened(尚未MR) | closed(取消MR) ',
  `target_branch` varchar(100) DEFAULT NULL,
  `source_branch` varchar(100) DEFAULT NULL,
  `author_name` varchar(100) DEFAULT NULL,
  `merged_by` varchar(100) DEFAULT NULL,
  `merged_at` datetime DEFAULT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_mr_id` (`mr_id`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb3;


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