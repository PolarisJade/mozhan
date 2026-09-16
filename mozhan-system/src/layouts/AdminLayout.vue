<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-header">
        <!-- 印章：整套界面的视觉锚点，也是唯一用朱红的地方，用多了就不像印了 -->
        <span class="seal font-display" aria-hidden="true">墨</span>
        <span class="brand-text">
          <span class="brand-name font-display">墨栈</span>
          <span class="brand-sub">管理后台</span>
        </span>
      </div>
      <nav class="sidebar-nav" ref="navRef">
        <!--
          指示条只有一根，在菜单项之间滑动，而不是每项各自高亮。
          这样"当前位置"是一个连续移动的物体，眼睛能跟着它走；
          每项自己亮一下则是一次跳变，扫视时需要重新找。
        -->
        <span
          v-show="indicator.visible"
          class="nav-indicator"
          :style="{ transform: `translateY(${indicator.top}px)`, height: `${indicator.height}px` }"
          aria-hidden="true"
        />
        <router-link
          v-for="item in menuItems"
          :key="item.path"
          :ref="setItemRef(item.path)"
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
      <header class="admin-header" :class="{ 'is-scrolled': scrolled }">
        <div class="header-left">
          <span class="crumb-muted">墨栈管理</span>
          <span class="crumb-sep" aria-hidden="true">/</span>
          <span class="crumb-current">{{ currentPageName }}</span>
        </div>
        <div class="header-right">
          <button class="logout-btn" @click="logout">退出</button>
        </div>
      </header>
      <div class="admin-content" ref="contentRef" @scroll.passive="onScroll">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, h, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { clearToken } from '@/utils/auth'

const router = useRouter()
const route = useRoute()

const GridIcon = h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', width: '16', height: '16' }, [
  h('rect', { x: '3', y: '3', width: '7', height: '7' }),
  h('rect', { x: '14', y: '3', width: '7', height: '7' }),
  h('rect', { x: '14', y: '14', width: '7', height: '7' }),
  h('rect', { x: '3', y: '14', width: '7', height: '7' })
])

const BookIcon = h('svg', { viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round', width: '16', height: '16' }, [
  h('path', { d: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20' }),
  h('path', { d: 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z' })
])

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
  { path: '/dashboard', label: '工作台', icon: GridIcon },
  { path: '/article', label: '文章管理', icon: BookIcon },
  { path: '/user', label: '用户管理', icon: UserIcon },
  { path: '/category', label: '分类管理', icon: FolderIcon },
  { path: '/tag', label: '标签管理', icon: TagIcon },
  { path: '/essay', label: '随笔管理', icon: FileTextIcon },
  { path: '/comment', label: '评论管理', icon: MessageSquareIcon }
]

const currentPageName = computed(() => {
  const item = menuItems.find(i => i.path === route.path)
  return item ? item.label : ''
})

/* ---- 滑动指示条 ---- */
const navRef = ref(null)
const itemEls = new Map()
const indicator = reactive({ top: 0, height: 0, visible: false })

function setItemRef(path) {
  return el => {
    if (el) itemEls.set(path, el.$el || el)
    else itemEls.delete(path)
  }
}

function updateIndicator() {
  const el = itemEls.get(route.path)
  if (!el || !navRef.value) {
    indicator.visible = false
    return
  }
  // .sidebar-nav 是 position:relative，所以 offsetTop 已经相对它，不用再减父级偏移
  indicator.top = el.offsetTop
  indicator.height = el.offsetHeight
  indicator.visible = true
}

/* ---- 顶栏滚动交界 ---- */
const contentRef = ref(null)
const scrolled = ref(false)

function onScroll() {
  const top = contentRef.value?.scrollTop || 0
  // 只在跨过阈值时改一次，避免每帧都写响应式状态
  const next = top > 4
  if (next !== scrolled.value) scrolled.value = next
}

onMounted(() => {
  updateIndicator()
  window.addEventListener('resize', updateIndicator)
  // 字体是异步加载的：等它落地后再量一次，否则量到的是兜底字体的行高，
  // 指示条会跟菜单项差几个像素——这种错位没人会去查原因，只会觉得"做得不细"。
  if (document.fonts?.ready) document.fonts.ready.then(updateIndicator)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', updateIndicator)
})

watch(() => route.path, () => nextTick(updateIndicator))

function logout() {
  clearToken()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: var(--paper, #f6f3ed);
}

/*
 * 侧边栏是半透明材质：下面的内容滚过去时会被模糊掉一层。
 * 层级靠材质轻重表达——侧边栏/顶栏是"浮在内容之上"的，所以它们半透明且有模糊，
 * 而页面卡片是不透明的纸。两层半透明叠在一起就谁也看不清了，所以不做。
 */
.admin-sidebar {
  width: 220px;
  background: var(--material-sidebar, #f6f3ed);
  backdrop-filter: var(--material-blur, none);
  -webkit-backdrop-filter: var(--material-blur, none);
  border-right: 1px solid rgba(26, 26, 26, 0.07);
  display: flex;
  flex-direction: column;
  position: fixed;
  left: 0;
  top: 0;
  height: 100vh;
  z-index: var(--z-sidebar, 100);
}

.sidebar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.07);
}

.seal {
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  background: var(--seal-red, #8b4513);
  color: #f6f3ed;
  border-radius: 3px;
  font-size: 18px;
  line-height: 1;
  /* 印章是印上去的，边角不该是数学上的完美直角 */
  box-shadow: inset 0 0 0 1px rgba(246, 243, 237, 0.25);
}

.brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.brand-name {
  font-size: 17px;
  color: var(--ink, #1a1a1a);
  letter-spacing: 0.14em;
}

.brand-sub {
  font-size: 11px;
  color: var(--ink-muted, #8a8580);
  letter-spacing: 0.2em;
}

.sidebar-nav {
  position: relative;
  flex: 1;
  padding: 8px 0;
  overflow-y: auto;
}

.nav-indicator {
  position: absolute;
  left: 0;
  top: 0;
  width: 2px;
  background: var(--ink, #1a1a1a);
  transition: transform var(--dur-slow) var(--ease-spring),
              height var(--dur-slow) var(--ease-spring);
  will-change: transform;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px 16px;
  color: var(--ink-light, #4a4a4a);
  text-decoration: none;
  font-size: 14px;
  line-height: 1.5;
  letter-spacing: 0.06em;
  transition: background-color var(--dur-base) var(--ease-out),
              color var(--dur-base) var(--ease-out),
              padding-left var(--dur-base) var(--ease-out);
}

.nav-item:hover {
  background: rgba(26, 26, 26, 0.04);
  color: var(--ink, #1a1a1a);
  padding-left: 20px;
}

.nav-item.active {
  color: var(--ink, #1a1a1a);
  font-weight: 600;
  background: rgba(26, 26, 26, 0.05);
}

/* 按下即有反馈，不等路由切换完成 */
.nav-item:active {
  transform: scale(0.98);
}

.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  opacity: 0.75;
}

.nav-item.active .nav-icon {
  opacity: 1;
}

.admin-main {
  flex: 1;
  margin-left: 220px;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 24px;
  background: var(--material-header, #fffcf7);
  backdrop-filter: var(--material-blur, none);
  -webkit-backdrop-filter: var(--material-blur, none);
  position: sticky;
  top: 0;
  z-index: var(--z-header, 50);
  /* 刻意不画 border-bottom：内容滚到它下面时由模糊和这道渐变表达交界，
     一条硬线会在滚动过程中一直横在那儿，比内容本身还抢眼。 */
  transition: box-shadow var(--dur-base) var(--ease-out);
}

.admin-header.is-scrolled {
  box-shadow: 0 1px 0 rgba(26, 26, 26, 0.06), 0 6px 18px rgba(26, 26, 26, 0.05);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.crumb-muted {
  font-size: 13px;
  color: var(--ink-faint, #b5b0a8);
  letter-spacing: 0.08em;
}

.crumb-sep {
  font-size: 12px;
  color: var(--ink-faint, #b5b0a8);
}

.crumb-current {
  font-size: 15px;
  color: var(--ink, #1a1a1a);
  letter-spacing: 0.08em;
}

.logout-btn {
  padding: 6px 18px;
  background: transparent;
  border: 1px solid rgba(26, 26, 26, 0.22);
  border-radius: 2px;
  cursor: pointer;
  font-size: 13px;
  font-family: inherit;
  color: var(--ink-light, #4a4a4a);
  letter-spacing: 0.14em;
  transition: border-color var(--dur-base) var(--ease-out),
              color var(--dur-base) var(--ease-out),
              background-color var(--dur-base) var(--ease-out),
              transform var(--dur-fast) var(--ease-out);
}

.logout-btn:hover {
  border-color: var(--ink, #1a1a1a);
  color: var(--ink, #1a1a1a);
  background: rgba(26, 26, 26, 0.04);
}

.logout-btn:active {
  transform: scale(0.97);
}

.logout-btn:focus-visible {
  outline: 2px solid var(--ink, #1a1a1a);
  outline-offset: 2px;
}

.admin-content {
  flex: 1;
  padding: 24px;
  overflow-y: auto;
}

@media (max-width: 860px) {
  .admin-sidebar {
    width: 64px;
  }

  .brand-text,
  .nav-item span {
    display: none;
  }

  .sidebar-header {
    justify-content: center;
    padding: 16px 8px;
  }

  .nav-item {
    justify-content: center;
    padding: 15px 8px;
  }

  .nav-item:hover {
    padding-left: 8px;
  }

  .admin-main {
    margin-left: 64px;
  }

  .admin-content {
    padding: 16px;
  }
}
</style>
