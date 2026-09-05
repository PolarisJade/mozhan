import request from '@/utils/request'
import { getToken } from '@/utils/auth'

// 生成文章大纲（非流式）
export function generateOutline(data) {
  return request.post('/ai/writer/outline', data)
}

// 根据正文生成标题与摘要（非流式）
export function generateMeta(data) {
  return request.post('/ai/writer/meta', data)
}

/**
 * POST + text/event-stream 读取封装
 * 后端流式接口返回的每个事件形如 {"eventData":"片段","eventType":1001}
 * 1001 数据、1002 结束。浏览器 EventSource 仅支持 GET，这里用 fetch 手动读取。
 */
async function streamSSE(path, body, onEvent, signal) {
  const token = getToken()
  const res = await fetch(`/api${path}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...(token ? { Authorization: token } : {}),
    },
    body: JSON.stringify(body),
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

// 根据主题/大纲流式生成正文（HTML）
export function streamGenerateArticle(data, onEvent, signal) {
  return streamSSE('/ai/writer/article', data, onEvent, signal)
}

// 在当前正文基础上流式修改（HTML）
export function streamReviseArticle(data, onEvent, signal) {
  return streamSSE('/ai/writer/revise', data, onEvent, signal)
}