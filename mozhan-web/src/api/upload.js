import request from '@/utils/request'

/** 上传头像 */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/avatar', formData)
}
