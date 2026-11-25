<template>
  <div class="page">
    <div class="grid2">
      <div class="card">
        <div class="card-title">个人资料<span class="sub">修改后右上角立即同步</span></div>
        <div class="avatar-row">
          <el-avatar :size="72" :src="form.avatar" />
          <div>
            <UploadField v-model="form.avatar" biz-type="avatar" />
            <div class="muted" style="margin-top: 6px">支持 jpg/png，上传后即时生效</div>
          </div>
        </div>
        <el-form :model="form" label-width="90px" style="margin-top: 12px">
          <el-form-item label="账号"><el-input :model-value="store.user?.username" disabled /></el-form-item>
          <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
          <el-form-item label="角色">
            <el-tag>{{ store.roleName }}</el-tag>
            <span class="muted" style="margin-left: 10px">数据范围 {{ store.user?.dataScope }}</span>
          </el-form-item>
          <el-form-item><el-button type="primary" @click="save">保存资料</el-button></el-form-item>
        </el-form>
      </div>

      <div class="card">
        <div class="card-title">修改密码<span class="sub">密码使用 BCrypt 加密保存</span></div>
        <el-form :model="pwd" label-width="90px">
          <el-form-item label="原密码"><el-input v-model="pwd.oldPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="pwd.newPassword" type="password" show-password /></el-form-item>
          <el-form-item label="确认密码"><el-input v-model="pwd.confirm" type="password" show-password /></el-form-item>
          <el-form-item><el-button type="primary" @click="changePwd">修改密码</el-button></el-form-item>
        </el-form>
        <el-divider />
        <div class="tips">
          <p>· 连续 5 次登录失败将锁定账号 10 分钟</p>
          <p>· 密码不得明文出现在任何接口返回与日志中</p>
          <p>· 本人负责的数据范围由管理员在用户管理中调整</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import UploadField from '../components/UploadField.vue'
import { useUserStore } from '../store/user'
import { authApi } from '../api'

const store = useUserStore()
const form = reactive({ realName: '', phone: '', email: '', avatar: '' })
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function save() {
  const r = await authApi.profile(form)
  store.user = r
  store.menus = r.menus || store.menus
  localStorage.setItem('at_user', JSON.stringify(r))
  ElMessage.success('资料已更新')
}

async function changePwd() {
  if (!pwd.oldPassword || !pwd.newPassword) { ElMessage.warning('请填写原密码与新密码'); return }
  if (pwd.newPassword !== pwd.confirm) { ElMessage.warning('两次输入的新密码不一致'); return }
  await authApi.password({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
  ElMessage.success('密码已修改，请重新登录')
  pwd.oldPassword = pwd.newPassword = pwd.confirm = ''
}

onMounted(async () => {
  const me = store.user || (await authApi.me())
  Object.assign(form, { realName: me.realName, phone: me.phone, email: me.email, avatar: me.avatar })
})
</script>

<style scoped>
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.avatar-row { display: flex; gap: 16px; align-items: center; }
.avatar-row > div { flex: 1; }
.tips p { font-size: 12px; color: #5b6b70; margin: 4px 0; }
</style>
