﻿<script setup>
import { ref, onMounted, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createEssay, getEssayDetail, updateEssay } from '@/api/essay'
import { getTagList, createTag } from '@/api/tag'
import { InkMessage } from '@/utils/message'
import { redirectToLogin } from '@/utils/auth'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const content = ref('')
const tagIds = ref([])
const tags = ref([])
const loading = ref(false)
const isEditMode = computed(() => !!route.params.id)
const essayId = computed(() => route.params.id)
const editorRef = ref(null)
const currentAlign = ref('left')

const alignButtons = [
  { value: 'left', label: '居左', icon: 'align-left' },
  { value: 'center', label: '居中', icon: 'align-center' },
  { value: 'right', label: '居右', icon: 'align-right' },
  { value: 'justify', label: '两端', icon: 'align-justify' },
]

onMounted(async () => {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录')
    return
  }

  await loadTags()

  if (isEditMode.value) {
    await loadEssay()
  }
})

onBeforeUnmount(() => {
  if (editorRef.value) {
    content.value = editorRef.value.innerHTML
  }
})

async function loadTags() {
  try {
    tags.value = await getTagList() || []
  } catch (e) {
    console.error('加载标签失败', e)
  }
}

async function loadEssay() {
  loading.value = true
  try {
    const data = await getEssayDetail(essayId.value)
    if (editorRef.value) {
      editorRef.value.innerHTML = data.content || ''
    }
    content.value = data.content || ''
    if (data.tagVOList) {
      tagIds.value = data.tagVOList.map(t => t.id)
    }
  } catch (e) {
    InkMessage.error('加载随笔失败')
    router.push({ name: 'EssayHome' })
  } finally {
    loading.value = false
  }
}

function handleAlign(align) {
  if (!editorRef.value) return
  editorRef.value.focus()
  
  let command = 'justifyLeft'
  switch (align) {
    case 'center':
      command = 'justifyCenter'
      break
    case 'right':
      command = 'justifyRight'
      break
    case 'justify':
      command = 'justifyFull'
      break
    default:
      command = 'justifyLeft'
  }
  document.execCommand(command, false, null)
  currentAlign.value = align
}

function handleInput() {
  if (editorRef.value) {
    content.value = editorRef.value.innerHTML
  }
}

function handleKeyUp() {
  updateCurrentAlign()
}

function handleMouseUp() {
  updateCurrentAlign()
}

function updateCurrentAlign() {
  const selection = window.getSelection()
  if (selection.rangeCount > 0) {
    const anchorNode = selection.anchorNode
    if (anchorNode && editorRef.value) {
      let element = anchorNode.parentElement
      while (element && element !== editorRef.value) {
        const align = element.style.textAlign
        if (align) {
          currentAlign.value = align
          return
        }
        element = element.parentElement
      }
      currentAlign.value = 'left'
    }
  }
}

async function handleTagChange(newTagIds) {
  const newTagNames = newTagIds.filter(id => typeof id === 'string' && !tags.value.some(t => t.name === id))

  for (const tagName of newTagNames) {
    try {
      const newTag = await createTag(tagName)
      tags.value.push(newTag)
      const index = tagIds.value.indexOf(tagName)
      if (index !== -1) {
        tagIds.value[index] = newTag.id
      }
      InkMessage.success(`标签 "${tagName}" 创建成功`)
    } catch (e) {
      console.error('创建标签失败', e)
      InkMessage.error(`创建标签 "${tagName}" 失败`)
      const index = tagIds.value.indexOf(tagName)
      if (index !== -1) {
        tagIds.value.splice(index, 1)
      }
    }
  }
}

async function handleSubmit() {
  if (!content.value.trim() || content.value === '<br>') {
    InkMessage.warning('请输入随笔内容')
    return
  }

  loading.value = true
  try {
    const data = {
      content: content.value.trim(),
      tagIdList: tagIds.value,
    }
    if (isEditMode.value) {
      await updateEssay(essayId.value, data)
    } else {
      await createEssay(data)
    }
    InkMessage.success(isEditMode.value ? '修改成功' : '发布成功')
    router.push({ name: 'EssayHome' })
  } catch (e) {
    InkMessage.error(isEditMode.value ? '修改失败' : '发布失败')
  } finally {
    loading.value = false
  }
}

function handleCancel() {
  router.push({ name: 'EssayHome' })
}
</script>

<template>
  <div class="essay-editor">
    <div class="editor-header">
      <h1 class="editor-title">{{ isEditMode ? '编辑随笔' : '写随笔' }}</h1>
    </div>

    <div class="editor-content">
      <div class="editor-toolbar">
        <div class="align-group">
          <button
            v-for="btn in alignButtons"
            :key="btn.value"
            class="toolbar-btn"
            :class="{ active: currentAlign === btn.value }"
            :title="btn.label"
            @click="handleAlign(btn.value)"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <template v-if="btn.icon === 'align-left'">
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="15" y2="12" />
                <line x1="3" y1="18" x2="18" y2="18" />
              </template>
              <template v-else-if="btn.icon === 'align-center'">
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="6" y1="12" x2="18" y2="12" />
                <line x1="4" y1="18" x2="20" y2="18" />
              </template>
              <template v-else-if="btn.icon === 'align-right'">
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="9" y1="12" x2="21" y2="12" />
                <line x1="6" y1="18" x2="21" y2="18" />
              </template>
              <template v-else-if="btn.icon === 'align-justify'">
                <line x1="3" y1="6" x2="21" y2="6" />
                <line x1="3" y1="12" x2="21" y2="12" />
                <line x1="3" y1="18" x2="21" y2="18" />
              </template>
            </svg>
          </button>
        </div>
      </div>

      <div class="content-section">
        <div
          ref="editorRef"
          class="content-editor"
          contenteditable="true"
          :disabled="loading"
          @input="handleInput"
          @keyup="handleKeyUp"
          @mouseup="handleMouseUp"
          data-placeholder="此刻的想法..."
        ></div>
      </div>

      <div class="tags-section">
        <el-select v-model="tagIds"
          mode="multiple"
          showSearch
          allow-create
          defaultActiveFirstOption
          :reserve-keyword="false"
          placeholder="请选择或输入新标签"
          class="tag-select"
          @change="handleTagChange"
        >
          <el-option
            v-for="item in tags"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <div class="tag-tip">可输入新标签名，回车后自动创建并保存</div>
      </div>

      <div class="actions">
        <button class="cancel-btn" @click="handleCancel" :disabled="loading">
          取消
        </button>
        <button class="submit-btn" @click="handleSubmit" :disabled="loading || !content.trim()">
          {{ loading ? '提交中...' : (isEditMode ? '保存修改' : '发布随笔') }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.essay-editor {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 20px;
}

.editor-header {
  padding: 40px 0 24px;
  text-align: center;
}

.editor-title {
  font-size: 28px;
  font-weight: 400;
  color: var(--ink);
  margin: 0;
  font-family: 'Noto Serif SC', 'ZCOOL XiaoWei', serif;
  letter-spacing: 0.1em;
}

.editor-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.editor-toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: rgba(255, 252, 247, 0.5);
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 8px 8px 0 0;
  border-bottom: none;
}

.align-group {
  display: flex;
  gap: 4px;
}

.toolbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--ink-muted);
  cursor: pointer;
  transition: all 0.2s;
}

.toolbar-btn svg {
  width: 18px;
  height: 18px;
}

.toolbar-btn:hover {
  background: rgba(26, 26, 26, 0.06);
  color: var(--ink);
}

.toolbar-btn.active {
  background: var(--ink);
  color: #fff;
}

.content-section {
  margin-bottom: 24px;
}

.content-editor {
  width: 100%;
  min-height: 300px;
  padding: 16px;
  font-size: 16px;
  line-height: 2;
  color: var(--ink);
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 0 0 8px 8px;
  background: rgba(255, 252, 247, 0.5);
  box-sizing: border-box;
  font-family: 'Noto Serif SC', 'ZCOOL XiaoWei', serif;
  letter-spacing: 0.02em;
  transition: border-color 0.2s;
  outline: none;
}

.content-editor:focus {
  border-color: var(--ink);
}

.content-editor:empty::before {
  content: attr(data-placeholder);
  color: var(--ink-muted);
  font-style: italic;
  pointer-events: none;
}

.content-editor:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.content-editor:deep([style*="text-align: center"]) {
  text-align: center;
}

.content-editor:deep([style*="text-align: right"]) {
  text-align: right;
}

.content-editor:deep([style*="text-align: justify"]) {
  text-align: justify;
}

.tags-section {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.tag-select {
  width: 100%;
}

.tag-select :deep(.el-select__tags) {
  max-height: 100px;
  overflow-y: auto;
}

.tag-select :deep(.el-tag) {
  background: rgba(26, 26, 26, 0.06);
  border: none;
  color: var(--ink-light);
  font-size: 13px;
  padding: 4px 10px;
  border-radius: 14px;
}

.tag-select :deep(.el-tag__close) {
  color: var(--ink-muted);
}

.tag-select :deep(.el-tag__close:hover) {
  color: #e74c3c;
}

.tag-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--ink-muted);
  letter-spacing: 0.05em;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.cancel-btn {
  padding: 10px 24px;
  font-size: 14px;
  border: 1px solid rgba(26, 26, 26, 0.15);
  border-radius: 20px;
  background: transparent;
  color: var(--ink-light);
  cursor: pointer;
  transition: all 0.2s;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.05em;
}

.cancel-btn:hover:not(:disabled) {
  border-color: var(--ink);
  color: var(--ink);
}

.cancel-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.submit-btn {
  padding: 10px 28px;
  font-size: 14px;
  border: none;
  border-radius: 20px;
  background: var(--ink);
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.05em;
}

.submit-btn:hover:not(:disabled) {
  background: #333;
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>
