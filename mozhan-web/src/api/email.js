import request from '@/utils/request'

export function sendVerificationCode(email) {
  return request.post('/email/sendCode', { email })
}