import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken } from '@/utils/auth'

// 建立 Axios 實例
const service = axios.create({
    baseURL: '/api', // Proxy target
    timeout: 5000
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

// 回應攔截器
service.interceptors.response.use(
    response => {
        // 直接回傳 response.data (即 backend JSON body)
        const res = response.data
        return res
    },
    error => {
        console.log('err' + error)
        ElMessage({
            message: error.message || 'Request Error',
            type: 'error',
            duration: 5 * 1000
        })
        return Promise.reject(error)
    }
)

export default service
