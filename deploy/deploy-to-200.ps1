# ==============================================================================
# 進口退稅核銷管理系統 - 自動打包並部署至 192.168.2.200 Docker 腳本
# ==============================================================================
$ErrorActionPreference = "Stop"

$RemoteUser = "gogoffcc"
$RemoteHost = "192.168.2.200"
$RemoteDir  = "/home/gogoffcc/tax-refund"
$RootDir    = Split-Path -Parent $PSScriptRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  進口退稅核銷管理系統 - 自動打包與部署至 $RemoteHost" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. 建置後端 Spring Boot JAR
Write-Host "`n[1/4] 正在打包後端 Spring Boot JAR (跳過測試加速打包)..." -ForegroundColor Yellow
Push-Location "$RootDir\backend\tax"
try {
    .\mvnw.cmd clean package -DskipTests
} finally {
    Pop-Location
}

# 2. 建置前端 Vue 靜態資源
Write-Host "`n[2/4] 正在建置前端 Vue3 正式版靜態檔案 (dist)..." -ForegroundColor Yellow
Push-Location "$RootDir\frontend"
try {
    npm run build
} finally {
    Pop-Location
}

# 3. 上傳檔案至 192.168.2.200
Write-Host "`n[3/4] 正在上傳執行檔與設定至 ${RemoteUser}@${RemoteHost}:${RemoteDir} ..." -ForegroundColor Yellow
ssh -n "${RemoteUser}@${RemoteHost}" "mkdir -p ${RemoteDir}/dist"
scp "$RootDir\deploy\docker-compose.yml" "$RootDir\deploy\nginx.conf" "${RemoteUser}@${RemoteHost}:${RemoteDir}/"
scp "$RootDir\backend\tax\target\tax-0.0.1-SNAPSHOT.jar" "${RemoteUser}@${RemoteHost}:${RemoteDir}/tax-api.jar.new"
scp -r "$RootDir\frontend\dist\*" "${RemoteUser}@${RemoteHost}:${RemoteDir}/dist/"

# 4. 啟動或重啟 Docker 容器
Write-Host "`n[4/4] 正在遠端主機啟動 Docker 容器..." -ForegroundColor Yellow
ssh -n "${RemoteUser}@${RemoteHost}" "cd ${RemoteDir} && sed -i 's/\r$//' docker-compose.yml nginx.conf && mv -f tax-api.jar.new tax-api.jar && docker compose up -d --force-recreate && docker compose ps"

Write-Host "`n==========================================================" -ForegroundColor Green
Write-Host "  部署完成！請開啟瀏覽器造訪： http://${RemoteHost}:9090" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
