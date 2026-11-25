<template>
  <div class="page">
    <div class="card">
      <div class="card-title">树木变更审核<span class="sub">保护级别 / 责任单位 / 树体状态变更需专家审核，原档案保留历史版本</span></div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column label="古树档案" min-width="140">
          <template #default="{ row }"><span class="num">{{ row.treeCode }}</span></template>
        </el-table-column>
        <el-table-column prop="changeType" label="变更类型" min-width="110" />
        <el-table-column label="变更前 → 后" min-width="300">
          <template #default="{ row }">
            <code class="before">{{ row.beforeJson }}</code>
            <el-icon style="margin: 0 6px"><Right /></el-icon>
            <code class="after">{{ row.afterJson }}</code>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="申请原因" min-width="240" show-overflow-tooltip />
        <el-table-column prop="applicantName" label="申请人" min-width="90" />
        <el-table-column label="申请时间" min-width="160"><template #default="{ row }">{{ fmtDateTime(row.applyTime) }}</template></el-table-column>
        <el-table-column label="状态" min-width="96"><template #default="{ row }"><StatusTag :value="row.auditStatus" /></template></el-table-column>
        <el-table-column label="审核信息" min-width="180">
          <template #default="{ row }">
            <span v-if="row.auditTime" class="muted">{{ row.auditorName }} {{ fmtDateTime(row.auditTime) }}<br />{{ row.auditRemark }}</span>
            <span v-else class="muted">待审核</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <template v-if="row.auditStatus === '待审核'">
              <el-button link type="success" @click="audit(row, true)">通过</el-button>
              <el-button link type="danger" @click="audit(row, false)">驳回</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>
        <template #empty><div class="empty-hint">当前没有待审核的变更申请</div></template>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="page"
                       :page-size="10" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import { treeApi } from '../api'
import { fmtDateTime } from '../utils/format'

const rows = ref([])
const total = ref(0)
const page = ref(1)
const loading = ref(false)

/**
 * 变更申请散布在各档案详情中，这里聚合展示：
 * 通过分页拉取档案列表再逐个取版本，数据量小（数十株）可接受。
 */
async function load() {
  loading.value = true
  try {
    const r = await treeApi.page({ page: page.value, size: 10 })
    const list = []
    for (const t of r.records || []) {
      const d = await treeApi.detail(t.id)
      ;(d.versions || []).forEach((v) => list.push({ ...v, treeCode: d.treeCode }))
    }
    list.sort((a, b) => String(b.applyTime).localeCompare(String(a.applyTime)))
    rows.value = list
    total.value = list.length
  } finally {
    loading.value = false
  }
}

async function audit(row, pass) {
  const title = pass ? '审核通过' : '审核驳回'
  try {
    const { value } = await ElMessageBox.prompt('请填写审核意见（不少于 5 字）', title, {
      inputPlaceholder: '如：复核资料齐全，同意变更'
    })
    if (!value || value.trim().length < 5) {
      ElMessage.warning('审核意见不少于 5 个字')
      return
    }
    await treeApi.audit(row.id, pass, value)
    ElMessage.success(pass ? '已通过，主档已同步' : '已驳回')
    load()
  } catch (e) { /* 用户取消或后端校验失败 */ }
}

onMounted(load)
</script>

<style scoped>
code { background: #f3f8f8; padding: 1px 6px; border-radius: 4px; font-size: 11px; color: #5b6b70; }
code.after { background: #eaf5ef; color: #247a58; }
</style>
