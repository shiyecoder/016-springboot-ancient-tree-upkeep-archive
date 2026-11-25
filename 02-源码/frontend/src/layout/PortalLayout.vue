<template>
  <div class="portal">
    <!-- 前台门户：导航 + 内容流，与后台管理端视觉明确区分 -->
    <header class="nav">
      <div class="nav-inner">
        <div class="logo">
          <span class="mark">古</span>
          <div>
            <b>古树名木保护公开档案</b>
            <i>本市古树名木一树一档信息公开平台</i>
          </div>
        </div>
        <nav class="links">
          <router-link to="/portal" class="on">档案浏览</router-link>
          <router-link to="/portal?tab=story">古树故事</router-link>
          <router-link to="/portal?tab=guide">保护指南</router-link>
        </nav>
        <el-button size="small" @click="$router.push('/login')">工作人员登录</el-button>
      </div>
    </header>
    <main class="body">
      <router-view />
    </main>
    <footer class="foot">
      本页面仅展示已审核通过的古树名木档案摘要，完整档案与巡查记录请使用工作人员账号登录系统查看。
    </footer>
  </div>
</template>

<script setup>
// 门户布局：无侧边栏，顶部导航 + 内容流
</script>

<style scoped>
.portal { min-height: 100vh; background: #f3f8f8; display: flex; flex-direction: column; }
.nav { background: #174a5c; color: #fff; }
.nav-inner {
  max-width: 1200px; margin: 0 auto; height: 68px; display: flex; align-items: center;
  gap: 32px; padding: 0 24px;
}
.logo { display: flex; align-items: center; gap: 10px; }
.logo .mark { width: 34px; height: 34px; border-radius: 8px; background: #2a7f86; display: grid; place-items: center; font-weight: 700; }
.logo b { font-size: 15px; display: block; }
.logo i { font-size: 11px; color: #9dc0c4; font-style: normal; }
.links { display: flex; gap: 24px; margin-left: auto; font-size: 14px; }
.links a { color: #cfe0e2; }
.links a.on { color: #fff; border-bottom: 2px solid #7fc0c4; padding-bottom: 4px; }
.body { flex: 1; }
.foot { background: #fff; border-top: 1px solid #dbe7e8; color: #5b6b70; font-size: 12px; text-align: center; padding: 16px; }
</style>
