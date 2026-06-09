import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    name: 'Checkin',
    component: () => import('@/views/Checkin.vue'),
    meta: { title: '今日打卡' }
  },
  {
    path: '/habits',
    name: 'Habits',
    component: () => import('@/views/Habits.vue'),
    meta: { title: '习惯清单' }
  },
  {
    path: '/schedule',
    name: 'Schedule',
    component: () => import('@/views/Schedule.vue'),
    meta: { title: '作息模板' }
  },
  {
    path: '/stats',
    name: 'Stats',
    component: () => import('@/views/Stats.vue'),
    meta: { title: '数据统计' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  document.title = to.meta.title || '习惯养成助手'
  next()
})

export default router
