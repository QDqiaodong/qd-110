<template>
  <div class="page-container habit-detail-page">
    <div v-if="habitDetail" class="detail-container">
      <div class="detail-header" :style="{ background: `linear-gradient(135deg, ${habitDetail.habit.color}20, ${habitDetail.habit.color}40)` }">
        <div class="back-btn" @click="goBack">
          <van-icon name="arrow-left" />
        </div>
        <div class="habit-info">
          <div class="habit-icon" :style="{ background: habitDetail.habit.color + '30', color: habitDetail.habit.color }">
            {{ getCategoryIcon(habitDetail.habit.category) }}
          </div>
          <div class="habit-text">
            <div class="habit-name">{{ habitDetail.habit.name }}</div>
            <div class="habit-category">{{ habitDetail.habit.category }} · 习惯详情</div>
          </div>
        </div>
      </div>

      <div class="stats-section">
        <div class="stats-row">
          <div class="stat-item">
            <div class="stat-value">{{ habitDetail.stats.totalCompleted }}</div>
            <div class="stat-label">累计打卡</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value streak" :class="{ 'streak-active': habitDetail.stats.currentStreak > 0 }">
              {{ habitDetail.stats.currentStreak }}
            </div>
            <div class="stat-label">当前连续</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value">{{ habitDetail.stats.maxStreak }}</div>
            <div class="stat-label">最长连续</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value">{{ habitDetail.stats.completionRate }}%</div>
            <div class="stat-label">完成率</div>
          </div>
        </div>
      </div>

      <div class="milestones-section">
        <div class="section-header">
          <div class="section-title">🏆 里程碑轨迹</div>
          <div class="section-subtitle">
            已达成 {{ achievedMilestoneCount }}/{{ habitDetail.milestones.length }} 个
          </div>
        </div>
        <div class="milestones-timeline">
          <div 
            v-for="(milestone, index) in habitDetail.milestones" 
            :key="milestone.type"
            class="timeline-item"
            :class="{ achieved: milestone.achieved, 'next-up': !milestone.achieved && isNextMilestone(index) }"
          >
            <div class="timeline-line" :class="{ 'line-done': milestone.achieved }"></div>
            <div class="timeline-node">
              <div class="node-icon" :style="{ background: milestone.achieved ? (habitDetail.habit.color + '30') : '#f3f4f6' }">
                <span v-if="milestone.achieved" class="icon-emoji">{{ milestone.icon }}</span>
                <span v-else class="icon-number">{{ milestone.type }}</span>
              </div>
            </div>
            <div class="timeline-content">
              <div class="milestone-header">
                <div class="milestone-type">{{ milestone.type }} 次打卡</div>
                <div class="milestone-status">
                  <van-icon v-if="milestone.achieved" name="checked" class="check-icon" />
                  <span v-else-if="isNextMilestone(index)" class="next-text">进行中</span>
                  <span v-else class="pending-text">未达成</span>
                </div>
              </div>
              <div class="milestone-label">{{ milestone.label }}</div>
              <div v-if="milestone.achieved" class="milestone-date">
                📅 {{ formatDate(milestone.achieveDate) }} 达成
              </div>
              <div v-else class="milestone-progress">
                <van-progress 
                  :percentage="getMilestoneProgress(milestone.type)" 
                  :color="habitDetail.habit.color" 
                  stroke-width="4" 
                  :pivot-text="`${habitDetail.stats.totalCompleted}/${milestone.type}`"
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="calendar-section">
        <div class="section-header">
          <div class="section-title">📅 最近打卡</div>
          <div class="section-subtitle">近30天记录</div>
        </div>
        <div class="calendar-grid">
          <div 
            v-for="day in habitDetail.recentCheckins" 
            :key="day.date"
            class="calendar-day"
            :class="{ completed: day.completed, today: isToday(day.date) }"
            :title="`${day.date} ${day.weekday}`"
          >
            <div class="day-label">{{ day.label.split('/')[1] }}</div>
            <div v-if="day.completed" class="day-check">✓</div>
          </div>
        </div>
        <div class="calendar-legend">
          <span class="legend-item">
            <span class="legend-dot completed" :style="{ background: habitDetail.habit.color }"></span>
            已完成
          </span>
          <span class="legend-item">
            <span class="legend-dot today"></span>
            今天
          </span>
          <span class="legend-item">
            <span class="legend-dot pending"></span>
            未打卡
          </span>
        </div>
      </div>

      <div class="action-section">
        <van-button 
          block 
          type="primary" 
          size="large" 
          round
          :style="{ background: habitDetail.habit.color, borderColor: habitDetail.habit.color }"
          @click="goToCheckin"
        >
          去打卡
        </van-button>
        <van-button 
          v-if="!hasActiveChallenge"
          block 
          type="default" 
          size="large" 
          round
          style="margin-top: 12px"
          @click="startChallenge"
        >
          🔥 发起21天挑战
        </van-button>
      </div>
    </div>

    <div v-else class="loading-state">
      <van-loading type="spinner" color="#3b82f6" />
      <div class="loading-text">加载中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useHabitStore } from '@/store/habit'
import { useChallengeStore } from '@/store/challenge'
import { showConfirmDialog, showToast } from 'vant'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const store = useHabitStore()
const challengeStore = useChallengeStore()

const habitDetail = ref(null)

const habitId = computed(() => Number(route.params.id))

const achievedMilestoneCount = computed(() => {
  if (!habitDetail.value) return 0
  return habitDetail.value.milestones.filter(m => m.achieved).length
})

const hasActiveChallenge = computed(() => {
  return !!challengeStore.getChallengeByHabitId(habitId.value)
})

const isNextMilestone = (index) => {
  if (!habitDetail.value) return false
  const milestones = habitDetail.value.milestones
  for (let i = 0; i < milestones.length; i++) {
    if (!milestones[i].achieved) {
      return i === index
    }
  }
  return false
}

const getMilestoneProgress = (milestoneType) => {
  if (!habitDetail.value) return 0
  const completed = habitDetail.value.stats.totalCompleted || 0
  return Math.min(100, Math.round((completed / milestoneType) * 100))
}

const isToday = (dateStr) => {
  return dayjs(dateStr).isSame(dayjs(), 'day')
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return dayjs(dateStr).format('YYYY年MM月DD日')
}

const getCategoryIcon = (category) => {
  const icons = {
    '生活': '🏠', '学习': '📚', '作息': '⏰', '健康': '💪',
    '工作': '💼', '运动': '🏃', '阅读': '📖', '其他': '✨'
  }
  return icons[category] || '✨'
}

onMounted(async () => {
  store.loadFromCache()
  challengeStore.loadFromCache()
  await loadDetail()
})

const loadDetail = async () => {
  const data = await store.loadHabitDetail(habitId.value)
  if (data) {
    habitDetail.value = data
  }
}

const goBack = () => {
  router.back()
}

const goToCheckin = () => {
  router.push('/')
}

const startChallenge = async () => {
  try {
    await showConfirmDialog({
      title: '发起21天挑战',
      message: `确定要为「${habitDetail.value.habit.name}」发起21天养成挑战吗？坚持就是胜利！`,
      confirmButtonColor: habitDetail.value.habit.color || '#3b82f6'
    })
    
    const challenge = await challengeStore.startChallenge(habitId.value, 21)
    if (challenge) {
      showToast('挑战已开始，加油！')
      router.push(`/challenge/${challenge.id}`)
    } else {
      showToast('发起挑战失败')
    }
  } catch (e) {}
}
</script>

<style lang="scss" scoped>
.habit-detail-page {
  padding: 0;
}

.detail-container {
  min-height: 100vh;
}

.detail-header {
  padding: 50px 20px 30px;
  position: relative;
  border-radius: 0 0 24px 24px;
}

.back-btn {
  position: absolute;
  top: 50px;
  left: 16px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.8);
  @include flex-center;
  font-size: 20px;
  cursor: pointer;
  
  &:active {
    opacity: 0.8;
  }
}

.habit-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-left: 50px;
}

.habit-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  @include flex-center;
  font-size: 28px;
}

.habit-text {
  flex: 1;
}

.habit-name {
  font-size: 20px;
  font-weight: 600;
  margin-bottom: 4px;
}

.habit-category {
  font-size: 14px;
  color: $text-secondary;
}

.stats-section {
  padding: 24px 16px;
  margin-top: -40px;
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: #fff;
  border-radius: 16px;
  padding: 20px 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 4px;
  
  &.streak-active {
    color: #f59e0b;
  }
}

.stat-label {
  font-size: 12px;
  color: $text-secondary;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: $border-color;
}

.milestones-section {
  padding: 0 16px 20px;
}

.section-header {
  @include flex-between;
  margin-bottom: 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.section-subtitle {
  font-size: 12px;
  color: $text-secondary;
}

.milestones-timeline {
  background: #fff;
  border-radius: 16px;
  padding: 8px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.timeline-item {
  display: flex;
  align-items: flex-start;
  padding: 16px;
  position: relative;
  
  &:not(:last-child) {
    padding-bottom: 20px;
  }
  
  &.achieved {
    .milestone-type {
      color: $text-primary;
    }
    .milestone-label {
      color: $text-primary;
    }
  }
  
  &.next-up {
    .node-icon {
      border: 2px solid #f59e0b;
    }
    .milestone-type {
      color: #f59e0b;
    }
  }
}

.timeline-line {
  position: absolute;
  left: 39px;
  top: 60px;
  bottom: 0;
  width: 2px;
  background: #e5e7eb;
  
  &.line-done {
    background: #10b981;
  }
}

.timeline-item:last-child .timeline-line {
  display: none;
}

.timeline-node {
  position: relative;
  z-index: 1;
  margin-right: 16px;
}

.node-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  @include flex-center;
  background: #f3f4f6;
  transition: all 0.3s;
}

.icon-emoji {
  font-size: 24px;
}

.icon-number {
  font-size: 16px;
  font-weight: 700;
  color: #9ca3af;
}

.timeline-content {
  flex: 1;
  padding-top: 4px;
}

.milestone-header {
  @include flex-between;
  margin-bottom: 4px;
}

.milestone-type {
  font-size: 15px;
  font-weight: 600;
  color: #9ca3af;
}

.milestone-status {
  .check-icon {
    color: #10b981;
    font-size: 18px;
  }
  
  .next-text {
    font-size: 12px;
    color: #f59e0b;
    font-weight: 500;
  }
  
  .pending-text {
    font-size: 12px;
    color: #9ca3af;
  }
}

.milestone-label {
  font-size: 13px;
  color: $text-secondary;
  margin-bottom: 8px;
}

.milestone-date {
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
}

.milestone-progress {
  margin-top: 4px;
}

.calendar-section {
  padding: 0 16px 20px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 6px;
  background: #fff;
  border-radius: 16px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 12px;
}

.calendar-day {
  aspect-ratio: 1;
  border-radius: 8px;
  background: #f9fafb;
  @include flex-center;
  flex-direction: column;
  font-size: 12px;
  color: $text-secondary;
  position: relative;
  
  &.completed {
    background: #10b981;
    color: #fff;
  }
  
  &.today {
    border: 2px solid #3b82f6;
    font-weight: 600;
    
    &.completed {
      border-color: #059669;
    }
  }
}

.day-label {
  font-size: 12px;
  font-weight: 500;
}

.day-check {
  font-size: 10px;
  margin-top: 1px;
}

.calendar-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 12px;
  color: $text-secondary;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  
  &.completed {
    background: #10b981;
  }
  
  &.today {
    background: #3b82f6;
  }
  
  &.pending {
    background: #f3f4f6;
    border: 1px solid #d1d5db;
  }
}

.action-section {
  padding: 0 16px 40px;
}

.loading-state {
  @include flex-center;
  flex-direction: column;
  min-height: 60vh;
  gap: 12px;
}

.loading-text {
  font-size: 14px;
  color: $text-secondary;
}
</style>
