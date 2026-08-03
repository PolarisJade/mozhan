<script setup>
import { onMounted, ref, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getEssayDetail, deleteEssay } from '@/api/essay'
import { likeEssay, unlikeEssay } from '@/api/like'
import { useUserStore } from '@/stores/user'
import { InkMessage, InkMessageBox } from '@/utils/message'
import { redirectToLogin } from '@/utils/auth'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const likeLoading = ref(false)
const essay = ref(null)

const isLiked = computed(() => {
  return essay.value?.isLike === true || essay.value?.isLike === 'true'
})

async function loadDetail() {
  loading.value = true
  try {
    essay.value = await getEssayDetail(route.params.id)
  } finally {
    loading.value = false
  }
}

function goToAuthorProfile() {
  if (essay.value?.authorId) {
    router.push({ name: 'UserProfile', params: { userId: essay.value.authorId } })
  }
}

async function handleLike() {
  if (!userStore.isLoggedIn) {
    redirectToLogin()
    return
  }
  if (!essay.value || likeLoading.value) return
  likeLoading.value = true
  try {
    if (isLiked.value) {
      await unlikeEssay(essay.value.id)
      essay.value.isLike = false
      essay.value.likeCount = Math.max(0, (essay.value.likeCount || 0) - 1)
    } else {
      await likeEssay(essay.value.id)
      essay.value.isLike = true
      essay.value.likeCount = (essay.value.likeCount || 0) + 1
    }
  } finally {
    likeLoading.value = false
  }
}

async function handleDelete() {
  if (!essay.value) return
  try {
    await InkMessageBox.confirm('确定要删除这篇随笔吗？', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteEssay(essay.value.id)
    InkMessage.success('删除成功')
    router.push({ name: 'EssayHome' })
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
    }
  }
}

function goEdit() {
  router.push({ name: 'EssayEdit', params: { id: essay.value.id } })
}

function formatTime(timeStr) {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

onMounted(loadDetail)
watch(() => route.params.id, loadDetail)
</script>

<template>
  <div class="detail">
    <el-skeleton v-if="loading" :rows="8" animated />

    <article v-else-if="essay" class="detail-card ink-card">
      <div class="author-section">
        <div class="author-info" @click="goToAuthorProfile">
          <el-avatar :size="48" :src="essay.avatar || essay.authorAvatar">
            {{ essay.authorName?.[0] || '匿' }}
          </el-avatar>
          <div class="author-details">
            <span class="author-name">{{ essay.authorName || '匿名用户' }}</span>
            <span class="publish-time">{{ formatTime(essay.createTime) }}</span>
          </div>
        </div>
        <div v-if="userStore.isLoggedIn && userStore.user?.id === essay.authorId" class="author-actions">
          <button class="action-btn edit-btn" @click="goEdit">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
            </svg>
            编辑
          </button>
          <button class="action-btn delete-btn" @click="handleDelete">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <polyline points="3 6 5 6 21 6" />
              <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
            </svg>
            删除
          </button>
        </div>
      </div>

      <div class="content">
        <div class="content-text" v-html="essay.content"></div>
      </div>

      <div v-if="essay.tagVOList && essay.tagVOList.length" class="tags-section">
        <el-tag
          v-for="tag in essay.tagVOList"
          :key="tag.id"
          size="default"
          class="tag-item"
        >
          # {{ tag.name }}
        </el-tag>
      </div>

      <div class="actions-section">
        <button class="like-btn" :class="{ liked: isLiked }" @click="handleLike" :disabled="likeLoading">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z" />
          </svg>
          <span>{{ essay.likeCount || 0 }}</span>
        </button>
      </div>
    </article>

    <el-empty v-else description="随笔不存在或已删除" />
  </div>
</template>

<style scoped>
.detail {
  max-width: 720px;
  margin: 0 auto;
  padding: 0 20px;
}

.detail-card {
  padding: 32px;
}

.author-section {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.author-info :deep(.el-avatar) {
  border: 1px solid rgba(26, 26, 26, 0.1);
}

.author-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.author-name {
  font-size: 16px;
  font-weight: 500;
  color: var(--ink);
}

.publish-time {
  font-size: 13px;
  color: var(--ink-muted);
}

.author-actions {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: 13px;
  border: 1px solid rgba(26, 26, 26, 0.15);
  border-radius: 16px;
  background: transparent;
  color: var(--ink-light);
  cursor: pointer;
  transition: all 0.2s;
}

.action-btn svg {
  width: 14px;
  height: 14px;
}

.edit-btn:hover {
  border-color: var(--ink);
  color: var(--ink);
}

.delete-btn:hover {
  border-color: #e74c3c;
  color: #e74c3c;
}

.content {
  margin-bottom: 24px;
}

.content-text {
  font-size: 16px;
  line-height: 2;
  color: var(--ink);
  margin: 0;
  word-break: break-word;
}

.tags-section {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.tag-item {
  border: none;
  background: rgba(26, 26, 26, 0.06);
  color: var(--ink-muted);
  font-size: 13px;
}

.tag-item :deep(.el-tag__content) {
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.02em;
}

.actions-section {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

.like-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 24px;
  font-size: 14px;
  border: 1px solid rgba(26, 26, 26, 0.15);
  border-radius: 24px;
  background: transparent;
  color: var(--ink-light);
  cursor: pointer;
  transition: all 0.2s;
}

.like-btn svg {
  width: 20px;
  height: 20px;
  transition: all 0.2s;
}

.like-btn:hover:not(:disabled) {
  border-color: #e74c3c;
  color: #e74c3c;
}

.like-btn:hover:not(:disabled) svg {
  stroke: #e74c3c;
}

.like-btn.liked {
  border-color: #e74c3c;
  background: rgba(231, 76, 60, 0.08);
  color: #e74c3c;
}

.like-btn.liked svg {
  fill: #e74c3c;
  stroke: #e74c3c;
}

.like-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
