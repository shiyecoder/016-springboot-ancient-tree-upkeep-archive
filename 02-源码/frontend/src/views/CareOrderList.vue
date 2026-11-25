<template>
  <div class="page">
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="养护类型">
          <el-select v-model="q.careType" clearable placeholder="全部" @change="reload">
            <el-option v-for="t in ['修剪', '支撑', '防腐', '施肥', '复壮', '病虫害防治']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="q.status" clearable placeholder="全部" @change="reload">
            <el-option v-for="s in ['待方案', '待审核', '执行中', '待验收', '已完成']" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="工单编号/方案" clearable @keyup.enter="reload" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="reload">查询</el-button></el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="tabs">
          <el-button v-for="t in tabs" :key="t" size="small" :type="q.tab === t ? 'primary' : 'default'"
                     @click="q.tab = t; reload()">{{ t }}</el-button>
        </div>
        <el-button type="primary" @click="dlg = true">新建工单</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column label="照片" width="76">
          <template #default="{ row }"><img v-if="row.coverImage" class="thumb" :src="row.coverImage" alt="古树" /></template>
        </el-table-column>
        <el-table-column prop="orderCode" label="工单编号" min-width="120" />
        <el-table-column label="古树" min-width="150">
          <template #default="{ row }"><span class="num">{{ row.treeCode }}</span><div class="muted">{{ row.species }} · {{ row.protectLevel }}</div></template>
        </el-table-column>
        <el-table-column prop="careType" label="养护类型" min-width="110" />
        <el-table-column prop="executorOrg" label="养护单位" min-width="170" show-overflow-tooltip />
        <el-table-column label="进度" min-width="120">
          <template #default="{ row }"><el-progress :percentage="row.progress || 0" :stroke-width="6" :show-text="true" /></template>
        </el-table-column>
        <el-table-column label="截止日期" min-width="118"><template #default="{ row }"><span class="num" :class="{ od: row.overdue }">{{ row.deadline }}</span></template></el-table-column>
        <el-table-column label="紧急" min-width="80"><template #default="{ row }"><el-tag v-if="row.urgency === '紧急'" type="danger" size="small">紧急</el-tag><span v-else class="muted">普通</span></template></el-table-column>
        <el-table-column label="状态" min-width="96"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/care/order/' + row.id)">查看</el-button>
            <el-button v-if="row.status === '待审核'" link type="primary" @click="audit(row)">审核</el-button>
          </template>
        </el-table-column>
        <template #empty><div class="empty-hint">当前筛选条件下无记录，切换快捷标签或清空筛选条件</div></template>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="q.page"
                       v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @current-change="load" @size-change="reload" />
      </div>
    </div>

    <el-dialog v-model="dlg" title="新建养护工单" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="古树" required>
          <el-select v-model="form.treeId" filterable style="width: 320px">
            <el-option v-for="t in trees" :key="t.id" :label="`${t.treeCode} ${t.species}`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="养护类型">
          <el-select v-model="form.careType" style="width: 200px">
            <el-option v-for="t in ['修剪', '支撑', '防腐', '施肥', '复壮', '病虫害防治']" :key="t" :label="t" :value="t" />
          </el-select>
        </el-form-item>
        <el-form-item label="方案内容"><el-input v-model="form.planContent" type="textarea" :rows="3" placeholder="养护单位填写方案后提交审核" /></el-form-item>
        <el-form-item label="紧急程度">
          <el-select v-model="form.urgency" style="width: 200px"><el-option label="普通" value="普通" /><el-option label="紧急" value="紧急" /></el-select>
        </el-form-item>
        <el-form-item label="截止日期"><el-date-picker v-model="form.deadline" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="create">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import { careApi, treeApi } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const trees = ref([])
const tabs = ['全部', '待办', '处理中', '已完成', '异常']
const q = reactive({ page: 1, size: 10, tab: '全部', careType: '', status: '', keyword: '' })
const form = reactive({ treeId: null, careType: '修剪', planContent: '', urgency: '普通', deadline: '' })

async function load() {
  loading.value = true
  try {
    const r = await careApi.page(q)
    rows.value = r.records
    total.value = r.total
  } finally { loading.value = false }
}
function reload() { q.page = 1; load() }

async function create() {
  if (!form.treeId) { ElMessage.warning('请选择古树'); return }
  await careApi.create(form)
  ElMessage.success('工单已创建，状态为待方案')
  dlg.value = false
  load()
}

/** 方案审核：通过进入执行中，驳回必须填写原因（不少于 5 字） */
async function audit(row) {
  try {
    const { value } = await ElMessageBox.prompt('审核意见（通过可留空，驳回需不少于 5 字原因）', `审核 ${row.orderCode}`, {
      inputPlaceholder: '如：方案与古树实际情况相符，同意执行'
    })
    if (value && value.trim() && value.trim().length < 5) { ElMessage.warning('驳回原因不少于 5 个字'); return }
    if (value && value.trim()) {
      await careApi.transition(row.id, { action: 'reject', reason: value })
      ElMessage.success('已驳回，工单回到待方案')
    } else {
      await careApi.transition(row.id, { action: 'audit' })
      ElMessage.success('审核通过，工单进入执行中')
    }
    load()
  } catch (e) { /* 取消或校验失败 */ }
}

onMounted(async () => {
  await load()
  const r = await treeApi.page({ page: 1, size: 100 })
  trees.value = r.records || []
})
</script>

<style scoped>
.tabs .el-button { border-radius: 14px; }
.od { color: #a62b32; font-weight: 600; }
</style>
