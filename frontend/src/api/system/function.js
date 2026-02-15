import request from '@/utils/request'

export function getFunctionList(params) {
    return request({
        url: '/functions',
        method: 'get',
        params
    })
}

export function createFunction(data) {
    return request({
        url: '/functions',
        method: 'post',
        data
    })
}

export function updateFunction(id, data) {
    return request({
        url: '/functions/' + id,
        method: 'put',
        data
    })
}

export function deleteFunction(id) {
    return request({
        url: '/functions/' + id,
        method: 'delete'
    })
}
