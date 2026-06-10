<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="title">习惯清单</div>
        <div class="subtitle">共 {{ store.habits.length }} 个习惯</div>
      </div>
      <div class="header-actions">
        <van-icon name="folder-o" class="archive-icon" @click="goToArchive" />
        <van-button type="primary" size="small" round icon="plus" @click="showAdd = true">
          添加
        </van-button>
      </div>
    </div>

    <div class="category-tabs">
      <van-tabs v-model:active="activeCategory" sticky line-width="24">
        <van-tab v-for="cat in categories" :key="cat" :title="cat" :name="cat" />
      </van-tabs>
    </div>

    <div class="habit-list">
      <div v-if="filteredHabits.length === 0" class="empty-state">
        <div class="empty-icon">📝</div>
        <div class="empty-text">该分类暂无习惯</div>
      </div>

      <div
        v-for="habit in filteredHabits"
        :key="habit.id"
        class="habit-card"
        @click="editHabit(habit)"
      >
        <div class="habit-left">
          <div class="habit-icon" :style="{ background: habit.color + '20', color: habit.color }">
            {{ getCategoryIcon(habit.category) }}
          </div>
          <div class="habit-info">
              <div class="habit-name">
                {{ habit.name }}
                <span v-if="habit.starred" class="star-badge">⭐</span>
                <span v-if="getHabitChallenge(habit.id)" class="challenge-badge">
                  🔥 {{ getChallengeDay(habit.id) }}天
                </span>
              </div>
              <div class="habit-meta">
                <span class="habit-category">{{ habit.category }}</span>
                <span v-if="habit.time" class="habit-time">⏰ {{ habit.time }}</span>
                <span v-if="habit.remind" class="habit-remind">🔔 提醒</span>
              </div>
            </div>
        </div>
        <div class="habit-actions">
          <van-icon
            :name="habit.starred ? 'star' : 'star-o'"
            :class="{ 'star-active': habit.starred }"
            class="star-icon"
            @click.stop="toggleStar(habit.id)"
          />
          <van-icon name="arrow" />
        </div>
      </div>
    </div>

    <van-popup v-model:show="showAdd" round position="bottom" :style="{ height: '70%' }">
      <div class="form-wrapper">
        <div class="form-header">
          <h3>{{ editingHabit ? '编辑习惯' : '添加习惯' }}</h3>
          <van-icon name="cross" @click="closeForm" />
        </div>
        <van-form @submit="submitForm">
          <van-cell-group inset>
            <van-field v-model="form.name" label="习惯名称" placeholder="请输入习惯名称" :rules="[{ required: true }]" />
            <van-field v-model="form.category" label="分类" is-link readonly placeholder="选择分类" @click="showCategory = true" />
            <van-field v-model="form.time" label="执行时间" placeholder="选择时间" readonly is-link @click="showTime = true" />
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
            <van-button 
              v-if="editingHabit && !getHabitChallenge(editingHabit.id)" 
              round block type="warning" 
              style="margin-bottom: 10px" 
              @click="startChallenge(editingHabit)"
            >
              🔥 发起21天挑战
            </van-button>
            <van-button 
              v-if="editingHabit && getHabitChallenge(editingHabit.id)" 
              round block type="primary" 
              style="margin-bottom: 10px" 
              @click="goToChallengeFromEdit"
            >
              查看挑战进度
            </van-button>
            <van-button v-if="editingHabit" round block type="default" style="margin-bottom: 10px" @click="archiveHabit">
              归档习惯
            </van-button>
            <van-button v-if="editingHabit" round block type="danger" style="margin-bottom: 10px" @click="deleteHabit">
              删除习惯
            </van-button>
            <van-button round block type="primary" native-type="submit">
              {{ editingHabit ? '保存修改' : '确认添加' }}
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="showCategory" round position="bottom">
      <van-picker
        :columns="categoryList"
        @confirm="(v) => { form.category = v; showCategory = false }"
        @cancel="showCategory = false"
      />
    </van-popup>

    <van-popup v-model:show="showTime" round position="bottom">
      <van-time-picker
        v-model="form.time"
        title="选择执行时间"
        @confirm="showTime = false"
        @cancel="showTime = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useHabitStore } from '@/store/habit'
import { useChallengeStore } from '@/store/challenge'
import { showToast, showConfirmDialog } from 'vant'

const store = useHabitStore()
const challengeStore = useChallengeStore()
const router = useRouter()
const activeCategory = ref('全部')
const showAdd = ref(false)
const showCategory = ref(false)
const showTime = ref(false)
const editingHabit = ref(null)

const categories = ['全部', '生活', '学习', '作息', '健康', '工作', '运动', '阅读', '其他']
const categoryList = ['生活', '学习', '作息', '健康', '工作', '运动', '阅读', '其他']
const colors = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899']

const form = ref({
  name: '',
  category: '生活',
  time: '',
  remind: false,
  starred: false,
  color: '#3b82f6'
})

const filteredHabits = computed(() => {
  if (activeCategory.value === '全部') return store.habits
  return store.habits.filter(h => h.category === activeCategory.value)
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

const editHabit = (habit) => {
  editingHabit.value = habit
  form.value = { ...habit }
  showAdd.value = true
}

const closeForm = () => {
  showAdd.value = false
  editingHabit.value = null
  form.value = { name: '', category: '生活', time: '', remind: false, starred: false, color: '#3b82f6' }
}

const submitForm = async () => {
  if (editingHabit.value) {
    await store.updateHabit(editingHabit.value.id, form.value)
    showToast('修改成功')
  } else {
    await store.addHabit(form.value)
    showToast('添加成功')
  }
  closeForm()
}

const deleteHabit = async () => {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: `确定要删除「${editingHabit.value.name}」吗？`,
      confirmButtonColor: '#ef4444'
    })
    await store.deleteHabit(editingHabit.value.id)
    showToast('删除成功')
    closeForm()
  } catch (e) {}
}

const archiveHabit = async () => {
  try {
    await showConfirmDialog({
      title: '确认归档',
      message: `确定要归档「${editingHabit.value.name}」吗？归档后不再出现在打卡和清单中，但历史数据仍可在归档页查看。`,
      confirmButtonColor: '#3b82f6'
    })
    const result = await store.archiveHabit(editingHabit.value.id)
    if (result) {
      showToast('已归档')
      closeForm()
    }
  } catch (e) {}
}

const goToArchive = () => {
  router.push('/archive')
}

const toggleStar = async (id) => {
  await store.toggleStarred(id)
}

const getCategoryIcon = (category) => {
  const icons = {
    '生活': '🏠', '学习': '📚', '作息': '⏰', '健康': '💪',
    '工作': '💼', '运动': '🏃', '阅读': '📖', '其他': '✨'
  }
  return icons[category] || '✨'
}

const getHabitChallenge = (habitId) => {
  return challengeStore.getChallengeByHabitId(habitId)
}

const getChallengeDay = (habitId) => {
  const challenge = getHabitChallenge(habitId)
  if (!challenge) return 0
  return challengeStore.getCurrentDay(challenge)
}

const startChallenge = async (habit) => {
  try {
    await showConfirmDialog({
      title: '发起21天挑战',
      message: `确定要为「${habit.name}」发起21天养成挑战吗？坚持就是胜利！`,
      confirmButtonColor: habit.color || '#3b82f6'
    })
    
    const challenge = await challengeStore.startChallenge(habit.id, 21)
    if (challenge) {
      showToast('挑战已开始，加油！')
      router.push(`/challenge/${challenge.id}`)
    } else {
      showToast('发起挑战失败')
    }
  } catch (e) {}
}

const goToChallenge = (habitId) => {
  const challenge = getHabitChallenge(habitId)
  if (challenge) {
    router.push(`/challenge/${challenge.id}`)
  }
}

const goToChallengeFromEdit = () => {
  if (editingHabit.value) {
    closeForm()
    goToChallenge(editingHabit.value.id)
  }
}
</script>

<style lang="scss" scoped>
.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.archive-icon {
  font-size: 22px;
  color: $text-secondary;
  cursor: pointer;
  
  &:active {
    opacity: 0.7;
  }
}

.category-tabs {
  margin: 0 -16px 16px;
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
  
  &:active {
    background: #f9fafb;
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
  flex-wrap: wrap;
}

.star-badge {
  font-size: 14px;
}

.challenge-badge {
  font-size: 11px;
  padding: 2px 8px;
  background: linear-gradient(135deg, #f97316, #ef4444);
  color: #fff;
  border-radius: 10px;
  font-weight: 500;
}

.habit-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.star-icon {
  font-size: 20px;
  color: #d1d5db;
  transition: all 0.2s;
  
  &.star-active {
    color: #f59e0b;
  }
  
  &:active {
    transform: scale(1.2);
  }
}

.habit-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: $text-secondary;
  flex-wrap: wrap;
}

.form-wrapper {
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
</style>
