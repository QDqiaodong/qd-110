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
  },
  {
    path: '/archive',
    name: 'Archive',
    component: () => import('@/views/Archive.vue'),
    meta: { title: '归档习惯' }
  },
  {
    path: '/challenge/:id',
    name: 'Challenge',
    component: () => import('@/views/Challenge.vue'),
    meta: { title: '21天挑战' }
  },
  {
    path: '/habit/:id',
    name: 'HabitDetail',
    component: () => import('@/views/HabitDetail.vue'),
    meta: { title: '习惯详情' }
  },
  {
    path: '/review',
    name: 'Review',
    component: () => import('@/views/Review.vue'),
    meta: { title: '作息偏差复盘' }
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
