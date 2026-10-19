#!/usr/bin/env bash
# ==============================================================================
# 進口退稅核銷管理系統 - 正式機 (fox_tax) 同步至 新版系統 (fox_refund) 排程執行腳本
# 部署位置：192.168.2.200:/home/gogoffcc/tax-refund-sync/run_sync.sh
# 用途：供 Ubuntu Crontab 定期呼叫，執行資料同步並記錄完整執行日誌
# ==============================================================================

set -euo pipefail

BASE_DIR="/home/gogoffcc/tax-refund-sync"
VENV_PYTHON="${BASE_DIR}/venv/bin/python"
SCRIPT_PATH="${BASE_DIR}/sync_prod_to_new.py"
LOG_DIR="${BASE_DIR}/logs"

mkdir -p "${LOG_DIR}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
LOG_FILE="${LOG_DIR}/sync_${TIMESTAMP}.log"
LATEST_LOG="${LOG_DIR}/latest.log"

{
    echo "========================================================================"
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 開始執行退稅系統資料同步排程"
    echo "========================================================================"
    "${VENV_PYTHON}" "${SCRIPT_PATH}" --yes
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] 退稅系統資料同步排程執行成功"
} > "${LOG_FILE}" 2>&1

# 更新最新日誌連結，方便快速檢視
cp -f "${LOG_FILE}" "${LATEST_LOG}"

# 自動清理超過 90 天的歷史日誌檔
find "${LOG_DIR}" -maxdepth 1 -name "sync_*.log" -type f -mtime +90 -delete
