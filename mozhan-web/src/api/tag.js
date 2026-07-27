import request from '@/utils/request'

/** 标签列表 */
export function getTagList() {
  return request.get('/tag/list')
}

/** 创建标签 */
export function createTag(name) {
  return request.post('/tag', { name })
}
