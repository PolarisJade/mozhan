<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getDiaryList, deleteDiary, getDiaryDetail } from '@/api/diary'
import { useUserStore } from '@/stores/user'
import { redirectToLogin } from '@/utils/auth'
import { InkMessage, InkMessageBox } from '@/utils/message'
import WeatherIcon from '@/components/WeatherIcon.vue'

const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const diaries = ref([])
const pageNo = ref(0)
const pageSize = 20
const total = ref(0)
const totalPages = ref(1)
const initialLoading = ref(true)
const selectedId = ref(null)
const detail = ref(null)
const detailLoading = ref(false)
const listScrollRef = ref(null)
const sentinelRef = ref(null)
let observer = null

// 按月份分组
const groupedDiaries = computed(() => {
  const map = new Map()
  for (const d of diaries.value) {
    const key = (d.diaryDate || '').slice(0, 7)
    if (!map.has(key)) {
      const [y, m] = key.split('-').map(Number)
      map.set(key, {
        key,
        label: `${y}年${m}月`,
        count: 0,
        items: [],
      })
    }
    const g = map.get(key)
    g.items.push(d)
    g.count += 1
  }
  return Array.from(map.values())
})

function parseMonthDay(dateStr) {
  if (!dateStr) return { month: '', day: '', year: '', monthEn: '' }
  const [y, m, d] = dateStr.split('-')
  const monthEn = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC']
  return {
    year: y,
    month: m,
    day: d,
    monthEn: monthEn[Number(m) - 1] || '',
  }
}

async function loadDiaries(reset = false) {
  if (loading.value) return
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后查看日记')
    return
  }
  loading.value = true
  try {
    const cur = reset ? 1 : pageNo.value + 1
    const data = await getDiaryList({
      pageNum: cur,
      pageSize,
      isAsc: false,
    })
    const record = data?.records?.[0]
    const list = record?.diaryList || []
    if (reset) {
      diaries.value = list
    } else {
      diaries.value.push(...list)
    }
    pageNo.value = data?.pageNo || cur
    total.value = data?.total || 0
    totalPages.value = data?.pages || 1
  } catch (e) {
    console.error('加载日记列表失败', e)
  } finally {
    loading.value = false
    initialLoading.value = false
  }
}

function setupObserver() {
  if (observer) observer.disconnect()
  if (!sentinelRef.value) return
  observer = new IntersectionObserver(
    (entries) => {
      if (entries[0].isIntersecting && pageNo.value < totalPages.value && !loading.value) {
        loadDiaries()
      }
    },
    {
      root: listScrollRef.value,
      rootMargin: '60px',
    },
  )
  observer.observe(sentinelRef.value)
}

async function selectItem(item) {
  selectedId.value = item.id
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getDiaryDetail(item.id)
  } catch (e) {
    console.error('加载详情失败', e)
  } finally {
    detailLoading.value = false
  }
}

function goWrite() {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后再写日记')
    return
  }
  router.push({ name: 'DiaryWrite' })
}

function goEdit() {
  if (!detail.value?.id) return
  router.push({ name: 'DiaryEdit', params: { id: detail.value.id } })
}

async function handleDelete() {
  if (!detail.value?.id) return
  try {
    await InkMessageBox.confirm('确定要删除这篇日记吗？删除后不可恢复。', '删除确认', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    const id = detail.value.id
    await deleteDiary(id)
    diaries.value = diaries.value.filter((d) => d.id !== id)
    total.value = Math.max(0, total.value - 1)
    InkMessage.success('删除成功')
    detail.value = null
    selectedId.value = null
  } catch (e) {
    if (e !== 'cancel') {
      console.error('删除失败', e)
    }
  }
}

function formatWeekday(dateStr) {
  if (!dateStr) return ''
  const [y, m, d] = dateStr.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  const week = ['日', '一', '二', '三', '四', '五', '六']
  return `周${week[date.getDay()]}`
}

onMounted(async () => {
  await loadDiaries(true)
  if (diaries.value.length) {
    selectItem(diaries.value[0])
  }
  await nextTick()
  setupObserver()
})

onUnmounted(() => {
  if (observer) observer.disconnect()
})

watch(totalPages, async () => {
  await nextTick()
  setupObserver()
})
</script>

<template>
  <div class="diary-home">
    <div class="diary-frame">
      <!-- 左侧：列表 -->
      <aside class="list-pane">
        <div class="list-header">
          <h2 class="list-title">时光小记</h2>
          <span class="list-count">共 {{ total }} 篇</span>
        </div>
        <div class="list-scroll" ref="listScrollRef">
          <div v-if="initialLoading" class="list-skeleton">
            <el-skeleton :rows="6" animated />
          </div>
          <el-empty v-else-if="!diaries.length" description="还没有日记" />
          <template v-else>
            <div v-for="group in groupedDiaries" :key="group.key" class="month-group">
              <div class="month-header">
                <span class="month-label">{{ group.label }}</span>
                <span class="month-meta">{{ group.count }}篇</span>
              </div>
              <div
                v-for="item in group.items"
                :key="item.id"
                class="list-item"
                :class="{ active: selectedId === item.id }"
                @click="selectItem(item)"
              >
                <span class="item-date">
                  <span class="item-month">{{ parseMonthDay(item.diaryDate).month }}-</span>
                  <span class="item-day">{{ parseMonthDay(item.diaryDate).day }}</span>
                </span>
                <span class="item-weekday">{{ formatWeekday(item.diaryDate) }}</span>
                <span class="item-weather">
                  <WeatherIcon :type="item.weather" />
                  <span>{{ item.weather || '' }}</span>
                </span>
              </div>
            </div>
            <div ref="sentinelRef" class="list-sentinel">
              <span v-if="loading">加载中...</span>
              <span v-else-if="pageNo >= totalPages">已加载全部</span>
            </div>
          </template>
        </div>
      </aside>

      <!-- 右侧：详情 -->
      <section class="detail-pane">
        <div v-if="!detail && !detailLoading" class="detail-empty">
          <el-empty description="选择左侧任意一篇日记查看" />
        </div>
        <div v-else-if="detailLoading" class="detail-skeleton">
          <el-skeleton :rows="10" animated />
        </div>
        <article v-else class="detail-card">
          <header class="detail-header">
            <div class="date-block">
              <span class="date-num">{{ parseMonthDay(detail.diaryDate).day }}</span>
              <span class="date-right">
                <span class="date-month-en">{{ parseMonthDay(detail.diaryDate).monthEn }}</span>
                <span class="date-year">{{ parseMonthDay(detail.diaryDate).year }}</span>
              </span>
            </div>
            <div class="detail-actions">
              <button class="icon-btn" @click="goEdit" title="编辑">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
                </svg>
              </button>
              <button class="icon-btn danger" @click="handleDelete" title="删除">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="3 6 5 6 21 6" />
                  <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
                </svg>
              </button>
            </div>
          </header>
          <div class="detail-info">
            <span class="info-item">
              <svg class="info-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="5" width="18" height="16" rx="2" />
                <path d="M3 9h18M8 3v4M16 3v4" />
              </svg>
              <span>{{ detail.diaryDate }}</span>
            </span>
            <span class="info-divider">·</span>
            <span class="info-item">{{ formatWeekday(detail.diaryDate) }}</span>
            <span class="info-divider">·</span>
            <span class="info-item info-weather">
              <WeatherIcon :type="detail.weather" />
              <span>{{ detail.weather || '未记天气' }}</span>
            </span>
          </div>
          <div class="detail-content" v-html="detail.content"></div>
        </article>
      </section>
    </div>

    <!-- 浮动写日记按钮 -->
    <button class="floating-write" @click="goWrite" title="写日记">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
        <path d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
      </svg>
    </button>
  </div>
</template>

<style scoped>
.diary-home {
  width: 100%;
  background: var(--paper);
  display: flex;
  flex-direction: column;
}
.diary-frame {
  flex: 1;
  display: flex;
  max-width: 1240px;
  width: 100%;
  margin: 0 auto;
  background: var(--paper-card);
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 6px;
  box-shadow: 0 4px 24px rgba(26, 26, 26, 0.05);
  overflow: hidden;
  height: calc(100vh - 68px - 12px);
}
/* 左侧列表 */
.list-pane {
  width: 360px;
  flex-shrink: 0;
  border-right: 1px solid rgba(26, 26, 26, 0.06);
  display: flex;
  flex-direction: column;
  background: #fdfaf4;
}
.list-header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 22px 24px 16px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.06);
}
.list-title {
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 400;
  color: var(--ink);
  margin: 0;
  letter-spacing: 0.12em;
}
.list-count {
  font-size: 12px;
  color: var(--ink-muted);
  letter-spacing: 0.05em;
}
.list-scroll {
  flex: 1;
  overflow-y: auto;
  scrollbar-gutter: stable;
  padding: 6px 0 20px;
}
.list-scroll::-webkit-scrollbar { width: 6px; }
.list-scroll::-webkit-scrollbar-thumb { background: rgba(26, 26, 26, 0.12); border-radius: 3px; }
.list-scroll::-webkit-scrollbar-track { background: transparent; }
.list-skeleton { padding: 20px 24px; }
.month-group { padding: 14px 0 6px; }
.month-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 4px 24px 8px;
}
.month-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink);
  letter-spacing: 0.05em;
}
.month-meta {
  font-size: 11px;
  color: var(--ink-muted);
  letter-spacing: 0.03em;
}
.list-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 9px 24px;
  cursor: pointer;
  transition: background-color 0.18s ease;
  border-left: 2px solid transparent;
}
@media (max-width: 480px) {
  .item-weather-text { display: none; }
}
.list-item:hover { background: rgba(26, 26, 26, 0.03); }
.list-item.active { background: rgba(26, 26, 26, 0.05); border-left-color: var(--ink); }
.item-date {
  flex-shrink: 0;
  font-family: 'Noto Serif SC', serif;
  color: var(--ink-muted);
  font-size: 12px;
  width: 44px;
  text-align: right;
  letter-spacing: 0.02em;
}
.item-month { opacity: 0.7; }
.item-day { font-weight: 500; color: var(--ink-light); font-size: 13px; }
.list-item.active .item-day { color: var(--ink); }
.item-weekday {
  flex: 1;
  min-width: 0;
  font-size: 13px;
  color: var(--ink-muted);
  letter-spacing: 0.04em;
}
.list-item.active .item-weekday { color: var(--ink-light); }
.item-weather {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  font-size: 12px;
  color: var(--ink-muted);
  letter-spacing: 0.02em;
}
.item-weather :deep(.weather-icon) { width: 14px; height: 14px; color: var(--ink-muted); }
.list-item.active .item-weather,
.list-item.active .item-weather :deep(.weather-icon),
.list-item:hover .item-weather,
.list-item:hover .item-weather :deep(.weather-icon) {
  color: var(--ink-light);
}
.list-sentinel {
  text-align: center;
  padding: 16px 0 8px;
  font-size: 12px;
  color: var(--ink-muted);
  letter-spacing: 0.05em;
}
/* 右侧详情 */
.detail-pane {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  scrollbar-gutter: stable;
  padding: 36px 48px 60px;
  background: var(--paper-card);
}
.detail-pane::-webkit-scrollbar { width: 6px; }
.detail-pane::-webkit-scrollbar-thumb { background: rgba(26, 26, 26, 0.12); border-radius: 3px; }
.detail-empty,
.detail-skeleton {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.detail-skeleton { display: block; padding: 20px 0; }
.detail-card { max-width: 720px; margin: 0 auto; }
.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 18px;
}
.date-block {
  display: flex;
  align-items: baseline;
  gap: 14px;
}
.date-num {
  font-family: 'Helvetica Neue', 'Arial', sans-serif;
  font-size: 52px;
  font-weight: 700;
  color: var(--ink);
  line-height: 1;
  letter-spacing: 0.01em;
}
.date-right {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 6px;
}
.date-month-en {
  font-family: 'Helvetica Neue', 'Arial', sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: var(--ink);
  letter-spacing: 0.1em;
}
.date-year {
  font-family: 'Helvetica Neue', 'Arial', sans-serif;
  font-size: 18px;
  font-weight: 400;
  color: var(--ink-muted);
  letter-spacing: 0.1em;
}
.detail-actions { display: flex; gap: 6px; }
.icon-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: 1px solid rgba(26, 26, 26, 0.1);
  border-radius: 4px;
  color: var(--ink-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}
.icon-btn:hover { border-color: var(--ink); color: var(--ink); }
.icon-btn.danger:hover { border-color: #c0392b; color: #c0392b; }
.icon-btn svg { width: 15px; height: 15px; }
.detail-info {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding-bottom: 18px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
  margin-bottom: 28px;
  font-size: 14px;
  color: var(--ink-muted);
  letter-spacing: 0.04em;
}
.info-item { display: flex; align-items: center; gap: 6px; }
.info-icon { width: 14px; height: 14px; color: var(--ink-muted); }
.info-divider { color: rgba(26, 26, 26, 0.3); }
.info-weather { color: var(--ink-light); }
.info-weather :deep(.weather-icon) { width: 16px; height: 16px; color: var(--ink-muted); }
.detail-content {
  font-family: 'Noto Serif SC', serif;
  font-size: 15px;
  line-height: 2;
  color: var(--ink);
  word-break: break-word;
  letter-spacing: 0.02em;
}
.detail-content :deep(p) {
  margin: 0 0 0.6em;
  line-height: 1.8;
}
.detail-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
  margin: 12px 0;
  display: block;
}
/* 浮动按钮 */
.floating-write {
  position: fixed;
  right: 32px;
  bottom: 32px;
  width: 52px;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--ink);
  color: #f0ebe3;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.18);
  transition: all 0.2s ease;
  z-index: 50;
}
.floating-write:hover {
  background: var(--accent);
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.22);
}
.floating-write svg { width: 22px; height: 22px; }
/* 响应式 */
@media (max-width: 960px) {
  .diary-frame { flex-direction: column; height: auto; }
  .list-pane {
    width: 100%;
    max-height: 340px;
    border-right: none;
    border-bottom: 1px solid rgba(26, 26, 26, 0.06);
  }
  .detail-pane { padding: 28px 22px 40px; }
}
@media (max-width: 640px) {
  .detail-pane { padding: 22px 16px 40px; }
  .date-num { font-size: 40px; }
  .date-month-en, .date-year { font-size: 15px; }
  .floating-write { right: 20px; bottom: 20px; width: 48px; height: 48px; }
}
</style>
