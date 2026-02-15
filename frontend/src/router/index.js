import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

export const constantRoutes = [
    {
        path: '/login',
        component: () => import('@/views/login/index.vue'),
        hidden: true
    },
    {
        path: '/',
        component: Layout,
        redirect: '/dashboard',
        children: [
            {
                path: 'dashboard',
                name: 'Dashboard',
                component: () => import('@/views/dashboard/index.vue'),
                meta: { title: '首頁', icon: 'dashboard' }
            }
        ]
    },
    {
        path: '/system',
        component: Layout,
        redirect: '/system/user',
        name: 'System',
        meta: { title: '系統管理', icon: 'setting' },
        children: [
            {
                path: 'user',
                name: 'User',
                component: () => import('@/views/system/user/index.vue'),
                meta: { title: '使用者管理', permissions: ['USER_VIEW'] }
            },
            {
                path: 'role',
                name: 'Role',
                component: () => import('@/views/system/role/index.vue'),
                meta: { title: '角色管理', permissions: ['ROLE_VIEW'] }
            },
            {
                path: 'function',
                name: 'Function',
                component: () => import('@/views/system/function/index.vue'),
                meta: { title: '功能管理', permissions: ['FUNCTION_VIEW'] }
            }
        ]
    },
    {
        path: '/refund',
        component: Layout,
        redirect: '/refund/standard',
        name: 'Refund',
        meta: { title: '退稅管理', icon: 'money' },
        children: [
            {
                path: 'standard',
                name: 'Standard',
                component: () => import('@/views/refund/standard/index.vue'),
                meta: { title: '核退標準維護', permissions: ['BOM_VIEW'] }
            },
            {
                path: 'import',
                name: 'Import',
                component: () => import('@/views/refund/import/index.vue'),
                meta: { title: '進口報單管理', permissions: ['IMPORT_DECLARATION_VIEW'] }
            },
            {
                path: 'export',
                name: 'Export',
                component: () => import('@/views/refund/export/index.vue'),
                meta: { title: '出口報單管理', permissions: ['EXPORT_DECLARATION_VIEW'] }
            },
            {
                path: 'list',
                name: 'TaxRefundList',
                component: () => import('@/views/refund/list/index.vue'),
                meta: { title: '退稅清單', permissions: ['TAX_REFUND_VIEW'] }
            }
        ]
    },
    {
        path: '/:pathMatch(.*)*',
        redirect: '/404',
        hidden: true
    }
]

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: constantRoutes
})

export default router
