import request from '@/utils/request'

export function searchArticle(query) {
  return request.get('/search/article', { params: query })
}

export function searchUser(query) {
  return request.get('/search/user', { params: query })
}