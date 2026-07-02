-- 專案設定表：儲存各專案的顯示名稱、分類、GitLab 專案 ID 等資訊
CREATE TABLE IF NOT EXISTS project_config (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(100) NOT NULL UNIQUE         COMMENT '系統名稱 (對應 project_versions.project_name)',
    display_name VARCHAR(200)                         COMMENT '前端顯示名稱',
    description  VARCHAR(500)                         COMMENT '專案描述',
    gitlab_project_id BIGINT                          COMMENT 'GitLab Project ID',
    category     VARCHAR(50)  NOT NULL DEFAULT 'backend' COMMENT '分類: frontend / backend',
    is_active    TINYINT(1)   NOT NULL DEFAULT 1      COMMENT '是否啟用 (1=啟用, 0=停用)',
    sort_order   INT          NOT NULL DEFAULT 0      COMMENT '排序 (數字越小越前)',
    created_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='專案設定表';

-- 初始資料 (對應原本 application.yml 中的 gitlab.projects)
INSERT INTO project_config (name, display_name, gitlab_project_id, category, sort_order) VALUES
    ('tkbgoapi', 'TKB Go API',  13,       'backend',  1),
    ('tkbtv',    'TKB TV',      3,        'backend',  2),
    ('go_nuxt',  'Go Nuxt',     17,       'frontend', 3);
