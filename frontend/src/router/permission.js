import router from './index'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress' // 進度條
import 'nprogress/nprogress.css' // 進度條樣式
import { getToken } from '@/utils/auth'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login'] // 免登入白名單

router.beforeEach(async (to, from, next) => {
    NProgress.start()

    // 設定頁面標題
    document.title = to.meta.title || '退稅管理系統'

    const hasToken = getToken()
    console.log('[Permission Guard] to:', to.path, 'hasToken:', !!hasToken)

    if (hasToken) {
        if (to.path === '/login') {
            // 已登入，重導向到首頁
            console.log('[Permission Guard] Already logged in, redirect to /')
            next({ path: '/' })
            NProgress.done()
        } else {
            const userStore = useUserStore()
            const hasRoles = userStore.roles && userStore.roles.length > 0
            console.log('[Permission Guard] hasRoles:', hasRoles, 'roles:', userStore.roles)
            if (hasRoles) {
                next()
            } else {
                try {
                    // 獲取使用者資訊
                    console.log('[Permission Guard] Fetching user info...')
                    const info = await userStore.getInfo()
                    console.log('[Permission Guard] getInfo success:', info)
                    next({ ...to, replace: true })
                } catch (error) {
                    // 獲取失敗，清除 token 並跳轉登入頁
                    console.error('[Permission Guard] getInfo failed:', error)
                    await userStore.resetToken()
                    ElMessage.error(typeof error === 'string' ? error : (error?.message || '驗證失敗，請重新登入'))
                    next(`/login?redirect=${to.path}`)
                    NProgress.done()
                }
            }
        }
    } else {
        if (whiteList.indexOf(to.path) !== -1) {
            next()
        } else {
            next(`/login?redirect=${to.path}`)
            NProgress.done()
        }
    }
})

router.afterEach(() => {
    NProgress.done()
})
