<template>
  <div class="page">
    <div class="card">
      <div class="card-title">用药台账<span class="sub">所有工单的用药记录汇总，剂量受方案核定总量约束</span></div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="orderCode" label="工单编号" min-width="120" />
        <el-table-column prop="treeCode" label="古树" min-width="120" />
        <el-table-column prop="careType" label="养护类型" min-width="110" />
        <el-table-column prop="drugName" label="药品" min-width="130" />
        <el-table-column label="本次剂量" min-width="110"><template #default="{ row }"><span class="num">{{ row.dosage }} {{ row.unit }}</span></template></el-table-column>
        <el-table-column label="核定总量" min-width="110"><template #default="{ row }"><span class="num">{{ row.planQuota }} {{ row.unit }}</span></template></el-table-column>
        <el-table-column label="累计用量" min-width="110"><template #default="{ row }"><span class="num">{{ row.usedQty }} {{ row.unit }}</span></template></el-table-column>
        <el-table-column prop="applyDate" label="施用日期" min-width="110" />
        <el-table-column prop="operatorName" label="操作人" min-width="90" />
        <el-table-column label="状态" min-width="96"><template #default="{ row }"><StatusTag :value="row.orderStatus" /></template></el-table-column>
        <template #empty><div class="empty-hint">暂无用药记录，工单进入执行中后可登记用药</div></template>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import StatusTag from '../components/StatusTag.vue'
import { careApi } from '../api'

const rows = ref([])
const loading = ref(false)

/** 汇总：拉取工单后逐个取用药记录，规模小（数十工单）可接受 */
onMounted(async () => {
  loading.value = true
  try {
    const r = await careApi.page({ page: 1, size: 100 })
    const out = []
    for (const o of r.records || []) {
      const ds = await careApi.drugs(o.id)
      ;(ds || []).forEach((d) => out.push({ ...d, orderCode: o.orderCode, treeCode: o.treeCode, careType: o.careType, orderStatus: o.status }))
    }
    rows.value = out
  } finally { loading.value = false }
})
</script>
