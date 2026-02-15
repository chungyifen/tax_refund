import request from '@/utils/request'

export function searchExportDeclarations(params) {
    return request({
        url: '/refund/export-declaration',
        method: 'get',
        params
    })
}

export function getDocNos() {
    return request({
        url: '/refund/export-declaration/doc-nos',
        method: 'get'
    })
}

export function updateExportDeclaration(id, data) {
    return request({
        url: '/refund/export-declaration/' + id,
        method: 'put',
        data
    })
}

export function deleteExportDeclaration(id) {
    return request({
        url: '/refund/export-declaration/' + id,
        method: 'delete'
    })
}

export function batchDeleteExportDeclarations(ids) {
    return request({
        url: '/refund/export-declaration/batch-delete',
        method: 'post',
        data: ids
    })
}

export function importExcel(data) {
    return request({
        url: '/refund/export-declaration/import',
        method: 'post',
        data,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

export function exportExcel(params) {
    return request({
        url: '/refund/export-declaration/export',
        method: 'get',
        params,
        responseType: 'blob'
    })
}

/**
 * 產生退稅清單
 * @param {string} docNo 出口報單號碼
 */
export function generateRefundList(docNo) {
    return request({
        url: '/refund/tax-refund/generate',
        method: 'post',
        params: { docNo }
    })
}

