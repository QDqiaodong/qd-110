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

    <div v-if="store.morningCards.length > 0" class="morning-cards-section">
      <div class="morning-cards-header">
        <div class="morning-cards-title">
          <span class="morning-icon">☀️</span>
          晨间提醒
        </div>
        <div class="morning-cards-subtitle">今日重点 · {{ morningCompletedCount }}/{{ store.morningCards.length }} 已完成</div>
      </div>
      <div class="morning-cards-list">
        <div
          v-for="card in store.morningCards"
          :key="card.id"
          class="morning-card"
          :class="{ completed: card.completed }"
          @click="toggleCheckin(card.id)"
        >
          <div class="morning-card-icon" :style="{ background: card.color + '20', color: card.color }">
            {{ getCategoryIcon(card.category) }}
          </div>
          <div class="morning-card-content">
            <div class="morning-card-name">{{ card.name }}</div>
            <div class="morning-card-meta">
              <span v-if="card.time" class="morning-card-time">{{ card.time }}</span>
              <span class="morning-card-category">{{ card.category }}</span>
            </div>
          </div>
          <div class="morning-card-status">
            <div v-if="card.completed" class="status-done">
              <van-icon name="success" />
              <span>已完成</span>
            </div>
            <div v-else class="status-pending">
              <div class="pending-circle" :style="{ borderColor: card.color }" />
              <span>待完成</span>
            </div>
          </div>
        </div>
      </div>
      <div v-if="morningAllDone" class="morning-cards-cheer">
        🎉 今日重点已全部完成，继续保持！
      </div>
    </div>

    <div v-if="store.gapSuggestions.length > 0" class="gap-suggestions-section">
      <div class="gap-suggestions-header">
        <div class="gap-suggestions-title">
          <span class="gap-icon">💡</span>
          空档建议板
        </div>
        <div class="gap-suggestions-subtitle">发现零散时间 · 塞入轻量习惯</div>
      </div>
      <div class="gap-suggestions-list">
        <div
          v-for="suggestion in store.gapSuggestions"
          :key="suggestion.id"
          class="gap-suggestion-card"
          :class="[`gap-type-${suggestion.type}`]"
        >
          <div class="gap-card-top">
            <div class="gap-card-icon">{{ suggestion.icon }}</div>
            <div class="gap-card-content">
              <div class="gap-card-title">{{ suggestion.title }}</div>
              <div class="gap-card-subtitle">{{ suggestion.subtitle }}</div>
            </div>
          </div>
          <div v-if="suggestion.suggestedHabits && suggestion.suggestedHabits.length > 0" class="gap-habit-tags">
            <div
              v-for="habit in suggestion.suggestedHabits"
              :key="habit.id"
              class="gap-habit-tag"
              :style="{ borderColor: habit.color, color: habit.color }"
              @click="toggleCheckin(habit.id)"
            >
              <span class="gap-habit-icon" :style="{ background: habit.color + '20' }">
                {{ getCategoryIcon(habit.category) }}
              </span>
              <span class="gap-habit-name">{{ habit.name }}</span>
              <span v-if="habit.time" class="gap-habit-time">{{ habit.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="view-toggle">
      <van-radio-group v-model="viewMode" direction="horizontal" shape="round">
        <van-radio name="time">
          <span class="radio-text">⏰ 按时段</span>
        </van-radio>
        <van-radio name="star">
          <span class="radio-text">⭐ 按星标</span>
        </van-radio>
      </van-radio-group>
    </div>

    <div v-if="viewMode === 'time'" class="time-period-section">
      <div v-for="group in store.timePeriodGroups" :key="group.key" class="period-group">
        <div v-if="group.habits.length > 0" class="period-section">
          <div class="period-header">
            <div class="period-title">{{ group.title }}</div>
            <div class="period-subtitle">{{ group.subtitle }} · {{ group.habits.length }}项</div>
          </div>
          <div class="period-list">
            <div
              v-for="habit in group.habits"
              :key="habit.id"
              class="habit-card period-card"
              :class="{ completed: isChecked(habit.id) }"
              @click="toggleCheckin(habit.id)"
            >
              <div class="habit-left">
                <div class="habit-icon" :style="{ background: habit.color + '20', color: habit.color }">
                  {{ getCategoryIcon(habit.category) }}
                </div>
                <div class="habit-info">
                  <div class="habit-name">
                    {{ habit.name }}
                    <span v-if="habit.starred" class="star-badge">⭐</span>
                  </div>
                  <div class="habit-meta">
                    <span class="habit-category">{{ habit.category }}</span>
                    <span v-if="habit.time" class="habit-time">⏰ {{ habit.time }}</span>
                  </div>
                </div>
              </div>
              <div class="habit-check">
                <van-icon name="chart-trending-o" class="detail-icon" @click.stop="goToDetail(habit)" />
                <van-checkbox 
                  :model-value="isChecked(habit.id)" 
                  :style="{ '--van-checkbox-checked-icon-color': habit.color }" 
                  @click.stop
                />
              </div>
            </div>
          </div>
        </div>
      </div>

      <div v-if="totalHabits === 0" class="empty-state">
        <div class="empty-icon">📋</div>
        <div class="empty-text">暂无习惯，去添加一个吧</div>
      </div>
    </div>

    <div v-else class="star-view">
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
              <van-icon name="chart-trending-o" class="detail-icon" @click.stop="goToDetail(habit)" />
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

        <div class="section-header non-starred-header" v-if="store.nonStarredHabits.length > 0">
          <div></div>
          <div class="section-action" @click="toggleNonStarredSortMode">
            <van-icon :name="isNonStarredSorting ? 'checked' : 'sort'" />
            <span>{{ isNonStarredSorting ? '完成' : '排序' }}</span>
          </div>
        </div>

        <div class="habit-list">
          <div v-if="store.nonStarredHabits.length === 0 && store.starredHabits.length === 0" class="empty-state">
            <div class="empty-icon">📋</div>
            <div class="empty-text">暂无习惯，去添加一个吧</div>
          </div>
          
          <div
            v-for="(habit, index) in store.nonStarredHabits"
            :key="habit.id"
            class="habit-card"
            :class="{ 
              completed: isChecked(habit.id),
              sorting: isNonStarredSorting,
              dragging: nonStarredDragIndex === index
            }"
            draggable="true"
            @dragstart="onNonStarredDragStart($event, index)"
            @dragover.prevent="onNonStarredDragOver($event, index)"
            @dragend="onNonStarredDragEnd"
            @click="!isNonStarredSorting && toggleCheckin(habit.id)"
          >
            <div v-if="isNonStarredSorting" class="drag-handle">
              <van-icon name="wap-nav" />
            </div>
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
              <van-icon name="chart-trending-o" class="detail-icon" @click.stop="goToDetail(habit)" />
              <van-checkbox :model-value="isChecked(habit.id)" :style="{ '--van-checkbox-checked-icon-color': habit.color }" @click.stop />
            </div>
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

    <van-popup v-model:show="showHabitMilestoneModal" round position="center" :style="{ width: '80%', maxWidth: '320px' }" class="milestone-popup">
      <div class="milestone-modal habit-milestone-modal">
        <div class="milestone-celebration">
          <div class="celebration-icon habit-milestone-icon">{{ habitMilestoneIcon }}</div>
          <div class="celebration-title habit-milestone-title">{{ habitMilestoneTitle }}</div>
          <div class="celebration-subtitle habit-milestone-subtitle">🎊 {{ habitMilestoneText }}</div>
        </div>
        <div class="milestone-message">
          {{ habitMilestoneMessage }}
        </div>
        <van-button 
          block 
          type="primary" 
          round
          :style="{ background: store.currentHabitMilestone?.habitColor || '#3b82f6', borderColor: store.currentHabitMilestone?.habitColor || '#3b82f6' }"
          @click="closeHabitMilestoneModal"
        >
          继续加油
        </van-button>
      </div>
    </van-popup>

    <van-popup v-model:show="showMissReasonModal" round position="bottom" :style="{ height: '70%' }">
      <div class="miss-reason-modal">
        <div class="modal-header">
          <div>
            <div class="modal-title">📝 记录未完成原因</div>
            <div class="modal-subtitle">{{ missedHabits.length }} 个习惯待记录，帮助后续复盘</div>
          </div>
          <van-icon name="cross" @click="showMissReasonModal = false" />
        </div>
        <div class="modal-content">
          <div v-for="habit in missedHabits" :key="habit.id" class="miss-habit-item">
            <div class="miss-habit-left">
              <div class="habit-icon" :style="{ background: habit.color + '20', color: habit.color }">
                {{ getCategoryIcon(habit.category) }}
              </div>
              <div class="habit-info">
                <div class="habit-name">{{ habit.name }}</div>
                <div class="habit-meta">
                  <span v-if="habit.time" class="habit-time">⏰ {{ habit.time }}</span>
                </div>
              </div>
            </div>
            <div class="miss-reason-section">
              <div class="preset-reasons">
                <van-tag
                  v-for="preset in store.missReasonPresets"
                  :key="preset"
                  :type="currentReasons[habit.id] === preset ? 'primary' : 'default'"
                  size="medium"
                  round
                  class="preset-tag"
                  @click="selectPresetReason(habit.id, preset)"
                >
                  {{ preset }}
                </van-tag>
              </div>
              <van-field
                v-model="customReasons[habit.id]"
                placeholder="或输入自定义原因..."
                class="custom-reason-input"
                @input="onCustomReasonChange(habit.id)"
              />
            </div>
          </div>
          <div v-if="missedHabits.length === 0" class="empty-miss">
            <div class="empty-icon">✨</div>
            <div class="empty-text">太棒了！今日习惯全部完成</div>
          </div>
        </div>
        <div class="modal-footer">
          <van-button block round type="primary" @click="saveMissReasons">
            保存记录
          </van-button>
        </div>
      </div>
    </van-popup>

    <van-button
      v-if="store.habits.length > 0 && missedHabits.length > 0"
      class="miss-reason-button"
      type="warning"
      round
      icon="edit"
      @click="openMissReasonModal"
    >
      记录原因
    </van-button>

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
import { useRouter } from 'vue-router'
import { useHabitStore } from '@/store/habit'
import { useChallengeStore } from '@/store/challenge'
import dayjs from 'dayjs'
import { showToast } from 'vant'

const router = useRouter()
const store = useHabitStore()
const challengeStore = useChallengeStore()
const viewMode = ref('time')
const showAdd = ref(false)
const showCategory = ref(false)
const showTime = ref(false)
const isSorting = ref(false)
const dragIndex = ref(-1)
const isNonStarredSorting = ref(false)
const nonStarredDragIndex = ref(-1)
const togglingHabitIds = ref(new Set())
const showMilestoneModal = ref(false)
const showHabitMilestoneModal = ref(false)
const showMissReasonModal = ref(false)
const pickerTime = ref(['00', '00'])
const currentReasons = ref({})
const customReasons = ref({})

const totalHabits = computed(() => store.habits.length)

const missedHabits = computed(() => {
  return store.getMissedHabitsWithReasons()
})

const morningCompletedCount = computed(() => store.morningCards.filter(c => c.completed).length)

const morningAllDone = computed(() => store.morningCards.length > 0 && morningCompletedCount.value === store.morningCards.length)

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
    
    if (checked && result.habitMilestoneInfo) {
      store.showHabitMilestone(result.habitMilestoneInfo)
      showHabitMilestoneModal.value = true
    } else if (checked && result.milestoneInfo) {
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

const toggleNonStarredSortMode = async () => {
  isNonStarredSorting.value = !isNonStarredSorting.value
  if (!isNonStarredSorting.value) {
    const newOrder = store.nonStarredHabits.map(h => h.id)
    await store.updateHabitsOrder(null, newOrder)
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

const onNonStarredDragStart = (e, index) => {
  if (!isNonStarredSorting.value) return
  nonStarredDragIndex.value = index
  e.dataTransfer.effectAllowed = 'move'
}

const onNonStarredDragOver = (e, index) => {
  if (!isNonStarredSorting.value || nonStarredDragIndex.value === -1 || nonStarredDragIndex.value === index) return
  
  const fromIndex = nonStarredDragIndex.value
  const toIndex = index
  
  if (fromIndex !== toIndex) {
    store.moveNonStarredHabit(fromIndex, toIndex)
    nonStarredDragIndex.value = toIndex
  }
}

const onNonStarredDragEnd = () => {
  nonStarredDragIndex.value = -1
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

const closeHabitMilestoneModal = () => {
  showHabitMilestoneModal.value = false
  store.closeHabitMilestoneModal()
}

const goToDetail = (habit) => {
  router.push(`/habit/${habit.id}`)
}

const habitMilestoneIcon = computed(() => {
  return store.currentHabitMilestone?.milestoneIcon || '🎉'
})

const habitMilestoneText = computed(() => {
  const info = store.currentHabitMilestone
  if (!info) return ''
  return `${info.milestoneType}次 · ${info.milestoneLabel}`
})

const habitMilestoneTitle = computed(() => {
  const info = store.currentHabitMilestone
  if (!info) return ''
  return `「${info.habitName}」`
})

const habitMilestoneMessage = computed(() => {
  return store.currentHabitMilestone?.message || ''
})

const openMissReasonModal = () => {
  currentReasons.value = {}
  customReasons.value = {}
  missedHabits.value.forEach(habit => {
    if (habit.reason) {
      if (store.missReasonPresets.includes(habit.reason)) {
        currentReasons.value[habit.id] = habit.reason
      } else {
        customReasons.value[habit.id] = habit.reason
      }
    }
  })
  showMissReasonModal.value = true
}

const selectPresetReason = (habitId, preset) => {
  if (currentReasons.value[habitId] === preset) {
    delete currentReasons.value[habitId]
  } else {
    currentReasons.value[habitId] = preset
    delete customReasons.value[habitId]
  }
}

const onCustomReasonChange = (habitId) => {
  const customVal = customReasons.value[habitId]?.trim()
  if (customVal) {
    delete currentReasons.value[habitId]
  }
}

const saveMissReasons = async () => {
  missedHabits.value.forEach(habit => {
    let reason = currentReasons.value[habit.id] || null
    if (!reason && customReasons.value[habit.id]?.trim()) {
      reason = customReasons.value[habit.id].trim()
    }
    store.saveMissReason(habit.id, reason)
  })
  showMissReasonModal.value = false
  showToast('已保存记录 ✨')
}
</script>

<style lang="scss" scoped>
.morning-cards-section {
  background: linear-gradient(135deg, #fef3c7 0%, #fff7ed 50%, #fef9c3 100%);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #fde68a;
  box-shadow: 0 4px 16px rgba(251, 191, 36, 0.12);
}

.morning-cards-header {
  margin-bottom: 12px;
}

.morning-cards-title {
  font-size: 17px;
  font-weight: 700;
  color: #92400e;
  display: flex;
  align-items: center;
  gap: 6px;

  .morning-icon {
    font-size: 20px;
  }
}

.morning-cards-subtitle {
  font-size: 12px;
  color: #b45309;
  margin-top: 2px;
}

.morning-cards-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.morning-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  transition: all 0.2s;
  border-left: 3px solid #f59e0b;

  &:active {
    transform: scale(0.98);
  }

  &.completed {
    opacity: 0.6;
    border-left-color: #d1d5db;

    .morning-card-name {
      text-decoration: line-through;
      color: $text-secondary;
    }
  }
}

.morning-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  @include flex-center;
  font-size: 18px;
  flex-shrink: 0;
}

.morning-card-content {
  flex: 1;
  min-width: 0;
}

.morning-card-name {
  font-size: 15px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 2px;
}

.morning-card-meta {
  display: flex;
  gap: 8px;
  font-size: 11px;
  color: $text-secondary;
}

.morning-card-time {
  font-weight: 500;
  color: #b45309;
}

.morning-card-status {
  flex-shrink: 0;
  text-align: center;
}

.status-done {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  color: $success-color;

  .van-icon {
    font-size: 22px;
  }

  span {
    font-size: 10px;
    font-weight: 500;
  }
}

.status-pending {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  color: $text-secondary;

  span {
    font-size: 10px;
    font-weight: 500;
  }
}

.pending-circle {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: currentColor;
    opacity: 0.3;
  }
}

.morning-cards-cheer {
  margin-top: 10px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #92400e;
  padding: 8px;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 8px;
}

.view-toggle {
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

.time-period-section {
  margin-bottom: 20px;
}

.period-group {
  margin-bottom: 20px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.period-section {
  .period-header {
    margin-bottom: 12px;
  }
  
  .period-title {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
    margin-bottom: 2px;
  }
  
  .period-subtitle {
    font-size: 12px;
    color: $text-secondary;
  }
}

.period-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.period-card {
  border-left: 3px solid transparent;
  
  &:active {
    transform: scale(0.98);
  }
}

.period-group:nth-child(1) .period-card {
  border-left-color: #f97316;
}

.period-group:nth-child(2) .period-card {
  border-left-color: #3b82f6;
}

.period-group:nth-child(3) .period-card {
  border-left-color: #8b5cf6;
}

.period-group:nth-child(4) .period-card {
  border-left-color: #9ca3af;
}

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

.non-starred-header {
  margin-bottom: 10px;
}

.habit-card {
  @include card;
  padding: 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  transition: all 0.2s;
  gap: 10px;
  
  &.completed {
    opacity: 0.7;
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

.habit-milestone-modal {
  .habit-milestone-icon {
    animation: bounce 0.6s ease-in-out;
  }
  
  .habit-milestone-title {
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
  }
  
  .habit-milestone-subtitle {
    font-size: 20px;
    font-weight: 700;
    color: #10b981;
  }
}

.habit-check {
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-icon {
  font-size: 20px;
  color: #9ca3af;
  transition: all 0.2s;
  
  &:active {
    transform: scale(1.2);
    color: $primary-color;
  }
}

.miss-reason-button {
  position: fixed;
  right: 20px;
  bottom: 152px;
  z-index: 10;
  box-shadow: 0 10px 24px rgba(245, 158, 11, 0.24);
}

.miss-reason-modal {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.modal-header {
  @include flex-between;
  padding: 20px;
  border-bottom: 1px solid $border-color;
  
  .van-icon {
    font-size: 20px;
    color: $text-secondary;
    cursor: pointer;
  }
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
}

.modal-subtitle {
  font-size: 12px;
  color: $text-secondary;
  margin-top: 4px;
}

.modal-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.miss-habit-item {
  @include card;
  padding: 14px;
  margin-bottom: 12px;
}

.miss-habit-left {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.miss-reason-section {
  padding-top: 12px;
  border-top: 1px solid $border-color;
}

.preset-reasons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.preset-tag {
  cursor: pointer;
  font-size: 13px;
  padding: 4px 12px;
}

.custom-reason-input {
  :deep(.van-field__control) {
    font-size: 13px;
  }
}

.empty-miss {
  text-align: center;
  padding: 60px 20px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
}

.empty-text {
  font-size: 14px;
  color: $text-secondary;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid $border-color;
}

.gap-suggestions-section {
  background: linear-gradient(135deg, #ecfeff 0%, #f0fdfa 50%, #eff6ff 100%);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 16px;
  border: 1px solid #a5f3fc;
  box-shadow: 0 4px 16px rgba(6, 182, 212, 0.10);
}

.gap-suggestions-header {
  margin-bottom: 12px;
}

.gap-suggestions-title {
  font-size: 17px;
  font-weight: 700;
  color: #155e75;
  display: flex;
  align-items: center;
  gap: 6px;

  .gap-icon {
    font-size: 20px;
  }
}

.gap-suggestions-subtitle {
  font-size: 12px;
  color: #0e7490;
  margin-top: 2px;
}

.gap-suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.gap-suggestion-card {
  background: rgba(255, 255, 255, 0.85);
  border-radius: 12px;
  padding: 12px 14px;
  border-left: 3px solid #06b6d4;
  transition: all 0.2s;

  &:active {
    transform: scale(0.98);
  }

  &.gap-type-time-gap {
    border-left-color: #f59e0b;
    background: linear-gradient(90deg, rgba(251, 191, 36, 0.06) 0%, rgba(255, 255, 255, 0.9) 100%);
  }

  &.gap-type-scene {
    border-left-color: #8b5cf6;
    background: linear-gradient(90deg, rgba(139, 92, 246, 0.06) 0%, rgba(255, 255, 255, 0.9) 100%);
  }

  &.gap-type-between-gap {
    border-left-color: #10b981;
    background: linear-gradient(90deg, rgba(16, 185, 129, 0.06) 0%, rgba(255, 255, 255, 0.9) 100%);
  }

  &.gap-type-light {
    border-left-color: #06b6d4;
    background: linear-gradient(90deg, rgba(6, 182, 212, 0.06) 0%, rgba(255, 255, 255, 0.9) 100%);
  }
}

.gap-card-top {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.gap-card-icon {
  width: 38px;
  height: 38px;
  border-radius: 10px;
  @include flex-center;
  font-size: 18px;
  flex-shrink: 0;
  background: rgba(6, 182, 212, 0.12);
}

.gap-type-time-gap .gap-card-icon {
  background: rgba(245, 158, 11, 0.15);
}

.gap-type-scene .gap-card-icon {
  background: rgba(139, 92, 246, 0.15);
}

.gap-type-between-gap .gap-card-icon {
  background: rgba(16, 185, 129, 0.15);
}

.gap-type-light .gap-card-icon {
  background: rgba(6, 182, 212, 0.15);
}

.gap-card-content {
  flex: 1;
  min-width: 0;
}

.gap-card-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 3px;
  line-height: 1.4;
}

.gap-card-subtitle {
  font-size: 11px;
  color: $text-secondary;
  line-height: 1.4;
}

.gap-habit-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed rgba(148, 163, 184, 0.25);
}

.gap-habit-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  border-radius: 18px;
  border: 1.5px solid;
  background: #fff;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;

  &:active {
    transform: scale(0.95);
    opacity: 0.8;
  }
}

.gap-habit-icon {
  width: 20px;
  height: 20px;
  border-radius: 6px;
  @include flex-center;
  font-size: 11px;
}

.gap-habit-name {
  white-space: nowrap;
}

.gap-habit-time {
  font-size: 10px;
  opacity: 0.7;
  padding-left: 4px;
  margin-left: 2px;
  border-left: 1px solid currentColor;
  opacity: 0.5;
}
</style>
