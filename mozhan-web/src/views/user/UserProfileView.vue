<script setup>
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { InkMessage } from '@/utils/message'
import { getUserProfile, followUser, unfollowUser } from '@/api/user'
import { getMyArticleList } from '@/api/article'

import { getMyEssayList } from '@/api/essay'
import { useUserStore } from '@/stores/user'
import { redirectToLogin } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function goArticleDetail(id) {
  router.push({ name: 'ArticleDetail', params: { id } })
}

function goEssayDetail(id) {
  router.push({ name: 'EssayDetail', params: { id } })
}

const loading = ref(false)
const articlesLoading = ref(false)
const essaysLoading = ref(false)
const followLoading = ref(false)
const profile = ref(null)
const articles = ref([])
const essays = ref([])
const isOwnProfile = ref(false)
const activeTab = ref('articles')  // articles, essays

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await getUserProfile(route.params.userId)
    checkOwnProfile()
    await loadArticles()
  } finally {
    loading.value = false
  }
}

async function loadArticles() {
  articlesLoading.value = true
  try {
    const userId = route.params.userId
    const query = {
      pageSize: 20,
      isAsc: false,
      sortBy: 'create_time',
    }
    
    if (!isOwnProfile.value) {
      query.status = 'PUBLISHED'
    }
    
    const data = await getMyArticleList(query, userId)
    articles.value = data.list || []
  } finally {
    articlesLoading.value = false
  }
}

async function loadEssays() {
  essaysLoading.value = true
  try {
    const userId = route.params.userId
    const query = {
      pageSize: 20,
    }
    const data = await getMyEssayList(userId, query)
    essays.value = data.list || []
  } finally {
    essaysLoading.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'essays') {
    loadEssays()
  } else {
    loadArticles()
  }
}

function checkOwnProfile() {
  isOwnProfile.value = userStore.isLoggedIn && userStore.user?.id === Number(route.params.userId)
}

function showFollowButton() {
  if (!userStore.isLoggedIn || isOwnProfile.value) return false
  return profile.value?.isFollowed !== null
}

async function toggleFollow() {
  if (!userStore.isLoggedIn) {
    redirectToLogin()
    return
  }
  
  followLoading.value = true
  try {
    if (profile.value.isFollowed) {
      await unfollowUser(route.params.userId)
      profile.value.isFollowed = false
      if (profile.value) {
        profile.value.followerCount = (profile.value.followerCount || 0) - 1
      }
      InkMessage.success('已取消关注')
    } else {
      await followUser(route.params.userId)
      profile.value.isFollowed = true
      if (profile.value) {
        profile.value.followerCount = (profile.value.followerCount || 0) + 1
      }
      InkMessage.success('关注成功')
    }
  } finally {
    followLoading.value = false
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
watch(() => route.params.userId, loadProfile)
</script>

<template>
  <div class="user-profile" v-loading="loading">
    <h2 class="ink-page-title">个人简介</h2>

    <div v-if="profile" class="profile-card ink-card">
      <button class="back-btn" @click="goBack">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
        返回
      </button>
      <div class="profile-header">
        <el-avatar :size="100" :src="profile.avatar">
          {{ profile.nickname?.[0] || profile.username?.[0] }}
        </el-avatar>
        <div class="profile-info">
          <h3 class="font-display">{{ profile.nickname || profile.username }}</h3>
          <div class="stats-row">
            <span class="stat">
              <span class="stat-value">{{ profile.followingCount ?? 0 }}</span>
              <span class="stat-label">关注</span>
            </span>
            <span class="stat-divider">|</span>
            <span class="stat">
              <span class="stat-value">{{ profile.followerCount ?? 0 }}</span>
              <span class="stat-label">粉丝</span>
            </span>
            <span class="stat-divider">|</span>
            <span class="stat">
              <span class="stat-value">{{ articles.length }}</span>
              <span class="stat-label">作品</span>
            </span>
          </div>
        </div>
        <div class="profile-actions">
          <el-button
            v-if="showFollowButton()"
            :loading="followLoading"
            :class="['follow-btn', { 'is-following': profile.isFollowed }]"
            @click="toggleFollow"
          >
            {{ profile.isFollowed ? '已关注' : '+ 关注' }}
          </el-button>
        </div>
      </div>

      <div class="profile-intro">
        <p>{{ profile.intro || '这个人很懒，什么都没写' }}</p>
      </div>
    </div>

    <div class="content-section" v-if="profile">
      <div class="main-tabs">
        <button 
          class="main-tab-btn"
          :class="{ active: activeTab === 'articles' }"
          @click="switchTab('articles')"
        >
          作品
        </button>
        <button 
          class="main-tab-btn"
          :class="{ active: activeTab === 'essays' }"
          @click="switchTab('essays')"
        >
          随笔
        </button>
      </div>

      <div v-if="activeTab === 'articles'" class="articles-section">
        <div class="articles-list" v-loading="articlesLoading">
          <div v-if="articles.length > 0" class="article-items">
            <article
              v-for="article in articles"
              :key="article.id"
              class="article-card ink-card"
              @click="goArticleDetail(article.id)"
            >
              <div class="article-header">
                <h3>{{ article.title }}</h3>
              </div>
              <p class="summary">{{ article.summary || '暂无摘要' }}</p>
              <div class="meta ink-meta">
                <span>{{ article.createTime }}</span>
                <span class="meta-divider">·</span>
                <span>{{ article.readCount }} 阅读</span>
                <span class="meta-divider">·</span>
                <span>{{ article.likeCount }} 点赞</span>
              </div>
            </article>
          </div>
        <el-empty v-else description="暂无文章" />
        </div>
      </div>

      <div v-else-if="activeTab === 'essays'" class="essays-section">
        <div class="essays-list" v-loading="essaysLoading">
          <div v-if="essays.length > 0" class="essay-grid">
            <article
              v-for="essay in essays"
              :key="essay.id"
              class="essay-card"
              @click="goEssayDetail(essay.id)"
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


    </div>

    <el-empty v-else-if="!loading" description="用户不存在" />
  </div>
</template>

<style scoped>
.user-profile {
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

.profile-header :deep(.ant-avatar) {
  border: 2px solid rgba(26, 26, 26, 0.08);
}

.profile-info {
  flex: 1;
}

.profile-info h3 {
  margin: 0 0 16px;
  font-size: 28px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.08em;
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

.profile-actions {
  display: flex;
  align-items: flex-start;
}

.follow-btn {
  padding: 8px 20px;
  font-size: 14px;
  letter-spacing: 0.08em;
  border-radius: 20px;
  border: 1px solid var(--ink);
  background: transparent;
  color: var(--ink);
  transition: all 0.2s;
}

.follow-btn:hover {
  background: var(--ink);
  color: #fff;
}

.follow-btn.is-following {
  background: rgba(26, 26, 26, 0.06);
  border-color: rgba(26, 26, 26, 0.15);
  color: var(--ink-light);
}

.follow-btn.is-following:hover {
  background: rgba(239, 68, 68, 0.1);
  border-color: rgba(239, 68, 68, 0.3);
  color: #ef4444;
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
  font-size: 20px;
  font-weight: 700;
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

.meta-divider {
  color: rgba(26, 26, 26, 0.15);
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
  margin-bottom: 1px;
}

.back-btn:hover {
  color: var(--ink);
}

.back-btn svg {
  width: 16px;
  height: 16px;
}
</style>
