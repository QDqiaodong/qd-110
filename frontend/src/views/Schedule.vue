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
        class="template-card"
        :class="{ active: store.currentSchedule?.id === tpl.id }"
        @click="selectTemplate(tpl)"
      >
        <div class="template-header">
          <div class="template-name" :title="tpl.name">{{ tpl.name }}</div>
          <van-tag v-if="store.currentSchedule?.id === tpl.id" type="primary" round size="small">使用中</van-tag>
        </div>
        <div class="schedule-items">
          <div v-for="(item, idx) in tpl.items.slice(0, 4)" :key="idx" class="schedule-item">
            <span class="item-time">{{ item.time }}</span>
            <span class="item-title" :title="item.title">{{ item.title }}</span>
          </div>
          <div v-if="tpl.items.length > 4" class="more-items">
            还有 {{ tpl.items.length - 4 }} 项...
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
        class="template-card"
        :class="{ active: store.currentSchedule?.id === tpl.id }"
        @click="selectTemplate(tpl)"
      >
        <div class="template-header">
          <div class="template-name" :title="tpl.name">{{ tpl.name }}</div>
          <div class="template-actions">
            <van-icon name="delete-o" size="18" @click.stop="deleteTemplate(tpl.id)" />
          </div>
        </div>
        <div class="schedule-items">
          <div v-for="(item, idx) in tpl.items.slice(0, 4)" :key="idx" class="schedule-item">
            <span class="item-time">{{ item.time }}</span>
            <span class="item-title" :title="item.title">{{ item.title }}</span>
          </div>
        </div>
      </div>
    </div>

    <van-popup v-model:show="showDetail" round position="bottom" :style="{ height: '75%' }">
      <div class="detail-wrapper" v-if="store.currentSchedule">
        <div class="detail-header">
          <h3>{{ store.currentSchedule.name }}</h3>
          <van-icon name="cross" @click="showDetail = false" />
        </div>
        <div class="detail-timeline">
          <div
            v-for="(item, idx) in store.currentSchedule.items"
            :key="idx"
            class="timeline-item"
          >
            <div class="timeline-time">{{ item.time }}</div>
            <div class="timeline-dot"></div>
            <div class="timeline-content" :title="item.title">{{ item.title }}</div>
          </div>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showAdd" round position="bottom" :style="{ height: '80%' }">
      <div class="add-wrapper">
        <div class="form-header">
          <h3>创建作息模板</h3>
          <van-icon name="cross" @click="showAdd = false" />
        </div>
        <van-form @submit="createTemplate">
          <van-cell-group inset>
            <van-field v-model="newTpl.name" label="模板名称" placeholder="请输入模板名称" :rules="[{ required: true }]" />
          </van-cell-group>
          
          <div class="items-section">
            <div class="section-title">时间安排</div>
            <div class="items-list">
              <div v-for="(item, idx) in newTpl.items" :key="idx" class="item-row">
                <van-field
                  v-model="item.time"
                  placeholder="时间"
                  readonly
                  is-link
                  class="time-input"
                  @click="editItemTime(idx)"
                />
                <van-field v-model="item.title" placeholder="事项" class="title-input" />
                <van-icon name="minus" class="remove-btn" @click="removeItem(idx)" />
              </div>
            </div>
            <van-button block plain type="primary" icon="plus" size="small" @click="addItem">
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useHabitStore } from '@/store/habit'
import { showToast, showConfirmDialog } from 'vant'

const router = useRouter()
const store = useHabitStore()
const activeTab = ref('system')
const showAdd = ref(false)
const showDetail = ref(false)
const showTimePicker = ref(false)
const editingIdx = ref(-1)
const pickedTime = ref('')

const newTpl = ref({
  name: '',
  items: [
    { time: '07:00', title: '' },
    { time: '08:00', title: '' }
  ]
})

onMounted(() => {
  store.loadFromCache()
})

const selectTemplate = (tpl) => {
  store.setCurrentSchedule(tpl)
  showToast('已切换作息模板')
  showDetail.value = true
}

const addItem = () => {
  newTpl.value.items.push({ time: '12:00', title: '' })
}

const removeItem = (idx) => {
  if (newTpl.value.items.length <= 1) {
    showToast('至少保留一项')
    return
  }
  newTpl.value.items.splice(idx, 1)
}

const editItemTime = (idx) => {
  editingIdx.value = idx
  pickedTime.value = newTpl.value.items[idx].time
  showTimePicker.value = true
}

const confirmTime = () => {
  if (editingIdx.value >= 0) {
    newTpl.value.items[editingIdx.value].time = pickedTime.value
  }
  showTimePicker.value = false
}

const createTemplate = () => {
  const items = newTpl.value.items.filter(i => i.title.trim())
  if (items.length === 0) {
    showToast('请至少添加一个事项')
    return
  }
  items.sort((a, b) => a.time.localeCompare(b.time))
  store.addCustomSchedule({
    name: newTpl.value.name,
    items
  })
  showAdd.value = false
  newTpl.value = { name: '', items: [{ time: '07:00', title: '' }, { time: '08:00', title: '' }] }
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

.template-header {
  @include flex-between;
  margin-bottom: 12px;
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

.schedule-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schedule-item {
  display: flex;
  gap: 12px;
  font-size: 14px;
  min-width: 0;
  
  .item-time {
    color: $primary-color;
    font-weight: 500;
    width: 55px;
    flex-shrink: 0;
  }
  
  .item-title {
    color: $text-secondary;
    min-width: 0;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.more-items {
  font-size: 12px;
  color: $text-secondary;
  padding-left: 67px;
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

.detail-timeline {
  position: relative;
  padding-left: 70px;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    left: 50px;
    top: 8px;
    bottom: 8px;
    width: 2px;
    background: $border-color;
  }
}

.timeline-item {
  position: relative;
  padding-bottom: 20px;
  
  &:last-child {
    padding-bottom: 0;
  }
}

.timeline-time {
  position: absolute;
  left: -70px;
  top: -2px;
  width: 50px;
  text-align: right;
  font-size: 14px;
  font-weight: 500;
  color: $primary-color;
}

.timeline-dot {
  position: absolute;
  left: -24px;
  top: 4px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: $primary-color;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px $primary-color;
}

.timeline-content {
  font-size: 15px;
  padding: 8px 12px;
  background: #f0f9ff;
  border-radius: 8px;
  min-width: 0;
  overflow-wrap: break-word;
  word-break: break-word;
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

.items-section {
  padding: 0 16px;
  margin-top: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 12px;
  color: $text-secondary;
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
</style>
