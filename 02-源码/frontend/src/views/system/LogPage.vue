<template>
  <div class="page">
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="操作人/动作" clearable @keyup.enter="reload" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="reload">查询</el-button></el-form-item>
      </el-form>
    </div>
    <div class="card">
      <div class="card-title">操作日志<span class="sub">登录、审批、用药登记、状态流转等关键动作留痕</span></div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column label="时间" min-width="170"><template #default="{ row }">{{ fmtDateTime(row.createTime) }}</template></el-table-column>
        <el-table-column prop="userName" label="操作人" min-width="100" />
        <el-table-column prop="action" label="动作" min-width="160" />
        <el-table-column prop="method" label="请求" min-width="240" show-overflow-tooltip />
        <el-table-column prop="params" label="参数摘要" min-width="240" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" min-width="120" />
        <el-table-column label="耗时" min-width="90"><template #default="{ row }"><span class="num">{{ row.costMs }} ms</span></template></el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, prev, pager, next" :total="total" v-model:current-page="q.page"
                       :page-size="10" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { systemApi } from '../../api'
import { fmtDateTime } from '../../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10, keyword: '' })

async function load() {
  loading.value = true
  try {
    const r = await systemApi.logs(q)
    rows.value = r.records
    total.value = r.total
  } finally { loading.value = false }
}
function reload() { q.page = 1; load() }
onMounted(load)
</script>
