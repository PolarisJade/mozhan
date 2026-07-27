<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getEssayList } from '@/api/essay'
import { getTagList } from '@/api/tag'

const router = useRouter()
const loading = ref(false)
const essays = ref([])
const hasMore = ref(false)
const cursor = ref(null)
const tags = ref([])
const selectedTagIds = ref([])

async function loadTags() {
  try {
    tags.value = await getTagList() || []
  } catch (e) {
    console.error('加载标签失败', e)
  }
}

async function loadEssays(reset = false) {
  if (loading.value) return
  loading.value = true
  try {
    const query = {
      pageSize: 12,
      cursor: reset ? null : cursor.value,
    }
    if (selectedTagIds.value.length > 0) {
      query.tagIdList = selectedTagIds.value
    }
    const data = await getEssayList(query)
    const list = data?.list || []
    if (reset) {
      essays.value = list
    } else {
      essays.value.push(...list)
    }
    hasMore.value = !!data?.hasMore
    cursor.value = data?.nextCursor ?? null
  } catch (e) {
    console.error('加载随笔列表失败', e)
  } finally {
    loading.value = false
  }
}

function toggleTag(tagId) {
  const index = selectedTagIds.value.indexOf(tagId)
  if (index > -1) {
    selectedTagIds.value.splice(index, 1)
  } else {
    selectedTagIds.value.push(tagId)
  }
  cursor.value = null
  loadEssays(true)
}

function handleTagChange() {
  cursor.value = null
  loadEssays(true)
}

function getTagName(tagId) {
  const tag = tags.value.find(t => t.id === tagId)
  return tag?.name || ''
}

function removeTag(tagId) {
  const index = selectedTagIds.value.indexOf(tagId)
  if (index > -1) {
    selectedTagIds.value.splice(index, 1)
    cursor.value = null
    loadEssays(true)
  }
}

function clearTags() {
  if (selectedTagIds.value.length > 0) {
    selectedTagIds.value = []
    cursor.value = null
    loadEssays(true)
  }
}

function goDetail(id) {
  router.push({ name: 'EssayDetail', params: { id } })
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

onMounted(async () => {
  await loadTags()
  loadEssays(true)
})
</script>

<template>
  <div class="essay-home">
    <div class="main-content">
      <div class="header-cover"></div>
      <div class="fixed-tag-filter">
        <div class="tag-filter">
          <div class="filter-header">
            <span class="filter-label">标签筛选</span>
            <el-select
              v-model="selectedTagIds"
              multiple
              filterable
              placeholder="请选择标签"
              class="tag-select"
              @change="handleTagChange"
            >
              <el-option
                v-for="tag in tags"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
              />
            </el-select>
          </div>
          <div v-if="selectedTagIds.length > 0" class="selected-tags">
            <span class="selected-label">已选标签：</span>
            <div class="selected-tag-list">
              <span
                v-for="tagId in selectedTagIds"
                :key="tagId"
                class="selected-tag-chip"
              >
                {{ getTagName(tagId) }}
                <button class="remove-tag" @click="removeTag(tagId)">×</button>
              </span>
            </div>
            <button class="clear-btn" @click="clearTags">清除全部</button>
          </div>
        </div>
      </div>

      <el-skeleton v-if="loading && !essays.length" :rows="6" animated />

      <el-empty v-else-if="!essays.length" description="暂无随笔，静待分享" />

      <div v-else class="essay-grid">
        <article
          v-for="item in essays"
          :key="item.id"
          class="essay-card"
          @click="goDetail(item.id)"
        >
          <div class="essay-content">
            <div class="content-text" v-html="item.content"></div>
          </div>
          <div v-if="item.tagVOList && item.tagVOList.length" class="essay-tags">
            <span
              v-for="tag in item.tagVOList"
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
              {{ item.likeCount || 0 }}
            </span>
            <span class="create-time">{{ formatTime(item.createTime) }}</span>
            <button class="detail-btn">详情</button>
          </div>
        </article>
      </div>

      <div v-if="hasMore" class="load-more">
        <el-button class="ink-btn-plain" :loading="loading" @click="loadEssays()">
          加载更多
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.essay-home {
  width: 100%;
  min-height: 100vh;
  background: var(--paper);
}

.main-content {
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 100px 20px 32px;
  box-sizing: border-box;
}

.header-cover {
  position: fixed;
  top: 68px;
  left: 50%;
  transform: translateX(-50%);
  width: 100%;
  max-width: 1200px;
  height: 50px;
  z-index: 49;
  background: var(--paper);
}

.fixed-tag-filter {
  position: fixed;
  top: 82px;
  left: 50%;
  transform: translateX(-50%);
  max-width: 1130px;
  width: calc(100% - 200px);
  z-index: 50;
  padding-bottom: 8px;
}

.tag-filter {
  background: #fafafa;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 20px;
  border: 1px solid rgba(26, 26, 26, 0.06);
}

.filter-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
}

.filter-label {
  font-size: 14px;
  color: var(--ink-light);
  font-weight: 500;
}

.tag-select {
  flex: 1;
  max-width: 300px;
}

.tag-select :deep(.el-input__wrapper) {
  border-radius: 8px;
}

.selected-tags {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(26, 26, 26, 0.08);
}

.selected-label {
  font-size: 13px;
  color: var(--ink-muted);
}

.selected-tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}

.selected-tag-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  font-size: 13px;
  color: var(--ink);
  background: rgba(26, 26, 26, 0.08);
  border-radius: 16px;
}

.remove-tag {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  font-size: 14px;
  color: #999;
  background: transparent;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.2s;
}

.remove-tag:hover {
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.1);
}

.clear-btn {
  font-size: 12px;
  color: #999;
  background: transparent;
  border: none;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.2s;
}

.clear-btn:hover {
  color: #e74c3c;
  background: rgba(231, 76, 60, 0.08);
}

.essay-grid {
  column-count: 3;
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

.load-more {
  text-align: center;
  padding: 40px 0;
}

.ink-btn-plain {
  padding: 10px 30px;
  border: 1px solid var(--ink);
  background: transparent;
  color: var(--ink);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-family: 'Noto Serif SC', 'ZCOOL XiaoWei', serif;
  letter-spacing: 0.05em;
}

.ink-btn-plain:hover {
  background: var(--ink);
  color: var(--paper);
}

@media (max-width: 1024px) {
  .essay-grid {
    column-count: 2;
  }
}

@media (max-width: 768px) {
  .essay-grid {
    column-count: 1;
  }
}
</style>
