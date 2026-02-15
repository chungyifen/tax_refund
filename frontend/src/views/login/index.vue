<template>
  <div class="login-container">
    <div class="login-card">
      <div class="title-container">
        <!-- Placeholder for Logo -->
        <h1 class="logo-text">退稅管理系統</h1> 
        <!-- <h3 class="title">Tax Refund Management System</h3> -->
      </div>

      <el-form ref="loginFormRef" :model="loginForm" :rules="loginRules" class="login-form">
        
        <el-form-item prop="username">
          <el-input
            ref="username"
            v-model="loginForm.username"
            placeholder="請輸入帳號"
            name="username"
            type="text"
            tabindex="1"
            class="custom-input"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            :key="passwordType"
            ref="password"
            v-model="loginForm.password"
            :type="passwordType"
            placeholder="請輸入密碼"
            name="password"
            tabindex="2"
            @keyup.enter="handleLogin"
            class="custom-input"
          >
            <template #suffix>
              <span class="show-pwd" @click="showPwd">
                 <el-icon><View v-if="passwordType === 'password'" /><Hide v-else /></el-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>


        <el-button :loading="loading" type="primary" style="width:100%; margin-top: 20px;" class="login-button" @click.prevent="handleLogin">
          登入
        </el-button>

      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, nextTick, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { View, Hide } from '@element-plus/icons-vue'

const loginForm = reactive({
  username: '', 
  password: ''
})

const value = ref('')

const validateUsername = (rule, value, callback) => {
  if (!value) {
    callback(new Error('Please enter the user name'))
  } else {
    callback()
  }
}
const validatePassword = (rule, value, callback) => {
  if (value.length < 6) {
    callback(new Error('The password can not be less than 6 digits'))
  } else {
    callback()
  }
}

const loginRules = {
  username: [{ required: true, trigger: 'blur', validator: validateUsername }],
  password: [{ required: true, trigger: 'blur', validator: validatePassword }]
}

const loading = ref(false)
const passwordType = ref('password')
const redirect = ref(undefined)
const loginFormRef = ref(null)

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

watch(route, (route) => {
    redirect.value = route.query && route.query.redirect
}, { immediate: true })

const showPwd = () => {
  passwordType.value = passwordType.value === 'password' ? '' : 'password'
}

const handleLogin = () => {
  loginFormRef.value.validate(valid => {
    if (valid) {
      loading.value = true
      // Hardcode login for demo if needed, or stick to store
       userStore.login(loginForm).then(() => {
        router.push({ path: redirect.value || '/' })
        loading.value = false
      }).catch(() => {
        loading.value = false
      })
    }
  })
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  width: 100%;
  background-image: url('https://images.unsplash.com/photo-1586528116311-ad8dd3c8310d?ixlib=rb-4.0.3&auto=format&fit=crop&w=2070&q=80'); /* Warehouse-like placeholder */
  background-size: cover;
  background-position: center;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  
  // Overlay to darken background slightly if needed
  &::before {
    content: '';
    position: absolute;
    top: 0; 
    left: 0;
    width: 100%; 
    height: 100%;
    background: rgba(0,0,0,0.3);
  }
}

.login-card {
  position: relative;
  width: 400px;
  background-color: #F8E8D8; // Light peach/beige
  padding: 40px;
  border-radius: 4px;
  box-shadow: 0 4px 6px rgba(0,0,0,0.1);
  text-align: center;
}

.title-container {
  margin-bottom: 30px;
  .logo-text {
      font-size: 40px;
      font-weight: 900;
      color: #1F2D3D;
      margin: 0;
      letter-spacing: 2px;
      font-family: Arial, sans-serif; // Or import a closer font
  }
  .title {
    font-size: 18px;
    color: #454545;
    margin-top: 10px;
    font-weight: normal;
  }
}

.login-form {
    :deep(.el-input__wrapper) {
        background-color: transparent !important;
        box-shadow: none !important;
        border-bottom: 2px solid #5a5e66;
        border-radius: 0;
        padding-left: 0;
        
        &.is-focus {
             border-bottom: 2px solid #1F2D3D;
        }
    }
    :deep(.el-input__inner) {
        color: #333;
    }
}

.login-button {
  background-color: #152538;
  border-color: #152538;
  height: 40px;
  font-size: 16px;
  
  &:hover {
     background-color: #2c3e50;
     border-color: #2c3e50;
  }
}
</style>
