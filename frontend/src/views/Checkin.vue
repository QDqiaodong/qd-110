<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="title">今日打卡</div>
        <div class="subtitle">{{ todayStr }} · 完成率 {{ store.completionRate }}%</div>
      </div>
      <van-circle :rate="store.completionRate" :stroke-width="6" size="50">
        <span class="rate-text">{{ store.completionRate }}%</span>
      </van-circle>
    </div>

    <div v-if="store.starredHabits.length > 0" class="starred-section">
      <div class="section-header">
        <div class="section-title">
          <span class="star-icon">⭐</span>
          今日先做
        </div>
        <div class="section-action" @click="toggleSortMode">
          <van-icon :name="isSorting ? 'checked' : 'sort'" />
          <span>{{ isSorting ? '完成' : '排序' }}</span>
        </div>
      </div>
      
      <div class="starred-list">
        <div
          v-for="(habit, index) in store.starredHabits"
          :key="habit.id"
          class="starred-card"
          :class="{ 
            completed: isChecked(habit.id),
            sorting: isSorting,
            dragging: dragIndex === index
          }"
          draggable="true"
          @dragstart="onDragStart($event, index)"
          @dragover.prevent="onDragOver($event, index)"
          @dragend="onDragEnd"
          @click="!isSorting && toggleCheckin(habit.id)"
        >
          <div v-if="isSorting" class="drag-handle">
            <van-icon name="wap-nav" />
          </div>
          <div class="habit-left">
            <div class="habit-icon" :style="{ background: habit.color + '20', color: habit.color }">
              {{ getCategoryIcon(habit.category) }}
            </div>
            <div class="habit-info">
              <div class="habit-name">
                {{ habit.name }}
                <span class="star-badge">⭐</span>
              </div>
              <div class="habit-meta">
                <span class="habit-category">{{ habit.category }}</span>
                <span v-if="habit.time" class="habit-time">⏰ {{ habit.time }}</span>
              </div>
            </div>
          </div>
          <div class="habit-check">
            <van-checkbox 
              :model-value="isChecked(habit.id)" 
              :style="{ '--van-checkbox-checked-icon-color': habit.color }" 
              @click.stop
            />
          </div>
        </div>
      </div>
    </div>

    <div class="habit-section">
      <div v-if="store.starredHabits.length > 0" class="section-divider">
        <span>其他习惯</span>
      </div>

      <div class="habit-list">
        <div v-if="store.nonStarredHabits.length === 0 && store.starredHabits.length === 0" class="empty-state">
          <div class="empty-icon">📋</div>
          <div class="empty-text">暂无习惯，去添加一个吧</div>
        </div>
        
        <div
          v-for="habit in store.nonStarredHabits"
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
            <van-field v-model="form.time" label="提醒时间" placeholder="选择时间" readonly is-link @click="openTimePicker" />
            <van-cell title="开启提醒" is-link>
              <template #right-icon>
                <van-switch v-model="form.remind" size="20" />
              </template>
            </van-cell>
            <van-cell title="设为星标" is-link>
              <template #right-icon>
                <van-switch v-model="form.starred" size="20" />
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
        v-model="pickerTime"
        title="选择提醒时间"
        @confirm="onTimeConfirm"
        @cancel="showTime = false"
      />
    </van-popup>

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
import { useChallengeStore } from '@/store/challenge'
import dayjs from 'dayjs'
import { showToast } from 'vant'

const store = useHabitStore()
const challengeStore = useChallengeStore()
const showAdd = ref(false)
const showCategory = ref(false)
const showTime = ref(false)
const isSorting = ref(false)
const dragIndex = ref(-1)
const togglingHabitIds = ref(new Set())
const showMilestoneModal = ref(false)
const pickerTime = ref(['00', '00'])

const todayStr = computed(() => dayjs().format('YYYY年MM月DD日 dddd'))

const categories = ['生活', '学习', '作息', '健康', '工作', '运动', '阅读', '其他']
const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899']

const form = ref({
  name: '',
  category: '生活',
  time: '',
  remind: false,
  starred: false,
  color: '#3b82f6'
})

onMounted(() => {
  store.loadFromCache()
  challengeStore.loadFromCache()
  initData()
})

const initData = async () => {
  await Promise.all([
    store.loadHabits(),
    loadChallenges()
  ])
}

const loadChallenges = async () => {
  await challengeStore.loadActiveChallenges()
}

const onTimeConfirm = () => {
  form.value.time = pickerTime.value.join(':')
  showTime.value = false
}

const openTimePicker = () => {
  if (form.value.time && typeof form.value.time === 'string') {
    pickerTime.value = form.value.time.split(':')
  } else {
    pickerTime.value = ['00', '00']
  }
  showTime.value = true
}

const isChecked = (habitId) => {
  return store.todayCheckins[habitId] || false
}

const toggleCheckin = async (habitId) => {
  if (togglingHabitIds.value.has(habitId)) return
  togglingHabitIds.value.add(habitId)
  
  try {
    const result = await store.toggleCheckin(habitId)
    const checked = result.completed
    showToast(checked ? '已完成 ✨' : '已取消')
    
    if (checked && result.milestoneInfo) {
      challengeStore.updateChallengeFromMilestoneInfo(result.milestoneInfo)
      if (challengeStore.showMilestoneModal) {
        showMilestoneModal.value = true
      }
    } else if (checked) {
      const challenge = challengeStore.getChallengeByHabitId(habitId)
      if (challenge) {
        await challengeStore.refreshChallenge(challenge.id)
        if (challengeStore.showMilestoneModal) {
          showMilestoneModal.value = true
        }
      }
    }
  } finally {
    togglingHabitIds.value.delete(habitId)
  }
}

const addHabit = async () => {
  try {
    await store.addHabit(form.value)
    showAdd.value = false
    form.value = { name: '', category: '生活', time: '', remind: false, starred: false, color: '#3b82f6' }
    showToast('添加成功')
  } catch (e) {
    showToast(e.message || '添加失败')
  }
}

const getCategoryIcon = (category) => {
  const icons = {
    '生活': '🏠', '学习': '📚', '作息': '⏰', '健康': '💪',
    '工作': '💼', '运动': '🏃', '阅读': '📖', '其他': '✨'
  }
  return icons[category] || '✨'
}

const toggleSortMode = async () => {
  isSorting.value = !isSorting.value
  if (!isSorting.value) {
    const newOrder = store.starredHabits.map(h => h.id)
    await store.updateStarredOrder(newOrder)
  }
}

const onDragStart = (e, index) => {
  if (!isSorting.value) return
  dragIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
}

const onDragOver = (e, index) => {
  if (!isSorting.value || dragIndex.value === -1 || dragIndex.value === index) return
  
  const fromIndex = dragIndex.value
  const toIndex = index
  
  if (fromIndex !== toIndex) {
    store.moveStarredHabit(fromIndex, toIndex)
    dragIndex.value = toIndex
  }
}

const onDragEnd = () => {
  dragIndex.value = -1
}

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

const closeMilestoneModal = () => {
  showMilestoneModal.value = false
  challengeStore.closeMilestoneModal()
}
</script>

<style lang="scss" scoped>
.starred-section {
  margin-bottom: 20px;
}

.section-header {
  @include flex-between;
  margin-bottom: 12px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 6px;
  
  .star-icon {
    font-size: 18px;
  }
}

.section-action {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: $primary-color;
  cursor: pointer;
  
  &:active {
    opacity: 0.7;
  }
}

.starred-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.starred-card {
  @include card;
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.2s;
  border-left: 3px solid #f59e0b;
  
  &.completed {
    opacity: 0.65;
    border-left-color: #d1d5db;
    
    .habit-name {
      text-decoration: line-through;
      color: $text-secondary;
    }
  }
  
  &.sorting {
    cursor: grab;
    
    &:active {
      cursor: grabbing;
    }
  }
  
  &.dragging {
    opacity: 0.5;
    transform: scale(1.02);
  }
  
  &:active {
    transform: scale(0.98);
  }
}

.drag-handle {
  color: #9ca3af;
  font-size: 18px;
  cursor: grab;
  padding: 4px;
}

.section-divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0 12px;
  
  span {
    font-size: 13px;
    color: $text-secondary;
    white-space: nowrap;
  }
  
  &::before,
  &::after {
    content: '';
    flex: 1;
    height: 1px;
    background: $border-color;
  }
}

.habit-section {
  margin-bottom: 20px;
}

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
  display: flex;
  align-items: center;
  gap: 6px;
}

.star-badge {
  font-size: 13px;
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

.milestone-popup {
  background: transparent !important;
  box-shadow: none !important;
}

.milestone-modal {
  padding: 30px 24px;
  text-align: center;
  background: #fff;
  border-radius: 16px;
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
</style>
