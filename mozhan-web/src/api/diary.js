import request from '@/utils/request'

export function getDiaryList(params) {
  return request.get('/diary/list', { params })
}

export function getDiaryDetail(id) {
  return request.get(`/diary/${id}`)
}

export function createDiary(data) {
  return request.post('/diary/add', data)
}

export function updateDiary(data) {
  return request.put('/diary/update', data)
}

export function deleteDiary(id) {
  return request.put(`/diary/delete/${id}`)
}
