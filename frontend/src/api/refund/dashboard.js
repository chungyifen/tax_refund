// 登入首頁儀表板統計 API 模組
import request from '@/utils/request'

/**
 * 取得登入首頁儀表板統計資訊
 * 包含：本周新增進口報單、本周新增核銷出口報單、未核銷進口報單數目、
 * 核銷前十名原料與前十名成品，以及作業進度概況。
 */
export function getDashboardStats() {
  return request({
    url: '/refund/dashboard/stats',
    method: 'get'
  })
}
