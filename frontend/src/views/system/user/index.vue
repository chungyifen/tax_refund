<template>
  <div class="app-container">
    <div class="filter-container">
      <el-input v-model="listQuery.username" placeholder="帳號" style="width: 200px;" class="filter-item" @keyup.enter="handleFilter" />
      <el-button class="filter-item" type="primary" :icon="Search" @click="handleFilter">
        查詢
      </el-button>
      <el-button class="filter-item" style="margin-left: 10px;" type="primary" :icon="Edit" @click="handleCreate">
        新增使用者
      </el-button>
    </div>

    <el-table
      v-loading="listLoading"
      :data="list"
      element-loading-text="載入中"
      border
      fit
      highlight-current-row
      style="width: 100%; margin-top: 20px;"
    >
      <el-table-column align="center" label="項次" width="95">
        <template #default="scope">
          {{ scope.$index + 1 }}
        </template>
      </el-table-column>
      <el-table-column label="帳號">
        <template #default="scope">
          {{ scope.row.username }}
        </template>
      </el-table-column>
      <el-table-column label="Email" align="center">
        <template #default="scope">
          <span>{{ scope.row.email }}</span>
        </template>
      </el-table-column>
      <el-table-column label="狀態" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.enabled ? 'success' : 'danger'">{{ scope.row.enabled ? '啟用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="登入時間" align="center">
         <template #default="scope">
          <span>{{ scope.row.loginDate }}</span>
        </template>
      </el-table-column>
       <el-table-column label="操作" align="center" width="300" class-name="small-padding fixed-width">
        <template #default="{row}">
          <el-button type="primary" size="small" @click="handleUpdate(row)">
            編輯
          </el-button>
          <el-button size="small" type="warning" @click="handleChangePassword(row)">
            變更密碼
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">
            刪除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Dialog -->
    <el-dialog :title="textMap[dialogStatus]" v-model="dialogFormVisible">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px" style="width: 400px; margin-left:50px;">
        <el-form-item label="帳號" prop="username">
          <el-input v-model="temp.username" />
        </el-form-item>
        <el-form-item label="密碼" prop="password" v-if="dialogStatus==='create'">
            <el-input v-model="temp.password" type="password" />
        </el-form-item>
        <el-form-item label="Email" prop="email">
            <el-input v-model="temp.email" />
        </el-form-item>
        <el-form-item label="啟用狀態">
             <el-switch v-model="temp.enabled" />
        </el-form-item>
        <el-form-item label="角色">
            <el-select v-model="temp.roles" multiple value-key="id" placeholder="請選擇角色" style="width: 100%">
                <el-option
                  v-for="item in rolesList"
                  :key="item.id"
                  :label="item.name"
                  :value="item"
                />
            </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">
            取消
          </el-button>
          <el-button type="primary" @click="dialogStatus==='create'?createData():updateData()">
            確認
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 變更密碼 Dialog -->
    <el-dialog title="變更密碼" v-model="pwdDialogVisible" width="400px">
      <el-form ref="pwdForm" :model="pwdTemp" :rules="pwdRules" label-width="100px">
        <el-form-item label="新密碼" prop="newPassword">
          <el-input v-model="pwdTemp.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="確認密碼" prop="confirmPassword">
          <el-input v-model="pwdTemp.confirmPassword" type="password" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitChangePassword">確認</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { getUserList, createUser, updateUser, deleteUser, changePassword } from '@/api/system/user'
import { getRoleList } from '@/api/system/role'
import { Search, Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref(null)
const listLoading = ref(true)
const listQuery = reactive({
  username: undefined
})

const rolesList = ref([])

const temp = reactive({
  id: undefined,
  username: '',
  password: '',
  email: '',
  enabled: true,
  roles: []
})

const dialogFormVisible = ref(false)
const dialogStatus = ref('')
const textMap = {
  update: '編輯',
  create: '新增'
}

const dataForm = ref(null)

const rules = {
  username: [{ required: true, message: '請輸入帳號', trigger: 'blur' }],
  password: [{ required: true, message: '請輸入密碼', trigger: 'blur' }],
  email: [{ type: 'email', message: '請輸入正確的 Email', trigger: ['blur', 'change'] }]
}

// 變更密碼相關
const pwdDialogVisible = ref(false)
const pwdForm = ref(null)
const pwdTemp = reactive({
  userId: null,
  newPassword: '',
  confirmPassword: ''
})

// 確認密碼驗證器
const validateConfirmPwd = (rule, value, callback) => {
  if (value !== pwdTemp.newPassword) {
    callback(new Error('兩次輸入的密碼不一致'))
  } else {
    callback()
  }
}

const pwdRules = {
  newPassword: [{ required: true, message: '請輸入新密碼', trigger: 'blur' }, { min: 6, message: '密碼長度不能少於 6 位', trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '請再次輸入密碼', trigger: 'blur' }, { validator: validateConfirmPwd, trigger: 'blur' }]
}

const handleChangePassword = (row) => {
  pwdTemp.userId = row.id
  pwdTemp.newPassword = ''
  pwdTemp.confirmPassword = ''
  pwdDialogVisible.value = true
  nextTick(() => {
    pwdForm.value.clearValidate()
  })
}

const submitChangePassword = () => {
  pwdForm.value.validate((valid) => {
    if (valid) {
      changePassword(pwdTemp.userId, pwdTemp.newPassword).then(() => {
        pwdDialogVisible.value = false
        ElMessage({ message: '密碼變更成功', type: 'success', duration: 2000 })
      })
    }
  })
}

const fetchList = () => {
  listLoading.value = true
  getUserList(listQuery).then(response => {
    list.value = response
    listLoading.value = false
  })
}

const fetchRoles = () => {
    getRoleList().then(response => {
        rolesList.value = response
    })
}

const handleFilter = () => {
  fetchList()
}

const resetTemp = () => {
  temp.id = undefined
  temp.username = ''
  temp.password = ''
  temp.email = ''
  temp.enabled = true
  temp.roles = []
}

const handleCreate = () => {
  resetTemp()
  dialogStatus.value = 'create'
  dialogFormVisible.value = true
  nextTick(() => {
    dataForm.value.clearValidate()
  })
}

const createData = () => {
  dataForm.value.validate((valid) => {
    if (valid) {
      createUser(temp).then(() => {
        list.value.unshift(temp)
        dialogFormVisible.value = false
        ElMessage({
          message: '建立成功',
          type: 'success',
          duration: 2000
        })
        fetchList()
      })
    }
  })
}

const handleUpdate = (row) => {
  temp.id = row.id
  temp.username = row.username
  temp.email = row.email
  temp.enabled = row.enabled
  temp.roles = row.roles
  // password is not shown/edited here for simplicity, usually needs separate api or specific logic
  
  dialogStatus.value = 'update'
  dialogFormVisible.value = true
  nextTick(() => {
    dataForm.value.clearValidate()
  })
}

const updateData = () => {
  dataForm.value.validate((valid) => {
    if (valid) {
      const tempData = Object.assign({}, temp)
      updateUser(tempData.id, tempData).then(() => {
         dialogFormVisible.value = false
         ElMessage({
            message: '更新成功',
            type: 'success',
            duration: 2000
         })
         fetchList()
      })
    }
  })
}

const handleDelete = (row) => {
    ElMessageBox.confirm('確定要刪除此使用者嗎?', '警告', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        deleteUser(row.id).then(() => {
            ElMessage({
                type: 'success',
                message: '刪除成功'
            })
            fetchList()
        })
    }).catch(() => {
        ElMessage({
            type: 'info',
            message: '已取消刪除'
        })
    })
}

onMounted(() => {
  fetchList()
  fetchRoles()
})
</script>
