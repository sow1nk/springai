import { createRouter, createWebHistory } from 'vue-router'
import ChatView from '../views/ChatView.vue'
import AuthView from '../views/AuthView.vue'
import { isAuthenticated } from '../api/auth'

const routes = [
  {
    path: '/',
    name: 'Chat',
    component: ChatView,
    meta: { requiresAuth: true }
  },
  {
    path: '/auth',
    name: 'Auth',
    component: AuthView,
    meta: { requiresGuest: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authenticated = isAuthenticated()

  // 需要认证的页面
  if (to.meta.requiresAuth && !authenticated) {
    next('/auth')
    return
  }

  // 已登录用户访问登录页，重定向到首页
  if (to.meta.requiresGuest && authenticated) {
    next('/')
    return
  }

  next()
})

export default router
