import request from '@/utils/request'

export function login(data) {
    return request({
        url: '/auth/login', // Adjust to match backend controller
        method: 'post',
        data
    })
}

// 獲取使用者資訊
export function getInfo() {
    return request({
        url: '/auth/info', // Token 由 request.js 攔截器自動附加
        method: 'get'
    })
}

export function logout() {
    return request({
        url: '/auth/logout', // Adjust to match backend controller
        method: 'post'
    })
}
