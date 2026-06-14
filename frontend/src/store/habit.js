import { defineStore } from 'pinia'
import dayjs from 'dayjs'
import { habitApi, checkinApi, milestoneApi, streakApi, scheduleApi } from '@/utils/request'

const CACHE_KEY = 'habit_assistant_cache'

export const useHabitStore = defineStore('habit', {
  state: () => ({
    habits: [],
    archivedHabits: [],
    checkins: {},
    missReasons: {},
    schedules: [],
    currentSchedule: null,
    starredOrder: [],
    nonStarredOrder: [],
    _morningCardsLegacy: [],
    habitsLoaded: false,
    _loadHabitsVersion: 0,
    _togglingHabits: new Set(),
    showHabitMilestoneModal: false,
    currentHabitMilestone: null,
    habitDetails: {},
    streakCache: {},
    archivedStreakCache: {},
    missReasonPresets: ['加班', '出门', '忘记', '状态差'],
    templateHistory: [],
    scheduleTags: ['上学日', '备考期', '健身期', '工作期', '假期', '其他'],
    templates: [
      { id: 1, name: '早起作息', version: 1, parentId: null, createTime: null, tag: '工作期',
        weekdayItems: [
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
        ],
        weekendItems: [
          { time: '08:00', title: '自然醒起床' },
          { time: '08:30', title: '悠闲早餐' },
          { time: '09:30', title: '晨间运动/散步' },
          { time: '11:00', title: '处理家务' },
          { time: '12:30', title: '午餐' },
          { time: '14:00', title: '午休/阅读' },
          { time: '16:00', title: '兴趣爱好时间' },
          { time: '18:30', title: '晚餐' },
          { time: '20:00', title: '休闲娱乐' },
          { time: '23:00', title: '准备睡觉' }
        ]
      },
      { id: 2, name: '学生作息', version: 1, parentId: null, createTime: null, tag: '上学日',
        weekdayItems: [
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
        ],
        weekendItems: [
          { time: '09:00', title: '起床' },
          { time: '09:30', title: '早餐' },
          { time: '10:30', title: '作业复习' },
          { time: '12:30', title: '午餐' },
          { time: '14:00', title: '午休' },
          { time: '15:30', title: '户外运动' },
          { time: '18:00', title: '晚餐' },
          { time: '19:30', title: '休闲娱乐' },
          { time: '22:00', title: '准备睡觉' },
          { time: '22:30', title: '入睡' }
        ]
      },
      { id: 3, name: '健身作息', version: 1, parentId: null, createTime: null, tag: '健身期',
        weekdayItems: [
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
        ],
        weekendItems: [
          { time: '07:30', title: '起床' },
          { time: '08:00', title: '轻量早餐' },
          { time: '09:00', title: '长时间户外训练' },
          { time: '11:30', title: '补充营养' },
          { time: '13:00', title: '午餐' },
          { time: '14:30', title: '午休' },
          { time: '16:00', title: '主动恢复/瑜伽' },
          { time: '18:00', title: '晚餐（欺骗餐）' },
          { time: '20:00', title: '休闲放松' },
          { time: '22:30', title: '睡觉' }
        ]
      }
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
      const activeIds = state.habits.map(h => h.id)
      const completed = Object.entries(todayCheckins).filter(([id, v]) => v && activeIds.includes(Number(id))).length
      return Math.round((completed / state.habits.length) * 100)
    },
    starredHabits: (state) => {
      const today = dayjs().format('YYYY-MM-DD')
      const todayCheckins = state.checkins[today] || {}
      const starred = state.habits.filter(h => h.starred).map(h => ({ ...h }))
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
    morningCards(state) {
      const today = dayjs().format('YYYY-MM-DD')
      const todayCheckins = state.checkins[today] || {}
      const starred = state.habits.filter(h => h.starred).map(h => ({
        ...h,
        completed: !!todayCheckins[h.id]
      }))

      const scored = starred.map(h => {
        let score = 0
        const period = this.getTimePeriod(h.time)
        if (period === 'morning') score += 100
        else if (period === 'daytime') score += 50
        if (!h.completed) score += 200
        const orderIdx = state.starredOrder.indexOf(h.id)
        score += orderIdx >= 0 ? (100 - orderIdx) : 0
        return { ...h, score }
      })

      scored.sort((a, b) => {
        if (a.completed !== b.completed) return a.completed ? 1 : -1
        return b.score - a.score
      })

      return scored.slice(0, 3)
    },
    nonStarredHabits: (state) => {
      const nonStarred = state.habits.filter(h => !h.starred).map(h => ({ ...h }))
      const orderMap = {}
      state.nonStarredOrder.forEach((id, index) => { orderMap[id] = index })
      nonStarred.sort((a, b) => {
        const aOrder = orderMap[a.id] !== undefined ? orderMap[a.id] : 999
        const bOrder = orderMap[b.id] !== undefined ? orderMap[b.id] : 999
        return aOrder - bOrder
      })
      return nonStarred
    },
    getTimePeriod: () => (time) => {
      if (!time) return 'other'
      const hour = parseInt(time.split(':')[0], 10)
      if (hour >= 4 && hour < 10) return 'morning'
      if (hour >= 10 && hour < 18) return 'daytime'
      return 'night'
    },
    morningHabits(state) {
      const habits = state.habits.filter(h => this.getTimePeriod(h.time) === 'morning')
      return habits.sort((a, b) => (a.time || '99:99').localeCompare(b.time || '99:99'))
    },
    daytimeHabits(state) {
      const habits = state.habits.filter(h => this.getTimePeriod(h.time) === 'daytime')
      return habits.sort((a, b) => (a.time || '99:99').localeCompare(b.time || '99:99'))
    },
    nighttimeHabits(state) {
      const habits = state.habits.filter(h => this.getTimePeriod(h.time) === 'night')
      return habits.sort((a, b) => {
        const aTime = a.time || '99:99'
        const bTime = b.time || '99:99'
        const aHour = parseInt(aTime.split(':')[0], 10)
        const bHour = parseInt(bTime.split(':')[0], 10)
        const aSort = aHour >= 18 ? aHour : aHour + 24
        const bSort = bHour >= 18 ? bHour : bHour + 24
        return aSort - bSort
      })
    },
    otherTimeHabits(state) {
      return state.habits.filter(h => this.getTimePeriod(h.time) === 'other')
    },
    timePeriodGroups(state) {
      const today = dayjs().format('YYYY-MM-DD')
      const todayCheckins = state.checkins[today] || {}
      
      const sortByTimeAndCompletion = (habits) => {
        const sorted = [...habits].sort((a, b) => {
          const aCompleted = todayCheckins[a.id] || false
          const bCompleted = todayCheckins[b.id] || false
          if (aCompleted !== bCompleted) {
            return aCompleted ? 1 : -1
          }
          return (a.time || '99:99').localeCompare(b.time || '99:99')
        })
        return sorted
      }

      const morning = sortByTimeAndCompletion(this.morningHabits)
      const daytime = sortByTimeAndCompletion(this.daytimeHabits)
      const night = this.nighttimeHabits.map(h => ({ ...h })).sort((a, b) => {
        const aCompleted = todayCheckins[a.id] || false
        const bCompleted = todayCheckins[b.id] || false
        if (aCompleted !== bCompleted) {
          return aCompleted ? 1 : -1
        }
        const aHour = parseInt((a.time || '99:99').split(':')[0], 10)
        const bHour = parseInt((b.time || '99:99').split(':')[0], 10)
        const aSort = aHour >= 18 ? aHour : aHour + 24
        const bSort = bHour >= 18 ? bHour : bHour + 24
        return aSort - bSort
      })
      const other = sortByTimeAndCompletion(this.otherTimeHabits)

      return [
        { key: 'morning', title: '🌅 清晨', subtitle: '04:00 - 10:00', habits: morning },
        { key: 'daytime', title: '☀️ 白天', subtitle: '10:00 - 18:00', habits: daytime },
        { key: 'night', title: '🌙 夜间', subtitle: '18:00 - 04:00', habits: night },
        { key: 'other', title: '📌 其他', subtitle: '未设置时间', habits: other }
      ]
    },
    weekStats: (state) => {
      const stats = []
      const activeIds = state.habits.map(h => h.id)
      for (let i = 6; i >= 0; i--) {
        const date = dayjs().subtract(i, 'day').format('YYYY-MM-DD')
        const checkins = state.checkins[date] || {}
        const completed = Object.entries(checkins).filter(([id, v]) => v && activeIds.includes(Number(id))).length
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
    },
    getScheduleItemsByTime: (state) => {
      if (!state.currentSchedule) return []
      const items = state.currentSchedule.weekdayItems || state.currentSchedule.items || []
      return [...items].sort((a, b) => a.time.localeCompare(b.time))
    },
    getWeekdayItems: (state) => {
      if (!state.currentSchedule) return []
      const items = state.currentSchedule.weekdayItems || state.currentSchedule.items || []
      return [...items].sort((a, b) => a.time.localeCompare(b.time))
    },
    getWeekendItems: (state) => {
      if (!state.currentSchedule) return []
      const items = state.currentSchedule.weekendItems || state.currentSchedule.items || []
      return [...items].sort((a, b) => a.time.localeCompare(b.time))
    },
    isWeekend: () => {
      const day = dayjs().day()
      return day === 0 || day === 6
    },
    getTodayItems: (state, getters) => {
      return getters.isWeekend ? getters.getWeekendItems : getters.getWeekdayItems
    },
    getHabitsByTimePeriod(state) {
      return (period) => {
        const habits = state.habits.filter(h => this.getTimePeriod(h.time) === period)
        return habits.sort((a, b) => (a.time || '99:99').localeCompare(b.time || '99:99'))
      }
    },
    getTemplateVersions: (state) => (templateId) => {
      const allTemplates = [...state.templates, ...state.schedules]
      const versions = allTemplates.filter(t => t.id === templateId || t.parentId === templateId || 
        (t.parentId && allTemplates.some(p => p.id === templateId && (p.parentId === t.parentId || p.id === t.parentId))))
      return versions.sort((a, b) => {
        if (a.id === templateId) return -1
        if (b.id === templateId) return 1
        return (b.createTime || 0) - (a.createTime || 0)
      })
    },
    getTemplateVersionTree: (state) => (templateId) => {
      const allTemplates = [...state.templates, ...state.schedules]
      const buildTree = (id) => {
        const template = allTemplates.find(t => t.id === id)
        if (!template) return null
        const children = allTemplates.filter(t => t.parentId === id).map(t => buildTree(t.id)).filter(Boolean)
        return { ...template, children }
      }
      return buildTree(templateId)
    },
    getDeviationAnalysis(state) {
      return (date = null) => {
        const targetDate = date || dayjs().format('YYYY-MM-DD')
        const checkins = state.checkins[targetDate] || {}
        const dayOfWeek = dayjs(targetDate).day()
        const isWeekend = dayOfWeek === 0 || dayOfWeek === 6
        let scheduleItems
        if (isWeekend) {
          scheduleItems = state.currentSchedule?.weekendItems || state.currentSchedule?.items || []
        } else {
          scheduleItems = state.currentSchedule?.weekdayItems || state.currentSchedule?.items || []
        }
        scheduleItems = [...scheduleItems].sort((a, b) => a.time.localeCompare(b.time))
        const habits = state.habits
        
        const periods = [
          { key: 'morning', title: '🌅 清晨', start: 4, end: 10 },
          { key: 'daytime', title: '☀️ 白天', start: 10, end: 18 },
          { key: 'night', title: '🌙 夜间', start: 18, end: 28 }
        ]
        
        const analysis = periods.map(period => {
          const periodScheduleItems = scheduleItems.filter(item => {
            const hour = parseInt(item.time.split(':')[0], 10)
            const adjustedHour = hour < 4 ? hour + 24 : hour
            return adjustedHour >= period.start && adjustedHour < period.end
          })
          
          const periodHabits = habits.filter(h => {
            const hPeriod = this.getTimePeriod(h.time)
            return hPeriod === period.key
          })
          
          const periodCompletedHabits = periodHabits.filter(h => checkins[h.id] === true)
          const periodMissedHabits = periodHabits.filter(h => checkins[h.id] !== true)
          
          let deviationType = 'normal'
          let deviationDesc = '作息正常'
          let severity = 0
          
          if (periodScheduleItems.length > 5) {
            deviationType = 'overload'
            deviationDesc = '任务过度堆叠'
            severity = 2
          } else if (periodMissedHabits.length >= 2 && periodHabits.length > 0) {
            deviationType = 'procrastination'
            deviationDesc = '存在拖延情况'
            severity = 1
          } else if (periodHabits.length === 0 && periodScheduleItems.length > 0) {
            deviationType = 'missing'
            deviationDesc = '有计划但无对应习惯'
            severity = 1
          }
          
          return {
            ...period,
            scheduleItems: periodScheduleItems,
            habits: periodHabits,
            completedHabits: periodCompletedHabits,
            missedHabits: periodMissedHabits,
            deviationType,
            deviationDesc,
            severity,
            completionRate: periodHabits.length > 0 
              ? Math.round((periodCompletedHabits.length / periodHabits.length) * 100) 
              : (periodScheduleItems.length > 0 ? 0 : 100)
          }
        })
        
        const overall = {
          totalScheduleItems: scheduleItems.length,
          totalHabits: habits.length,
          completedHabits: habits.filter(h => checkins[h.id] === true).length,
          overallRate: habits.length > 0 
            ? Math.round((habits.filter(h => checkins[h.id] === true).length / habits.length) * 100) 
            : 0,
          worstPeriod: analysis.reduce((worst, curr) => 
            curr.severity > worst.severity ? curr : worst, 
            analysis[0]
          ),
          suggestions: []
        }
        
        if (overall.worstPeriod.deviationType === 'overload') {
          overall.suggestions.push(`⚠️ ${overall.worstPeriod.title}时段安排了${overall.worstPeriod.scheduleItems.length}项任务，建议精简至3-4项`)
        }
        if (overall.worstPeriod.deviationType === 'procrastination') {
          overall.suggestions.push(`⚠️ ${overall.worstPeriod.title}时段完成率仅${overall.worstPeriod.completionRate}%，建议设置更明确的提醒`)
        }
        if (overall.worstPeriod.deviationType === 'missing') {
          overall.suggestions.push(`💡 ${overall.worstPeriod.title}时段有计划但未设置对应习惯，建议添加相关习惯`)
        }
        if (overall.overallRate < 50) {
          overall.suggestions.push('💪 整体完成率偏低，建议从减少习惯数量开始，聚焦最重要的2-3个习惯')
        }
        
        return { periods: analysis, overall }
      }
    },
    getMissedHabitsWithReasons: (state) => (date = null) => {
      const targetDate = date || dayjs().format('YYYY-MM-DD')
      const checkins = state.checkins[targetDate] || {}
      const reasons = state.missReasons[targetDate] || {}
      const missed = []
      state.habits.forEach(habit => {
        if (!checkins[habit.id]) {
          missed.push({
            ...habit,
            reason: reasons[habit.id] || null
          })
        }
      })
      return missed
    },
    getMissReasonStats: (state) => (days = 7) => {
      const stats = {}
      const today = dayjs()
      for (let i = days - 1; i >= 0; i--) {
        const dateStr = today.subtract(i, 'day').format('YYYY-MM-DD')
        const reasons = state.missReasons[dateStr] || {}
        Object.values(reasons).forEach(reason => {
          if (reason) {
            stats[reason] = (stats[reason] || 0) + 1
          }
        })
      }
      return Object.entries(stats)
        .map(([reason, count]) => ({ reason, count }))
        .sort((a, b) => b.count - a.count)
    },
    getTimeSlotComparison(state) {
      return (date = null) => {
        const targetDate = date || dayjs().format('YYYY-MM-DD')
        const checkins = state.checkins[targetDate] || {}
        const dayOfWeek = dayjs(targetDate).day()
        const isWeekend = dayOfWeek === 0 || dayOfWeek === 6
        let scheduleItems
        if (isWeekend) {
          scheduleItems = state.currentSchedule?.weekendItems || state.currentSchedule?.items || []
        } else {
          scheduleItems = state.currentSchedule?.weekdayItems || state.currentSchedule?.items || []
        }
        scheduleItems = [...scheduleItems].sort((a, b) => a.time.localeCompare(b.time))
        const habits = state.habits
        
        const timeSlots = []
        
        scheduleItems.forEach(item => {
          const hour = parseInt(item.time.split(':')[0], 10)
          const matchedHabits = habits.filter(h => {
            if (!h.time) return false
            const hHour = parseInt(h.time.split(':')[0], 10)
            return Math.abs(hour - hHour) <= 1
          })
          
          timeSlots.push({
            time: item.time,
            planned: item.title,
            actualHabits: matchedHabits.map(h => ({
              ...h,
              completed: checkins[h.id] === true
            })),
            deviation: matchedHabits.length === 0 
              ? 'missing' 
              : matchedHabits.every(h => checkins[h.id] !== true)
                ? 'missed'
                : matchedHabits.some(h => checkins[h.id] !== true)
                  ? 'partial'
                  : 'completed'
          })
        })
        
        return timeSlots
      }
    },
    templateHistoryWithStats(state) {
      const activeIds = state.habits.map(h => h.id)
      return state.templateHistory.map(record => {
        const start = dayjs(record.startDate)
        const end = record.endDate ? dayjs(record.endDate) : dayjs()
        const durationDays = end.diff(start, 'day') + 1
        let completedCount = 0
        let totalPossible = 0
        for (let i = 0; i < durationDays; i++) {
          const dateStr = start.add(i, 'day').format('YYYY-MM-DD')
          const dayCheckins = state.checkins[dateStr] || {}
          activeIds.forEach(id => {
            totalPossible++
            if (dayCheckins[id]) completedCount++
          })
        }
        const completionRate = totalPossible > 0 ? Math.round((completedCount / totalPossible) * 100) : 0
        let durationText = ''
        if (durationDays >= 30) {
          durationText = `${Math.floor(durationDays / 30)}个月${durationDays % 30 > 0 ? durationDays % 30 + '天' : ''}`
        } else {
          durationText = `${durationDays}天`
        }
        return {
          ...record,
          durationDays,
          durationText,
          completionRate,
          isActive: !record.endDate
        }
      }).reverse()
    },
    timeToMinutes() {
      return (timeStr) => {
        if (!timeStr) return -1
        const [h, m] = timeStr.split(':').map(Number)
        return h * 60 + m
      }
    },
    getUncompletedHabits(state) {
      const today = dayjs().format('YYYY-MM-DD')
      const todayCheckins = state.checkins[today] || {}
      return state.habits.filter(h => !todayCheckins[h.id])
    },
    gapSuggestions(state) {
      const now = dayjs()
      const nowMinutes = now.hour() * 60 + now.minute()
      const dayOfWeek = now.day()
      const isWeekend = dayOfWeek === 0 || dayOfWeek === 6
      let todayItems = []
      if (state.currentSchedule) {
        const rawItems = isWeekend 
          ? (state.currentSchedule.weekendItems || state.currentSchedule.items || [])
          : (state.currentSchedule.weekdayItems || state.currentSchedule.items || [])
        todayItems = [...rawItems].sort((a, b) => a.time.localeCompare(b.time))
      }
      const today = dayjs().format('YYYY-MM-DD')
      const todayCheckins = state.checkins[today] || {}
      const uncompletedHabits = state.habits.filter(h => !todayCheckins[h.id])
      const suggestions = []

      const timeToMin = (t) => {
        const [h, m] = t.split(':').map(Number)
        return h * 60 + m
      }

      const minToTime = (mins) => {
        const h = Math.floor(mins / 60)
        const m = mins % 60
        return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
      }

      const formatGapMinutes = (mins) => {
        if (mins >= 60) {
          const h = Math.floor(mins / 60)
          const m = mins % 60
          return m > 0 ? `${h}小时${m}分钟` : `${h}小时`
        }
        return `${mins}分钟`
      }

      let nextScheduleItem = null
      for (let i = 0; i < todayItems.length; i++) {
        const itemMin = timeToMin(todayItems[i].time)
        if (itemMin > nowMinutes) {
          nextScheduleItem = { ...todayItems[i], minutes: itemMin }
          break
        }
      }

      if (nextScheduleItem) {
        const gap = nextScheduleItem.minutes - nowMinutes
        if (gap >= 5 && gap <= 120) {
          const matchedHabits = uncompletedHabits.filter(h => {
            if (!h.time) return true
            const hMin = timeToMin(h.time)
            return Math.abs(hMin - nowMinutes) <= 60 || Math.abs(hMin - nextScheduleItem.minutes) <= 60
          })
          const suggestedHabits = matchedHabits.length > 0 
            ? matchedHabits.slice(0, 2) 
            : uncompletedHabits.slice(0, 2)

          const timeLabel = now.hour() < 12 ? '上午' : (now.hour() < 18 ? '下午' : '晚上')
          suggestions.push({
            id: 'immediate-gap',
            type: 'time-gap',
            icon: '⏱️',
            title: `${timeLabel}${nextScheduleItem.time}前还有${formatGapMinutes(gap)}空档`,
            subtitle: `下一项：${nextScheduleItem.title}`,
            gapMinutes: gap,
            suggestedHabits,
            priority: 100 - Math.min(gap, 100)
          })
        }
      }

      const sceneMatchers = [
        {
          id: 'after-dinner',
          keywords: ['晚餐', '晚饭', '吃饭'],
          match: (item) => item.title.includes('晚餐') || item.title.includes('晚饭'),
          sceneTitle: '晚饭后',
          sceneIcon: '🌙',
          preferredCategories: ['阅读', '学习', '生活'],
          timeWindow: { start: 30, end: 120 }
        },
        {
          id: 'after-lunch',
          keywords: ['午餐', '午饭'],
          match: (item) => item.title.includes('午餐') || item.title.includes('午饭'),
          sceneTitle: '午饭后',
          sceneIcon: '☀️',
          preferredCategories: ['阅读', '生活', '健康'],
          timeWindow: { start: 20, end: 90 }
        },
        {
          id: 'after-breakfast',
          keywords: ['早餐', '早饭'],
          match: (item) => item.title.includes('早餐') || item.title.includes('早饭'),
          sceneTitle: '早饭后',
          sceneIcon: '🌅',
          preferredCategories: ['学习', '阅读', '健康'],
          timeWindow: { start: 20, end: 90 }
        },
        {
          id: 'before-sleep',
          keywords: ['睡觉', '入睡', '准备睡觉'],
          match: (item) => item.title.includes('睡觉') || item.title.includes('入睡'),
          sceneTitle: '睡前',
          sceneIcon: '💤',
          preferredCategories: ['阅读', '生活', '学习'],
          timeWindow: { start: -60, end: 0 }
        },
        {
          id: 'after-exercise',
          keywords: ['运动', '锻炼', '训练', '晨练', '健身', '晨跑', '户外', '拉伸'],
          match: (item) => {
            const title = item.title
            return title.includes('运动') || title.includes('锻炼') || title.includes('训练') 
              || title.includes('晨练') || title.includes('健身') || title.includes('晨跑')
              || title.includes('户外') || title.includes('拉伸')
          },
          sceneTitle: '运动后',
          sceneIcon: '💪',
          preferredCategories: ['健康', '生活'],
          timeWindow: { start: 10, end: 60 }
        }
      ]

      for (let i = 0; i < todayItems.length; i++) {
        const currentItem = todayItems[i]
        const currentItemMin = timeToMin(currentItem.time)
        const nextItem = i < todayItems.length - 1 ? todayItems[i + 1] : null
        const nextItemMin = nextItem ? timeToMin(nextItem.time) : 24 * 60

        for (const matcher of sceneMatchers) {
          if (matcher.match(currentItem)) {
            const sceneStartMin = currentItemMin + matcher.timeWindow.start
            const sceneEndMin = currentItemMin + matcher.timeWindow.end
            const isInSceneWindow = nowMinutes >= sceneStartMin && nowMinutes <= sceneEndMin
            const isUpcomingScene = nowMinutes < sceneStartMin && (sceneStartMin - nowMinutes) <= 180

            if (isInSceneWindow || isUpcomingScene) {
              const sceneHabits = uncompletedHabits.filter(h => {
                if (matcher.preferredCategories.includes(h.category)) return true
                if (!h.time) return true
                const hMin = timeToMin(h.time)
                return hMin >= sceneStartMin && hMin <= sceneEndMin
              })

              const preferredHabits = sceneHabits.filter(h => matcher.preferredCategories.includes(h.category))
              const otherHabits = sceneHabits.filter(h => !matcher.preferredCategories.includes(h.category))
              const sortedHabits = [...preferredHabits, ...otherHabits].slice(0, 3)

              if (sortedHabits.length > 0) {
                let sceneTitleText = `${matcher.sceneTitle}适合安排${sortedHabits[0].category}`
                if (sortedHabits.length > 1) {
                  sceneTitleText = `${matcher.sceneTitle}适合安排${sortedHabits[0].name}或${sortedHabits[1].category}`
                }

                suggestions.push({
                  id: `scene-${matcher.id}-${i}`,
                  type: 'scene',
                  icon: matcher.sceneIcon,
                  title: sceneTitleText,
                  subtitle: isInSceneWindow 
                    ? `现在正是好时机（${minToTime(sceneStartMin)} - ${minToTime(sceneEndMin)}）` 
                    : `即将到来：${minToTime(sceneStartMin)} - ${minToTime(sceneEndMin)}`,
                  anchorItem: currentItem.title,
                  suggestedHabits: sortedHabits,
                  priority: isInSceneWindow ? 90 : 50
                })
              }
            }
          }
        }

        if (nextItem) {
          const betweenGap = nextItemMin - currentItemMin
          if (betweenGap >= 45 && betweenGap <= 180) {
            const gapHabits = uncompletedHabits.filter(h => {
              if (!h.time) return true
              const hMin = timeToMin(h.time)
              return hMin >= currentItemMin && hMin <= nextItemMin
            })

            if (gapHabits.length > 0 && nowMinutes < nextItemMin && nowMinutes >= currentItemMin - 30) {
              const remainingGap = nextItemMin - Math.max(nowMinutes, currentItemMin)
              if (remainingGap >= 15) {
                suggestions.push({
                  id: `between-gap-${i}`,
                  type: 'between-gap',
                  icon: '🎯',
                  title: `${currentItem.title}到${nextItem.title}之间有${formatGapMinutes(betweenGap)}可利用`,
                  subtitle: `还剩约${formatGapMinutes(remainingGap)} · ${currentItem.time} - ${nextItem.time}`,
                  gapMinutes: betweenGap,
                  remainingMinutes: remainingGap,
                  suggestedHabits: gapHabits.slice(0, 2),
                  priority: 70 - Math.min(remainingGap, 60)
                })
              }
            }
          }
        }
      }

      const habitWithoutTime = uncompletedHabits.filter(h => !h.time)
      if (habitWithoutTime.length > 0 && now.hour() >= 10 && now.hour() <= 20) {
        const lightHabits = habitWithoutTime.filter(h => {
          const name = h.name
          return name.includes('喝水') || name.includes('杯') || name.includes('分钟') 
            || name.includes('伸展') || name.includes('深呼吸')
        })
        const displayHabits = lightHabits.length > 0 ? lightHabits.slice(0, 2) : habitWithoutTime.slice(0, 2)
        if (displayHabits.length > 0) {
          suggestions.push({
            id: 'light-habits',
            type: 'light',
            icon: '✨',
            title: `别忘了${displayHabits.map(h => h.name).join('、')}`,
            subtitle: '零散时间插入轻量习惯',
            suggestedHabits: displayHabits,
            priority: 40
          })
        }
      }

      suggestions.sort((a, b) => (b.priority || 0) - (a.priority || 0))
      return suggestions.slice(0, 3)
    }
  },

  actions: {
    loadFromCache() {
      if (this.habitsLoaded) return
      try {
        const cached = localStorage.getItem(CACHE_KEY)
        if (cached) {
          const data = JSON.parse(cached)
          this.habits = data.habits || []
          this.archivedHabits = data.archivedHabits || []
          this.checkins = data.checkins || {}
          this.missReasons = data.missReasons || {}
          this.schedules = (data.schedules || []).map(s => this.migrateScheduleData(s))
          this.currentSchedule = this.migrateScheduleData(data.currentSchedule || this.templates[0])
          this.starredOrder = data.starredOrder || []
          this.nonStarredOrder = data.nonStarredOrder || []
          this.templateHistory = data.templateHistory || []
          this.templates.forEach(t => {
            if (!t.createTime) {
              t.createTime = dayjs().format('YYYY-MM-DD HH:mm')
            }
          })
          this.habitsLoaded = true
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
          archivedHabits: this.archivedHabits,
          checkins: this.checkins,
          missReasons: this.missReasons,
          schedules: this.schedules,
          currentSchedule: this.currentSchedule,
          starredOrder: this.starredOrder,
          nonStarredOrder: this.nonStarredOrder,
          templateHistory: this.templateHistory
        }))
      } catch (e) {
        console.error('缓存保存失败', e)
      }
    },
    saveMissReason(habitId, reason, date = null) {
      const targetDate = date || dayjs().format('YYYY-MM-DD')
      if (!this.missReasons[targetDate]) {
        this.missReasons[targetDate] = {}
      }
      if (reason) {
        this.missReasons[targetDate][habitId] = reason
      } else {
        delete this.missReasons[targetDate][habitId]
      }
      this.saveToCache()
    },
    getMissReason(habitId, date = null) {
      const targetDate = date || dayjs().format('YYYY-MM-DD')
      return this.missReasons[targetDate]?.[habitId] || null
    },
    initDefaultData() {
      const nowStr = dayjs().format('YYYY-MM-DD HH:mm')
      this.habits = [
        { id: 1, name: '早起', category: '作息', time: '07:00', remind: true, color: '#3b82f6', starred: true, archived: false, sortOrder: 1 },
        { id: 2, name: '阅读30分钟', category: '学习', time: '20:00', remind: true, color: '#10b981', starred: false, archived: false, sortOrder: 1 },
        { id: 3, name: '运动锻炼', category: '健康', time: '18:00', remind: false, color: '#f59e0b', starred: false, archived: false, sortOrder: 2 },
        { id: 4, name: '喝8杯水', category: '健康', time: '', remind: false, color: '#06b6d4', starred: true, archived: false, sortOrder: 2 }
      ]
      this.archivedHabits = []
      this.starredOrder = [1, 4]
      this.nonStarredOrder = [2, 3]
      this.templates.forEach(t => {
        t.createTime = t.createTime || nowStr
      })
      this.currentSchedule = this.templates[0]
      this.templateHistory = [{
        templateId: this.templates[0].id,
        templateName: this.templates[0].name,
        startDate: dayjs().format('YYYY-MM-DD HH:mm'),
        endDate: null
      }]
      this.habitsLoaded = true
      this.saveToCache()
    },

    migrateScheduleData(schedule) {
      if (!schedule) return schedule
      const nowStr = dayjs().format('YYYY-MM-DD HH:mm')
      return {
        ...schedule,
        version: schedule.version || 1,
        parentId: schedule.parentId || null,
        createTime: schedule.createTime || nowStr,
        tag: schedule.tag || '其他',
        versionNote: schedule.versionNote || ''
      }
    },

    async loadHabits(force = false) {
      if (this.habitsLoaded && !force) {
        return this.habits
      }
      
      const requestVersion = ++this._loadHabitsVersion
      
      try {
        const res = await habitApi.getList()
        if (res.code === 0 && res.data) {
          if (requestVersion !== this._loadHabitsVersion) {
            return this.habits
          }
          this.habits = res.data
          const starred = this.habits.filter(h => h.starred).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
          const nonStarred = this.habits.filter(h => !h.starred).sort((a, b) => (a.sortOrder || 0) - (b.sortOrder || 0))
          this.starredOrder = starred.map(h => h.id)
          this.nonStarredOrder = nonStarred.map(h => h.id)
          this.habitsLoaded = true
          this.saveToCache()
          return this.habits
        }
      } catch (e) {
        console.error('加载习惯列表失败', e)
      }
      return this.habits
    },

    getRandomColor() {
      const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899']
      return colors[Math.floor(Math.random() * colors.length)]
    },

    normalizeHabitName(name) {
      if (!name) return ''
      return name.trim().replace(/\s+/g, ' ')
    },

    isDuplicateHabitName(normalizedName, excludeId = null) {
      return this.habits.some(h => {
        if (excludeId !== null && h.id === excludeId) return false
        return this.normalizeHabitName(h.name) === normalizedName
      })
    },

    async addHabit(habit) {
      const normalizedName = this.normalizeHabitName(habit.name)
      if (!normalizedName) {
        throw new Error('习惯名称不能为空')
      }
      if (this.isDuplicateHabitName(normalizedName)) {
        throw new Error('已存在同名习惯')
      }

      const tempId = Date.now()
      const normalizedHabit = { ...habit, name: normalizedName }
      if (Array.isArray(normalizedHabit.time)) {
        normalizedHabit.time = normalizedHabit.time.join(':')
      }
      const optimisticHabit = {
        id: tempId,
        ...normalizedHabit,
        color: normalizedHabit.color || this.getRandomColor(),
        starred: normalizedHabit.starred || false
      }
      this.habits.push(optimisticHabit)
      if (optimisticHabit.starred) {
        this.starredOrder.push(tempId)
      } else {
        this.nonStarredOrder.push(tempId)
      }
      this.saveToCache()
      
      this._loadHabitsVersion++
      
      try {
        const res = await habitApi.create(normalizedHabit)
        if (res.code === 0 && res.data) {
          const index = this.habits.findIndex(h => h.id === tempId)
          if (index > -1) {
            this.habits[index] = res.data
          }
          this.starredOrder = this.starredOrder.map(id => id === tempId ? res.data.id : id)
          this.nonStarredOrder = this.nonStarredOrder.map(id => id === tempId ? res.data.id : id)
          if (res.data.starred && !this.starredOrder.includes(res.data.id)) {
            this.starredOrder.push(res.data.id)
          } else if (!res.data.starred) {
            this.starredOrder = this.starredOrder.filter(id => id !== res.data.id)
          }
          if (!res.data.starred && !this.nonStarredOrder.includes(res.data.id)) {
            this.nonStarredOrder.push(res.data.id)
          } else if (res.data.starred) {
            this.nonStarredOrder = this.nonStarredOrder.filter(id => id !== res.data.id)
          }
          this.habitsLoaded = true
          this.saveToCache()
          return res.data
        }
      } catch (e) {
        console.error('创建习惯失败', e)
        this.habits = this.habits.filter(h => h.id !== tempId)
        this.starredOrder = this.starredOrder.filter(id => id !== tempId)
        this.nonStarredOrder = this.nonStarredOrder.filter(id => id !== tempId)
        this.saveToCache()
      }
      return optimisticHabit
    },

    async updateHabit(id, habit) {
      const normalizedName = this.normalizeHabitName(habit.name)
      if (habit.name !== undefined) {
        if (!normalizedName) {
          throw new Error('习惯名称不能为空')
        }
        if (this.isDuplicateHabitName(normalizedName, id)) {
          throw new Error('已存在同名习惯')
        }
      }

      const index = this.habits.findIndex(h => h.id === id)
      const oldHabit = index > -1 ? { ...this.habits[index] } : null

      const normalizedHabit = { ...habit }
      if (habit.name !== undefined) {
        normalizedHabit.name = normalizedName
      }
      if (Array.isArray(normalizedHabit.time)) {
        normalizedHabit.time = normalizedHabit.time.join(':')
      }

      if (index > -1) {
        const oldStarred = this.habits[index].starred
        this.habits[index] = { ...this.habits[index], ...normalizedHabit }
        if (normalizedHabit.starred !== undefined && normalizedHabit.starred !== oldStarred) {
          if (normalizedHabit.starred) {
            if (!this.starredOrder.includes(id)) {
              this.starredOrder.push(id)
            }
            this.nonStarredOrder = this.nonStarredOrder.filter(sid => sid !== id)
          } else {
            this.starredOrder = this.starredOrder.filter(sid => sid !== id)
            if (!this.nonStarredOrder.includes(id)) {
              this.nonStarredOrder.push(id)
            }
          }
        }
        this.saveToCache()
      }
      
      this._loadHabitsVersion++
      
      try {
        const res = await habitApi.update(id, normalizedHabit)
        if (res.code === 0 && res.data) {
          const idx = this.habits.findIndex(h => h.id === id)
          if (idx > -1) {
            this.habits[idx] = res.data
            if (res.data.starred) {
              if (!this.starredOrder.includes(id)) {
                this.starredOrder.push(id)
              }
              this.nonStarredOrder = this.nonStarredOrder.filter(sid => sid !== id)
            } else {
              this.starredOrder = this.starredOrder.filter(sid => sid !== id)
              if (!this.nonStarredOrder.includes(id)) {
                this.nonStarredOrder.push(id)
              }
            }
          }
          this.saveToCache()
          return res.data
        }
      } catch (e) {
        console.error('更新习惯失败', e)
        if (oldHabit && index > -1) {
          this.habits[index] = oldHabit
          const oldStarred = oldHabit.starred
          if (oldStarred) {
            if (!this.starredOrder.includes(id)) {
              this.starredOrder.push(id)
            }
            this.nonStarredOrder = this.nonStarredOrder.filter(sid => sid !== id)
          } else {
            this.starredOrder = this.starredOrder.filter(sid => sid !== id)
            if (!this.nonStarredOrder.includes(id)) {
              this.nonStarredOrder.push(id)
            }
          }
          this.saveToCache()
        }
      }
      return null
    },

    async deleteHabit(id) {
      const index = this.habits.findIndex(h => h.id === id)
      const oldHabit = index > -1 ? { ...this.habits[index] } : null
      const oldStarredOrder = [...this.starredOrder]
      const oldNonStarredOrder = [...this.nonStarredOrder]
      const oldCheckins = JSON.parse(JSON.stringify(this.checkins))
      
      this.habits = this.habits.filter(h => h.id !== id)
      this.starredOrder = this.starredOrder.filter(sid => sid !== id)
      this.nonStarredOrder = this.nonStarredOrder.filter(sid => sid !== id)
      
      const newCheckins = {}
      for (const date in this.checkins) {
        const dateCheckins = { ...this.checkins[date] }
        delete dateCheckins[id]
        newCheckins[date] = dateCheckins
      }
      this.checkins = newCheckins
      
      this.saveToCache()
      
      this._loadHabitsVersion++
      
      try {
        const res = await habitApi.delete(id)
        if (res.code === 0) {
          return true
        }
      } catch (e) {
        console.error('删除习惯失败', e)
        if (oldHabit) {
          this.habits.splice(index, 0, oldHabit)
          this.starredOrder = oldStarredOrder
          this.nonStarredOrder = oldNonStarredOrder
          this.checkins = oldCheckins
          this.saveToCache()
        }
      }
      return false
    },

    async toggleStarred(id) {
      const habit = this.habits.find(h => h.id === id)
      const oldStarred = habit ? habit.starred : null
      
      if (habit) {
        habit.starred = !habit.starred
        if (habit.starred) {
          if (!this.starredOrder.includes(id)) {
            this.starredOrder.push(id)
          }
          this.nonStarredOrder = this.nonStarredOrder.filter(sid => sid !== id)
        } else {
          this.starredOrder = this.starredOrder.filter(sid => sid !== id)
          if (!this.nonStarredOrder.includes(id)) {
            this.nonStarredOrder.push(id)
          }
        }
        this.saveToCache()
      }
      
      this._loadHabitsVersion++
      
      try {
        const res = await habitApi.toggleStar(id)
        if (res.code === 0 && res.data) {
          const idx = this.habits.findIndex(h => h.id === id)
          if (idx > -1) {
            this.habits[idx] = res.data
            if (res.data.starred) {
              if (!this.starredOrder.includes(id)) {
                this.starredOrder.push(id)
              }
              this.nonStarredOrder = this.nonStarredOrder.filter(sid => sid !== id)
            } else {
              this.starredOrder = this.starredOrder.filter(sid => sid !== id)
              if (!this.nonStarredOrder.includes(id)) {
                this.nonStarredOrder.push(id)
              }
            }
          }
          this.saveToCache()
          return res.data
        }
      } catch (e) {
        console.error('切换星标失败', e)
        if (habit && oldStarred !== null) {
          habit.starred = oldStarred
          if (oldStarred) {
            if (!this.starredOrder.includes(id)) {
              this.starredOrder.push(id)
            }
            this.nonStarredOrder = this.nonStarredOrder.filter(sid => sid !== id)
          } else {
            this.starredOrder = this.starredOrder.filter(sid => sid !== id)
            if (!this.nonStarredOrder.includes(id)) {
              this.nonStarredOrder.push(id)
            }
          }
          this.saveToCache()
        }
      }
      return null
    },

    async updateStarredOrder(newOrder) {
      this.starredOrder = newOrder
      this.saveToCache()
      
      try {
        const res = await habitApi.updateStarredOrder(newOrder)
        if (res.code === 0) {
          return true
        }
      } catch (e) {
        console.error('更新星标顺序失败', e)
      }
      return false
    },

    async updateHabitsOrder(starredIds, nonStarredIds) {
      if (starredIds) {
        this.starredOrder = starredIds
      }
      if (nonStarredIds) {
        this.nonStarredOrder = nonStarredIds
      }
      this.saveToCache()
      
      try {
        const res = await habitApi.updateHabitsOrder(
          starredIds || this.starredOrder, 
          nonStarredIds || this.nonStarredOrder
        )
        if (res.code === 0) {
          return true
        }
      } catch (e) {
        console.error('更新习惯顺序失败', e)
      }
      return false
    },

    moveStarredHabit(fromIndex, toIndex) {
      const result = [...this.starredOrder]
      const [removed] = result.splice(fromIndex, 1)
      result.splice(toIndex, 0, removed)
      this.starredOrder = result
      this.saveToCache()
    },

    moveNonStarredHabit(fromIndex, toIndex) {
      const result = [...this.nonStarredOrder]
      const [removed] = result.splice(fromIndex, 1)
      result.splice(toIndex, 0, removed)
      this.nonStarredOrder = result
      this.saveToCache()
    },

    async toggleCheckin(habitId, date = null) {
      const targetDate = date || dayjs().format('YYYY-MM-DD')
      
      if (this._togglingHabits.has(`${habitId}_${targetDate}`)) {
        const currentVal = !!(this.checkins[targetDate]?.[habitId])
        return { completed: currentVal, milestoneInfo: null, habitMilestoneInfo: null }
      }
      this._togglingHabits.add(`${habitId}_${targetDate}`)
      
      const dateCheckins = { ...(this.checkins[targetDate] || {}) }
      const oldValue = !!dateCheckins[habitId]
      const newValue = !oldValue
      
      dateCheckins[habitId] = newValue
      this.checkins = {
        ...this.checkins,
        [targetDate]: dateCheckins
      }
      this.saveToCache()
      
      let milestoneInfo = null
      let habitMilestoneInfo = null
      let finalCompleted = newValue
      let serverSuccess = false
      try {
        const res = await checkinApi.toggle(habitId, targetDate)
        if (res && res.code === 0 && res.data) {
          serverSuccess = true
          milestoneInfo = res.data.milestoneInfo
          habitMilestoneInfo = res.data.habitMilestoneInfo
          if (res.data.checkin) {
            finalCompleted = !!res.data.checkin.completed
            const recheckins = { ...(this.checkins[targetDate] || {}) }
            recheckins[habitId] = finalCompleted
            this.checkins = {
              ...this.checkins,
              [targetDate]: recheckins
            }
            this.saveToCache()
          }
        }
      } catch (e) {
        console.error('打卡同步失败', e)
      } finally {
        if (!serverSuccess) {
          const rollbackCheckins = { ...(this.checkins[targetDate] || {}) }
          rollbackCheckins[habitId] = oldValue
          this.checkins = {
            ...this.checkins,
            [targetDate]: rollbackCheckins
          }
          this.saveToCache()
          finalCompleted = oldValue
        } else {
          this.clearStreakCache(habitId)
        }
        this._togglingHabits.delete(`${habitId}_${targetDate}`)
      }

      return { completed: finalCompleted, milestoneInfo, habitMilestoneInfo }
    },

    setCurrentSchedule(schedule) {
      const now = dayjs().format('YYYY-MM-DD HH:mm')
      if (this.currentSchedule) {
        const lastRecord = this.templateHistory[this.templateHistory.length - 1]
        if (lastRecord && !lastRecord.endDate) {
          lastRecord.endDate = now
        }
      }
      this.templateHistory.push({
        templateId: schedule.id,
        templateName: schedule.name,
        startDate: now,
        endDate: null
      })
      this.currentSchedule = schedule
      this.saveToCache()
    },

    analyzeTemplateConflicts(newTemplate) {
      const habits = this.habits.filter(h => h.time)
      const newItems = (newTemplate.weekdayItems || newTemplate.items || [])
      
      const conflicts = {
        overloadedSlots: [],
        unplaceableHabits: [],
        totalConflicts: 0
      }
      
      const slotMap = {}
      newItems.forEach(item => {
        const hour = parseInt(item.time.split(':')[0], 10)
        const slotKey = `${hour}:00`
        if (!slotMap[slotKey]) {
          slotMap[slotKey] = { time: item.time, habits: [], scheduleItems: [] }
        }
        slotMap[slotKey].scheduleItems.push(item)
      })
      
      habits.forEach(habit => {
        const habitHour = parseInt(habit.time.split(':')[0], 10)
        const slotKey = `${habitHour}:00`
        
        if (!slotMap[slotKey]) {
          let nearestSlot = null
          let minDiff = 999
          Object.keys(slotMap).forEach(key => {
            const slotHour = parseInt(key.split(':')[0], 10)
            const diff = Math.abs(slotHour - habitHour)
            if (diff < minDiff) {
              minDiff = diff
              nearestSlot = key
            }
          })
          
          conflicts.unplaceableHabits.push({
            habit,
            nearestSlot: nearestSlot ? slotMap[nearestSlot].time : null,
            nearestDiff: minDiff
          })
        } else {
          slotMap[slotKey].habits.push(habit)
        }
      })
      
      Object.values(slotMap).forEach(slot => {
        if (slot.habits.length >= 3) {
          conflicts.overloadedSlots.push({
            time: slot.time,
            habitCount: slot.habits.length,
            scheduleItemCount: slot.scheduleItems.length,
            habits: slot.habits,
            scheduleItems: slot.scheduleItems
          })
        }
      })
      
      conflicts.totalConflicts = conflicts.overloadedSlots.length + conflicts.unplaceableHabits.length
      
      return conflicts
    },

    addCustomSchedule(schedule) {
      const nowStr = dayjs().format('YYYY-MM-DD HH:mm')
      const newSchedule = {
        id: Date.now(),
        version: 1,
        parentId: null,
        createTime: nowStr,
        tag: schedule.tag || '其他',
        versionNote: schedule.versionNote || `创建于${nowStr}`,
        ...schedule,
        isCustom: true
      }
      if (newSchedule.items && !newSchedule.weekdayItems) {
        newSchedule.weekdayItems = [...newSchedule.items]
        newSchedule.weekendItems = [...newSchedule.items]
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

    async copySchedule(template, options = {}) {
      const allTemplates = [...this.templates, ...this.schedules]
      const source = allTemplates.find(t => t.id === template.id) || template
      const now = Date.now()
      const nowStr = dayjs().format('YYYY-MM-DD HH:mm')
      
      const baseName = options.name || source.name
      const version = (source.version || 1) + 1
      
      const newSchedule = {
        id: now,
        name: options.name ? options.name : `${baseName} v${version}`,
        version,
        parentId: source.id,
        createTime: nowStr,
        tag: options.tag || source.tag || '其他',
        isCustom: true,
        versionNote: options.versionNote || `基于「${source.name}」复制于${nowStr}`,
        weekdayItems: JSON.parse(JSON.stringify(source.weekdayItems || source.items || [])),
        weekendItems: JSON.parse(JSON.stringify(source.weekendItems || source.items || []))
      }
      
      this.schedules.push(newSchedule)
      this.saveToCache()
      
      try {
        const res = await scheduleApi.copy(source.id, {
          name: newSchedule.name,
          tag: newSchedule.tag,
          versionNote: newSchedule.versionNote,
          weekdayItems: JSON.stringify(newSchedule.weekdayItems),
          weekendItems: JSON.stringify(newSchedule.weekendItems)
        })
        if (res.code === 0 && res.data) {
          const idx = this.schedules.findIndex(s => s.id === now)
          if (idx > -1) {
            this.schedules[idx].id = res.data.id
            if (this.currentSchedule?.id === now) {
              this.currentSchedule.id = res.data.id
            }
            this.saveToCache()
          }
        }
      } catch (e) {
        console.error('同步复制模板到后端失败', e)
      }
      
      return newSchedule
    },

    renameSchedule(id, newName) {
      const scheduleIndex = this.schedules.findIndex(s => s.id === id)
      if (scheduleIndex > -1) {
        this.schedules[scheduleIndex].name = newName
        if (this.currentSchedule?.id === id) {
          this.currentSchedule.name = newName
        }
        this.saveToCache()
        return this.schedules[scheduleIndex]
      }
      
      const templateIndex = this.templates.findIndex(t => t.id === id)
      if (templateIndex > -1) {
        this.templates[templateIndex].name = newName
        if (this.currentSchedule?.id === id) {
          this.currentSchedule.name = newName
        }
        this.saveToCache()
        return this.templates[templateIndex]
      }
      
      return null
    },

    updateScheduleTag(id, tag) {
      const scheduleIndex = this.schedules.findIndex(s => s.id === id)
      if (scheduleIndex > -1) {
        this.schedules[scheduleIndex].tag = tag
        if (this.currentSchedule?.id === id) {
          this.currentSchedule.tag = tag
        }
        this.saveToCache()
        return true
      }
      
      const templateIndex = this.templates.findIndex(t => t.id === id)
      if (templateIndex > -1) {
        this.templates[templateIndex].tag = tag
        if (this.currentSchedule?.id === id) {
          this.currentSchedule.tag = tag
        }
        this.saveToCache()
        return true
      }
      
      return false
    },

    updateScheduleVersionNote(id, note) {
      const scheduleIndex = this.schedules.findIndex(s => s.id === id)
      if (scheduleIndex > -1) {
        this.schedules[scheduleIndex].versionNote = note
        this.saveToCache()
        return true
      }
      return false
    },

    getRandomColor() {
      const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899']
      return colors[Math.floor(Math.random() * colors.length)]
    },

    async archiveHabit(id) {
      const index = this.habits.findIndex(h => h.id === id)
      const oldHabit = index > -1 ? { ...this.habits[index] } : null
      const oldStarredOrder = [...this.starredOrder]
      const oldNonStarredOrder = [...this.nonStarredOrder]
      
      if (index > -1) {
        this.habits.splice(index, 1)
        this.starredOrder = this.starredOrder.filter(sid => sid !== id)
        this.nonStarredOrder = this.nonStarredOrder.filter(sid => sid !== id)
        
        this.saveToCache()
      }
      
      this._loadHabitsVersion++
      
      try {
        const res = await habitApi.archive(id)
        if (res.code === 0 && res.data) {
          const archivedHabit = res.data
          const existsIndex = this.archivedHabits.findIndex(h => h.id === id)
          if (existsIndex > -1) {
            this.archivedHabits[existsIndex] = archivedHabit
          } else {
            this.archivedHabits.unshift(archivedHabit)
          }
          this.saveToCache()
          return archivedHabit
        }
      } catch (e) {
        console.error('归档失败', e)
        if (oldHabit && index > -1) {
          this.habits.splice(index, 0, oldHabit)
          this.starredOrder = oldStarredOrder
          this.nonStarredOrder = oldNonStarredOrder
          this.saveToCache()
        }
      }
      return null
    },

    async unarchiveHabit(id) {
      const archiveIndex = this.archivedHabits.findIndex(h => h.id === id)
      const oldArchivedHabit = archiveIndex > -1 ? { ...this.archivedHabits[archiveIndex] } : null
      
      if (archiveIndex > -1) {
        this.archivedHabits.splice(archiveIndex, 1)
        this.saveToCache()
      }
      
      this._loadHabitsVersion++
      
      try {
        const res = await habitApi.unarchive(id)
        if (res.code === 0 && res.data) {
          const habit = res.data
          const existsIndex = this.habits.findIndex(h => h.id === id)
          if (existsIndex > -1) {
            this.habits[existsIndex] = habit
          } else {
            this.habits.push(habit)
          }
          if (habit.starred) {
            if (!this.starredOrder.includes(id)) {
              this.starredOrder.push(id)
            }
            this.nonStarredOrder = this.nonStarredOrder.filter(sid => sid !== id)
          } else {
            this.starredOrder = this.starredOrder.filter(sid => sid !== id)
            if (!this.nonStarredOrder.includes(id)) {
              this.nonStarredOrder.push(id)
            }
          }
          
          await this.loadHabitCheckins(id)
          
          this.saveToCache()
          return habit
        }
      } catch (e) {
        console.error('取消归档失败', e)
        if (oldArchivedHabit && archiveIndex > -1) {
          this.archivedHabits.splice(archiveIndex, 0, oldArchivedHabit)
          this.saveToCache()
        }
      }
      return null
    },

    async loadHabitCheckins(habitId) {
      try {
        const res = await checkinApi.getByHabit(habitId)
        if (res.code === 0 && res.data) {
          res.data.forEach(item => {
            const date = item.date
            const completed = item.completed
            if (!this.checkins[date]) {
              this.checkins[date] = {}
            }
            if (completed) {
              this.checkins[date][habitId] = true
            }
          })
          this.saveToCache()
        }
      } catch (e) {
        console.error('加载习惯打卡记录失败', e)
      }
    },

    async loadArchivedHabits() {
      try {
        const res = await habitApi.getArchivedList()
        if (res.code === 0 && res.data) {
          this.archivedHabits = res.data
          for (const habit of res.data) {
            await this.loadHabitCheckins(habit.id)
          }
          this.saveToCache()
          return this.archivedHabits
        }
      } catch (e) {
        console.error('加载归档列表失败', e)
      }
      return this.archivedHabits
    },

    getHabitCheckinStats(habitId) {
      if (this.streakCache[habitId]) {
        return this.streakCache[habitId]
      }
      return {
        totalDays: 0,
        completedDays: 0,
        currentStreak: 0,
        maxStreak: 0,
        completionRate: 0
      }
    },

    async fetchHabitStats(habitId, force = false) {
      if (!force && this.streakCache[habitId]) {
        return this.streakCache[habitId]
      }
      try {
        const res = await streakApi.getByHabit(habitId)
        if (res.code === 0 && res.data) {
          const s = res.data
          const totalCompleted = s.totalCompletedDays || 0
          let totalDays = 0
          if (s.firstCheckinDate) {
            totalDays = dayjs().diff(dayjs(s.firstCheckinDate), 'day') + 1
          }
          const stats = {
            totalDays,
            completedDays: totalCompleted,
            currentStreak: s.currentStreak || 0,
            maxStreak: s.maxStreak || 0,
            completionRate: totalDays > 0 ? Math.round((totalCompleted / totalDays) * 100) : 0,
            firstCheckinDate: s.firstCheckinDate,
            lastCheckinDate: s.lastCheckinDate,
            currentStreakStartDate: s.currentStreakStartDate,
            currentStreakEndDate: s.currentStreakEndDate,
            maxStreakStartDate: s.maxStreakStartDate,
            maxStreakEndDate: s.maxStreakEndDate,
            segments: s.segments || [],
            breakPoints: s.breakPoints || [],
            totalSegments: s.totalSegments || 0,
            totalBreakPoints: s.totalBreakPoints || 0
          }
          this.streakCache[habitId] = stats
          return stats
        }
      } catch (e) {
        console.error('加载习惯统计失败', e)
      }
      return {
        totalDays: 0,
        completedDays: 0,
        currentStreak: 0,
        maxStreak: 0,
        completionRate: 0
      }
    },

    getArchivedHabitStats(habitId, archiveTime) {
      if (this.archivedStreakCache[habitId]) {
        return this.archivedStreakCache[habitId]
      }
      return {
        totalDays: 0,
        completedDays: 0,
        maxStreak: 0,
        completionRate: 0,
        firstDate: null,
        lastDate: null,
        activeDays: 0
      }
    },

    async fetchArchivedHabitStats(habitId, archiveTime, force = false) {
      const cacheKey = habitId
      if (!force && this.archivedStreakCache[cacheKey]) {
        return this.archivedStreakCache[cacheKey]
      }
      try {
        const archiveDateStr = dayjs(archiveTime).format('YYYY-MM-DD')
        const res = await streakApi.getArchived(habitId, archiveDateStr)
        if (res.code === 0 && res.data) {
          const s = res.data
          const totalCompleted = s.totalCompletedDays || 0
          let totalDays = 0
          let activeDays = 0
          let firstDate = null
          let lastDate = null
          if (s.firstCheckinDate) {
            firstDate = s.firstCheckinDate
            totalDays = dayjs(archiveTime).diff(dayjs(s.firstCheckinDate), 'day') + 1
          }
          if (s.lastCheckinDate) {
            lastDate = s.lastCheckinDate
            activeDays = dayjs(s.lastCheckinDate).diff(dayjs(s.firstCheckinDate), 'day') + 1
          }
          const stats = {
            totalDays,
            completedDays: totalCompleted,
            maxStreak: s.maxStreak || 0,
            completionRate: activeDays > 0 ? Math.round((totalCompleted / activeDays) * 100) : 0,
            firstDate,
            lastDate,
            activeDays,
            segments: s.segments || [],
            breakPoints: s.breakPoints || []
          }
          this.archivedStreakCache[cacheKey] = stats
          return stats
        }
      } catch (e) {
        console.error('加载归档习惯统计失败', e)
      }
      return {
        totalDays: 0,
        completedDays: 0,
        maxStreak: 0,
        completionRate: 0,
        firstDate: null,
        lastDate: null,
        activeDays: 0
      }
    },

    clearStreakCache(habitId) {
      if (habitId) {
        delete this.streakCache[habitId]
        delete this.archivedStreakCache[habitId]
      } else {
        this.streakCache = {}
        this.archivedStreakCache = {}
      }
    },

    getHabitCheckinDetail(habitId, days = 30) {
      const detail = []
      const today = dayjs()
      
      for (let i = days - 1; i >= 0; i--) {
        const date = today.subtract(i, 'day')
        const dateStr = date.format('YYYY-MM-DD')
        const completed = this.checkins[dateStr]?.[habitId] === true
        detail.push({
          date: dateStr,
          label: date.format('MM/DD'),
          weekday: date.format('dd'),
          completed
        })
      }
      
      return detail
    },

    showHabitMilestone(info) {
      this.currentHabitMilestone = info
      this.showHabitMilestoneModal = true
    },

    closeHabitMilestoneModal() {
      this.showHabitMilestoneModal = false
      this.currentHabitMilestone = null
    },

    async loadHabitDetail(habitId) {
      try {
        const res = await milestoneApi.getByHabit(habitId)
        if (res.code === 0 && res.data) {
          this.habitDetails[habitId] = res.data
          return res.data
        }
      } catch (e) {
        console.error('加载习惯详情失败', e)
      }
      return this.habitDetails[habitId] || null
    },

    getHabitDetail(habitId) {
      return this.habitDetails[habitId] || null
    }
  }
})
