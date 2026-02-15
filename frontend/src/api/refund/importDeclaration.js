// 進口報單管理 API 模組
import request from '@/utils/request'

/**
 * 搜尋進口報單（分頁）
 * @param {Object} params - 查詢參數 { docNo, materialName, status, page, size }
 */
export function searchImportDeclarations(params) {
    return request({
        url: '/refund/import-declaration',
        method: 'get',
        params
    })
}

/**
 * 取得所有不重複的進口報單號碼（auto-complete 下拉選單用）
 */
export function getDocNos() {
    return request({
        url: '/refund/import-declaration/doc-nos',
        method: 'get'
    })
}

/**
 * 更新進口報單
 * @param {Number} id - 報單 ID
 * @param {Object} data - 更新資料
 */
export function updateImportDeclaration(id, data) {
    return request({
        url: '/refund/import-declaration/' + id,
        method: 'put',
        data
    })
}

/**
 * 刪除進口報單
 * @param {Number} id - 報單 ID
 */
export function deleteImportDeclaration(id) {
    return request({
        url: '/refund/import-declaration/' + id,
        method: 'delete'
    })
}

/**
 * 批次刪除進口報單
 * @param {Array<Number>} ids - 報單 ID 列表
 */
export function batchDeleteImportDeclarations(ids) {
    return request({
        url: '/refund/import-declaration/batch-delete',
        method: 'post',
        data: ids
    })
}

/**
 * 匯入進口報單 Excel
 * @param {FormData} data - 包含 file 的 FormData
 */
export function importExcel(data) {
    return request({
        url: '/refund/import-declaration/import',
        method: 'post',
        data,
        headers: {
            'Content-Type': 'multipart/form-data'
        }
    })
}

/**
 * 下載搜尋結果 Excel
 * @param {Object} params - 查詢參數 { docNo, materialName, status }
 */
export function exportExcel(params) {
    return request({
        url: '/refund/import-declaration/export',
        method: 'get',
        params,
        responseType: 'blob'
    })
}
