import request from '@/utils/request'

/** 分类列表 */
export function getCategoryList() {
  return request.get('/category/list')
}
