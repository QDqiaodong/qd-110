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
  create: (data) => request.post('/habits', data),
  update: (id, data) => request.put(`/habits/${id}`, data),
  delete: (id) => request.delete(`/habits/${id}`),
  toggleStar: (id) => request.post(`/habits/${id}/star`),
  updateStarredOrder: (habitIds) => request.put('/habits/starred/order', { habitIds })
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

export default request
