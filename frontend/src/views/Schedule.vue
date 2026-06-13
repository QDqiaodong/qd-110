<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <div class="title">作息模板</div>
        <div class="subtitle">当前：{{ store.currentSchedule?.name || '未选择' }}</div>
      </div>
      <van-button type="primary" size="small" round icon="plus" @click="showAdd = true">
        自定义
      </van-button>
    </div>

    <div class="review-entry" @click="goToReview">
      <div class="review-icon">📊</div>
      <div class="review-info">
        <div class="review-title">作息偏差复盘</div>
        <div class="review-desc">查看计划与实际的偏差分析</div>
      </div>
      <van-icon name="arrow" />
    </div>

    <div class="history-section">
      <div class="history-header" @click="showHistory = !showHistory">
        <div class="history-header-left">
          <span class="history-icon">🕐</span>
          <span class="history-title">启用历史</span>
          <span class="history-count">{{ historyList.length }}条记录</span>
        </div>
        <van-icon :name="showHistory ? 'arrow-up' : 'arrow-down'" />
      </div>

      <div v-if="showHistory" class="history-timeline">
        <div v-if="historyList.length === 0" class="history-empty">
          暂无模板切换记录
        </div>
        <div
          v-for="(item, idx) in historyList"
          :key="idx"
          class="history-item"
          :class="{ active: item.isActive }"
        >
          <div class="history-dot-wrap">
            <div class="history-dot" :class="{ active: item.isActive }"></div>
            <div v-if="idx < historyList.length - 1" class="history-line"></div>
          </div>
          <div class="history-content">
            <div class="history-template-name">
              {{ item.templateName }}
              <van-tag v-if="item.isActive" type="primary" round size="mini">使用中</van-tag>
            </div>
            <div class="history-meta">
              <span class="meta-item">
                <van-icon name="clock-o" size="12" />
                {{ formatHistoryDate(item.startDate) }} 启用
              </span>
              <span v-if="item.endDate" class="meta-item">
                → {{ formatHistoryDate(item.endDate) }} 结束
              </span>
            </div>
            <div class="history-stats">
              <span class="stat-badge duration">
                <van-icon name="calendar-o" size="12" />
                {{ item.durationText }}
              </span>
              <span class="stat-badge rate" :class="getRateClass(item.completionRate)">
                <van-icon name="bar-chart-o" size="12" />
                {{ item.completionRate }}%完成率
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="dual-view-header">
      <div class="day-type-badge weekday">
        <span class="badge-icon">📅</span>
        <span class="badge-text">平日作息</span>
        <span class="badge-days">周一至周五</span>
      </div>
      <div class="day-type-divider">VS</div>
      <div class="day-type-badge weekend">
        <span class="badge-icon">🌴</span>
        <span class="badge-text">休息日安排</span>
        <span class="badge-days">周六至周日</span>
      </div>
    </div>

    <div class="template-tabs">
      <van-tabs v-model:active="activeTab" sticky line-width="24">
        <van-tab title="系统模板" name="system" />
        <van-tab title="我的模板" name="custom" />
      </van-tabs>
    </div>

    <div v-if="activeTab === 'system'" class="template-list">
      <div
        v-for="tpl in store.templates"
        :key="tpl.id"
        class="template-card dual-column-card"
        :class="{ active: store.currentSchedule?.id === tpl.id }"
        @click="selectTemplate(tpl)"
      >
        <div class="template-header">
          <div class="template-name" :title="tpl.name">{{ tpl.name }}</div>
          <van-tag v-if="store.currentSchedule?.id === tpl.id" type="primary" round size="small">使用中</van-tag>
        </div>
        
        <div class="dual-column-content">
          <div class="schedule-column weekday-column">
            <div class="column-label">
              <span class="label-icon">📅</span>
              <span>平日</span>
              <span class="item-count">{{ getWeekdayItems(tpl).length }}项</span>
            </div>
            <div class="schedule-items">
              <div v-for="(item, idx) in getWeekdayItems(tpl).slice(0, 4)" :key="idx" class="schedule-item">
                <span class="item-time">{{ item.time }}</span>
                <span class="item-title" :title="item.title">{{ item.title }}</span>
              </div>
              <div v-if="getWeekdayItems(tpl).length > 4" class="more-items">
                还有 {{ getWeekdayItems(tpl).length - 4 }} 项...
              </div>
              <div v-if="getWeekdayItems(tpl).length === 0" class="empty-items">暂无安排</div>
            </div>
          </div>
          
          <div class="column-separator"></div>
          
          <div class="schedule-column weekend-column">
            <div class="column-label">
              <span class="label-icon">🌴</span>
              <span>周末</span>
              <span class="item-count">{{ getWeekendItems(tpl).length }}项</span>
            </div>
            <div class="schedule-items">
              <div v-for="(item, idx) in getWeekendItems(tpl).slice(0, 4)" :key="idx" class="schedule-item">
                <span class="item-time">{{ item.time }}</span>
                <span class="item-title" :title="item.title">{{ item.title }}</span>
              </div>
              <div v-if="getWeekendItems(tpl).length > 4" class="more-items">
                还有 {{ getWeekendItems(tpl).length - 4 }} 项...
              </div>
              <div v-if="getWeekendItems(tpl).length === 0" class="empty-items">暂无安排</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="template-list">
      <div v-if="store.schedules.length === 0" class="empty-state">
        <div class="empty-icon">📅</div>
        <div class="empty-text">暂无自定义模板</div>
      </div>
      <div
        v-for="tpl in store.schedules"
        :key="tpl.id"
        class="template-card dual-column-card"
        :class="{ active: store.currentSchedule?.id === tpl.id }"
        @click="selectTemplate(tpl)"
      >
        <div class="template-header">
          <div class="template-name" :title="tpl.name">{{ tpl.name }}</div>
          <div class="template-actions">
            <van-icon name="delete-o" size="18" @click.stop="deleteTemplate(tpl.id)" />
          </div>
        </div>
        
        <div class="dual-column-content">
          <div class="schedule-column weekday-column">
            <div class="column-label">
              <span class="label-icon">📅</span>
              <span>平日</span>
              <span class="item-count">{{ getWeekdayItems(tpl).length }}项</span>
            </div>
            <div class="schedule-items">
              <div v-for="(item, idx) in getWeekdayItems(tpl).slice(0, 4)" :key="idx" class="schedule-item">
                <span class="item-time">{{ item.time }}</span>
                <span class="item-title" :title="item.title">{{ item.title }}</span>
              </div>
              <div v-if="getWeekdayItems(tpl).length > 4" class="more-items">
                还有 {{ getWeekdayItems(tpl).length - 4 }} 项...
              </div>
              <div v-if="getWeekdayItems(tpl).length === 0" class="empty-items">暂无安排</div>
            </div>
          </div>
          
          <div class="column-separator"></div>
          
          <div class="schedule-column weekend-column">
            <div class="column-label">
              <span class="label-icon">🌴</span>
              <span>周末</span>
              <span class="item-count">{{ getWeekendItems(tpl).length }}项</span>
            </div>
            <div class="schedule-items">
              <div v-for="(item, idx) in getWeekendItems(tpl).slice(0, 4)" :key="idx" class="schedule-item">
                <span class="item-time">{{ item.time }}</span>
                <span class="item-title" :title="item.title">{{ item.title }}</span>
              </div>
              <div v-if="getWeekendItems(tpl).length > 4" class="more-items">
                还有 {{ getWeekendItems(tpl).length - 4 }} 项...
              </div>
              <div v-if="getWeekendItems(tpl).length === 0" class="empty-items">暂无安排</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <van-popup v-model:show="showDetail" round position="bottom" :style="{ height: '85%' }">
      <div class="detail-wrapper" v-if="store.currentSchedule">
        <div class="detail-header">
          <h3>{{ store.currentSchedule.name }}</h3>
          <van-icon name="cross" @click="showDetail = false" />
        </div>
        
        <div class="detail-dual-view">
          <div class="detail-column weekday-detail">
            <div class="detail-column-header">
              <span class="header-icon">📅</span>
              <span class="header-title">平日作息</span>
              <span class="header-subtitle">周一至周五</span>
            </div>
            <div class="detail-timeline">
              <div
                v-for="(item, idx) in store.getWeekdayItems"
                :key="'wd-' + idx"
                class="timeline-item"
              >
                <div class="timeline-time">{{ item.time }}</div>
                <div class="timeline-dot"></div>
                <div class="timeline-content" :title="item.title">{{ item.title }}</div>
              </div>
              <div v-if="store.getWeekdayItems.length === 0" class="empty-timeline">
                暂无平日安排
              </div>
            </div>
          </div>
          
          <div class="detail-column weekend-detail">
            <div class="detail-column-header weekend">
              <span class="header-icon">🌴</span>
              <span class="header-title">休息日安排</span>
              <span class="header-subtitle">周六至周日</span>
            </div>
            <div class="detail-timeline">
              <div
                v-for="(item, idx) in store.getWeekendItems"
                :key="'we-' + idx"
                class="timeline-item"
              >
                <div class="timeline-time">{{ item.time }}</div>
                <div class="timeline-dot weekend"></div>
                <div class="timeline-content weekend" :title="item.title">{{ item.title }}</div>
              </div>
              <div v-if="store.getWeekendItems.length === 0" class="empty-timeline">
                暂无周末安排
              </div>
            </div>
          </div>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showAdd" round position="bottom" :style="{ height: '90%' }">
      <div class="add-wrapper">
        <div class="form-header">
          <h3>创建作息模板</h3>
          <van-icon name="cross" @click="showAdd = false" />
        </div>
        <van-form @submit="createTemplate">
          <van-cell-group inset>
            <van-field v-model="newTpl.name" label="模板名称" placeholder="请输入模板名称" :rules="[{ required: true }]" />
          </van-cell-group>
          
          <div class="items-tabs">
            <van-radio-group v-model="activeItemTab" direction="horizontal" shape="round">
              <van-radio name="weekday">
                <span class="radio-text">📅 平日</span>
              </van-radio>
              <van-radio name="weekend">
                <span class="radio-text">🌴 周末</span>
              </van-radio>
            </van-radio-group>
          </div>

          <div v-if="activeItemTab === 'weekday'" class="items-section">
            <div class="section-title">
              <span class="title-icon">📅</span>
              平日时间安排
            </div>
            <div class="items-list">
              <div v-for="(item, idx) in newTpl.weekdayItems" :key="idx" class="item-row">
                <van-field
                  v-model="item.time"
                  placeholder="时间"
                  readonly
                  is-link
                  class="time-input"
                  @click="editItemTime('weekday', idx)"
                />
                <van-field v-model="item.title" placeholder="事项" class="title-input" />
                <van-icon name="minus" class="remove-btn" @click="removeItem('weekday', idx)" />
              </div>
            </div>
            <van-button block plain type="primary" icon="plus" size="small" @click="addItem('weekday')">
              添加事项
            </van-button>
          </div>

          <div v-else class="items-section">
            <div class="section-title">
              <span class="title-icon">🌴</span>
              周末时间安排
            </div>
            <div class="items-list">
              <div v-for="(item, idx) in newTpl.weekendItems" :key="idx" class="item-row">
                <van-field
                  v-model="item.time"
                  placeholder="时间"
                  readonly
                  is-link
                  class="time-input"
                  @click="editItemTime('weekend', idx)"
                />
                <van-field v-model="item.title" placeholder="事项" class="title-input" />
                <van-icon name="minus" class="remove-btn" @click="removeItem('weekend', idx)" />
              </div>
            </div>
            <van-button block plain type="primary" icon="plus" size="small" @click="addItem('weekend')">
              添加事项
            </van-button>
          </div>

          <div class="form-actions">
            <van-button round block type="primary" native-type="submit">创建模板</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="showTimePicker" round position="bottom">
      <van-time-picker
        v-model="pickedTime"
        title="选择时间"
        @confirm="confirmTime"
        @cancel="showTimePicker = false"
      />
    </van-popup>

    <van-popup v-model:show="showConflictDialog" round position="center" :style="{ width: '90%', maxWidth: '360px' }" class="conflict-popup">
      <div class="conflict-dialog">
        <div class="conflict-header">
          <div class="conflict-icon">⚠️</div>
          <div class="conflict-title">检测到潜在冲突</div>
          <div class="conflict-subtitle">切换到「{{ pendingTemplate?.name }}」前请确认</div>
        </div>
        
        <div class="conflict-content">
          <div v-if="conflicts.overloadedSlots.length > 0" class="conflict-section">
            <div class="conflict-section-title">
              <van-tag type="danger" round size="medium">时段堆叠</van-tag>
            </div>
            <div class="conflict-list">
              <div v-for="(slot, idx) in conflicts.overloadedSlots" :key="'ol-' + idx" class="conflict-item">
                <div class="conflict-item-time">{{ slot.time }}</div>
                <div class="conflict-item-info">
                  <div class="conflict-item-desc">
                    该时段有 <b>{{ slot.habitCount }}</b> 个习惯 + <b>{{ slot.scheduleItemCount }}</b> 项计划
                  </div>
                  <div class="conflict-item-habits">
                    <span v-for="(habit, hIdx) in slot.habits.slice(0, 3)" :key="hIdx" class="habit-tag">
                      {{ habit.name }}
                    </span>
                    <span v-if="slot.habits.length > 3" class="more-habits">+{{ slot.habits.length - 3 }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div v-if="conflicts.unplaceableHabits.length > 0" class="conflict-section">
            <div class="conflict-section-title">
              <van-tag type="warning" round size="medium">无法匹配</van-tag>
            </div>
            <div class="conflict-list">
              <div v-for="(item, idx) in conflicts.unplaceableHabits" :key="'up-' + idx" class="conflict-item">
                <div class="conflict-item-time">{{ item.habit.time }}</div>
                <div class="conflict-item-info">
                  <div class="conflict-item-desc">
                    <b>{{ item.habit.name }}</b> 没有对应时段
                  </div>
                  <div class="conflict-item-suggestion">
                    建议调整到最近时段 <b>{{ item.nearestSlot }}</b>（相差{{ item.nearestDiff }}小时）
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="conflict-summary">
            共检测到 <b class="danger-text">{{ conflicts.totalConflicts }}</b> 处潜在问题
          </div>
        </div>
        
        <div class="conflict-actions">
          <van-button round block plain type="default" @click="cancelTemplateSwitch">
            取消切换
          </van-button>
          <van-button round block type="primary" @click="confirmTemplateSwitch">
            确认切换
          </van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useHabitStore } from '@/store/habit'
import { showToast, showConfirmDialog } from 'vant'
import dayjs from 'dayjs'

const router = useRouter()
const store = useHabitStore()
const activeTab = ref('system')
const showAdd = ref(false)
const showDetail = ref(false)
const showTimePicker = ref(false)
const showConflictDialog = ref(false)
const showHistory = ref(false)
const editingType = ref('')
const editingIdx = ref(-1)
const pickedTime = ref('')
const activeItemTab = ref('weekday')
const pendingTemplate = ref(null)
const conflicts = ref({
  overloadedSlots: [],
  unplaceableHabits: [],
  totalConflicts: 0
})

const historyList = computed(() => store.templateHistoryWithStats || [])

const formatHistoryDate = (dateStr) => {
  if (!dateStr) return ''
  return dayjs(dateStr).format('MM/DD HH:mm')
}

const getRateClass = (rate) => {
  if (rate >= 80) return 'rate-high'
  if (rate >= 50) return 'rate-medium'
  return 'rate-low'
}

const newTpl = ref({
  name: '',
  weekdayItems: [
    { time: '07:00', title: '' },
    { time: '08:00', title: '' }
  ],
  weekendItems: [
    { time: '09:00', title: '' },
    { time: '10:00', title: '' }
  ]
})

onMounted(() => {
  store.loadFromCache()
})

const getWeekdayItems = (tpl) => {
  return tpl.weekdayItems || tpl.items || []
}

const getWeekendItems = (tpl) => {
  return tpl.weekendItems || tpl.items || []
}

const selectTemplate = (tpl) => {
  if (store.currentSchedule?.id === tpl.id) {
    showDetail.value = true
    return
  }
  
  const detectedConflicts = store.analyzeTemplateConflicts(tpl)
  if (detectedConflicts.totalConflicts > 0) {
    pendingTemplate.value = tpl
    conflicts.value = detectedConflicts
    showConflictDialog.value = true
  } else {
    applyTemplate(tpl)
  }
}

const applyTemplate = (tpl) => {
  store.setCurrentSchedule(tpl)
  showToast('已切换作息模板')
  showDetail.value = true
}

const cancelTemplateSwitch = () => {
  showConflictDialog.value = false
  pendingTemplate.value = null
}

const confirmTemplateSwitch = () => {
  if (pendingTemplate.value) {
    applyTemplate(pendingTemplate.value)
  }
  showConflictDialog.value = false
  pendingTemplate.value = null
}

const addItem = (type) => {
  if (type === 'weekday') {
    newTpl.value.weekdayItems.push({ time: '12:00', title: '' })
  } else {
    newTpl.value.weekendItems.push({ time: '12:00', title: '' })
  }
}

const removeItem = (type, idx) => {
  const items = type === 'weekday' ? newTpl.value.weekdayItems : newTpl.value.weekendItems
  if (items.length <= 1) {
    showToast('至少保留一项')
    return
  }
  items.splice(idx, 1)
}

const editItemTime = (type, idx) => {
  editingType.value = type
  editingIdx.value = idx
  const items = type === 'weekday' ? newTpl.value.weekdayItems : newTpl.value.weekendItems
  pickedTime.value = items[idx].time
  showTimePicker.value = true
}

const confirmTime = () => {
  if (editingIdx.value >= 0) {
    const items = editingType.value === 'weekday' ? newTpl.value.weekdayItems : newTpl.value.weekendItems
    items[editingIdx.value].time = pickedTime.value
  }
  showTimePicker.value = false
}

const createTemplate = () => {
  const weekdayItems = newTpl.value.weekdayItems.filter(i => i.title.trim())
  const weekendItems = newTpl.value.weekendItems.filter(i => i.title.trim())
  
  if (weekdayItems.length === 0 && weekendItems.length === 0) {
    showToast('请至少添加一个事项')
    return
  }
  
  weekdayItems.sort((a, b) => a.time.localeCompare(b.time))
  weekendItems.sort((a, b) => a.time.localeCompare(b.time))
  
  store.addCustomSchedule({
    name: newTpl.value.name,
    weekdayItems,
    weekendItems
  })
  
  showAdd.value = false
  newTpl.value = {
    name: '',
    weekdayItems: [{ time: '07:00', title: '' }, { time: '08:00', title: '' }],
    weekendItems: [{ time: '09:00', title: '' }, { time: '10:00', title: '' }]
  }
  showToast('创建成功')
}

const deleteTemplate = async (id) => {
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: '确定要删除这个模板吗？'
    })
    store.deleteCustomSchedule(id)
    showToast('删除成功')
  } catch (e) {}
}

const goToReview = () => {
  router.push('/review')
}
</script>

<style lang="scss" scoped>
.review-entry {
  @include card;
  padding: 14px 16px;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: linear-gradient(135deg, #fef3c7 0%, #fef9c3 100%);
  border: 1px solid #fde68a;
  
  &:active {
    transform: scale(0.98);
  }
}

.review-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  @include flex-center;
  font-size: 20px;
  background: rgba(245, 158, 11, 0.15);
  flex-shrink: 0;
}

.review-info {
  flex: 1;
}

.review-title {
  font-size: 14px;
  font-weight: 600;
  color: #92400e;
  margin-bottom: 2px;
}

.review-desc {
  font-size: 12px;
  color: #b45309;
}

.history-section {
  @include card;
  margin-bottom: 16px;
  overflow: hidden;
}

.history-header {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;

  &:active {
    background: #f9fafb;
  }
}

.history-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-icon {
  font-size: 20px;
}

.history-title {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.history-count {
  font-size: 11px;
  color: $text-secondary;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 10px;
}

.history-timeline {
  padding: 0 16px 16px;
}

.history-empty {
  text-align: center;
  padding: 24px;
  font-size: 13px;
  color: $text-secondary;
  background: #f9fafb;
  border-radius: 10px;
}

.history-item {
  display: flex;
  gap: 12px;
  min-height: 80px;

  &.active {
    .history-content {
      background: linear-gradient(135deg, #eff6ff 0%, #f0f9ff 100%);
      border: 1px solid #bfdbfe;
    }
  }
}

.history-dot-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 16px;
  flex-shrink: 0;
}

.history-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #d1d5db;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #d1d5db;
  flex-shrink: 0;
  margin-top: 6px;

  &.active {
    background: $primary-color;
    box-shadow: 0 0 0 2px $primary-color;
  }
}

.history-line {
  width: 2px;
  flex: 1;
  background: #e5e7eb;
  margin: 4px 0;
}

.history-content {
  flex: 1;
  padding: 10px 12px;
  background: #f9fafb;
  border-radius: 10px;
  margin-bottom: 8px;
}

.history-template-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.history-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: $text-secondary;
}

.history-stats {
  display: flex;
  gap: 8px;
}

.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 10px;
  font-weight: 500;

  &.duration {
    background: #f3f4f6;
    color: $text-secondary;
  }

  &.rate {
    &.rate-high {
      background: #ecfdf5;
      color: #059669;
    }
    &.rate-medium {
      background: #fffbeb;
      color: #d97706;
    }
    &.rate-low {
      background: #fef2f2;
      color: #dc2626;
    }
  }
}

.dual-view-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: linear-gradient(135deg, #eff6ff 0%, #f5f3ff 100%);
  border-radius: 12px;
  border: 1px solid #dbeafe;
}

.day-type-badge {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 8px;
  
  &.weekday {
    .badge-icon {
      font-size: 24px;
    }
    .badge-text {
      font-weight: 600;
      color: #1d4ed8;
      font-size: 13px;
    }
    .badge-days {
      font-size: 11px;
      color: #60a5fa;
    }
  }
  
  &.weekend {
    .badge-icon {
      font-size: 24px;
    }
    .badge-text {
      font-weight: 600;
      color: #9333ea;
      font-size: 13px;
    }
    .badge-days {
      font-size: 11px;
      color: #c084fc;
    }
  }
}

.day-type-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 24px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  color: #9ca3af;
}

.template-tabs {
  margin: 0 -16px 16px;
}

.template-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.template-card {
  @include card;
  padding: 16px;
  
  &.active {
    border: 2px solid $primary-color;
  }
  
  &:active {
    background: #f9fafb;
  }
}

.dual-column-card {
  .template-header {
    @include flex-between;
    margin-bottom: 12px;
  }
}

.template-name {
  font-size: 16px;
  font-weight: 600;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.template-actions {
  display: flex;
  gap: 12px;
  color: $text-secondary;
  flex-shrink: 0;
}

.dual-column-content {
  display: flex;
  gap: 8px;
}

.schedule-column {
  flex: 1;
  min-width: 0;
  
  &.weekday-column {
    .column-label {
      background: #eff6ff;
      color: #1d4ed8;
    }
    .item-time {
      color: #3b82f6;
    }
  }
  
  &.weekend-column {
    .column-label {
      background: #f5f3ff;
      color: #7c3aed;
    }
    .item-time {
      color: #8b5cf6;
    }
  }
}

.column-label {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 8px;
  
  .label-icon {
    font-size: 14px;
  }
  
  .item-count {
    margin-left: auto;
    font-weight: 500;
    font-size: 11px;
    opacity: 0.8;
  }
}

.column-separator {
  width: 1px;
  background: $border-color;
  margin: 0 4px;
}

.schedule-items {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.schedule-item {
  display: flex;
  gap: 6px;
  font-size: 12px;
  min-width: 0;
  
  .item-time {
    font-weight: 500;
    width: 42px;
    flex-shrink: 0;
  }
  
  .item-title {
    color: $text-secondary;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    flex: 1;
  }
}

.more-items {
  font-size: 11px;
  color: $text-secondary;
  padding-left: 48px;
}

.empty-items {
  font-size: 11px;
  color: #9ca3af;
  text-align: center;
  padding: 8px;
  background: #f9fafb;
  border-radius: 6px;
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  
  .empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }
  
  .empty-text {
    font-size: 14px;
    color: $text-secondary;
  }
}

.detail-wrapper {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.detail-header {
  @include flex-between;
  margin-bottom: 20px;
  
  h3 {
    font-size: 18px;
    font-weight: 600;
  }
}

.detail-dual-view {
  display: flex;
  gap: 16px;
  height: calc(100% - 60px);
}

.detail-column {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  
  &.weekday-detail {
    .detail-column-header {
      background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
      border: 1px solid #bfdbfe;
    }
    .timeline-dot {
      background: #3b82f6;
      box-shadow: 0 0 0 2px #3b82f6;
    }
    .timeline-content {
      background: #eff6ff;
    }
  }
  
  &.weekend-detail {
    .detail-column-header {
      background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);
      border: 1px solid #ddd6fe;
    }
    .timeline-dot {
      background: #8b5cf6;
      box-shadow: 0 0 0 2px #8b5cf6;
    }
    .timeline-content {
      background: #f5f3ff;
    }
  }
}

.detail-column-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 12px;
  border-radius: 12px;
  margin-bottom: 16px;
  
  .header-icon {
    font-size: 24px;
  }
  
  .header-title {
    font-size: 15px;
    font-weight: 600;
    color: $text-primary;
  }
  
  .header-subtitle {
    font-size: 11px;
    color: $text-secondary;
  }
}

.detail-timeline {
  position: relative;
  padding-left: 55px;
  overflow-y: auto;
  flex: 1;
  
  &::before {
    content: '';
    position: absolute;
    left: 35px;
    top: 8px;
    bottom: 8px;
    width: 2px;
    background: $border-color;
  }
}

.timeline-item {
  position: relative;
  padding-bottom: 16px;
  
  &:last-child {
    padding-bottom: 0;
  }
}

.timeline-time {
  position: absolute;
  left: -55px;
  top: -2px;
  width: 45px;
  text-align: right;
  font-size: 12px;
  font-weight: 500;
  color: $primary-color;
}

.timeline-dot {
  position: absolute;
  left: -28px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: $primary-color;
  border: 2px solid #fff;
}

.timeline-content {
  font-size: 13px;
  padding: 6px 10px;
  background: #f0f9ff;
  border-radius: 8px;
  min-width: 0;
  overflow-wrap: break-word;
  word-break: break-word;
}

.empty-timeline {
  text-align: center;
  padding: 30px 10px;
  font-size: 13px;
  color: $text-secondary;
  background: #f9fafb;
  border-radius: 12px;
}

.add-wrapper {
  padding: 20px;
  height: 100%;
  overflow-y: auto;
}

.form-header {
  @include flex-between;
  margin-bottom: 16px;
  
  h3 {
    font-size: 18px;
    font-weight: 600;
  }
}

.items-tabs {
  padding: 0 16px;
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

.items-section {
  padding: 0 16px;
  margin-top: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
  color: $text-secondary;
  display: flex;
  align-items: center;
  gap: 6px;
  
  .title-icon {
    font-size: 16px;
  }
}

.items-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 12px;
}

.item-row {
  display: flex;
  gap: 8px;
  align-items: center;
  
  .time-input {
    flex: 0 0 100px;
  }
  
  .title-input {
    flex: 1;
  }
  
  .remove-btn {
    color: $danger-color;
    padding: 8px;
  }
}

.form-actions {
  padding: 20px 16px 0;
}

.conflict-popup {
  background: transparent !important;
  box-shadow: none !important;
}

.conflict-dialog {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
}

.conflict-header {
  text-align: center;
  padding: 24px 20px 16px;
  background: linear-gradient(135deg, #fef3c7 0%, #fef9c3 100%);
}

.conflict-icon {
  font-size: 48px;
  margin-bottom: 8px;
}

.conflict-title {
  font-size: 18px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 4px;
}

.conflict-subtitle {
  font-size: 13px;
  color: #92400e;
}

.conflict-content {
  padding: 16px 20px;
  max-height: 280px;
  overflow-y: auto;
}

.conflict-section {
  margin-bottom: 16px;
  
  &:last-child {
    margin-bottom: 0;
  }
}

.conflict-section-title {
  margin-bottom: 10px;
}

.conflict-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.conflict-item {
  display: flex;
  gap: 10px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 10px;
  border-left: 3px solid $warning-color;
}

.conflict-item-time {
  font-size: 14px;
  font-weight: 600;
  color: $primary-color;
  flex-shrink: 0;
  width: 50px;
}

.conflict-item-info {
  flex: 1;
  min-width: 0;
}

.conflict-item-desc {
  font-size: 13px;
  color: $text-primary;
  margin-bottom: 4px;
  
  b {
    color: $danger-color;
  }
}

.conflict-item-habits {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.habit-tag {
  display: inline-block;
  padding: 2px 8px;
  background: #eff6ff;
  color: #1d4ed8;
  border-radius: 10px;
  font-size: 11px;
}

.more-habits {
  font-size: 11px;
  color: $text-secondary;
}

.conflict-item-suggestion {
  font-size: 12px;
  color: $text-secondary;
  
  b {
    color: #7c3aed;
  }
}

.conflict-summary {
  margin-top: 16px;
  padding: 12px;
  background: #fef2f2;
  border-radius: 10px;
  text-align: center;
  font-size: 13px;
  color: $text-primary;
}

.danger-text {
  color: $danger-color;
  font-size: 16px;
}

.conflict-actions {
  padding: 16px 20px;
  display: flex;
  gap: 10px;
  
  .van-button {
    flex: 1;
  }
}
</style>
