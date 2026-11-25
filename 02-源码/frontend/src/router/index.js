import { createRouter, createWebHashHistory } from 'vue-router'

/** 路由表：后台走 AdminLayout，公开门户走 PortalLayout */
const routes = [
  { path: '/login', component: () => import('../views/Login.vue'), meta: { public: true } },
  {
    path: '/portal',
    component: () => import('../layout/PortalLayout.vue'),
    meta: { public: true },
    children: [{ path: '', component: () => import('../views/portal/PortalHome.vue'), meta: { public: true } }]
  },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '工作台' } },
      { path: 'board', component: () => import('../views/Dashboard.vue'), meta: { title: '统计看板' } },
      { path: 'tree/list', component: () => import('../views/TreeList.vue'), meta: { title: '档案管理' } },
      { path: 'tree/detail/:id', component: () => import('../views/TreeDetail.vue'), meta: { title: '档案详情' } },
      { path: 'tree/change', component: () => import('../views/TreeChange.vue'), meta: { title: '变更审核' } },
      { path: 'patrol/plan', component: () => import('../views/PatrolPlan.vue'), meta: { title: '巡查计划' } },
      { path: 'patrol/record', component: () => import('../views/PatrolRecord.vue'), meta: { title: '巡查记录' } },
      { path: 'patrol/issue', component: () => import('../views/PatrolIssue.vue'), meta: { title: '问题台账' } },
      { path: 'care/order', component: () => import('../views/CareOrderList.vue'), meta: { title: '养护工单' } },
      { path: 'care/order/:id', component: () => import('../views/CareOrderDetail.vue'), meta: { title: '工单详情' } },
      { path: 'care/drug', component: () => import('../views/DrugPage.vue'), meta: { title: '用药管理' } },
      { path: 'assessment', component: () => import('../views/AssessmentPage.vue'), meta: { title: '年度评估' } },
      { path: 'season', component: () => import('../views/SeasonPlan.vue'), meta: { title: '季节性养护计划' } },
      { path: 'message', component: () => import('../views/MessagePage.vue'), meta: { title: '消息中心' } },
      { path: 'profile', component: () => import('../views/ProfilePage.vue'), meta: { title: '个人中心' } },
      { path: 'system/user', component: () => import('../views/system/UserPage.vue'), meta: { title: '用户管理' } },
      { path: 'system/role', component: () => import('../views/system/RolePage.vue'), meta: { title: '角色管理' } },
      { path: 'system/menu', component: () => import('../views/system/MenuPage.vue'), meta: { title: '菜单管理' } },
      { path: 'system/dict', component: () => import('../views/system/DictPage.vue'), meta: { title: '字典管理' } },
      { path: 'system/log', component: () => import('../views/system/LogPage.vue'), meta: { title: '操作日志' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({ history: createWebHashHistory(), routes })

// 登录校验：公开页面直接放行
router.beforeEach((to, from, next) => {
  const isPublic = to.matched.some((r) => r.meta && r.meta.public)
  const token = localStorage.getItem('at_token')
  if (isPublic || token) return next()
  next('/login')
})

export default router
