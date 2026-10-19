# 系統架構與百萬筆資料效能優化紀錄 (2026-10-19)

本文件記錄新版「進口退稅核銷管理系統」於上線前完成之核心業務邏輯修正、架構重構，以及匯入正式機 **1,061,777 筆**真實資料後之效能調校成果。

---

## 一、 核心業務邏輯與資料一致性優化

1. **跨規格先進先出 (FIFO) 與悲觀鎖防超額扣抵**
   * **問題**：舊版系統在處理 BOM 多規格原料（以逗號分隔）時採逐一規格迴圈查詢，導致未能跨規格依時間先進先出，且多人同時執行核銷時可能發生超額扣抵（Race Condition）。
   * **解決方案**：
     * 於 [`ImportDeclarationRepository.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/repository/ImportDeclarationRepository.java#L29-L31) 新增帶有 `@Lock(LockModeType.PESSIMISTIC_WRITE)` 的 `findByMaterialNameAndMaterialSpecInOrderByIdAsc` 方法。
     * 於 [`TaxRefundService.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/service/TaxRefundService.java#L114-L126) 改為單次 `IN` 查詢，確保跨規格依匯入順序 (`id ASC`) 扣抵並鎖定庫存列。
2. **欄位精度對齊正式機 (`DECIMAL(19,3)`)**
   * **問題**：正式機 `fox_import_declaration.total_refund_qty` 為 `decimal(19,3)`（目前單筆最大達 `655,000.000`），而新版原設定為 `numeric(9,3)`（上限 `999,999.999`），存在大額進口報單累計核銷溢位風險。
   * **解決方案**：將 [`ImportDeclaration.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/entity/ImportDeclaration.java#L67-L73) 及新版資料庫 `total_refund_qty` 欄位精度升級為 `DECIMAL(19,3)`。

---

## 二、 百萬筆正式機資料效能調校 (Performance Tuning)

將正式機全量資料（`tax_bom` 531 筆、`import_declaration` 35,065 筆、`export_declaration` 136,096 筆、`tax_refund` 890,085 筆，共 **1,061,777 筆**）同步至新版資料庫後，針對實測瓶頸完成以下調校：

| 優化項目 | 調校前表現 | 調校後表現 | 優化作法與相關檔案 |
| :--- | :--- | :--- | :--- |
| **首頁儀表板統計 API**<br>(`/api/refund/dashboard/stats`) | **17.25 秒**<br>(觸發前端 5 秒 Timeout 報錯) | **1.09 秒** (SQL)<br>**2.50 秒** (含網路) | 1. 移除 [`ExportDeclarationRepository.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/repository/ExportDeclarationRepository.java#L43-L48) 中的 `OR EXISTS` 關聯子查詢，改於 [`DashboardService.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/service/DashboardService.java#L153-L185) 記憶體合併去重。<br>2. 將 [`TaxRefundRepository.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/repository/TaxRefundRepository.java#L35-L56) 前十名核銷原料查詢改由已維護 `total_refund_qty` 的 `ImportDeclaration` (3.5 萬筆) 直接聚合，避開 89 萬筆退稅明細 `JOIN` 全表掃描。<br>3. 為 `import_declaration`、`export_declaration`、`tax_refund` 補齊 `create_time` 與 `update_time` 索引。 |
| **進出口報單號碼下拉選單**<br>(`/refund/import`, `/refund/export`) | 渲染 **19,012 個** `<el-option>` DOM 節點導致瀏覽器卡死 | **每次僅渲染 100 筆**<br>(0 延遲秒開) | 於 [`import/index.vue`](file:///c:/git/tax_Refund/frontend/src/views/refund/import/index.vue#L216-L238) 與 [`export/index.vue`](file:///c:/git/tax_Refund/frontend/src/views/refund/export/index.vue#L202-L225) 實作 `:filter-method` 前端限量渲染，預設顯示最新前 100 筆，輸入關鍵字時即時檢索全部 9,506 / 5,727 筆報單號碼。 |
| **報表匯出 (Report L / A) 與退稅查詢** | N+1 Lazy Loading 查詢 | 單次 `JOIN` 載入 | 於 [`ExportDeclarationRepository.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/repository/ExportDeclarationRepository.java#L27-L34) 與 [`TaxRefundRepository.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/repository/TaxRefundRepository.java#L16-L28) 加上 `@EntityGraph`。 |
| **退稅清單頁面預設排序**<br>(`/refund/list`) | 未指定排序，第一頁顯示 6 年前最舊報單 | 預設依 `docNo DESC` 顯示最新報單 | 於 [`TaxRefundController.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/controller/TaxRefundController.java#L75-L79) 加上 `@PageableDefault(size = 20, sort = "docNo", direction = Sort.Direction.DESC)`。 |

---

## 三、 後端架構與前端體驗完善

1. **全域異常處理器 ([`GlobalExceptionHandler.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/common/exception/GlobalExceptionHandler.java))**：統一攔截業務與參數驗證例外並回傳標準 JSON 格式，前端 [`request.js`](file:///c:/git/tax_Refund/frontend/src/utils/request.js) 自動提取後端 `message` 顯示清楚的中文錯誤提示，並將請求逾時由 5 秒放寬至 30 秒。
2. **全面落實 DTO 模式 ([`TaxRefundDto.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/dto/TaxRefundDto.java)、[`TaxRefundMapper.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/mapper/TaxRefundMapper.java))**：移除 Controller 直接回傳 Entity 的做法，並在轉換時自動計算進口報單剩餘可核銷量 (`unverifiedQty`)，於前端「修改退稅清單」視窗即時呈現。
3. **子路徑部署支援 (`/tax_refund/`)**：前端 [`vite.config.js`](file:///c:/git/tax_Refund/frontend/vite.config.js) 與 [`request.js`](file:///c:/git/tax_Refund/frontend/src/utils/request.js) 支援 `/tax_refund/` 子路徑部署，搭配容器內 [`deploy/nginx.conf`](file:///c:/git/tax_Refund/deploy/nginx.conf) 可無縫掛載於主機 Nginx 反向代理下。
