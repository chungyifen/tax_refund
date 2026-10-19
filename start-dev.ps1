# 進口退稅核銷管理系統 - 本機服務啟動程序 (PowerShell)
$Host.UI.RawUI.WindowTitle = "啟動本機測試服務"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$rootDir = Split-Path -Parent $MyInvocation.MyCommand.Path

Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host "   進口退稅核銷管理系統 - 本機服務啟動程序 (PowerShell)" -ForegroundColor Cyan
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "連線資料庫目標: 192.168.2.79:33061 (MySQL Schema: fox_refund)" -ForegroundColor Yellow
Write-Host "後端服務端口: http://localhost:8080" -ForegroundColor Gray
Write-Host "前端服務端口: http://localhost:5173" -ForegroundColor Gray
Write-Host ""
Write-Host "預設登入帳號: admin" -ForegroundColor Green
Write-Host "預設登入密碼: admin123" -ForegroundColor Green
Write-Host "======================================================================" -ForegroundColor Cyan
Write-Host ""

# 啟動後端
Write-Host "[1/2] 正在新視窗啟動後端 Spring Boot 服務..." -ForegroundColor Yellow
$backendPath = Join-Path $rootDir "backend\tax"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$backendPath'; Write-Host '啟動後端服務中...' -ForegroundColor Cyan; .\mvnw.cmd spring-boot:run"

# 啟動前端
Write-Host "[2/2] 正在新視窗啟動前端 Vite 開發伺服器..." -ForegroundColor Yellow
$frontendPath = Join-Path $rootDir "frontend"
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$frontendPath'; Write-Host '啟動前端服務中...' -ForegroundColor Cyan; npm run dev"

Write-Host ""
Write-Host "兩個服務已在新視窗啟動！" -ForegroundColor Green
Write-Host "請稍候後端啟動完成後，開啟瀏覽器造訪: http://localhost:5173" -ForegroundColor White
Write-Host ""
Read-Host "按 Enter 鍵關閉此引導視窗 (前後端服務視窗將保持執行)"
