package com.tkb.service;

import java.util.Map;

public interface ProjectInitService {

    /**
     * 初始化專案到遠端機器
     * - PROD：建立 /opt/docker_image/{project}/script/ 並寫入三支腳本
     * - DEV ：追加 service 到 /opt/docker_image/docker-compose.yml
     * - 兩者都寫入 /opt/vcs/tools/{scriptName}/config.sh（若為空則建立空白）
     *
     * @param projectName DB project_config.name
     * @return { prod: "...", dev: "...", config: "..." } 各步驟輸出
     */
    Map<String, String> initProject(String projectName);
}
