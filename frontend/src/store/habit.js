import { defineStore } from 'pinia'
import dayjs from 'dayjs'

const CACHE_KEY = 'habit_assistant_cache'

export const useHabitStore = defineStore('habit', {
  state: () => ({
    habits: [],
    checkins: {},
    schedules: [],
    currentSchedule: null,
    starredOrder: [],
    templates: [
      { id: 1, name: '早起作息', items: [
        { time: '06:00', title: '起床洗漱' },
        { time: '06:30', title: '晨练运动' },
        { time: '07:30', title: '早餐' },
        { time: '08:30', title: '开始工作/学习' },
        { time: '12:00', title: '午餐休息' },
        { time: '14:00', title: '下午工作/学习' },
        { time: '18:00', title: '晚餐' },
        { time: '19:00', title: '自由活动' },
        { time: '22:00', title: '准备睡觉' },
        { time: '22:30', title: '入睡' }
      ]},
      { id: 2, name: '学生作息', items: [
        { time: '07:00', title: '起床早餐' },
        { time: '08:00', title: '早读' },
        { time: '08:30', title: '上午课程' },
        { time: '12:00', title: '午餐' },
        { time: '13:00', title: '午休' },
        { time: '14:00', title: '下午课程' },
        { time: '18:00', title: '晚餐' },
        { time: '19:00', title: '晚自习' },
        { time: '21:30', title: '自由活动' },
        { time: '23:00', title: '睡觉' }
      ]},
      { id: 3, name: '健身作息', items: [
        { time: '06:30', title: '起床' },
        { time: '07:00', title: '晨跑30分钟' },
        { time: '08:00', title: '早餐+蛋白质补充' },
        { time: '09:00', title: '工作/学习' },
        { time: '12:00', title: '午餐（高蛋白）' },
        { time: '13:00', title: '午休' },
        { time: '14:00', title: '工作/学习' },
        { time: '17:30', title: '健身房训练' },
        { time: '19:30', title: '晚餐' },
        { time: '20:30', title: '拉伸放松' },
        { time: '22:30', title: '睡觉' }
      ]}
    ]
  }),

  getters: {
    todayCheckins: (state) => {
      const today = dayjs().format('YYYY-MM-DD')
      return state.checkins[today] || {}
    },
    completionRate: (state) => {
      const today = dayjs().format('YYYY-MM-DD')
      const todayCheckins = state.checkins[today] || {}
      if (state.habits.length === 0) return 0
      const completed = Object.values(todayCheckins).filter(v => v).length
      return Math.round((completed / state.habits.length) * 100)
    },
    starredHabits: (state) => {
      const today = dayjs().format('YYYY-MM-DD')
      const todayCheckins = state.checkins[today] || {}
      const starred = state.habits.filter(h => h.starred)
      const orderMap = {}
      state.starredOrder.forEach((id, index) => { orderMap[id] = index })
      starred.sort((a, b) => {
        const aCompleted = todayCheckins[a.id] || false
        const bCompleted = todayCheckins[b.id] || false
        if (aCompleted !== bCompleted) {
          return aCompleted ? 1 : -1
        }
        const aOrder = orderMap[a.id] !== undefined ? orderMap[a.id] : 999
        const bOrder = orderMap[b.id] !== undefined ? orderMap[b.id] : 999
        return aOrder - bOrder
      })
      return starred
    },
    nonStarredHabits: (state) => {
      return state.habits.filter(h => !h.starred)
    },
    weekStats: (state) => {
      const stats = []
      for (let i = 6; i >= 0; i--) {
        const date = dayjs().subtract(i, 'day').format('YYYY-MM-DD')
        const checkins = state.checkins[date] || {}
        const completed = Object.values(checkins).filter(v => v).length
        const total = state.habits.length
        stats.push({
          date,
          label: dayjs().subtract(i, 'day').format('MM/DD'),
          completed,
          total,
          rate: total > 0 ? Math.round((completed / total) * 100) : 0
        })
      }
      return stats
    }
  },

  actions: {
    loadFromCache() {
      try {
        const cached = localStorage.getItem(CACHE_KEY)
        if (cached) {
          const data = JSON.parse(cached)
          this.habits = data.habits || []
          this.checkins = data.checkins || {}
          this.schedules = data.schedules || []
          this.currentSchedule = data.currentSchedule || this.templates[0]
          this.starredOrder = data.starredOrder || []
        } else {
          this.initDefaultData()
        }
      } catch (e) {
        this.initDefaultData()
      }
    },
    
    saveToCache() {
      try {
        localStorage.setItem(CACHE_KEY, JSON.stringify({
          habits: this.habits,
          checkins: this.checkins,
          schedules: this.schedules,
          currentSchedule: this.currentSchedule,
          starredOrder: this.starredOrder
        }))
      } catch (e) {
        console.error('缓存保存失败', e)
      }
    },

    initDefaultData() {
      this.habits = [
        { id: 1, name: '早起', category: '作息', time: '07:00', remind: true, color: '#3b82f6', starred: true },
        { id: 2, name: '阅读30分钟', category: '学习', time: '20:00', remind: true, color: '#10b981', starred: false },
        { id: 3, name: '运动锻炼', category: '健康', time: '18:00', remind: false, color: '#f59e0b', starred: false },
        { id: 4, name: '喝8杯水', category: '健康', time: '', remind: false, color: '#06b6d4', starred: true }
      ]
      this.starredOrder = [1, 4]
      this.currentSchedule = this.templates[0]
      this.saveToCache()
    },

    addHabit(habit) {
      const newHabit = {
        id: Date.now(),
        ...habit,
        color: habit.color || this.getRandomColor(),
        starred: habit.starred || false
      }
      this.habits.push(newHabit)
      if (newHabit.starred) {
        this.starredOrder.push(newHabit.id)
      }
      this.saveToCache()
      return newHabit
    },

    updateHabit(id, habit) {
      const index = this.habits.findIndex(h => h.id === id)
      if (index > -1) {
        const oldStarred = this.habits[index].starred
        this.habits[index] = { ...this.habits[index], ...habit }
        if (habit.starred !== undefined && habit.starred !== oldStarred) {
          if (habit.starred) {
            if (!this.starredOrder.includes(id)) {
              this.starredOrder.push(id)
            }
          } else {
            this.starredOrder = this.starredOrder.filter(sid => sid !== id)
          }
        }
        this.saveToCache()
      }
    },

    deleteHabit(id) {
      this.habits = this.habits.filter(h => h.id !== id)
      this.starredOrder = this.starredOrder.filter(sid => sid !== id)
      this.saveToCache()
    },

    toggleStarred(id) {
      const habit = this.habits.find(h => h.id === id)
      if (habit) {
        habit.starred = !habit.starred
        if (habit.starred) {
          if (!this.starredOrder.includes(id)) {
            this.starredOrder.push(id)
          }
        } else {
          this.starredOrder = this.starredOrder.filter(sid => sid !== id)
        }
        this.saveToCache()
      }
    },

    updateStarredOrder(newOrder) {
      this.starredOrder = newOrder
      this.saveToCache()
    },

    moveStarredHabit(fromIndex, toIndex) {
      const result = [...this.starredOrder]
      const [removed] = result.splice(fromIndex, 1)
      result.splice(toIndex, 0, removed)
      this.starredOrder = result
      this.saveToCache()
    },

    toggleCheckin(habitId, date = null) {
      const targetDate = date || dayjs().format('YYYY-MM-DD')
      if (!this.checkins[targetDate]) {
        this.checkins[targetDate] = {}
      }
      this.checkins[targetDate][habitId] = !this.checkins[targetDate][habitId]
      this.saveToCache()
    },

    setCurrentSchedule(schedule) {
      this.currentSchedule = schedule
      this.saveToCache()
    },

    addCustomSchedule(schedule) {
      const newSchedule = {
        id: Date.now(),
        ...schedule,
        isCustom: true
      }
      this.schedules.push(newSchedule)
      this.saveToCache()
      return newSchedule
    },

    deleteCustomSchedule(id) {
      this.schedules = this.schedules.filter(s => s.id !== id)
      if (this.currentSchedule?.id === id) {
        this.currentSchedule = this.templates[0]
      }
      this.saveToCache()
    },

    getRandomColor() {
      const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899']
      return colors[Math.floor(Math.random() * colors.length)]
    }
  }
})
