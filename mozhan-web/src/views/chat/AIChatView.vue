<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { marked } from 'marked'
import {
  createAISession,
  getAISessionHistory,
  getAISessionMessages,
  deleteAISession,
  streamAIChat,
  stopAIChat,
} from '@/api/ai'
import { InkMessage, InkMessageBox } from '@/utils/message'

marked.setOptions({ breaks: true, gfm: true })

const assistant = ref({ title: 'AI助手', describe: '你的个人AI助手' })
const sessionId = ref(null)
const examples = ref([])
const historyList = ref([])
const historyLoading = ref(false)
const messages = ref([])
const input = ref('')
const streaming = ref(false)
const abortController = ref(null)
const messagesContainer = ref(null)

let msgSeq = 0
const newId = () => ++msgSeq
let historyTimer = null

onMounted(async () => {
  await initNewSession()
  loadHistory()
})

onBeforeUnmount(() => {
  if (abortController.value) abortController.value.abort()
  if (historyTimer) clearTimeout(historyTimer)
})

async function initNewSession() {
  if (abortController.value) abortController.value.abort()
  streaming.value = false
  try {
    const data = await createAISession(3)
    assistant.value = {
      title: data?.title || 'AI助手',
      describe: data?.describe || '',
    }
    sessionId.value = data.sessionId
    examples.value = data?.examples || []
    messages.value = []
    nextTick(scrollToBottom)
  } catch (e) {
    InkMessage.error('初始化会话失败，请稍后重试')
  }
}

async function loadHistory() {
  historyLoading.value = true
  try {
    const data = await getAISessionHistory({ pageSize: 20 })
    historyList.value = data?.list || []
  } catch (e) {
    /* 历史列表加载失败不打断主流程 */
  } finally {
    historyLoading.value = false
  }
}

// 会话结束后刷新历史列表；由于标题由后端异步生成，稍作延迟再刷一次
function scheduleHistoryRefresh() {
  if (historyTimer) clearTimeout(historyTimer)
  historyTimer = setTimeout(() => {
    loadHistory()
    historyTimer = null
  }, 2500)
}

function handleNewChat() {
  initNewSession()
}

async function selectHistory(sess) {
  if (streaming.value && abortController.value) abortController.value.abort()
  streaming.value = false
  sessionId.value = sess.sessionId
  try {
    const list = await getAISessionMessages(sess.sessionId)
    messages.value = (list || []).map((m) => ({
      id: newId(),
      type: m.type,
      content: m.content || '',
      params: m.params || null,
      streaming: false,
    }))
    examples.value = []
    nextTick(scrollToBottom)
  } catch (e) {
    InkMessage.error('加载历史对话失败')
  }
}

async function handleDelete(sess) {
  try {
    await InkMessageBox.confirm('确定删除该对话吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteAISession(sess.sessionId)
    if (sessionId.value === sess.sessionId) {
      await initNewSession()
    }
    await loadHistory()
  } catch (e) {
    InkMessage.error('删除失败，请稍后重试')
  }
}

function send(text) {
  const question = (text ?? input.value).trim()
  if (!question) return
  if (streaming.value) return
  if (!sessionId.value) return

  input.value = ''
  examples.value = []

  // 用户消息
  messages.value.push({
    id: newId(),
    type: 'USER',
    content: question,
    params: null,
    streaming: false,
  })

  // AI 占位消息（reactive 包裹，流式追加内容才能触发视图更新）
  const assistantMsg = reactive({
    id: newId(),
    type: 'ASSISTANT',
    content: '',
    params: null,
    streaming: true,
  })
  messages.value.push(assistantMsg)

  streaming.value = true
  const ctl = new AbortController()
  abortController.value = ctl

  streamAIChat({
    question,
    sessionId: sessionId.value,
    signal: ctl.signal,
    onEvent: (event) => {
      if (event.eventType === 1001) {
        // 文本片段，累加
        assistantMsg.content += event.eventData ?? ''
      } else if (event.eventType === 1003) {
        // 工具参数（如文章推荐列表）
        assistantMsg.params = event.eventData ?? null
      } else if (event.eventType === 1002) {
        // 生成结束
        assistantMsg.streaming = false
      }
      scrollToBottom()
    },
  })
    .then(() => {
      assistantMsg.streaming = false
      streaming.value = false
      abortController.value = null
      loadHistory()
      scheduleHistoryRefresh()
    })
    .catch((err) => {
      assistantMsg.streaming = false
      streaming.value = false
      abortController.value = null
      if (err?.name !== 'AbortError') {
        InkMessage.error(err?.message || '对话失败，请稍后重试')
      } else {
        loadHistory()
      }
    })
}

function handleStop() {
  if (abortController.value) abortController.value.abort()
  if (sessionId.value) {
    stopAIChat(sessionId.value).catch(() => {})
  }
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 将 AI 文本渲染为 Markdown HTML
function md(text) {
  return marked.parse(text || '')
}

// 从工具参数中提取可展示的文章/随笔推荐卡片
function extractResults(params) {
  const list = []
  if (!params) return list
  for (const [key, value] of Object.entries(params)) {
    if (!Array.isArray(value)) continue
    const isArticle = key.startsWith('articleInfo')
    for (const item of value) {
      if (item && (item.title || item.summary)) {
        list.push({
          id: item.id,
          title: item.title,
          summary: item.summary,
          authorName: item.authorName,
          type: isArticle ? 'article' : 'essay',
        })
      }
    }
  }
  return list
}

function goResult(result) {
  const name = result.type === 'article' ? 'ArticleDetail' : 'EssayDetail'
  window.open(
    `${window.location.origin}/${result.type}/${result.id}`,
    '_blank',
  )
}

function formatSessionTime(value) {
  if (!value) return ''
  let date
  if (Array.isArray(value)) {
    const [y, m, d, hh, mm, ss] = value
    date = new Date(y, (m ?? 1) - 1, d ?? 1, hh ?? 0, mm ?? 0, ss ?? 0)
  } else {
    date = new Date(value)
  }
  if (isNaN(date.getTime())) return ''

  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`

  const y = date.getFullYear()
  const mo = String(date.getMonth() + 1).padStart(2, '0')
  const da = String(date.getDate()).padStart(2, '0')
  if (y === now.getFullYear()) return `${mo}-${da}`
  return `${y}-${mo}-${da}`
}
</script>

<template>
  <div class="ai-page">
    <!-- 历史会话侧边栏 -->
    <div class="ai-sidebar">
      <div class="sidebar-header">
        <h2 class="font-display">历史对话</h2>
      </div>
      <button class="new-chat-btn" @click="handleNewChat">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="M12 5v14M5 12h14"/>
        </svg>
        <span>新对话</span>
      </button>

      <div v-if="historyList.length > 0" class="history-list">
        <div
          v-for="sess in historyList"
          :key="sess.sessionId"
          class="history-item"
          :class="{ active: sessionId === sess.sessionId }"
          @click="selectHistory(sess)"
        >
          <div class="history-info">
            <div class="history-title">{{ sess.title }}</div>
            <div class="history-time">{{ formatSessionTime(sess.updateTime) }}</div>
          </div>
          <button
            class="history-delete"
            title="删除"
            @click.stop="handleDelete(sess)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0 0 1 1 1v2m3 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/>
            </svg>
          </button>
        </div>
      </div>

      <div v-else class="history-empty">
        <p>{{ historyLoading ? '加载中...' : '暂无历史对话' }}</p>
      </div>
    </div>

    <!-- 对话区 -->
    <div class="ai-chat">
      <div class="ai-header">
        <div class="ai-header-info">
          <div class="ai-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3l1.6 4.2L18 8.8l-4.4 1.6L12 14.6l-1.6-4.2L6 8.8l4.4-1.6L12 3z"/>
              <path d="M19 14l.7 1.8L21.5 16.5l-1.8.7L19 19l-.7-1.8L16.5 16.5l1.8-.7L19 14z"/>
              <path d="M5 14l.7 1.8L7.5 16.5l-1.8.7L5 19l-.7-1.8L2.5 16.5l1.8-.7L5 14z"/>
            </svg>
          </div>
          <div class="ai-header-text">
            <h3>{{ assistant.title }}</h3>
            <p v-if="assistant.describe">{{ assistant.describe }}</p>
          </div>
        </div>
      </div>

      <!-- 消息区域 -->
      <div ref="messagesContainer" class="ai-messages">
        <!-- 空状态：欢迎语 + 示例问题 -->
        <div v-if="messages.length === 0" class="ai-welcome">
          <div class="welcome-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M12 3l1.6 4.2L18 8.8l-4.4 1.6L12 14.6l-1.6-4.2L6 8.8l4.4-1.6L12 3z"/>
              <path d="M19 14l.7 1.8L21.5 16.5l-1.8.7L19 19l-.7-1.8L16.5 16.5l1.8-.7L19 14z"/>
              <path d="M5 14l.7 1.8L7.5 16.5l-1.8.7L5 19l-.7-1.8L2.5 16.5l1.8-.7L5 14z"/>
            </svg>
          </div>
          <h4>你好，我是{{ assistant.title }}</h4>
          <p class="welcome-desc">可以向我提问网站文章相关的问题，或随便聊聊。</p>

          <div v-if="examples.length" class="examples">
            <button
              v-for="(ex, i) in examples"
              :key="i"
              class="example-card"
              @click="send(ex.title)"
            >
              <span class="example-title">{{ ex.title }}</span>
              <span v-if="ex.describe" class="example-desc">{{ ex.describe }}</span>
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <template v-for="msg in messages" :key="msg.id">
          <div class="msg-item" :class="msg.type === 'USER' ? 'user' : 'assistant'">
            <div v-if="msg.type === 'ASSISTANT'" class="msg-avatar ai-avatar">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 3l1.6 4.2L18 8.8l-4.4 1.6L12 14.6l-1.6-4.2L6 8.8l4.4-1.6L12 3z"/>
              </svg>
            </div>
            <div class="msg-main">
              <div class="msg-bubble">
                <span v-if="msg.type === 'USER'" class="msg-text">{{ msg.content }}</span>
                <div v-else class="ai-md" v-html="md(msg.content)"></div>
                <span v-if="msg.streaming && !msg.content" class="typing">
                  <i></i><i></i><i></i>
                </span>
              </div>

              <!-- 工具返回的文章/随笔推荐 -->
              <div
                v-if="msg.type === 'ASSISTANT' && extractResults(msg.params).length"
                class="result-cards"
              >
                <div
                  v-for="r in extractResults(msg.params)"
                  :key="r.id"
                  class="result-card"
                  @click="goResult(r)"
                >
                  <div class="result-type">{{ r.type === 'article' ? '文章' : '随笔' }}</div>
                  <div class="result-title">{{ r.title }}</div>
                  <div v-if="r.summary" class="result-summary">{{ r.summary }}</div>
                  <div v-if="r.authorName" class="result-author">{{ r.authorName }}</div>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- 输入区域 -->
      <div class="ai-input-area">
        <textarea
          v-model="input"
          class="ai-input"
          placeholder="输入你的问题..."
          rows="1"
          @keydown="handleKeydown"
        ></textarea>
        <button
          v-if="streaming"
          class="stop-btn"
          @click="handleStop"
          title="停止生成"
        >
          <svg viewBox="0 0 24 24" fill="currentColor" stroke="none">
            <rect x="6" y="6" width="12" height="12" rx="1"/>
          </svg>
        </button>
        <button
          v-else
          class="send-btn"
          :disabled="!input.trim()"
          @click="send()"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M22 2L11 13" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M22 2L15 22L11 13L2 9L22 2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.ai-page {
  display: flex;
  height: calc(100vh - 124px);
  padding-top: 16px;
  box-sizing: border-box;
  overflow: hidden;
}

/* 侧边栏 */
.ai-sidebar {
  width: 280px;
  border-right: 1px solid rgba(26, 26, 26, 0.08);
  background: var(--paper-card);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.sidebar-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.1em;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin: 16px 16px 8px;
  padding: 9px 14px;
  font-size: 14px;
  font-family: 'Noto Serif SC', serif;
  color: #f0ebe3;
  background: var(--ink);
  border: none;
  border-radius: 3px;
  cursor: pointer;
  transition: background-color 0.2s;
  letter-spacing: 0.05em;
}

.new-chat-btn:hover {
  background: var(--accent);
}

.new-chat-btn svg {
  width: 15px;
  height: 15px;
}

.history-list {
  flex: 1;
  overflow-y: auto;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: rgba(26, 26, 26, 0.04);
}

.history-item.active {
  background-color: rgba(26, 26, 26, 0.06);
}

.history-info {
  flex: 1;
  min-width: 0;
}

.history-title {
  font-size: 14px;
  color: var(--ink);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 3px;
}

.history-time {
  font-size: 12px;
  color: var(--ink-muted);
}

.history-delete {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  color: var(--ink-muted);
  cursor: pointer;
  border-radius: 3px;
  opacity: 0;
  transition: all 0.2s;
  flex-shrink: 0;
}

.history-item:hover .history-delete {
  opacity: 1;
}

.history-delete:hover {
  color: var(--seal-red);
  background: rgba(26, 26, 26, 0.05);
}

.history-delete svg {
  width: 15px;
  height: 15px;
}

.history-empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-muted);
}

.history-empty p {
  font-size: 13px;
  letter-spacing: 0.02em;
}

/* 对话区 */
.ai-chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--paper);
}

.ai-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
  background: var(--paper-card);
}

.ai-header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.ai-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--ink);
  color: #f0ebe3;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.ai-avatar svg {
  width: 22px;
  height: 22px;
}

.ai-header-text h3 {
  margin: 0 0 2px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 0.05em;
}

.ai-header-text p {
  margin: 0;
  font-size: 13px;
  color: var(--ink-muted);
  letter-spacing: 0.02em;
}

/* 消息区 */
.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.msg-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.msg-item.user {
  justify-content: flex-end;
}

.msg-item.user .msg-main {
  align-items: flex-end;
}

.msg-avatar {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.msg-avatar svg {
  width: 16px;
  height: 16px;
}

.msg-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 78%;
  min-width: 0;
}

.msg-bubble {
  padding: 10px 16px;
  border-radius: 4px;
  line-height: 1.7;
  word-break: break-word;
}

.msg-item.assistant .msg-bubble {
  background: var(--paper);
  border: 1px solid rgba(26, 26, 26, 0.08);
}

.msg-item.user .msg-bubble {
  background: var(--ink);
  color: #f0ebe3;
}

.msg-text {
  font-size: 15px;
  white-space: pre-wrap;
  color: inherit;
}

/* Markdown 渲染 */
.ai-md {
  font-size: 15px;
  color: var(--ink-light);
}

.ai-md :deep(p) {
  margin: 0 0 10px 0;
}

.ai-md :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-md :deep(h1),
.ai-md :deep(h2),
.ai-md :deep(h3),
.ai-md :deep(h4) {
  margin: 14px 0 8px 0;
  font-weight: 600;
  color: var(--ink);
}

.ai-md :deep(h1) { font-size: 20px; }
.ai-md :deep(h2) { font-size: 18px; }
.ai-md :deep(h3) { font-size: 16px; }
.ai-md :deep(h4) { font-size: 15px; }

.ai-md :deep(ul),
.ai-md :deep(ol) {
  margin: 8px 0;
  padding-left: 22px;
}

.ai-md :deep(li) {
  margin-bottom: 4px;
}

.ai-md :deep(code) {
  background: rgba(74, 74, 74, 0.08);
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.9em;
  font-family: 'Consolas', 'Monaco', monospace;
}

.ai-md :deep(pre) {
  background: rgba(26, 26, 26, 0.95);
  border-radius: 4px;
  padding: 12px 16px;
  overflow-x: auto;
  margin: 10px 0;
}

.ai-md :deep(pre code) {
  background: transparent;
  padding: 0;
  color: #e4e4e4;
  font-size: 13px;
  line-height: 1.6;
}

.ai-md :deep(blockquote) {
  border-left: 3px solid var(--ink-light);
  padding-left: 12px;
  margin: 10px 0;
  color: var(--ink-muted);
}

.ai-md :deep(a) {
  color: var(--ink);
  text-decoration: underline;
}

/* 打字指示 */
.typing {
  display: inline-flex;
  gap: 4px;
  align-items: center;
  padding: 4px 0;
}

.typing i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--ink-muted);
  animation: blink 1.4s infinite ease-in-out both;
}

.typing i:nth-child(2) { animation-delay: 0.2s; }
.typing i:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0.2; }
  40% { opacity: 1; }
}

/* 工具返回的推荐卡片 */
.result-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.result-card {
  border: 1px solid rgba(26, 26, 26, 0.1);
  border-radius: 4px;
  padding: 12px 16px;
  cursor: pointer;
  background: var(--paper);
  transition: all 0.2s ease;
}

.result-card:hover {
  border-color: var(--ink);
  background: rgba(255, 253, 248, 0.8);
}

.result-type {
  display: inline-block;
  font-size: 12px;
  color: var(--seal-red);
  border: 1px solid var(--seal-red);
  border-radius: 2px;
  padding: 1px 8px;
  margin-bottom: 6px;
}

.result-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
  margin-bottom: 4px;
}

.result-summary {
  font-size: 13px;
  color: var(--ink-light);
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-author {
  font-size: 12px;
  color: var(--ink-muted);
  margin-top: 4px;
}

/* 欢迎页 */
.ai-welcome {
  margin: auto;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.welcome-icon {
  width: 64px;
  height: 64px;
  color: var(--ink);
  opacity: 0.8;
  margin-bottom: 20px;
}

.welcome-icon svg {
  width: 100%;
  height: 100%;
}

.ai-welcome h4 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 0.05em;
}

.welcome-desc {
  margin: 0 0 28px 0;
  font-size: 14px;
  color: var(--ink-muted);
  letter-spacing: 0.02em;
}

.examples {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: center;
}

.example-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 14px 22px;
  background: var(--paper);
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.example-card:hover {
  border-color: var(--ink);
  transform: translateY(-1px);
}

.example-title {
  font-size: 15px;
  color: var(--ink);
  letter-spacing: 0.05em;
}

.example-desc {
  font-size: 12px;
  color: var(--ink-muted);
}

/* 输入区 */
.ai-input-area {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid rgba(26, 26, 26, 0.08);
  background: var(--paper-card);
  align-items: flex-end;
}

.ai-input {
  flex: 1;
  min-height: 40px;
  max-height: 140px;
  padding: 10px 14px;
  background: rgba(255, 252, 247, 0.8);
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 4px;
  font-size: 15px;
  font-family: 'Noto Serif SC', serif;
  color: var(--ink);
  resize: none;
  outline: none;
  transition: border-color 0.2s;
}

.ai-input:focus {
  border-color: var(--ink);
}

.ai-input::placeholder {
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

.stop-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--seal-red);
  border: none;
  border-radius: 4px;
  color: #fff;
  cursor: pointer;
  flex-shrink: 0;
  transition: background-color 0.2s;
}

.stop-btn:hover {
  opacity: 0.85;
}

.stop-btn svg {
  width: 16px;
  height: 16px;
}
</style>