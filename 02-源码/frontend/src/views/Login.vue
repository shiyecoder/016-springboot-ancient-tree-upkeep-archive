<template>
  <div class="login-page">
    <!-- 登录页重做：左侧品牌信息 + 右侧表单卡片，非默认表单样式 -->
    <div class="visual">
      <img class="bg" src="/login-bg.jpg" alt="古树保护" />
      <div class="veil"></div>
      <div class="vtext">
        <div class="tag">古树名木养护档案管理系统</div>
        <h1>一树一档<br />让每一株古树都有完整生命记录</h1>
        <p>档案 · 巡查 · 养护 · 评估 全流程留痕<br />分级巡查 / 紧急抢救 / 年度健康评估</p>
        <div class="facts">
          <div><b>22</b><span>在册古树</span></div>
          <div><b>34</b><span>养护工单</span></div>
          <div><b>38</b><span>年度评估</span></div>
        </div>
      </div>
    </div>

    <div class="panel">
      <div class="head">
        <span class="mark">古</span>
        <div>
          <b>登录系统</b>
          <i>请使用单位分配的账号登录</i>
        </div>
      </div>

      <el-tabs v-model="tab" class="tabs">
        <el-tab-pane label="账号登录" name="login">
          <el-form :model="form" :rules="rules" ref="fref" label-position="top" @keyup.enter="submit">
            <el-form-item label="账号" prop="username">
              <el-input v-model="form.username" size="large" placeholder="请输入账号">
                <template #prefix><el-icon><User /></el-icon></template>
              </el-input>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="form.password" type="password" size="large" show-password placeholder="请输入密码">
                <template #prefix><el-icon><Lock /></el-icon></template>
              </el-input>
            </el-form-item>
            <div class="row">
              <el-checkbox v-model="remember">记住账号</el-checkbox>
              <span class="muted">连续 5 次失败将锁定 10 分钟</span>
            </div>
            <el-button type="primary" size="large" class="submit" :loading="loading" @click="submit">登 录</el-button>
          </el-form>

          <div class="roles">
            <span class="muted">演示账号（点击填充）：</span>
            <el-tag v-for="r in demo" :key="r.u" size="small" class="rt" @click="fill(r)">{{ r.n }}</el-tag>
          </div>
        </el-tab-pane>

        <el-tab-pane label="注册账号" name="reg">
          <el-form :model="reg" label-position="top">
            <el-form-item label="账号"><el-input v-model="reg.username" size="large" placeholder="6-20 位字母或数字" /></el-form-item>
            <el-form-item label="姓名"><el-input v-model="reg.realName" size="large" /></el-form-item>
            <el-form-item label="手机号"><el-input v-model="reg.phone" size="large" /></el-form-item>
            <el-form-item label="申请角色">
              <el-select v-model="reg.roleCode" style="width: 100%" size="large">
                <el-option label="巡查人员" value="patrol" />
                <el-option label="养护单位" value="care" />
                <el-option label="园林专家" value="expert" />
              </el-select>
            </el-form-item>
            <el-form-item label="密码"><el-input v-model="reg.password" type="password" size="large" show-password /></el-form-item>
            <el-button type="primary" size="large" class="submit" :loading="loading" @click="doRegister">提交注册</el-button>
            <p class="muted" style="margin-top: 10px">注册后默认数据范围为「本人」，需管理员调整角色与范围。</p>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'
import { authApi } from '../api'

const router = useRouter()
const store = useUserStore()
const tab = ref('login')
const loading = ref(false)
const remember = ref(true)
const fref = ref(null)
const form = reactive({ username: 'admin', password: 'admin123' })
const reg = reactive({ username: '', realName: '', phone: '', roleCode: 'patrol', password: '' })

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const demo = [
  { n: '管理员', u: 'admin', p: 'admin123' },
  { n: '巡查人员', u: 'zhaomin', p: '123456' },
  { n: '养护单位', u: 'lijing', p: '123456' },
  { n: '园林专家', u: 'chenshu', p: '123456' }
]

function fill(r) {
  form.username = r.u
  form.password = r.p
}

async function submit() {
  await fref.value.validate(async (ok) => {
    if (!ok) return
    loading.value = true
    try {
      await store.login({ username: form.username, password: form.password })
      ElMessage.success('登录成功')
      router.push('/dashboard')
    } catch (e) {
      /* 错误提示由拦截器统一处理 */
    } finally {
      loading.value = false
    }
  })
}

async function doRegister() {
  if (!reg.username || !reg.password || !reg.realName) {
    ElMessage.warning('账号、姓名、密码不能为空')
    return
  }
  loading.value = true
  try {
    await authApi.register(reg)
    ElMessage.success('注册成功，请使用新账号登录')
    tab.value = 'login'
    form.username = reg.username
  } catch (e) { /* 已提示 */ } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page { display: flex; min-height: 100vh; background: #f3f8f8; }

.visual { flex: 1.15; position: relative; overflow: hidden; min-width: 0; }
.bg { width: 100%; height: 100%; object-fit: cover; display: block; }
.veil {
  position: absolute; inset: 0;
  background: linear-gradient(115deg, rgba(13, 47, 58, 0.92) 0%, rgba(23, 74, 92, 0.78) 45%, rgba(42, 127, 134, 0.45) 100%);
}
.vtext { position: absolute; inset: 0; padding: 64px 56px; color: #fff; display: flex; flex-direction: column; justify-content: center; }
.tag {
  display: inline-block; align-self: flex-start; font-size: 12px; letter-spacing: 2px;
  border: 1px solid rgba(255, 255, 255, 0.45); padding: 5px 12px; border-radius: 14px; color: #dff0f1;
}
.vtext h1 { font-size: 34px; line-height: 1.45; margin: 22px 0 16px; font-weight: 600; letter-spacing: 1px; }
.vtext p { color: #c6dde0; font-size: 14px; line-height: 2; margin: 0; }
.facts { display: flex; gap: 36px; margin-top: 44px; }
.facts b { font-size: 26px; font-variant-numeric: tabular-nums; display: block; }
.facts span { font-size: 12px; color: #a9c9cd; }

.panel {
  width: 480px; flex-shrink: 0; background: #fff; padding: 48px 44px;
  display: flex; flex-direction: column; justify-content: center;
}
.head { display: flex; align-items: center; gap: 12px; margin-bottom: 22px; }
.head .mark { width: 40px; height: 40px; border-radius: 10px; background: #174a5c; color: #fff; display: grid; place-items: center; font-weight: 700; font-size: 18px; }
.head b { font-size: 20px; display: block; }
.head i { font-style: normal; font-size: 12px; color: #5b6b70; }
.tabs :deep(.el-tabs__item) { font-size: 14px; }
.row { display: flex; align-items: center; justify-content: space-between; margin: 4px 0 18px; }
.submit { width: 100%; letter-spacing: 6px; }
.roles { margin-top: 22px; padding-top: 16px; border-top: 1px dashed #dbe7e8; display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.rt { cursor: pointer; }

@media (max-width: 1000px) {
  .visual { display: none; }
  .panel { width: 100%; }
}
</style>
