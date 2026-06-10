<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="title">数据统计</div>
        <div class="subtitle">查看你的自律轨迹</div>
      </div>
    </div>

    <div class="stats-overview">
      <div class="stat-card">
        <div class="stat-value">{{ totalHabits }}</div>
        <div class="stat-label">习惯总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ todayCompleted }}</div>
        <div class="stat-label">今日完成</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ avgRate }}%</div>
        <div class="stat-label">周均完成率</div>
      </div>
    </div>

    <div class="chart-section">
      <div class="section-header">
        <div class="section-title">
          <van-radio-group v-model:active="statsType" direction="horizontal">
            <van-radio name="week">本周</van-radio>
            <van-radio name="month">本月</van-radio>
          </van-radio-group>
        </div>
      </div>
      <div class="chart-card">
        <div ref="chartRef" class="chart-container"></div>
      </div>
    </div>

    <div class="chart-section">
      <div class="section-header">
        <div class="section-title">习惯完成情况</div>
      </div>
      <div class="habit-stats">
        <div v-for="habit in habitStats" :key="habit.id" class="habit-stat-item">
          <div class="habit-left">
            <div class="habit-icon" :style="{ background: habit.color + '20', color: habit.color }">
              {{ getCategoryIcon(habit.category) }}
            </div>
            <div class="habit-info">
              <div class="habit-name">{{ habit.name }}</div>
              <div class="habit-rate">本周完成 {{ habit.completed }}/{{ weekStats.length }} 天</div>
            </div>
          </div>
          <div class="habit-progress">
            <van-progress :percentage="habit.rate" :color="habit.color" stroke-width="6" />
          </div>
        </div>
      </div>
    </div>

    <div class="chart-section">
      <div class="section-header">
        <div class="section-title">本周日历</div>
      </div>
      <div class="calendar-grid">
        <div
          v-for="day in weekStats"
          :key="day.date"
          class="calendar-day"
          :class="{ completed: day.rate >= 80, partial: day.rate > 0 && day.rate < 80 }"
        >
          <div class="day-label">{{ day.label }}</div>
          <div class="day-rate">{{ day.rate }}%</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useHabitStore } from '@/store/habit'
import * as echarts from 'echarts'

const store = useHabitStore()
const statsType = ref('week')
const chartRef = ref(null)
let chartInstance = null

const totalHabits = computed(() => store.habits.length)
const todayCompleted = computed(() => {
  const activeIds = store.habits.map(h => h.id)
  return Object.entries(store.todayCheckins).filter(([id, v]) => v && activeIds.includes(Number(id))).length
})
const avgRate = computed(() => {
  const stats = store.weekStats
  if (stats.length === 0) return 0
  const sum = stats.reduce((acc, s) => acc + s.rate, 0)
  return Math.round(sum / stats.length)
})

const weekStats = computed(() => store.weekStats)

const habitStats = computed(() => {
  return store.habits.map(habit => {
    let completed = 0
    for (let i = 6; i >= 0; i--) {
      const date = new Date()
      date.setDate(date.getDate() - i)
      const dateStr = date.toISOString().split('T')[0]
      if (store.checkins[dateStr]?.[habit.id]) {
        completed++
      }
    }
    return {
      ...habit,
      completed,
      rate: Math.round((completed / 7) * 100)
    }
  }).sort((a, b) => b.rate - a.rate)
})

onMounted(() => {
  store.loadFromCache()
  initData()
})

const initData = async () => {
  await store.loadHabits()
  nextTick(() => {
    initChart()
  })
}

watch(statsType, () => {
  nextTick(() => {
    updateChart()
  })
})

const initChart = () => {
  if (!chartRef.value) return
  chartInstance = echarts.init(chartRef.value)
  updateChart()
  window.addEventListener('resize', () => chartInstance?.resize())
}

const updateChart = () => {
  if (!chartInstance) return
  
  const data = statsType.value === 'week' ? getWeekData() : getMonthData()
  
  chartInstance.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: '{b}<br/>完成率: {c}%'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: data.labels,
      axisLine: { lineStyle: { color: '#e5e7eb' } },
      axisLabel: { color: '#6b7280', fontSize: 12 }
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: '#6b7280', fontSize: 12, formatter: '{value}%' },
      splitLine: { lineStyle: { color: '#f3f4f6' } }
    },
    series: [{
      data: data.values,
      type: 'bar',
      barWidth: '50%',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#60a5fa' },
          { offset: 1, color: '#3b82f6' }
        ]),
        borderRadius: [4, 4, 0, 0]
      }
    }]
  })
}

const getWeekData = () => {
  const stats = store.weekStats
  return {
    labels: stats.map(s => s.label),
    values: stats.map(s => s.rate)
  }
}

const getMonthData = () => {
  const labels = []
  const values = []
  const daysInMonth = new Date().getDate()
  const sampleDays = Math.min(daysInMonth, 30)
  const activeIds = store.habits.map(h => h.id)
  
  for (let i = sampleDays - 1; i >= 0; i -= 2) {
    const date = new Date()
    date.setDate(date.getDate() - i)
    const dateStr = date.toISOString().split('T')[0]
    labels.push(date.toLocaleDateString('zh-CN', { month: 'numeric', day: 'numeric' }))
    
    const checkins = store.checkins[dateStr] || {}
    const completed = Object.entries(checkins).filter(([id, v]) => v && activeIds.includes(Number(id))).length
    const rate = store.habits.length > 0 ? Math.round((completed / store.habits.length) * 100) : 0
    values.push(rate)
  }
  
  return { labels, values }
}

const getCategoryIcon = (category) => {
  const icons = {
    '生活': '🏠', '学习': '📚', '作息': '⏰', '健康': '💪',
    '工作': '💼', '运动': '🏃', '阅读': '📖', '其他': '✨'
  }
  return icons[category] || '✨'
}
</script>

<style lang="scss" scoped>
.stats-overview {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.stat-card {
  @include card;
  padding: 16px 12px;
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: $primary-color;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 12px;
  color: $text-secondary;
}

.chart-section {
  margin-bottom: 20px;
}

.section-header {
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
}

.chart-card {
  @include card;
  padding: 16px;
}

.chart-container {
  height: 220px;
  width: 100%;
}

.habit-stats {
  @include card;
  padding: 8px 0;
}

.habit-stat-item {
  padding: 12px 16px;
  
  &:not(:last-child) {
    border-bottom: 1px solid $border-color;
  }
}

.habit-left {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.habit-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  @include flex-center;
  font-size: 16px;
}

.habit-info {
  flex: 1;
}

.habit-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 2px;
}

.habit-rate {
  font-size: 12px;
  color: $text-secondary;
}

.calendar-grid {
  @include card;
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 8px;
}

.calendar-day {
  @include flex-center;
  flex-direction: column;
  padding: 12px 4px;
  border-radius: 8px;
  background: #f9fafb;
  
  &.completed {
    background: $success-color + '20';
    .day-rate { color: $success-color; }
  }
  
  &.partial {
    background: $warning-color + '20';
    .day-rate { color: $warning-color; }
  }
}

.day-label {
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 4px;
}

.day-rate {
  font-size: 12px;
  font-weight: 600;
  color: $text-secondary;
}
</style>
