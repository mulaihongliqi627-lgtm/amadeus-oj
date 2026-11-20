import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { guardRoute } from './guard'

export { getLoginDestination } from './guard'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
      meta: { title: '管理员登录', public: true },
    },
    {
      path: '/',
      component: () => import('../layouts/AdminLayout.vue'),
      children: [
        { path: '', redirect: '/dashboard' },
        {
          path: 'dashboard',
          name: 'dashboard',
          component: () => import('../views/DashboardView.vue'),
          meta: { title: '工作台' },
        },
        {
          path: 'admins',
          name: 'admins',
          component: () => import('../views/AdminsView.vue'),
          meta: { title: '管理员管理' },
        },
        {
          path: 'questions',
          name: 'questions',
          component: () => import('../views/ComingSoonView.vue'),
          meta: { title: '题目管理', module: 'questions' },
        },
        {
          path: 'contests',
          name: 'contests',
          component: () => import('../views/ComingSoonView.vue'),
          meta: { title: '竞赛管理', module: 'contests' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
      meta: { title: '页面未找到', public: true },
    },
  ],
})

router.beforeEach((to) => guardRoute(to, useAuthStore()))

router.afterEach((to) => {
  document.title = `${String(to.meta.title || '管理工作台')} · Amadeus OJ`
})
