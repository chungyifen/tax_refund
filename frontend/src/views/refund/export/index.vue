<template>
  <div class="app-container" style="display: flex; height: calc(100vh - 84px); padding: 0;">
    <!-- 左側搜尋區 -->
    <transition name="slide-fade">
      <div v-show="!isSearchCollapsed" class="search-sidebar">
        <div class="search-header">
          <span>查詢條件</span>
          <el-icon class="collapse-btn" @click="toggleSearch">
            <Fold />
          </el-icon>
        </div>
        <div class="search-content">
          <el-form :model="listQuery" label-position="top">
            <!-- 出口報單號碼：auto-complete 下拉選單 -->
            <el-form-item label="出口報單號碼">
              <el-select v-model="listQuery.docNo" filterable clearable placeholder="請選擇報單號碼" style="width: 100%"
                @change="handleFilter">
                <el-option v-for="doc in docNoOptions" :key="doc" :label="doc" :value="doc" />
              </el-select>
            </el-form-item>
            <!-- 成品規格：文字輸入 -->
            <el-form-item label="成品規格">
              <el-input v-model="listQuery.prodType" placeholder="請輸入成品規格" @keyup.enter="handleFilter" />
            </el-form-item>
            <!-- 成品名稱：文字輸入 -->
            <el-form-item label="成品名稱">
              <el-input v-model="listQuery.prodName" placeholder="請輸入成品名稱" @keyup.enter="handleFilter" />
            </el-form-item>
            <!-- 核退狀態：下拉選單 -->
            <el-form-item label="核退狀態">
              <el-select v-model="listQuery.status" placeholder="請選擇核退狀態" style="width: 100%">
                <el-option label="全部" :value="0" />
                <el-option label="已匯入出口明細" :value="1" />
                <el-option label="已產生核銷清單" :value="2" />
                <el-option label="已產生核銷清單報表" :value="3" />
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
        查詢條件 <el-icon>
          <Expand />
        </el-icon>
      </div>
    </div>

    <!-- 右側內容區 -->
    <div class="main-content">
      <div class="action-bar">
        <div class="left-panel">
          <!-- 可放標題或麵包屑 -->
        </div>
        <div class="right-panel">
            <el-button type="primary" :icon="List" @click="handleGenerateRefund" v-permission="['EXPORT_DECLARATION_EDIT']">產生退稅清單</el-button>
            <el-button type="success" :icon="Download" @click="handleExportExcel" v-permission="['EXPORT_DECLARATION_VIEW']">下載出口報單搜尋結果文件</el-button>
            <el-button type="danger" :icon="Delete" @click="handleBatchDelete" :disabled="selectedRows.length === 0" v-permission="['EXPORT_DECLARATION_EDIT']">批次刪除 ({{ selectedRows.length }})</el-button>
            <el-button type="warning" :icon="Upload" @click="handleImportClick" v-permission="['EXPORT_DECLARATION_EDIT']">上傳出口報單</el-button>
        </div>
        <!-- 隱藏的檔案上傳 input -->
        <input type="file" ref="fileInput" style="display: none" @change="handleFileChange" accept=".xlsx, .xls" />
      </div>

      <!-- 資料表格 -->
      <el-table ref="tableRef" v-loading="listLoading" :data="list" element-loading-text="載入中" border fit
        highlight-current-row height="calc(100% - 90px)" style="width: 100%;"
        :header-cell-style="{ background: '#2d3a4b', color: '#ffffff', fontWeight: 'bold' }"
        @selection-change="handleSelectionChange">
        <!-- 勾選欄：狀態非 1 (已匯入) 不可勾選 -->
        <el-table-column type="selection" width="50" align="center" :selectable="checkSelectable" />
        <el-table-column align="center" label="項次" width="60">
          <template #default="scope">
            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="報單號碼" prop="docNo" width="160" />
        <el-table-column align="center" label="報單項次" prop="items" width="100" />
        <el-table-column align="center" label="成品規格" prop="prodType" min-width="120" />
        <el-table-column align="center" label="成品名稱" prop="prodName" min-width="180" />
        <el-table-column align="center" label="出口數量" prop="exportQty" width="120">
          <template #default="{ row }">
            {{ formatNumber(row.exportQty) }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="核退狀態" width="140">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusLabel(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column align="center" label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" :icon="Edit" @click="handleUpdate(row)" v-permission="['EXPORT_DECLARATION_EDIT']" />
            <el-button type="danger" size="small" :icon="Delete" @click="handleDelete(row)"
              :disabled="row.status !== 1" v-permission="['EXPORT_DECLARATION_EDIT']" />
          </template>
        </el-table-column>
      </el-table>

      <!-- 分頁 -->
      <div class="pagination-container">
        <el-pagination v-model:current-page="currentPage" v-model:page-size="pageSize" :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
          @current-change="handleCurrentChange" />
      </div>
    </div>

    <!-- 修改 Dialog -->
    <el-dialog :title="dialogStatus === 'create' ? '新增' : '修改'" v-model="dialogFormVisible" width="500px">
      <el-form ref="dataForm" :rules="rules" :model="temp" label-position="left" label-width="100px"
        style="width: 400px; margin-left:50px;">
        <el-form-item label="報單號碼" prop="docNo">
          <el-input v-model="temp.docNo" disabled />
        </el-form-item>
        <el-form-item label="報單項次" prop="items">
          <el-input v-model="temp.items" disabled />
        </el-form-item>
        <el-form-item label="成品規格" prop="prodType">
          <el-input v-model="temp.prodType" />
        </el-form-item>
        <el-form-item label="成品名稱" prop="prodName">
          <el-input v-model="temp.prodName" />
        </el-form-item>
        <el-form-item label="出口數量" prop="exportQty">
          <el-input-number v-model="temp.exportQty" :precision="3" :step="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogFormVisible = false">取消</el-button>
          <el-button type="primary" @click="updateData">確認</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 產生退稅清單 Dialog -->
    <el-dialog title="產生退稅清單" v-model="generateDialogVisible" width="450px">
      <el-form label-position="top">
        <el-form-item label="請選擇出口報單號碼">
          <el-select v-model="generateDocNo" filterable clearable placeholder="請選擇報單號碼" style="width: 100%">
            <el-option v-for="doc in docNoOptions" :key="doc" :label="doc" :value="doc" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="generateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmGenerateRefund" :loading="generateLoading"
            :disabled="!generateDocNo">確認產生</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import {
  searchExportDeclarations,
  getDocNos,
  updateExportDeclaration,
  deleteExportDeclaration,
  importExcel,
  exportExcel,
  batchDeleteExportDeclarations,
  generateRefundList
} from '@/api/refund/exportDeclaration'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Upload, Download, Edit, Delete, Fold, Expand, List } from '@element-plus/icons-vue'

// 搜尋條件
const listQuery = reactive({
  docNo: '',
  prodType: '',
  prodName: '',
  status: 0
})

// 分頁相關
const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const currentPage = ref(1)
const pageSize = ref(20)

const docNoOptions = ref([])
const isSearchCollapsed = ref(false)

// 編輯 Dialog
const dialogFormVisible = ref(false)
const dialogStatus = ref('')
const temp = reactive({
  id: undefined,
  docNo: '',
  items: '',
  prodType: '',
  prodName: '',
  exportQty: 0
})

const rules = {
  prodName: [{ required: true, message: '請輸入成品名稱', trigger: 'blur' }],
  exportQty: [{ required: true, message: '請輸入出口數量', trigger: 'blur' }]
}
const dataForm = ref(null)

// 上傳與選取
const fileInput = ref(null)
const tableRef = ref(null)
const selectedRows = ref([])

// 產生退稅清單
const generateDialogVisible = ref(false)
const generateDocNo = ref('')
const generateLoading = ref(false)

onMounted(() => {
  fetchDocNos()
  fetchList()
})

const fetchDocNos = () => {
  getDocNos().then(response => {
    docNoOptions.value = response
  })
}

const fetchList = () => {
  listLoading.value = true
  const params = {
    page: currentPage.value - 1,
    size: pageSize.value,
    ...listQuery
  }
  searchExportDeclarations(params).then(response => {
    list.value = response.content
    total.value = response.totalElements
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
  listQuery.prodType = ''
  listQuery.prodName = ''
  listQuery.status = 0
  handleFilter()
}

const toggleSearch = () => {
  isSearchCollapsed.value = !isSearchCollapsed.value
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  fetchList()
}

// 狀態與樣式
const getStatusLabel = (status) => {
  const statusMap = {
    1: '已匯入出口明細',
    2: '已產生核銷清單',
    3: '已產生核銷清單報表'
  }
  return statusMap[status] || '未知'
}

const getStatusType = (status) => {
  const statusMap = {
    1: 'info',
    2: 'success',
    3: 'warning'
  }
  return statusMap[status] || ''
}

const formatNumber = (num) => {
  return num ? Number(num).toLocaleString(undefined, { minimumFractionDigits: 3, maximumFractionDigits: 3 }) : '0.000'
}

// 修改
const handleUpdate = (row) => {
  Object.assign(temp, row)
  dialogStatus.value = 'update'
  dialogFormVisible.value = true
  nextTick(() => {
    dataForm.value?.clearValidate()
  })
}

const updateData = () => {
  dataForm.value.validate((valid) => {
    if (valid) {
      updateExportDeclaration(temp.id, temp).then(() => {
        dialogFormVisible.value = false
        ElMessage.success('更新成功')
        fetchList()
      })
    }
  })
}

// 刪除
const handleDelete = (row) => {
  if (row.status !== 1) {
    ElMessage.warning('僅有「已匯入出口明細」狀態的報單可刪除')
    return
  }
  ElMessageBox.confirm('確定要刪除此筆出口報單嗎?', '警告', {
    type: 'warning'
  }).then(() => {
    deleteExportDeclaration(row.id).then(() => {
      ElMessage.success('刪除成功')
      fetchList()
    }).catch(err => {
        if(err.response && err.response.data && err.response.data.message) {
            ElMessage.error(err.response.data.message)
        } else {
             ElMessage.error('刪除失敗')
        }
    })
  })
}

// 選取與批次刪除
const checkSelectable = (row) => {
  return row.status === 1
}

const handleSelectionChange = (val) => {
  selectedRows.value = val
}

const handleBatchDelete = () => {
  if (selectedRows.value.length === 0) return

  const ids = selectedRows.value.map(row => row.id)
  ElMessageBox.confirm(`確定要刪除選取的 ${ids.length} 筆出口報單嗎?`, '警告', {
    type: 'warning'
  }).then(() => {
    batchDeleteExportDeclarations(ids).then(response => {
      const result = response
      if (result.errors && result.errors.length > 0) {
        // 顯示部分刪除結果
        let msg = `成功刪除: ${result.successCount} 筆。`
        if (result.failureCount > 0) {
            msg += `<br/>失敗: ${result.failureCount} 筆。<br/>錯誤訊息:<br/>`
            msg += result.errors.join('<br/>')
            ElMessageBox.alert(msg, '批次刪除結果', {
                dangerouslyUseHTMLString: true,
                customStyle: { maxWidth: '600px' }
            })
        } else {
            ElMessage.success(msg)
        }
      } else {
        ElMessage.success(`成功刪除 ${result.successCount} 筆`)
      }
      fetchList()
    })
  })
}

// 匯入
const handleImportClick = () => {
  fileInput.value.click()
}

const handleFileChange = (e) => {
  const files = e.target.files
  if (files.length > 0) {
    const formData = new FormData()
    formData.append('file', files[0])
    importExcel(formData).then(response => {
      const result = response
      if (result.errors && result.errors.length > 0) {
        let errorHtml = `<div style="max-height: 300px; overflow-y: auto; text-align: left;">`
        errorHtml += `<p>成功匯入: ${result.successCount} 筆</p>`
        errorHtml += `<p style="color: red;">失敗: ${result.failureCount} 筆</p>`
        errorHtml += `<ul>`
        result.errors.forEach(err => {
          errorHtml += `<li>${err}</li>`
        })
        errorHtml += `</ul></div>`

        ElMessageBox.alert(errorHtml, '匯入結果', {
          dangerouslyUseHTMLString: true,
          title: '匯入結果 (含錯誤)',
          customStyle: { maxWidth: '600px', width: '80%' }
        }).then(() => {
            fetchList()
            fetchDocNos() // 更新報單號碼清單
        })
      } else {
        ElMessage.success(`成功匯入 ${result.successCount} 筆資料`)
        fetchList()
        fetchDocNos()
      }
    }).finally(() => {
      e.target.value = '' // Reset file input
    })
  }
}

// 匯出
const handleExportExcel = () => {
  const params = { ...listQuery }
  exportExcel(params).then(response => {
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', 'export_declarations.xlsx')
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
  })
}

// 產生退稅清單
const handleGenerateRefund = () => {
  generateDocNo.value = ''
  generateDialogVisible.value = true
}

const confirmGenerateRefund = () => {
  if (!generateDocNo.value) {
    ElMessage.warning('請先選擇出口報單號碼')
    return
  }
  generateLoading.value = true
  generateRefundList(generateDocNo.value).then(result => {
    generateDialogVisible.value = false
    generateLoading.value = false

    // 組合結果訊息
    if (result.warnings && result.warnings.length > 0) {
      let html = `<div style="text-align: left;">`
      html += `<p><b>報表號碼：</b>${result.reportNo}</p>`
      html += `<p><b>成功建立：</b>${result.successCount} 筆退稅紀錄</p>`
      html += `<p style="color: #E6A23C; font-weight: bold;">⚠ 警告訊息：</p>`
      html += `<ul style="max-height: 250px; overflow-y: auto; padding-left: 20px;">`
      result.warnings.forEach(w => {
        html += `<li style="margin-bottom: 4px;">${w}</li>`
      })
      html += `</ul></div>`
      ElMessageBox.alert(html, '產生退稅清單結果', {
        dangerouslyUseHTMLString: true,
        customStyle: { maxWidth: '600px', width: '80%' }
      })
    } else {
      ElMessage.success(`退稅清單產生完成！報表號碼: ${result.reportNo}，共 ${result.successCount} 筆`)
    }

    // 重新載入清單以反映狀態變更
    fetchList()
  }).catch(() => {
    generateLoading.value = false
  })
}
</script>

<style scoped>
.search-sidebar {
  width: 250px;
  background-color: #f0f2f5;
  border-right: 1px solid #dcdfe6;
  display: flex;
  flex-direction: column;
  transition: width 0.3s;
  flex-shrink: 0;
}

.search-header {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 15px;
  background-color: #2d3a4b;
  color: white;
  font-weight: bold;
}

.collapse-btn {
  cursor: pointer;
  font-size: 18px;
}

.search-content {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
}

.search-actions {
  margin-top: 20px;
}

/* 縮小後的側邊欄 */
.search-sidebar-collapsed {
  width: 40px;
  background-color: #2d3a4b;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 15px;
  transition: width 0.3s;
  flex-shrink: 0;
}

.collapsed-title {
  writing-mode: vertical-rl;
  letter-spacing: 5px;
  display: flex;
  align-items: center;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 10px;
  overflow: hidden;
  background-color: #fff;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.pagination-container {
  padding: 10px 0;
  display: flex;
  justify-content: flex-end;
}

/* Transition animation */
.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.3s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  transform: translateX(-20px);
  opacity: 0;
}
</style>
