import { defineStore } from 'pinia'
import { login, logout, getInfo } from '@/api/auth'
import { getToken, setToken, removeToken } from '@/utils/auth'
// import { resetRouter } from '@/router'

export const useUserStore = defineStore('user', {
    state: () => ({
        token: getToken() || '',
        name: '',
        avatar: '',
        roles: []
    }),
    actions: {
        // User login
        login(userInfo) {
            const { username, password } = userInfo
            return new Promise((resolve, reject) => {
                login({ username: username.trim(), password: password }).then(response => {
                    // Response is the body directly due to request.js interceptor
                    // Backend returns { accessToken: "...", tokenType: "Bearer" }
                    const { accessToken } = response
                    this.token = accessToken
                    setToken(accessToken)
                    resolve()
                }).catch(error => {
                    reject(error)
                })
            })
        },

        // Get user info
        getInfo() {
            return new Promise((resolve, reject) => {
                getInfo().then(response => {
                    // Backend returns { roles: [], name: "", avatar: "" } directly
                    const { roles, name, avatar } = response

                    if (!roles || roles.length <= 0) {
                        reject('getInfo: roles must be a non-null array!')
                    }
                    this.roles = roles
                    this.name = name
                    this.avatar = avatar
                    resolve(response)
                }).catch(error => {
                    reject(error)
                })
            })
        },

        // User logout
        logout() {
            return new Promise((resolve, reject) => {
                logout(this.token).then(() => {
                    this.token = ''
                    this.roles = []
                    removeToken()
                    // resetRouter()
                    resolve()
                }).catch(error => {
                    reject(error)
                })
            })
        },

        // Remove token
        resetToken() {
            return new Promise(resolve => {
                this.token = ''
                this.roles = []
                removeToken()
                resolve()
            })
        }
    }
})
