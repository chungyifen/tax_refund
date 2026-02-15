<template>
  <div class="app-container">
    <div class="filter-container">
      <el-button class="filter-item" type="primary" :icon="Edit" @click="handleCreate">
        新增功能
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
      <el-table-column label="功能代碼">
        <template #default="scope">
          {{ scope.row.code }}
        </template>
      </el-table-column>
      <el-table-column label="功能名稱">
        <template #default="scope">
          {{ scope.row.name }}
        </template>
      </el-table-column>
      <el-table-column label="描述">
        <template #default="scope">
          {{ scope.row.description }}
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
        <el-form-item label="功能代碼" prop="code">
          <el-input v-model="temp.code" />
        </el-form-item>
        <el-form-item label="功能名稱" prop="name">
          <el-input v-model="temp.name" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="temp.description" type="textarea" />
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
import { getFunctionList, createFunction, updateFunction, deleteFunction } from '@/api/system/function'
import { Edit } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref(null)
const listLoading = ref(true)

const temp = reactive({
  id: undefined,
  code: '',
  name: '',
  description: ''
})

const dialogFormVisible = ref(false)
const dialogStatus = ref('')
const textMap = {
  update: '編輯',
  create: '新增'
}

const dataForm = ref(null)

const rules = {
  code: [{ required: true, message: '請輸入功能代碼', trigger: 'blur' }],
  name: [{ required: true, message: '請輸入功能名稱', trigger: 'blur' }]
}

const fetchList = () => {
  listLoading.value = true
  getFunctionList().then(response => {
    list.value = response
    listLoading.value = false
  }).catch(() => {
    listLoading.value = false
  })
}

const resetTemp = () => {
  temp.id = undefined
  temp.code = ''
  temp.name = ''
  temp.description = ''
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
      createFunction(temp).then(() => {
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
  temp.code = row.code
  temp.name = row.name
  temp.description = row.description
  
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
      updateFunction(tempData.id, tempData).then(() => {
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
    ElMessageBox.confirm('確定要刪除此功能嗎?', '警告', {
        confirmButtonText: '確定',
        cancelButtonText: '取消',
        type: 'warning'
    }).then(() => {
        deleteFunction(row.id).then(() => {
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
})
</script>
