<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button class="filter-item" type="primary" :icon="Edit" @click="handleCreate">
        新增角色
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
      <el-table-column label="角色名稱">
        <template #default="scope">
          {{ scope.row.name }}
        </template>
      </el-table-column>
      <el-table-column label="描述" align="center">
        <template #default="scope">
          <span>{{ scope.row.description }}</span>
        </template>
      </el-table-column>
       <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
        <template #default="{row}">
          <el-button type="primary" size="small" @click="handleUpdate(row)">
            編輯
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
        <el-form-item label="角色名稱" prop="name">
          <el-input v-model="temp.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="temp.description" type="textarea" />
        </el-form-item>
        <el-form-item label="權限設定">
            <el-tree
                ref="treeRef"
                :data="functionList"
                show-checkbox
                node-key="id"
                :props="defaultProps"
            />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { getRoleList, createRole, updateRole, deleteRole } from '@/api/system/role'
import { getFunctionList } from '@/api/system/function'
import { Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref(null)
const listLoading = ref(true)
const functionList = ref([])

const temp = reactive({
  id: undefined,
  name: '',
  description: '',
  functions: []
})

const dialogFormVisible = ref(false)
const dialogStatus = ref('')
const textMap = {
  update: '編輯',
  create: '新增'
}

const dataForm = ref(null)
const treeRef = ref(null)

const rules = {
  name: [{ required: true, message: '請輸入角色名稱', trigger: 'blur' }]
}

const defaultProps = {
  children: 'children', // Assuming flat list for now, or backend returns hierarchy? Backend likely returns flat list.
  label: 'name'
}

const fetchList = () => {
  listLoading.value = true
  getRoleList().then(response => {
    list.value = response
    listLoading.value = false
  })
}

const fetchFunctions = () => {
    getFunctionList().then(response => {
        functionList.value = response
    })
}


const resetTemp = () => {
  temp.id = undefined
  temp.name = ''
  temp.description = ''
  temp.functions = []
  if (treeRef.value) {
      treeRef.value.setCheckedKeys([])
  }
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
      const checkedKeys = treeRef.value.getCheckedKeys()
      // Backend expects Set<Function>, so we need to map IDs back to objects or just send IDs if backend supports it.
      // Based on RoleController, it expects Role object with Set<Function>.
      // We should probably convert checkedKeys to Function objects.
      // Assuming functionList contains full objects.
      const selectedFunctions = functionList.value.filter(f => checkedKeys.includes(f.id))
      
      temp.functions = selectedFunctions

      createRole(temp).then(() => {
        list.value.push(temp) // Re-fetch might be safer
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
  temp.name = row.name
  temp.description = row.description
  temp.functions = row.functions || []
  
  dialogStatus.value = 'update'
  dialogFormVisible.value = true
  nextTick(() => {
    dataForm.value.clearValidate()
    // Set checked keys
    const checkedIds = temp.functions.map(f => f.id)
    treeRef.value.setCheckedKeys(checkedIds)
  })
}

const updateData = () => {
  dataForm.value.validate((valid) => {
    if (valid) {
      const checkedKeys = treeRef.value.getCheckedKeys()
      const selectedFunctions = functionList.value.filter(f => checkedKeys.includes(f.id))
      temp.functions = selectedFunctions

      const tempData = Object.assign({}, temp)
      updateRole(tempData.id, tempData).then(() => {
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
    ElMessageBox.confirm('確定要刪除此角色嗎?', '警告', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        deleteRole(row.id).then(() => {
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
  fetchFunctions()
})
</script>
