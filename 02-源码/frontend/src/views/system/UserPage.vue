<template>
  <div class="page">
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="角色">
          <el-select v-model="q.roleCode" clearable placeholder="全部角色" @change="reload">
            <el-option label="主管部门管理员" value="admin" /><el-option label="巡查人员" value="patrol" />
            <el-option label="养护单位" value="care" /><el-option label="园林专家" value="expert" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="账号/姓名" clearable @keyup.enter="reload" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="reload">查询</el-button></el-form-item>
      </el-form>
    </div>
    <div class="card">
      <div class="toolbar"><span class="muted">密码使用 BCrypt 保存，接口不返回密码字段</span><el-button type="primary" @click="open()">新增用户</el-button></div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column label="头像" width="70">
          <template #default="{ row }"><el-avatar :size="30" :src="row.avatar" /></template>
        </el-table-column>
        <el-table-column prop="username" label="账号" min-width="110" />
        <el-table-column prop="realName" label="姓名" min-width="90" />
        <el-table-column prop="roleName" label="角色" min-width="130" />
        <el-table-column prop="orgName" label="所属组织" min-width="180" />
        <el-table-column prop="dataScope" label="数据范围" min-width="100" />
        <el-table-column prop="phone" label="电话" min-width="120" />
        <el-table-column label="状态" min-width="80"><template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="最近登录" min-width="160"><template #default="{ row }">{{ fmtDateTime(row.lastLoginTime) }}</template></el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="warning" @click="reset(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="q.page"
                       :page-size="10" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dlg" :title="form.id ? '编辑用户' : '新增用户'" width="480px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="账号"><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色">
          <el-select v-model="form.roleCode" style="width: 200px">
            <el-option label="主管部门管理员" value="admin" /><el-option label="巡查人员" value="patrol" />
            <el-option label="养护单位" value="care" /><el-option label="园林专家" value="expert" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据范围">
          <el-select v-model="form.dataScope" style="width: 200px">
            <el-option label="全部数据" value="ALL" /><el-option label="本部门" value="ORG" /><el-option label="本人" value="SELF" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属组织">
          <el-select v-model="form.orgId" style="width: 220px">
            <el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item v-if="!form.id" label="初始密码"><el-input v-model="form.password" placeholder="留空默认 123456" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { systemApi } from '../../api'
import { fmtDateTime } from '../../utils/format'

const rows = ref([])
const orgs = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const q = reactive({ page: 1, size: 10, roleCode: '', keyword: '' })
const form = reactive({})

function blank() {
  return { id: null, username: '', realName: '', roleCode: 'patrol', dataScope: 'SELF', orgId: 2, phone: '', email: '', status: 1, password: '' }
}

async function load() {
  loading.value = true
  try {
    const r = await systemApi.users(q)
    rows.value = r.records
    total.value = r.total
  } finally { loading.value = false }
}
function reload() { q.page = 1; load() }

function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : blank())
  dlg.value = true
}

async function save() {
  const payload = { ...form }
  if (payload.orgId) {
    const o = orgs.value.find((x) => x.id === payload.orgId)
    payload.orgName = o ? o.orgName : payload.orgName
  }
  await systemApi.saveUser(payload, form.id)
  ElMessage.success('保存成功')
  dlg.value = false
  load()
}

async function reset(row) {
  try {
    const { value } = await ElMessageBox.prompt('输入新密码（留空重置为 123456）', `重置 ${row.realName} 的密码`, { inputValue: '' })
    await systemApi.resetPwd(row.id, value || '123456')
    ElMessage.success('密码已重置')
  } catch (e) { /* 取消 */ }
}

onMounted(async () => {
  await load()
  orgs.value = await systemApi.orgs()
})
</script>
