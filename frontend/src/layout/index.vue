<template>
  <div class="app-wrapper">
    <Sidebar class="sidebar-container" />
    <div class="main-container">
      <Navbar />
      <TagsView />
      <AppMain />
    </div>
  </div>
</template>

<script setup>
import { Sidebar, Navbar, AppMain, TagsView } from './components'
</script>

<style lang="scss" scoped>
  .app-wrapper {
    position: relative;
    height: 100%;
    width: 100%;
  }
  .sidebar-container {
      width: 210px;
      height: 100%;
      position: fixed;
      top: 0;
      bottom: 0;
      left: 0;
      z-index: 1001;
      overflow: hidden;
  }
  .main-container {
      min-height: 100%;
      margin-left: 210px;
      position: relative;
  }
</style>
