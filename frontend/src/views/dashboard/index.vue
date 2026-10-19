<template>
  <div v-loading="loading" class="dashboard-container" element-loading-text="載入首頁統計資料中...">
    <!-- 頂部歡迎橫幅與快速操作捷徑 -->
    <div class="welcome-banner">
      <div class="welcome-left">
        <div class="welcome-avatar">
          <el-icon :size="28"><UserFilled /></el-icon>
        </div>
        <div class="welcome-info">
          <div class="welcome-title">
            歡迎回來，<span>{{ name || '操作人員' }}</span>
            <el-tag
              v-for="role in roles"
              :key="role"
              size="small"
              effect="dark"
              type="primary"
              class="role-tag"
            >
              {{ formatRoleName(role) }}
            </el-tag>
          </div>
          <div class="welcome-subtitle">
            <span>進口退稅核銷管理系統營運儀表板</span>
            <el-divider direction="vertical" />
            <span>
              本週統計區間：{{ stats.weekStartDate || '--' }} 至 {{ stats.weekEndDate || '--' }}
            </span>
          </div>
        </div>
      </div>

      <div class="welcome-actions">
        <el-button type="primary" plain :icon="Document" @click="navigateTo('/refund/standard')">
          核退標準維護
        </el-button>
        <el-button type="success" plain :icon="Download" @click="navigateTo('/refund/import')">
          進口報單管理
        </el-button>
        <el-button type="warning" plain :icon="Upload" @click="navigateTo('/refund/export')">
          出口報單管理
        </el-button>
        <el-button type="info" plain :icon="List" @click="navigateTo('/refund/list')">
          退稅核銷清單
        </el-button>
        <el-button type="primary" :icon="Refresh" :loading="loading" @click="fetchStats">
          重新整理
        </el-button>
      </div>
    </div>

    <!-- 第一排：四大核心 KPI 指標卡片 -->
    <el-row :gutter="20" class="kpi-row">
      <!-- 1. 本周新增的進口報單 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="kpi-card kpi-blue" @click="navigateTo('/refund/import')">
          <div class="kpi-body">
            <div class="kpi-icon-wrapper">
              <el-icon :size="30"><Box /></el-icon>
            </div>
            <div class="kpi-content">
              <div class="kpi-label">本周新增進口報單</div>
              <div class="kpi-value">
                {{ formatInteger(stats.weeklyImportDocCount) }}
                <span class="kpi-unit">張</span>
              </div>
              <div class="kpi-sub">
                包含 <strong>{{ formatInteger(stats.weeklyImportItemCount) }}</strong> 筆報單明細項次
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 2. 本周新增核銷的出口報單 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="kpi-card kpi-green" @click="navigateTo('/refund/list')">
          <div class="kpi-body">
            <div class="kpi-icon-wrapper">
              <el-icon :size="30"><CircleCheckFilled /></el-icon>
            </div>
            <div class="kpi-content">
              <div class="kpi-label">本周新增核銷出口報單</div>
              <div class="kpi-value">
                {{ formatInteger(stats.weeklyVerifiedExportDocCount) }}
                <span class="kpi-unit">張</span>
              </div>
              <div class="kpi-sub">
                已完成 <strong>{{ formatInteger(stats.weeklyVerifiedExportItemCount) }}</strong> 筆出口項次核銷
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 3. 目前為止還有未核銷項目的進口報單數目 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="kpi-card kpi-orange" @click="navigateTo('/refund/import')">
          <div class="kpi-body">
            <div class="kpi-icon-wrapper">
              <el-icon :size="30"><WarningFilled /></el-icon>
            </div>
            <div class="kpi-content">
              <div class="kpi-label">尚有未核銷項目之進口報單</div>
              <div class="kpi-value">
                {{ formatInteger(stats.unverifiedImportDocCount) }}
                <span class="kpi-unit">張</span>
              </div>
              <div class="kpi-sub">
                尚有 <strong>{{ formatInteger(stats.unverifiedImportItemCount) }}</strong> 筆未核銷項次（餘額
                {{ formatNumber(stats.totalUnverifiedImportQty) }}）
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 建議項目：待處理出口報單提醒 (待核銷 / 待產出報表) -->
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="kpi-card kpi-purple" @click="navigateTo('/refund/export')">
          <div class="kpi-body">
            <div class="kpi-icon-wrapper">
              <el-icon :size="30"><Timer /></el-icon>
            </div>
            <div class="kpi-content">
              <div class="kpi-label">待核銷出口報單（待辦提醒）</div>
              <div class="kpi-value">
                {{ formatInteger(stats.pendingExportDocCount) }}
                <span class="kpi-unit">張</span>
              </div>
              <div class="kpi-sub">
                共 <strong>{{ formatInteger(stats.pendingExportItemCount) }}</strong> 筆待核銷｜待匯出報表
                <strong>{{ formatInteger(stats.verifiedUnreportedExportDocCount) }}</strong> 張
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第二排（建議擴充區塊）：整體原料核銷率 & 出口報單作業進度總覽 -->
    <el-row :gutter="20" class="section-row">
      <!-- 整體進口原料核銷水位與 BOM 概況 -->
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" class="panel-card overview-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><DataAnalysis /></el-icon>
                全系統進口原料核銷水位與 BOM 標準概況
              </span>
              <el-tag size="small" type="info" effect="plain">建議營運指標</el-tag>
            </div>
          </template>
          <div class="overview-body">
            <div class="gauge-box">
              <el-progress
                type="dashboard"
                :percentage="Number(stats.overallVerificationRate || 0)"
                :width="136"
                :stroke-width="12"
                :color="gaugeColors"
              >
                <template #default="{ percentage }">
                  <span class="gauge-value">{{ percentage }}%</span>
                  <span class="gauge-label">整體核銷率</span>
                </template>
              </el-progress>
            </div>
            <div class="overview-metrics">
              <div class="metric-item">
                <span class="metric-name">累計進口原料總量</span>
                <span class="metric-num">{{ formatNumber(stats.totalImportQty) }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-name">累計已核銷總量</span>
                <span class="metric-num text-success">{{ formatNumber(stats.totalRefundQty) }}</span>
              </div>
              <div class="metric-item">
                <span class="metric-name">剩餘可核銷總餘額</span>
                <span class="metric-num text-warning">
                  {{ formatNumber(stats.totalUnverifiedImportQty) }}
                </span>
              </div>
              <div class="metric-item">
                <span class="metric-name">已建檔核退標準 (BOM)</span>
                <span class="metric-num text-primary">
                  {{ formatInteger(stats.totalBomDocCount) }} 份文號（{{
                    formatInteger(stats.totalBomItemCount)
                  }}
                  筆用料）
                </span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 出口報單三階段作業流程進度 -->
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" class="panel-card workflow-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Operation /></el-icon>
                出口報單退稅核銷作業流程進度
              </span>
              <el-tag size="small" type="info" effect="plain">建議追蹤指標</el-tag>
            </div>
          </template>
          <div class="workflow-steps">
            <!-- 階段 1：已匯入待核銷 -->
            <div class="workflow-step step-pending" @click="navigateTo('/refund/export')">
              <div class="step-header">
                <span class="step-badge badge-warning">階段 1</span>
                <span class="step-name">已匯入待核銷</span>
              </div>
              <div class="step-count">
                <strong>{{ formatInteger(stats.pendingExportDocCount) }}</strong> 張報單
                <span class="step-items">（{{ formatInteger(stats.pendingExportItemCount) }} 項次）</span>
              </div>
              <el-progress
                :percentage="getExportStagePercentage(stats.pendingExportDocCount)"
                status="warning"
                :stroke-width="8"
              />
              <div class="step-hint">尚待執行 FIFO 自動核銷計算</div>
            </div>

            <!-- 階段 2：已產生核銷清單待產出報表 -->
            <div class="workflow-step step-verified" @click="navigateTo('/refund/list')">
              <div class="step-header">
                <span class="step-badge badge-primary">階段 2</span>
                <span class="step-name">已核銷待匯出報表</span>
              </div>
              <div class="step-count">
                <strong>{{ formatInteger(stats.verifiedUnreportedExportDocCount) }}</strong> 張報單
                <span class="step-items">
                  （{{ formatInteger(stats.verifiedUnreportedExportItemCount) }} 項次）
                </span>
              </div>
              <el-progress
                :percentage="getExportStagePercentage(stats.verifiedUnreportedExportDocCount)"
                :stroke-width="8"
              />
              <div class="step-hint">可匯出用料清表 (L) 與沖退稅申請 (A)</div>
            </div>

            <!-- 階段 3：已產生核銷清單報表 -->
            <div class="workflow-step step-reported" @click="navigateTo('/refund/list')">
              <div class="step-header">
                <span class="step-badge badge-success">階段 3</span>
                <span class="step-name">已完成報表匯出</span>
              </div>
              <div class="step-count">
                <strong>{{ formatInteger(stats.reportedExportDocCount) }}</strong> 張報單
                <span class="step-items">
                  （{{ formatInteger(stats.reportedExportItemCount) }} 項次）
                </span>
              </div>
              <el-progress
                :percentage="getExportStagePercentage(stats.reportedExportDocCount)"
                status="success"
                :stroke-width="8"
              />
              <div class="step-hint">已完成沖退稅報表產出歸檔</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第三排：本周新增進口報單 & 本周新增核銷出口報單 明細列表 -->
    <el-row :gutter="20" class="section-row">
      <!-- 1. 本周新增的進口報單明細 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><DocumentAdd /></el-icon>
                本周新增的進口報單
                <el-tag size="small" type="primary" class="header-count-tag">
                  {{ formatInteger(stats.weeklyImportDocCount) }} 張 /
                  {{ formatInteger(stats.weeklyImportItemCount) }} 項次
                </el-tag>
              </span>
              <el-button link type="primary" @click="navigateTo('/refund/import')">
                查看全部 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>

          <el-table
            :data="stats.weeklyImportDeclarations"
            border
            stripe
            height="310"
            style="width: 100%"
            empty-text="本周尚無新增的進口報單"
            :header-cell-style="{ background: '#f5f7fa', color: '#303133', fontWeight: 'bold' }"
          >
            <el-table-column label="進口報單號碼" prop="docNo" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="link-text" @click="navigateTo('/refund/import')">{{ row.docNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="項次數" prop="itemCount" width="80" align="center" />
            <el-table-column label="總進口數量" width="115" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.totalImportQty) }}
              </template>
            </el-table-column>
            <el-table-column label="已核銷數量" width="115" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.totalRefundQty) }}
              </template>
            </el-table-column>
            <el-table-column label="核銷狀態" width="105" align="center">
              <template #default="{ row }">
                <el-tag
                  v-if="Number(row.totalRefundQty) <= 0"
                  size="small"
                  type="info"
                >
                  未核銷
                </el-tag>
                <el-tag
                  v-else-if="Number(row.totalRefundQty) < Number(row.totalImportQty)"
                  size="small"
                  type="warning"
                >
                  部分核銷
                </el-tag>
                <el-tag v-else size="small" type="success">已核銷完畢</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="新增時間" width="145" align="center">
              <template #default="{ row }">
                {{ formatDateTime(row.createTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 2. 本周新增核銷的出口報單明細 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Checked /></el-icon>
                本周新增核銷的出口報單
                <el-tag size="small" type="success" class="header-count-tag">
                  {{ formatInteger(stats.weeklyVerifiedExportDocCount) }} 張 /
                  {{ formatInteger(stats.weeklyVerifiedExportItemCount) }} 項次
                </el-tag>
              </span>
              <el-button link type="primary" @click="navigateTo('/refund/list')">
                前往退稅清單 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>

          <el-table
            :data="stats.weeklyVerifiedExportDeclarations"
            border
            stripe
            height="310"
            style="width: 100%"
            empty-text="本周尚無新增核銷的出口報單"
            :header-cell-style="{ background: '#f5f7fa', color: '#303133', fontWeight: 'bold' }"
          >
            <el-table-column label="出口報單號碼" prop="docNo" min-width="150" show-overflow-tooltip>
              <template #default="{ row }">
                <span class="link-text" @click="navigateTo('/refund/list')">{{ row.docNo }}</span>
              </template>
            </el-table-column>
            <el-table-column label="核銷項次" prop="itemCount" width="85" align="center" />
            <el-table-column label="出口總數量" width="115" align="right">
              <template #default="{ row }">
                {{ formatNumber(row.totalExportQty) }}
              </template>
            </el-table-column>
            <el-table-column label="扣抵筆數" prop="refundRecordCount" width="85" align="center">
              <template #default="{ row }">
                {{ row.refundRecordCount || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="作業狀態" width="115" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.status === 3" size="small" type="success">已產出報表</el-tag>
                <el-tag v-else size="small" type="primary">已產生核銷清單</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="核銷時間" width="145" align="center">
              <template #default="{ row }">
                {{ formatDateTime(row.verifiedTime) }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 第四排：4. 核銷最多前十名的原料 & 5. 核銷最多前十名的成品 -->
    <el-row :gutter="20" class="section-row">
      <!-- 4. 核銷最多前十名的原料 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card rank-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Trophy /></el-icon>
                核銷最多前十名的原料 (Top 10)
              </span>
              <el-radio-group v-model="materialSortMode" size="small">
                <el-radio-button value="qty">依核銷數量</el-radio-button>
                <el-radio-button value="count">依核銷次數</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <div v-if="displayedTopMaterials.length === 0" class="empty-wrapper">
            <el-empty description="目前尚無原料核銷紀錄" :image-size="90" />
          </div>
          <div v-else class="rank-list">
            <div
              v-for="(item, index) in displayedTopMaterials"
              :key="item.materialName + '-' + index"
              class="rank-item"
            >
              <div class="rank-row-top">
                <div class="rank-title-group">
                  <span :class="['rank-badge', getRankBadgeClass(index)]">{{ index + 1 }}</span>
                  <span class="rank-name" :title="item.materialName">{{ item.materialName }}</span>
                  <el-tag v-if="item.materialUnit" size="small" type="info" effect="plain">
                    {{ item.materialUnit }}
                  </el-tag>
                </div>
                <div class="rank-stats-group">
                  <span class="primary-stat">
                    {{ formatNumber(item.totalVerifiedQty) }}
                    <small>{{ item.materialUnit || '' }}</small>
                  </span>
                  <span class="secondary-stat">
                    核銷 {{ formatInteger(item.verifyCount) }} 次｜{{ formatInteger(item.docCount) }} 張報單
                  </span>
                </div>
              </div>
              <el-progress
                :percentage="getMaterialBarPercentage(item)"
                :stroke-width="8"
                :show-text="false"
                :color="getRankBarColor(index)"
              />
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 5. 核銷最多前十名的成品 -->
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="panel-card rank-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">
                <el-icon><Medal /></el-icon>
                核銷最多前十名的成品 (Top 10)
              </span>
              <el-radio-group v-model="productSortMode" size="small">
                <el-radio-button value="qty">依核銷數量</el-radio-button>
                <el-radio-button value="count">依核銷項次</el-radio-button>
              </el-radio-group>
            </div>
          </template>

          <div v-if="displayedTopProducts.length === 0" class="empty-wrapper">
            <el-empty description="目前尚無成品核銷紀錄" :image-size="90" />
          </div>
          <div v-else class="rank-list">
            <div
              v-for="(item, index) in displayedTopProducts"
              :key="item.prodName + '-' + item.prodType + '-' + index"
              class="rank-item"
            >
              <div class="rank-row-top">
                <div class="rank-title-group">
                  <span :class="['rank-badge', getRankBadgeClass(index)]">{{ index + 1 }}</span>
                  <span class="rank-name" :title="item.prodName">{{ item.prodName }}</span>
                  <el-tag v-if="item.prodType" size="small" type="primary" effect="plain">
                    {{ item.prodType }}
                  </el-tag>
                </div>
                <div class="rank-stats-group">
                  <span class="primary-stat">
                    {{ formatNumber(item.totalVerifiedQty) }}
                    <small>SET</small>
                  </span>
                  <span class="secondary-stat">
                    {{ formatInteger(item.verifyCount) }} 筆項次｜{{ formatInteger(item.docCount) }} 張報單
                  </span>
                </div>
              </div>
              <el-progress
                :percentage="getProductBarPercentage(item)"
                :stroke-width="8"
                :show-text="false"
                :color="getRankBarColor(index)"
              />
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  UserFilled,
  Document,
  Download,
  Upload,
  List,
  Refresh,
  Box,
  CircleCheckFilled,
  WarningFilled,
  Timer,
  DataAnalysis,
  Operation,
  DocumentAdd,
  Checked,
  ArrowRight,
  Trophy,
  Medal
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { getDashboardStats } from '@/api/refund/dashboard'

const router = useRouter()
const userStore = useUserStore()

// 使用者名稱與角色資訊
const name = computed(() => userStore.name)
const roles = computed(() => userStore.roles)

// 載入狀態與統計資料物件
const loading = ref(false)
const stats = ref({
  weekStartDate: '',
  weekEndDate: '',
  weeklyImportDocCount: 0,
  weeklyImportItemCount: 0,
  weeklyImportDeclarations: [],
  weeklyVerifiedExportDocCount: 0,
  weeklyVerifiedExportItemCount: 0,
  weeklyVerifiedExportDeclarations: [],
  unverifiedImportDocCount: 0,
  unverifiedImportItemCount: 0,
  totalUnverifiedImportQty: 0,
  topMaterialsByQty: [],
  topMaterialsByCount: [],
  topProductsByQty: [],
  topProductsByCount: [],
  pendingExportDocCount: 0,
  pendingExportItemCount: 0,
  verifiedUnreportedExportDocCount: 0,
  verifiedUnreportedExportItemCount: 0,
  reportedExportDocCount: 0,
  reportedExportItemCount: 0,
  totalImportQty: 0,
  totalRefundQty: 0,
  overallVerificationRate: 0,
  totalBomDocCount: 0,
  totalBomItemCount: 0
})

// 前十名排行榜排序切換模式 ('qty': 依核銷數量, 'count': 依核銷次數/項次)
const materialSortMode = ref('qty')
const productSortMode = ref('qty')

// 環形進度條顏色配置
const gaugeColors = [
  { color: '#f56c6c', percentage: 30 },
  { color: '#e6a23c', percentage: 70 },
  { color: '#67c23a', percentage: 100 }
]

// 依據使用者選擇的排序模式回傳前十名原料列表
const displayedTopMaterials = computed(() => {
  return materialSortMode.value === 'count'
    ? stats.value.topMaterialsByCount || []
    : stats.value.topMaterialsByQty || []
})

// 依據使用者選擇的排序模式回傳前十名成品列表
const displayedTopProducts = computed(() => {
  return productSortMode.value === 'count'
    ? stats.value.topProductsByCount || []
    : stats.value.topProductsByQty || []
})

// 取得首頁儀表板統計資料
const fetchStats = async () => {
  loading.value = true
  try {
    const res = await getDashboardStats()
    if (res) {
      stats.value = {
        ...stats.value,
        ...res
      }
    }
  } catch (error) {
    console.error('載入首頁儀表板統計失敗:', error)
    ElMessage.error('載入首頁統計資料失敗，請稍後再試')
  } finally {
    loading.value = false
  }
}

// 頁面路由跳轉輔助函式
const navigateTo = (path) => {
  router.push(path)
}

// 計算出口報單階段佔比 (0~100)
const getExportStagePercentage = (stageCount) => {
  const total =
    Number(stats.value.pendingExportDocCount || 0) +
    Number(stats.value.verifiedUnreportedExportDocCount || 0) +
    Number(stats.value.reportedExportDocCount || 0)
  if (total <= 0) return 0
  return Math.min(100, Math.round((Number(stageCount || 0) / total) * 100))
}

// 計算前十名原料長條圖相對比例 (0~100)
const getMaterialBarPercentage = (item) => {
  const list = displayedTopMaterials.value
  if (!list || list.length === 0) return 0
  const field = materialSortMode.value === 'count' ? 'verifyCount' : 'totalVerifiedQty'
  const maxVal = Math.max(...list.map((x) => Number(x[field] || 0)), 1)
  return Math.min(100, Math.max(4, Math.round((Number(item[field] || 0) / maxVal) * 100)))
}

// 計算前十名成品長條圖相對比例 (0~100)
const getProductBarPercentage = (item) => {
  const list = displayedTopProducts.value
  if (!list || list.length === 0) return 0
  const field = productSortMode.value === 'count' ? 'verifyCount' : 'totalVerifiedQty'
  const maxVal = Math.max(...list.map((x) => Number(x[field] || 0)), 1)
  return Math.min(100, Math.max(4, Math.round((Number(item[field] || 0) / maxVal) * 100)))
}

// 取得前三名與一般名次的徽章樣式
const getRankBadgeClass = (index) => {
  if (index === 0) return 'rank-gold'
  if (index === 1) return 'rank-silver'
  if (index === 2) return 'rank-bronze'
  return 'rank-normal'
}

// 取得排行榜進度條顏色
const getRankBarColor = (index) => {
  if (index === 0) return '#f59e0b'
  if (index === 1) return '#64748b'
  if (index === 2) return '#d97706'
  return '#409eff'
}

// 格式化角色代碼為中文名稱
const formatRoleName = (role) => {
  const roleMap = {
    ROLE_ADMIN: '系統管理員',
    ROLE_OP: '操作人員'
  }
  return roleMap[role] || role
}

// 格式化整數千分位
const formatInteger = (val) => {
  if (val === null || val === undefined) return '0'
  return Number(val).toLocaleString('zh-TW', { maximumFractionDigits: 0 })
}

// 格式化小數千分位（最多保留 3 位小數）
const formatNumber = (val) => {
  if (val === null || val === undefined) return '0'
  return Number(val).toLocaleString('zh-TW', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 3
  })
}

// 格式化日期時間為 YYYY-MM-DD HH:mm
const formatDateTime = (dateVal) => {
  if (!dateVal) return '-'
  const d = new Date(dateVal)
  if (Number.isNaN(d.getTime())) return String(dateVal)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(() => {
  fetchStats()
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 84px);
}

/* 頂部歡迎橫幅 */
.welcome-banner {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  color: #ffffff;
  padding: 20px 24px;
  border-radius: 10px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.12);

  .welcome-left {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .welcome-avatar {
    width: 52px;
    height: 52px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.14);
    display: flex;
    align-items: center;
    justify-content: center;
    color: #60a5fa;
  }

  .welcome-info {
    .welcome-title {
      font-size: 20px;
      font-weight: 600;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
      margin-bottom: 6px;
    }

    .role-tag {
      font-weight: 500;
    }

    .welcome-subtitle {
      font-size: 13px;
      color: #cbd5e1;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 4px;
    }
  }

  .welcome-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
}

/* 第一排 KPI 卡片 */
.kpi-row {
  margin-bottom: 4px;
}

.kpi-card {
  margin-bottom: 16px;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-3px);
  }

  .kpi-body {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .kpi-icon-wrapper {
    width: 60px;
    height: 60px;
    border-radius: 14px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  .kpi-content {
    flex: 1;
    min-width: 0;

    .kpi-label {
      font-size: 14px;
      color: #606266;
      margin-bottom: 6px;
      font-weight: 500;
    }

    .kpi-value {
      font-size: 28px;
      font-weight: 700;
      color: #1f2937;
      line-height: 1.2;
      margin-bottom: 6px;

      .kpi-unit {
        font-size: 14px;
        font-weight: 500;
        color: #6b7280;
        margin-left: 4px;
      }
    }

    .kpi-sub {
      font-size: 12px;
      color: #909399;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;

      strong {
        color: #303133;
      }
    }
  }
}

.kpi-blue {
  border-left: 4px solid #409eff;
  .kpi-icon-wrapper {
    background: rgba(64, 158, 255, 0.12);
    color: #409eff;
  }
}

.kpi-green {
  border-left: 4px solid #67c23a;
  .kpi-icon-wrapper {
    background: rgba(103, 194, 58, 0.12);
    color: #67c23a;
  }
}

.kpi-orange {
  border-left: 4px solid #e6a23c;
  .kpi-icon-wrapper {
    background: rgba(230, 162, 60, 0.14);
    color: #e6a23c;
  }
}

.kpi-purple {
  border-left: 4px solid #8b5cf6;
  .kpi-icon-wrapper {
    background: rgba(139, 92, 246, 0.12);
    color: #8b5cf6;
  }
}

/* 區塊卡片通用樣式 */
.section-row {
  margin-bottom: 4px;
}

.panel-card {
  margin-bottom: 16px;
  border-radius: 10px;
  border: 1px solid #ebeef5;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;

    .card-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }

    .header-count-tag {
      margin-left: 4px;
      font-weight: 500;
    }
  }
}

/* 整體進口核銷率與 BOM 卡片 */
.overview-body {
  display: flex;
  align-items: center;
  gap: 24px;
  min-height: 155px;

  .gauge-box {
    flex-shrink: 0;
    display: flex;
    justify-content: center;
    align-items: center;

    .gauge-value {
      display: block;
      font-size: 22px;
      font-weight: 700;
      color: #1f2937;
    }

    .gauge-label {
      display: block;
      font-size: 12px;
      color: #909399;
      margin-top: 4px;
    }
  }

  .overview-metrics {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 12px;

    .metric-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-bottom: 8px;
      border-bottom: 1px dashed #ebeef5;

      &:last-child {
        border-bottom: none;
        padding-bottom: 0;
      }

      .metric-name {
        font-size: 13px;
        color: #606266;
      }

      .metric-num {
        font-size: 14px;
        font-weight: 600;
        color: #303133;
      }
    }
  }
}

.text-success {
  color: #67c23a !important;
}
.text-warning {
  color: #e6a23c !important;
}
.text-primary {
  color: #409eff !important;
}

/* 出口報單三階段流程卡片 */
.workflow-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  min-height: 155px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
  }

  .workflow-step {
    background: #f8fafc;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    padding: 14px 16px;
    cursor: pointer;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    transition: background-color 0.2s, border-color 0.2s;

    &:hover {
      background: #f1f5f9;
      border-color: #cbd5e1;
    }

    .step-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      .step-badge {
        font-size: 11px;
        padding: 2px 8px;
        border-radius: 10px;
        font-weight: 600;
      }

      .badge-warning {
        background: #fef3c7;
        color: #d97706;
      }

      .badge-primary {
        background: #dbeafe;
        color: #2563eb;
      }

      .badge-success {
        background: #dcfce7;
        color: #16a34a;
      }

      .step-name {
        font-size: 14px;
        font-weight: 600;
        color: #334155;
      }
    }

    .step-count {
      font-size: 14px;
      color: #475569;
      margin-bottom: 8px;

      strong {
        font-size: 22px;
        color: #0f172a;
      }

      .step-items {
        font-size: 12px;
        color: #64748b;
      }
    }

    .step-hint {
      font-size: 12px;
      color: #94a3b8;
      margin-top: 8px;
    }
  }
}

.link-text {
  color: #409eff;
  font-weight: 600;
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
}

/* 前十名排行榜樣式 */
.rank-card {
  .empty-wrapper {
    height: 380px;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .rank-list {
    display: flex;
    flex-direction: column;
    gap: 14px;
    min-height: 380px;
    max-height: 460px;
    overflow-y: auto;
    padding-right: 4px;
  }

  .rank-item {
    display: flex;
    flex-direction: column;
    gap: 6px;
  }

  .rank-row-top {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
  }

  .rank-title-group {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
    flex: 1;

    .rank-badge {
      width: 24px;
      height: 24px;
      border-radius: 6px;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 700;
      flex-shrink: 0;
    }

    .rank-gold {
      background: linear-gradient(135deg, #fbbf24, #f59e0b);
      color: #ffffff;
    }

    .rank-silver {
      background: linear-gradient(135deg, #94a3b8, #64748b);
      color: #ffffff;
    }

    .rank-bronze {
      background: linear-gradient(135deg, #f97316, #d97706);
      color: #ffffff;
    }

    .rank-normal {
      background: #f1f5f9;
      color: #475569;
    }

    .rank-name {
      font-size: 14px;
      font-weight: 600;
      color: #1e293b;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .rank-stats-group {
    display: flex;
    align-items: baseline;
    gap: 10px;
    flex-shrink: 0;

    .primary-stat {
      font-size: 15px;
      font-weight: 700;
      color: #0f172a;

      small {
        font-size: 12px;
        font-weight: 500;
        color: #64748b;
        margin-left: 2px;
      }
    }

    .secondary-stat {
      font-size: 12px;
      color: #64748b;
    }
  }
}
</style>
