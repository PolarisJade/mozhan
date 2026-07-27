<script setup>
import { onMounted, ref, watch, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticleDetail, likeArticle } from '@/api/article'
import { followUser, unfollowUser } from '@/api/user'
import { getCommentsByArticle, createComment } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import { InkMessage } from '@/utils/message'
import { redirectToLogin } from '@/utils/auth'
import { marked, Renderer } from 'marked'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const likeLoading = ref(false)
const followLoading = ref(false)
const commentLoading = ref(false)
const submitLoading = ref(false)
const article = ref(null)
const comments = ref([])
const newComment = ref('')
const commentsContainer = ref(null)
const expandedReplies = ref([])
const replyCommentId = ref(null)
const replyToId = ref(null)
const replyToNickname = ref('')
const contentRef = ref(null)
const toc = ref([])
const activeHeadingId = ref('')

const articleContent = computed(() => {
  if (!article.value?.content) return ''
  
  let index = 0
  const renderer = new Renderer()
  
  renderer.heading = function(text, level) {
    index++
    const id = `article-heading-${index}`
    return `<h${level} id="${id}">${text}</h${level}>`
  }
  
  return marked(article.value.content, { renderer })
})

function extractToc() {
  setTimeout(() => {
    if (!contentRef.value) return
    const headings = contentRef.value.querySelectorAll('h1, h2, h3, h4')
    console.log('Found headings:', headings.length)
    
    let index = 0
    toc.value = Array.from(headings).map(heading => {
      index++
      const id = heading.id || `article-heading-${index}`
      if (!heading.id) {
        heading.id = id
      }
      console.log('Heading:', id, heading.textContent)
      return {
        id,
        text: heading.textContent,
        level: parseInt(heading.tagName[1])
      }
    })
    
    console.log('TOC extracted:', toc.value)
  }, 100)
}

const isLiked = computed(() => {
  return article.value?.isLike === true || article.value?.isLike === 'true'
})

function toggleReplies(commentId) {
  const index = expandedReplies.value.indexOf(commentId)
  if (index > -1) {
    expandedReplies.value.splice(index, 1)
  } else {
    expandedReplies.value.push(commentId)
  }
}

async function loadDetail() {
  loading.value = true
  try {
    article.value = await getArticleDetail(route.params.id)
    await loadComments()
  } finally {
    loading.value = false
    setTimeout(() => {
      extractToc()
    }, 50)
  }
}

async function loadComments() {
  commentLoading.value = true
  try {
    const data = await getCommentsByArticle(route.params.id, {
      pageSize: 20,
      isAsc: false,
      sortBy: 'create_time',
    })
    comments.value = data.list || []
  } finally {
    commentLoading.value = false
  }
}

function showFollowButton() {
  if (!userStore.isLoggedIn) return false
  if (article.value?.isAuthor) return false
  return article.value?.isFollowed !== null
}

function goToAuthorProfile() {
  if (article.value?.authorId) {
    router.push(`/user/${article.value.authorId}`)
  }
}

function goToUserProfile(userId) {
  if (userId) {
    router.push(`/user/${userId}`)
  }
}

function scrollToComments() {
  nextTick(() => {
    if (commentsContainer.value) {
      commentsContainer.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
    }
  })
}

async function handleLike() {
  if (!userStore.isLoggedIn) {
    redirectToLogin()
    return
  }
  if (!article.value || likeLoading.value) return
  likeLoading.value = true
  try {
    const likeCount = await likeArticle(article.value.id)
    article.value.likeCount = likeCount
    const currentLike = article.value.isLike === true || article.value.isLike === 'true'
    article.value.isLike = !currentLike
  } finally {
    likeLoading.value = false
  }
}

async function handleFollow() {
  if (!userStore.isLoggedIn) {
    redirectToLogin()
    return
  }
  
  followLoading.value = true
  try {
    if (article.value.isFollowed) {
      await unfollowUser(article.value.authorId)
      article.value.isFollowed = false
      InkMessage.success('已取消关注')
    } else {
      await followUser(article.value.authorId)
      article.value.isFollowed = true
      InkMessage.success('关注成功')
    }
  } finally {
    followLoading.value = false
  }
}

function startReply(commentId, replyToIdVal, nickname) {
  replyCommentId.value = commentId
  replyToId.value = replyToIdVal
  replyToNickname.value = nickname
  newComment.value = ''
}

function cancelReply() {
  replyCommentId.value = null
  replyToId.value = null
  replyToNickname.value = ''
  newComment.value = ''
}

async function submitComment() {
  if (!newComment.value.trim()) {
    InkMessage.warning('请输入评论内容')
    return
  }

  if (!userStore.isLoggedIn) {
    redirectToLogin()
    return
  }
  
  submitLoading.value = true
  try {
    const params = {
      articleId: route.params.id,
      content: newComment.value.trim(),
    }
    
    if (replyCommentId.value) {
      params.parentId = replyCommentId.value
    }
    
    if (replyToId.value) {
      params.replyToId = replyToId.value
    }
    
    await createComment(params)
    newComment.value = ''
    article.value.commentCount = (article.value.commentCount || 0) + 1
    await loadComments()
    cancelReply()
    InkMessage.success('评论成功')
  } finally {
    submitLoading.value = false
  }
}

function scrollToHeading(id) {
  const element = document.getElementById(id)
  if (element) {
    element.scrollIntoView({ behavior: 'smooth', block: 'start' })
    activeHeadingId.value = id
  }
}

function handleScroll() {
  const headings = contentRef.value?.querySelectorAll('h1, h2, h3, h4') || []
  let currentId = ''
  
  headings.forEach(heading => {
    const rect = heading.getBoundingClientRect()
    if (rect.top <= 100) {
      currentId = heading.id
    }
  })
  
  activeHeadingId.value = currentId
}

onMounted(() => {
  loadDetail()
  window.addEventListener('scroll', handleScroll)
})

watch(() => route.params.id, () => {
  loadDetail()
  toc.value = []
  activeHeadingId.value = ''
})

watch(articleContent, () => {
  extractToc()
})
</script>

<template>
  <div class="detail">
    <el-skeleton v-if="loading" :rows="8" animated />

    <div v-else-if="article" class="detail-layout">
      <article class="detail-card ink-card">
        <h1 class="title font-display">{{ article.title }}</h1>
        
        <div class="author-info">
          <span class="author-name" @click="goToAuthorProfile">{{ article.authorName }}</span>
          <button
            v-if="showFollowButton()"
            class="follow-btn"
            :class="{ followed: article.isFollowed }"
            :loading="followLoading"
            @click="handleFollow"
          >
            {{ article.isFollowed ? '已关注' : '+ 关注' }}
          </button>
        </div>

        <div class="category-row">
          <span class="category">{{ article.categoryName }}</span>
          <span v-if="article.tags?.length" class="tags">
            <span v-for="tag in article.tags" :key="tag.id" class="tag">{{ tag.name }}</span>
          </span>
        </div>

        <div class="stats-row">
          <span class="stat-item" @click="scrollToComments">
            <svg class="icon-comment" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>{{ article.commentCount ?? 0 }}</span>
          </span>
          <span class="stat-item like-item" @click="handleLike">
            <svg :class="['icon-like', { liked: isLiked }]" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M6 18c-2 0-4-1.5-4-4v-5c0-1.5 1-3 3-3h6l2 5h4c2 0 3 1.5 3 3v2c0 1.5-1.5 3-3 3H6z" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M9 12l2 3 4-5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>{{ article.likeCount ?? 0 }}</span>
            <span class="like-text">{{ isLiked ? '已赞' : '点赞' }}</span>
          </span>
        </div>

        <el-divider class="ink-divider" />
        <div ref="contentRef" class="content" v-html="articleContent" />
      </article>

      <aside v-if="toc.length > 0" class="toc-sidebar">
        <div class="toc-header">
          <span class="toc-title">目录</span>
        </div>
        <nav class="toc-nav">
          <ul class="toc-list">
            <li 
              v-for="item in toc" 
              :key="item.id" 
              class="toc-item"
              :class="{ active: activeHeadingId === item.id, 'level-3': item.level === 3, 'level-4': item.level === 4 }"
            >
              <a :href="'#' + item.id">{{ item.text }}</a>
            </li>
          </ul>
        </nav>
      </aside>
    </div>

    <div ref="commentsContainer" class="comments-section ink-card" v-if="article">
      <h3 class="comments-title">评论 ({{ article.commentCount ?? 0 }})</h3>
      
      <div class="comment-input">
        <textarea
          v-model="newComment"
          placeholder="写下你的评论..."
          class="comment-textarea"
          rows="3"
          @keydown.enter.exact.prevent="submitComment"
        />
        <button class="submit-btn" :loading="submitLoading" @click="submitComment">
          发表评论
        </button>
      </div>

      <div class="comments-list" v-loading="commentLoading">
        <div v-if="comments.length > 0" class="comment-items">
          <div
            v-for="comment in comments"
            :key="comment.id"
            class="comment-item"
          >
            <div class="comment-header">
              <el-avatar 
                :size="40" 
                :src="comment.avatar"
                class="avatar-clickable"
                @click="goToUserProfile(comment.userId)"
              >
                {{ comment.nickname?.[0] || comment.username?.[0] }}
              </el-avatar>
              <div class="comment-info">
                <div class="author-row">
                  <span class="comment-author">{{ comment.nickname || comment.username }}</span>
                  <span v-if="comment.isAuthor" class="author-badge">作者</span>
                </div>
                <span class="comment-time">{{ comment.createTime }}</span>
                <span 
                  v-if="userStore.isLoggedIn" 
                  class="comment-reply"
                  @click="startReply(comment.id, null, comment.nickname)"
                >
                  回复
                </span>
              </div>
            </div>
            <p class="comment-content">{{ comment.content }}</p>
            
            <div v-if="replyCommentId === comment.id && !replyToId" class="reply-input-section">
              <textarea 
                v-model="newComment" 
                placeholder="写下你的回复..." 
                class="reply-input"
              ></textarea>
              <div class="reply-actions">
                <button class="reply-cancel" @click="cancelReply">取消</button>
                <button class="reply-submit" @click="submitComment">回复</button>
              </div>
            </div>
            
            <div v-if="comment.totalReplies && comment.totalReplies > 0" class="replies-section">
              <div 
                class="replies-toggle" 
                @click="toggleReplies(comment.id)"
              >
                <span class="replies-count">{{ comment.totalReplies }} 条回复</span>
                <span class="toggle-icon">{{ expandedReplies.includes(comment.id) ? '收起' : '展开' }}</span>
              </div>
              
              <div v-if="expandedReplies.includes(comment.id)" class="replies-list">
                <div 
                  v-for="reply in comment.replies" 
                  :key="reply.id" 
                  class="reply-item"
                >
                  <div class="reply-header">
                    <el-avatar 
                      :size="32" 
                      :src="reply.avatar"
                      class="avatar-clickable"
                      @click="goToUserProfile(reply.userId)"
                    >
                      {{ reply.nickname?.[0] || reply.username?.[0] }}
                    </el-avatar>
                    <div class="reply-info">
                      <span class="reply-author">{{ reply.nickname || reply.username }}</span>
                      <span v-if="reply.isAuthor" class="author-badge">作者</span>
                      <span class="reply-arrow">→</span>
                      <span v-if="reply.replyToNickname" class="reply-to">{{ reply.replyToNickname }}</span>
                      <span class="reply-time">{{ reply.createTime }}</span>
                      <span 
                        v-if="userStore.isLoggedIn" 
                        class="reply-reply"
                        @click="startReply(comment.id, reply.id, reply.nickname)"
                      >
                        回复
                      </span>
                    </div>
                  </div>
                  <p class="reply-content">{{ reply.content }}</p>
                  
                  <div v-if="replyCommentId === comment.id && replyToId === reply.id" class="reply-input-section">
                    <textarea 
                      v-model="newComment" 
                      :placeholder="`回复 ${reply.nickname}...`" 
                      class="reply-input"
                    ></textarea>
                    <div class="reply-actions">
                      <button class="reply-cancel" @click="cancelReply">取消</button>
                      <button class="reply-submit" @click="submitComment">回复</button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无评论，快来发表第一条评论吧" />
      </div>
    </div>

    <el-empty v-else description="文章不存在或已删除" />
  </div>
</template>

<style scoped>
.detail-card {
  padding: 32px 36px;
}

.title {
  margin: 0;
  font-size: 32px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.06em;
  line-height: 1.4;
}

.author-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
}

.author-name {
  font-size: 15px;
  color: var(--ink-light);
  cursor: pointer;
  text-decoration: underline;
  text-underline-offset: 2px;
}

.follow-btn {
  padding: 6px 16px;
  font-size: 13px;
  letter-spacing: 0.06em;
  border-radius: 16px;
  border: 1px solid var(--ink);
  background: transparent;
  color: var(--ink);
  transition: all 0.2s;
}

.follow-btn:hover {
  background: var(--ink);
  color: #fff;
}

.follow-btn.followed {
  background: rgba(26, 26, 26, 0.06);
  border-color: rgba(26, 26, 26, 0.15);
  color: var(--ink-light);
}

.follow-btn.followed:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
}

.category-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.category {
  font-size: 14px;
  color: var(--ink-muted);
  padding: 4px 10px;
  background: rgba(26, 26, 26, 0.05);
  border-radius: 4px;
}

.tags {
  display: flex;
  gap: 8px;
}

.tag {
  font-size: 14px;
  color: var(--ink-muted);
  padding: 4px 10px;
  background: rgba(26, 26, 26, 0.05);
  border-radius: 4px;
}

.stats-row {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-top: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--ink-light);
  font-size: 14px;
}

.stat-item svg {
  width: 18px;
  height: 18px;
}

.comment-item {
  cursor: pointer;
}

.comment-item:hover .icon-comment {
  stroke: #5B7D6D;
}

.icon-comment {
  stroke: #6B8E7D;
}

.icon-like {
  stroke: #8B7355;
}

.icon-like.liked {
  stroke: #e74c3c;
  fill: #ffeaea;
}

.like-item {
  cursor: pointer;
}

.like-text {
  margin-left: 2px;
}

.content {
  margin-top: 24px;
  line-height: 1.9;
  color: var(--ink-light);
  word-break: break-word;
  font-size: 15px;
  font-family: 'Noto Serif SC', 'STSong', 'SimSun', serif;
  letter-spacing: 0.03em;
}

.content :deep(img) {
  max-width: 100%;
  border-radius: 4px;
  margin: 12px 0;
  box-shadow: 0 4px 16px rgba(26, 26, 26, 0.1);
}

.content :deep(h1),
.content :deep(h2),
.content :deep(h3),
.content :deep(h4),
.content :deep(h5),
.content :deep(h6) {
  color: var(--ink);
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-weight: 400;
  margin-top: 1.8em;
  margin-bottom: 0.6em;
  letter-spacing: 0.05em;
}

.content :deep(h1) { font-size: 26px; }
.content :deep(h2) { font-size: 22px; }
.content :deep(h3) { font-size: 19px; }
.content :deep(h4) { font-size: 17px; }

.content :deep(p) {
  margin-bottom: 1.2em;
  text-align: justify;
}

.content :deep(blockquote) {
  border-left: 3px solid var(--ink-light);
  padding-left: 16px;
  margin: 16px 0;
  color: var(--ink-muted);
  font-style: italic;
  background: rgba(74, 74, 74, 0.03);
  padding: 12px 16px;
  border-radius: 0 4px 4px 0;
}

.content :deep(code) {
  background: rgba(74, 74, 74, 0.08);
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 0.9em;
  font-family: 'Consolas', 'Monaco', monospace;
}

.content :deep(pre) {
  background: rgba(26, 26, 26, 0.95);
  padding: 20px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 16px 0;
}

.content :deep(pre code) {
  background: transparent;
  color: #e4e4e4;
  padding: 0;
  font-size: 14px;
  line-height: 1.6;
}

.content :deep(ul),
.content :deep(ol) {
  margin: 12px 0;
  padding-left: 28px;
}

.content :deep(li) {
  margin-bottom: 8px;
}

.content :deep(a) {
  color: var(--ink);
  text-decoration: none;
  border-bottom: 1px solid rgba(26, 26, 26, 0.2);
  padding-bottom: 1px;
  transition: border-color 0.2s;
}

.content :deep(a:hover) {
  border-bottom-color: var(--ink);
}

.content :deep(hr) {
  border: none;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(26, 26, 26, 0.2), transparent);
  margin: 32px 0;
}

.content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  font-size: 14px;
}

.content :deep(th),
.content :deep(td) {
  border: 1px solid rgba(26, 26, 26, 0.1);
  padding: 10px 14px;
  text-align: left;
}

.content :deep(th) {
  background: rgba(74, 74, 74, 0.05);
  font-weight: 600;
  color: var(--ink);
}

.comments-section {
  margin-top: 24px;
  padding: 24px 36px;
}

.comments-title {
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.1em;
  margin: 0 0 20px;
}

.comment-input {
  margin-bottom: 24px;
}

.comment-textarea {
  width: 100%;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.6;
  border: 1px solid rgba(26, 26, 26, 0.15);
  border-radius: 8px;
  resize: none;
  background: rgba(255, 252, 247, 0.5);
  color: var(--ink);
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.comment-textarea:focus {
  outline: none;
  border-color: var(--ink);
}

.comment-textarea::placeholder {
  color: var(--ink-muted);
}

.submit-btn {
  margin-top: 12px;
  padding: 8px 24px;
  font-size: 14px;
  letter-spacing: 0.06em;
  border-radius: 20px;
  border: 1px solid var(--ink);
  background: var(--ink);
  color: #fff;
  cursor: pointer;
  transition: all 0.2s;
  float: right;
}

.submit-btn:hover:not(:disabled) {
  background: #333;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.comments-list {
  clear: both;
  padding-top: 16px;
}

.comment-items {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.comment-item {
  padding-bottom: 20px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.comment-header :deep(.ant-avatar) {
  border: 1px solid rgba(26, 26, 26, 0.08);
}

.avatar-clickable {
  cursor: pointer;
  transition: transform 0.2s;
}

.avatar-clickable:hover {
  transform: scale(1.05);
}

.comment-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.author-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.comment-author {
  font-size: 14px;
  color: var(--ink);
  font-weight: 500;
}

.author-badge {
  font-size: 11px;
  color: #c53d43;
  background-color: rgba(197, 61, 67, 0.1);
  padding: 1px 6px;
  border-radius: 4px;
  margin-left: 6px;
  font-weight: 500;
}

.comment-time {
  font-size: 12px;
  color: var(--ink-muted);
}

.comment-reply {
  font-size: 12px;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 2px 8px;
  border-radius: 4px;
  display: inline-block;
  width: fit-content;
  transition: all 0.2s;
}

.comment-reply:hover {
  color: var(--ink-light);
  background-color: rgba(26, 26, 26, 0.05);
}

.comment-content {
  margin: 0;
  font-size: 15px;
  color: var(--ink-light);
  line-height: 1.7;
  padding-left: 52px;
}

.replies-section {
  margin-top: 12px;
  padding-left: 52px;
}

.replies-toggle {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 6px 0;
  transition: color 0.2s;
}

.replies-toggle:hover {
  color: var(--ink-light);
}

.replies-count {
  font-weight: 500;
}

.toggle-icon {
  font-size: 12px;
}

.replies-list {
  margin-top: 8px;
  border-left: 1px solid rgba(26, 26, 26, 0.1);
  padding-left: 16px;
}

.reply-item {
  padding: 12px 0;
  border-bottom: 1px solid rgba(26, 26, 26, 0.06);
}

.reply-item:last-child {
  border-bottom: none;
}

.reply-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.reply-header :deep(.ant-avatar) {
  border: 1px solid rgba(26, 26, 26, 0.08);
}

.reply-info {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
}

.reply-time {
  flex-basis: 100%;
}

.reply-author {
  font-size: 13px;
  color: var(--ink);
  font-weight: 500;
}

.reply-arrow {
  font-size: 12px;
  color: var(--ink-muted);
  display: none;
}

.reply-to {
  font-size: 12px;
  color: var(--ink-muted);
  display: none;
}

.reply-info:has(.reply-to) .reply-arrow,
.reply-info:has(.reply-to) .reply-to {
  display: inline;
}

.reply-time {
  font-size: 12px;
  color: var(--ink-muted);
  margin-left: 4px;
}

.reply-content {
  margin: 0;
  font-size: 14px;
  color: var(--ink-light);
  line-height: 1.6;
  padding-left: 40px;
}

.comment-reply,
.reply-reply {
  font-size: 12px;
  color: var(--ink-muted);
  cursor: pointer;
  padding: 2px 6px;
  border-radius: 4px;
  transition: color 0.2s;
}

.comment-reply:hover,
.reply-reply:hover {
  color: var(--ink-light);
  background-color: rgba(26, 26, 26, 0.05);
}

.reply-input-section {
  margin-top: 12px;
  padding-left: 52px;
}

.reply-input {
  width: 100%;
  min-height: 60px;
  padding: 10px 14px;
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 6px;
  font-size: 14px;
  color: var(--ink);
  background-color: rgba(255, 252, 247, 0.6);
  resize: vertical;
  box-sizing: border-box;
}

.reply-input:focus {
  outline: none;
  border-color: var(--ink);
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.reply-cancel {
  padding: 6px 16px;
  font-size: 14px;
  color: var(--ink-muted);
  background: transparent;
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.reply-cancel:hover {
  color: var(--ink-light);
  border-color: var(--ink-light);
}

.reply-submit {
  padding: 6px 16px;
  font-size: 14px;
  color: #fff;
  background-color: var(--ink);
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.reply-submit:hover {
  background-color: rgba(26, 26, 26, 0.8);
}

.detail-layout {
  display: flex;
  gap: 32px;
}

.detail-layout .detail-card {
  flex: 1;
  min-width: 0;
}

.toc-sidebar {
  width: 280px;
  flex-shrink: 0;
  position: sticky;
  top: 88px;
  align-self: flex-start;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.toc-header {
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.toc-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink);
}

.toc-nav {
  overflow-y: auto;
  max-height: calc(100vh - 160px);
}

.toc-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.toc-item {
  padding: 0;
  font-size: 13px;
  color: var(--ink-muted);
  border-radius: 4px;
  transition: all 0.2s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.toc-item a {
  display: block;
  padding: 6px 8px;
  color: inherit;
  text-decoration: none;
  cursor: pointer;
}

.toc-item:hover a {
  background: rgba(26, 26, 26, 0.04);
  color: var(--ink-light);
}

.toc-item:hover {
  background: rgba(26, 26, 26, 0.04);
  color: var(--ink-light);
}

.toc-item.active {
  color: #5B7D6D;
  font-weight: 500;
}

.toc-item.level-3 {
  padding-left: 20px;
  font-size: 12px;
}

.toc-item.level-4 {
  padding-left: 32px;
  font-size: 12px;
}

@media (max-width: 900px) {
  .detail-layout {
    flex-direction: column;
  }
  
  .toc-sidebar {
    width: 100%;
    position: static;
    order: -1;
  }
}
</style>

<style>
html {
  scroll-behavior: smooth;
  scroll-padding-top: 88px;
}
</style>
