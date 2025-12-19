import request from "@/utils/request";

/** 
 * switchTraffic
 * @param {string} projectName 
  * @param {string} target      blue / green
  * @param {string} mode       (選填)未填切換正式分流 / (header) 切換  header 的 upstream
  * @returns  "data": "無須切換" or "data": "green header切換完畢"
 */
export function switchTraffic(projectName, target, mode) {
  return request.patch(`/monitor/switchTraffic?projectName=${projectName}&target=${target}&mode=${mode}`);
}


/** 
 * getCurrentTraffic
 * @param {string} projectName 
  * @param {string} type     live / header
  * @returns   projectName=tv type=header  "data": "GREEN_ACTIVE"
 */
export function getCurrentTraffic(projectName, type) {
  return request.get(`/monitor/traffic/${projectName}/${type}`);
}

/** 
 * healthCheck
 * @param {string} projectName 
  * @param {string} type        blue / green
  * @returns  "data": 200
 */
export function healthCheck(projectName, type) {
  return request.get(`/monitor/health/${projectName}/${type}`);
}