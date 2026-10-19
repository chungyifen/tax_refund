@echo off
chcp 65001 > nul
setlocal enabledelayedexpansion

echo ======================================================================
echo    進口退稅核銷管理系統 - 本機服務啟動程序
echo ======================================================================
echo.
echo 連線資料庫目標: 192.168.2.79:33061 (MySQL Schema: fox_refund)
echo 後端服務端口: http://localhost:8080
echo 前端服務端口: http://localhost:5173
echo.
echo 預設登入帳號: admin
echo 預設登入密碼: admin123
echo ======================================================================
echo.

REM 取得當前目錄
set "ROOT_DIR=%~dp0"

echo [1/2] 正在獨立視窗啟動後端 Spring Boot 服務...
start "Tax Refund Backend (Port: 8080)" cmd /k "cd /d "%ROOT_DIR%backend\tax" && mvnw.cmd spring-boot:run"

echo [2/2] 正在獨立視窗啟動前端 Vite 開發伺服器...
start "Tax Refund Frontend (Port: 5173)" cmd /k "cd /d "%ROOT_DIR%frontend" && npm run dev"

echo.
echo 兩個服務已於獨立終端視窗中啟動！
echo.
echo - 請稍候約 10~20 秒待後端啟動完成 (出現 Started TaxApplication 即表示就緒)。
echo - 啟動完成後，即可在瀏覽器開啟: http://localhost:5173
echo.
echo 按任意鍵關閉此引導視窗 (後端與前端視窗將保持執行)...
pause > nul
