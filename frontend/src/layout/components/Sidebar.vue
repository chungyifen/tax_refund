<template>
  <div class="sidebar-container has-logo">
    <Logo :collapse="isCollapse" />
    <el-scrollbar wrap-class="scrollbar-wrapper">
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :background-color="variables.menuBg"
        :text-color="variables.menuText"
        :active-text-color="variables.menuActiveText"
        :unique-opened="false"
        :collapse-transition="false"
        mode="vertical"
      >
        <template v-for="route in routes" :key="route.path">
            <template v-if="!route.hidden">
                 <el-sub-menu v-if="route.children && route.children.length > 1" :index="route.path">
                    <template #title>
                        <el-icon v-if="route.meta && route.meta.icon"><component :is="route.meta.icon" /></el-icon>
                        <span>{{ route.meta?.title }}</span>
                    </template>
                    <router-link v-for="child in route.children" :key="child.path" :to="resolvePath(route.path, child.path)">
                         <el-menu-item :index="resolvePath(route.path, child.path)">
                             <template #title>{{ child.meta?.title }}</template>
                         </el-menu-item>
                    </router-link>
                 </el-sub-menu>
                 <router-link v-else-if="route.children && route.children.length === 1" :to="resolvePath(route.path, route.children[0].path)">
                    <el-menu-item :index="resolvePath(route.path, route.children[0].path)">
                        <el-icon v-if="route.children[0].meta && route.children[0].meta.icon"><component :is="route.children[0].meta.icon" /></el-icon>
                         <template #title>{{ route.children[0].meta?.title }}</template>
                    </el-menu-item>
                 </router-link>
                  <router-link v-else :to="route.path">
                    <el-menu-item :index="route.path">
                        <el-icon v-if="route.meta && route.meta.icon"><component :is="route.meta.icon" /></el-icon>
                         <template #title>{{ route.meta?.title }}</template>
                    </el-menu-item>
                 </router-link>
            </template>
        </template>
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { constantRoutes } from '@/router'
import { useUserStore } from '@/stores/user'
import Logo from './Sidebar/Logo.vue'

const route = useRoute()
const userStore = useUserStore()

/**
 * 檢查使用者是否具有路由所需的權限
 * 如果路由沒有設定 permissions，表示所有人都可以看到
 */
const hasPermission = (route) => {
  if (route.meta && route.meta.permissions) {
    return route.meta.permissions.some(p => userStore.roles.includes(p))
  }
  return true
}

/**
 * 根據權限過濾路由
 * - 如果父路由有子路由，只保留有權限的子路由
 * - 如果過濾後沒有可見的子路由，則隱藏整個父路由
 */
const filterRoutes = (routes) => {
  const result = []
  routes.forEach(route => {
    // 複製路由物件避免修改原始資料
    const tmp = { ...route }
    if (tmp.children) {
      tmp.children = tmp.children.filter(child => hasPermission(child))
      // 如果過濾後沒有可見的子路由，不加入結果
      if (tmp.children.length === 0) return
    }
    if (hasPermission(tmp)) {
      result.push(tmp)
    }
  })
  return result
}

// 根據使用者權限過濾後的路由
const routes = computed(() => filterRoutes(constantRoutes))

const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})

const isCollapse = false
const variables = {
  menuBg: '#2d3a4b', 
  menuText: '#bfcbd9',
  menuActiveText: '#409EFF' 
}

const resolvePath = (basePath, routePath) => {
    if (basePath === '/') return '/' + routePath
    return basePath + '/' + routePath
}
</script>

<style lang="scss" scoped>
.sidebar-container {
    height: 100%;
    background-color: #2d3a4b;
    .el-menu {
        border: none;
        height: 100%;
        width: 100% !important;
    }
}
</style>
