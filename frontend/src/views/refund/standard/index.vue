<template>
  <div class="app-container" style="display: flex; height: calc(100vh - 84px); padding: 0;">
    <!-- 左側搜尋區 -->
    <transition name="slide-fade">
      <div v-show="!isSearchCollapsed" class="search-sidebar">
        <div class="search-header">
          <span>查詢條件</span>
          <el-icon class="collapse-btn" @click="toggleSearch"><Fold /></el-icon>
        </div>
        <div class="search-content">
          <el-form :model="listQuery" label-position="top">
            <el-form-item label="核准文號">
              <el-input v-model="listQuery.docNo" placeholder="請輸入核准文號" @keyup.enter="handleFilter" />
            </el-form-item>
            <el-form-item label="成品規格">
              <el-input v-model="listQuery.prodType" placeholder="請輸入成品規格" @keyup.enter="handleFilter" />
            </el-form-item>
            <el-form-item label="成品名稱(英)">
              <el-input v-model="listQuery.prodName" placeholder="請輸入成品名稱" @keyup.enter="handleFilter" />
            </el-form-item>

            
            <div class="search-actions">
               <el-button type="primary" :icon="Search" @click="handleFilter" style="width: 100%; margin-bottom: 10px;">查詢</el-button>
               <el-button :icon="Refresh" @click="resetFilter" style="width: 100%; margin-left: 0;">清除</el-button>
            </div>
          </el-form>
        </div>
      </div>
    </transition>

    <!-- 縮小後的搜尋條 -->
    <div v-show="isSearchCollapsed" class="search-sidebar-collapsed" @click="toggleSearch">
      <div class="collapsed-title">
        查詢條件 <el-icon><Expand /></el-icon>
      </div>
    </div>

    <!-- 右側內容區 -->
    <div class="main-content">
      <div class="action-bar">
         <div class="left-panel">
            <!-- 麵包屑或標題可放這 -->
         </div>
         <div class="right-panel">
            <el-button type="primary" :icon="Plus" @click="handleCreate" v-permission="['BOM_EDIT']">新增</el-button>
            <el-button type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="multipleSelection.length === 0" v-permission="['BOM_EDIT']">批次刪除</el-button>
            <el-button type="primary" :icon="Upload" @click="handleImportClick" v-permission="['BOM_EDIT']">上傳核退標準</el-button>
         </div>
         <!-- 隱藏的檔案上傳 input -->
         <input type="file" ref="fileInput" style="display: none" @change="handleFileChange" accept=".xlsx, .xls" />
      </div>

      <el-table
        v-loading="listLoading"
        :data="list"
        element-loading-text="載入中"
        border
        fit
        highlight-current-row
        height="calc(100% - 90px)"
        style="width: 100%;"
        :header-cell-style="{ background: '#2d3a4b', color: '#ffffff', fontWeight: 'bold' }"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column align="center" label="項次" width="60">
          <template #default="scope">
            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="核准文號" prop="docNo" width="150" />
        <el-table-column align="center" label="成品規格" prop="prodType" width="100" />
        <el-table-column align="center" label="成品名稱" prop="prodName" width="200" />
        <el-table-column align="center" label="原料序號" prop="materialNum" width="80" />
        <el-table-column align="center" label="原料名稱" prop="materialName" min-width="150" />
        <el-table-column align="center" label="原料SPEC" prop="materialSpec" min-width="150" />
        <el-table-column align="center" label="使用數量" prop="usageQty" width="100" />
        <el-table-column align="center" label="使用單位" prop="materialUnit" width="80" />
        
        <el-table-column align="center" label="操作" width="100" fixed="right">
          <template #default="{row}">
            <el-button type="warning" size="small" :icon="Edit" @click="handleUpdate(row)" circle v-permission="['BOM_EDIT']" />
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)" circle v-permission="['BOM_EDIT']" />
          </template>
        </el-table-column>
      </el-table>

      <!-- 分頁 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 編輯 Dialog -->
    <el-dialog :title="textMap[dialogStatus]" v-model="dialogFormVisible" width="600px" append-to-body>
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px" style="width: 400px; margin-left:50px;">
        <el-form-item label="核准文號" prop="docNo">
          <el-input v-model="temp.docNo" />
        </el-form-item>
        <el-form-item label="成品規格" prop="prodType">
          <el-input v-model="temp.prodType" />
        </el-form-item>
        <el-form-item label="成品名稱" prop="prodName">
          <el-input v-model="temp.prodName" />
        </el-form-item>
         <el-form-item label="原料序號" prop="materialNum">
          <el-input v-model.number="temp.materialNum" />
        </el-form-item>
        <el-form-item label="原料名稱" prop="materialName">
          <el-input v-model="temp.materialName" />
        </el-form-item>
         <el-form-item label="原料SPEC" prop="materialSpec">
          <el-input v-model="temp.materialSpec" />
        </el-form-item>
        <el-form-item label="使用數量" prop="usageQty">
          <el-input-number v-model="temp.usageQty" :precision="3" :step="0.001" style="width: 100%;"/>
        </el-form-item>
        <el-form-item label="使用單位" prop="materialUnit">
          <el-input v-model="temp.materialUnit" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="dialogStatus==='create'?createData():updateData()">確認</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { getTaxBomList, createTaxBom, updateTaxBom, deleteTaxBom, deleteTaxBomBatch, importTaxBom } from '@/api/refund/taxBom'
import { Search, Refresh, Upload, Edit, Delete, Fold, Expand, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const list = ref([])
const listLoading = ref(true)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const listQuery = reactive({
  docNo: '',
  prodName: '',
  prodType: ''
})
const fileInput = ref(null)
const isSearchCollapsed = ref(false)
const tempPlaceholder = ref('')
const multipleSelection = ref([])

const temp = reactive({
  id: undefined,
  docNo: '',
  prodType: '',
  prodName: '',
  prodUnit: 'SET', // default
  materialNum: 1,
  materialName: '',
  materialSpec: 'NIL',
  usageQty: 0,
  materialUnit: 'EAC'
})

const dialogFormVisible = ref(false)
const dialogStatus = ref('')
const textMap = {
  update: '編輯',
  create: '新增'
}
const dataForm = ref(null)
const rules = {
  docNo: [{ required: true, message: '必填', trigger: 'blur' }],
  prodName: [{ required: true, message: '必填', trigger: 'blur' }]
}

const toggleSearch = () => {
    isSearchCollapsed.value = !isSearchCollapsed.value
}

const fetchList = () => {
  listLoading.value = true
  const params = {
    ...listQuery,
    page: currentPage.value - 1,
    size: pageSize.value
  }
  getTaxBomList(params).then(response => {
    list.value = response.content || []
    total.value = response.totalElements || 0
    listLoading.value = false
  }).catch(() => {
    listLoading.value = false
  })
}

const handleFilter = () => {
  currentPage.value = 1
  fetchList()
}

const resetFilter = () => {
  listQuery.docNo = ''
  listQuery.prodName = ''
  listQuery.prodType = ''
  currentPage.value = 1
  fetchList()
}

const handleSizeChange = () => {
  currentPage.value = 1
  fetchList()
}

const handleCurrentChange = () => {
  fetchList()
}

const resetTemp = () => {
  temp.id = undefined
  temp.docNo = ''
  temp.prodType = ''
  temp.prodName = ''
  temp.prodUnit = 'SET'
  temp.materialNum = 1
  temp.materialName = ''
  temp.materialSpec = 'NIL'
  temp.usageQty = 0
  temp.materialUnit = 'EAC'
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
      createTaxBom(temp).then(() => {
        dialogFormVisible.value = false
        ElMessage.success('建立成功')
        fetchList()
      })
    }
  })
}

const handleUpdate = (row) => {
  Object.assign(temp, row)
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
      updateTaxBom(tempData.id, tempData).then(() => {
        dialogFormVisible.value = false
        ElMessage.success('更新成功')
        fetchList()
      })
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm('確定要刪除嗎?', '警告', {
    type: 'warning'
  }).then(() => {
    deleteTaxBom(row.id).then(() => {
      ElMessage.success('刪除成功')
      fetchList()
    })
  })
}

const handleBatchDelete = () => {
    if (multipleSelection.value.length === 0) return
    ElMessageBox.confirm(`確定要刪除選取的 ${multipleSelection.value.length} 筆資料嗎?`, '警告', {
        type: 'warning'
    }).then(() => {
        const ids = multipleSelection.value.map(item => item.id)
        deleteTaxBomBatch(ids).then(() => {
            ElMessage.success('批次刪除成功')
            fetchList()
        })
    })
}

const handleSelectionChange = (val) => {
    multipleSelection.value = val
}

const handleImportClick = () => {
  fileInput.value.click()
}

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  importTaxBom(formData).then(res => {
     ElMessage.success(`匯入成功：新增${res.successCount}筆，失敗${res.errorCount}筆`)
     fetchList()
     e.target.value = '' // reset input
  }).catch(() => {
     e.target.value = ''
  })
}

onMounted(() => {
  fetchList()
})
</script>

<style lang="scss" scoped>
.app-container {
    background-color: #f0f2f5;
}

.search-sidebar {
    width: 280px;
    background-color: #fff;
    border-right: 1px solid #dcdfe6;
    display: flex;
    flex-direction: column;
    padding: 10px;
    transition: width 0.3s;
    overflow-y: auto;
    
    .search-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 20px;
        font-weight: bold;
        font-size: 16px;
        
        .collapse-btn {
            cursor: pointer;
            font-size: 20px;
            &:hover { color: #409EFF; }
        }
    }
}

.search-sidebar-collapsed {
    width: 40px;
    background-color: #fff;
    border-right: 1px solid #dcdfe6;
    cursor: pointer;
    display: flex;
    justify-content: center;
    padding-top: 20px;
    transition: width 0.3s;
    
    .collapsed-title {
        writing-mode: vertical-lr; /* 垂直文字 */
        letter-spacing: 5px;
        display: flex;
        align-items: center;
        gap: 10px;
        font-weight: bold;
        color: #606266;
    }
    
    &:hover {
        background-color: #f9fafc;
        color: #409EFF;
    }
}

.main-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    background-color: #fff;
    padding: 10px;
    margin-left: 10px; /* Gap between sidebar and content */
    overflow: hidden;
}

.action-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
    flex-wrap: wrap;
    gap: 8px;
}

.pagination-container {
    display: flex;
    justify-content: flex-end;
    padding: 10px 0;
}
</style>
