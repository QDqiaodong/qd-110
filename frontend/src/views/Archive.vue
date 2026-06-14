<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="title">归档习惯回顾</div>
        <div class="subtitle">共 {{ store.archivedHabits.length }} 个已归档习惯 · 回顾曾经的坚持</div>
      </div>
    </div>

    <div v-if="store.archivedHabits.length === 0" class="empty-state">
      <div class="empty-icon">📦</div>
      <div class="empty-text">暂无归档的习惯</div>
      <div class="empty-desc">长期完成或暂不执行的习惯可以归档保存</div>
    </div>

    <div v-else class="review-grid">
      <div
        v-for="habit in store.archivedHabits"
        :key="habit.id"
        class="review-card"
        :style="{ '--habit-color': habit.color }"
        @click="toggleExpand(habit.id)"
      >
        <div class="card-top">
          <div class="habit-header">
            <div class="habit-icon" :style="{ background: habit.color + '20', color: habit.color }">
              {{ getCategoryIcon(habit.category) }}
            </div>
            <div class="habit-info">
              <div class="habit-name">{{ habit.name }}</div>
              <div class="habit-meta">
                <span class="habit-category">{{ habit.category }}</span>
                <span v-if="habit.starred" class="star-badge">⭐</span>
              </div>
            </div>
          </div>
          <van-icon :name="expandedId === habit.id ? 'arrow-up' : 'arrow-down'" class="expand-icon" />
        </div>

        <div class="stats-row">
          <div class="stat-circle">
            <svg class="progress-ring" viewBox="0 0 60 60">
              <circle class="progress-bg" cx="30" cy="30" r="25" />
              <circle 
                class="progress-fill" 
                cx="30" cy="30" r="25"
                :style="{ 
                  strokeDasharray: 157, 
                  strokeDashoffset: 157 - (157 * getArchivedStats(habit.id).completionRate / 100)
                }"
              />
            </svg>
            <div class="stat-circle-content">
              <div class="stat-circle-value">{{ getArchivedStats(habit.id).completionRate }}%</div>
              <div class="stat-circle-label">完成率</div>
            </div>
          </div>

          <div class="stat-blocks">
            <div class="stat-block">
              <div class="stat-block-icon">🔥</div>
              <div class="stat-block-content">
                <div class="stat-block-value">{{ getArchivedStats(habit.id).maxStreak }}</div>
                <div class="stat-block-label">最长连续</div>
              </div>
            </div>
            <div class="stat-block">
              <div class="stat-block-icon">✅</div>
              <div class="stat-block-content">
                <div class="stat-block-value">{{ getArchivedStats(habit.id).completedDays }}</div>
                <div class="stat-block-label">累计打卡</div>
              </div>
            </div>
          </div>
        </div>

        <div class="date-range">
          <van-icon name="calendar-o" />
          <span v-if="getArchivedStats(habit.id).firstDate">
            {{ formatDate(getArchivedStats(habit.id).firstDate) }} — {{ formatDate(habit.archiveTime) }}
          </span>
          <span v-else>暂无打卡记录</span>
        </div>

        <div class="archive-badge">
          <van-icon name="clock-o" />
          <span>归档于 {{ formatArchiveTime(habit.archiveTime) }}</span>
        </div>

        <div v-if="expandedId === habit.id" class="card-detail">
          <div class="detail-title">
            <span>活跃期打卡记录</span>
            <span class="detail-sub">
              共 {{ getArchivedStats(habit.id).activeDays }} 天 · 完成 {{ getArchivedStats(habit.id).completedDays }} 天
            </span>
          </div>
          <div class="calendar-grid" :title="getCalendarTitle(habit.id)">
            <div
              v-for="(day, index) in getArchiveCheckinDetail(habit.id)"
              :key="index"
              class="calendar-day"
              :class="{ completed: day.completed, empty: day.empty }"
              :title="day.date + (day.empty ? '' : (day.completed ? ' 已完成' : ' 未完成'))"
            >
              <div class="day-dot"></div>
            </div>
          </div>
          <div class="calendar-legend">
            <span class="legend-item">
              <span class="legend-dot completed"></span>
              <span>已完成</span>
            </span>
            <span class="legend-item">
              <span class="legend-dot"></span>
              <span>未完成</span>
            </span>
            <span class="legend-item">
              <span class="legend-dot empty"></span>
              <span>归档后</span>
            </span>
          </div>
        </div>

        <div class="card-actions" @click.stop>
          <van-button 
            size="small" 
            type="default" 
            plain 
            @click.stop="handleDelete(habit)"
          >
            删除
          </van-button>
          <van-button 
            size="small" 
            type="primary" 
            @click.stop="handleUnarchive(habit)"
          >
            重新启用
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useHabitStore } from '@/store/habit'
import { showToast, showConfirmDialog } from 'vant'
import dayjs from 'dayjs'

const store = useHabitStore()
const expandedId = ref(null)
const statsCache = ref({})

onMounted(() => {
  store.loadFromCache()
  initData()
})

const initData = async () => {
  await Promise.all([
    store.loadHabits(),
    store.loadArchivedHabits()
  ])
  await refreshStatsCache()
}

const refreshStatsCache = async () => {
  statsCache.value = {}
  const promises = store.archivedHabits.map(async (habit) => {
    const stats = await store.fetchArchivedHabitStats(habit.id, habit.archiveTime, true)
    statsCache.value[habit.id] = stats
    return stats
  })
  await Promise.all(promises)
}

const getCategoryIcon = (category) => {
  const icons = {
    '生活': '🏠', '学习': '📚', '作息': '⏰', '健康': '💪',
    '工作': '💼', '运动': '🏃', '阅读': '📖', '其他': '✨'
  }
  return icons[category] || '✨'
}

const getArchivedStats = (habitId) => {
  if (!statsCache.value[habitId]) {
    const habit = store.archivedHabits.find(h => h.id === habitId)
    if (habit) {
      statsCache.value[habitId] = store.getArchivedHabitStats(habitId, habit.archiveTime)
    }
  }
  return statsCache.value[habitId] || {
    totalDays: 0,
    completedDays: 0,
    maxStreak: 0,
    completionRate: 0,
    firstDate: null,
    lastDate: null,
    activeDays: 0
  }
}

const getArchiveCheckinDetail = (habitId) => {
  const habit = store.archivedHabits.find(h => h.id === habitId)
  if (!habit) return []
  
  const archiveDate = dayjs(habit.archiveTime)
  const stats = getArchivedStats(habitId)
  const startDate = stats.firstDate ? dayjs(stats.firstDate) : archiveDate.subtract(30, 'day')
  const days = Math.min(Math.max(stats.activeDays, 30), 90)
  
  const detail = []
  for (let i = days - 1; i >= 0; i--) {
    const date = startDate.add(days - 1 - i, 'day')
    const dateStr = date.format('YYYY-MM-DD')
    const isAfterArchive = date.isAfter(archiveDate, 'day')
    const completed = !isAfterArchive && store.checkins[dateStr]?.[habitId] === true
    
    detail.push({
      date: dateStr,
      completed,
      empty: isAfterArchive
    })
  }
  
  return detail
}

const getCalendarTitle = (habitId) => {
  const stats = getArchivedStats(habitId)
  return `活跃期: ${stats.activeDays}天 · 完成: ${stats.completedDays}天 · 最长连续: ${stats.maxStreak}天`
}

const formatDate = (date) => {
  if (!date) return '未知'
  return dayjs(date).format('YYYY/MM/DD')
}

const formatArchiveTime = (time) => {
  if (!time) return '未知'
  return dayjs(time).format('YYYY年MM月DD日')
}

const toggleExpand = (id) => {
  expandedId.value = expandedId.value === id ? null : id
}

const handleUnarchive = async (habit) => {
  try {
    await showConfirmDialog({
      title: '重新启用',
      message: `确定要重新启用「${habit.name}」吗？\n历史打卡记录和归档轨迹将完整保留。`,
      confirmButtonColor: '#3b82f6'
    })
    const result = await store.unarchiveHabit(habit.id)
    if (result) {
      showToast('已重新启用，历史记录完整保留')
      await refreshStatsCache()
    }
  } catch (e) {}
}

const handleDelete = async (habit) => {
  try {
    await showConfirmDialog({
      title: '删除习惯',
      message: `确定要删除「${habit.name}」吗？\n所有历史打卡记录将被永久删除，无法恢复。`,
      confirmButtonColor: '#ef4444'
    })
    const result = await store.deleteHabit(habit.id)
    if (result) {
      showToast('已删除')
      await refreshStatsCache()
    }
  } catch (e) {}
}
</script>

<style lang="scss" scoped>
.review-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
}

@media (min-width: 640px) {
  .review-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (min-width: 1024px) {
  .review-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

.review-card {
  @include card;
  padding: 20px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  background: linear-gradient(145deg, #ffffff 0%, #fafafa 100%);
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, var(--habit-color), var(--habit-color)80);
    opacity: 0.8;
  }
  
  &:active {
    transform: scale(0.98);
  }
}

.card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 16px;
}

.habit-header {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.habit-icon {
  width: 48px;
  height: 48px;
  border-radius: 14px;
  @include flex-center;
  font-size: 22px;
  flex-shrink: 0;
}

.habit-info {
  flex: 1;
  min-width: 0;
}

.habit-name {
  font-size: 17px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.habit-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: $text-secondary;
  align-items: center;
}

.habit-category {
  padding: 2px 8px;
  background: $bg-secondary;
  border-radius: 10px;
  font-size: 11px;
}

.star-badge {
  font-size: 12px;
}

.expand-icon {
  font-size: 18px;
  color: $text-secondary;
  margin-left: 8px;
  flex-shrink: 0;
}

.stats-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-circle {
  position: relative;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
}

.progress-ring {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.progress-bg {
  fill: none;
  stroke: #f3f4f6;
  stroke-width: 6;
}

.progress-fill {
  fill: none;
  stroke: var(--habit-color);
  stroke-width: 6;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.6s ease;
}

.stat-circle-content {
  position: absolute;
  inset: 0;
  @include flex-center;
  flex-direction: column;
}

.stat-circle-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--habit-color);
  line-height: 1.2;
}

.stat-circle-label {
  font-size: 10px;
  color: $text-secondary;
}

.stat-blocks {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-block {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  background: $bg-secondary;
  border-radius: 10px;
}

.stat-block-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.stat-block-content {
  flex: 1;
}

.stat-block-value {
  font-size: 16px;
  font-weight: 600;
  color: $text-primary;
  line-height: 1.2;
}

.stat-block-label {
  font-size: 11px;
  color: $text-secondary;
}

.date-range {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 8px;
  padding: 8px 12px;
  background: $bg-secondary;
  border-radius: 8px;
}

.archive-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--habit-color);
  font-weight: 500;
  padding: 6px 12px;
  background: var(--habit-color)15;
  border-radius: 8px;
  margin-bottom: 12px;
}

.card-detail {
  padding: 12px 0;
  border-top: 1px solid $border-color;
  margin-top: 4px;
}

.detail-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
  font-weight: 500;
  color: $text-primary;
}

.detail-sub {
  font-size: 11px;
  font-weight: normal;
  color: $text-secondary;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(15, 1fr);
  gap: 2px;
  margin-bottom: 10px;
}

.calendar-day {
  aspect-ratio: 1;
  border-radius: 2px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  
  &:hover {
    transform: scale(1.3);
    z-index: 1;
  }
  
  &.completed {
    background: var(--habit-color);
    
    .day-dot {
      background: transparent;
    }
  }
  
  &.empty {
    background: #f9fafb;
    opacity: 0.4;
  }
}

.day-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: transparent;
}

.calendar-legend {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  color: $text-secondary;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 2px;
  background: #f3f4f6;
  
  &.completed {
    background: var(--habit-color);
  }
  
  &.empty {
    background: #f9fafb;
    border: 1px solid #e5e7eb;
  }
}

.card-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid $border-color;
}
</style>
