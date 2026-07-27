import request from '@/utils/request'

/** 文章评论列表 */
export function getCommentsByArticle(articleId, query) {
  return request.get(`/comment/${articleId}`, { params: query })
}

/** 发表评论 */
export function createComment(data) {
  return request.post('/comment', data)
}

/** 回复评论 */
export function replyComment(data) {
  return request.post('/comment/reply', data)
}

/** 删除评论 */
export function deleteComment(id) {
  return request.delete(`/comment/${id}`)
}

/** 我的评论 */
export function getMyComments(query) {
  return request.get('/comment/my', { params: query })
}
