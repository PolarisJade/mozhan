import request from '@/utils/request'

/** 用户注册 */
export function register(data) {
  return request.post('/user/register', data)
}

/** 用户登录 */
export function login(data) {
  return request.post('/user/login', data)
}

/** 获取用户信息 */
export function getUserInfo(userId) {
  return request.get(`/user/info/${userId}`)
}

/** 修改用户信息 */
export function updateUserInfo(data) {
  return request.put('/user/info/update', data)
}

/** 修改密码 */
export function updatePassword(data) {
  return request.put('/user/password', data)
}

/** 获取用户主页 */
export function getUserProfile(userId) {
  return request.get(`/user/profile/${userId}`)
}

/** 关注用户 */
export function followUser(userId) {
  return request.post(`/user/follow/${userId}`)
}

/** 取消关注用户 */
export function unfollowUser(userId) {
  return request.delete(`/user/follow/${userId}`)
}

/** 检查是否已关注 */
export function isFollowing(userId) {
  return request.get(`/user/follow/status/${userId}`)
}
