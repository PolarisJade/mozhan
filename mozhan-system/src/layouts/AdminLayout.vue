<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <h1>墨栈管理</h1>
      </div>
      <nav class="sidebar-nav">
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: route.path === item.path }"
        >
          <component :is="item.icon" class="nav-icon" />
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
    </aside>
    <main class="admin-main">
      <header class="admin-header">
        <div class="header-left">
          <span class="current-path">{{ currentPageName }}</span>
        </div>
        <div class="header-right">
          <button class="logout-btn" @click="logout">退出</button>
        </div>
      </header>
      <div class="admin-content">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, h } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { clearToken } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

const UserIcon = h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', width: '16', height: '16' }, [
  h('path', { d: 'M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2' }),
  h('circle', { cx: '12', cy: '7', r: '4' })
])

const FolderIcon = h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', width: '16', height: '16' }, [
  h('path', { d: 'M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z' })
])

const TagIcon = h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', width: '16', height: '16' }, [
  h('path', { d: 'M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z' }),
  h('line', { x1: '7', y1: '7', x2: '7', y2: '7' })
])

const FileTextIcon = h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', width: '16', height: '16' }, [
  h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
  h('polyline', { points: '14 2 14 8 20 8' }),
  h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
  h('line', { x1: '16', y1: '17', x2: '8', y2: '17' }),
  h('polyline', { points: '10 9 9 9 8 9' })
])

const MessageSquareIcon = h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', width: '16', height: '16' }, [
  h('path', { d: 'M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z' })
])

const menuItems = [
  { path: '/user', label: '用户管理', icon: UserIcon },
  { path: '/category', label: '分类管理', icon: FolderIcon },
  { path: '/tag', label: '标签管理', icon: TagIcon },
  { path: '/essay', label: '随笔管理', icon: FileTextIcon },
  { path: '/comment', label: '评论管理', icon: MessageSquareIcon },
]

const currentPageName = computed(() => {
  const item = menuItems.find(i => i.path === route.path)
  return item ? item.label : ''
})

function logout() {
  clearToken()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: #f5f5f5;
}

.admin-sidebar {
  width: 220px;
  background: #1a1a2e;
  color: #fff;
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  z-index: 100;
}

.sidebar-header {
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-header h1 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
}

.sidebar-nav {
  flex: 1;
  padding: 8px 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 16px;
  color: rgba(255, 255, 255, 0.7);
  text-decoration: none;
  transition: all 0.2s;
  font-size: 14px;
  line-height: 1.5;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
}

.nav-item.active {
  background: #2d2d44;
  color: #fff;
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.admin-main {
  flex: 1;
  margin-left: 220px;
  display: flex;
  flex-direction: column;
}

.admin-header {
  background: #fff;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 50;
}

.current-path {
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.logout-btn {
  padding: 6px 16px;
  background: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #eee;
  color: #333;
}

.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}
</style>
