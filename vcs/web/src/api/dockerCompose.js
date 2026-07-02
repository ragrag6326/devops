import request from "@/utils/request";

/**
 * 讀取遠端 docker-compose.yml
 * PROD: /opt/docker_image/{project}/docker-compose.yml
 * DEV:  /opt/docker_image/docker-compose.yml
 * @param {string} projectName
 * @param {string} env - "prod" | "dev"
 * @returns {{ content: string, exists: string }}
 */
export const getDockerCompose = (projectName, env) =>
    request.get(`/docker-compose/${projectName}`, { params: { env } });

/**
 * 寫入遠端 docker-compose.yml（無則建立）
 * @param {string} projectName
 * @param {string} env - "prod" | "dev"
 * @param {string} content - YAML 文字內容
 */
export const saveDockerCompose = (projectName, env, content) =>
    request.post(`/docker-compose/${projectName}`, { content }, { params: { env } });
