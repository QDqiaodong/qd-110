<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="title">归档习惯</div>
        <div class="subtitle">共 {{ store.archivedHabits.length }} 个已归档习惯</div>
      </div>
    </div>

    <div v-if="store.archivedHabits.length === 0" class="empty-state">
      <div class="empty-icon">📦</div>
      <div class="empty-text">暂无归档的习惯</div>
      <div class="empty-desc">长期完成或暂不执行的习惯可以归档保存</div>
    </div>

    <div v-else class="archive-list">
      <div
        v-for="habit in store.archivedHabits"
        :key="habit.id"
        class="archive-card"
      >
        <div class="card-header">
          <div class="habit-left">
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
        </div>

        <div class="card-stats">
          <div class="stat-item">
            <div class="stat-value">{{ getHabitStats(habit.id).completedDays }}</div>
            <div class="stat-label">累计打卡</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ getHabitStats(habit.id).maxStreak }}</div>
            <div class="stat-label">最长连续</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ getHabitStats(habit.id).completionRate }}%</div>
            <div class="stat-label">完成率</div>
          </div>
        </div>

        <div class="card-footer">
          <div class="archive-time">
            <van-icon name="clock-o" />
            <span>归档于 {{ formatArchiveTime(habit.archiveTime) }}</span>
          </div>
          <van-button size="small" type="primary" plain @click="handleUnarchive(habit)">
            重新启用
          </van-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useHabitStore } from '@/store/habit'
import { showToast, showConfirmDialog } from 'vant'
import dayjs from 'dayjs'

const store = useHabitStore()

onMounted(() => {
  store.loadFromCache()
})

const getCategoryIcon = (category) => {
  const icons = {
    '生活': '🏠', '学习': '📚', '作息': '⏰', '健康': '💪',
    '工作': '💼', '运动': '🏃', '阅读': '📖', '其他': '✨'
  }
  return icons[category] || '✨'
}

const getHabitStats = (habitId) => {
  return store.getHabitCheckinStats(habitId)
}

const formatArchiveTime = (time) => {
  if (!time) return '未知'
  return dayjs(time).format('YYYY年MM月DD日')
}

const handleUnarchive = async (habit) => {
  try {
    await showConfirmDialog({
      title: '重新启用',
      message: `确定要重新启用「${habit.name}」吗？`,
      confirmButtonColor: '#3b82f6'
    })
    store.unarchiveHabit(habit.id)
    showToast('已重新启用')
  } catch (e) {}
}
</script>

<style lang="scss" scoped>
.archive-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.archive-card {
  @include card;
  padding: 16px;
}

.card-header {
  margin-bottom: 12px;
}

.habit-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.habit-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  @include flex-center;
  font-size: 20px;
}

.habit-info {
  flex: 1;
}

.habit-name {
  font-size: 16px;
  font-weight: 500;
  margin-bottom: 4px;
}

.habit-meta {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: $text-secondary;
  align-items: center;
}

.star-badge {
  font-size: 12px;
}

.card-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  padding: 12px 0;
  border-top: 1px solid $border-color;
  border-bottom: 1px solid $border-color;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: $primary-color;
  margin-bottom: 2px;
}

.stat-label {
  font-size: 12px;
  color: $text-secondary;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
}

.archive-time {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: $text-secondary;
}
</style>
