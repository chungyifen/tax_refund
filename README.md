# 進口退稅核銷管理系統 (Tax Refund Management System)

協助業務與 OP 人員管理工業局核退標準（BOM）、進口報單、出口報單，並依據先進先出（FIFO）原則自動計算退稅核銷數量、產出向海關／工業局申報之法定報表（**外銷品使用原料及其供應商資料清表 L 表**、**外銷品沖退原料稅申請書 A 表**）。

---

## 📚 文件導覽 (Documentation Index)

| 文件名稱 | 說明 |
| :--- | :--- |
| [`README.md`](file:///c:/git/tax_Refund/README.md) | 專案總覽、本機快速啟動與環境說明（本文件） |
| [`docs/DEPLOYMENT_AND_OPERATIONS.md`](file:///c:/git/tax_Refund/docs/DEPLOYMENT_AND_OPERATIONS.md) | **部署與維運手冊**：`192.168.2.200` Docker 部署架構、Nginx `/tax_refund/` 子路徑反向代理、一鍵發布腳本、CI/CD 設定 |
| [`docs/DATA_SYNC_AND_PARALLEL_RUN.md`](file:///c:/git/tax_Refund/docs/DATA_SYNC_AND_PARALLEL_RUN.md) | **資料同步與雙軌並行測試手冊**：新舊資料庫 (`fox_tax` vs `fox_refund`) 結構對照、`192.168.2.200` 每週自動同步排程、並行測試檢核與正式切換 (Cutover) SOP |
| [`docs/OPTIMIZATION_CHANGELOG_20261019.md`](file:///c:/git/tax_Refund/docs/OPTIMIZATION_CHANGELOG_20261019.md) | **系統架構與百萬筆資料效能優化紀錄**：跨規格 FIFO 悲觀鎖、N+1 查詢消除、百萬筆資料儀表板 SQL 調校與前端大量 DOM 渲染優化 |
| [`Agent.md`](file:///c:/git/tax_Refund/Agent.md) / [`CLAUDE.md`](file:///c:/git/tax_Refund/CLAUDE.md) | 專案開發規範與 AI 輔助開發指引 |

---

## 🛠️ 技術堆疊 (Tech Stack)

* **後端 (`backend/tax/`)**：Java 17+、Spring Boot 3.2、Spring Data JPA、QueryDSL 5、Spring Security + JWT、MapStruct、Apache POI、Maven
* **前端 (`frontend/`)**：Vue 3 (`<script setup>`)、Vite 7、Element Plus、Pinia、Vue Router、Axios
* **資料庫**：MySQL 8.0 (`fox_refund`)
* **容器化部署 (`deploy/`)**：Docker Compose (`eclipse-temurin:21-jre` + `nginx:alpine`)

---

## 🚀 快速啟動與存取資訊

### 1. 測試／並行環境 (Ubuntu `192.168.2.200`)
* **外部網域網址**：`https://testotms.gogoffcc.com/tax_refund/`
* **內網容器網址**：`http://192.168.2.200:9090/tax_refund/`
* **預設管理員帳號**：`admin` / `admin123`

### 2. 本機開發環境一鍵啟動 (Windows)
於專案根目錄直接執行啟動腳本（將自動開啟後端 `:8080` 與前端 `:5173` 兩個視窗）：
```powershell
# 使用批次檔啟動
.\start-dev.bat

# 或使用 PowerShell 啟動
powershell -ExecutionPolicy Bypass -File .\start-dev.ps1
```

### 3. 常用建置與測試指令
```bash
# 後端單元與整合測試 (使用 H2 記憶體資料庫)
cd backend/tax
.\mvnw.cmd test

# 後端打包 JAR
.\mvnw.cmd clean package -DskipTests

# 前端正式環境打包
cd ../../frontend
npm run build
```
