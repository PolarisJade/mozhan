﻿﻿﻿<script setup>
import { onMounted, ref, reactive, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { InkMessage } from '@/utils/message'
import 'vditor/dist/index.css'
import { createArticle, updateArticle, getArticleDetail, getArticleInfo, publishArticle } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList, createTag } from '@/api/tag'
import { useUserStore } from '@/stores/user'
import { redirectToLogin } from '@/utils/auth'

const router = useRouter()
const userStore = useUserStore()

const isEdit = ref(false)
const articleId = ref(null)
const loading = ref(false)
const submitting = ref(false)
const categories = ref([])
const tags = ref([])
const editorRef = ref(null)
let vditor = null

const form = reactive({
  title: '',
  summary: '',
  content: '',
  categoryId: null,
  tagIds: [],
})

const formRef = ref(null)

const rules = {
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { min: 2, max: 100, message: '标题长度为 2-100 字', trigger: 'blur' },
  ],
  categoryId: [
    { required: true, message: '请选择文章分类', trigger: 'change' },
  ],
}

async function loadCategoriesAndTags() {
  try {
    categories.value = await getCategoryList() || []
    tags.value = await getTagList() || []
  } catch (e) {
    console.error('加载分类/标签失败', e)
  }
}

async function handleTagChange(newTagIds) {
  // 检查是否有新创建的标签（字符串类型，不是数字ID）
  const newTagNames = newTagIds.filter(id => typeof id === 'string' && !tags.value.some(t => t.name === id))
  
  for (const tagName of newTagNames) {
    try {
      const newTag = await createTag(tagName)
      // 将新标签添加到列表
      tags.value.push(newTag)
      // 将字符串标签名替换为新创建的标签ID
      const index = form.tagIds.indexOf(tagName)
      if (index !== -1) {
        form.tagIds[index] = newTag.id
      }
      InkMessage.success(`标签 "${tagName}" 创建成功`)
    } catch (e) {
      console.error('创建标签失败', e)
      InkMessage.error(`创建标签 "${tagName}" 失败`)
      // 移除失败的标签
      const index = form.tagIds.indexOf(tagName)
      if (index !== -1) {
        form.tagIds.splice(index, 1)
      }
    }
  }
}

async function loadArticle() {
  if (!articleId.value) return
  loading.value = true
  try {
    const data = await getArticleInfo(articleId.value)
    form.title = data.title || ''
    form.summary = data.summary || ''
    form.content = data.content || ''
    form.categoryId = data.categoryId || null
    form.tagIds = data.tags?.map(t => t.id) || []
    
    const editor = document.querySelector('.rich-editor')
    if (editor && data.content) {
      editor.innerHTML = data.content
    }
  } finally {
    loading.value = false
  }
}

function saveSelection() {
  const selection = window.getSelection()
  if (selection.rangeCount > 0) {
    const range = selection.getRangeAt(0)
    const container = range.commonAncestorContainer
    const editor = document.querySelector('.rich-editor')
    
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
  const editor = document.querySelector('.rich-editor')
  if (!editor) return
  
  const savedRange = saveSelection()
  
  editor.focus()
  
  if (savedRange) {
    const selection = window.getSelection()
    selection.removeAllRanges()
    selection.addRange(savedRange)
  }
  
  document.execCommand(command, false, value)
}

function handleKeyDown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    e.stopPropagation()
    
    const editor = document.querySelector('.rich-editor')
    if (!editor) return
    
    editor.focus()
    document.execCommand('insertLineBreak', false)
  }
}

function handlePaste(e) {
  e.preventDefault()
  
  let text = e.clipboardData.getData('text/html') || e.clipboardData.getData('text/plain')
  
  if (text) {
    const containsWordStyles = text.includes('@font-face') || text.includes('mso-') || text.includes('<!DOCTYPE') || text.includes('<html')
    
    if (containsWordStyles) {
      text = e.clipboardData.getData('text/plain') || ''
    }
    
    if (!text.trim()) return
    
    text = text.replace(/\r\n/g, '\n')
    
    const paragraphs = text.split(/\n{2,}/)
    
    const processedParagraphs = paragraphs.map(p => {
      const lines = p.split('\n').filter(line => line.trim())
      return lines.join('<br>')
    }).filter(p => p.trim())
    
    text = processedParagraphs.map(p => `<p>${p}</p>`).join('\n')
    
    const editor = document.querySelector('.rich-editor')
    if (editor) {
      editor.focus()
      document.execCommand('insertHTML', false, text)
    }
  }
}

const toolbarButtons = [
  { label: 'H1', action: () => execCommand('formatBlock', 'h1') },
  { label: 'H2', action: () => execCommand('formatBlock', 'h2') },
  { label: 'H3', action: () => execCommand('formatBlock', 'h3') },
  { label: '粗体', action: () => execCommand('bold') },
  { label: '斜体', action: () => execCommand('italic') },
  { label: '下划线', action: () => execCommand('underline') },
  { label: '删除线', action: () => execCommand('strikeThrough') },
  { label: '引用', action: () => execCommand('formatBlock', 'blockquote') },
  { label: '代码', action: () => execCommand('code') },
  { label: '无序列表', action: () => execCommand('insertUnorderedList') },
  { label: '有序列表', action: () => execCommand('insertOrderedList') },
  { label: '左对齐', action: () => execCommand('justifyLeft') },
  { label: '居中', action: () => execCommand('justifyCenter') },
  { label: '右对齐', action: () => execCommand('justifyRight') },
]

async function handleSubmit(publish = false) {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后再写文章')
    return
  }

  await formRef.value.validate()
  
  const editor = document.querySelector('.rich-editor')
  const content = editor ? editor.innerHTML : ''
  if (!content.trim()) {
    InkMessage.error('请输入文章内容')
    return
  }
  
  submitting.value = true
  try {
    const data = {
      title: form.title,
      summary: form.summary || form.title,
      content: content,
      categoryId: form.categoryId,
      tagIds: form.tagIds,
      status: publish ? '发布' : '草稿',
    }

    let result
    if (isEdit.value && articleId.value) {
      await updateArticle(articleId.value, data)
      result = { id: articleId.value }
      if (publish) {
        await publishArticle(articleId.value)
      }
      InkMessage.success(publish ? '文章已更新并发布' : '文章已保存')
    } else {
      result = await createArticle(data)
      if (publish && result.id) {
        await publishArticle(result.id)
      }
      InkMessage.success(publish ? '文章发布成功' : '文章已保存为草稿')
    }

    router.push({ name: 'ArticleDetail', params: { id: result.id } })
  } catch (e) {
    console.error('保存文章失败', e)
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后再写文章')
    return
  }
  
  loadCategoriesAndTags()
  
  const id = router.currentRoute.value.params.id
  if (id) {
    isEdit.value = true
    articleId.value = id
    setTimeout(() => loadArticle(), 300)
  }
})
</script>

<template>
  <div class="editor-page" v-loading="loading">
    <div class="editor-header">
      <h2 class="ink-page-title">{{ isEdit ? '编辑文章' : '撰写新文' }}</h2>
      <div class="header-actions">
        <el-button class="ink-btn-plain" @click="goBack">返回</el-button>
        <el-button class="ink-btn-plain" :loading="submitting" @click="handleSubmit(false)">
          保存草稿
        </el-button>
        <el-button class="ink-btn" :loading="submitting" @click="handleSubmit(true)">
          发布文章
        </el-button>
      </div>
    </div>

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-position="top"
      class="editor-form ink-card"
    >
      <el-form-item label="文章标题" prop="title">
        <el-input
          v-model="form.title"
          placeholder="请输入文章标题"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="文章摘要">
        <el-input
          v-model="form.summary"
          type="textarea"
          :rows="3"
          placeholder="可选，不填则自动截取内容前200字"
          maxlength="300"
          show-word-limit
        />
      </el-form-item>

      <el-row :gutter="24">
        <el-col :span="12">
          <el-form-item label="文章分类" prop="categoryId">
            <el-select
              v-model="form.categoryId"
              placeholder="请选择分类"
              style="width: 100%"
            >
              <el-option
                v-for="cat in categories"
                :key="cat.id"
                :label="cat.name"
                :value="cat.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="文章标签">
            <el-select
              v-model="form.tagIds"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="请选择或输入新标签"
              style="width: 100%"
              @change="handleTagChange"
            >
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
              />
            </el-select>
            <div class="tag-tip">可输入新标签名，回车后自动创建并保存</div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="editor-toolbar-wrapper">
      <div class="editor-toolbar">
        <button
          v-for="btn in toolbarButtons"
          :key="btn.label"
          type="button"
          class="toolbar-btn"
          @mousedown.prevent="btn.action"
          :title="btn.label"
        >
          {{ btn.label }}
        </button>
      </div>
    </div>

    <div
      ref="editorRef"
      class="rich-editor"
      contenteditable="true"
      placeholder="请开始撰写文章..."
      @blur="saveSelection"
      @paste="handlePaste"
      @keydown="handleKeyDown"
    ></div>
  </div>
</template>

<style scoped>
.editor-page {
  max-width: 900px;
  margin: 0 auto;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.editor-form {
  padding: 32px 36px;
}

.editor-form :deep(.el-form-item__label) {
  font-family: 'Noto Serif SC', 'ZCOOL XiaoWei', serif;
  color: var(--ink);
  font-size: 14px;
  letter-spacing: 0.05em;
}

.editor-form :deep(.el-input__wrapper),
.editor-form :deep(.el-textarea__inner) {
  background: rgba(255, 253, 248, 0.6);
  border: 1px solid rgba(74, 74, 74, 0.12);
  border-radius: 0;
  transition: all 0.25s ease;
}

.editor-form :deep(.el-input__wrapper:hover),
.editor-form :deep(.el-textarea__inner:hover) {
  border-color: rgba(74, 74, 74, 0.25);
}

.editor-form :deep(.el-input__wrapper:focus-within),
.editor-form :deep(.el-textarea__inner:focus) {
  border-color: var(--ink);
  background: rgba(255, 253, 248, 0.9);
}

.editor-form :deep(.el-input__inner),
.editor-form :deep(.el-textarea__inner) {
  color: var(--ink);
  font-size: 14px;
  line-height: 1.8;
}

.editor-form :deep(.el-textarea__inner) {
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.03em;
}

.tag-tip {
  font-size: 12px;
  color: var(--ink-muted);
  margin-top: 8px;
  letter-spacing: 0.02em;
}

.editor-toolbar-wrapper {
  position: sticky;
  top: 68px;
  z-index: 1000;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 12px 16px;
  background: rgb(255, 252, 247);
  border: 1px solid rgba(74, 74, 74, 0.12);
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.toolbar-btn {
  padding: 6px 12px;
  font-size: 13px;
  font-family: 'Noto Serif SC', serif;
  color: var(--ink-light);
  background: transparent;
  border: 1px solid rgba(74, 74, 74, 0.15);
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toolbar-btn:hover {
  background: rgba(74, 74, 74, 0.05);
  border-color: var(--ink-light);
  color: var(--ink);
}

.rich-editor {
  width: 100%;
  min-height: 450px;
  margin-top: 0;
  padding: 24px;
  font-family: 'Noto Serif SC', 'STSong', 'SimSun', serif;
  font-size: 15px;
  line-height: 2;
  letter-spacing: 0.03em;
  color: var(--ink);
  background: rgba(255, 253, 248, 0.95);
  border: 1px solid rgba(74, 74, 74, 0.12);
  border-radius: 4px;
  box-sizing: border-box;
  transition: border-color 0.25s ease;
  overflow-y: auto;
  text-align: left;
}

.rich-editor:focus {
  outline: none;
  border-color: var(--ink);
  background: rgba(255, 253, 248, 0.98);
}

.rich-editor:empty:before {
  content: attr(placeholder);
  color: var(--ink-muted);
  pointer-events: none;
}

.rich-editor :deep(h1),
.rich-editor :deep(h2),
.rich-editor :deep(h3) {
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-weight: 400;
  color: var(--ink);
  margin-top: 1.5em;
  margin-bottom: 0.5em;
}

.rich-editor :deep(h1) { font-size: 24px; letter-spacing: 0.1em; }
.rich-editor :deep(h2) { font-size: 20px; letter-spacing: 0.08em; }
.rich-editor :deep(h3) { font-size: 18px; letter-spacing: 0.05em; }

.rich-editor :deep(p) {
  margin-bottom: 0.6em;
  line-height: 1.8;
  margin-top: 0;
}

.rich-editor :deep(strong) {
  font-weight: 600;
}

.rich-editor :deep(blockquote) {
  border-left: 3px solid var(--ink-light);
  padding-left: 16px;
  margin: 16px 0;
  color: var(--ink-light);
  font-style: italic;
  background: rgba(74, 74, 74, 0.03);
  padding: 12px 16px;
}

.rich-editor :deep(code) {
  background: rgba(74, 74, 74, 0.08);
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 0.9em;
  font-family: 'Consolas', 'Monaco', monospace;
}

.rich-editor :deep(ul),
.rich-editor :deep(ol) {
  margin: 16px 0;
  padding-left: 28px;
}

.rich-editor :deep(li) {
  margin-bottom: 12px;
  line-height: 1.8;
}

.rich-editor :deep(hr) {
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(26, 26, 26, 0.2), transparent);
  margin: 32px 0;
}
</style>