import { getToken } from '@/utils/auth'

class ChatSocket {
  constructor() {
    this.socket = null
    this.reconnectTimer = null
    this.isConnected = false
    this.listeners = new Map()
    this.reconnectDelay = 5000
  }

  connect() {
    if (this.isConnected || this.socket?.readyState === WebSocket.OPEN) {
      return
    }

    const token = getToken()
    if (!token) {
      console.warn('未登录，无法连接WebSocket')
      return
    }

    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const wsUrl = `${wsProtocol}//${window.location.host}/api/chatServer?token=${token}`

    this.socket = new WebSocket(wsUrl)

    this.socket.onopen = () => {
      this.isConnected = true
      console.log('WebSocket连接成功')
      this.notify('connected')
      if (this.reconnectTimer) {
        clearTimeout(this.reconnectTimer)
        this.reconnectTimer = null
      }
    }

    this.socket.onmessage = (event) => {
      try {
        const message = JSON.parse(event.data)
        this.notify(message.type, message.data, message.error)
      } catch (e) {
        console.error('解析WebSocket消息失败:', e)
      }
    }

    this.socket.onclose = () => {
      this.isConnected = false
      console.log('WebSocket连接关闭')
      this.notify('disconnected')
      this.scheduleReconnect()
    }

    this.socket.onerror = (error) => {
      console.error('WebSocket错误:', error)
      this.isConnected = false
      this.notify('error', error)
    }
  }

  disconnect() {
    if (this.socket) {
      this.socket.close()
      this.socket = null
    }
    this.isConnected = false
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
  }

  scheduleReconnect() {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
    }
    this.reconnectTimer = setTimeout(() => {
      this.connect()
    }, this.reconnectDelay)
  }

  send(type, data) {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
      console.warn('WebSocket未连接，无法发送消息')
      return false
    }

    const message = JSON.stringify({
      type,
      ...data,
    })

    try {
      this.socket.send(message)
      return true
    } catch (e) {
      console.error('发送消息失败:', e)
      return false
    }
  }

  sendChat(receiverId, content) {
    return this.send('CHAT', { receiverId, content })
  }

  sendRecall(messageId) {
    return this.send('RECALL', { messageId })
  }

  sendRead(sessionId) {
    return this.send('READ', { sessionId: Number(sessionId) })
  }

  sendPing() {
    return this.send('PING', {})
  }

  on(type, callback) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, [])
    }
    this.listeners.get(type).push(callback)
  }

  off(type, callback) {
    const callbacks = this.listeners.get(type)
    if (callbacks) {
      const index = callbacks.indexOf(callback)
      if (index > -1) {
        callbacks.splice(index, 1)
      }
    }
  }

  notify(type, data, error) {
    const callbacks = this.listeners.get(type)
    if (callbacks) {
      callbacks.forEach((callback) => {
        try {
          callback(data, error)
        } catch (e) {
          console.error('事件回调执行失败:', e)
        }
      })
    }
  }

  getConnectionState() {
    return this.socket?.readyState
  }
}

export const chatSocket = new ChatSocket()
export default chatSocket
