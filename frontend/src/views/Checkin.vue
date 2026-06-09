<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="title">今日打卡</div>
        <div class="subtitle">{{ todayStr }} · 完成率 {{ store.completionRate }}%</div>
      </div>
      <van-circle v-model="completionRate" :rate="store.completionRate" :stroke-width="6" size="50">
        <span class="rate-text">{{ store.completionRate }}%</span>
      </van-circle>
    </div>

    <div class="habit-list">
      <div v-if="store.habits.length === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-text">暂无习惯，去添加一个吧</div>
      </div>
      
      <div
        v-for="habit in store.habits"
        :key="habit.id"
        class="habit-card"
        :class="{ completed: isChecked(habit.id) }"
        @click="toggleCheckin(habit.id)"
      >
        <div class="habit-left">
          <div class="habit-icon" :style="{ background: habit.color + '20', color: habit.color }">
            {{ getCategoryIcon(habit.category) }}
          </div>
          <div class="habit-info">
            <div class="habit-name">{{ habit.name }}</div>
            <div class="habit-meta">
              <span class="habit-category">{{ habit.category }}</span>
              <span v-if="habit.time" class="habit-time">⏰ {{ habit.time }}</span>
            </div>
          </div>
        </div>
        <div class="habit-check">
          <van-checkbox :model-value="isChecked(habit.id)" :style="{ '--van-checkbox-checked-icon-color': habit.color }" />
        </div>
      </div>
    </div>

    <van-popup v-model:show="showAdd" round position="bottom" :style="{ height: '60%' }">
      <div class="add-form">
        <div class="form-header">
          <h3>添加习惯</h3>
          <van-icon name="cross" @click="showAdd = false" />
        </div>
        <van-form @submit="addHabit">
          <van-cell-group inset>
            <van-field v-model="form.name" label="习惯名称" placeholder="请输入习惯名称" :rules="[{ required: true }]" />
            <van-field v-model="form.category" label="分类" is-link readonly placeholder="选择分类" @click="showCategory = true" />
            <van-field v-model="form.time" label="提醒时间" placeholder="选择时间" readonly is-link @click="showTime = true" />
            <van-cell title="开启提醒" is-link>
              <template #right-icon>
                <van-switch v-model="form.remind" size="20" />
              </template>
            </van-cell>
            <van-field label="选择颜色">
              <template #input>
                <div class="color-picker">
                  <div
                    v-for="color in colors"
                    :key="color"
                    class="color-item"
                    :class="{ active: form.color === color }"
                    :style="{ background: color }"
                    @click="form.color = color"
                  />
                </div>
              </template>
            </van-field>
          </van-cell-group>
          <div class="form-actions">
            <van-button round block type="primary" native-type="submit">确认添加</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="showCategory" round position="bottom">
      <van-picker
        :columns="categories"
        @confirm="(v) => { form.category = v; showCategory = false }"
        @cancel="showCategory = false"
      />
    </van-popup>

    <van-popup v-model:show="showTime" round position="bottom">
      <van-time-picker
        v-model="form.time"
        title="选择提醒时间"
        @confirm="showTime = false"
        @cancel="showTime = false"
      />
    </van-popup>

    <van-button
      v-if="store.habits.length > 0"
      class="floating-add-button"
      type="primary"
      round
      icon="plus"
      @click="showAdd = true"
    >
      添加
    </van-button>
    <van-button v-else block type="primary" round @click="showAdd = true" style="margin-top: 20px">
      添加第一个习惯
    </van-button>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useHabitStore } from '@/store/habit'
import dayjs from 'dayjs'
import { showToast } from 'vant'

const store = useHabitStore()
const completionRate = ref(0)
const showAdd = ref(false)
const showCategory = ref(false)
const showTime = ref(false)

const todayStr = computed(() => dayjs().format('YYYY年MM月DD日 dddd'))

const categories = ['生活', '学习', '作息', '健康', '工作', '运动', '阅读', '其他']
const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899']

const form = ref({
  name: '',
  category: '生活',
  time: '',
  remind: false,
  color: '#3b82f6'
})

onMounted(() => {
  store.loadFromCache()
  completionRate.value = store.completionRate
})

const isChecked = (habitId) => {
  return store.todayCheckins[habitId] || false
}

const toggleCheckin = (habitId) => {
  store.toggleCheckin(habitId)
  completionRate.value = store.completionRate
  showToast(isChecked(habitId) ? '已完成 ✨' : '已取消')
}

const addHabit = () => {
  store.addHabit(form.value)
  showAdd.value = false
  form.value = { name: '', category: '生活', time: '', remind: false, color: '#3b82f6' }
  showToast('添加成功')
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
.habit-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.habit-card {
  @include card;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.2s;
  
  &.completed {
    opacity: 0.7;
    .habit-name {
      text-decoration: line-through;
      color: $text-secondary;
    }
  }
  
  &:active {
    transform: scale(0.98);
  }
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
  gap: 12px;
  font-size: 12px;
  color: $text-secondary;
}

.rate-text {
  font-size: 12px;
  font-weight: 600;
  color: $primary-color;
}

.add-form {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.form-header {
  @include flex-between;
  margin-bottom: 20px;
  
  h3 {
    font-size: 18px;
    font-weight: 600;
  }
}

.form-actions {
  padding: 20px 16px 0;
}

.color-picker {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.color-item {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid transparent;
  
  &.active {
    border-color: $text-primary;
  }
}

.floating-add-button {
  position: fixed;
  right: 20px;
  bottom: 88px;
  z-index: 10;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.24);
}
</style>
