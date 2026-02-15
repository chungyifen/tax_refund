# Agent.md - 進口退稅核銷管理系統開發指南

## 1. 專案概述
**專案名稱**：進口退稅核銷管理系統 (Tax Refund Management System)
**目標**：協助 OP 人員進行進出口報單管理、BOM 表維護，並自動計算退稅核銷金額。
**核心功能**：
1. RBAC 權限管理 (角色、功能授權)。
2. 退稅標準 (BOM) 維護 (含 Excel 匯入)。
3. 進出口報單上傳與維護。
4. 退稅核銷計算與報表產出。

---

## 2. 技術堆疊 (Tech Stack)

### 後端 (Backend)
* **語言**：Java 17+
* **框架**：Spring Boot 3.x
* **資料庫**：MySQL 8.0
* **ORM**：Spring Data JPA
* **安全**：Spring Security + JWT (Stateless)
* **工具**：Lombok, Apache POI (Excel處理), Maven
* **測試**：JUnit 5, Mockito

### 前端 (Frontend)
* **框架**：Vue.js 3 (Composition API)
* **建構工具**：Vite
* **UI 庫**：Element Plus
* **狀態管理**：Pinia
* **HTTP 客戶端**：Axios
* **路由**：Vue Router

---

## 3. 系統架構與目錄規範

採用 **依據模組分 (Package by Feature)** 的架構設計，確保高內聚低耦合。

### 後端結構 (`backend/src/main/java/com/fox/tax`)
com.fox.tax
├── common/                  # 共用層 (全域設定、工具)
│   ├── config/              # SecurityConfig, SwaggerConfig, DataInitializer
│   ├── exception/           # GlobalExceptionHandler
│   ├── utils/               # JwtTokenProvider, ExcelUtils
│   └── entity/              # AbstractPersistable (Base Entity)
│
└── modules/                 # 業務模組層
    ├── rbac/                # [已完成] 權限模組 (User, Role, Auth)
    │   ├── controller/      # AuthController, UserController
    │   ├── service/         # UserDetailsServiceImpl
    │   ├── repository/
    │   └── entity/          # User, Role, Function
    │
    ├── refund/              # [進行中] 退稅核心模組 (BOM, 報單)
    │   ├── controller/      # TaxBomController [已完成]
    |   |                    # ImportDeclarationController   
    │   ├── service/         # TaxBomService [已完成]
    |   |                    # ImportDeclarationService
    │   ├── repository/      # TaxBomRepository [已完成]
    │   |                    # ImportDeclarationRepository
    │   ├── entity/          # TaxBom (退稅標準) [已完成]
    │   |                    # ImportDeclaration(進口報單)
    │   |                    # TaxRefund
    │   |                    # ExportDeclaration
    │   ├── dto/             # TaxBomDto (API 傳輸物件) [已完成]
    │   |                    # ImportDeclarationDto (API 傳輸物件)
    └── operation/           # [待開發] 日常作業 (進出口報單上傳)

### 前端結構 (`frontend/src`)

    frontend/src/
            ├── api/             # API 接口管理 (auth.js, bom.js)
            ├── components/      # 共用元件 (UploadExcel.vue)
            ├── layout/          # 系統佈局
            ├── stores/          # Pinia 狀態 (user.js, permission.js)
            ├── utils/           # request.js (Axios 攔截器), validate.js
            └── views/           # 頁面視圖
                ├── login/       # 登入頁
                ├── dashboard/   # 首頁
                ├── refund/      # 退稅管理
                │   └── bom/     # BOM 維護頁
                └── system/      # 系統管理 (帳號/角色)

---

## 4. 模程式碼開發規範 (Coding Standards)

### 後端開發原則
1. DTO 模式：Controller 禁止直接回傳 Entity，必須透過 DTO 轉換（避免循環參照與安全問題）。
2. Service 層邏輯：業務邏輯（如檢查重複、計算）一律寫在 Service，Controller 僅負責接收請求與驗證格式。
3. 安全性：
    - 密碼必須經過 BCrypt 加密。
    - API 需加上 @PreAuthorize 進行權限控管。
4. 檔案上傳需檢查檔案類型與大小。
5. 例外處理：使用全域異常處理器，回傳統一的 JSON 格式。
6. Excel 處理：使用 Apache POI，讀取時需考慮記憶體效能（使用 XSSFWorkbook 或串流讀取）。

### 前端開發原則
1. Composition API：全面使用 <script setup> 語法糖。
2. API 管理：所有後端請求需封裝在 src/api 資料夾中，禁止在 Vue 檔中直接寫 axios.get(...)。
3. 權限控制：
    - 路由層：使用 Router Guard (beforeEach) 檢查 Token。
    - 元件層：使用 Custom Directive (如 v-permission="['BOM_EDIT']") 控制按鈕顯示。
    - 元件庫：優先使用 Element Plus 提供的組件，保持介面風格一致。
4. 狀態管理：使用 Pinia 管理全域狀態，禁止直接操作 DOM。
5. HTTP 客戶端：使用 Axios，並添加請求攔截器進行 Token 檢查。
6. 驗證：使用 Element Plus 的表單驗證，並統一處理驗證失敗的提示。
7. 基於Vue 3 + Element Plus的設計風格
8. 系統一律使用中文

### 資料庫設計重點
1. 稽核欄位：所有主要表格需繼承 AbstractPersistable (或包含 createTime, updateTime, createBy, updateBy)。
2. 索引 (Index)：針對查詢頻繁的欄位（如 docNo, productCode, declarationNo）建立索引。
3. 關聯性：
    - TaxBom (1) <-> (N) TaxRefund
    - User (N) <-> (N) Role

### 目前開發進度 (Current Status)
1. 系統初始化：專案架構搭建、MySQL 連線配置。
2. RBAC 模組：
    - 使用者/角色 Entity 與 Repository。
    - Spring Security + JWT 認證流程 (JwtTokenProvider, JwtAuthenticationFilter)。
    - 登入 API (AuthController) 與資料初始化 (DataInitializer)。
3. 退稅標準模組 (Tax Refund Standard)：
    - TaxRefundStandard Entity 與 DTO 設計 [已完成]。
    - Excel 批次匯入功能 (含 Upsert 邏輯與錯誤報告) [已完成]。
    - CRUD API 實作 [已完成]。
4. 進出口報單模組：待開發。
5. 退稅核銷計算：待開發。
6. 前端頁面整合：待開發。

## 5. 協作指令 (Prompting Guide)

### 新增功能時：請先定義 Entity 與 Repository，再建立 Service 邏輯，最後實作 Controller 與 DTO。
### 修改程式碼時：請引用現有的檔案內容，並說明修改原因。
### 遇到 Bug 時：請提供 Error Log 與相關程式碼片段。