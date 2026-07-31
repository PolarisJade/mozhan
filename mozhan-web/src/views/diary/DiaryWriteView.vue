<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createDiary, getDiaryDetail, updateDiary } from '@/api/diary'
import { uploadImage } from '@/api/upload'
import { useUserStore } from '@/stores/user'
import { redirectToLogin } from '@/utils/auth'
import { InkMessage } from '@/utils/message'
import WeatherIcon from '@/components/WeatherIcon.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const isEditMode = computed(() => !!route.params.id)
const diaryId = computed(() => route.params.id)
const weatherOptions = ['晴', '多云', '阴', '小雨', '大雨', '雷阵雨', '雪']

const editorRef = ref(null)
const imageInput = ref(null)
const hasContent = ref(false)

function todayStr() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const form = reactive({
  diaryDate: todayStr(),
  weather: '晴',
})

const canSubmit = computed(() => {
  return form.diaryDate && form.weather && hasContent.value
})

function disableFuture(date) {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date.getTime() > today.getTime()
}

async function loadDiary() {
  loading.value = true
  try {
    const data = await getDiaryDetail(diaryId.value)
    form.diaryDate = data.diaryDate || todayStr()
    form.weather = data.weather || '晴'
    await nextTick()
    if (editorRef.value) {
      editorRef.value.innerHTML = data.content || ''
      updateHasContent()
    }
  } catch (e) {
    InkMessage.error('加载日记失败')
    router.push({ name: 'DiaryHome' })
  } finally {
    loading.value = false
  }
}

// ===== 富文本编辑器相关 =====
function updateHasContent() {
  if (editorRef.value) {
    const html = editorRef.value.innerHTML
    // 过滤掉纯 <br> / <div><br></div> 等空内容
    const text = editorRef.value.innerText.replace(/\u200b/g, '').trim()
    hasContent.value = text.length > 0 || /<img/i.test(html)
  }
}

function saveSelection() {
  const selection = window.getSelection()
  if (selection.rangeCount > 0) {
    const range = selection.getRangeAt(0)
    const container = range.commonAncestorContainer
    const editor = editorRef.value
    if (editor && (container === editor || editor.contains(container))) {
      const rangeCopy = document.createRange()
      rangeCopy.setStart(range.startContainer, range.startOffset)
      rangeCopy.setEnd(range.endContainer, range.endOffset)
      return rangeCopy
    }
  }
  return null
}

function execCommand(command, value = null) {
  const editor = editorRef.value
  if (!editor) return
  const savedRange = saveSelection()
  editor.focus()
  if (savedRange) {
    const selection = window.getSelection()
    selection.removeAllRanges()
    selection.addRange(savedRange)
  }
  document.execCommand(command, false, value)
  updateHasContent()
}

function handleKeyDown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    e.stopPropagation()
    const editor = editorRef.value
    if (!editor) return
    editor.focus()
    document.execCommand('insertLineBreak', false)
    updateHasContent()
  }
}

function handleInput() {
  updateHasContent()
}

function handlePaste(e) {
  // 优先检测剪贴板中的图片
  const items = e.clipboardData?.items
  if (items) {
    for (const item of items) {
      if (item.type.startsWith('image/')) {
        e.preventDefault()
        const file = item.getAsFile()
        if (file) uploadAndInsertImage(file)
        return
      }
    }
  }

  e.preventDefault()
  let text = e.clipboardData.getData('text/html') || e.clipboardData.getData('text/plain')
  if (!text || !text.trim()) return

  // 粘贴 HTML 时若含图片标签，直接作为 HTML 插入
  const isHtml = text.includes('<') && (text.includes('<p') || text.includes('<img') || text.includes('<div') || text.includes('<br'))
  if (isHtml && !text.includes('@font-face') && !text.includes('mso-')) {
    const editor = editorRef.value
    if (editor) {
      editor.focus()
      document.execCommand('insertHTML', false, text)
      updateHasContent()
    }
    return
  }

  // 纯文本处理：按段落包裹 <p>
  text = text.replace(/\r\n/g, '\n')
  const paragraphs = text.split(/\n{2,}/)
  const processedParagraphs = paragraphs.map(p => {
    const lines = p.split('\n').filter(line => line.trim())
    return lines.join('<br>')
  }).filter(p => p.trim())
  text = processedParagraphs.map(p => `<p>${p}</p>`).join('\n')

  const editor = editorRef.value
  if (editor) {
    editor.focus()
    document.execCommand('insertHTML', false, text)
    updateHasContent()
  }
}

// ===== 图片上传相关 =====
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif']
const MAX_IMAGE_SIZE = 5 * 1024 * 1024

function validateImageFile(file) {
  if (!file) return false
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    InkMessage.error('仅支持 jpg/jpeg/png/gif 格式图片')
    return false
  }
  if (file.size > MAX_IMAGE_SIZE) {
    InkMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

async function uploadAndInsertImage(file) {
  if (!validateImageFile(file)) return
  const hide = InkMessage.loading('图片上传中...', 0)
  try {
    const data = await uploadImage(file)
    const imgHtml = `<img src="${data.url}" alt="${data.fileName || ''}" />`
    const editor = editorRef.value
    if (editor) {
      editor.focus()
      document.execCommand('insertHTML', false, imgHtml)
      updateHasContent()
    }
    InkMessage.success('图片上传成功')
  } catch (e) {
    console.error('图片上传失败', e)
  } finally {
    hide?.()
  }
}

function triggerImageSelect() {
  if (imageInput.value) {
    imageInput.value.value = ''
    imageInput.value.click()
  }
}

function handleImageSelect(e) {
  const file = e.target.files?.[0]
  if (file) uploadAndInsertImage(file)
}

function handleEditorDrop(e) {
  const files = e.dataTransfer?.files
  if (!files || files.length === 0) return
  const file = Array.from(files).find(f => f.type.startsWith('image/'))
  if (file) uploadAndInsertImage(file)
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  const content = editorRef.value?.innerHTML || ''
  if (!content.trim()) {
    InkMessage.error('请输入日记内容')
    return
  }
  submitting.value = true
  try {
    const payload = {
      diaryDate: form.diaryDate,
      weather: form.weather,
      content: content,
    }
    if (isEditMode.value) {
      payload.id = Number(diaryId.value)
      await updateDiary(payload)
    } else {
      await createDiary(payload)
    }
    InkMessage.success(isEditMode.value ? '修改成功' : '已记下今日')
    router.push({ name: 'DiaryHome' })
  } catch (e) {
    InkMessage.error(isEditMode.value ? '修改失败' : '保存失败')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push({ name: 'DiaryHome' })
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后再写日记')
    return
  }
  if (isEditMode.value) {
    loadDiary()
  }
})
</script>

<template>
  <div class="diary-editor">
    <div class="editor-content" v-loading="loading">
      <h2 class="editor-page-title">{{ isEditMode ? '修改日记' : '写日记' }}</h2>
      <div class="meta-row">
        <div class="meta-item date-item">
          <label class="meta-label">日期</label>
          <el-date-picker
            v-model="form.diaryDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disableFuture"
            placeholder="选择日期"
            class="date-picker"
          />
        </div>
        <div class="meta-item weather-item">
          <label class="meta-label">天气</label>
          <div class="weather-options">
            <button
              v-for="w in weatherOptions"
              :key="w"
              type="button"
              class="weather-option"
              :class="{ active: form.weather === w }"
              @click="form.weather = w"
            >
              <WeatherIcon :type="w" />
              <span>{{ w }}</span>
            </button>
          </div>
        </div>
      </div>

      <div class="content-section">
        <div class="content-label-row">
          <label class="meta-label">正文</label>
          <button type="button" class="insert-image-btn" @click="triggerImageSelect" :disabled="submitting">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
            插入图片
          </button>
        </div>
        <div
          ref="editorRef"
          class="content-input"
          contenteditable="true"
          placeholder="今天发生了什么，心情如何……"
          @blur="saveSelection"
          @paste="handlePaste"
          @input="handleInput"
          @keydown="handleKeyDown"
          @dragover.prevent
          @drop.prevent="handleEditorDrop"
        ></div>
      </div>

      <div class="actions">
        <button class="cancel-btn" @click="handleCancel" :disabled="submitting">取消</button>
        <button class="submit-btn" @click="handleSubmit" :disabled="submitting || !canSubmit">
          {{ submitting ? '保存中...' : (isEditMode ? '保存修改' : '保存日记') }}
        </button>
      </div>

      <input
        ref="imageInput"
        type="file"
        accept="image/jpeg,image/png,image/gif"
        hidden
        @change="handleImageSelect"
      />
    </div>
  </div>
</template>

<style scoped>
.diary-editor {
  max-width: 980px;
  margin: 0 auto 0;
  padding: 0 20px;
}

.editor-content {
  background: var(--paper-card);
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 4px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(26, 26, 26, 0.05);
}

.editor-page-title {
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.1em;
  margin: 0 0 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.meta-row {
  display: flex;
  gap: 40px;
  margin-bottom: 24px;
  align-items: flex-start;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.date-item {
  flex-shrink: 0;
}

.weather-item {
  flex: 1;
  min-width: 0;
}

.meta-label {
  font-size: 13px;
  color: var(--ink-light);
  letter-spacing: 0.1em;
  font-weight: 500;
}

.date-picker {
  width: 200px;
}

.date-picker :deep(.el-input__wrapper) {
  border-radius: 2px;
  background: rgba(255, 252, 247, 0.6);
}

.weather-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.weather-option {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  background: rgba(255, 252, 247, 0.6);
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 2px;
  color: var(--ink-light);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.03em;
}

.weather-option :deep(.weather-icon) {
  width: 16px;
  height: 16px;
  font-size: 16px;
}

.weather-option:hover {
  border-color: rgba(26, 26, 26, 0.3);
  color: var(--ink);
}

.weather-option.active {
  background: var(--ink);
  border-color: var(--ink);
  color: #f0ebe3;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 28px;
}

.content-label-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.insert-image-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 5px 12px;
  font-size: 13px;
  font-family: 'Noto Serif SC', serif;
  color: var(--ink-light);
  background: transparent;
  border: 1px solid rgba(26, 26, 26, 0.15);
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.2s ease;
  letter-spacing: 0.05em;
}

.insert-image-btn svg {
  width: 14px;
  height: 14px;
}

.insert-image-btn:hover:not(:disabled) {
  border-color: var(--ink);
  color: var(--ink);
  background: rgba(26, 26, 26, 0.04);
}

.insert-image-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.content-input {
  width: 100%;
  height: calc(100vh - 460px);
  min-height: 160px;
  max-height: 420px;
  padding: 14px 16px;
  font-size: 15px;
  line-height: 1.9;
  color: var(--ink);
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 2px;
  background: rgba(255, 252, 247, 0.5);
  box-sizing: border-box;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.02em;
  overflow-y: auto;
  outline: none;
  transition: border-color 0.2s ease;
  text-align: left;
}

.content-input::placeholder {
  color: var(--ink-muted);
  font-style: italic;
}

.content-input:empty:before {
  content: attr(placeholder);
  color: var(--ink-muted);
  font-style: italic;
  pointer-events: none;
}

.content-input:focus {
  border-color: var(--ink);
  background: rgba(255, 252, 247, 0.8);
}

.content-input :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 12px 0;
  display: block;
}

.content-input :deep(p) {
  margin: 0 0 0.6em;
  line-height: 1.8;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid rgba(26, 26, 26, 0.06);
}

.cancel-btn {
  padding: 9px 24px;
  font-size: 14px;
  border: 1px solid rgba(26, 26, 26, 0.18);
  border-radius: 2px;
  background: transparent;
  color: var(--ink-light);
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.08em;
}

.cancel-btn:hover:not(:disabled) {
  border-color: var(--ink);
  color: var(--ink);
}

.cancel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-btn {
  padding: 9px 28px;
  font-size: 14px;
  border: none;
  border-radius: 2px;
  background: var(--ink);
  color: #f0ebe3;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.08em;
}

.submit-btn:hover:not(:disabled) {
  background: var(--accent);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 640px) {
  .meta-row {
    flex-direction: column;
    gap: 18px;
  }
  .date-picker {
    width: 100%;
  }
  .editor-content {
    padding: 20px 16px;
  }
}
</style>
