import request from '@/utils/request'

/** 上传头像 */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData)
}

/** 上传图片（文章/日记配图，jpg/jpeg/png/gif，最大 5MB） */
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData)
}
