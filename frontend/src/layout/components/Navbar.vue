<template>
  <div class="navbar">
    <div class="right-menu">
      <el-dropdown class="avatar-container" trigger="click">
        <div class="avatar-wrapper">
          <span class="user-name">{{ name }}</span>
          <el-icon class="el-icon--right"><arrow-down /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu class="user-dropdown">
            <router-link to="/">
              <el-dropdown-item> 首頁 </el-dropdown-item>
            </router-link>
            <el-dropdown-item divided @click="handleLogout">
              <span style="display:block;">登出</span>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useUserStore } from '@/stores/user'
import { useRouter, useRoute } from 'vue-router'
import { ArrowDown } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const name = computed(() => userStore.name || 'User')

// 登出處理：確認後清除 token 並跳轉登入頁
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('確定要登出系統嗎？', '提示', {
      confirmButtonText: '確定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // 即使後端 logout API 失敗，也要清除前端 token
    try {
      await userStore.logout()
    } catch (e) {
      // 後端可能沒有 logout 端點，忽略錯誤
      await userStore.resetToken()
    }
    router.push(`/login?redirect=${route.fullPath}`)
  } catch {
    // 使用者取消登出
  }
}
</script>

<style lang="scss" scoped>
.navbar {
  height: 50px;
  overflow: hidden;
  position: relative;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0,21,41,.08);

  .right-menu {
    float: right;
    height: 100%;
    line-height: 50px;
    padding-right: 20px;

    &:focus {
      outline: none;
    }

    .avatar-container {
      margin-right: 30px;

      .avatar-wrapper {
        margin-top: 5px;
        position: relative;
        cursor: pointer;
        display: flex;
        align-items: center;

        .user-name {
          font-weight: 600;
          margin-right: 5px;
        }

        .el-icon--right {
          font-size: 12px;
        }
      }
    }
  }
}
</style>
