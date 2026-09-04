import { createRouter, createWebHistory } from 'vue-router'
import { getToken, isTokenExpired, setRouterInstance } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      children: [
        {
          path: '',
          name: 'Home',
          component: () => import('@/views/article/HomeView.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'essay',
          name: 'EssayHome',
          component: () => import('@/views/essay/EssayHomeView.vue'),
          meta: { title: '随笔' },
        },
        {
          path: 'essay/:id',
          name: 'EssayDetail',
          component: () => import('@/views/essay/EssayDetailView.vue'),
          meta: { title: '随笔详情' },
        },
        {
          path: 'essay/write',
          name: 'EssayWrite',
          component: () => import('@/views/essay/EssayEditorView.vue'),
          meta: { title: '写随笔', requiresAuth: true },
        },
        {
          path: 'essay/edit/:id',
          name: 'EssayEdit',
          component: () => import('@/views/essay/EssayEditorView.vue'),
          meta: { title: '编辑随笔', requiresAuth: true },
        },
        {
          path: 'diary',
          name: 'DiaryHome',
          component: () => import('@/views/diary/DiaryHomeView.vue'),
          meta: { title: '日记', requiresAuth: true },
        },
        {
          path: 'diary/write',
          name: 'DiaryWrite',
          component: () => import('@/views/diary/DiaryWriteView.vue'),
          meta: { title: '写日记', requiresAuth: true },
        },
        {
          path: 'diary/edit/:id',
          name: 'DiaryEdit',
          component: () => import('@/views/diary/DiaryWriteView.vue'),
          meta: { title: '编辑日记', requiresAuth: true },
        },
        {
          path: 'article/:id',
          name: 'ArticleDetail',
          component: () => import('@/views/article/ArticleDetailView.vue'),
          meta: { title: '文章详情' },
        },
        {
          path: 'write',
          name: 'ArticleWrite',
          component: () => import('@/views/article/ArticleEditorView.vue'),
          meta: { title: '写文章', requiresAuth: true },
        },
        {
          path: 'edit/:id',
          name: 'ArticleEdit',
          component: () => import('@/views/article/ArticleEditorView.vue'),
          meta: { title: '编辑文章', requiresAuth: true },
        },
        {
          path: 'profile',
          name: 'Profile',
          component: () => import('@/views/user/ProfileView.vue'),
          meta: { title: '个人资料', requiresAuth: true },
        },
        {
          path: 'user/:userId',
          name: 'UserProfile',
          component: () => import('@/views/user/UserProfileView.vue'),
          meta: { title: '用户主页' },
        },
        {
          path: 'chat',
          name: 'Chat',
          component: () => import('@/views/chat/ChatView.vue'),
          meta: { title: '私信', requiresAuth: true },
        },
        {
          path: 'ai',
          name: 'AIChat',
          component: () => import('@/views/chat/AIChatView.vue'),
          meta: { title: 'AI助手', requiresAuth: true },
        },
      ],
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/user/AuthView.vue'),
      meta: { title: '登录', guest: true },
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/user/AuthView.vue'),
      meta: { title: '注册', guest: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: () => import('@/views/NotFoundView.vue'),
      meta: { title: '页面不存在' },
    },
  ],
})

setRouterInstance(router)

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 墨栈` : '墨栈'

  const hasToken = !!getToken()
  const tokenExpired = isTokenExpired()

  if (to.meta.guest && hasToken && !tokenExpired) {
    next({ name: 'Home' })
    return
  }

  if (to.meta.requiresAuth && (!hasToken || tokenExpired)) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  next()
})

export default router
