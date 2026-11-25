<template>
  <div class="page">
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="问题分级">
          <el-select v-model="q.issueLevel" clearable placeholder="全部" @change="reload">
            <el-option label="日常养护" value="日常养护" /><el-option label="重点观察" value="重点观察" />
            <el-option label="紧急抢救" value="紧急抢救" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理状态">
          <el-select v-model="q.status" clearable placeholder="全部" @change="reload">
            <el-option label="待处理" value="待处理" /><el-option label="已派单" value="已派单" /><el-option label="已关闭" value="已关闭" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="reload">查询</el-button></el-form-item>
      </el-form>
    </div>
    <div class="card">
      <div class="card-title">问题台账<span class="sub">巡查发现的问题按分级流转，紧急抢救自动生成工单</span></div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="issueCode" label="问题编号" min-width="120" />
        <el-table-column label="古树" min-width="160">
          <template #default="{ row }"><span class="num">{{ row.treeCode }}</span><div class="muted">{{ row.species }} · {{ row.protectLevel }}</div></template>
        </el-table-column>
        <el-table-column prop="issueType" label="问题类型" min-width="110" />
        <el-table-column prop="severity" label="严重程度" min-width="90" />
        <el-table-column prop="description" label="问题描述" min-width="300" show-overflow-tooltip />
        <el-table-column label="分级" min-width="100"><template #default="{ row }"><StatusTag :value="row.issueLevel" /></template></el-table-column>
        <el-table-column label="状态" min-width="90"><template #default="{ row }"><StatusTag :value="row.status === '已派单' ? '执行中' : row.status === '已关闭' ? '已完成' : '待方案'" /></template></el-table-column>
        <el-table-column label="关联工单" min-width="100">
          <template #default="{ row }">
            <el-button v-if="row.handleOrderId" link type="primary" @click="$router.push('/care/order/' + row.handleOrderId)">查看</el-button>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="发现时间" min-width="160"><template #default="{ row }">{{ fmtDateTime(row.createTime) }}</template></el-table-column>
        <template #empty><div class="empty-hint">当前没有符合条件的问题记录</div></template>
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
import StatusTag from '../components/StatusTag.vue'
import { patrolApi } from '../api'
import { fmtDateTime } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const q = reactive({ page: 1, size: 10, issueLevel: '', status: '' })

async function load() {
  loading.value = true
  try {
    const r = await patrolApi.issuePage(q)
    rows.value = r.records
    total.value = r.total
  } finally { loading.value = false }
}
function reload() { q.page = 1; load() }
onMounted(load)
</script>
