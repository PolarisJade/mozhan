import request from '@/utils/request'

export function getSessionList(query) {
  return request.get('/chat/session/sessions', { params: query })
}

export function getMessageHistory(query) {
  const { sessionId, ...rest } = query
  return request.get(`/chat/session/${Number(sessionId)}`, { params: rest })
}

export function markSessionAsRead(sessionId) {
  return request.put(`/chat/session/${Number(sessionId)}/read`)
}

export function recallMessage(messageId) {
  return request.put(`/chat/message/${Number(messageId)}/recall`)
}

export function getUnreadCount() {
  return request.get('/chat/message/unread/count')
}