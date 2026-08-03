import request from '@/utils/request'

export function getHotArticles(params) {
  return request.get('/article/hot', { params })
}

export function getArticleList(params) {
  return request.get('/article/list', { params })
}

export function getArticlesByAuthor(authorId, params) {
  return request.get('/article/list', { params: { ...params, authorId } })
}

export function getArticleDetail(id) {
  return request.get(`/article/detail/${id}`)
}

export function getArticleInfo(id) {
  return request.get(`/article/info/${id}`)
}

export function createArticle(data) {
  return request.post('/article', data)
}

export function updateArticle(id, data) {
  return request.put(`/article/update/${id}`, data)
}

export function publishArticle(id) {
  return request.put(`/article/publish/${id}`)
}

export function getMyArticleList(params) {
  return request.get('/article/my', { params })
}

export function deleteArticle(id) {
  return request.delete(`/article/delete/${id}`)
}
