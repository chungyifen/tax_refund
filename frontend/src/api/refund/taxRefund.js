import request from '@/utils/request'

/**
 * 搜尋出口報單列表 (退稅清單功能)
 * params: { docNo, status, page, size }
 */
export function searchExportList(params) {
    return request({
        url: '/refund/tax-refund/list',
        method: 'get',
        params
    })
}

/**
 * 更新退稅核銷數量
 */
export function updateRefundQty(id, usageQty) {
    return request({
        url: '/refund/tax-refund/' + id,
        method: 'put',
        data: { usageQty }
    })
}

/**
 * 依據出口報單 ID 查詢退稅清單
 */
export function getRefundListByExportId(exportId) {
    return request({
        url: '/refund/tax-refund/export-declaration/' + exportId,
        method: 'get'
    })
}

/**
 * 導出用料清表 (Report L)
 */
export function exportReportL(docNo) {
    return request({
        url: '/refund/tax-refund/export/L',
        method: 'get',
        params: { docNo },
        responseType: 'blob'
    })
}

/**
 * 導出沖退稅申請 (Report A)
 */
export function exportReportA(docNo) {
    return request({
        url: '/refund/tax-refund/export/A',
        method: 'get',
        params: { docNo },
        responseType: 'blob'
    })
}
