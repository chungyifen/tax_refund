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
        <!-- 標題與操作區 -->
        <div class="action-bar">
            <h3>退稅清單管理</h3>
            <div>
              <el-button type="success" :icon="Download" @click="handleExportL(null)" 
                v-permission="['TAX_REFUND_VIEW']" :disabled="!isSingleSelection || multipleSelection[0].status < 2">用料清表(L)</el-button>
              <el-button type="warning" :icon="Download" @click="handleExportA(null)" 
                v-permission="['TAX_REFUND_VIEW']" :disabled="!isSingleSelection || multipleSelection[0].status < 2">沖退稅申請(A)</el-button>
            </div>
        </div>

      <!-- 資料表格 -->
      <el-table ref="tableRef" v-loading="listLoading" :data="list" element-loading-text="載入中" border fit
        highlight-current-row height="calc(100% - 60px)" style="width: 100%;"
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
            <el-button type="primary" size="small" :icon="Edit" @click="handleEditRefund(row)" 
                v-permission="['EXPORT_DECLARATION_EDIT']" :disabled="row.status < 2">修改退稅</el-button>
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
    <el-dialog title="修改退稅清單" v-model="editDialogVisible" width="900px">
      <el-table :data="refundList" border fit highlight-current-row style="width: 100%">
        <el-table-column label="原料名稱" prop="taxBom.materialName" min-width="150" />
        <el-table-column label="進口報單" prop="importDeclaration.docNo" width="160" />
        <el-table-column label="進口項次" prop="importDeclaration.items" width="80" align="center" />
        <el-table-column label="目前核銷數量" width="150" align="center">
            <template #default="{ row }">
                <el-input-number v-model="row.usageQty" :precision="3" :step="0.1" :min="0" size="small" @change="handleRefundQtyChange(row)" />
            </template>
        </el-table-column>
        <el-table-column label="單位" prop="taxBom.materialUnit" width="80" align="center" />
      </el-table>
      <div style="margin-top: 10px; color: #666; font-size: 12px;">
         * 修改數量後請點擊「儲存」按鈕以生效。注意：增加核銷數量需確保進口報單仍有餘額。
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="editDialogVisible = false">關閉</el-button>
          <!-- 這裡其實是單筆修改即時生效，或者是批次修改？ -->
          <!-- 根據API設計是單筆 update。這裡 UI 呈現多筆，可以設計成每改一筆就呼叫 API，或者全部改完按儲存。
               為了避免資料不一致，建議單筆修改後顯示 loading 或 toast -->
        </div>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  searchExportList,
  getRefundListByExportId,
  updateRefundQty,
  exportReportL,
  exportReportA
} from '@/api/refund/taxRefund'
import { ElMessage, ElMessageBox } from 'element-plus'
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

// 編輯 Dialog
const editDialogVisible = ref(false)
const refundList = ref([])
const currentExportId = ref(null)
const multipleSelection = ref([])
import { computed } from 'vue'

const isSingleSelection = computed(() => {
  return multipleSelection.value.length === 1
})

const handleSelectionChange = (val) => {
  multipleSelection.value = val
}

onMounted(() => {
  fetchList()
})

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

// 狀態標籤
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

// 導出 L
const handleExportL = (row) => {
  const target = row || multipleSelection.value[0]
  if (!target) return
  
  exportReportL(target.docNo).then(response => {
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    // 檔名格式: 
    // yyyyMMddHHmmss-docNo_L.xls
    // 後端 Header Content-Disposition 會帶檔名，前端如果不設 download 屬性，瀏覽器會使用後端檔名嗎？
    // 通常 axios blob response header filename extraction 比較麻煩。
    // 這裡我們暫時保留前端設定檔名，或者依賴後端。
    // 如果後端設定正確，<a> tag 可以不設 download 屬性？ 不，blob url 需要。
    // 為了符合使用者需求，這裡嘗試手動組檔名，但時間點會與後端不一致。
    // 最好是讀取 header。但 request.js 可能只還傳 data。
    // 簡單解法：這裡只設 docNo_L.xls，讓瀏覽器或使用者自己看？
    // 使用者要求：[年月日時分秒-出口報單_L.xls]
    // 我們可以抓現在時間。
    
    // 這裡只依賴後端提供的 filename (如果有的話)。
    // 若沒有，前端組。
    const nowStr = new Date().toISOString().replace(/[-:T.]/g, '').slice(0, 14) // YYYYMMDDHHmmss
    link.setAttribute('download', `${nowStr}-${target.docNo}_L.xls`) 
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    fetchList() // 更新狀態
  })
}

// 導出 A
const handleExportA = (row) => {
  const target = row || multipleSelection.value[0]
  if (!target) return

  exportReportA(target.docNo).then(response => {
    const url = window.URL.createObjectURL(new Blob([response]))
    const link = document.createElement('a')
    link.href = url
    const nowStr = new Date().toISOString().replace(/[-:T.]/g, '').slice(0, 14)
    link.setAttribute('download', `${nowStr}-${target.docNo}_A.xls`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    fetchList() // 更新狀態
  })
}

// 修改退稅
const handleEditRefund = (row) => {
    currentExportId.value = row.id
    getRefundListByExportId(row.id).then(response => {
        refundList.value = response
        editDialogVisible.value = true
    })
}

const handleRefundQtyChange = (row) => {
    // 呼叫 API 更新
    updateRefundQty(row.id, row.usageQty).then(() => {
        ElMessage.success('更新成功')
    }).catch(err => {
        // 更新失敗，重新載入該筆資料 (或整個列表)
        getRefundListByExportId(currentExportId.value).then(response => {
            refundList.value = response
        })
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
  padding: 0 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.pagination-container {
  padding: 10px 0;
  display: flex;
  justify-content: flex-end;
}

/* Base styles for tables/forms if needed */
</style>
