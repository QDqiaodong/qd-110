import axios from 'axios'
import { showToast } from 'vant'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.request.use(
  config => {
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  response => {
    return response.data
  },
  error => {
    const msg = error.response?.data?.message || error.message || '请求失败'
    showToast(msg)
    return Promise.reject(error)
  }
)

export const habitApi = {
  getList: () => request.get('/habits'),
  getMorningCards: () => request.get('/habits/morning-cards'),
  create: (data) => request.post('/habits', data),
  update: (id, data) => request.put(`/habits/${id}`, data),
  delete: (id) => request.delete(`/habits/${id}`),
  toggleStar: (id) => request.post(`/habits/${id}/star`),
  updateStarredOrder: (habitIds) => request.put('/habits/starred/order', { habitIds }),
  getArchivedList: () => request.get('/habits/archived'),
  archive: (id) => request.post(`/habits/${id}/archive`),
  unarchive: (id) => request.post(`/habits/${id}/unarchive`)
}

export const checkinApi = {
  getByDate: (date) => request.get(`/checkins/${date}`),
  toggle: (habitId, date) => request.post('/checkins', { habitId, date })
}

export const scheduleApi = {
  getTemplates: () => request.get('/schedules/templates'),
  getCurrent: () => request.get('/schedules/current'),
  setCurrent: (id) => request.put('/schedules/current', { id })
}

export const statsApi = {
  getWeekStats: () => request.get('/stats/week'),
  getMonthStats: () => request.get('/stats/month')
}

export const challengeApi = {
  getActiveList: () => request.get('/challenges/active'),
  getHistory: () => request.get('/challenges/history'),
  getByHabit: (habitId) => request.get(`/challenges/habit/${habitId}`),
  getDetail: (id) => request.get(`/challenges/${id}`),
  getStats: (id) => request.get(`/challenges/${id}/stats`),
  start: (habitId, totalDays = 21) => request.post('/challenges', { habitId, totalDays }),
  refresh: (id) => request.post(`/challenges/${id}/refresh`),
  giveUp: (id) => request.post(`/challenges/${id}/give-up`)
}

export const milestoneApi = {
  getByHabit: (habitId) => request.get(`/habit-milestones/habit/${habitId}`),
  getAll: () => request.get('/habit-milestones/all'),
  getDefinitions: () => request.get('/habit-milestones/definitions')
}

export default request
