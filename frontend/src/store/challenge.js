import { defineStore } from 'pinia'
import dayjs from 'dayjs'
import { challengeApi } from '@/utils/request'

const CACHE_KEY = 'habit_challenge_cache'

export const useChallengeStore = defineStore('challenge', {
  state: () => ({
    activeChallenges: [],
    challengeHistory: [],
    currentChallenge: null,
    showMilestoneModal: false,
    milestoneType: null
  }),

  getters: {
    activeChallengeCount: (state) => state.activeChallenges.length,
    
    getChallengeByHabitId: (state) => (habitId) => {
      return state.activeChallenges.find(c => c.habitId === habitId)
    },
    
    getRemainingDays: () => (challenge) => {
      if (!challenge) return 0
      const today = dayjs()
      const endDate = dayjs(challenge.endDate)
      if (today.isAfter(endDate)) return 0
      return endDate.diff(today, 'day') + 1
    },
    
    getCurrentDay: () => (challenge) => {
      if (!challenge) return 0
      const today = dayjs()
      const startDate = dayjs(challenge.startDate)
      if (today.isBefore(startDate)) return 0
      return today.diff(startDate, 'day') + 1
    },
    
    getProgress: () => (challenge) => {
      if (!challenge || !challenge.totalDays) return 0
      return Math.round((challenge.completedDays / challenge.totalDays) * 100)
    },
    
    isInterrupted: () => (challenge) => {
      if (!challenge || challenge.status !== 'active') return false
      return challenge.currentStreak === 0 && challenge.completedDays > 0
    },
    
    milestones: () => (challenge) => {
      if (!challenge) return []
      return [
        { day: 7, achieved: challenge.milestone7, label: '一周达成', icon: '🌟' },
        { day: 14, achieved: challenge.milestone14, label: '两周坚持', icon: '🏆' },
        { day: 21, achieved: challenge.milestone21, label: '习惯养成', icon: '👑' }
      ]
    }
  },

  actions: {
    loadFromCache() {
      try {
        const cached = localStorage.getItem(CACHE_KEY)
        if (cached) {
          const data = JSON.parse(cached)
          this.activeChallenges = data.activeChallenges || []
          this.challengeHistory = data.challengeHistory || []
        }
      } catch (e) {
        console.error('加载挑战缓存失败', e)
      }
    },
    
    saveToCache() {
      try {
        localStorage.setItem(CACHE_KEY, JSON.stringify({
          activeChallenges: this.activeChallenges,
          challengeHistory: this.challengeHistory
        }))
      } catch (e) {
        console.error('保存挑战缓存失败', e)
      }
    },

    async loadActiveChallenges() {
      try {
        const res = await challengeApi.getActiveList()
        if (res.code === 0 && res.data) {
          this.activeChallenges = res.data
          this.saveToCache()
          return this.activeChallenges
        }
      } catch (e) {
        console.error('加载进行中挑战失败', e)
      }
      return this.activeChallenges
    },

    async loadChallengeHistory() {
      try {
        const res = await challengeApi.getHistory()
        if (res.code === 0 && res.data) {
          this.challengeHistory = res.data
          this.saveToCache()
          return this.challengeHistory
        }
      } catch (e) {
        console.error('加载挑战历史失败', e)
      }
      return this.challengeHistory
    },

    async getChallengeByHabit(habitId) {
      try {
        const res = await challengeApi.getByHabit(habitId)
        if (res.code === 0) {
          return res.data
        }
      } catch (e) {
        console.error('获取习惯挑战失败', e)
      }
      return null
    },

    async startChallenge(habitId, totalDays = 21) {
      try {
        const res = await challengeApi.start(habitId, totalDays)
        if (res.code === 0 && res.data) {
          const newChallenge = res.data
          const existingIndex = this.activeChallenges.findIndex(c => c.habitId === habitId)
          if (existingIndex > -1) {
            this.activeChallenges[existingIndex] = newChallenge
          } else {
            this.activeChallenges.push(newChallenge)
          }
          this.saveToCache()
          return newChallenge
        }
      } catch (e) {
        console.error('开始挑战失败', e)
      }
      return null
    },

    async getChallengeDetail(id) {
      try {
        const res = await challengeApi.getDetail(id)
        if (res.code === 0 && res.data) {
          this.currentChallenge = res.data
          return res.data
        }
      } catch (e) {
        console.error('获取挑战详情失败', e)
      }
      return null
    },

    async refreshChallenge(id) {
      try {
        const oldChallenge = this.activeChallenges.find(c => c.id === id)
        const wasMilestone7 = oldChallenge?.milestone7
        const wasMilestone14 = oldChallenge?.milestone14
        const wasMilestone21 = oldChallenge?.milestone21
        
        const res = await challengeApi.refresh(id)
        if (res.code === 0 && res.data) {
          const updated = res.data
          const index = this.activeChallenges.findIndex(c => c.id === id)
          if (index > -1) {
            this.activeChallenges[index] = updated
          }
          if (this.currentChallenge?.id === id) {
            this.currentChallenge = updated
          }
          this.saveToCache()
          
          const newMilestone7 = updated.milestone7 && !wasMilestone7
          const newMilestone14 = updated.milestone14 && !wasMilestone14
          const newMilestone21 = updated.milestone21 && !wasMilestone21
          const isCompleted = updated.status === 'completed' && oldChallenge?.status === 'active'
          
          if (newMilestone7 || newMilestone14 || newMilestone21 || isCompleted) {
            if (newMilestone7) this.milestoneType = 7
            else if (newMilestone14) this.milestoneType = 14
            else if (newMilestone21) this.milestoneType = 21
            else if (isCompleted) this.milestoneType = 'complete'
            
            this.showMilestoneModal = true
          }
          
          return updated
        }
      } catch (e) {
        console.error('刷新挑战失败', e)
      }
      return null
    },

    async giveUpChallenge(id) {
      try {
        const res = await challengeApi.giveUp(id)
        if (res.code === 0) {
          this.activeChallenges = this.activeChallenges.filter(c => c.id !== id)
          if (this.currentChallenge?.id === id) {
            this.currentChallenge = null
          }
          this.saveToCache()
          return true
        }
      } catch (e) {
        console.error('放弃挑战失败', e)
      }
      return false
    },

    updateChallengeFromMilestoneInfo(milestoneInfo) {
      if (!milestoneInfo) return
      
      const { challengeId, completedDays, currentStreak, newMilestone7, newMilestone14, newMilestone21, isCompleted } = milestoneInfo
      
      const challenge = this.activeChallenges.find(c => c.id === challengeId)
      if (challenge) {
        challenge.completedDays = completedDays
        challenge.currentStreak = currentStreak
        
        if (newMilestone7) challenge.milestone7 = true
        if (newMilestone14) challenge.milestone14 = true
        if (newMilestone21) challenge.milestone21 = true
        
        if (isCompleted) {
          challenge.status = 'completed'
          this.activeChallenges = this.activeChallenges.filter(c => c.id !== challengeId)
        }
        
        if (this.currentChallenge?.id === challengeId) {
          this.currentChallenge = { ...challenge }
        }
        
        this.saveToCache()
      }
      
      if (newMilestone7 || newMilestone14 || newMilestone21 || isCompleted) {
        if (newMilestone7) this.milestoneType = 7
        else if (newMilestone14) this.milestoneType = 14
        else if (newMilestone21) this.milestoneType = 21
        else if (isCompleted) this.milestoneType = 'complete'
        
        this.showMilestoneModal = true
      }
    },

    closeMilestoneModal() {
      this.showMilestoneModal = false
      this.milestoneType = null
    }
  }
})
