

CREATE DATABASE IF NOT EXISTS `vcs` 
USE `vcs`;

CREATE TABLE project_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(50) NOT NULL,
    project_env  VARCHAR(50) NOT NULL COMMENT '部屬環境',
    version VARCHAR(50) NOT NULL COMMENT '記錄版號:如 1.0.3',
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);





CREATE TABLE gitlab_merge_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(100) NOT NULL,
    mr_iid BIGINT NOT NULL COMMENT 'GitLab MR IID',
    title VARCHAR(255) NOT NULL,
    description TEXT,
    author VARCHAR(100),
    source_branch VARCHAR(100),
    target_branch VARCHAR(100),
    merge_status VARCHAR(50),            -- opened / merged / closed
    merged_time DATETIME,                -- MR 被 merged 的時間 (★ 查詢最關鍵)
    web_url VARCHAR(255),
    created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);




CREATE TABLE gitlab_merge_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_name VARCHAR(255) COMMENT "專案名稱",
    mr_id BIGINT NOT NULL COMMENT "GitLab MR 的 id", 
    iid INT NOT NULL COMMENT "專案內 MR 序號",  
    title VARCHAR(255) COMMENT "MR標題",
    description TEXT COMMENT "MR說明",
    state VARCHAR(20) COMMENT "狀態 merged(完成MR) | opened(尚未MR) | closed(取消MR) ",
    target_branch VARCHAR(100),
    source_branch VARCHAR(100),
    author_name VARCHAR(100),
    merged_by VARCHAR(100),
    merged_at DATETIME NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    UNIQUE KEY uk_mr_id (mr_id)
);





-- vcs.project_versions definition

CREATE TABLE `project_versions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(50) NOT NULL,
  `project_env` varchar(50) NOT NULL COMMENT '部屬環境',
  `version` varchar(50) NOT NULL COMMENT '記錄版號:如 1.0.3',
  `created_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb3;


-- vcs.gitlab_merge_requests definition

CREATE TABLE `gitlab_merge_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `project_name` varchar(50) DEFAULT NULL COMMENT '專案名稱',
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