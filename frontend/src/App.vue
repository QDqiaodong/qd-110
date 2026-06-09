<template>
  <div class="app-container">
    <router-view v-slot="{ Component }">
      <transition name="fade" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
    <van-tabbar v-model="active" route active-color="#3b82f6">
      <van-tabbar-item icon="todo-list" to="/">今日打卡</van-tabbar-item>
      <van-tabbar-item icon="orders-o" to="/habits">习惯清单</van-tabbar-item>
      <van-tabbar-item icon="schedule" to="/schedule">作息模板</van-tabbar-item>
      <van-tabbar-item icon="bar-chart-o" to="/stats">数据统计</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
const active = ref(0)
</script>

<style lang="scss" scoped>
.app-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  padding-bottom: 50px;
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
