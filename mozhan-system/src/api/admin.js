import request from '@/utils/request'

export function login(data) {
  return request.post('/admin/user/login', data)
}

export function getUserPage(params) {
  return request.get('/admin/user/page', { params })
}

export function getCategoryPage(params) {
  return request.get('/admin/category/page', { params })
}

export function addCategory(data) {
  return request.post('/admin/category', data)
}

export function updateCategory(categoryId, data) {
  return request.put(`/admin/category/${categoryId}`, {}, { params: data })
}

export function deleteCategory(categoryId) {
  return request.delete(`/admin/category/${categoryId}`)
}

export function getTagPage(params) {
  return request.get('/admin/tag/page', { params })
}

export function addTag(data) {
  return request.post('/admin/tag', data)
}

export function deleteTag(id) {
  return request.delete(`/admin/tag/${id}`)
}

export function getEssayPage(params) {
  return request.get('/admin/essay/page', { params })
}

export function deleteEssay(id) {
  return request.delete(`/admin/essay/delete/${id}`)
}

export function getCommentPage(params) {
  return request.get('/admin/comment/page', { params })
}

export function deleteComment(id) {
  return request.delete(`/admin/comment/${id}`)
}
