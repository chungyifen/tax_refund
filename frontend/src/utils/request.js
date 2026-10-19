import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'

// 建立 Axios 實例（baseURL 自動帶入 Vite 子路徑 /tax_refund/api，並將逾時放寬至 30 秒）
const service = axios.create({
    baseURL: `${import.meta.env.BASE_URL}api`,
    timeout: 30000
})

// 請求攔截器 - 自動附加 JWT Token
service.interceptors.request.use(
    config => {
        // 使用統一的 getToken 方法讀取 token
        const token = getToken()
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        return config
    },
    error => {
        console.log(error)
        return Promise.reject(error)
    }
)

// 回應攔截器 - 統一處理回應資料與後端業務錯誤訊息
service.interceptors.response.use(
  response => {
    // 直接回傳 response.data (即後端 JSON body 或 Blob)
    return response.data
  },
  error => {
    console.error('API 請求錯誤:', error)
    // 優先讀取後端 GlobalExceptionHandler 回傳的 message 欄位
    const backendMessage =
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.message ||
      '請求發生錯誤'
    ElMessage({
      message: backendMessage,
      type: 'error',
      duration: 5 * 1000
    })
    return Promise.reject(error)
  }
)

export default service
