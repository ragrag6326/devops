import request from '@/utils/request'


export function getLogAnalysisPage(params) {
  return request.get('/log_analysis', { params })
}