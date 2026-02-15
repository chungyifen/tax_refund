import request from '@/utils/request'

export function getUserList(params) {
    return request({
        url: '/users',
        method: 'get',
        params
    })
}

export function getUser(id) {
    return request({
        url: '/users/' + id,
        method: 'get'
    })
}

export function createUser(data) {
    return request({
        url: '/users',
        method: 'post',
        data
    })
}

export function updateUser(id, data) {
    return request({
        url: '/users/' + id,
        method: 'put',
        data
    })
}

// 變更密碼
export function changePassword(id, newPassword) {
    return request({
        url: '/users/' + id + '/password',
        method: 'put',
        data: { newPassword }
    })
}

export function deleteUser(id) {
    return request({
        url: '/users/' + id,
        method: 'delete'
    })
}
