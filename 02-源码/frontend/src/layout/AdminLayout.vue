<template>
  <div class="admin-layout">
    <!-- 侧边栏 220px，折叠后保留图标 -->
    <aside class="side" :class="{ collapsed }">
      <div class="brand">
        <span class="mark">古</span>
        <span v-if="!collapsed" class="brand-text">
          <b>古树名木养护档案</b>
          <i>Ancient Tree Archive</i>
        </span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu :default-active="active" :collapse="collapsed" :collapse-transition="false"
                 background-color="#174a5c" text-color="#cfe0e2" active-text-color="#ffffff"
                 router unique-opened>
          <template v-for="m in menus" :key="m.id">
            <el-sub-menu v-if="m.children && m.children.length" :index="m.path">
              <template #title>
                <el-icon><component :is="iconOf(m.icon)" /></el-icon>
                <span>{{ m.name }}</span>
              </template>
              <el-menu-item v-for="c in m.children" :key="c.id" :index="c.path">
                {{ c.name }}
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="m.path">
              <el-icon><component :is="iconOf(m.icon)" /></el-icon>
              <template #title>{{ m.name }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
    </aside>

    <div class="main">
      <!-- 顶部栏 56px：面包屑 / 消息 / 帮助 / 头像 -->
      <header class="topbar">
        <div class="left">
          <el-icon class="fold" @click="collapsed = !collapsed"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>古树名木养护档案</el-breadcrumb-item>
            <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="right">
          <el-badge :value="unread" :hidden="!unread" class="msg-badge">
            <el-icon class="ico" @click="$router.push('/message')"><Bell /></el-icon>
          </el-badge>
          <el-tooltip content="帮助：一树一档 → 巡查 → 工单 → 评估" placement="bottom">
            <el-icon class="ico"><QuestionFilled /></el-icon>
          </el-tooltip>
          <el-dropdown @command="onCommand">
            <span class="user">
              <el-avatar :size="30" :src="avatar" />
              <span class="nm">{{ store.user?.realName }}</span>
              <span class="role">{{ store.roleName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="portal">公开门户</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="content">
        <router-view v-slot="{ Component }">
          <component :is="Component" />
        </router-view>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'
import { systemApi } from '../api'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const unread = ref(0)

const menus = computed(() => store.menus || [])
const active = computed(() => route.path)
const pageTitle = computed(() => route.meta.title || '工作台')
const avatar = computed(() => store.user?.avatar || '')

// 图标按后端的 icon 标识映射到 Element Plus 图标组件
const ICONS = {
  dashboard: 'Odometer', tree: 'Files', file: 'Document', audit: 'Stamp', patrol: 'Location',
  calendar: 'Calendar', check: 'Checked', warning: 'Warning', care: 'FirstAidKit', order: 'Tickets',
  drug: 'Coin', chart: 'TrendCharts', assess: 'DataAnalysis', spark: 'MagicStick', board: 'PieChart',
  globe: 'Globe', setting: 'Setting', user: 'User', role: 'Avatar', menu: 'Menu', dict: 'Collection',
  log: 'Notebook', message: 'ChatDotRound', me: 'Postcard'
}
const iconOf = (k) => ICONS[k] || 'Grid'

onMounted(async () => {
  if (!store.user) await store.fetchMe()
  else if (!store.menus || !store.menus.length) await store.fetchMe()
  try {
    const r = await systemApi.unread()
    unread.value = r.count || 0
  } catch (e) { /* 消息数量失败不影响主流程 */ }
})

function onCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  } else if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'portal') {
    router.push('/portal')
  }
}
</script>

<style scoped>
.admin-layout { display: flex; height: 100vh; overflow: hidden; }

.side {
  width: 220px;
  background: #174a5c;
  transition: width 0.18s;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.side.collapsed { width: 64px; }

.brand {
  height: 56px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 16px;
  color: #fff;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.brand .mark {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  background: #2a7f86;
  display: grid;
  place-items: center;
  font-weight: 700;
  flex-shrink: 0;
}

.brand-text { display: flex; flex-direction: column; line-height: 1.25; }
.brand-text b { font-size: 14px; }
.brand-text i { font-size: 10px; color: #9dc0c4; font-style: normal; letter-spacing: 0.5px; }

.menu-scroll { flex: 1; }
.menu-scroll :deep(.el-menu) { border-right: none; }
.menu-scroll :deep(.el-menu-item.is-active) { background: #2a7f86 !important; }

.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }

.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #dbe7e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  flex-shrink: 0;
}

.topbar .left { display: flex; align-items: center; gap: 16px; }
.fold { cursor: pointer; color: #5b6b70; font-size: 18px; }
.topbar .right { display: flex; align-items: center; gap: 18px; }
.ico { font-size: 18px; color: #5b6b70; cursor: pointer; }
.user { display: flex; align-items: center; gap: 8px; cursor: pointer; outline: none; }
.user .nm { font-size: 13px; }
.user .role { font-size: 11px; color: #2a7f86; background: #eaf3f3; padding: 1px 6px; border-radius: 10px; }

.content { flex: 1; overflow: auto; background: #f3f8f8; }
</style>
