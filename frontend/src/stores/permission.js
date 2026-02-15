import { defineStore } from 'pinia'
import { constantRoutes } from '@/router'

// Filter asynchronous routing tables by recursion
// export function filterAsyncRoutes(routes, roles) {
//   const res = []
//   routes.forEach(route => {
//     const tmp = { ...route }
//     if (hasPermission(roles, tmp)) {
//       if (tmp.children) {
//         tmp.children = filterAsyncRoutes(tmp.children, roles)
//       }
//       res.push(tmp)
//     }
//   })
//   return res
// }

export const usePermissionStore = defineStore('permission', {
    state: () => ({
        routes: [],
        addRoutes: []
    }),
    actions: {
        generateRoutes(roles) {
            return new Promise(resolve => {
                // let accessedRoutes
                // if (roles.includes('admin')) {
                //   accessedRoutes = asyncRoutes || []
                // } else {
                //   accessedRoutes = filterAsyncRoutes(asyncRoutes, roles)
                // }
                // this.addRoutes = accessedRoutes
                // this.routes = constantRoutes.concat(accessedRoutes)
                // resolve(accessedRoutes)

                // For now just return constantRoutes until dynamic routes are implemented
                this.routes = constantRoutes
                resolve(constantRoutes)
            })
        }
    }
})
