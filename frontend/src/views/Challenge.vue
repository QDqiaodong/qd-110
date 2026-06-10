<template>
  <div class="page-container challenge-page">
    <div v-if="challenge" class="challenge-container">
      <div class="challenge-header" :style="{ background: `linear-gradient(135deg, ${challenge.habitColor}20, ${challenge.habitColor}40)` }">
        <div class="back-btn" @click="goBack">
          <van-icon name="arrow-left" />
        </div>
        <div class="habit-info">
          <div class="habit-icon" :style="{ background: challenge.habitColor + '30', color: challenge.habitColor }">
            🔥
          </div>
          <div class="habit-text">
            <div class="habit-name">{{ challenge.habitName }}</div>
            <div class="challenge-title">21天养成挑战</div>
          </div>
        </div>
        
        <div class="days-remaining">
          <div class="days-number">{{ remainingDays }}</div>
          <div class="days-label">剩余天数</div>
        </div>
      </div>

      <div class="progress-section">
        <div class="progress-ring-wrapper">
          <van-circle 
            :rate="progress" 
            :stroke-width="8" 
            size="160" 
            :color="challenge.habitColor"
            layer-color="#f3f4f6"
          >
            <div class="progress-content">
              <div class="progress-day">第 {{ currentDay }} 天</div>
              <div class="progress-total">/ {{ challenge.totalDays }} 天</div>
            </div>
          </van-circle>
        </div>
        
        <div class="stats-row">
          <div class="stat-item">
            <div class="stat-value">{{ challenge.completedDays }}</div>
            <div class="stat-label">已完成</div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value streak" :class="{ 'streak-warning': isInterrupted && challenge.currentStreak === 0 }">
              {{ challenge.currentStreak }}
            </div>
            <div class="stat-label">连续天数
              <span v-if="isInterrupted" class="interrupt-tip">已中断</span>
            </div>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <div class="stat-value">{{ challenge.maxStreak }}</div>
            <div class="stat-label">最长连续</div>
          </div>
        </div>
      </div>

      <div v-if="isInterrupted" class="interrupt-warning">
        <div class="warning-icon">⚠️</div>
        <div class="warning-text">
          <div class="warning-title">挑战已中断</div>
          <div class="warning-desc">别灰心，重新开始也需要勇气，继续加油！</div>
        </div>
        <van-button size="small" type="primary" round @click="restartChallenge">重新挑战</van-button>
      </div>

      <div class="milestones-section">
        <div class="section-title">里程碑</div>
        <div class="milestones-list">
          <div 
            v-for="milestone in milestones" 
            :key="milestone.day"
            class="milestone-item"
            :class="{ achieved: milestone.achieved, current: currentDay >= milestone.day && !milestone.achieved }"
          >
            <div class="milestone-icon">{{ milestone.icon }}</div>
            <div class="milestone-info">
              <div class="milestone-day">第 {{ milestone.day }} 天</div>
              <div class="milestone-label">{{ milestone.label }}</div>
            </div>
            <div class="milestone-status">
              <van-icon v-if="milestone.achieved" name="checked" class="check-icon" />
              <span v-else class="pending-text">未达成</span>
            </div>
          </div>
        </div>
      </div>

      <div class="calendar-section">
        <div class="section-title">打卡记录</div>
        <div class="calendar-grid">
          <div 
            v-for="(day, index) in calendarDays" 
            :key="index"
            class="calendar-day"
            :class="{ 
              completed: day.completed, 
              future: day.isFuture,
              today: day.isToday,
              'not-started': day.isBeforeStart
            }"
          >
            <div class="day-number">{{ day.dayNum }}</div>
            <div v-if="day.completed" class="day-check">✓</div>
          </div>
        </div>
        <div class="calendar-legend">
          <span class="legend-item">
            <span class="legend-dot completed"></span>
            已完成
          </span>
          <span class="legend-item">
            <span class="legend-dot today"></span>
            今天
          </span>
          <span class="legend-item">
            <span class="legend-dot future"></span>
            未开始
          </span>
        </div>
      </div>

      <div class="action-section">
        <van-button 
          v-if="challenge.status === 'active'"
          block 
          type="primary" 
          size="large" 
          round
          :style="{ background: challenge.habitColor, borderColor: challenge.habitColor }"
          @click="toggleTodayCheckin"
        >
          {{ todayCompleted ? '今日已打卡 ✓' : '今日打卡' }}
        </van-button>
        <van-button 
          v-if="challenge.status === 'active'"
          block 
          type="default" 
          size="large" 
          round
          style="margin-top: 12px"
          @click="showGiveUpConfirm = true"
        >
          放弃挑战
        </van-button>
      </div>
    </div>

    <van-popup v-model:show="showMilestoneModal" round position="center" :style="{ width: '80%', maxWidth: '320px' }" class="milestone-popup">
      <div class="milestone-modal">
        <div class="milestone-celebration">
          <div class="celebration-icon">{{ milestoneIcon }}</div>
          <div class="celebration-title">🎉 恭喜达成！</div>
          <div class="celebration-subtitle">{{ milestoneText }}</div>
        </div>
        <div class="milestone-message">
          {{ milestoneMessage }}
        </div>
        <van-button 
          block 
          type="primary" 
          round
          @click="closeMilestoneModal"
        >
          继续加油
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showGiveUpConfirm" round position="bottom" :style="{ height: '30%' }">
      <div class="giveup-form">
        <div class="form-header">
          <h3>确认放弃挑战？</h3>
        </div>
        <div class="giveup-desc">
          放弃后挑战将无法恢复，但你可以随时重新开始新的挑战。
        </div>
        <div class="form-actions">
          <van-button block round type="default" @click="showGiveUpConfirm = false">
            再想想
          </van-button>
          <van-button block round type="danger" style="margin-top: 12px" @click="confirmGiveUp">
            确认放弃
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChallengeStore } from '@/store/challenge'
import { useHabitStore } from '@/store/habit'
import { showToast, showConfirmDialog } from 'vant'
import dayjs from 'dayjs'

const route = useRoute()
const router = useRouter()
const challengeStore = useChallengeStore()
const habitStore = useHabitStore()

const challenge = ref(null)
const showMilestoneModal = ref(false)
const showGiveUpConfirm = ref(false)
const todayCompleted = ref(false)

const challengeId = computed(() => Number(route.params.id))

const remainingDays = computed(() => {
  if (!challenge.value) return 0
  return challengeStore.getRemainingDays(challenge.value)
})

const currentDay = computed(() => {
  if (!challenge.value) return 0
  return challengeStore.getCurrentDay(challenge.value)
})

const progress = computed(() => {
  if (!challenge.value) return 0
  return challengeStore.getProgress(challenge.value)
})

const isInterrupted = computed(() => {
  if (!challenge.value) return false
  return challengeStore.isInterrupted(challenge.value)
})

const milestones = computed(() => {
  return challengeStore.milestones(challenge.value)
})

const calendarDays = computed(() => {
  if (!challenge.value) return []
  const days = []
  const startDate = dayjs(challenge.value.startDate)
  const totalDays = challenge.value.totalDays
  const today = dayjs()
  
  for (let i = 0; i < totalDays; i++) {
    const date = startDate.add(i, 'day')
    const dateStr = date.format('YYYY-MM-DD')
    const completed = habitStore.checkins[dateStr]?.[challenge.value.habitId] === true
    
    days.push({
      date: dateStr,
      dayNum: i + 1,
      completed,
      isToday: date.isSame(today, 'day'),
      isFuture: date.isAfter(today, 'day'),
      isBeforeStart: date.isBefore(startDate, 'day')
    })
  }
  return days
})

const milestoneIcon = computed(() => {
  const type = challengeStore.milestoneType
  if (type === 7) return '🌟'
  if (type === 14) return '🏆'
  if (type === 21) return '👑'
  if (type === 'complete') return '🎊'
  return '🎉'
})

const milestoneText = computed(() => {
  const type = challengeStore.milestoneType
  if (type === 7) return '一周达成'
  if (type === 14) return '两周坚持'
  if (type === 21) return '习惯养成'
  if (type === 'complete') return '挑战成功'
  return ''
})

const milestoneMessage = computed(() => {
  const type = challengeStore.milestoneType
  if (type === 7) return '太棒了！你已经坚持了一周，这是一个很好的开始！继续保持，你正在养成一个好习惯！'
  if (type === 14) return '两周了！你已经证明了自己的毅力。习惯正在慢慢形成，再加把劲！'
  if (type === 21) return '恭喜你！21天挑战成功！这个习惯已经成为你生活的一部分了！'
  if (type === 'complete') return '你成功完成了21天挑战！这个习惯已经深深植根于你的日常生活中。继续保持，让好习惯伴你成长！'
  return ''
})

onMounted(async () => {
  await loadChallenge()
  checkTodayStatus()
})

const loadChallenge = async () => {
  const data = await challengeStore.getChallengeDetail(challengeId.value)
  if (data) {
    challenge.value = data
  }
}

const checkTodayStatus = () => {
  const today = dayjs().format('YYYY-MM-DD')
  todayCompleted.value = habitStore.checkins[today]?.[challenge.value?.habitId] === true
}

const toggleTodayCheckin = async () => {
  if (!challenge.value) return
  
  const habitId = challenge.value.habitId
  const today = dayjs().format('YYYY-MM-DD')
  
  const result = await habitStore.toggleCheckin(habitId, today)
  todayCompleted.value = result.completed
  
  showToast(todayCompleted.value ? '打卡成功！' : '已取消打卡')
  
  if (todayCompleted.value && result.milestoneInfo) {
    challengeStore.updateChallengeFromMilestoneInfo(result.milestoneInfo)
    const updated = challengeStore.activeChallenges.find(c => c.id === challengeId.value)
    if (updated) {
      challenge.value = { ...updated }
    }
    if (challengeStore.showMilestoneModal) {
      showMilestoneModal.value = true
    }
  } else if (todayCompleted.value) {
    const updated = await challengeStore.refreshChallenge(challengeId.value)
    if (updated) {
      challenge.value = { ...updated }
    }
    if (challengeStore.showMilestoneModal) {
      showMilestoneModal.value = true
    }
  } else {
    const updated = await challengeStore.refreshChallenge(challengeId.value)
    if (updated) {
      challenge.value = { ...updated }
    }
  }
}

const goBack = () => {
  router.back()
}

const getCategoryIcon = (category) => {
  const icons = {
    '生活': '🏠', '学习': '📚', '作息': '⏰', '健康': '💪',
    '工作': '💼', '运动': '🏃', '阅读': '📖', '其他': '✨'
  }
  return icons[category] || '✨'
}

const closeMilestoneModal = () => {
  showMilestoneModal.value = false
  challengeStore.closeMilestoneModal()
}

const restartChallenge = async () => {
  try {
    await showConfirmDialog({
      title: '重新开始挑战',
      message: '确定要重新开始21天挑战吗？',
      confirmButtonColor: '#3b82f6'
    })
    
    const newChallenge = await challengeStore.startChallenge(challenge.value.habitId, 21)
    if (newChallenge) {
      challenge.value = newChallenge
      showToast('新挑战已开始')
    }
  } catch (e) {}
}

const confirmGiveUp = async () => {
  const success = await challengeStore.giveUpChallenge(challengeId.value)
  if (success) {
    showToast('已放弃挑战')
    showGiveUpConfirm.value = false
    router.back()
  }
}
</script>

<style lang="scss" scoped>
.challenge-page {
  padding: 0;
}

.challenge-container {
  min-height: 100vh;
}

.challenge-header {
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
  margin-bottom: 24px;
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

.challenge-title {
  font-size: 14px;
  color: $text-secondary;
}

.days-remaining {
  text-align: center;
  padding: 20px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 16px;
  backdrop-filter: blur(10px);
}

.days-number {
  font-size: 48px;
  font-weight: 700;
  line-height: 1;
  color: $text-primary;
}

.days-label {
  font-size: 14px;
  color: $text-secondary;
  margin-top: 4px;
}

.progress-section {
  padding: 24px 16px;
  margin-top: -40px;
}

.progress-ring-wrapper {
  @include flex-center;
  margin-bottom: 24px;
}

.progress-content {
  text-align: center;
  
  .progress-day {
    font-size: 28px;
    font-weight: 700;
    color: $text-primary;
  }
  
  .progress-total {
    font-size: 14px;
    color: $text-secondary;
    margin-top: 2px;
  }
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 4px;
  
  &.streak-warning {
    color: #ef4444;
  }
}

.stat-label {
  font-size: 13px;
  color: $text-secondary;
  position: relative;
}

.interrupt-tip {
  display: block;
  font-size: 11px;
  color: #ef4444;
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: $border-color;
}

.interrupt-warning {
  margin: 0 16px 20px;
  padding: 16px;
  background: #fef2f2;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.warning-icon {
  font-size: 32px;
}

.warning-text {
  flex: 1;
}

.warning-title {
  font-size: 15px;
  font-weight: 600;
  color: #dc2626;
  margin-bottom: 2px;
}

.warning-desc {
  font-size: 12px;
  color: #ef4444;
}

.milestones-section {
  padding: 0 16px 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.milestones-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.milestone-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.3s;
  
  &.achieved {
    background: linear-gradient(135deg, #fef3c7, #fde68a);
  }
  
  &.current {
    border: 2px solid #f59e0b;
  }
}

.milestone-icon {
  font-size: 28px;
}

.milestone-info {
  flex: 1;
}

.milestone-day {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 2px;
}

.milestone-label {
  font-size: 12px;
  color: $text-secondary;
}

.milestone-status {
  .check-icon {
    color: #10b981;
    font-size: 20px;
  }
  
  .pending-text {
    font-size: 12px;
    color: #9ca3af;
  }
}

.calendar-section {
  padding: 0 16px 20px;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
  margin-bottom: 12px;
}

.calendar-day {
  aspect-ratio: 1;
  border-radius: 8px;
  background: #f3f4f6;
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
  
  &.future {
    background: #f9fafb;
    color: #d1d5db;
  }
  
  &.not-started {
    opacity: 0.5;
  }
}

.day-number {
  font-size: 12px;
  font-weight: 500;
}

.day-check {
  font-size: 10px;
  margin-top: 2px;
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
  
  &.future {
    background: #f3f4f6;
    border: 1px solid #d1d5db;
  }
}

.action-section {
  padding: 0 16px 40px;
}

.milestone-popup {
  background: transparent !important;
  box-shadow: none !important;
}

.milestone-modal {
  padding: 30px 24px;
  text-align: center;
}

.milestone-celebration {
  margin-bottom: 20px;
}

.celebration-icon {
  font-size: 64px;
  margin-bottom: 12px;
  animation: bounce 0.6s ease-in-out;
}

@keyframes bounce {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

.celebration-title {
  font-size: 24px;
  font-weight: 700;
  color: $text-primary;
  margin-bottom: 8px;
}

.celebration-subtitle {
  font-size: 18px;
  font-weight: 600;
  color: #f59e0b;
}

.milestone-message {
  font-size: 14px;
  color: $text-secondary;
  line-height: 1.6;
  margin-bottom: 24px;
}

.giveup-form {
  padding: 24px 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.form-header {
  text-align: center;
  margin-bottom: 16px;
  
  h3 {
    font-size: 18px;
    font-weight: 600;
  }
}

.giveup-desc {
  font-size: 14px;
  color: $text-secondary;
  text-align: center;
  margin-bottom: auto;
  line-height: 1.6;
}

.form-actions {
  padding-top: 20px;
}
</style>
