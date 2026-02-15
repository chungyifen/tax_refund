import request from '@/utils/request'

export function getTaxBomList(params) {
    return request({
        url: '/refund/bom',
        method: 'get',
        params
    })
}

export function createTaxBom(data) {
    return request({
        url: '/refund/bom',
        method: 'post',
        data
    })
}

export function updateTaxBom(id, data) {
    return request({
        url: '/refund/bom/' + id,
        method: 'put',
        data
    })
}

export function deleteTaxBom(id) {
    return request({
        url: '/refund/bom/' + id,
        method: 'delete'
    })
}

export function deleteTaxBomBatch(ids) {
    return request({
        url: '/refund/bom/batch',
        method: 'delete',
        data: ids
    })
}

export function importTaxBom(data) {
    return request({
        url: '/refund/bom/import',
        method: 'post',
        data,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}
