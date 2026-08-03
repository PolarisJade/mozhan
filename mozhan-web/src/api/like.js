import request from '@/utils/request'

/**
 * 点赞（文章/随笔通用）
 * @param {string} type - 'article' 或 'essay'
 * @param {number} id - 目标 ID
 */
export function like(type, id) {
  return request.post(`/like/${type}/${id}`)
}

/**
 * 取消点赞（文章/随笔通用）
 * @param {string} type - 'article' 或 'essay'
 * @param {number} id - 目标 ID
 */
export function unlike(type, id) {
  return request.delete(`/like/${type}/${id}`)
}

/* 文章点赞便捷方法 */
export function likeArticle(id) {
  return like('article', id)
}

export function unlikeArticle(id) {
  return unlike('article', id)
}

/* 随笔点赞便捷方法 */
export function likeEssay(id) {
  return like('essay', id)
}

export function unlikeEssay(id) {
  return unlike('essay', id)
}
