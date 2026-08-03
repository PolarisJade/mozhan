<script setup>
import { computed, h, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useChatStore } from '@/stores/chat'
import { redirectToLogin } from '@/utils/auth'
import { InkMessage } from '@/utils/message'

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()

const searchKeyword = ref('')
const searchFocused = ref(false)
const showUserMenu = ref(false)
const showWriteMenu = ref(false)
let menuHideTimer = null

// 私信未读总数
const unreadTotal = computed(() => chatStore.unreadTotal || 0)

const PenIcon = h('svg', {
  class: 'pen-icon',
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  'stroke-width': '1.5',
  'stroke-linecap': 'round',
  'stroke-linejoin': 'round'
}, [
  h('path', { d: 'M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z' })
])

const SearchIcon = h('svg', {
  class: 'search-icon',
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  'stroke-width': '1.5',
  'stroke-linecap': 'round',
  'stroke-linejoin': 'round',
  style: { width: '16px', height: '16px' }
}, [
  h('circle', { cx: '11', cy: '11', r: '8' }),
  h('path', { d: 'M21 21l-4.35-4.35' })
])

function handleSearch() {
  if (!searchKeyword.value.trim()) return
  router.push({ name: 'Home', query: { keyword: searchKeyword.value } })
}

function goHome() {
  router.push({ name: 'Home' })
}

function goLogin() {
  router.push({ name: 'Login' })
}

function goProfile() {
  router.push({ name: 'Profile' })
}

function handleLogout() {
  userStore.logout()
  router.push({ name: 'Home' })
}

function handleMenuEnter() {
  if (menuHideTimer) {
    clearTimeout(menuHideTimer)
    menuHideTimer = null
  }
  showUserMenu.value = true
}

function handleMenuLeave() {
  menuHideTimer = setTimeout(() => {
    showUserMenu.value = false
    menuHideTimer = null
  }, 200)
}

function goWrite() {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后再写文章')
    return
  }
  router.push({ name: 'ArticleWrite' })
}

function goEssayWrite() {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后再写随笔')
    return
  }
  router.push({ name: 'EssayWrite' })
}

function goDiaryWrite() {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后再写日记')
    return
  }
  router.push({ name: 'DiaryWrite' })
}

function goDiary() {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后查看日记')
    return
  }
  router.push({ name: 'DiaryHome' })
}

function goChat() {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后查看私信')
    return
  }
  router.push({ name: 'Chat' })
}
</script>

<template>
  <div class="layout">
    <header class="header">
      <div class="header-inner">
        <div class="logo" @click="goHome">
            <img src="@/assets/logo.svg" alt="墨栈" class="logo-img" />
            <span class="logo-text font-display">墨栈</span>
          </div>
        <nav class="main-nav">
          <router-link to="/" class="nav-item" :class="{ active: $route.name === 'Home' }">文章</router-link>
          <router-link to="/essay" class="nav-item" :class="{ active: $route.name === 'EssayHome' }">随笔</router-link>
          <button
            class="nav-item"
            :class="{ active: ['DiaryHome', 'DiaryWrite', 'DiaryEdit'].includes($route.name) }"
            @click="goDiary"
          >
            小记
          </button>
        </nav>
        <div class="user-area">
          <div class="search-wrapper">
            <div class="search-box" :class="{ focused: searchFocused }">
              <button class="search-btn" @click="handleSearch" title="搜索">
                <component :is="SearchIcon" />
              </button>
              <input
                type="text"
                v-model="searchKeyword"
                placeholder="墨海寻踪..."
                class="search-input"
                @focus="searchFocused = true"
                @blur="searchFocused = false"
                @keyup.enter="handleSearch"
              />
              <span v-if="searchKeyword" class="search-clear" @click="searchKeyword = ''">×</span>
            </div>
          </div>
          <template v-if="userStore.isLoggedIn">
            <!-- 私信入口 -->
            <button class="chat-btn" :class="{ active: $route.name === 'Chat' }" @click="goChat" title="私信">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z" />
              </svg>
              <span v-if="unreadTotal > 0" class="chat-badge">{{ unreadTotal > 99 ? '99+' : unreadTotal }}</span>
            </button>
            <div class="write-btn-wrapper">
              <button class="write-btn" @mouseenter="showWriteMenu = true">
                <component :is="PenIcon" />
                <span>创作</span>
                <svg class="arrow-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <path d="M6 9l6 6 6-6" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
              </button>
              <div v-show="showWriteMenu" class="user-menu" @mouseenter="showWriteMenu = true" @mouseleave="showWriteMenu = false">
                <button class="menu-item" @click="goEssayWrite">
                  <span>随笔</span>
                </button>
                <button class="menu-item" @click="goWrite">
                  <span>文章</span>
                </button>
                <button class="menu-item" @click="goDiaryWrite">
                  <span>日记</span>
                </button>
              </div>
            </div>
            <div class="user-info-wrapper" @mouseenter="handleMenuEnter" @mouseleave="handleMenuLeave">
              <div class="user-info">
                <el-avatar :size="32" :src="userStore.user?.avatar" class="user-avatar">
                  {{ userStore.user?.nickname?.[0] || userStore.user?.username?.[0] }}
                </el-avatar>
                <span class="user-name">{{ userStore.user?.nickname || userStore.user?.username }}</span>
              </div>
              <div v-if="showUserMenu" class="user-menu" @mouseenter="handleMenuEnter" @mouseleave="handleMenuLeave">
                <button class="menu-item" @click.stop="goProfile">
                  <span>个人主页</span>
                </button>
                <button class="menu-item" @click.stop="handleLogout">
                  <span>退出登录</span>
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <button class="write-btn" @click="goWrite">
              <component :is="PenIcon" />
              <span>写文章</span>
            </button>
            <el-button link class="nav-link" @click="goLogin">登录</el-button>
            <router-link to="/register">
              <el-button class="ink-btn header-btn">注册</el-button>
            </router-link>
          </template>
        </div>
      </div>
    </header>

    <main class="main">
      <router-view />
    </main>

    <footer v-if="$route.name !== 'Chat'" class="footer">
      <p>© 2026 墨栈 · 挥毫落纸，栈叙文心</p>
    </footer>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--paper);
}

.header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 100;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
  background: rgba(255, 252, 247, 0.95);
  backdrop-filter: blur(12px);
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
  height: 72px;
  display: flex;
  align-items: center;
  gap: 28px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 0;
  cursor: pointer;
  user-select: none;
}

.logo-img {
  width: 98px;
  height: 98px;
  object-fit: contain;
  margin-top: -10px;
}

.logo-text {
  font-size: 30px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.15em;
  margin-left: -10px;
}

.main-nav {
  display: flex;
  gap: 32px;
}

.nav-item {
  position: relative;
  color: var(--ink-muted);
  text-decoration: none;
  font-size: 18px;
  letter-spacing: 0.05em;
  padding: 8px 0;
  border-radius: 0;
  transition: color 0.3s;
  background: transparent;
  border: none;
  cursor: pointer;
}

.nav-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--ink);
  transition: width 0.3s ease;
}

.nav-item:hover {
  color: var(--ink);
}

.nav-item:hover::after {
  width: 100%;
}

.nav-item.active {
  color: var(--ink);
  font-weight: 500;
}

.nav-item.active::after {
  width: 100%;
}

.nav a {
  color: var(--ink-muted);
  text-decoration: none;
  font-size: 20px;
  letter-spacing: 0.1em;
  transition: color 0.3s;
}

.nav a:hover,
.nav a.router-link-active {
  color: var(--ink);
}

.user-area {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.nav-link {
  color: var(--ink-muted);
  text-decoration: none;
  font-size: 14px;
  letter-spacing: 0.1em;
  transition: color 0.3s;
  padding: 6px 12px;
}

.nav-link:hover {
  color: var(--ink);
}

.nav-link.router-link-active {
  color: var(--ink);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 22px;
  transition: background-color 0.2s;
}

.user-info:hover {
  background-color: rgba(26, 26, 26, 0.04);
}

.user-info-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.user-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 120px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 4px 0;
  z-index: 200;
}

.menu-item {
  width: 100%;
  padding: 8px 16px;
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  font-size: 16px;
  color: var(--ink-light);
  transition: background-color 0.2s;
  display: block;
  text-decoration: none;
}

.menu-item:hover {
  background-color: rgba(26, 26, 26, 0.04);
}

.user-avatar {
  border: 1px solid rgba(26, 26, 26, 0.1);
}

.user-name {
  font-size: 14px;
  color: var(--ink-light);
  letter-spacing: 0.05em;
}

.nav-link {
  color: var(--ink-light) !important;
  font-size: 15px;
  letter-spacing: 0.05em;
}

.nav-link:hover {
  color: var(--ink) !important;
}

.header-btn {
  height: 32px;
  padding: 0 16px;
  font-size: 14px;
  letter-spacing: 0.1em;
}

.write-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: transparent;
  border: none;
  border-radius: 0;
  color: var(--ink-light);
  font-family: 'Noto Serif SC', 'ZCOOL XiaoWei', serif;
  font-size: 15px;
  letter-spacing: 0.05em;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
  white-space: nowrap;
}

.write-btn:hover {
  color: var(--ink);
}

.write-btn .pen-icon {
  width: 16px;
  height: 16px;
  opacity: 0.7;
  transition: opacity 0.2s ease;
}

.write-btn:hover .pen-icon {
  opacity: 1;
}

.write-btn .arrow-icon {
  width: 12px;
  height: 12px;
  opacity: 0.7;
  transition: all 0.2s ease;
}

.write-btn:hover .arrow-icon {
  opacity: 1;
  transform: rotate(180deg);
}

.write-btn-wrapper {
  position: relative;
}

/* 私信按钮 */
.chat-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  background: transparent;
  border: none;
  border-radius: 50%;
  color: var(--ink-light);
  cursor: pointer;
  transition: all 0.2s ease;
}

.chat-btn:hover,
.chat-btn.active {
  color: var(--ink);
  background: rgba(26, 26, 26, 0.05);
}

.chat-btn svg {
  width: 22px;
  height: 22px;
}

.chat-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background: #e54d42;
  color: #fff;
  font-size: 10px;
  line-height: 16px;
  text-align: center;
  font-weight: 500;
  box-sizing: border-box;
  box-shadow: 0 0 0 2px rgba(255, 252, 247, 0.95);
  letter-spacing: 0;
}



.search-wrapper {
  flex: 0 1 auto;
  width: 180px;
  transition: width 0.3s ease;
}

.search-wrapper:focus-within {
  width: 300px;
}

.search-box {
  position: relative;
  display: flex;
  align-items: center;
  width: 100%;
  height: 40px;
  min-height: 40px;
  max-height: 40px;
  padding: 0 14px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(26, 26, 26, 0.15);
  border-radius: 20px;
  transition: border-color 0.2s ease;
}

.search-box:hover {
  border-color: rgba(26, 26, 26, 0.25);
}

.search-box.focused {
  border-color: rgba(121, 85, 72, 0.5);
  height: 40px !important;
}

.search-box .search-icon {
  width: 18px !important;
  height: 18px !important;
  min-width: 18px !important;
  min-height: 18px !important;
  max-width: 18px !important;
  max-height: 18px !important;
  color: rgba(26, 26, 26, 0.45) !important;
  margin-right: 8px;
  flex-shrink: 0;
  flex-grow: 0;
  object-fit: contain;
  overflow: hidden;
  transform: scale(1);
  transform-origin: center center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.search-box.focused .search-icon {
  color: rgba(26, 26, 26, 0.6);
}

.search-btn {
  background: transparent;
  border: none;
  outline: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  margin-right: 8px;
  flex-shrink: 0;
  transition: opacity 0.2s ease;
}

.search-btn:hover .search-icon {
  opacity: 0.8;
}

.search-input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 14px;
  color: var(--ink);
  font-family: 'Noto Serif SC', 'ZCOOL XiaoWei', serif;
  letter-spacing: 0.03em;
}

.search-input::placeholder {
  color: rgba(26, 26, 26, 0.4);
  font-style: italic;
}

.search-clear {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(26, 26, 26, 0.4);
  font-size: 14px;
  cursor: pointer;
  border-radius: 50%;
  transition: all 0.2s ease;
}

.search-clear:hover {
  color: rgba(26, 26, 26, 0.7);
  background: rgba(26, 26, 26, 0.08);
}

.main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 0 auto;
  padding: 92px 20px 32px;
  box-sizing: border-box;
}

.footer {
  text-align: center;
  padding: 20px;
  color: var(--ink-muted);
  font-size: 12px;
  letter-spacing: 0.08em;
  border-top: 1px solid rgba(26, 26, 26, 0.06);
}
</style>
