<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="title">作息偏差复盘</div>
        <div class="subtitle">发现问题，优化下一轮作息</div>
      </div>
      <van-calendar
        v-model:show="showCalendar"
        :default-date="new Date(selectedDate)"
        :max-date="new Date()"
        color="#3b82f6"
        @confirm="onDateConfirm"
      >
        <template #title>
          <div class="calendar-title">选择复盘日期</div>
        </template>
      </van-calendar>
      <div class="date-picker" @click="showCalendar = true">
        <van-icon name="calendar-o" />
        <span>{{ selectedDateLabel }}</span>
      </div>
    </div>

    <div class="overview-section">
      <div class="overview-header">
        <div class="overview-title">📊 今日概览</div>
        <div class="overview-rate">
          <van-circle :rate="deviationData.overall.overallRate" :stroke-width="6" size="56">
            <span class="rate-text">{{ deviationData.overall.overallRate }}%</span>
          </van-circle>
        </div>
      </div>
      <div class="overview-stats">
        <div class="stat-item">
          <div class="stat-value">{{ deviationData.overall.totalScheduleItems }}</div>
          <div class="stat-label">计划事项</div>
        </div>
        <div class="stat-item">
          <div class="stat-value">{{ deviationData.overall.totalHabits }}</div>
          <div class="stat-label">习惯总数</div>
        </div>
        <div class="stat-item">
          <div class="stat-value completed">{{ deviationData.overall.completedHabits }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
    </div>

    <div v-if="deviationData.overall.suggestions.length > 0" class="suggestions-section">
      <div class="section-title">💡 调整建议</div>
      <div class="suggestion-list">
        <div v-for="(suggestion, idx) in deviationData.overall.suggestions" :key="idx" class="suggestion-item">
          {{ suggestion }}
        </div>
      </div>
    </div>

    <div class="comparison-section">
      <div class="section-title">⏰ 计划 vs 实际</div>
      
      <div class="comparison-tabs">
        <van-radio-group v-model="viewMode" direction="horizontal" shape="round">
          <van-radio name="sidebyside">
            <span class="radio-text">并排对照</span>
          </van-radio>
          <van-radio name="timeline">
            <span class="radio-text">时间轴</span>
          </van-radio>
        </van-radio-group>
      </div>

      <div v-if="viewMode === 'sidebyside'" class="side-by-side">
        <div class="column planned-column">
          <div class="column-header">
            <div class="column-title">📝 计划安排</div>
            <div class="column-subtitle">{{ currentScheduleName }}</div>
          </div>
          <div class="column-content">
            <div v-for="period in deviationData.periods" :key="period.key" class="period-block">
              <div class="period-title">{{ period.title }}</div>
              <div v-if="period.scheduleItems.length === 0" class="empty-period">
                暂无计划
              </div>
              <div
                v-for="(item, idx) in period.scheduleItems"
                :key="idx"
                class="schedule-item"
                :class="{ 'overload-warning': period.deviationType === 'overload' }"
              >
                <span class="item-time">{{ item.time }}</span>
                <span class="item-title">{{ item.title }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="column actual-column">
          <div class="column-header">
            <div class="column-title">✅ 实际完成</div>
            <div class="column-subtitle">{{ selectedDateLabel }}</div>
          </div>
          <div class="column-content">
            <div v-for="period in deviationData.periods" :key="period.key" class="period-block">
              <div class="period-title">
                {{ period.title }}
                <span class="period-badge" :class="period.deviationType">
                  {{ period.deviationDesc }}
                </span>
              </div>
              <div v-if="period.habits.length === 0" class="empty-period">
                暂无习惯
              </div>
              <div
                v-for="habit in period.habits"
                :key="habit.id"
                class="habit-item"
                :class="{ completed: habitCompleted(habit.id) }"
              >
                <div class="habit-icon" :style="{ background: habit.color + '20', color: habit.color }">
                  {{ getCategoryIcon(habit.category) }}
                </div>
                <div class="habit-info">
                  <div class="habit-name">{{ habit.name }}</div>
                  <div class="habit-time">{{ habit.time || '未设时间' }}</div>
                </div>
                <div class="habit-status">
                  <van-icon v-if="habitCompleted(habit.id)" name="success" class="status-icon completed" />
                  <van-icon v-else name="cross" class="status-icon missed" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="timeline-view">
        <div class="timeline-list">
          <div
            v-for="slot in timeSlotComparison"
            :key="slot.time"
            class="timeline-slot"
            :class="slot.deviation"
          >
            <div class="slot-time">{{ slot.time }}</div>
            <div class="slot-content">
              <div class="slot-planned">
                <span class="label">计划：</span>
                <span class="text">{{ slot.planned }}</span>
              </div>
              <div class="slot-actual">
                <span class="label">实际：</span>
                <div v-if="slot.actualHabits.length === 0" class="no-habits">
                  无对应习惯
                </div>
                <div v-else class="actual-habits">
                  <div
                    v-for="habit in slot.actualHabits"
                    :key="habit.id"
                    class="actual-habit"
                    :class="{ completed: habit.completed }"
                  >
                    <span class="habit-dot" :style="{ background: habit.color }"></span>
                    <span class="habit-name">{{ habit.name }}</span>
                    <van-icon v-if="habit.completed" name="success" class="mini-icon" />
                    <van-icon v-else name="cross" class="mini-icon miss" />
                  </div>
                </div>
              </div>
            </div>
            <div class="slot-deviation">
              <span class="deviation-tag" :class="slot.deviation">
                {{ getDeviationLabel(slot.deviation) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="deviation-analysis-section">
      <div class="section-title">🔍 偏差分析</div>
      <div class="analysis-cards">
        <div
          v-for="period in deviationData.periods"
          :key="period.key"
          class="analysis-card"
          :class="{ 'has-issue': period.severity > 0 }"
        >
          <div class="card-header">
            <div class="card-title">{{ period.title }}</div>
            <van-tag :type="getSeverityType(period.severity)" round size="small">
              {{ period.deviationDesc }}
            </van-tag>
          </div>
          <div class="card-stats">
            <div class="stat-row">
              <span class="stat-label">计划事项</span>
              <span class="stat-value">{{ period.scheduleItems.length }} 项</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">对应习惯</span>
              <span class="stat-value">{{ period.habits.length }} 个</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">完成情况</span>
              <span class="stat-value">
                {{ period.completedHabits.length }}/{{ period.habits.length }}
                <span v-if="period.habits.length > 0" class="rate">
                  ({{ period.completionRate }}%)
                </span>
              </span>
            </div>
          </div>
          <div class="card-progress">
            <van-progress
              :percentage="period.completionRate"
              :color="getProgressColor(period)"
              stroke-width="6"
            />
          </div>
          <div v-if="period.severity > 0" class="card-suggestion">
            {{ getPeriodSuggestion(period) }}
          </div>
        </div>
      </div>
    </div>

    <div class="deviation-types-section">
      <div class="section-title">📌 偏差类型说明</div>
      <div class="type-cards">
        <div class="type-card procrastination">
          <div class="type-icon">⏳</div>
          <div class="type-info">
            <div class="type-name">拖延</div>
            <div class="type-desc">该时段2个以上习惯未完成</div>
          </div>
        </div>
        <div class="type-card missing">
          <div class="type-icon">❌</div>
          <div class="type-info">
            <div class="type-name">漏做</div>
            <div class="type-desc">有计划但未设置对应习惯</div>
          </div>
        </div>
        <div class="type-card overload">
          <div class="type-icon">📚</div>
          <div class="type-info">
            <div class="type-name">过度堆叠</div>
            <div class="type-desc">一时段安排超过5项任务</div>
          </div>
        </div>
      </div>
    </div>

    <div class="action-section">
      <van-button type="primary" round block icon="edit" @click="goToSchedule">
        去调整作息模板
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useHabitStore } from '@/store/habit'
import dayjs from 'dayjs'

const router = useRouter()
const store = useHabitStore()
const showCalendar = ref(false)
const selectedDate = ref(dayjs().format('YYYY-MM-DD'))
const viewMode = ref('sidebyside')

const selectedDateLabel = computed(() => {
  const date = dayjs(selectedDate.value)
  if (date.isSame(dayjs(), 'day')) {
    return '今天 ' + date.format('MM/DD')
  } else if (date.isSame(dayjs().subtract(1, 'day'), 'day')) {
    return '昨天 ' + date.format('MM/DD')
  }
  return date.format('YYYY年MM月DD日')
})

const currentScheduleName = computed(() => {
  return store.currentSchedule?.name || '未选择'
})

const deviationData = computed(() => {
  return store.getDeviationAnalysis(selectedDate.value)
})

const timeSlotComparison = computed(() => {
  return store.getTimeSlotComparison(selectedDate.value)
})

const habitCompleted = (habitId) => {
  const checkins = store.checkins[selectedDate.value] || {}
  return checkins[habitId] === true
}

const getCategoryIcon = (category) => {
  const icons = {
    '生活': '🏠', '学习': '📚', '作息': '⏰', '健康': '💪',
    '工作': '💼', '运动': '🏃', '阅读': '📖', '其他': '✨'
  }
  return icons[category] || '✨'
}

const getDeviationLabel = (deviation) => {
  const labels = {
    'completed': '全部完成',
    'partial': '部分完成',
    'missed': '全部未完成',
    'missing': '无对应习惯'
  }
  return labels[deviation] || deviation
}

const getSeverityType = (severity) => {
  if (severity >= 2) return 'danger'
  if (severity >= 1) return 'warning'
  return 'success'
}

const getProgressColor = (period) => {
  if (period.deviationType === 'overload') return '#ef4444'
  if (period.deviationType === 'procrastination') return '#f59e0b'
  if (period.deviationType === 'missing') return '#8b5cf6'
  if (period.completionRate >= 80) return '#10b981'
  if (period.completionRate >= 50) return '#f59e0b'
  return '#ef4444'
}

const getPeriodSuggestion = (period) => {
  if (period.deviationType === 'overload') {
    return '建议减少该时段的任务安排，聚焦最重要的事项'
  }
  if (period.deviationType === 'procrastination') {
    return '建议设置更明确的提醒，或将任务拆分为更小的步骤'
  }
  if (period.deviationType === 'missing') {
    return '建议添加与计划对应的习惯，让计划更落地'
  }
  if (period.completionRate < 50) {
    return '该时段完成率较低，建议调整任务难度或时间安排'
  }
  return '表现不错，继续保持！'
}

const onDateConfirm = (value) => {
  selectedDate.value = dayjs(value).format('YYYY-MM-DD')
  showCalendar.value = false
}

const goToSchedule = () => {
  router.push('/schedule')
}

onMounted(() => {
  store.loadFromCache()
  store.loadHabits()
})
</script>

<style lang="scss" scoped>
.date-picker {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #f3f4f6;
  border-radius: 20px;
  font-size: 13px;
  color: $text-secondary;
  
  &:active {
    background: #e5e7eb;
  }
}

.calendar-title {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  text-align: center;
}

.overview-section {
  @include card;
  padding: 20px;
  margin-bottom: 16px;
}

.overview-header {
  @include flex-between;
  margin-bottom: 16px;
}

.overview-title {
  font-size: 17px;
  font-weight: 600;
}

.overview-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: $text-primary;
  
  &.completed {
    color: $success-color;
  }
}

.stat-label {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 4px;
}

.rate-text {
  font-size: 13px;
  font-weight: 600;
  color: $primary-color;
}

.suggestions-section {
  @include card;
  padding: 16px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #fef3c7 0%, #fff7ed 100%);
  border: 1px solid #fde68a;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.suggestion-item {
  font-size: 14px;
  color: #92400e;
  line-height: 1.5;
}

.comparison-section {
  margin-bottom: 20px;
}

.comparison-tabs {
  margin-bottom: 16px;
  
  :deep(.van-radio-group) {
    background: #f3f4f6;
    padding: 4px;
    border-radius: 20px;
    gap: 4px;
  }
  
  :deep(.van-radio) {
    flex: 1;
    justify-content: center;
    padding: 8px 0;
    
    .van-radio__icon {
      display: none;
    }
    
    .van-radio__label {
      margin-left: 0 !important;
    }
  }
  
  :deep(.van-radio--checked) {
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    
    .radio-text {
      color: $primary-color;
      font-weight: 600;
    }
  }
}

.radio-text {
  font-size: 14px;
  color: $text-secondary;
}

.side-by-side {
  display: flex;
  gap: 12px;
}

.column {
  flex: 1;
  @include card;
  padding: 12px;
}

.column-header {
  text-align: center;
  padding-bottom: 12px;
  border-bottom: 1px solid $border-color;
  margin-bottom: 12px;
}

.column-title {
  font-size: 15px;
  font-weight: 600;
}

.column-subtitle {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 2px;
}

.column-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.period-block {
  margin-bottom: 4px;
}

.period-title {
  font-size: 13px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 8px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.period-badge {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  font-weight: 500;
  
  &.normal {
    background: $success-color + '20';
    color: $success-color;
  }
  
  &.procrastination {
    background: $warning-color + '20';
    color: $warning-color;
  }
  
  &.missing {
    background: '#8b5cf6' + '20';
    color: '#8b5cf6';
  }
  
  &.overload {
    background: $danger-color + '20';
    color: $danger-color;
  }
}

.empty-period {
  font-size: 12px;
  color: $text-secondary;
  text-align: center;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
}

.schedule-item {
  display: flex;
  gap: 8px;
  padding: 8px 10px;
  background: #f0f9ff;
  border-radius: 8px;
  margin-bottom: 6px;
  
  &.overload-warning {
    background: #fef2f2;
    border-left: 2px solid $danger-color;
  }
  
  .item-time {
    font-size: 12px;
    font-weight: 600;
    color: $primary-color;
    flex-shrink: 0;
  }
  
  .item-title {
    font-size: 12px;
    color: $text-primary;
  }
}

.habit-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 6px;
  
  &.completed {
    background: #f0fdf4;
    opacity: 0.8;
    
    .habit-name {
      text-decoration: line-through;
      color: $text-secondary;
    }
  }
  
  .habit-icon {
    width: 28px;
    height: 28px;
    border-radius: 8px;
    @include flex-center;
    font-size: 14px;
    flex-shrink: 0;
  }
  
  .habit-info {
    flex: 1;
    min-width: 0;
  }
  
  .habit-name {
    font-size: 12px;
    font-weight: 500;
    color: $text-primary;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .habit-time {
    font-size: 10px;
    color: $text-secondary;
  }
  
  .status-icon {
    font-size: 16px;
    
    &.completed {
      color: $success-color;
    }
    
    &.missed {
      color: $danger-color;
    }
  }
}

.timeline-view {
  @include card;
  padding: 16px;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.timeline-slot {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  border-left: 3px solid $border-color;
  
  &.completed {
    background: #f0fdf4;
    border-left-color: $success-color;
  }
  
  &.partial {
    background: #fffbeb;
    border-left-color: $warning-color;
  }
  
  &.missed {
    background: #fef2f2;
    border-left-color: $danger-color;
  }
  
  &.missing {
    background: #f5f3ff;
    border-left-color: #8b5cf6;
  }
}

.slot-time {
  font-size: 14px;
  font-weight: 600;
  color: $primary-color;
  flex-shrink: 0;
  width: 50px;
}

.slot-content {
  flex: 1;
  min-width: 0;
}

.slot-planned,
.slot-actual {
  display: flex;
  gap: 6px;
  font-size: 13px;
  
  .label {
    color: $text-secondary;
    flex-shrink: 0;
  }
  
  .text {
    color: $text-primary;
    font-weight: 500;
  }
}

.slot-planned {
  margin-bottom: 6px;
}

.no-habits {
  color: $text-secondary;
  font-style: italic;
}

.actual-habits {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.actual-habit {
  display: flex;
  align-items: center;
  gap: 6px;
  
  &.completed {
    opacity: 0.7;
    
    .habit-name {
      text-decoration: line-through;
    }
  }
  
  .habit-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }
  
  .habit-name {
    font-size: 13px;
    color: $text-primary;
  }
  
  .mini-icon {
    font-size: 14px;
    color: $success-color;
    
    &.miss {
      color: $danger-color;
    }
  }
}

.slot-deviation {
  flex-shrink: 0;
}

.deviation-tag {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 12px;
  font-weight: 500;
  
  &.completed {
    background: $success-color + '20';
    color: $success-color;
  }
  
  &.partial {
    background: $warning-color + '20';
    color: $warning-color;
  }
  
  &.missed {
    background: $danger-color + '20';
    color: $danger-color;
  }
  
  &.missing {
    background: '#8b5cf6' + '20';
    color: '#8b5cf6';
  }
}

.deviation-analysis-section {
  margin-bottom: 20px;
}

.analysis-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.analysis-card {
  @include card;
  padding: 16px;
  
  &.has-issue {
    border: 1px solid $warning-color;
  }
}

.card-header {
  @include flex-between;
  margin-bottom: 12px;
}

.card-title {
  font-size: 15px;
  font-weight: 600;
}

.card-stats {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 12px;
}

.stat-row {
  @include flex-between;
  font-size: 13px;
  
  .stat-label {
    color: $text-secondary;
  }
  
  .stat-value {
    color: $text-primary;
    font-weight: 500;
    
    .rate {
      color: $text-secondary;
      font-weight: 400;
    }
  }
}

.card-progress {
  margin-bottom: 10px;
}

.card-suggestion {
  font-size: 12px;
  color: $warning-color;
  padding: 8px 12px;
  background: $warning-color + '10';
  border-radius: 8px;
  line-height: 1.5;
}

.deviation-types-section {
  margin-bottom: 20px;
}

.type-cards {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.type-card {
  @include card;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  
  &.procrastination {
    border-left: 3px solid $warning-color;
  }
  
  &.missing {
    border-left: 3px solid #8b5cf6;
  }
  
  &.overload {
    border-left: 3px solid $danger-color;
  }
}

.type-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.type-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 2px;
}

.type-desc {
  font-size: 12px;
  color: $text-secondary;
}

.action-section {
  margin-bottom: 20px;
}
</style>
