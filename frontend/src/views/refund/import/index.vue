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
            <!-- 進口報單號碼：auto-complete 下拉選單 -->
            <el-form-item label="進口報單號碼">
              <el-select
                v-model="listQuery.docNo"
                filterable
                clearable
                placeholder="請選擇報單號碼"
                style="width: 100%"
                @change="handleFilter"
              >
                <el-option
                  v-for="doc in docNoOptions"
                  :key="doc"
                  :label="doc"
                  :value="doc"
                />
              </el-select>
            </el-form-item>
            <!-- 原料名稱：文字輸入 -->
            <el-form-item label="原料名稱">
              <el-input v-model="listQuery.materialName" placeholder="請輸入原料名稱" @keyup.enter="handleFilter" />
            </el-form-item>
            <!-- 核退狀態：下拉選單 -->
            <el-form-item label="核退狀態">
              <el-select v-model="listQuery.status" placeholder="請選擇核退狀態" style="width: 100%">
                <el-option label="全部" :value="0" />
                <el-option label="未完成核銷" :value="1" />
                <el-option label="已完成核銷" :value="2" />
              </el-select>
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
          <!-- 可放標題或麵包屑 -->
        </div>
        <div class="right-panel">
          <el-button type="success" :icon="Download" @click="handleExportExcel" v-permission="['IMPORT_DECLARATION_VIEW']">下載進口報單搜尋結果文件</el-button>
          <el-button type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="selectedRows.length === 0" v-permission="['IMPORT_DECLARATION_EDIT']">批次刪除 ({{ selectedRows.length }})</el-button>
          <el-button type="warning" :icon="Upload" @click="handleImportClick" v-permission="['IMPORT_DECLARATION_EDIT']">上傳進口報單</el-button>
        </div>
        <!-- 隱藏的檔案上傳 input -->
        <input type="file" ref="fileInput" style="display: none" @change="handleFileChange" accept=".xlsx, .xls" />
      </div>

      <!-- 資料表格 -->
      <el-table
        ref="tableRef"
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
        <!-- 勾選欄：已核銷數量 > 0 不可勾選 -->
        <el-table-column type="selection" width="50" align="center" :selectable="checkSelectable" />
        <el-table-column align="center" label="項次" width="60">
          <template #default="scope">
            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="報單號碼" prop="docNo" width="160" />
        <el-table-column align="center" label="報單項次" prop="items" width="100" />
        <el-table-column align="center" label="原料名稱" prop="materialName" min-width="180" />
        <el-table-column align="center" label="原料SPEC" prop="materialSpec" min-width="120" />
        <el-table-column align="center" label="進口數量" prop="importQty" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.importQty) }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="已核銷數量" prop="totalRefundQty" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.totalRefundQty) }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="未核銷數量" prop="unverifiedQty" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.unverifiedQty) }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" :icon="View" size="small" @click="handleView(row)" circle v-permission="['IMPORT_DECLARATION_VIEW']" />
            <el-button type="warning" :icon="Edit" size="small" @click="handleUpdate(row)" circle v-permission="['IMPORT_DECLARATION_EDIT']" />
            <el-button
              type="danger"
              :icon="Delete"
              size="small"
              @click="handleDelete(row)"
              circle
              :disabled="row.totalRefundQty > 0"
              :title="row.totalRefundQty > 0 ? '已核銷數量大於0，不可刪除' : '刪除'"
              v-permission="['IMPORT_DECLARATION_EDIT']"
            />
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

    <!-- 檢視 Dialog -->
    <el-dialog title="檢視進口報單" v-model="viewDialogVisible" width="600px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="報單號碼">{{ viewRow.docNo }}</el-descriptions-item>
        <el-descriptions-item label="報單項次">{{ viewRow.items }}</el-descriptions-item>
        <el-descriptions-item label="原料名稱">{{ viewRow.materialName }}</el-descriptions-item>
        <el-descriptions-item label="原料SPEC">{{ viewRow.materialSpec }}</el-descriptions-item>
        <el-descriptions-item label="原料單位">{{ viewRow.materialUnit }}</el-descriptions-item>
        <el-descriptions-item label="進口數量">{{ formatNumber(viewRow.importQty) }}</el-descriptions-item>
        <el-descriptions-item label="已核銷數量">{{ formatNumber(viewRow.totalRefundQty) }}</el-descriptions-item>
        <el-descriptions-item label="未核銷數量">{{ formatNumber(viewRow.unverifiedQty) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 編輯 Dialog -->
    <el-dialog title="修改進口報單" v-model="editDialogVisible" width="600px" append-to-body>
      <el-form ref="editForm" :rules="rules" :model="temp" label-position="left" label-width="100px" style="width: 420px; margin-left: 50px;">
        <el-form-item label="報單號碼">
          <el-input v-model="temp.docNo" disabled />
        </el-form-item>
        <el-form-item label="報單項次">
          <el-input v-model="temp.items" disabled />
        </el-form-item>
        <el-form-item label="原料名稱" prop="materialName">
          <el-input v-model="temp.materialName" />
        </el-form-item>
        <el-form-item label="原料SPEC" prop="materialSpec">
          <el-input v-model="temp.materialSpec" />
        </el-form-item>
        <el-form-item label="原料單位" prop="materialUnit">
          <el-input v-model="temp.materialUnit" />
        </el-form-item>
        <el-form-item label="進口數量" prop="importQty">
          <el-input-number v-model="temp.importQty" :precision="3" :step="1" :min="0" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="updateData">確認</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import {
  searchImportDeclarations,
  getDocNos,
  updateImportDeclaration,
  deleteImportDeclaration,
  batchDeleteImportDeclarations,
  importExcel,
  exportExcel
} from '@/api/refund/importDeclaration'
import { Search, Refresh, Upload, Download, Edit, Delete, View, Fold, Expand } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { getToken } from '@/utils/auth'

// ===== 查詢條件 =====
const listQuery = reactive({
  docNo: '',
  materialName: '',
  status: 0
})

// ===== 報單號碼下拉選項 =====
const docNoOptions = ref([])

// ===== 表格資料 =====
const list = ref([])
const listLoading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

// ===== 搜尋欄收合 =====
const isSearchCollapsed = ref(false)

// ===== 檢視 Dialog =====
const viewDialogVisible = ref(false)
const viewRow = ref({})

// ===== 編輯 Dialog =====
const editDialogVisible = ref(false)
const editForm = ref(null)
const temp = reactive({
  id: undefined,
  docNo: '',
  items: '',
  materialName: '',
  materialSpec: '',
  materialUnit: '',
  importQty: 0
})

// ===== 上傳 =====
const fileInput = ref(null)

// ===== 表格 ref =====
const tableRef = ref(null)

// ===== 多選狀態 =====
const selectedRows = ref([])

// ===== 表單驗證規則 =====
const rules = {
  materialName: [{ required: true, message: '原料名稱必填', trigger: 'blur' }],
  importQty: [{ required: true, message: '進口數量必填', trigger: 'blur' }]
}

// ===== 切換搜尋欄 =====
const toggleSearch = () => {
  isSearchCollapsed.value = !isSearchCollapsed.value
}

// ===== 數字格式化 =====
const formatNumber = (val) => {
  if (val === null || val === undefined) return '0.000'
  return Number(val).toFixed(3)
}

// ===== 載入報單號碼列表（auto-complete） =====
const fetchDocNos = () => {
  getDocNos().then(response => {
    docNoOptions.value = response
  })
}

// ===== 載入表格資料 =====
const fetchList = () => {
  listLoading.value = true
  const params = {
    page: currentPage.value - 1, // Spring 分頁從 0 開始
    size: pageSize.value
  }
  if (listQuery.docNo) params.docNo = listQuery.docNo
  if (listQuery.materialName) params.materialName = listQuery.materialName
  if (listQuery.status !== undefined && listQuery.status !== null) params.status = listQuery.status

  searchImportDeclarations(params).then(response => {
    list.value = response.content || []
    total.value = response.totalElements || 0
    listLoading.value = false
  }).catch(() => {
    listLoading.value = false
  })
}

// ===== 查詢 =====
const handleFilter = () => {
  currentPage.value = 1
  fetchList()
}

// ===== 清除查詢條件 =====
const resetFilter = () => {
  listQuery.docNo = ''
  listQuery.materialName = ''
  listQuery.status = 0
  currentPage.value = 1
  fetchList()
}

// ===== 分頁事件 =====
const handleSizeChange = () => {
  currentPage.value = 1
  fetchList()
}
const handleCurrentChange = () => {
  fetchList()
}

// ===== 檢視 =====
const handleView = (row) => {
  viewRow.value = row
  viewDialogVisible.value = true
}

// ===== 修改 =====
const handleUpdate = (row) => {
  Object.assign(temp, row)
  editDialogVisible.value = true
  nextTick(() => {
    editForm.value?.clearValidate()
  })
}

const updateData = () => {
  editForm.value.validate((valid) => {
    if (valid) {
      const data = {
        materialName: temp.materialName,
        materialSpec: temp.materialSpec,
        materialUnit: temp.materialUnit,
        importQty: temp.importQty
      }
      updateImportDeclaration(temp.id, data).then(() => {
        editDialogVisible.value = false
        ElMessage.success('更新成功')
        fetchList()
      })
    }
  })
}

// ===== 刪除 =====
const handleDelete = (row) => {
  // 前端再次檢查已核銷數量
  if (row.totalRefundQty > 0) {
    ElMessage.warning('已核銷數量大於 0，不可刪除此報單')
    return
  }
  ElMessageBox.confirm('確定要刪除此筆進口報單嗎?', '警告', {
    type: 'warning'
  }).then(() => {
    deleteImportDeclaration(row.id).then(() => {
      ElMessage.success('刪除成功')
      fetchList()
    }).catch((err) => {
      // 後端也會檢查，顯示後端錯誤訊息
      if (err.response && err.response.data) {
        ElMessage.error(err.response.data.message || '刪除失敗')
      }
    })
  })
}

// ===== 多選變更事件 =====
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// ===== 檢查是否可勾選（已核銷數量 > 0 不可勾選） =====
const checkSelectable = (row) => {
  return !(row.totalRefundQty > 0)
}

// ===== 批次刪除 =====
const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('請先勾選要刪除的報單')
    return
  }
  ElMessageBox.confirm(`確定要刪除這 ${selectedRows.value.length} 筆進口報單嗎?`, '警告', {
    type: 'warning'
  }).then(() => {
    const ids = selectedRows.value.map(row => row.id)
    batchDeleteImportDeclarations(ids).then(res => {
      if (res.errorCount > 0) {
        const errorHtml = `<p>成功刪除：${res.successCount} 筆，失敗：${res.errorCount} 筆</p><hr/><div style="max-height: 300px; overflow-y: auto; color: red;">${res.errorMessages.join('<br/>')}</div>`
        ElMessageBox.alert(errorHtml, '批次刪除結果', {
          dangerouslyUseHTMLString: true,
          customClass: 'import-error-dialog'
        })
      } else {
        ElMessage.success(`成功刪除 ${res.successCount} 筆報單`)
      }
      fetchList()
    })
  })
}

// ===== 上傳 Excel =====
const handleImportClick = () => {
  fileInput.value.click()
}

const handleFileChange = (e) => {
  const file = e.target.files[0]
  if (!file) return
  const formData = new FormData()
  formData.append('file', file)
  importExcel(formData).then(res => {
    if (res.errorCount > 0) {
      const errorHtml = `<p>成功：${res.successCount} 筆，失敗：${res.errorCount} 筆</p><hr/><div style="max-height: 300px; overflow-y: auto; color: red;">${res.errorMessages.join('<br/>')}</div>`
      ElMessageBox.alert(errorHtml, '匯入結果', {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '關閉',
        type: 'warning',
        customClass: 'import-error-dialog'
      })
    } else {
      ElMessage.success(`匯入成功：新增 ${res.successCount} 筆`)
    }
    fetchList()
    fetchDocNos() // 重新載入報單號碼選單
    e.target.value = '' // 重設 input
  }).catch((err) => {
    console.error(err)
    ElMessage.error('匯入失敗，請檢查檔案或是網路連線')
    e.target.value = ''
  })
}

// ===== 下載 Excel =====
const handleExportExcel = () => {
  const params = {}
  if (listQuery.docNo) params.docNo = listQuery.docNo
  if (listQuery.materialName) params.materialName = listQuery.materialName
  if (listQuery.status !== undefined && listQuery.status !== null) params.status = listQuery.status

  // 使用 axios 直接下載 blob（繞過 request.js 的 response.data 攔截器）
  const token = getToken()
  axios({
    url: '/api/refund/import-declaration/export',
    method: 'get',
    params,
    responseType: 'blob',
    headers: token ? { 'Authorization': 'Bearer ' + token } : {}
  }).then(response => {
    const url = window.URL.createObjectURL(new Blob([response.data]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', '進口報單搜尋結果.xlsx')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('下載成功')
  }).catch(() => {
    ElMessage.error('下載失敗')
  })
}

// ===== 初始化 =====
onMounted(() => {
  fetchDocNos()
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
    writing-mode: vertical-lr;
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
  margin-left: 10px;
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

<style lang="scss">
.import-error-dialog {
  width: 800px !important;
  max-width: 90vw !important;
}
</style>
