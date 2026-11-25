import { defineStore } from 'pinia'
import request from '../utils/request'

/** 登录态与用户信息。用户对象不放密码，后端也不会返回。 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('at_token') || '',
    user: JSON.parse(localStorage.getItem('at_user') || 'null'),
    menus: []
  }),
  getters: {
    isLogin: (s) => !!s.token,
    roleCode: (s) => (s.user && s.user.roleCode) || '',
    roleName: (s) => {
      const m = { admin: '主管部门管理员', patrol: '巡查人员', care: '养护单位', expert: '园林专家' }
      return m[(s.user && s.user.roleCode) || ''] || '用户'
    }
  },
  actions: {
    async login(payload) {
      const data = await request.post('/api/auth/login', payload)
      this.token = data.token
      this.user = data.user
      this.menus = data.menus || []
      localStorage.setItem('at_token', data.token)
      localStorage.setItem('at_user', JSON.stringify(data.user))
      return data
    },
    async fetchMe() {
      const data = await request.get('/api/auth/me')
      this.user = data
      this.menus = data.menus || []
      localStorage.setItem('at_user', JSON.stringify(data))
      return data
    },
    logout() {
      this.token = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('at_token')
      localStorage.removeItem('at_user')
    }
  }
})
