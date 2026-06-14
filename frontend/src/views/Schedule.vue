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
          <div class="template-name-wrap">
            <div class="template-name" :title="tpl.name">{{ tpl.name }}</div>
            <van-tag v-if="tpl.tag" size="mini" class="template-tag" :type="getTagType(tpl.tag)">{{ tpl.tag }}</van-tag>
          </div>
          <div class="template-header-right">
            <van-tag v-if="store.currentSchedule?.id === tpl.id" type="primary" round size="small">使用中</van-tag>
            <van-dropdown-menu v-if="!tpl.isCustom" class="template-more" @change="(v) => handleSystemTemplateAction(tpl, v)">
              <van-dropdown-item :options="getSystemTemplateActions(tpl)" />
            </van-dropdown-menu>
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
          <div class="template-name-wrap">
            <div class="template-name" :title="tpl.name">{{ tpl.name }}</div>
            <div class="template-meta">
              <van-tag v-if="tpl.tag" size="mini" class="template-tag" :type="getTagType(tpl.tag)">{{ tpl.tag }}</van-tag>
              <span v-if="tpl.version" class="version-badge">v{{ tpl.version }}</span>
            </div>
          </div>
          <div class="template-actions">
            <van-icon name="copy-o" size="16" @click.stop="showCopyDialog(tpl)" />
            <van-icon name="edit" size="16" @click.stop="showRenameDialog(tpl)" />
            <van-icon name="clock-o" size="16" @click.stop="showVersionHistory(tpl)" />
            <van-icon name="delete-o" size="16" @click.stop="deleteTemplate(tpl.id)" />
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
          <van-icon name="cross" @click="closeAddPopup" />
        </div>
        <van-form @submit="createTemplate">
          <van-cell-group inset>
            <van-field v-model="newTpl.name" label="模板名称" placeholder="请输入模板名称" :rules="[{ required: true }]" />
          </van-cell-group>
          
          <div class="timeline-type-tabs">
            <van-radio-group v-model="activeItemTab" direction="horizontal" shape="round">
              <van-radio name="weekday">
                <span class="radio-text">📅 平日</span>
              </van-radio>
              <van-radio name="weekend">
                <span class="radio-text">🌴 周末</span>
              </van-radio>
            </van-radio-group>
          </div>

          <div class="timeline-editor-section">
            <div class="timeline-editor-header">
              <div class="editor-tip">
                <van-icon name="info-o" size="14" />
                <span>拖动节点调整时间 · 双击空白处新增 · 点击节点编辑</span>
              </div>
              <div class="editor-legend">
                <span class="legend-item wake"><i></i>起床</span>
                <span class="legend-item work"><i></i>工作</span>
                <span class="legend-item sport"><i></i>运动</span>
                <span class="legend-item rest"><i></i>休息</span>
                <span class="legend-item meal"><i></i>用餐</span>
                <span class="legend-item other"><i></i>其他</span>
              </div>
            </div>

            <div 
              class="timeline-editor"
              ref="timelineEditorRef"
              @dblclick="handleTimelineDblClick"
            >
              <div class="timeline-axis">
                <div 
                  v-for="hour in 24" 
                  :key="hour - 1" 
                  class="axis-hour"
                  :class="{ 'is-now': (hour - 1) === currentHour }"
                >
                  <div class="hour-label">{{ formatHourLabel(hour - 1) }}</div>
                  <div class="hour-line" :class="{ major: (hour - 1) % 6 === 0 }"></div>
                  <div class="half-hour-line"></div>
                </div>
              </div>

              <div class="timeline-content-area">
                <div 
                  v-for="(item, idx) in currentEditItems" 
                  :key="idx"
                  class="timeline-node"
                  :class="[getItemCategory(item.title), { dragging: draggingIdx === idx }]"
                  :style="getNodeStyle(idx)"
                  @mousedown="startDrag($event, idx)"
                  @touchstart="startDrag($event, idx)"
                  @click.stop="editNodeTitle(idx)"
                >
                  <div class="node-handle">
                    <van-icon name="wap-nav" size="12" />
                  </div>
                  <div class="node-info">
                    <div class="node-time">{{ item.time }}</div>
                    <div class="node-title" v-if="item.title">{{ item.title }}</div>
                    <div class="node-title placeholder" v-else>点击输入事项</div>
                  </div>
                  <div class="node-delete" @click.stop="deleteCurrentItem(idx)">
                    <van-icon name="cross" size="12" />
                  </div>
                </div>
              </div>
            </div>

            <div class="timeline-editor-footer">
              <van-button 
                block 
                plain 
                type="primary" 
                icon="plus" 
                size="small" 
                @click="quickAddItem"
              >
                快速添加事项
              </van-button>
            </div>
          </div>

          <div class="form-actions">
            <van-button round block type="primary" native-type="submit">创建模板</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="showTitleEditor" round position="bottom" :style="{ maxHeight: '40%' }">
      <div class="title-editor-wrapper">
        <div class="title-editor-header">
          <span class="editor-cancel" @click="showTitleEditor = false">取消</span>
          <span class="editor-title-label">编辑事项</span>
          <span class="editor-confirm" @click="confirmNodeTitle">确定</span>
        </div>
        <div class="title-editor-body">
          <div class="title-editor-row">
            <label>时间</label>
            <van-field
              v-model="editingNodeTime"
              readonly
              is-link
              @click="showNodeTimePicker = true"
            />
          </div>
          <div class="title-editor-row">
            <label>事项</label>
            <van-field
              v-model="editingNodeTitle"
              placeholder="请输入事项名称"
              maxlength="20"
            />
          </div>
          <div class="title-editor-categories">
            <div 
              v-for="cat in categoryOptions" 
              :key="cat.key"
              class="category-chip"
              :class="{ active: editingCategory === cat.key }"
              :style="{ '--cat-color': cat.color }"
              @click="applyCategoryPreset(cat)"
            >
              <span class="cat-icon">{{ cat.icon }}</span>
              <span class="cat-label">{{ cat.label }}</span>
            </div>
          </div>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showNodeTimePicker" round position="bottom">
      <van-time-picker
        v-model="pickedTime"
        title="选择时间"
        @confirm="confirmNodeTime"
        @cancel="showNodeTimePicker = false"
      />
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

    <van-popup v-model:show="showCopyPopup" round position="bottom" :style="{ maxHeight: '70%' }">
      <div class="copy-dialog">
        <div class="dialog-header">
          <h3>复制模板</h3>
          <van-icon name="cross" @click="closeCopyPopup" />
        </div>
        <div class="dialog-body">
          <div class="copy-source-info">
            <span class="source-label">源模板</span>
            <span class="source-name">{{ copyingTemplate?.name }}</span>
          </div>
          <van-cell-group inset>
            <van-field
              v-model="copyForm.name"
              label="新名称"
              placeholder="请输入新模板名称"
              :rules="[{ required: true, message: '请输入模板名称' }]"
            />
            <van-field
              v-model="copyForm.versionNote"
              label="版本说明"
              placeholder="可选：描述此版本的用途"
              maxlength="50"
              show-word-limit
            />
            <van-field
              v-model="copyForm.tag"
              label="标签"
              placeholder="选择或输入标签"
              is-link
              readonly
              @click="showTagPickerFor('copy')"
            />
          </van-cell-group>
          <div v-if="copyingTemplate?.versionNote" class="version-note-preview">
            <div class="note-label">源版本说明</div>
            <div class="note-content">{{ copyingTemplate.versionNote }}</div>
          </div>
        </div>
        <div class="dialog-footer">
          <van-button round block type="primary" @click="confirmCopy">创建副本</van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showRenamePopup" round position="bottom" :style="{ maxHeight: '60%' }">
      <div class="rename-dialog">
        <div class="dialog-header">
          <h3>编辑模板</h3>
          <van-icon name="cross" @click="closeRenamePopup" />
        </div>
        <div class="dialog-body">
          <van-cell-group inset>
            <van-field
              v-model="renameForm.name"
              label="名称"
              placeholder="请输入模板名称"
              :rules="[{ required: true, message: '请输入模板名称' }]"
            />
            <van-field
              v-model="renameForm.tag"
              label="标签"
              placeholder="选择或输入标签"
              is-link
              readonly
              @click="showTagPickerFor('rename')"
            />
            <van-field
              v-model="renameForm.versionNote"
              label="版本说明"
              placeholder="可选：描述此版本的用途"
              maxlength="50"
              show-word-limit
            />
          </van-cell-group>
        </div>
        <div class="dialog-footer">
          <van-button round block type="primary" @click="confirmRename">保存修改</van-button>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showVersionPopup" round position="bottom" :style="{ height: '80%' }">
      <div class="version-history-dialog">
        <div class="dialog-header">
          <h3>版本历史</h3>
          <van-icon name="cross" @click="closeVersionPopup" />
        </div>
        <div class="dialog-body">
          <div v-if="versionHistory.length === 0" class="empty-history">
            <div class="empty-icon">📋</div>
            <div class="empty-text">暂无版本记录</div>
            <div class="empty-desc">复制模板后会在此处显示版本历史</div>
          </div>
          <div v-else class="version-tree">
            <div
              v-for="(item, idx) in versionHistory"
              :key="item.id"
              class="version-item"
              :class="{ current: store.currentSchedule?.id === item.id }"
            >
              <div class="version-item-left">
                <div class="version-dot" :class="{ root: !item.parentId }"></div>
                <div v-if="idx < versionHistory.length - 1" class="version-line"></div>
              </div>
              <div class="version-item-content">
                <div class="version-item-header">
                  <span class="version-name">{{ item.name }}</span>
                  <van-tag v-if="item.tag" size="mini" :type="getTagType(item.tag)">{{ item.tag }}</van-tag>
                  <van-tag v-if="store.currentSchedule?.id === item.id" type="primary" size="mini">当前</van-tag>
                </div>
                <div class="version-item-meta">
                  <span class="meta-item">
                    <van-icon name="clock-o" size="11" />
                    {{ item.createTime || '未知时间' }}
                  </span>
                  <span v-if="item.version" class="meta-item">
                    <van-icon name="flag-o" size="11" />
                    v{{ item.version }}
                  </span>
                </div>
                <div v-if="item.versionNote" class="version-item-note">
                  {{ item.versionNote }}
                </div>
                <div class="version-item-actions">
                  <van-button size="mini" type="primary" plain @click="selectTemplate(item)">
                    使用此版本
                  </van-button>
                  <van-button size="mini" @click.stop="showCopyDialog(item)">
                    复制
                  </van-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="showTagPicker" round position="bottom">
      <div class="tag-picker-dialog">
        <div class="dialog-header">
          <h3>选择标签</h3>
          <van-icon name="cross" @click="showTagPicker = false" />
        </div>
        <div class="dialog-body">
          <div class="tag-options">
            <div
              v-for="tag in store.scheduleTags"
              :key="tag"
              class="tag-chip"
              :class="{ active: tagFormValue === tag }"
              :style="{ '--tag-color': getTagColor(tag) }"
              @click="selectTag(tag)"
            >
              {{ tag }}
            </div>
          </div>
          <van-field
            v-model="customTag"
            placeholder="或输入自定义标签"
            maxlength="8"
            class="custom-tag-input"
          />
        </div>
        <div class="dialog-footer">
          <van-button round block type="primary" @click="confirmCustomTag">确定</van-button>
        </div>
      </div>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useHabitStore } from '@/store/habit'
import { showToast, showConfirmDialog } from 'vant'
import dayjs from 'dayjs'

const HOUR_HEIGHT = 56
const MINUTE_HEIGHT = HOUR_HEIGHT / 60
const TIMELINE_TOP_OFFSET = 8
const DRAG_THRESHOLD = 5

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
const showCopyPopup = ref(false)
const showRenamePopup = ref(false)
const showVersionPopup = ref(false)
const showTagPicker = ref(false)
const copyingTemplate = ref(null)
const renamingTemplate = ref(null)
const versionHistoryTemplate = ref(null)
const versionHistory = ref([])
const tagFormValue = ref('')
const customTag = ref('')
const tagFormTarget = ref('')
const copyForm = ref({
  name: '',
  tag: '',
  versionNote: ''
})
const renameForm = ref({
  name: '',
  tag: '',
  versionNote: ''
})

const currentHour = computed(() => dayjs().hour())
const timelineEditorRef = ref(null)
const showTitleEditor = ref(false)
const showNodeTimePicker = ref(false)
const draggingIdx = ref(-1)
const dragStartY = ref(0)
const dragStartTop = ref(0)
const dragStartMinute = ref(0)
const isDragging = ref(false)
const editingNodeIdx = ref(-1)
const editingNodeTime = ref('')
const editingNodeTitle = ref('')
const editingCategory = ref('other')

const categoryOptions = [
  { key: 'wake', label: '起床', icon: '🌅', color: '#f59e0b', keywords: ['起床', '洗漱', '醒', '睡', '入眠'] },
  { key: 'work', label: '工作', icon: '💼', color: '#3b82f6', keywords: ['工作', '学习', '课', '办公', '会议', '项目', '任务', '作业', '复习', '早读', '晚自', '晚自习'] },
  { key: 'sport', label: '运动', icon: '🏃', color: '#10b981', keywords: ['运动', '跑', '健身', '训练', '锻炼', '晨练', '瑜伽', '拉伸', '散步', '户外', '游泳'] },
  { key: 'rest', label: '休息', icon: '😴', color: '#8b5cf6', keywords: ['休息', '午休', '自由', '休闲', '娱乐', '放松', '阅读', '兴趣', '爱好', '游戏', '刷'] },
  { key: 'meal', label: '用餐', icon: '🍽️', color: '#ef4444', keywords: ['早餐', '午餐', '晚餐', '饭', '用餐', '吃', '餐', '夜宵'] },
  { key: 'other', label: '其他', icon: '📌', color: '#6b7280', keywords: [] }
]

const newTpl = ref({
  name: '',
  weekdayItems: [
    { time: '07:00', title: '起床洗漱' },
    { time: '08:00', title: '早餐' },
    { time: '09:00', title: '开始工作' },
    { time: '12:00', title: '午餐' },
    { time: '14:00', title: '下午工作' },
    { time: '18:00', title: '晚餐' },
    { time: '22:00', title: '睡觉' }
  ],
  weekendItems: [
    { time: '09:00', title: '起床' },
    { time: '10:00', title: '早餐' },
    { time: '12:30', title: '午餐' },
    { time: '14:00', title: '午休' },
    { time: '18:30', title: '晚餐' },
    { time: '23:00', title: '睡觉' }
  ]
})

const historyList = computed(() => store.templateHistoryWithStats || [])

const currentEditItems = computed({
  get() {
    const items = activeItemTab.value === 'weekday'
      ? newTpl.value.weekdayItems
      : newTpl.value.weekendItems
    return [...items].sort((a, b) => a.time.localeCompare(b.time))
  },
  set(val) {
    if (activeItemTab.value === 'weekday') {
      newTpl.value.weekdayItems = val
    } else {
      newTpl.value.weekendItems = val
    }
  }
})

watch(activeItemTab, () => {
  draggingIdx.value = -1
  isDragging.value = false
})

onMounted(() => {
  store.loadFromCache()
  document.addEventListener('mousemove', handleDragMove)
  document.addEventListener('mouseup', handleDragEnd)
  document.addEventListener('touchmove', handleDragMove, { passive: false })
  document.addEventListener('touchend', handleDragEnd)
})

onUnmounted(() => {
  document.removeEventListener('mousemove', handleDragMove)
  document.removeEventListener('mouseup', handleDragEnd)
  document.removeEventListener('touchmove', handleDragMove)
  document.removeEventListener('touchend', handleDragEnd)
})

const formatHourLabel = (hour) => {
  return `${String(hour).padStart(2, '0')}:00`
}

const timeToMinutes = (timeStr) => {
  if (!timeStr) return 0
  const [h, m] = timeStr.split(':').map(n => parseInt(n, 10))
  return h * 60 + (m || 0)
}

const minutesToTime = (minutes) => {
  const m = Math.max(0, Math.min(1439, Math.round(minutes)))
  const h = Math.floor(m / 60)
  const mm = m % 60
  return `${String(h).padStart(2, '0')}:${String(mm).padStart(2, '0')}`
}

const getItemCategory = (title) => {
  if (!title) return 'other'
  const t = title.toLowerCase()
  for (let i = 0; i < categoryOptions.length - 1; i++) {
    const cat = categoryOptions[i]
    if (cat.keywords.some(kw => t.includes(kw))) {
      return cat.key
    }
  }
  return 'other'
}

const getCategoryColor = (key) => {
  const cat = categoryOptions.find(c => c.key === key)
  return cat ? cat.color : '#6b7280'
}

const getNodeStyle = (idx) => {
  const items = currentEditItems.value
  if (!items[idx]) return {}
  const minutes = timeToMinutes(items[idx].time)
  const top = TIMELINE_TOP_OFFSET + minutes * MINUTE_HEIGHT
  return {
    top: `${top}px`,
    '--cat-color': getCategoryColor(getItemCategory(items[idx].title))
  }
}

const formatHistoryDate = (dateStr) => {
  if (!dateStr) return ''
  return dayjs(dateStr).format('MM/DD HH:mm')
}

const getRateClass = (rate) => {
  if (rate >= 80) return 'rate-high'
  if (rate >= 50) return 'rate-medium'
  return 'rate-low'
}

const getWeekdayItems = (tpl) => {
  return tpl.weekdayItems || tpl.items || []
}

const getWeekendItems = (tpl) => {
  return tpl.weekendItems || tpl.items || []
}

const getEventY = (e) => {
  if (e.touches && e.touches.length > 0) {
    return e.touches[0].clientY
  }
  if (e.changedTouches && e.changedTouches.length > 0) {
    return e.changedTouches[0].clientY
  }
  return e.clientY
}

const startDrag = (e, idx) => {
  if (e.target.closest('.node-delete') || e.target.closest('.node-info')) {
    if (!e.target.closest('.node-handle')) return
  }
  e.preventDefault()
  draggingIdx.value = idx
  isDragging.value = false
  dragStartY.value = getEventY(e)
  const items = currentEditItems.value
  dragStartMinute.value = timeToMinutes(items[idx].time)
  dragStartTop.value = TIMELINE_TOP_OFFSET + dragStartMinute.value * MINUTE_HEIGHT
}

const handleDragMove = (e) => {
  if (draggingIdx.value < 0) return
  const currentY = getEventY(e)
  const deltaY = currentY - dragStartY.value
  if (!isDragging.value && Math.abs(deltaY) < DRAG_THRESHOLD) return
  if (e.cancelable) e.preventDefault()
  isDragging.value = true
  const deltaMinutes = deltaY / MINUTE_HEIGHT
  const newMinutes = dragStartMinute.value + deltaMinutes
  const snappedMinutes = Math.round(newMinutes / 5) * 5
  const clampedMinutes = Math.max(0, Math.min(1439, snappedMinutes))
  const newTime = minutesToTime(clampedMinutes)
  const items = activeItemTab.value === 'weekday'
    ? newTpl.value.weekdayItems
    : newTpl.value.weekendItems
  const sourceItems = activeItemTab.value === 'weekday'
    ? newTpl.value.weekdayItems
    : newTpl.value.weekendItems
  const sorted = [...sourceItems].sort((a, b) => a.time.localeCompare(b.time))
  const originalItem = sorted[draggingIdx.value]
  const originalIdxInSource = sourceItems.indexOf(originalItem)
  if (originalIdxInSource >= 0) {
    sourceItems[originalIdxInSource].time = newTime
  }
}

const handleDragEnd = () => {
  draggingIdx.value = -1
  isDragging.value = false
}

const getTimelineClickMinutes = (e) => {
  if (!timelineEditorRef.value) return null
  const rect = timelineEditorRef.value.getBoundingClientRect()
  const clickY = getEventY(e) - rect.top - TIMELINE_TOP_OFFSET
  if (clickY < 0) return 0
  const minutes = clickY / MINUTE_HEIGHT
  const snapped = Math.round(minutes / 5) * 5
  return Math.max(0, Math.min(1439, snapped))
}

const handleTimelineDblClick = (e) => {
  if (e.target.closest('.timeline-node')) return
  const minutes = getTimelineClickMinutes(e)
  if (minutes === null) return
  const newTime = minutesToTime(minutes)
  const items = activeItemTab.value === 'weekday'
    ? newTpl.value.weekdayItems
    : newTpl.value.weekendItems
  items.push({ time: newTime, title: '' })
  const newIdx = items.length - 1
  nextTick(() => {
    editNodeTitleBySourceIdx(newIdx)
  })
}

const quickAddItem = () => {
  const items = activeItemTab.value === 'weekday'
    ? newTpl.value.weekdayItems
    : newTpl.value.weekendItems
  let newMinutes = 12 * 60
  if (items.length > 0) {
    const sorted = [...items].sort((a, b) => a.time.localeCompare(b.time))
    const lastMinutes = timeToMinutes(sorted[sorted.length - 1].time)
    newMinutes = Math.min(1439, lastMinutes + 60)
  }
  items.push({ time: minutesToTime(newMinutes), title: '' })
  const newIdx = items.length - 1
  nextTick(() => {
    editNodeTitleBySourceIdx(newIdx)
  })
}

const editNodeTitle = (sortedIdx) => {
  if (isDragging.value) return
  nextTick(() => {
    const sourceItems = activeItemTab.value === 'weekday'
      ? newTpl.value.weekdayItems
      : newTpl.value.weekendItems
    const sorted = [...sourceItems].sort((a, b) => a.time.localeCompare(b.time))
    const item = sorted[sortedIdx]
    const sourceIdx = sourceItems.indexOf(item)
    if (sourceIdx >= 0) {
      editNodeTitleBySourceIdx(sourceIdx)
    }
  })
}

const editNodeTitleBySourceIdx = (sourceIdx) => {
  const items = activeItemTab.value === 'weekday'
    ? newTpl.value.weekdayItems
    : newTpl.value.weekendItems
  editingNodeIdx.value = sourceIdx
  editingNodeTime.value = items[sourceIdx].time
  editingNodeTitle.value = items[sourceIdx].title
  editingCategory.value = getItemCategory(items[sourceIdx].title)
  showTitleEditor.value = true
}

const applyCategoryPreset = (cat) => {
  editingCategory.value = cat.key
  if (!editingNodeTitle.value || editingNodeTitle.value.trim() === '') {
    if (cat.key === 'wake') editingNodeTitle.value = '起床'
    else if (cat.key === 'work') editingNodeTitle.value = '工作'
    else if (cat.key === 'sport') editingNodeTitle.value = '运动'
    else if (cat.key === 'rest') editingNodeTitle.value = '休息'
    else if (cat.key === 'meal') editingNodeTitle.value = '用餐'
  }
}

const confirmNodeTitle = () => {
  if (editingNodeIdx.value < 0) return
  const items = activeItemTab.value === 'weekday'
    ? newTpl.value.weekdayItems
    : newTpl.value.weekendItems
  if (!editingNodeTitle.value.trim()) {
    showToast('请输入事项名称')
    return
  }
  items[editingNodeIdx.value].title = editingNodeTitle.value.trim()
  items[editingNodeIdx.value].time = editingNodeTime.value
  showTitleEditor.value = false
  editingNodeIdx.value = -1
}

const confirmNodeTime = () => {
  editingNodeTime.value = pickedTime.value
  showNodeTimePicker.value = false
}

const deleteCurrentItem = (sortedIdx) => {
  const sourceItems = activeItemTab.value === 'weekday'
    ? newTpl.value.weekdayItems
    : newTpl.value.weekendItems
  if (sourceItems.length <= 1) {
    showToast('至少保留一项')
    return
  }
  const sorted = [...sourceItems].sort((a, b) => a.time.localeCompare(b.time))
  const item = sorted[sortedIdx]
  const sourceIdx = sourceItems.indexOf(item)
  if (sourceIdx >= 0) {
    sourceItems.splice(sourceIdx, 1)
  }
}

const closeAddPopup = () => {
  showAdd.value = false
  showTitleEditor.value = false
  showNodeTimePicker.value = false
  draggingIdx.value = -1
  isDragging.value = false
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
  
  closeAddPopup()
  newTpl.value = {
    name: '',
    weekdayItems: [
      { time: '07:00', title: '起床洗漱' },
      { time: '08:00', title: '早餐' },
      { time: '09:00', title: '开始工作' },
      { time: '12:00', title: '午餐' },
      { time: '14:00', title: '下午工作' },
      { time: '18:00', title: '晚餐' },
      { time: '22:00', title: '睡觉' }
    ],
    weekendItems: [
      { time: '09:00', title: '起床' },
      { time: '10:00', title: '早餐' },
      { time: '12:30', title: '午餐' },
      { time: '14:00', title: '午休' },
      { time: '18:30', title: '晚餐' },
      { time: '23:00', title: '睡觉' }
    ]
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

const tagColors = {
  '上学日': '#3b82f6',
  '备考期': '#ef4444',
  '健身期': '#10b981',
  '工作期': '#f59e0b',
  '假期': '#8b5cf6',
  '其他': '#6b7280'
}

const getTagColor = (tag) => {
  return tagColors[tag] || '#6b7280'
}

const getTagType = (tag) => {
  const typeMap = {
    '上学日': 'primary',
    '备考期': 'danger',
    '健身期': 'success',
    '工作期': 'warning',
    '假期': 'primary',
    '其他': 'default'
  }
  return typeMap[tag] || 'default'
}

const getSystemTemplateActions = (tpl) => {
  return [
    { text: '复制模板', value: 'copy' },
    { text: '查看版本', value: 'versions' }
  ]
}

const handleSystemTemplateAction = (tpl, value) => {
  if (value === 'copy') {
    showCopyDialog(tpl)
  } else if (value === 'versions') {
    showVersionHistory(tpl)
  }
}

const showCopyDialog = (tpl) => {
  copyingTemplate.value = tpl
  copyForm.value = {
    name: `${tpl.name} v${(tpl.version || 1) + 1}`,
    tag: tpl.tag || '其他',
    versionNote: ''
  }
  showCopyPopup.value = true
}

const closeCopyPopup = () => {
  showCopyPopup.value = false
  copyingTemplate.value = null
}

const confirmCopy = async () => {
  if (!copyForm.value.name.trim()) {
    showToast('请输入模板名称')
    return
  }
  const newTemplate = await store.copySchedule(copyingTemplate.value, {
    name: copyForm.value.name.trim(),
    tag: copyForm.value.tag,
    versionNote: copyForm.value.versionNote.trim()
  })
  closeCopyPopup()
  showToast(`已复制为「${newTemplate.name}」`)
  activeTab.value = 'custom'
}

const showRenameDialog = (tpl) => {
  renamingTemplate.value = tpl
  renameForm.value = {
    name: tpl.name,
    tag: tpl.tag || '其他',
    versionNote: tpl.versionNote || ''
  }
  showRenamePopup.value = true
}

const closeRenamePopup = () => {
  showRenamePopup.value = false
  renamingTemplate.value = null
}

const confirmRename = () => {
  if (!renameForm.value.name.trim()) {
    showToast('请输入模板名称')
    return
  }
  const tpl = renamingTemplate.value
  if (tpl) {
    store.renameSchedule(tpl.id, renameForm.value.name.trim())
    store.updateScheduleTag(tpl.id, renameForm.value.tag)
    if (tpl.isCustom) {
      store.updateScheduleVersionNote(tpl.id, renameForm.value.versionNote.trim())
    }
    showToast('保存成功')
  }
  closeRenamePopup()
}

const showVersionHistory = (tpl) => {
  versionHistoryTemplate.value = tpl
  const allTemplates = [...store.templates, ...store.schedules]
  const collectVersions = (id, collected = new Set()) => {
    if (collected.has(id)) return []
    collected.add(id)
    const template = allTemplates.find(t => t.id === id)
    if (!template) return []
    const result = [template]
    const children = allTemplates.filter(t => t.parentId === id)
    children.forEach(child => {
      result.push(...collectVersions(child.id, collected))
    })
    return result
  }
  
  let versions = collectVersions(tpl.id)
  const rootId = tpl.parentId || tpl.id
  if (tpl.parentId) {
    const rootVersions = collectVersions(rootId, new Set())
    versions = [...new Map(rootVersions.map(v => [v.id, v])).values()]
  }
  
  versionHistory.value = versions.sort((a, b) => {
    if (!a.parentId) return -1
    if (!b.parentId) return 1
    if (a.parentId === rootId && b.parentId !== rootId) return -1
    if (b.parentId === rootId && a.parentId !== rootId) return 1
    return (new Date(b.createTime || 0)) - (new Date(a.createTime || 0))
  })
  
  showVersionPopup.value = true
}

const closeVersionPopup = () => {
  showVersionPopup.value = false
  versionHistoryTemplate.value = null
  versionHistory.value = []
}

const selectTag = (tag) => {
  tagFormValue.value = tag
  customTag.value = ''
}

const confirmCustomTag = () => {
  const finalTag = customTag.value.trim() || tagFormValue.value
  if (!finalTag) {
    showToast('请选择或输入标签')
    return
  }
  if (tagFormTarget.value === 'copy') {
    copyForm.value.tag = finalTag
  } else if (tagFormTarget.value === 'rename') {
    renameForm.value.tag = finalTag
  }
  showTagPicker.value = false
}

const showTagPickerFor = (target) => {
  tagFormTarget.value = target
  if (target === 'copy') {
    tagFormValue.value = copyForm.value.tag
  } else if (target === 'rename') {
    tagFormValue.value = renameForm.value.tag
  }
  customTag.value = ''
  showTagPicker.value = true
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

.timeline-type-tabs {
  padding: 0 16px;
  margin-bottom: 12px;
  
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

.timeline-editor-section {
  padding: 0 4px;
  margin-top: 8px;
}

.timeline-editor-header {
  padding: 0 12px 10px;
  border-bottom: 1px solid $border-color;
  margin-bottom: 10px;
}

.editor-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: $text-secondary;
  margin-bottom: 10px;
  opacity: 0.85;
}

.editor-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  
  .legend-item {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 11px;
    color: $text-secondary;
    font-weight: 500;
    
    i {
      display: inline-block;
      width: 10px;
      height: 10px;
      border-radius: 3px;
    }
    
    &.wake i { background: #f59e0b; }
    &.work i { background: #3b82f6; }
    &.sport i { background: #10b981; }
    &.rest i { background: #8b5cf6; }
    &.meal i { background: #ef4444; }
    &.other i { background: #6b7280; }
  }
}

.timeline-editor {
  position: relative;
  height: 560px;
  overflow-y: auto;
  overflow-x: hidden;
  background: linear-gradient(180deg, #fafbff 0%, #ffffff 50%, #fffbf5 100%);
  border-radius: 12px;
  border: 1px solid $border-color;
  display: flex;
  user-select: none;
  -webkit-user-select: none;
  touch-action: pan-y;
}

.timeline-axis {
  width: 56px;
  flex-shrink: 0;
  position: relative;
  background: #f9fafb;
  border-right: 1px solid $border-color;
}

.axis-hour {
  height: 56px;
  position: relative;
  
  .hour-label {
    position: absolute;
    top: -7px;
    left: 0;
    right: 4px;
    font-size: 10px;
    color: $text-secondary;
    font-weight: 500;
    text-align: right;
    line-height: 1;
    padding-top: 3px;
    padding-right: 6px;
  }
  
  .hour-line {
    position: absolute;
    top: 0;
    left: 52px;
    right: -100vw;
    height: 1px;
    background: $border-color;
    
    &.major {
      background: #d1d5db;
      height: 1.5px;
    }
  }
  
  .half-hour-line {
    position: absolute;
    top: 28px;
    left: 52px;
    right: -100vw;
    height: 1px;
    background: #f3f4f6;
  }
  
  &.is-now {
    .hour-label {
      color: $primary-color;
      font-weight: 700;
    }
    .hour-line {
      background: rgba(59, 130, 246, 0.5);
      height: 2px;
    }
  }
}

.timeline-content-area {
  flex: 1;
  position: relative;
  min-width: 0;
  padding-right: 8px;
  padding-left: 4px;
}

.timeline-node {
  position: absolute;
  left: 4px;
  right: 8px;
  display: flex;
  align-items: stretch;
  gap: 4px;
  background: #fff;
  border: 1.5px solid var(--cat-color, #6b7280);
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 0 0 3px color-mix(in srgb, var(--cat-color, #6b7280) 10%, transparent);
  min-height: 44px;
  z-index: 2;
  cursor: grab;
  transition: box-shadow 0.2s ease, transform 0.15s ease;
  
  &:before {
    content: '';
    position: absolute;
    left: -6px;
    top: 50%;
    transform: translateY(-50%);
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--cat-color, #6b7280);
    border: 2px solid #fff;
    box-shadow: 0 0 0 2px var(--cat-color, #6b7280);
    z-index: 3;
  }
  
  &:active {
    cursor: grabbing;
  }
  
  &.dragging {
    z-index: 10;
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15), 0 0 0 4px color-mix(in srgb, var(--cat-color, #6b7280) 20%, transparent);
    transform: scale(1.02);
    opacity: 0.95;
  }
  
  &.wake { --cat-color: #f59e0b; }
  &.work { --cat-color: #3b82f6; }
  &.sport { --cat-color: #10b981; }
  &.rest { --cat-color: #8b5cf6; }
  &.meal { --cat-color: #ef4444; }
  &.other { --cat-color: #6b7280; }
}

.node-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  flex-shrink: 0;
  background: color-mix(in srgb, var(--cat-color, #6b7280) 12%, transparent);
  color: var(--cat-color, #6b7280);
  cursor: grab;
  border-right: 1px solid color-mix(in srgb, var(--cat-color, #6b7280) 15%, transparent);
  border-radius: 8px 0 0 8px;
  
  &:active {
    cursor: grabbing;
  }
}

.node-info {
  flex: 1;
  min-width: 0;
  padding: 5px 8px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 1px;
  cursor: pointer;
}

.node-time {
  font-size: 11px;
  font-weight: 700;
  color: var(--cat-color, #6b7280);
  line-height: 1.2;
  letter-spacing: 0.2px;
}

.node-title {
  font-size: 12px;
  color: $text-primary;
  font-weight: 500;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  
  &.placeholder {
    color: #9ca3af;
    font-style: italic;
    font-weight: 400;
  }
}

.node-delete {
  width: 24px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #d1d5db;
  cursor: pointer;
  border-radius: 0 8px 8px 0;
  transition: all 0.2s ease;
  
  &:hover, &:active {
    background: #fef2f2;
    color: $danger-color;
  }
}

.timeline-editor-footer {
  padding: 12px 4px 0;
}

.title-editor-wrapper {
  padding: 0;
}

.title-editor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid $border-color;
  
  .editor-cancel {
    font-size: 14px;
    color: $text-secondary;
    cursor: pointer;
  }
  
  .editor-title-label {
    font-size: 16px;
    font-weight: 600;
    color: $text-primary;
  }
  
  .editor-confirm {
    font-size: 14px;
    font-weight: 600;
    color: $primary-color;
    cursor: pointer;
  }
}

.title-editor-body {
  padding: 16px 20px 24px;
}

.title-editor-row {
  margin-bottom: 12px;
  
  label {
    display: block;
    font-size: 12px;
    color: $text-secondary;
    margin-bottom: 6px;
    padding: 0 4px;
    font-weight: 500;
  }
  
  :deep(.van-field) {
    background: #f9fafb;
    border-radius: 10px;
  }
}

.title-editor-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px dashed $border-color;
}

.category-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 7px 12px;
  border-radius: 20px;
  background: #f9fafb;
  border: 1.5px solid $border-color;
  cursor: pointer;
  transition: all 0.2s ease;
  
  .cat-icon {
    font-size: 14px;
    line-height: 1;
  }
  
  .cat-label {
    font-size: 12px;
    color: $text-secondary;
    font-weight: 500;
  }
  
  &.active {
    background: color-mix(in srgb, var(--cat-color, $primary-color) 12%, #fff);
    border-color: var(--cat-color, $primary-color);
    box-shadow: 0 0 0 3px color-mix(in srgb, var(--cat-color, $primary-color) 10%, transparent);
    
    .cat-label {
      color: var(--cat-color, $primary-color);
      font-weight: 600;
    }
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

.template-name-wrap {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  flex: 1;
}

.template-header-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.template-tag {
  flex-shrink: 0;
}

.template-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.version-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 10px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 1px 6px;
  border-radius: 8px;
  font-weight: 500;
}

.template-actions {
  display: flex;
  gap: 14px;
  color: $text-secondary;
  flex-shrink: 0;
  align-items: center;

  .van-icon {
    cursor: pointer;
    transition: color 0.2s ease;

    &:hover, &:active {
      color: $primary-color;
    }
  }
}

.template-more {
  width: auto;
  min-width: 0;
  background: transparent;
  border: none;

  :deep(.van-dropdown-menu__bar) {
    background: transparent;
    height: auto;
    box-shadow: none;
  }

  :deep(.van-dropdown-item__title) {
    color: $text-secondary;
    font-size: 16px;
    padding: 0;
  }
}

.copy-dialog,
.rename-dialog,
.version-history-dialog,
.tag-picker-dialog {
  padding: 0;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid $border-color;

  h3 {
    font-size: 16px;
    font-weight: 600;
    margin: 0;
  }

  .van-icon {
    font-size: 20px;
    color: $text-secondary;
    cursor: pointer;
  }
}

.dialog-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
}

.dialog-footer {
  padding: 16px 20px;
  border-top: 1px solid $border-color;
}

.copy-source-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  margin: 0 16px 16px;
  background: #f9fafb;
  border-radius: 10px;

  .source-label {
    font-size: 13px;
    color: $text-secondary;
  }

  .source-name {
    font-size: 13px;
    font-weight: 600;
    color: $primary-color;
  }
}

.version-note-preview {
  margin: 16px;
  padding: 12px;
  background: #fef3c7;
  border-radius: 8px;
  border-left: 3px solid #f59e0b;

  .note-label {
    font-size: 11px;
    color: #92400e;
    margin-bottom: 4px;
    font-weight: 500;
  }

  .note-content {
    font-size: 12px;
    color: #78350f;
    line-height: 1.5;
  }
}

.empty-history {
  text-align: center;
  padding: 60px 20px;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 12px;
  }

  .empty-text {
    font-size: 14px;
    color: $text-primary;
    margin-bottom: 4px;
  }

  .empty-desc {
    font-size: 12px;
    color: $text-secondary;
  }
}

.version-tree {
  padding: 0 16px;
}

.version-item {
  display: flex;
  gap: 12px;

  &.current {
    .version-item-content {
      background: linear-gradient(135deg, #eff6ff 0%, #f0f9ff 100%);
      border: 1px solid #bfdbfe;
    }
  }
}

.version-item-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 16px;
  flex-shrink: 0;
}

.version-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #d1d5db;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #d1d5db;
  flex-shrink: 0;
  margin-top: 6px;

  &.root {
    background: $primary-color;
    box-shadow: 0 0 0 2px $primary-color;
  }
}

.version-line {
  width: 2px;
  flex: 1;
  background: #e5e7eb;
  margin: 4px 0;
  min-height: 20px;
}

.version-item-content {
  flex: 1;
  padding: 12px;
  background: #f9fafb;
  border-radius: 10px;
  margin-bottom: 8px;
}

.version-item-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.version-name {
  font-size: 14px;
  font-weight: 600;
  color: $text-primary;
}

.version-item-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;

  .meta-item {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 11px;
    color: $text-secondary;
  }
}

.version-item-note {
  font-size: 12px;
  color: #6b7280;
  background: #fff;
  padding: 8px 10px;
  border-radius: 6px;
  margin-bottom: 10px;
  line-height: 1.5;
}

.version-item-actions {
  display: flex;
  gap: 8px;
}

.tag-options {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 0 20px 16px;
}

.tag-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 6px 14px;
  font-size: 13px;
  color: $text-secondary;
  background: #f9fafb;
  border: 1.5px solid $border-color;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;

  &.active {
    background: color-mix(in srgb, var(--tag-color, $primary-color) 12%, #fff);
    border-color: var(--tag-color, $primary-color);
    color: var(--tag-color, $primary-color);
    font-weight: 500;
  }

  &:active {
    transform: scale(0.96);
  }
}

.custom-tag-input {
  padding: 0 20px;
}
</style>
