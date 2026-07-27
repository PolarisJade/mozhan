import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatSocket } from '@/utils/chatSocket'
import {
  getSessionList,
  getMessageHistory,
  markSessionAsRead,
  recallMessage,
  getUnreadCount,
} from '@/api/chat'
import { useUserStore } from './user'

export const useChatStore = defineStore('chat', () => {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref({})
  const unreadTotal = ref(0)
  const onlineUsers = ref(new Set())
  const isSocketConnected = ref(false)
  const isInitialized = ref(false)
  const loading = ref(false)
  let pollTimer = null
  let heartbeatTimer = null

  const userStore = useUserStore()

  const currentSession = computed(() => {
    if (!currentSessionId.value) return null
    return sessions.value.find((s) => s.sessionId === currentSessionId.value) || null
  })

  const currentMessages = computed(() => {
    return messages.value[currentSessionId.value] || []
  })

  function isUserOnline(userId) {
    return onlineUsers.value.has(userId)
  }

  function initSocket() {
    if (isInitialized.value) {
      chatSocket.connect()
      startPolling()
      startHeartbeat()
      return
    }

    isInitialized.value = true

    chatSocket.on('connected', () => {
      isSocketConnected.value = true
    })

    chatSocket.on('disconnected', () => {
      isSocketConnected.value = false
      onlineUsers.value = new Set()
    })

    chatSocket.on('NEW_MESSAGE', (message) => {
      handleNewMessage(message)
    })

    chatSocket.on('RECALL_NOTICE', (messageId) => {
      handleRecallMessage(messageId)
    })

    chatSocket.on('READ', (sessionId) => {
      handleRead(sessionId)
    })

    chatSocket.on('UNREAD_COUNT', (count) => {
      handleUnreadCount(count)
    })

    chatSocket.on('ONLINE_USERS', (userIds) => {
      onlineUsers.value = new Set(userIds)
    })

    chatSocket.on('USER_ONLINE', (userId) => {
      onlineUsers.value.add(userId)
      onlineUsers.value = new Set(onlineUsers.value)
    })

    chatSocket.on('USER_OFFLINE', (userId) => {
      onlineUsers.value.delete(userId)
      onlineUsers.value = new Set(onlineUsers.value)
    })

    chatSocket.on('MESSAGE_READ', (sessionId) => {
      handleMessageRead(sessionId)
    })

    chatSocket.connect()

    startPolling()
    startHeartbeat()
  }

  function startHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
    }
    heartbeatTimer = setInterval(() => {
      chatSocket.sendPing()
    }, 30000)
  }

  function stopHeartbeat() {
    if (heartbeatTimer) {
      clearInterval(heartbeatTimer)
      heartbeatTimer = null
    }
  }

  function startPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
    }
    pollTimer = setInterval(async () => {
      try {
        const count = await getUnreadCount()
        if (count !== unreadTotal.value) {
          unreadTotal.value = count
          loadSessions()
        }
      } catch (e) {
        console.error('Poll unread count error:', e)
      }
    }, 5000)
  }

  function stopPolling() {
    if (pollTimer) {
      clearInterval(pollTimer)
      pollTimer = null
    }
  }

  function disconnectSocket() {
    chatSocket.disconnect()
    isSocketConnected.value = false
    stopPolling()
    stopHeartbeat()
  }

  async function loadSessions() {
    loading.value = true
    try {
      const data = await getSessionList({})
      const sessionList = data.list || data
      sessions.value = sessionList.map(session => ({
        ...session,
        sessionId: String(session.sessionId)
      }))
    } catch (e) {
      console.error('Load sessions error:', e)
    } finally {
      loading.value = false
    }
  }

  async function loadMessages(sessionId) {
    const sessionIdStr = String(sessionId)
    if (!sessionIdStr) return
    loading.value = true
    try {
      const data = await getMessageHistory({ sessionId: sessionIdStr, isAsc: true })
      const newMessages = data.list || data || []

      if (!messages.value[sessionIdStr]) {
        messages.value[sessionIdStr] = []
      }

      const existingIds = new Set(messages.value[sessionIdStr].map(m => m.messageId))
      let added = 0
      newMessages.forEach(msg => {
        if (!existingIds.has(msg.messageId)) {
          messages.value[sessionIdStr].push(msg)
          added++
        }
      })

      if (added > 0) {
        messages.value[sessionIdStr].sort((a, b) => {
          const timeA = typeof a.createTime === 'string' ? new Date(a.createTime).getTime() : (a.createTime || 0)
          const timeB = typeof b.createTime === 'string' ? new Date(b.createTime).getTime() : (b.createTime || 0)
          return timeA - timeB
        })
      }

      await markAsRead(sessionIdStr)
    } catch (e) {
      console.error('Load messages error:', e)
    } finally {
      loading.value = false
    }
  }

  async function selectSession(sessionId) {
    const sessionIdStr = String(sessionId)
    currentSessionId.value = sessionIdStr
    await loadMessages(sessionIdStr)
  }

  function sendMessage(receiverId, content) {
    if (!content.trim()) return false

    const success = chatSocket.sendChat(receiverId, content)
    if (success && currentSessionId.value) {
      const sessionId = String(currentSessionId.value)
      const tempMessage = {
        messageId: -Date.now(),
        sessionId,
        senderId: userStore.user?.id,
        receiverId,
        content,
        status: 'NORMAL',
        readStatus: 0,
        createTime: Date.now()
      }
      if (!messages.value[sessionId]) {
        messages.value[sessionId] = []
      }
      messages.value[sessionId].push(tempMessage)
    }
    return success
  }

  function requestNotificationPermission() {
    if ('Notification' in window) {
      Notification.requestPermission()
    }
  }

  function showNotification(title, body) {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification(title, {
        body,
        icon: '/favicon.ico'
      })
    }
  }

  function handleNewMessage(message) {
    const sessionId = String(message.sessionId)

    if (!messages.value[sessionId]) {
      messages.value[sessionId] = []
    }

    const existingIndex = messages.value[sessionId].findIndex(m => m.messageId === message.messageId)
    if (existingIndex > -1) {
      messages.value[sessionId][existingIndex] = message
    } else {
      const tempIndex = messages.value[sessionId].findIndex(
        m => m.senderId === userStore.user?.id && m.content === message.content && m.messageId < 0
      )
      if (tempIndex > -1) {
        messages.value[sessionId][tempIndex] = message
      } else {
        messages.value[sessionId].push(message)
        messages.value[sessionId].sort((a, b) => {
          const timeA = typeof a.createTime === 'string' ? new Date(a.createTime).getTime() : (a.createTime || 0)
          const timeB = typeof b.createTime === 'string' ? new Date(b.createTime).getTime() : (b.createTime || 0)
          return timeA - timeB
        })
      }
    }

    const sessionIndex = sessions.value.findIndex((s) => s.sessionId === sessionId)
    if (sessionIndex > -1) {
      sessions.value[sessionIndex].lastMessage = message.content
      sessions.value[sessionIndex].lastMessageTime = message.createTime

      if (String(message.senderId) !== String(userStore.user?.id)) {
        sessions.value[sessionIndex].unreadCount =
          (sessions.value[sessionIndex].unreadCount || 0) + 1
      }

      sessions.value.splice(0, 0, sessions.value.splice(sessionIndex, 1)[0])
    } else {
      loadSessions()
    }

    if (currentSessionId.value === sessionId) {
      markAsRead(sessionId)
    }
  }

  function handleRecallMessage(messageId) {
    for (const sessionId in messages.value) {
      const msgIndex = messages.value[sessionId].findIndex((m) => m.messageId === messageId)
      if (msgIndex > -1) {
        messages.value[sessionId][msgIndex].content = '[Message recalled]'
        messages.value[sessionId][msgIndex].status = 'RECALLED'
        break
      }
    }
  }

  function handleRead(sessionId) {
    const sessionIdStr = String(sessionId)
    const session = sessions.value.find((s) => s.sessionId === sessionIdStr)
    if (session) {
      session.unreadCount = 0
    }
  }

  function handleUnreadCount(count) {
    if (typeof count === 'number') {
      unreadTotal.value = count
      loadSessions()
    }
  }

  function handleMessageRead(sessionId) {
    const sessionIdStr = String(sessionId)
    const msgs = messages.value[sessionIdStr]
    if (!msgs) return
    const myId = userStore.user?.id
    if (!myId) return
    let changed = false
    msgs.forEach(m => {
      if (m.senderId === myId && m.readStatus !== 1) {
        m.readStatus = 1
        changed = true
      }
    })
    if (changed) {
      messages.value[sessionIdStr] = [...msgs]
    }
  }

  async function markAsRead(sessionId) {
    const sessionIdStr = String(sessionId)
    const session = sessions.value.find((s) => s.sessionId === sessionIdStr)
    if (session && session.unreadCount > 0) {
      try {
        await markSessionAsRead(sessionIdStr)
        handleRead(sessionIdStr)
      } catch (e) {
        console.error('Mark as read error:', e)
      }
    }
  }

  async function doRecall(messageId) {
    try {
      await recallMessage(messageId)
      handleRecallMessage(messageId)
      return true
    } catch (e) {
      console.error('Recall message error:', e)
      return false
    }
  }

  async function refreshUnreadCount() {
    try {
      const count = await getUnreadCount()
      unreadTotal.value = count
    } catch (e) {
      console.error('Refresh unread count error:', e)
    }
  }

  function clearMessages(sessionId) {
    if (messages.value[sessionId]) {
      delete messages.value[sessionId]
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    unreadTotal,
    onlineUsers,
    isSocketConnected,
    isInitialized,
    loading,
    currentSession,
    currentMessages,
    isUserOnline,
    initSocket,
    disconnectSocket,
    loadSessions,
    loadMessages,
    selectSession,
    sendMessage,
    markAsRead,
    doRecall,
    refreshUnreadCount,
    clearMessages,
    requestNotificationPermission,
  }
})
