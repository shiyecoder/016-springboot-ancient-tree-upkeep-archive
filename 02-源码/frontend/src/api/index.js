import request from '../utils/request'

/** 接口统一出口，页面不直接写 URL */
export const authApi = {
  me: () => request.get('/api/auth/me'),
  profile: (d) => request.put('/api/auth/profile', d),
  password: (d) => request.put('/api/auth/password', d),
  register: (d) => request.post('/api/auth/register', d)
}

export const treeApi = {
  page: (p) => request.get('/api/ancient-tree/page', { params: p }),
  detail: (id) => request.get(`/api/ancient-tree/${id}`),
  create: (d) => request.post('/api/ancient-tree', d),
  update: (id, d) => request.put(`/api/ancient-tree/${id}`, d),
  remove: (id) => request.delete(`/api/ancient-tree/${id}`),
  change: (id, d) => request.post(`/api/ancient-tree/${id}/change`, d),
  audit: (vid, pass, remark) => request.post(`/api/ancient-tree/version/${vid}/audit`, null, { params: { pass, remark } }),
  transition: (id, d) => request.post(`/api/ancient-tree/${id}/transition`, d),
  publicList: (p) => request.get('/api/ancient-tree/public/list', { params: p }),
  dashboard: (days = 30) => request.get('/api/ancient-tree/dashboard', { params: { days } })
}

export const patrolApi = {
  planPage: (p) => request.get('/api/patrol/plan/page', { params: p }),
  savePlan: (d, id) => (id ? request.put(`/api/patrol/plan/${id}`, d) : request.post('/api/patrol/plan', d)),
  recordPage: (p) => request.get('/api/patrol/record/page', { params: p }),
  submit: (d) => request.post('/api/patrol/record', d),
  issuePage: (p) => request.get('/api/patrol/issue/page', { params: p })
}

export const careApi = {
  page: (p) => request.get('/api/care/order/page', { params: p }),
  detail: (id) => request.get(`/api/care/order/${id}`),
  create: (d) => request.post('/api/care/order', d),
  transition: (id, p) => request.post(`/api/care/order/${id}/transition`, null, { params: p }),
  drugs: (id) => request.get(`/api/care/order/${id}/drugs`),
  addDrug: (id, d) => request.post(`/api/care/order/${id}/drug`, d)
}

export const analysisApi = {
  assessPage: (p) => request.get('/api/assessment/page', { params: p }),
  createAssess: (d) => request.post('/api/assessment', d),
  trend: (id) => request.get(`/api/assessment/trend/${id}`),
  rules: () => request.get('/api/season/rule/list'),
  saveRule: (d) => request.post('/api/season/rule', d),
  evaluate: (id, d) => request.post(`/api/ancient-tree/${id}/evaluate`, d || {}),
  scores: (id) => request.get(`/api/ancient-tree/${id}/scores`),
  adjust: (sid, d) => request.post(`/api/season/score/${sid}/adjust`, d),
  adopt: (sid, adopted) => request.post(`/api/season/score/${sid}/adopt`, null, { params: { adopted } }),
  stats: () => request.get('/api/season/stats')
}

export const systemApi = {
  users: (p) => request.get('/api/system/user/page', { params: p }),
  saveUser: (d, id) => (id ? request.put(`/api/system/user/${id}`, d) : request.post('/api/system/user', d)),
  resetPwd: (id, password) => request.post(`/api/system/user/${id}/reset-password`, null, { params: { password } }),
  roles: () => request.get('/api/system/role/list'),
  menus: () => request.get('/api/system/menu/list'),
  menuTree: () => request.get('/api/system/menu/tree'),
  roleMenus: (code) => request.get(`/api/system/role/${code}/menus`),
  assignMenus: (code, ids) => request.post(`/api/system/role/${code}/menus`, ids),
  dicts: (type) => request.get('/api/system/dict/list', { params: { type } }),
  saveDict: (d) => request.post('/api/system/dict', d),
  delDict: (id) => request.delete(`/api/system/dict/${id}`),
  orgs: () => request.get('/api/system/org/list'),
  messages: () => request.get('/api/system/message/list'),
  unread: () => request.get('/api/system/message/unread'),
  readMsg: (id) => request.post(`/api/system/message/${id}/read`),
  readAll: () => request.post('/api/system/message/read-all'),
  logs: (p) => request.get('/api/system/log/page', { params: p })
}

export const fileApi = {
  upload: (formData, bizType) =>
    request.post('/api/file/upload', formData, {
      params: { bizType },
      headers: { 'Content-Type': 'multipart/form-data' }
    })
}
