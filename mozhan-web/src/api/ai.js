import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 新建会话，返回 SessionVO（sessionId、助手标题、描述、示例列表）
export function createAISession(num = 3) {
  return request.post('/ai/session', null, { params: { num } })
}

// 获取热门示例问题
export function getAIHotExamples(num = 3) {
  return request.get('/ai/session/hot', { params: { num } })
}

// 查询某个会话的历史消息
export function getAISessionMessages(sessionId) {
  return request.get(`/ai/session/${sessionId}`)
}

// 查询历史会话列表（游标分页：pageSize / sortBy / isAsc / nextCursor）
export function getAISessionHistory(query) {
  return request.get('/ai/session/history', { params: query })
}

// 更新会话标题
export function updateAISessionTitle(sessionId, title) {
  return request.put('/ai/session/history', null, { params: { sessionId, title } })
}

// 删除历史会话
export function deleteAISession(sessionId) {
  return request.delete('/ai/session/history', { params: { sessionId } })
}

// 停止当前生成
export function stopAIChat(sessionId) {
  return request.post('/ai/chat/stop', null, { params: { sessionId } })
}

/**
 * 流式对话（SSE）
 * 后端 POST /ai/chat 返回 text/event-stream，每个事件形如：
 *   {"eventData":"文本片段...","eventType":1001}  -> DATA 数据事件
 *   {"eventData":{...},          "eventType":1003} -> PARAM 工具参数事件（如文章推荐）
 *   {"eventData":null,           "eventType":1002} -> STOP 停止事件
 * 注意：请求体为 JSON，不能使用浏览器原生 EventSource（仅支持 GET），
 * 因此这里用 fetch 手动读取响应流并解析 SSE 的 data: 行。
 */
export async function streamAIChat({ question, sessionId, onEvent, signal }) {
  const token = getToken()

  const res = await fetch('/api/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: token } : {}),
    },
    body: JSON.stringify({ question, sessionId }),
    signal,
  })

  if (!res.ok || !res.body) {
    let message = `请求失败（${res.status}）`
    try {
      const data = await res.json()
      if (data?.message) message = data.message
    } catch (_) {
      /* 忽略解析失败，使用默认错误信息 */
    }
    throw new Error(message)
  }

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buffer = ''

  const parseLine = (line) => {
    const trimmed = line.trim()
    if (!trimmed.startsWith('data:')) return
    const payload = trimmed.slice(5).trim()
    if (!payload) return
    try {
      onEvent(JSON.parse(payload))
    } catch (_) {
      /* 忽略无法解析的数据行 */
    }
  }

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })

    let idx
    while ((idx = buffer.indexOf('\n')) !== -1) {
      const line = buffer.slice(0, idx)
      buffer = buffer.slice(idx + 1)
      parseLine(line)
    }
  }

  // 处理流结束后可能残留的最后一行
  buffer += decoder.decode()
  if (buffer.trim()) parseLine(buffer)
}