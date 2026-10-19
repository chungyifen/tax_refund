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
            <el-form-item label="出口報單號碼">
              <el-input v-model="listQuery.docNo" placeholder="請輸入報單號碼" @keyup.enter="handleFilter" />
            </el-form-item>
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
          <el-button type="success" :icon="Download" @click="handleExportL(null)"
            v-permission="['TAX_REFUND_VIEW']" :disabled="!isSingleSelection || multipleSelection[0].status < 2">用料清表(L)</el-button>
          <el-button type="warning" :icon="Download" @click="handleExportA(null)"
            v-permission="['TAX_REFUND_VIEW']" :disabled="!isSingleSelection || multipleSelection[0].status < 2">沖退稅申請(A)</el-button>
        </div>
      </div>

      <!-- 資料表格 -->
      <el-table ref="tableRef" v-loading="listLoading" :data="list" element-loading-text="載入中" border fit
        highlight-current-row height="calc(100% - 90px)" style="width: 100%;"
        :header-cell-style="{ background: '#2d3a4b', color: '#ffffff', fontWeight: 'bold' }"
        @selection-change="handleSelectionChange">
        
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column align="center" label="項次" width="60">
          <template #default="scope">
            {{ (currentPage - 1) * pageSize + scope.$index + 1 }}
          </template>
        </el-table-column>
        <el-table-column align="center" label="報單號碼" prop="docNo" width="160" />
        <el-table-column align="center" label="報單項次" prop="items" width="100" />
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
        <el-table-column align="center" label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="warning" size="small" :icon="Edit" @click="handleEditRefund(row)"
                circle v-permission="['EXPORT_DECLARATION_EDIT']" :disabled="row.status < 2" />
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

    <!-- 修改退稅清單 Dialog -->
    <el-dialog title="修改退稅清單" v-model="editDialogVisible" width="960px">
      <el-table :data="refundList" border fit highlight-current-row style="width: 100%">
        <el-table-column label="原料名稱" prop="taxBom.materialName" min-width="150" />
        <el-table-column label="進口報單" prop="importDeclaration.docNo" width="160" />
        <el-table-column label="進口項次" prop="importDeclaration.items" width="90" align="center" />
        <el-table-column label="進口剩餘可核銷量" width="150" align="center">
          <template #default="{ row }">
            {{ formatNumber(row.importDeclaration?.unverifiedQty) }}
          </template>
        </el-table-column>
        <el-table-column label="目前核銷數量" width="160" align="center">
          <template #default="{ row }">
            <el-input-number
              v-model="row.usageQty"
              :precision="3"
              :step="0.1"
              :min="0"
              size="small"
              @change="handleRefundQtyChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="單位" prop="taxBom.materialUnit" width="80" align="center" />
      </el-table>
      <div style="margin-top: 10px; color: #666; font-size: 12px;">
        * 調整「目前核銷數量」後系統將即時儲存並同步更新進口報單剩餘可核銷量。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">關閉</el-button>
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import {
  searchExportList,
  getRefundListByExportId,
  updateRefundQty,
  exportReportL,
  exportReportA
} from '@/api/refund/taxRefund'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Download, Edit, Fold, Expand } from '@element-plus/icons-vue'

// 搜尋條件
const listQuery = reactive({
  docNo: '',
  status: 0
})

// 分頁相關
const list = ref([])
const total = ref(0)
const listLoading = ref(true)
const currentPage = ref(1)
const pageSize = ref(20)
const isSearchCollapsed = ref(false)

// 編輯 Dialog 狀態
const editDialogVisible = ref(false)
const refundList = ref([])
const currentExportId = ref(null)
const multipleSelection = ref([])

// 是否僅勾選單筆資料
const isSingleSelection = computed(() => {
  return multipleSelection.value.length === 1
})

const handleSelectionChange = (val) => {
  multipleSelection.value = val
}

onMounted(() => {
  fetchList()
})

// 查詢出口報單列表
const fetchList = () => {
  listLoading.value = true
  const params = {
    page: currentPage.value - 1,
    size: pageSize.value,
    ...listQuery
  }
  searchExportList(params).then(response => {
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

// 狀態標籤轉換
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

// 數值格式化（小數點後三位）
const formatNumber = (num) => {
  return num !== null && num !== undefined
    ? Number(num).toLocaleString(undefined, { minimumFractionDigits: 3, maximumFractionDigits: 3 })
    : '0.000'
}

// 產生本地時區時間戳記字串 (格式: yyyyMMddHHmmss)，避免 toISOString() 產生 UTC 時差
const getLocalTimestamp = () => {
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return (
    now.getFullYear() +
    pad(now.getMonth() + 1) +
    pad(now.getDate()) +
    pad(now.getHours()) +
    pad(now.getMinutes()) +
    pad(now.getSeconds())
  )
}

// 導出用料清表 (Report L)
const handleExportL = (row) => {
  const target = row || multipleSelection.value[0]
  if (!target) return

  exportReportL(target.docNo).then(response => {
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `${getLocalTimestamp()}-${target.docNo}_L.xls`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    fetchList() // 更新報單狀態
  })
}

// 導出沖退稅申請 (Report A)
const handleExportA = (row) => {
  const target = row || multipleSelection.value[0]
  if (!target) return

  exportReportA(target.docNo).then(response => {
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `${getLocalTimestamp()}-${target.docNo}_A.xls`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    fetchList() // 更新報單狀態
  })
}

// 開啟修改退稅清單視窗
const handleEditRefund = (row) => {
  currentExportId.value = row.id
  getRefundListByExportId(row.id).then(response => {
    refundList.value = response
    editDialogVisible.value = true
  })
}

// 更新單筆核銷數量並同步刷新進口報單剩餘可核銷量
const handleRefundQtyChange = (row) => {
  updateRefundQty(row.id, row.usageQty).then(() => {
    ElMessage.success('更新成功')
    getRefundListByExportId(currentExportId.value).then(response => {
      refundList.value = response
    })
  }).catch(() => {
    // 更新失敗時重新載入列表以還原數值
    getRefundListByExportId(currentExportId.value).then(response => {
      refundList.value = response
    })
  })
}
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
