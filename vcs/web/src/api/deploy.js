import request from "@/utils/request";


/** 
 * deploying
 * @param {object} params DeployDTO
 * @param {string} params.projectName 
 * @param {string} params.projectEnv 
 * @param {string} params.version 
 * @param {string} params.user 
 * @param {string} params.releaseNote 
 * @returns
 */
export function deploying(params) {
  return request.post(`/deploy/deploying`, params);
}

