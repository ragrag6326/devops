import request from '@/utils/request'

export const getMrReviewPage = (params) =>
  request.get('/mr-review', { params })

export const getMrReviewDetail = (projectName, mrIid) =>
  request.get('/mr-review/detail', { params: { projectName, mrIid } })

export const scanMrReviewProject = (projectName) =>
  request.post(`/mr-review/scan/${projectName}`)

export const scanMrReviewAll = () =>
  request.post('/mr-review/scan')
