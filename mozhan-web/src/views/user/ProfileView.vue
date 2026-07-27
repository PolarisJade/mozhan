﻿﻿﻿﻿<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { InkMessage, InkMessageBox } from '@/utils/message'
import { updateUserInfo, updatePassword, getUserProfile } from '@/api/user'
import { uploadAvatar } from '@/api/upload'
import { getMyArticleList, publishArticle, deleteArticle } from '@/api/article'
import { getMyComments } from '@/api/comment'
import { getMyEssayList } from '@/api/essay'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()
const pageLoading = ref(false)
const articlesLoading = ref(false)
const profileLoading = ref(false)
const pwdLoading = ref(false)
const profileFormRef = ref()
const pwdFormRef = ref()

const profileForm = reactive({
  nickname: '',
  avatar: '',
  intro: '',
  email: '',
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const articles = ref([])
const essays = ref([])
const userStats = ref({
  followingCount: 0,
  followerCount: 0,
  articleCount: 0,
})

const activeMainTab = ref('articles')  // articles, essays, comments
const activeArticleTab = ref('all')  // all, published, draft
const showEditModal = ref(false)
const showPwdModal = ref(false)

const articleTabs = [
  { key: 'all', label: '全部' },
  { key: 'published', label: '已发布' },
  { key: 'draft', label: '草稿' },
]

const comments = ref([])
const commentsLoading = ref(false)
const essaysLoading = ref(false)

const profileRules = {
  nickname: [{ max: 20, message: '昵称不超过 20 个字符', trigger: 'blur' }],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
  intro: [{ max: 200, message: '简介不超过 200 个字符', trigger: 'blur' }],
}

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度为 6-20 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== pwdForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function fillForm(user) {
  profileForm.nickname = user?.nickname || ''
  profileForm.avatar = user?.avatar || ''
  profileForm.intro = user?.intro || ''
  profileForm.email = user?.email || ''
  userStats.value.followingCount = user?.followingCount || 0
  userStats.value.followerCount = user?.followerCount || 0
  userStats.value.articleCount = user?.articleCount || 0
}

async function loadProfile() {
  pageLoading.value = true
  try {
    const user = await getUserProfile(userStore.user?.id)
    fillForm(user)
    await loadArticles()
  } finally {
    pageLoading.value = false
  }
}

async function loadArticles(status = '') {
  articlesLoading.value = true
  try {
    const query = {}
    if (status && status !== 'all') {
      query.status = status.toUpperCase()
    }
    const data = await getMyArticleList(query, userStore.user?.id)
    articles.value = data.list || []
  } finally {
    articlesLoading.value = false
  }
}

async function loadEssays() {
  essaysLoading.value = true
  try {
    const query = {
      pageSize: 20,
    }
    const data = await getMyEssayList(userStore.user?.id, query)
    essays.value = data.list || []
  } finally {
    essaysLoading.value = false
  }
}

async function loadComments() {
  commentsLoading.value = true
  try {
    const data = await getMyComments({ pageSize: 50 })
    comments.value = data.list || []
  } finally {
    commentsLoading.value = false
  }
}

function switchMainTab(tab) {
  activeMainTab.value = tab
  if (tab === 'articles') {
    loadArticles(activeArticleTab.value)
  } else if (tab === 'essays') {
    loadEssays()
  } else {
    loadComments()
  }
}

function switchArticleTab(tab) {
  activeArticleTab.value = tab
  loadArticles(tab)
}

async function onSaveProfile() {
  await profileFormRef.value.validate()
  profileLoading.value = true
  try {
    await updateUserInfo({ ...profileForm })
    userStore.updateLocalUser({ ...profileForm })
    InkMessage.success('资料保存成功')
    showEditModal.value = false
  } finally {
    profileLoading.value = false
  }
}

async function onChangePassword() {
  await pwdFormRef.value.validate()
  pwdLoading.value = true
  try {
    await updatePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
    })
    InkMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirmPassword = ''
    pwdFormRef.value.resetFields()
    showPwdModal.value = false
  } finally {
    pwdLoading.value = false
  }
}

async function handleAvatarUpload(options) {
  try {
    const data = await uploadAvatar(options.file)
    profileForm.avatar = data.url
    InkMessage.success('头像上传成功')
    options.onSuccess?.(data)
  } catch (err) {
    options.onError?.(err)
  }
}

function beforeAvatarUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isImage) {
    InkMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    InkMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'comments') {
    loadComments()
  } else {
    loadArticles(tab)
  }
}

function goArticle(articleId) {
  router.push({ name: 'ArticleDetail', params: { id: articleId } })
}

function goEssay(essayId) {
  router.push({ name: 'EssayDetail', params: { id: essayId } })
}

function goEdit(articleId) {
  router.push({ name: 'ArticleEdit', params: { id: articleId } })
}

async function goPublish(articleId) {
  try {
    await publishArticle(articleId)
    InkMessage.success('发布成功')
    loadArticles(activeArticleTab.value)
  } catch (e) {
    InkMessage.error('发布失败')
  }
}

async function handleDelete(articleId) {
  try {
    await InkMessageBox.confirm('确定要删除这篇文章吗？', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteArticle(articleId)
    InkMessage.success('删除成功')
    loadArticles(activeArticleTab.value)
  } catch (e) {
    if (e !== 'cancel') {
      InkMessage.error('删除失败')
    }
  }
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const now = new Date()
  const diff = now - date
  const months = Math.floor(diff / (1000 * 60 * 60 * 24 * 30))
  
  if (months > 0) {
    return `${months}个月前`
  }
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  if (days > 0) {
    return `${days}天前`
  }
  return '今天'
}

function goBack() {
  router.back()
}

onMounted(loadProfile)
</script>

<template>
  <div class="profile-page" v-loading="pageLoading">
    <h2 class="ink-page-title">个人简介</h2>

    <div class="profile-card ink-card">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回
      </button>
      <div class="profile-header">
        <el-avatar :size="100" :src="profileForm.avatar">
          {{ profileForm.nickname?.[0] || userStore.user?.username?.[0] }}
        </el-avatar>
        <div class="profile-info">
          <div class="info-header">
            <h3 class="font-display">{{ profileForm.nickname || userStore.user?.username }}</h3>
            <div class="edit-actions">
              <button class="edit-btn" @click="showEditModal = true">
                <svg class="edit-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
                </svg>
                编辑资料
              </button>
              <button class="pwd-btn" @click="showPwdModal = true">
                <svg class="pwd-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                </svg>
                修改密码
              </button>
            </div>
          </div>
          <div class="stats-row">
            <span class="stat">
              <span class="stat-value">{{ userStats.followingCount }}</span>
              <span class="stat-label">关注</span>
            </span>
            <span class="stat-divider">|</span>
            <span class="stat">
              <span class="stat-value">{{ userStats.followerCount }}</span>
              <span class="stat-label">粉丝</span>
            </span>
            <span class="stat-divider">|</span>
            <span class="stat">
              <span class="stat-value">{{ userStats.articleCount }}</span>
              <span class="stat-label">作品</span>
            </span>
          </div>
        </div>
      </div>

      <div class="profile-intro">
        <p>{{ profileForm.intro || '这个人很懒，什么都没写' }}</p>
      </div>
    </div>

    <div class="content-section">
      <div class="main-tabs">
        <button 
          class="main-tab-btn"
          :class="{ active: activeMainTab === 'articles' }"
          @click="switchMainTab('articles')"
        >
          作品
        </button>
        <button 
          class="main-tab-btn"
          :class="{ active: activeMainTab === 'essays' }"
          @click="switchMainTab('essays')"
        >
          随笔
        </button>
        <button 
          class="main-tab-btn"
          :class="{ active: activeMainTab === 'comments' }"
          @click="switchMainTab('comments')"
        >
          评论
        </button>
      </div>

      <div v-if="activeMainTab === 'articles'" class="articles-section">
        <div class="article-sub-tabs">
          <button 
            v-for="tab in articleTabs" 
            :key="tab.key"
            class="sub-tab-btn"
            :class="{ active: activeArticleTab === tab.key }"
            @click="switchArticleTab(tab.key)"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="articles-list" v-loading="articlesLoading">
          <div v-if="articles.length > 0" class="article-items">
            <article
              v-for="article in articles"
              :key="article.id"
              class="article-card ink-card"
              @click="goArticle(article.id)"
            >
              <div class="article-header">
                <h3>{{ article.title }}</h3>
                <span :class="['status-tag', article.status === '草稿' ? 'draft' : 'published']">
                  {{ article.status === '草稿' ? '草稿' : '已发布' }}
                </span>
              </div>
              <p class="summary">{{ article.summary || '暂无摘要' }}</p>
              <div class="meta ink-meta">
                <span>{{ article.readCount }} 阅读</span>
                <span class="meta-divider">·</span>
                <span>{{ article.likeCount }} 点赞</span>
              </div>
              <div class="article-actions">
                <button class="action-btn edit-btn" @click.stop="goEdit(article.id)">
                  <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"/>
                  </svg>
                  <span>编辑</span>
                </button>
                <button v-if="article.status === '草稿'" class="action-btn publish-btn" @click.stop="goPublish(article.id)">
                  <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17 8 12 3 7 8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                  <span>发布</span>
                </button>
                <button class="action-btn delete-btn" @click.stop="handleDelete(article.id)">
                  <svg class="action-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M19 7l-.867 12.142A2 2 0 0 1 16.138 21H7.862a2 2 0 0 1-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v3M4 7h16"/>
                  </svg>
                  <span>删除</span>
                </button>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无作品" />
        </div>
      </div>

      <div v-else-if="activeMainTab === 'essays'" class="essays-section">
        <div class="essays-list" v-loading="essaysLoading">
          <div v-if="essays.length > 0" class="essay-grid">
            <article
              v-for="essay in essays"
              :key="essay.id"
              class="essay-card"
              @click="goEssay(essay.id)"
            >
              <div class="essay-content">
                <div class="content-text" v-html="essay.content"></div>
              </div>
              <div v-if="essay.tagVOList && essay.tagVOList.length" class="essay-tags">
                <span
                  v-for="tag in essay.tagVOList"
                  :key="tag.id"
                  class="tag-item"
                >
                  {{ tag.name }}
                </span>
              </div>
              <div class="essay-footer">
                <span class="like-count">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
                  </svg>
                  {{ essay.likeCount || 0 }}
                </span>
                <span class="create-time">{{ formatTime(essay.createTime) }}</span>
                <button class="detail-btn">详情</button>
              </div>
            </article>
          </div>
          <el-empty v-else description="暂无随笔" />
        </div>
      </div>

      <div v-else class="comments-section">
        <div class="comments-list" v-loading="commentsLoading">
          <div v-if="comments.length > 0" class="comment-items">
            <div
              v-for="comment in comments"
              :key="comment.id"
              class="comment-item ink-card"
              @click="goArticle(comment.articleId)"
            >
              <div class="comment-article-title">评论文章：{{ comment.articleTitle }}</div>
              <p class="comment-content">{{ comment.content }}</p>
              <div class="comment-meta ink-meta">
                <span>{{ comment.createTime }}</span>
                <span class="meta-divider">·</span>
                <span>{{ comment.likeCount || 0 }} 点赞</span>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无评论" />
        </div>
      </div>
    </div>
  </div>

  <el-dialog title="编辑个人资料" v-model="showEditModal" width="500px" :close-on-click-modal="false">
    <el-form ref="profileFormRef" :model="profileForm" :rules="profileRules" label-width="80px">
      <el-form-item label="昵称" prop="nickname">
        <el-input v-model="profileForm.nickname" placeholder="请输入昵称" />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="profileForm.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="简介" prop="intro">
        <el-input type="textarea" v-model="profileForm.intro" placeholder="请输入个人简介" :rows="3" />
      </el-form-item>
      <el-form-item label="头像">
        <el-upload
          class="avatar-uploader"
          :action="''"
          :show-file-list="false"
          :before-upload="beforeAvatarUpload"
          :http-request="handleAvatarUpload"
        >
          <el-avatar :size="100" :src="profileForm.avatar">
            {{ profileForm.nickname?.[0] || '头' }}
          </el-avatar>
        </el-upload>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="showEditModal = false">取消</el-button>
        <el-button type="primary" @click="onSaveProfile" :loading="profileLoading">保存</el-button>
      </span>
    </template>
  </el-dialog>

  <el-dialog title="修改密码" v-model="showPwdModal" width="400px" :close-on-click-modal="false">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="100px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input type="password" v-model="pwdForm.oldPassword" placeholder="请输入原密码" />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input type="password" v-model="pwdForm.newPassword" placeholder="请输入新密码" />
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword">
        <el-input type="password" v-model="pwdForm.confirmPassword" placeholder="请再次输入新密码" />
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="showPwdModal = false">取消</el-button>
        <el-button type="primary" @click="onChangePassword" :loading="pwdLoading">确认修改</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped>
.profile-page {
  max-width: 880px;
  margin: 0 auto;
}

.profile-card {
  padding: 32px;
  margin-bottom: 24px;
}

.profile-header {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.profile-header :deep(.el-avatar) {
  border: 2px solid rgba(26, 26, 26, 0.08);
}

.profile-info {
  flex: 1;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.profile-info h3 {
  margin: 0;
  font-size: 28px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.08em;
}

.edit-actions {
  display: flex;
  gap: 12px;
}

.edit-actions .edit-btn,
.edit-actions .pwd-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 13px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.edit-actions .edit-btn {
  background: rgba(26, 26, 26, 0.06);
  color: var(--ink-muted);
}

.edit-actions .edit-btn:hover {
  background: rgba(26, 26, 26, 0.1);
  color: var(--ink);
}

.edit-actions .pwd-btn {
  background: rgba(44, 24, 16, 0.08);
  color: #2c1810;
}

.edit-actions .pwd-btn:hover {
  background: rgba(44, 24, 16, 0.12);
}

.edit-icon,
.pwd-icon {
  width: 14px;
  height: 14px;
}

.stats-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat-label {
  font-size: 12px;
  color: var(--ink-muted);
  letter-spacing: 0.1em;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 20px;
  font-weight: 600;
  color: var(--ink);
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
}

.stat-divider {
  color: rgba(26, 26, 26, 0.15);
}

.profile-intro {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(26, 26, 26, 0.08);
}

.profile-intro p {
  margin: 0;
  line-height: 1.8;
  color: var(--ink-light);
  font-size: 15px;
  text-indent: 2em;
}

.content-section {
  margin-top: 24px;
}

.main-tabs {
  display: flex;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
  margin-bottom: 20px;
}

.main-tab-btn {
  position: relative;
  padding: 12px 24px;
  font-size: 16px;
  font-family: 'Noto Serif SC', serif;
  color: var(--ink-light);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.25s ease;
  letter-spacing: 0.1em;
  margin-right: 32px;
}

.main-tab-btn:hover {
  color: var(--ink-muted);
}

.main-tab-btn.active {
  color: var(--ink);
}

.main-tab-btn.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--ink);
}

.articles-section {
  margin-bottom: 24px;
}

.article-sub-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.sub-tab-btn {
  padding: 8px 20px;
  font-size: 14px;
  font-family: 'Noto Serif SC', serif;
  color: var(--ink-light);
  background: transparent;
  border: 1px solid rgba(74, 74, 74, 0.15);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.25s ease;
  letter-spacing: 0.08em;
}

.sub-tab-btn:hover {
  border-color: var(--ink-muted);
  color: var(--ink-muted);
}

.sub-tab-btn.active {
  background: var(--ink);
  border-color: var(--ink);
  color: #fff;
}

.articles-list {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.article-card {
  display: flex;
  flex-direction: column;
  padding: 20px 24px;
  cursor: pointer;
  transition: all 0.2s ease;
  border-bottom: 1px solid #f0f0f0;
  position: relative;
}

.article-card:last-child {
  border-bottom: none;
}

.article-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  width: 3px;
  height: 100%;
  background: transparent;
  transition: all 0.2s ease;
}

.article-card:hover {
  transform: translateY(-2px);
}

.article-card:hover::before {
  background: #1a1a1a;
}

.article-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.article-header h3 {
  font-size: 17px;
  font-weight: 500;
  color: #1a1a1a;
  margin: 0;
  flex: 1;
  line-height: 1.5;
}

.summary {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 10px 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.article-status {
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 12px;
  letter-spacing: 0.08em;
}

.article-status.published {
  background: rgba(46, 125, 50, 0.1);
  color: #2e7d32;
}

.article-status.draft {
  background: rgba(121, 85, 72, 0.1);
  color: #795548;
}

.article-summary {
  margin: 0 0 12px;
  font-size: 14px;
  color: var(--ink-light);
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.meta-divider {
  color: rgba(26, 26, 26, 0.15);
}

.status-tag {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 8px;
}

.status-tag.draft {
  background: rgba(121, 85, 72, 0.1);
  color: #795548;
}

.status-tag.published {
  background: rgba(76, 175, 80, 0.1);
  color: #4caf50;
}

.article-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(26, 26, 26, 0.06);
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn .action-icon {
  width: 14px;
  height: 14px;
}

.edit-btn {
  background: rgba(26, 26, 26, 0.04);
  color: var(--ink-muted);
}

.edit-btn:hover {
  background: rgba(26, 26, 26, 0.08);
  color: var(--ink);
}

.publish-btn {
  background: var(--ink-primary);
  color: #fff;
}

.publish-btn:hover {
  background: var(--ink-primary-dark);
}

.delete-btn {
  background: rgba(229, 57, 53, 0.08);
  color: #e53935;
}

.delete-btn:hover {
  background: rgba(229, 57, 53, 0.15);
}

.essays-list {
  display: flex;
  flex-direction: column;
}

.essay-grid {
  column-count: 2;
  column-gap: 20px;
}

.essay-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #f0f0f0;
  break-inside: avoid;
  margin-bottom: 20px;
}

.essay-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

.essay-content {
  margin-bottom: 16px;
}

.content-text {
  font-size: 15px;
  line-height: 1.8;
  color: var(--ink);
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.essay-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.tag-item {
  padding: 4px 12px;
  background: rgba(26, 26, 26, 0.06);
  color: #666;
  font-size: 12px;
  border-radius: 12px;
}

.essay-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid rgba(26, 26, 26, 0.06);
}

.like-count {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #999;
}

.like-count svg {
  width: 14px;
  height: 14px;
}

.create-time {
  font-size: 12px;
  color: #999;
}

.detail-btn {
  padding: 4px 12px;
  font-size: 12px;
  color: #666;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
}

.detail-btn:hover {
  color: var(--ink);
}

.comments-list {
  display: flex;
  flex-direction: column;
}

.comment-item {
  padding: 20px 24px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  margin-bottom: 32px;
}

.comment-item:last-child {
  margin-bottom: 0;
}

.comment-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(26, 26, 26, 0.08);
}

.comment-article-title {
  font-size: 13px;
  color: var(--ink-muted);
  margin-bottom: 10px;
  letter-spacing: 0.04em;
}

.comment-content {
  margin: 0 0 12px;
  font-size: 15px;
  color: var(--ink);
  line-height: 1.7;
}

.comment-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px 8px 0;
  font-size: 14px;
  color: var(--ink-light);
  background: transparent;
  border: none;
  cursor: pointer;
  transition: color 0.2s;
  margin-bottom: 8px;
}

.back-btn:hover {
  color: var(--ink);
}

.back-btn svg {
  width: 16px;
  height: 16px;
}
</style>
