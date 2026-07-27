import { createRouter, createWebHistory } from 'vue-router'
import { getToken, isTokenExpired } from '@/utils/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue')
    },
    {
      path: '/',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/user'
        },
        {
          path: '/user',
          name: 'AdminUser',
          component: () => import('@/views/admin/AdminUserView.vue'),
          meta: { title: '用户管理' }
        },
        {
          path: '/category',
          name: 'AdminCategory',
          component: () => import('@/views/admin/AdminCategoryView.vue'),
          meta: { title: '分类管理' }
        },
        {
          path: '/tag',
          name: 'AdminTag',
          component: () => import('@/views/admin/AdminTagView.vue'),
          meta: { title: '标签管理' }
        },
        {
          path: '/essay',
          name: 'AdminEssay',
          component: () => import('@/views/admin/AdminEssayView.vue'),
          meta: { title: '随笔管理' }
        },
        {
          path: '/comment',
          name: 'AdminComment',
          component: () => import('@/views/admin/AdminCommentView.vue'),
          meta: { title: '评论管理' }
        }
      ]
    }
  ]
})

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 墨栈管理` : '墨栈管理'

  const hasToken = !!getToken()
  const tokenExpired = isTokenExpired()

  if (to.path === '/login') {
    if (hasToken && !tokenExpired) {
      next('/')
      return
    }
    next()
    return
  }

  if (to.meta.requiresAuth && (!hasToken || tokenExpired)) {
    next('/login')
    return
  }

  next()
})

export default router
