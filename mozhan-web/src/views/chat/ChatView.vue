<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { InkMessage, InkAvatar } from '@/utils/message'

const route = useRoute()
const chatStore = useChatStore()
const userStore = useUserStore()

const messageInput = ref('')
const messagesContainer = ref(null)

const newChatUserId = ref(null)
const newChatUserInfo = ref(null)

const currentChatUser = computed(() => {
  if (chatStore.currentSession) {
    return chatStore.currentSession.targetUser
  }
  return newChatUserInfo.value
})

onMounted(() => {
  chatStore.initSocket()
  chatStore.loadSessions()
  chatStore.refreshUnreadCount()
  
  if (route.query.userId) {
    newChatUserId.value = Number(route.query.userId)
    newChatUserInfo.value = {
      id: newChatUserId.value,
      nickname: route.query.nickname || '',
      avatar: route.query.avatar || ''
    }
    connectToUser(newChatUserId.value)
  }
})

async function connectToUser(userId) {
  await chatStore.loadSessions()
  
  const session = chatStore.sessions.find(s => s.targetUser.id === userId)
  if (session) {
    await chatStore.selectSession(session.sessionId)
  }
}

async function handleSelectSession(session) {
  await chatStore.selectSession(session.sessionId)
  newChatUserId.value = null
  newChatUserInfo.value = null
  nextTick(() => {
    scrollToBottom()
  })
}

function handleSendMessage() {
  if (!messageInput.value.trim()) return

  const receiverId = chatStore.currentSession 
    ? chatStore.currentSession.targetUser.id 
    : newChatUserId.value

  if (!receiverId) return

  const content = messageInput.value.trim()

  const success = chatStore.sendMessage(receiverId, content)
  if (success) {
    messageInput.value = ''
    nextTick(() => {
      scrollToBottom()
    })
  } else {
    InkMessage.error('Send failed. Please try again.')
  }
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSendMessage()
  }
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

let scrollHideTimer = null
function onMessagesScroll() {
  if (!messagesContainer.value) return
  messagesContainer.value.classList.add('is-scrolling')
  if (scrollHideTimer) clearTimeout(scrollHideTimer)
  scrollHideTimer = setTimeout(() => {
    if (messagesContainer.value) {
      messagesContainer.value.classList.remove('is-scrolling')
    }
    scrollHideTimer = null
  }, 600)
}

function formatTime(dateStr) {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) {
    return 'Just now'
  } else if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`
  } else if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`
  } else if (diff < 604800000) {
    return `${Math.floor(diff / 86400000)}天前`
  }

  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')

  if (year === now.getFullYear()) {
    return `${month}-${day} ${hour}:${minute}`
  }
  return `${year}-${month}-${day} ${hour}:${minute}`
}

function shouldShowTimeDivider(index) {
  if (index === 0) return true
  const currentMsg = chatStore.currentMessages[index]
  const prevMsg = chatStore.currentMessages[index - 1]
  const currentTime = new Date(currentMsg.createTime).getTime()
  const prevTime = new Date(prevMsg.createTime).getTime()
  return currentTime - prevTime > 60000
}

function formatTimeDivider(dateStr) {
  const date = new Date(dateStr)
  const now = new Date()
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')

  if (year === now.getFullYear() && month === String(now.getMonth() + 1).padStart(2, '0') && day === String(now.getDate()).padStart(2, '0')) {
    return `Today ${hour}:${minute}`
  }
  return `${month}-${day} ${hour}:${minute}`
}

async function handleRecall(message) {
  if (message.senderId !== userStore.user?.id) {
    InkMessage.warning('Can only recall your own messages')
    return
  }

  const ok = await chatStore.doRecall(message.messageId)
  if (ok) {
    InkMessage.success('Message recalled')
  }
}

watch(
  () => chatStore.currentMessages,
  () => {
    nextTick(() => {
      scrollToBottom()
    })
  },
  { deep: true }
)
</script>

<template>
  <div class="chat-page">
    <div class="chat-sidebar">
      <div class="sidebar-header">
        <h2 class="font-display">消息列表</h2>
        <span class="unread-badge" v-if="chatStore.unreadTotal > 0">
          {{ chatStore.unreadTotal }}
        </span>
      </div>

      <div class="session-list" v-if="chatStore.sessions.length > 0">
        <div
          v-for="session in chatStore.sessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: chatStore.currentSessionId === session.sessionId }"
          @click="handleSelectSession(session)"
        >
          <InkAvatar
            :size="40"
            :src="session.targetUser.avatar"
            class="session-avatar"
          >
            {{ session.targetUser.nickname?.[0] || session.targetUser.username?.[0] }}
          </InkAvatar>
          <div class="session-info">
            <div class="session-name">
              {{ session.targetUser.nickname || session.targetUser.username }}
              <span class="online-dot" v-if="chatStore.isUserOnline(session.targetUser.id)"></span>
            </div>
            <div class="session-preview">{{ session.lastMessage || 'No messages yet' }}</div>
          </div>
          <div class="session-meta">
            <span class="session-time">{{ formatTime(session.lastMessageTime) }}</span>
            <span class="unread-count" v-if="session.unreadCount > 0">
              {{ session.unreadCount }}
            </span>
          </div>
        </div>
      </div>

      <div class="empty-state" v-else>
        <div class="empty-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
        </div>
        <p>No messages yet</p>
      </div>
    </div>

    <div class="chat-content" v-if="chatStore.currentSession || newChatUserInfo">
      <div class="chat-header">
        <InkAvatar
          :size="44"
          :src="currentChatUser?.avatar"
          class="chat-avatar"
        >
          {{ currentChatUser?.nickname?.[0] || currentChatUser?.username?.[0] }}
        </InkAvatar>
        <div class="chat-header-info">
          <h3>{{ currentChatUser?.nickname || currentChatUser?.username }}</h3>
          <span class="online-status" :class="{ online: currentChatUser && chatStore.isUserOnline(currentChatUser.id) }">
            {{ currentChatUser && chatStore.isUserOnline(currentChatUser.id) ? '在线' : '离线' }}
          </span>
        </div>
      </div>

      <div ref="messagesContainer" class="messages-container" @scroll="onMessagesScroll">
        <template v-for="(message, index) in chatStore.currentMessages" :key="message.messageId">
          <div
            v-if="shouldShowTimeDivider(index)"
            class="time-divider"
          >
            <span>{{ formatTimeDivider(message.createTime) }}</span>
          </div>
          <div
            class="message-item"
            :class="{
              'self': message.senderId === userStore.user?.id,
              'recalled': message.status === 'RECALLED'
            }"
          >
            <InkAvatar
              :size="36"
              :src="message.senderId === userStore.user?.id ? userStore.user.avatar : currentChatUser?.avatar"
              class="message-avatar"
            >
              {{
                message.senderId === userStore.user?.id
                  ? userStore.user.nickname?.[0] || userStore.user.username?.[0]
                  : currentChatUser?.nickname?.[0] || currentChatUser?.username?.[0]
              }}
            </InkAvatar>
            <div class="message-main">
              <div class="message-bubble">
                <div class="message-content">{{ message.content }}</div>
              </div>
              <div class="message-footer">
                <span
                  v-if="message.senderId === userStore.user?.id && message.status !== 'RECALLED'"
                  class="recall-btn"
                  @click="handleRecall(message)"
                  title="Recall"
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <path d="M3 10h10a5 5 0 0 1 5 5v2M3 10l4-4m-4 4l4 4"/>
                  </svg>
                  <span>Recall</span>
                </span>
              </div>
            </div>
            <div v-if="message.senderId === userStore.user?.id" class="read-status-wrapper">
              <span v-if="message.readStatus === 1 || message.readStatus === 'READ'" class="read-status-read">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </span>
              <span v-else class="read-status-unread">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <circle cx="12" cy="12" r="10"/>
                </svg>
              </span>
            </div>
          </div>
        </template>

        <div class="typing-indicator" v-if="false">
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
          <span class="typing-dot"></span>
        </div>
      </div>

      <div class="chat-input-area">
        <textarea
          v-model="messageInput"
          class="message-input"
          placeholder="请输入消息......"
          rows="1"
          @keydown="handleKeydown"
        ></textarea>
        <button class="send-btn" @click="handleSendMessage" :disabled="!messageInput.trim()">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M22 2L11 13" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M22 2L15 22L11 13L2 9L22 2" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
        </button>
      </div>
    </div>

    <div class="chat-content empty-chat" v-else>
      <div class="empty-chat-icon">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
        </svg>
      </div>
      <p>选择一个对话开始聊天吧</p>
    </div>
  </div>
</template>

<style scoped>
.chat-page {
  display: flex;
  height: calc(100vh - 124px);  /* 100vh - 92px(上) - 32px(下) = 填满 main 内容区 */
  gap: 0;
  padding-top: 16px;
  box-sizing: border-box;
  overflow: hidden;             /* 防止内部内容溢出导致页面滚动 */
}

.chat-sidebar {
  width: 320px;
  border-right: 1px solid rgba(26, 26, 26, 0.08);
  background: var(--paper-card);
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.sidebar-header h2 {
  font-size: 20px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.1em;
  margin: 0;
}

.unread-badge {
  background: var(--seal-red);
  color: white;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 18px;
  text-align: center;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.session-item:hover {
  background-color: rgba(26, 26, 26, 0.04);
}

.session-item.active {
  background-color: rgba(26, 26, 26, 0.06);
}

.session-avatar {
  flex-shrink: 0;
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-name {
  font-size: 15px;
  color: var(--ink);
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.online-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  background: #52c41a;
  border-radius: 50%;
  margin-left: 6px;
  vertical-align: middle;
  flex-shrink: 0;
}

.session-preview {
  font-size: 13px;
  color: var(--ink-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.session-time {
  font-size: 12px;
  color: var(--ink-muted);
}

.unread-count {
  background: var(--seal-red);
  color: white;
  font-size: 11px;
  padding: 1px 5px;
  border-radius: 8px;
  min-width: 16px;
  text-align: center;
}

.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--ink-muted);
}

.empty-icon {
  width: 48px;
  height: 48px;
  margin-bottom: 12px;
  opacity: 0.4;
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--paper);
}

.empty-chat {
  align-items: center;
  justify-content: center;
  color: var(--ink-muted);
}

.empty-chat-icon {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  opacity: 0.3;
}

.empty-chat-icon svg {
  width: 100%;
  height: 100%;
}

.chat-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
  background: var(--paper-card);
}

.chat-avatar {
  flex-shrink: 0;
}

.chat-header-info h3 {
  font-size: 16px;
  font-weight: 500;
  color: var(--ink);
  margin: 0 0 4px 0;
}

.online-status {
  font-size: 13px;
  color: var(--ink-muted);
}

.online-status.online {
  color: #52c41a;
}

.time-divider {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 16px 0;
}

.time-divider span {
  font-size: 12px;
  color: var(--ink-muted);
  background: rgba(255, 255, 255, 0.8);
  padding: 4px 12px;
  border-radius: 12px;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 40px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  scrollbar-width: thin;
  scrollbar-color: transparent transparent;
  transition: scrollbar-color 0.25s ease;
}

.messages-container::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.messages-container::-webkit-scrollbar-track {
  background: transparent;
}

.messages-container::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 3px;
  transition: background 0.25s ease;
}

/* 悬停时显示滚动条 */
.messages-container:hover,
.messages-container:focus-within {
  scrollbar-color: rgba(26, 26, 26, 0.2) transparent;
}

.messages-container:hover::-webkit-scrollbar-thumb,
.messages-container:focus-within::-webkit-scrollbar-thumb {
  background: rgba(26, 26, 26, 0.2);
}

/* 滚动时显示滚动条（由 onMessagesScroll 控制 is-scrolling 类，600ms 后自动移除） */
.messages-container.is-scrolling {
  scrollbar-color: rgba(26, 26, 26, 0.2) transparent;
}

.messages-container.is-scrolling::-webkit-scrollbar-thumb {
  background: rgba(26, 26, 26, 0.2);
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 70%;
  align-items: center;
}

.message-item.self {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-self: flex-start;
  padding-top: 15px;
}

.read-status-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
}

.message-avatar {
  flex-shrink: 0;
}

.read-status-wrapper {
  display: flex;
}

.message-bubble {
  background: var(--paper-card);
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 4px;
  padding: 10px 14px;
  max-width: 100%;
}

.message-item.self .message-bubble {
  background: var(--ink);
  border-color: var(--ink);
}

.message-content {
  font-size: 14px;
  line-height: 1.6;
  color: var(--ink-light);
  word-break: break-word;
}

.message-item.self .message-content {
  color: #f0ebe3;
}

.message-item.recalled .message-content {
  color: var(--ink-muted);
  font-style: italic;
}

.message-footer {
  display: flex;
  justify-content: flex-end;
}

.recall-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--ink-muted);
  cursor: pointer;
  transition: color 0.2s;
}

.recall-btn svg {
  width: 12px;
  height: 12px;
}

.recall-btn:hover {
  color: var(--seal-red);
}

.message-item.self .recall-btn {
  color: rgba(240, 235, 227, 0.6);
}

.message-item.self .recall-btn:hover {
  color: #ff4d4f;
}

.read-status-read {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background-color: #52c41a;
  color: white;
}

.read-status-read svg {
  width: 10px;
  height: 10px;
}

.read-status-unread {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background-color: #d9d9d9;
  color: rgba(0, 0, 0, 0.5);
}

.read-status-unread svg {
  width: 10px;
  height: 10px;
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 12px;
  background: var(--paper-card);
  border-radius: 4px;
  align-self: flex-start;
}

.typing-dot {
  width: 6px;
  height: 6px;
  background: var(--ink-muted);
  border-radius: 50%;
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-dot:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.chat-input-area {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba(26, 26, 26, 0.08);
  background: var(--paper-card);
  align-items: flex-end;
}

.message-input {
  flex: 1;
  min-height: 36px;
  max-height: 120px;
  padding: 10px 14px;
  background: rgba(255, 252, 247, 0.8);
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 4px;
  font-size: 14px;
  font-family: 'Noto Serif SC', serif;
  color: var(--ink);
  resize: none;
  outline: none;
  transition: border-color 0.2s;
}

.message-input:focus {
  border-color: var(--ink);
}

.message-input::placeholder {
  color: var(--ink-muted);
}

.send-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ink);
  border: none;
  border-radius: 4px;
  color: #f0ebe3;
  cursor: pointer;
  transition: background-color 0.2s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: var(--accent);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn svg {
  width: 18px;
  height: 18px;
}
</style>
