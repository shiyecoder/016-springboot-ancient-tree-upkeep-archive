<template>
  <div class="page">
    <div class="card">
      <div class="card-title">
        字典管理<span class="sub">保护等级、健康等级、养护类型、工单状态等统一维护</span>
        <el-button size="small" type="primary" @click="open()">新增字典项</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="dictType" label="字典类型" min-width="150" />
        <el-table-column prop="dictLabel" label="显示名" min-width="150" />
        <el-table-column prop="dictValue" label="字典值" min-width="150" />
        <el-table-column prop="sortNo" label="排序" min-width="80" />
        <el-table-column label="状态" min-width="90">
          <template #default="{ row }"><el-tag size="small" :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">编辑</el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dlg" title="字典项" width="440px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="字典类型"><el-input v-model="form.dictType" placeholder="如 protectLevel" /></el-form-item>
        <el-form-item label="显示名"><el-input v-model="form.dictLabel" /></el-form-item>
        <el-form-item label="字典值"><el-input v-model="form.dictValue" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sortNo" :min="0" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
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

const rows = ref([])
const loading = ref(false)
const dlg = ref(false)
const form = reactive({})

async function load() {
  loading.value = true
  try { rows.value = await systemApi.dicts() } finally { loading.value = false }
}

function open(row) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row ? { ...row } : { dictType: '', dictLabel: '', dictValue: '', sortNo: 0, status: 1 })
  dlg.value = true
}

async function save() {
  await systemApi.saveDict(form)
  ElMessage.success('保存成功')
  dlg.value = false
  load()
}

async function del(row) {
  try {
    await ElMessageBox.confirm(`确认删除字典项「${row.dictLabel}」？`, '删除确认', { type: 'warning' })
    await systemApi.delDict(row.id)
    ElMessage.success('已删除')
    load()
  } catch (e) { /* 取消 */ }
}

onMounted(load)
</script>
