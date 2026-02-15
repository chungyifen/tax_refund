<template>
  <div class="dashboard-container">
    <div class="dashboard-text">name: {{ name }}</div>
    <div class="dashboard-text">roles: <span v-for="role in roles" :key="role">{{ role }} </span></div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const name = computed(() => userStore.name)
const roles = computed(() => userStore.roles)
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 30px;
  .dashboard-text {
    font-size: 30px;
    line-height: 46px;
  }
}
</style>
