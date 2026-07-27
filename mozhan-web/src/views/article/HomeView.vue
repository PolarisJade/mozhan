<script setup>import { onMounted, ref, h, watch } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getArticleList, getHotArticles } from '@/api/article';
import { getCategoryList } from '@/api/category';
import { searchArticle, searchUser } from '@/api/search';
import { getStatistics } from '@/api/statistics';
const SearchIcon = h('svg', {
 class: 'search-icon',
 viewBox: '0 0 24 24',
 fill: 'none',
 stroke: 'currentColor',
 'stroke-width': '1.5',
 'stroke-linecap': 'round',
 'stroke-linejoin': 'round'
}, [
 h('circle', { cx: '11', cy: '11', r: '7' }),
 h('path', { d: 'M21 21l-4.35-4.35' })
]);
const ArticleIcon = h('svg', {
 class: 'icon-article',
 viewBox: '0 0 24 24',
 fill: 'none',
 stroke: 'currentColor',
 'stroke-width': '1.5',
 'stroke-linecap': 'round',
 'stroke-linejoin': 'round'
}, [
 h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
 h('polyline', { points: '14 2 14 8 20 8' }),
 h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
 h('line', { x1: '16', y1: '17', x2: '8', y2: '17' }),
 h('polyline', { points: '10 9 9 9 8 9' })
]);
const UserIcon = h('svg', {
 class: 'icon-user',
 viewBox: '0 0 24 24',
 fill: 'none',
 stroke: 'currentColor',
 'stroke-width': '1.5',
 'stroke-linecap': 'round',
 'stroke-linejoin': 'round'
}, [
 h('path', { d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' }),
 h('circle', { cx: '12', cy: '7', r: '4' })
]);
const BookOpenIcon = h('svg', {
 class: 'icon-book-open',
 viewBox: '0 0 24 24',
 fill: 'none',
 stroke: 'currentColor',
 'stroke-width': '1.5',
 'stroke-linecap': 'round',
 'stroke-linejoin': 'round'
}, [
 h('path', { d: 'M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z' }),
 h('path', { d: 'M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z' })
]);
const HashIcon = h('svg', {
 class: 'icon-hash',
 viewBox: '0 0 24 24',
 fill: 'none',
 stroke: 'currentColor',
 'stroke-width': '1.5',
 'stroke-linecap': 'round',
 'stroke-linejoin': 'round'
}, [
 h('path', { d: 'M16 3h5v5h-5zM4 3h5v5H4zM16 16h5v5h-5zM4 16h5v5H4z' })
]);
const FeatherIcon = h('svg', {
 class: 'icon-feather',
 viewBox: '0 0 24 24',
 fill: 'none',
 stroke: 'currentColor',
 'stroke-width': '1.5',
 'stroke-linecap': 'round',
 'stroke-linejoin': 'round'
}, [
 h('path', { d: 'M20.24 12.24a6 6 0 0 0-8.49-8.49L5 10.5V19h8.5z' }),
 h('path', { d: 'M16 3.86V6a6 6 0 0 0 0 12v2.14' }),
 h('line', { x1: '16', y1: '19', x2: '16', y2: '22' }),
 h('line', { x1: '8', y1: '22', x2: '8', y2: '19' }),
 h('path', { d: 'M16 19a3 3 0 0 1-3-3' })
]);
const UsersIcon = h('svg', {
 class: 'icon-users',
 viewBox: '0 0 24 24',
 fill: 'none',
 stroke: 'currentColor',
 'stroke-width': '1.5',
 'stroke-linecap': 'round',
 'stroke-linejoin': 'round'
}, [
 h('path', { d: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2' }),
 h('circle', { cx: '9', cy: '7', r: '4' }),
 h('path', { d: 'M23 21v-2a4 4 0 0 0-3-3.87' }),
 h('path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' })
]);
const router = useRouter();
const route = useRoute();
const loading = ref(false);
const articles = ref([]);
const hasMore = ref(false);
const cursor = ref(null);
const categories = ref([]);
const selectedCategory = ref(null);
const searchKeyword = ref('');
const searchType = ref('article');
const searchResults = ref(null);
const isSearching = ref(false);
const statistics = ref(null);
const hotArticles = ref([]);
async function loadStatistics() {
 try {
 const data = await getStatistics();
 statistics.value = data || null;
 }
 catch (e) {
 console.error('加载统计数据失败', e);
 }
}
async function loadHotArticles() {
  try {
    const data = await getHotArticles({ pageNo: 1, pageSize: 6 });
    // 后端返回 PageQueryVO<HotArticleVO>，数据在 records 字段
    hotArticles.value = data?.records || [];
  }
  catch (e) {
    console.error('加载热门文章失败', e);
  }
}
async function loadCategories() {
 try {
 const data = await getCategoryList();
 categories.value = data || [];
 }
 catch (e) {
 console.error('加载分类失败', e);
 }
}
async function loadArticles(reset = false) {
  if (loading.value)
    return;
  loading.value = true;
  try {
    const query = {
      pageNum: reset ? 1 : (cursor.value || 1),
      pageSize: 10,
    };
    if (selectedCategory.value) {
      query.categoryId = selectedCategory.value;
    }
    const data = await getArticleList(query);
    // 后端返回 PageQueryVO<ArticleVO>，数据在 records 字段
    const list = data?.records || [];
    if (reset) {
      articles.value = list;
    }
    else {
      articles.value.push(...list);
    }
    // 分页对象字段：total / pageNo / pages
    cursor.value = (data?.pageNo || 1) + 1;
    hasMore.value = cursor.value <= (data?.pages || 1);
  }
  finally {
    loading.value = false;
  }
}
async function handleSearch() {
  if (!searchKeyword.value.trim()) {
    searchResults.value = null;
    loadArticles(true);
    return;
  }
  isSearching.value = true;
  try {
    const query = {
      keyword: searchKeyword.value.trim(),
      pageNum: 1,
      pageSize: 10,
    };
    if (searchType.value === 'article') {
      const data = await searchArticle(query);
      searchResults.value = {
        type: 'article',
        list: data?.records || [],
        hasMore: (data?.pageNo || 1) < (data?.pages || 1),
        nextCursor: (data?.pageNo || 1) + 1,
      };
    }
    else {
      const data = await searchUser(query);
      searchResults.value = {
        type: 'user',
        list: data?.records || [],
        hasMore: (data?.pageNo || 1) < (data?.pages || 1),
        nextCursor: (data?.pageNo || 1) + 1,
      };
    }
  }
  catch (e) {
    console.error('搜索失败', e);
    searchResults.value = null;
  }
  finally {
    isSearching.value = false;
  }
}
function goDetail(id) {
 router.push({ name: 'ArticleDetail', params: { id } });
}
function goUserProfile(userId) {
 router.push({ name: 'UserProfile', params: { userId } });
}
function selectCategory(categoryId) {
 selectedCategory.value = categoryId;
 searchKeyword.value = '';
 searchResults.value = null;
 loadArticles(true);
}
function clearSearch() {
 searchKeyword.value = '';
 searchResults.value = null;
 loadArticles(true);
}
function initSearchFromUrl() {
  const keyword = route.query.keyword;
  if (keyword) {
    searchKeyword.value = decodeURIComponent(keyword);
    selectedCategory.value = null;
    handleSearch();
  }
}

onMounted(() => {
  loadStatistics();
  loadCategories();
  loadHotArticles();
  initSearchFromUrl();
  if (!route.query.keyword) {
    loadArticles(true);
  }
});

watch(() => route.query.keyword, (newKeyword) => {
  if (newKeyword) {
    searchKeyword.value = decodeURIComponent(newKeyword);
    selectedCategory.value = null;
    handleSearch();
  } else {
    searchKeyword.value = '';
    searchResults.value = null;
    loadArticles(true);
  }
});
</script>

<template>
  <div class="home">
    <div class="main-content">
      <div v-if="!searchResults">
        <div class="header-cover"></div>
        <div class="fixed-header">
        <div class="category-nav-wrapper">
          <div class="category-nav">
            <button
              class="category-item"
              :class="{ active: !selectedCategory && !searchKeyword }"
              @click="selectCategory(null)"
            >
              全部
            </button>
            <button
              v-for="cat in categories"
              :key="cat.id"
              class="category-item"
              :class="{ active: selectedCategory === cat.id }"
              @click="selectCategory(cat.id)"
            >
              {{ cat.name }}
            </button>
          </div>
        </div>
        <h2 class="ink-page-title">文章列表</h2>
        </div>
      </div>

      <div v-if="searchResults" class="search-results">
        <div class="search-header">
          <h2 class="search-title">搜索结果</h2>
          <span class="search-count">共找到 {{ searchResults.list.length }} 条结果</span>
          <button class="clear-btn" @click="clearSearch">清除搜索</button>
        </div>
        
        <div class="search-tabs">
          <button 
            class="search-tab" 
            :class="{ active: searchType === 'article' }"
            @click="searchType = 'article'; handleSearch()"
          >
            <component :is="ArticleIcon" />
            文章
          </button>
          <button 
            class="search-tab" 
            :class="{ active: searchType === 'user' }"
            @click="searchType = 'user'; handleSearch()"
          >
            <component :is="UserIcon" />
            作者
          </button>
        </div>

        <div v-if="searchResults.type === 'article'" class="article-list">
          <article
            v-for="item in searchResults.list"
            :key="item.id"
            class="article-card ink-card"
            @click="goDetail(item.id)"
          >
            <div class="article-header">
              <h3>{{ item.title }}</h3>
              <el-tag v-if="item.isTop" type="warning" size="small">置顶</el-tag>
            </div>
            <p class="summary">{{ item.summary || '暂无摘要' }}</p>
            <div class="meta ink-meta">
              <span>{{ item.authorName }}</span>
              <span v-if="item.categoryName"> · {{ item.categoryName }}</span>
              <span class="meta-icon">
                <svg class="icon-like" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 18c-2 0-4-1.5-4-4v-5c0-1.5 1-3 3-3h6l2 5h4c2 0 3 1.5 3 3v2c0 1.5-1.5 3-3 3H6z" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M9 12l2 3 4-5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>{{ item.likeCount ?? 0 }}</span>
              </span>
              <span class="meta-icon">
                <svg class="icon-comment" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>{{ item.commentCount ?? 0 }}</span>
              </span>
            </div>
          </article>
        </div>

        <div v-else class="user-list">
          <div
            v-for="item in searchResults.list"
            :key="item.id"
            class="user-card ink-card"
            @click="goUserProfile(item.id)"
          >
            <div class="user-avatar">
              <img :src="item.avatar || '/default-avatar.png'" alt="avatar" />
            </div>
            <div class="user-info">
              <h3>{{ item.nickname }}</h3>
              <p class="user-intro">{{ item.intro || '暂无简介' }}</p>
            </div>
            <div class="user-stats">
              <span>{{ item.articleCount ?? 0 }} 文章</span>
              <span>{{ item.followerCount ?? 0 }} 粉丝</span>
            </div>
          </div>
        </div>

        <div v-if="!searchResults.list.length" class="empty-result">
          <el-empty description="暂无相关结果" />
        </div>
      </div>

      <div v-else>
        <el-skeleton v-if="loading && !articles.length" :rows="5" animated />

        <el-empty v-else-if="!articles.length" description="暂无文章，静待墨香" />

        <div v-else class="article-list">
          <article
            v-for="item in articles"
            :key="item.id"
            class="article-card ink-card"
            @click="goDetail(item.id)"
          >
            <div class="article-header">
              <h3>{{ item.title }}</h3>
              <el-tag v-if="item.isTop" type="warning" size="small">置顶</el-tag>
            </div>
            <p class="summary">{{ item.summary || '暂无摘要' }}</p>
            <div class="meta ink-meta">
              <span>{{ item.authorName }}</span>
              <span v-if="item.categoryName"> · {{ item.categoryName }}</span>
              <span class="meta-icon">
                <svg class="icon-like" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 18c-2 0-4-1.5-4-4v-5c0-1.5 1-3 3-3h6l2 5h4c2 0 3 1.5 3 3v2c0 1.5-1.5 3-3 3H6z" stroke-linecap="round" stroke-linejoin="round"/>
                  <path d="M9 12l2 3 4-5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>{{ item.likeCount ?? 0 }}</span>
              </span>
              <span class="meta-icon">
                <svg class="icon-comment" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                <span>{{ item.commentCount ?? 0 }}</span>
              </span>
            </div>
          </article>
        </div>

        <div v-if="hasMore" class="load-more">
          <el-button class="ink-btn-plain" :loading="loading" @click="loadArticles()">
            加载更多
          </el-button>
        </div>
      </div>
    </div>

    <aside class="sidebar">
      <div class="sidebar-section" v-if="statistics">
        <h3 class="sidebar-title">数据统计</h3>
        <div class="stats-list">
          <div class="stat-item">
            <span class="stat-icon"><component :is="BookOpenIcon" /></span>
            <div class="stat-info">
              <span class="stat-num">{{ statistics.articleCount ?? 0 }}</span>
              <span class="stat-text">文章总数</span>
            </div>
          </div>
          <div class="stat-item">
            <span class="stat-icon"><component :is="UsersIcon" /></span>
            <div class="stat-info">
              <span class="stat-num">{{ statistics.userCount ?? 0 }}</span>
              <span class="stat-text">用户总数</span>
            </div>
          </div>
          <div class="stat-item">
            <span class="stat-icon"><component :is="HashIcon" /></span>
            <div class="stat-info">
              <span class="stat-num">{{ statistics.tagCount ?? 0 }}</span>
              <span class="stat-text">标签总数</span>
            </div>
          </div>
          <div class="stat-item">
            <span class="stat-icon"><component :is="FeatherIcon" /></span>
            <div class="stat-info">
              <span class="stat-num">{{ statistics.essayCount ?? 0 }}</span>
              <span class="stat-text">随笔总数</span>
            </div>
          </div>
        </div>
      </div>
      <div class="sidebar-section" v-if="hotArticles.length > 0">
        <h3 class="sidebar-title">热门文章</h3>
        <div class="hot-list">
          <div 
            v-for="(article, index) in hotArticles" 
            :key="article.id" 
            class="hot-item"
            @click="goDetail(article.id)"
          >
            <span class="hot-rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
            <span class="hot-title">{{ article.title }}</span>
          </div>
        </div>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.home {
  width: 100%;
  min-height: 100vh;
  display: flex;
  gap: 32px;
  padding: 0;
  box-sizing: border-box;
}

.main-content {
  flex: 1;
  min-width: 0;
}

.fixed-header {
  position: fixed;
  top: 95px;
  left: 50%;
  transform: translateX(calc(-50% - 157px));
  max-width: 860px;
  width: calc(100% - 30px);
  z-index: 50;
  background: var(--paper);
  padding-bottom: 12px;
}

.header-cover {
  position: fixed;
  top: 68px;
  left: 50%;
  transform: translateX(calc(-50% - 157px));
  width: calc(100% - 30px);
  max-width: 860px;
  height: 30px;
  z-index: 49;
  background: var(--paper);
}

.category-nav-wrapper {
  padding: 0;
  margin-bottom: 12px;
  margin-top: -8px;
}

.category-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 0;
  border: 1px solid rgba(26, 26, 26, 0.08);
}

.category-item {
  position: relative;
  padding: 8px 0;
  border: none;
  background: transparent;
  border-radius: 0;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s ease;
}

.category-item:hover {
  color: #1a1a1a;
}

.category-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: #1a1a1a;
  transition: width 0.3s ease;
}

.category-item:hover::after {
  width: 100%;
}

.category-item.active {
  color: #1a1a1a;
  font-weight: 500;
}

.category-item.active::after {
  width: 100%;
}

.search-results {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.search-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.search-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.search-count {
  font-size: 14px;
  color: #999;
}

.clear-btn {
  margin-left: auto;
  padding: 6px 12px;
  font-size: 14px;
  color: #999;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  cursor: pointer;
  background: transparent;
}

.clear-btn:hover {
  border-color: #2c1810;
  color: #2c1810;
}

.search-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.search-tab {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  font-size: 13px;
  border: none;
  background: #f5f5f5;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.search-tab:hover {
  background: #e8e8e8;
}

.search-tab.active {
  background: #2c1810;
  color: #fff;
}

.search-tab .icon-article,
.search-tab .icon-user {
  width: 16px;
  height: 16px;
}

.article-list {
  display: flex;
  flex-direction: column;
  padding-top: 130px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
}

.search-results .article-list {
  padding-top: 0;
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
  gap: 16px;
  font-size: 13px;
  color: #999;
}

.meta-icon {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-icon svg {
  width: 14px;
  height: 14px;
}

.user-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s ease;
}

.user-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.user-avatar img {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
}

.user-info {
  flex: 1;
}

.user-info h3 {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 4px 0;
}

.user-intro {
  font-size: 13px;
  color: #999;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}

.empty-result {
  padding: 40px;
  text-align: center;
}

.ink-page-title {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 5px 0;
}

.load-more {
  text-align: center;
  padding: 20px;
}

.ink-btn-plain {
  padding: 10px 30px;
  border: 1px solid #2c1810;
  background: transparent;
  color: #2c1810;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ink-btn-plain:hover {
  background: #2c1810;
  color: #fff;
}

.sidebar {
  width: 280px;
  flex-shrink: 0;
  position: sticky;
  top: 88px;
  height: fit-content;
}

.sidebar-section {
  background: #fff;
  border-radius: 0;
  padding: 20px;
  border: 1px solid rgba(26, 26, 26, 0.08);
  margin-bottom: 16px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.stats-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f0e6;
  border-radius: 8px;
  color: #2c1810;
}

.stat-icon svg {
  width: 18px;
  height: 18px;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-num {
  font-size: 18px;
  font-weight: 600;
  color: #2c1810;
}

.stat-text {
  font-size: 12px;
  color: #999;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s ease;
}

.hot-item:hover {
  background: #f9f9f9;
}

.hot-rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f0f0;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #999;
}

.hot-rank.top {
  background: #e74c3c;
  color: #fff;
}

.hot-title {
  flex: 1;
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

@media (max-width: 768px) {
  .home {
    flex-direction: column;
    padding: 16px;
  }
  
  .sidebar {
    width: 100%;
    position: static;
  }
}
</style>