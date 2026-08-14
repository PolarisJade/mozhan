# 图片上传到阿里云 OSS（写日记 & 写文章）

## Context

用户希望在写日记和写博客文章时能上传照片，照片保存到阿里云 OSS。**后端接口已就绪**：[FileUploadController.java](file:///D:/javaCode/mozhan/mozhan/src/main/java/com/god/mz/controller/user/FileUploadController.java) 提供 `POST /upload/image`，返回 `Result<UploadVO>`（`{url, fileName, fileSize}`），支持 jpg/jpeg/png/gif，最大 5MB。

前端缺口：
- `src/api/upload.js` 只有 `uploadAvatar`，缺 `uploadImage`
- 文章编辑器（[ArticleEditorView.vue](file:///D:/javaCode/mozhan/mozhan-web/src/views/article/ArticleEditorView.vue)）是 `contenteditable` 富文本，工具栏无图片按钮
- 日记编辑器（[DiaryWriteView.vue](file:///D:/javaCode/mozhan/mozhan-web/src/views/diary/DiaryWriteView.vue)）是纯文本 `<textarea>`，详情页 `{{ detail.content }}` 纯文本渲染

**用户已确认方案**：
- 日记改为富文本编辑器（与文章编辑器风格一致）
- 文章 + 日记都支持「按钮点击 + Ctrl+V 粘贴 + 拖拽」三种上传交互

## 实施步骤

### 1. 新增上传 API — `src/api/upload.js`
复用现有 `uploadAvatar` 写法，新增：
```js
export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/upload/image', formData)
}
```
返回值经 [request.js](file:///D:/javaCode/mozhan/mozhan-web/src/utils/request.js) 响应拦截器自动解包，直接拿到 `{url, fileName, fileSize}`。

### 2. 文章编辑器 — `src/views/article/ArticleEditorView.vue`

**a. 工具栏添加「图片」按钮**
- `toolbarButtons` 数组（L180）末尾追加：`{ label: '图片', action: triggerImageSelect }`
- 新增 `triggerImageSelect`：触发隐藏的 `<input type="file">` 的 click

**b. 模板添加隐藏 input**
```html
<input ref="imageInput" type="file" accept="image/jpeg,image/png,image/gif" hidden @change="handleImageSelect">
```

**c. 新增上传逻辑函数**
- `uploadAndInsertImage(file)`：核心函数。校验类型/大小 → `InkMessage.loading('上传中...')` → 调 `uploadImage(file)` → `execCommand('insertHTML', '<img src="url">')` → 关闭 loading → 成功/失败提示
- `handleImageSelect(e)`：取 `e.target.files[0]` → 调 `uploadAndInsertImage` → 重置 input value 以便重复选择同一文件

**d. 编辑器增强 paste / drop 事件**
当前模板（L367-375）已有 `@paste="handlePaste"`，但 `handlePaste`（L147）只处理文本。改为：
- 在 `handlePaste` 开头先检测 `e.clipboardData.items` 中是否有 `image/*` 类型，若有则 `e.preventDefault()` → 取 file → `uploadAndInsertImage` → return；否则走原有文本处理逻辑
- 新增 `handleEditorDrop(e)`：`e.preventDefault()` → 取 `e.dataTransfer.files[0]` → 校验是图片 → `uploadAndInsertImage`
- 模板 `<div class="rich-editor">` 添加 `@dragover.prevent` 和 `@drop.prevent="handleEditorDrop"`

**e. 图片样式**
`.rich-editor :deep(img)`：`max-width: 100%; height: auto; border-radius: 4px; margin: 12px 0; display: block;`

### 3. 日记编辑器 — `src/views/diary/DiaryWriteView.vue`（改造为富文本）

**a. 替换 textarea 为 contenteditable**
```html
<div
  ref="editorRef"
  class="diary-content-input"
  contenteditable="true"
  placeholder="今天发生了什么，心情如何……"
  @blur="saveSelection"
  @paste="handlePaste"
  @drop.prevent="handleEditorDrop"
  @dragover.prevent
  @keydown="handleKeyDown"
></div>
```

**b. 复用文章编辑器的核心逻辑**（参考 [ArticleEditorView.vue L100-178](file:///D:/javaCode/mozhan/mozhan-web/src/views/article/ArticleEditorView.vue#L100-L178)）
- `saveSelection()` / `execCommand()` / `handleKeyDown()` / `handlePaste()`（含图片检测分支）
- `triggerImageSelect` / `handleImageSelect` / `uploadAndInsertImage` / `handleEditorDrop`
- 模板添加隐藏 `<input type="file">` + 在 actions 区添加「插入图片」按钮

**c. 数据流改造**
- `form.content` 改为只在初始化用；提交时取 `editorRef.value.innerHTML` 作为 content
- `loadDiary`：`editorRef.value.innerHTML = data.content || ''`（需 `nextTick` 后赋值，因为 contenteditable 需先挂载）
- `canSubmit`：判断 `editorRef.value?.innerHTML.trim()` 非空（且非纯空白），可在 `@input` 事件里同步一个 `hasContent` ref

**d. 样式**
- `.diary-content-input` 保留原 `.content-input` 的高度/字号/字距等样式，去掉 `resize`、改为 `overflow-y: auto`
- 添加 `.diary-content-input:empty:before { content: attr(placeholder); color: var(--ink-muted); pointer-events: none; }`
- `.diary-content-input :deep(img)` 同文章编辑器

### 4. 日记详情页 — `src/views/diary/DiaryHomeView.vue`

**a. 改为 v-html 渲染**（L278）
```html
<div class="detail-content" v-html="detail.content"></div>
```

**b. 添加图片样式**
`.detail-content :deep(img)`：`max-width: 100%; height: auto; border-radius: 4px; margin: 12px 0; display: block;`

**c. 历史纯文本兼容**
历史日记是纯文本，经 `v-html` 渲染时原样显示为文本（不含 HTML 标签的字符串通过 `v-html` 会按文本展示）。仅当历史内容含 `<` `>` 等字符时可能异常，但属小概率，不做特殊转义处理。

## 复用的现有资源

| 资源 | 位置 |
|---|---|
| 后端上传接口 | [FileUploadController.java:21](file:///D:/javaCode/mozhan/mozhan/src/main/java/com/god/mz/controller/user/FileUploadController.java#L21) |
| 前端 request 实例（自动解包 res.data） | [request.js](file:///D:/javaCode/mozhan/mozhan-web/src/utils/request.js) |
| 文章编辑器富文本模式（execCommand/saveSelection/handlePaste） | [ArticleEditorView.vue:100-178](file:///D:/javaCode/mozhan/mozhan-web/src/views/article/ArticleEditorView.vue#L100-L178) |
| InkMessage 通知 | `@/utils/message` |
| OssUtil（已支持类型/大小校验，前端可轻量校验） | [OssUtil.java](file:///D:/javaCode/mozhan/mozhan/src/main/java/com/god/mz/util/OssUtil.java) |

## 验证步骤

1. **文章按钮上传**：登录 → `/article/edit` → 点工具栏「图片」→ 选 jpg/png → 编辑器出现图片 → 发布 → 详情页可见
2. **文章粘贴上传**：截图或复制图片 → 编辑器 Ctrl+V → 自动上传并插入
3. **文章拖拽上传**：从文件管理器拖图片到编辑器 → 自动上传并插入
4. **日记按钮上传**：`/diary/write` → 输入文字 → 点「插入图片」→ 图片出现 → 保存 → `/diary` 详情页可见图片
5. **日记粘贴/拖拽**：同文章
6. **历史日记兼容**：打开一篇旧纯文本日记，详情页仍正常显示
7. **错误处理**：上传超过 5MB 的图片 → 后端返回错误 → 前端 InkMessage 提示
8. **编辑回填**：编辑已有带图日记/文章 → 图片正常显示在编辑器中
