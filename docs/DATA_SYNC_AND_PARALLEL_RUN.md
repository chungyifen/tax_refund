# 資料同步與雙軌並行測試手冊 (Data Sync & Parallel Run Guide)

本文件說明舊版退稅系統正式機（`35.194.138.29:3306` / `fox_tax`）與新版退稅系統資料庫（`192.168.2.79:33061` / `fox_refund`）之結構對照、定期自動同步與測試資料重置機制，以及雙軌並行測試（Parallel Run）與正式切換上線（Cutover）之標準作業流程。

---

## 一、 新舊資料庫結構與欄位對照

| 業務資料表 | 舊版正式機 (`35.194.138.29:3306/fox_tax`) | 新版系統 (`192.168.2.79:33061/fox_refund`) | 2026-10-19 基準筆數 | 結構與欄位轉換重點 |
| :--- | :--- | :--- | ---: | :--- |
| **核退標準 (BOM)** | `fox_tax_bom` | `tax_bom` | 531 筆 | 舊版稽核欄位為大寫 (`ID`, `CREATE_TIME`, `CREATOR`, `UPDATE_TIME`, `UPDATOR`)，新版統一轉為小寫。 |
| **進口報單** | `fox_import_declaration` | `import_declaration` | 35,065 筆 | 1. `material_spec` 由舊版 `varchar(20)` 擴充為 `varchar(200)`。<br>2. `total_refund_qty` 已對齊正式機為 `DECIMAL(19,3) NOT NULL DEFAULT 0.000`。<br>3. 新增 `idx_import_dec_create_time` 索引。 |
| **出口報單** | `fox_export_declaration` | `export_declaration` | 136,096 筆 | 新增 `idx_export_dec_create_time` 與 `idx_export_dec_update_time` 索引。 |
| **退稅核銷紀錄** | `fox_tax_refund` | `tax_refund` | 890,085 筆 | 1. 外鍵 `export_declaration_id`、`import_declaration_id`、`tax_bom_id` 100% 完整保留原始 `id` 關聯。<br>2. 新增 `idx_tax_refund_create_time` 索引。 |

> **權限表隔離保護**：新版資料庫中的系統權限表（`sys_user`、`sys_role`、`sys_function`、`sys_user_roles`、`sys_role_functions`）與上述 4 張業務表完全獨立。執行資料同步與重置時，**僅清空並重建 4 張業務表，完全不會影響新版系統的使用者帳號與權限設定**。

---

## 二、 自動化資料同步與測試重置機制

為確保雙軌並行測試期間，操作人員在新版系統的練習、測試匯入或刪除操作不會造成庫存餘額（`total_refund_qty`）長期偏離正式機，已於 `192.168.2.200` 建立定期快照重置機制。

### 1. 腳本位置與組成
* **遠端部署路徑**：`192.168.2.200:/home/gogoffcc/tax-refund-sync/`
  * `sync_prod_to_new.py`（對應版控 [`scripts/sync_prod_to_new.py`](file:///c:/git/tax_Refund/scripts/sync_prod_to_new.py)）
  * `run_sync.sh`（對應版控 [`scripts/run_sync.sh`](file:///c:/git/tax_Refund/scripts/run_sync.sh)）
  * `venv/`（Python 3.10 虛擬環境，含 `pymysql`）
  * `logs/`（執行日誌目錄，`latest.log` 為最近一次執行紀錄，自動保留 90 天內歷史日誌）
* **本機手動執行腳本**：[`sync-prod-to-new.bat`](file:///c:/git/tax_Refund/sync-prod-to-new.bat)

### 2. 安全防護設計
1. **正式機強制唯讀**：腳本連線至 `35.194.138.29` 後立即執行 `SET SESSION TRANSACTION READ ONLY`，且內建目標 IP 防呆檢查（禁止目標指向 `35.194.138.29` 或 `fox_tax`），確保正式機絕對安全。
2. **串流批次高速寫入**：使用 `SSCursor` 串流讀取與每批 5,000 筆批次寫入，同步百萬筆資料僅需約 2.5～3 分鐘，不佔用主機記憶體。

### 3. Crontab 自動排程設定
已於 `192.168.2.200` (`gogoffcc` 帳號) 設定每週一早上 06:00 自動執行：
```cron
# 每週一早上 06:00 自動從正式機 (fox_tax) 同步最新退稅資料至新版系統 (fox_refund) 並重置測試紀錄
0 6 * * 1 /home/gogoffcc/tax-refund-sync/run_sync.sh
```

### 4. 手動觸發與檢查指令
```bash
# 1. 僅預覽正式機與新版資料庫目前筆數差異（不修改任何資料）
ssh -n gogoffcc@192.168.2.200 "/home/gogoffcc/tax-refund-sync/venv/bin/python /home/gogoffcc/tax-refund-sync/sync_prod_to_new.py --dry-run"

# 2. 立即手動觸發一次完整同步（清除新版測試操作並與正式機 100% 對齊）
ssh -n gogoffcc@192.168.2.200 "/home/gogoffcc/tax-refund-sync/run_sync.sh"

# 3. 查看最近一次同步日誌
ssh -n gogoffcc@192.168.2.200 "cat /home/gogoffcc/tax-refund-sync/logs/latest.log"
```

---

## 三、 雙軌並行測試 (Parallel Run) 檢核重點

1. **測試基準對齊**：
   * 每週一早上 06:00 系統會自動將正式機最新資料同步至新版資料庫（或於測試前手動執行一次 `run_sync.sh`）。
2. **雙軌同步操作**：
   * 當週若有新增 BOM、進口報單、出口報單，於舊版正式機操作後，同步於新版系統（`https://testotms.gogoffcc.com/tax_refund/`）匯入並執行「產生退稅清單」。
3. **報表比對重點**：
   * 下載新版系統產出的 **用料清表 (Report L)** 與 **沖退稅申請 (Report A)**，與舊版產出的報表比對：
     * 出口報單項次、成品數量、工業局標準文號是否一致。
     * 扣抵之進口報單號碼 (`doc_no`)、進口項次 (`items`)、申退數量 (`usage_qty`) 是否一致。
4. **已知預期差異說明（跨規格 FIFO 邏輯優化）**：
   * 當 BOM 的 `material_spec` 含有逗號分隔的多個規格（例如 `SPEC-A, SPEC-B`）時：
     * **舊版系統**：採用迴圈逐一規格查詢，會先把 `SPEC-A` 的所有進口報單（包含較晚進口的）全部扣完，才開始扣 `SPEC-B`。
     * **新版系統**：於 [`TaxRefundService.java`](file:///c:/git/tax_Refund/backend/tax/src/main/java/com/fox/tax/modules/refund/service/TaxRefundService.java#L114-L126) 改用 `IN (specs) ORDER BY id ASC`，實現**跨規格整體依進口先後順序先進先出 (True Global FIFO)**。因此遇到多規格原料時，新版扣抵順序更符合真實先進先出原則。

---

## 四、 正式切換上線 (Cutover) 標準步驟

當雙軌並行測試驗證無誤、準備停用舊版並正式切換至新版系統時，請依序執行以下步驟：

1. **停用舊版退稅寫入**：通知 OP 人員停止在舊版系統匯入報單或產生退稅清單。
2. **執行最終一次全量資料同步**：
   ```bash
   ssh -n gogoffcc@192.168.2.200 "/home/gogoffcc/tax-refund-sync/run_sync.sh"
   ssh -n gogoffcc@192.168.2.200 "cat /home/gogoffcc/tax-refund-sync/logs/latest.log"
   ```
   確認 4 張業務表筆數皆顯示 `[OK]` 與正式機 100% 一致。
3. **移除每週重置排程（關鍵步驟！）**：
   正式切換上線後，新版資料庫即成為唯一正式庫，**務必停用每週一自動覆蓋的 Crontab 排程**，避免新版正式營運資料被舊庫覆蓋：
   ```bash
   ssh -n gogoffcc@192.168.2.200 "crontab -l | grep -v 'tax-refund-sync' | crontab -"
   ```
4. **正式啟用新版系統**：通知 OP 人員全面改由新版系統進行日常退稅作業。
