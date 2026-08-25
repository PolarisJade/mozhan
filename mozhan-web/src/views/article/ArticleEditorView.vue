<script setup>
import { onMounted, onUnmounted, ref, reactive, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { InkMessage } from '@/utils/message'
import 'vditor/dist/index.css'
import { createArticle, updateArticle, getArticleDetail, getArticleInfo, publishArticle } from '@/api/article'
import { getCategoryList } from '@/api/category'
import { getTagList, createTag } from '@/api/tag'
import { uploadImage } from '@/api/upload'
import { useUserStore } from '@/stores/user'
import { redirectToLogin } from '@/utils/auth'
import formatIcon from '@/assets/edit/format.svg'
import strongerIcon from '@/assets/edit/stronger.svg'
import otherIcon from '@/assets/edit/other.svg'
import quoteIcon from '@/assets/edit/quote.svg'
import codeIcon from '@/assets/edit/code.svg'
import listIcon from '@/assets/edit/list.svg'
import alignIcon from '@/assets/edit/align.svg'
import photoIcon from '@/assets/edit/photo.svg'

const router = useRouter()
const userStore = useUserStore()

const isEdit = ref(false)
const articleId = ref(null)
const loading = ref(false)
const submitting = ref(false)
const categories = ref([])
const tags = ref([])
const editorRef = ref(null)
const imageInput = ref(null)
const coverInput = ref(null)
const coverUploading = ref(false)
let vditor = null

const form = reactive({
  title: '',
  summary: '',
  content: '',
  coverImage: '',
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
    form.coverImage = data.coverImage || ''
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

function wrapCode() {
  const editor = document.querySelector('.rich-editor')
  if (!editor) return

  editor.focus()
  const selection = window.getSelection()
  if (selection.rangeCount === 0) return

  const range = selection.getRangeAt(0)

  // 检查选区是否已在代码块内
  let node = range.commonAncestorContainer
  if (node.nodeType === Node.TEXT_NODE) node = node.parentNode
  const existingPre = node.closest && node.closest('pre')

  if (existingPre) {
    // 如果在代码块内，将代码块转为普通文本
    const textNode = document.createTextNode(existingPre.textContent)
    existingPre.parentNode.replaceChild(textNode, existingPre)
    return
  }

  // 检查选区是否在行内 <code> 内
  const existingCode = node.closest && node.closest('code')
  if (existingCode && !existingCode.closest('pre')) {
    // unwrap：取消行内代码
    const parent = existingCode.parentNode
    while (existingCode.firstChild) {
      parent.insertBefore(existingCode.firstChild, existingCode)
    }
    parent.removeChild(existingCode)
    return
  }

  // 检测是否在 <li> 内，需要分离列表项再插入代码块
  const listItem = node.closest && node.closest('li')
  if (listItem) {
    return wrapCodeInListItem(listItem, range, selection)
  }

  if (range.collapsed) {
    // 没有选中内容，插入一个空的代码块
    const wrapper = createCodeBlock('<br>')
    range.insertNode(wrapper)
    // 光标定位到代码块内
    const newRange = document.createRange()
    newRange.setStart(wrapper.querySelector('code'), 0)
    newRange.collapse(true)
    selection.removeAllRanges()
    selection.addRange(newRange)
    bindCopyButtons(wrapper)
    return
  }

  // 提取选区内容，包裹进代码块
  const fragment = range.extractContents()
  const tempDiv = document.createElement('div')
  tempDiv.appendChild(fragment)
  const text = tempDiv.textContent

  const wrapper = createCodeBlock(text)
  range.insertNode(wrapper)

  // 选中整个代码块
  const newRange = document.createRange()
  newRange.selectNode(wrapper)
  selection.removeAllRanges()
  selection.addRange(newRange)
  bindCopyButtons(wrapper)
}

function wrapCodeInListItem(listItem, range, selection) {
  const isCollapsed = range.collapsed
  const list = listItem.closest('ol, ul')
  if (!list) return

  // 第一步：克隆"光标之后"的内容（用于新的 <li>）
  const afterRange = document.createRange()
  afterRange.selectNodeContents(listItem)
  afterRange.setStart(range.endContainer, range.endOffset)
  const afterClone = afterRange.cloneContents()

  // 第二步：获取代码块内容
  let codeContent
  if (isCollapsed) {
    codeContent = '<br>'
  } else {
    const fragment = range.extractContents()
    const tempDiv = document.createElement('div')
    tempDiv.appendChild(fragment)
    codeContent = tempDiv.textContent
  }

  // 第三步：删除当前 <li> 中"光标之后"的内容
  const deleteRange = document.createRange()
  deleteRange.selectNodeContents(listItem)
  deleteRange.setStart(range.endContainer, range.endOffset)
  deleteRange.deleteContents()

  // 第四步：创建代码块
  const wrapper = createCodeBlock(codeContent)

  // 第五步：创建新的 <li>（包含光标之后的内容）
  const newLi = document.createElement('li')
  newLi.appendChild(afterClone)
  if (newLi.childNodes.length === 0) {
    newLi.innerHTML = '<br>'
  }

  // 第六步：拆分列表 —— 将当前项之后的 <li> 移到新列表中
  const newList = list.cloneNode(false)
  newList.appendChild(newLi)

  let nextSibling = listItem.nextElementSibling
  while (nextSibling) {
    const nextNext = nextSibling.nextElementSibling
    if (nextSibling.tagName === 'LI') {
      newList.appendChild(nextSibling)
    }
    nextSibling = nextNext
  }

  // 第七步：在原列表之后插入代码块和新列表
  list.after(wrapper, newList)

  // 第八步：有序列表序号衔接
  if (list.tagName === 'OL') {
    const itemCount = list.querySelectorAll('li').length
    if (itemCount > 0) {
      newList.setAttribute('start', String(itemCount + 1))
    }
  }

  // 第九步：定位光标到代码块内
  const newRange = document.createRange()
  if (isCollapsed) {
    newRange.setStart(wrapper.querySelector('code'), 0)
    newRange.collapse(true)
  } else {
    newRange.selectNode(wrapper)
  }
  selection.removeAllRanges()
  selection.addRange(newRange)
  bindCopyButtons(wrapper)
}

function createCodeBlock(content) {
  const wrapper = document.createElement('div')
  wrapper.className = 'editor-code-block'

  const pre = document.createElement('pre')
  pre.style.margin = '0'
  pre.style.background = 'rgba(26, 26, 26, 0.95)'
  pre.style.borderRadius = '6px'
  pre.style.overflow = 'hidden'
  pre.style.position = 'relative'

  const header = document.createElement('div')
  header.className = 'editor-code-header'
  header.style.cssText = 'display:flex;justify-content:space-between;align-items:center;padding:10px 16px;background:#2d2d2d;border-bottom:1px solid rgba(255,255,255,0.08);font-size:13px;color:rgba(255,255,255,0.6);'

  const langSpan = document.createElement('span')
  langSpan.className = 'editor-code-lang'
  langSpan.textContent = 'code'

  const copyBtn = document.createElement('button')
  copyBtn.className = 'editor-code-copy'
  copyBtn.style.cssText = 'display:flex;align-items:center;gap:4px;padding:4px 10px;font-size:12px;color:rgba(255,255,255,0.6);background:transparent;border:1px solid rgba(255,255,255,0.15);border-radius:4px;cursor:pointer;transition:all 0.2s;'
  copyBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="14" height="14"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"/></svg><span>复制</span>'

  header.appendChild(langSpan)
  header.appendChild(copyBtn)

  const code = document.createElement('code')
  code.style.cssText = 'display:block;padding:16px 20px;color:#e4e4e4;font-size:14px;line-height:1.6;font-family:Consolas,Monaco,monospace;white-space:pre-wrap;'
  code.innerHTML = content

  pre.appendChild(header)
  pre.appendChild(code)
  wrapper.appendChild(pre)

  return wrapper
}

function bindCopyButtons(root) {
  const buttons = root.querySelectorAll('.editor-code-copy')
  buttons.forEach(btn => {
    if (btn.dataset.bound) return
    btn.dataset.bound = 'true'
    btn.addEventListener('click', () => {
      const pre = btn.closest('pre')
      const code = pre.querySelector('code')
      const text = code.textContent
      navigator.clipboard.writeText(text).then(() => {
        const span = btn.querySelector('span')
        const originalText = span.textContent
        span.textContent = '已复制'
        btn.style.color = '#4ade80'
        btn.style.borderColor = 'rgba(74, 222, 128, 0.5)'
        setTimeout(() => {
          span.textContent = originalText
          btn.style.color = ''
          btn.style.borderColor = ''
        }, 2000)
      })
    })
  })
}

function handleKeyDown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    const selection = window.getSelection()
    if (selection.rangeCount > 0) {
      let node = selection.getRangeAt(0).startContainer
      if (node.nodeType === Node.TEXT_NODE) node = node.parentNode

      // 如果光标在代码块内
      const inPre = node.closest && node.closest('pre')
      if (inPre) {
        const code = inPre.querySelector('code')
        if (code) {
          const range = selection.getRangeAt(0)
          const endRange = document.createRange()
          endRange.selectNodeContents(code)
          endRange.setStart(range.endContainer, range.endOffset)
          const isAtEnd = endRange.toString().length === 0

          if (isAtEnd) {
            const wrapper = inPre.closest('.editor-code-block') || inPre
            const p = document.createElement('p')
            p.innerHTML = '<br>'
            wrapper.after(p)
            const newRange = document.createRange()
            newRange.setStart(p, 0)
            newRange.collapse(true)
            selection.removeAllRanges()
            selection.addRange(newRange)
            e.preventDefault()
            e.stopPropagation()
            return
          }
        }

        // 在代码块内插入 <br>
        const range = selection.getRangeAt(0)
        const br = document.createElement('br')
        range.deleteContents()
        range.insertNode(br)
        const newRange = document.createRange()
        newRange.setStartAfter(br)
        newRange.collapse(true)
        selection.removeAllRanges()
        selection.addRange(newRange)
        e.preventDefault()
        e.stopPropagation()
        return
      }

      // 检测当前光标是否在标题元素内（H1-H6）
      const heading = node.closest && node.closest('h1, h2, h3, h4, h5, h6')
      if (heading) {
        e.preventDefault()
        e.stopPropagation()
        const editor = document.querySelector('.rich-editor')
        if (!editor) return
        editor.focus()
        // 在标题后插入一个空段落，光标移入新段落
        const p = document.createElement('p')
        p.innerHTML = '<br>'
        heading.after(p)
        const newRange = document.createRange()
        newRange.setStart(p, 0)
        newRange.collapse(true)
        const sel = window.getSelection()
        sel.removeAllRanges()
        sel.addRange(newRange)
        return
      }

      // 检测当前光标是否在列表项内（li）
      // 不拦截 Enter，让浏览器默认创建新的 <li>，有序列表序号自动递增
      const listItem = node.closest && node.closest('li')
      if (listItem) {
        return
      }
    }
    e.preventDefault()
    e.stopPropagation()

    const editor = document.querySelector('.rich-editor')
    if (!editor) return

    editor.focus()
    document.execCommand('insertLineBreak', false)
  }
}

function handlePaste(e) {
  const selection = window.getSelection()
  let inPre = false
  if (selection.rangeCount > 0) {
    let node = selection.getRangeAt(0).startContainer
    if (node.nodeType === Node.TEXT_NODE) node = node.parentNode
    inPre = !!(node.closest && node.closest('pre'))
  }

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
  
  if (text) {
    const containsWordStyles = text.includes('@font-face') || text.includes('mso-') || text.includes('<!DOCTYPE') || text.includes('<html')
    
    if (containsWordStyles) {
      text = e.clipboardData.getData('text/plain') || ''
    }
    
    if (!text.trim()) return

    if (inPre) {
      text = text.replace(/\r\n/g, '\n')
      const editor = document.querySelector('.rich-editor')
      if (editor) {
        editor.focus()
        document.execCommand('insertText', false, text)
      }
      return
    }
    
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
    const editor = document.querySelector('.rich-editor')
    if (editor) {
      editor.focus()
      document.execCommand('insertHTML', false, imgHtml)
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

// ===== 封面上传相关 =====
function triggerCoverSelect() {
  if (coverInput.value) {
    coverInput.value.value = ''
    coverInput.value.click()
  }
}

async function handleCoverSelect(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (!validateImageFile(file)) return
  coverUploading.value = true
  const hide = InkMessage.loading('封面上传中...', 0)
  try {
    const data = await uploadImage(file)
    form.coverImage = data.url
    InkMessage.success('封面设置成功')
  } catch (err) {
    console.error('封面上传失败', err)
  } finally {
    coverUploading.value = false
    hide?.()
  }
}

function removeCover() {
  form.coverImage = ''
}

const openDropdown = ref('')

const formatItems = [
  { label: '正文', size: '16px', action: () => execCommand('formatBlock', 'p') },
  { label: '标题一', size: '32px', action: () => execCommand('formatBlock', 'h1') },
  { label: '标题二', size: '28px', action: () => execCommand('formatBlock', 'h2') },
  { label: '标题三', size: '24px', action: () => execCommand('formatBlock', 'h3') },
  { label: '标题四', size: '20px', action: () => execCommand('formatBlock', 'h4') },
  { label: '标题五', size: '18px', action: () => execCommand('formatBlock', 'h5') },
  { label: '标题六', size: '16px', action: () => execCommand('formatBlock', 'h6') },
]

const toolbarGroups = [
  { label: '格式', icon: formatIcon, type: 'dropdown', items: formatItems },
  { label: '粗体', icon: strongerIcon, type: 'button', action: () => execCommand('bold') },
  {
    label: '其它',
    icon: otherIcon,
    type: 'dropdown',
    items: [
      { label: '斜体', action: () => execCommand('italic') },
      { label: '下划线', action: () => execCommand('underline') },
      { label: '删除线', action: () => execCommand('strikeThrough') },
    ],
  },
  { label: '引用', icon: quoteIcon, type: 'button', action: () => execCommand('formatBlock', 'blockquote') },
  { label: '代码', icon: codeIcon, type: 'button', action: wrapCode },
  {
    label: '列表',
    icon: listIcon,
    type: 'dropdown',
    items: [
      { label: '无序列表', action: () => execCommand('insertUnorderedList') },
      { label: '有序列表', action: () => execCommand('insertOrderedList') },
    ],
  },
  {
    label: '对齐',
    icon: alignIcon,
    type: 'dropdown',
    items: [
      { label: '左对齐', action: () => execCommand('justifyLeft') },
      { label: '居中对齐', action: () => execCommand('justifyCenter') },
      { label: '右对齐', action: () => execCommand('justifyRight') },
    ],
  },
  { label: '图片', icon: photoIcon, type: 'button', action: triggerImageSelect },
]

function toggleDropdown(label) {
  openDropdown.value = openDropdown.value === label ? '' : label
}

function handleDropdownItem(item) {
  item.action()
  openDropdown.value = ''
}

function handleToolbarClickOutside(e) {
  const wrapper = document.querySelector('.editor-toolbar-wrapper')
  if (wrapper && !wrapper.contains(e.target)) {
    openDropdown.value = ''
  }
}

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
      coverImage: form.coverImage || null,
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
  document.addEventListener('mousedown', handleToolbarClickOutside)
  
  const id = router.currentRoute.value.params.id
  if (id) {
    isEdit.value = true
    articleId.value = id
    setTimeout(() => loadArticle(), 300)
  }
})

onUnmounted(() => {
  document.removeEventListener('mousedown', handleToolbarClickOutside)
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
        <el-col :span="14">
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

          <el-form-item label="文章标签" style="margin-top: 20px;">
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

        <el-col :span="10">
          <el-form-item label="文章封面">
            <div class="cover-uploader">
              <div
                v-if="form.coverImage"
                class="cover-preview"
              >
                <img :src="form.coverImage" alt="文章封面" class="cover-img" />
                <div class="cover-overlay">
                  <button type="button" class="cover-btn" @click="triggerCoverSelect" :disabled="coverUploading">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                    </svg>
                    更换
                  </button>
                  <button type="button" class="cover-btn cover-btn-remove" @click="removeCover" :disabled="coverUploading">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="3 6 5 6 21 6" />
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                    </svg>
                    移除
                  </button>
                </div>
              </div>
              <div
                v-else
                class="cover-placeholder"
                :class="{ 'is-uploading': coverUploading }"
                @click="triggerCoverSelect"
              >
                <div v-if="coverUploading" class="cover-loading">
                  <span class="loading-spinner"></span>
                  <span>上传中...</span>
                </div>
                <template v-else>
                  <svg class="cover-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                    <circle cx="8.5" cy="8.5" r="1.5" />
                    <polyline points="21 15 16 10 5 21" />
                  </svg>
                  <span class="cover-text">点击上传封面</span>
                  <span class="cover-hint">建议 1200×630，jpg/png/gif</span>
                </template>
              </div>
              <input
                ref="coverInput"
                type="file"
                accept="image/jpeg,image/png,image/gif"
                hidden
                @change="handleCoverSelect"
              />
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="editor-toolbar-wrapper">
      <div class="editor-toolbar">
        <template v-for="group in toolbarGroups" :key="group.label">
          <div v-if="group.type === 'dropdown'" class="toolbar-dropdown">
            <button
              type="button"
              class="toolbar-btn dropdown-toggle"
              :class="{ active: openDropdown === group.label }"
              :title="group.label"
              @mousedown.prevent
              @click.stop="toggleDropdown(group.label)"
            >
              <img class="toolbar-icon" :src="group.icon" alt="" />
              <span>{{ group.label }}</span>
              <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                <path d="M6 9l6 6 6-6" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <div v-show="openDropdown === group.label" class="toolbar-dropdown-menu" @mousedown.prevent>
              <button
                v-for="item in group.items"
                :key="item.label"
                type="button"
                class="toolbar-dropdown-item"
                :style="item.size ? { fontSize: item.size } : undefined"
                @click="handleDropdownItem(item)"
              >
                {{ item.label }}
              </button>
            </div>
          </div>
          <button
            v-else
            type="button"
            class="toolbar-btn"
            :title="group.label"
            @mousedown.prevent="group.action"
          >
            <img class="toolbar-icon" :src="group.icon" alt="" />
            <span>{{ group.label }}</span>
          </button>
        </template>
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
      @dragover.prevent
      @drop.prevent="handleEditorDrop"
    ></div>

    <input
      ref="imageInput"
      type="file"
      accept="image/jpeg,image/png,image/gif"
      hidden
      @change="handleImageSelect"
    />
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

.editor-header .ink-page-title {
  font-size: 26px;
}

.header-actions .ink-btn,
.header-actions .ink-btn-plain {
  font-size: 16px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.editor-form {
  padding: 32px 36px;
}

/* 封面上传 */
.cover-uploader {
  width: 100%;
}

.cover-placeholder {
  width: 100%;
  height: 188px;
  border: 2px dashed rgba(74, 74, 74, 0.2);
  border-radius: 4px;
  background: rgba(255, 253, 248, 0.4);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  transition: all 0.25s ease;
}

.cover-placeholder:hover {
  border-color: var(--ink);
  background: rgba(255, 253, 248, 0.7);
}

.cover-placeholder.is-uploading {
  cursor: default;
  border-color: var(--ink-light);
}

.cover-icon {
  width: 40px;
  height: 40px;
  color: var(--ink-light);
}

.cover-text {
  font-family: 'Noto Serif SC', serif;
  font-size: 16px;
  color: var(--ink);
  letter-spacing: 0.05em;
}

.cover-hint {
  font-size: 14px;
  color: var(--ink-muted);
  letter-spacing: 0.02em;
}

.cover-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: var(--ink-light);
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 2px solid rgba(74, 74, 74, 0.15);
  border-top-color: var(--ink);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.cover-preview {
  position: relative;
  width: 100%;
  height: 188px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid rgba(74, 74, 74, 0.12);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.cover-preview:hover .cover-overlay {
  opacity: 1;
}

.cover-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 15px;
  font-family: 'Noto Serif SC', serif;
  color: #fff;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s ease;
  letter-spacing: 0.05em;
}

.cover-btn svg {
  width: 14px;
  height: 14px;
}

.cover-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.5);
}

.cover-btn-remove:hover:not(:disabled) {
  background: rgba(220, 53, 69, 0.6);
  border-color: rgba(220, 53, 69, 0.8);
}

.cover-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
  font-size: 14px;
  color: var(--ink-muted);
  margin-top: 8px;
  letter-spacing: 0.02em;
}

.editor-form :deep(.el-select .el-tag) {
  font-size: 15px;
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
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  font-size: 15px;
  font-family: 'Noto Serif SC', serif;
  color: var(--ink-light);
  background: transparent;
  border: none;
  border-radius: 3px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toolbar-icon {
  width: 15px;
  height: 15px;
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.toolbar-btn:hover .toolbar-icon {
  opacity: 1;
}

.toolbar-btn:hover {
  background: rgba(74, 74, 74, 0.05);
  color: var(--ink);
}

.toolbar-dropdown {
  position: relative;
}

.toolbar-btn.dropdown-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
}

.toolbar-btn.dropdown-toggle.active {
  background: rgba(74, 74, 74, 0.05);
  color: var(--ink);
}

.toolbar-dropdown .arrow-icon {
  width: 11px;
  height: 11px;
  opacity: 0.7;
  transition: transform 0.2s;
}

.toolbar-btn.dropdown-toggle.active .arrow-icon {
  transform: rotate(180deg);
}

.toolbar-dropdown-menu {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  min-width: 110px;
  background: #fff;
  border: 1px solid rgba(74, 74, 74, 0.12);
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  z-index: 200;
}

.toolbar-dropdown-item {
  display: block;
  width: 100%;
  padding: 6px 14px;
  font-family: 'Noto Serif SC', serif;
  font-size: 15px;
  color: var(--ink-light);
  background: transparent;
  border: none;
  text-align: left;
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.15s;
}

.toolbar-dropdown-item:hover {
  background: rgba(74, 74, 74, 0.05);
  color: var(--ink);
}

.rich-editor {
  width: 100%;
  min-height: 450px;
  margin-top: 0;
  padding: 24px;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', 'Hiragino Sans GB', sans-serif;
  font-size: 16px;
  line-height: 1.9;
  letter-spacing: 0.02em;
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
.rich-editor :deep(h3),
.rich-editor :deep(h4),
.rich-editor :deep(h5),
.rich-editor :deep(h6) {
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Noto Sans SC', sans-serif;
  font-weight: 600;
  color: var(--ink);
  margin-top: 0.4em;
  margin-bottom: 0.5em;
  line-height: 1.4;
}

.rich-editor :deep(h1) { font-size: 32px; letter-spacing: 0.08em; }
.rich-editor :deep(h2) { font-size: 28px; letter-spacing: 0.06em; }
.rich-editor :deep(h3) { font-size: 24px; letter-spacing: 0.04em; }
.rich-editor :deep(h4) { font-size: 20px; }
.rich-editor :deep(h5) { font-size: 18px; }
.rich-editor :deep(h6) { font-size: 16px; }

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

.rich-editor :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 12px 0;
  display: block;
}
</style>