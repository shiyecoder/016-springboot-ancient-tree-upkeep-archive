<template>
  <div class="page">
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="q.status" clearable placeholder="全部" @change="reload">
            <el-option label="草稿" value="草稿" /><el-option label="执行中" value="执行中" /><el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
        <el-form-item label="保护等级">
          <el-select v-model="q.protectLevel" clearable placeholder="全部" @change="reload">
            <el-option label="一级" value="一级" /><el-option label="二级" value="二级" /><el-option label="三级" value="三级" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="计划名称/编号" clearable @keyup.enter="reload" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="reload">查询</el-button></el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <span class="muted">巡查频率按保护级别设置：一级 ≥4 次/年，二级 ≥2 次/年，三级 ≥1 次/年</span>
        <el-button type="primary" @click="open()">新建计划</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="planCode" label="计划编号" min-width="120" />
        <el-table-column prop="planName" label="计划名称" min-width="220" />
        <el-table-column prop="protectLevel" label="适用等级" min-width="100" />
        <el-table-column prop="region" label="区域" min-width="110" />
        <el-table-column label="巡查频次" min-width="100">
          <template #default="{ row }"><span class="num">{{ row.frequency }}</span> 次/年</template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="执行人" min-width="90" />
        <el-table-column label="有效期" min-width="200">
          <template #default="{ row }"><span class="num">{{ row.startDate }} ~ {{ row.endDate }}</span></template>
        </el-table-column>
        <el-table-column label="覆盖树木" min-width="90">
          <template #default="{ row }"><span class="num">{{ row.coverCount }}</span> 株</template>
        </el-table-column>
        <el-table-column label="状态" min-width="90"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column label="操作" width="100"><template #default="{ row }"><el-button link type="primary" @click="open(row)">编辑</el-button></template></el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="q.page"
                       v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @current-change="load" @size-change="reload" />
      </div>
    </div>

    <el-drawer v-model="dlg" :title="form.id ? '编辑巡查计划' : '新建巡查计划'" size="480px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="计划名称"><el-input v-model="form.planName" /></el-form-item>
        <el-form-item label="保护等级">
          <el-select v-model="form.protectLevel" style="width: 200px">
            <el-option label="一级" value="一级" /><el-option label="二级" value="二级" />
            <el-option label="三级" value="三级" /><el-option label="后续资源" value="后续资源" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域">
          <el-select v-model="form.region" style="width: 200px">
            <el-option v-for="r in ['城东片区', '城西片区', '城南片区', '城北片区', '老城中心区']" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="每年频次"><el-input-number v-model="form.frequency" :min="1" :max="12" /></el-form-item>
        <el-form-item label="执行人">
          <el-select v-model="form.assigneeId" style="width: 200px" @change="(v) => pickUser(v)">
            <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始日期"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 200px">
            <el-option label="草稿" value="草稿" /><el-option label="执行中" value="执行中" /><el-option label="已结束" value="已结束" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import { patrolApi, systemApi } from '../api'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const users = ref([])
const q = reactive({ page: 1, size: 10, status: '', protectLevel: '', keyword: '' })
const form = reactive({})

function blank() {
  return { id: null, planName: '', protectLevel: '一级', region: '城东片区', frequency: 4, assigneeId: null,
    assigneeName: '', startDate: '', endDate: '', status: '草稿', remark: '' }
}

async function load() {
  loading.value = true
  try {
    const r = await patrolApi.planPage(q)
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

function pickUser(id) {
  const u = users.value.find((x) => x.id === id)
  form.assigneeName = u ? u.realName : ''
}

async function save() {
  await patrolApi.savePlan(form, form.id)
  ElMessage.success('保存成功')
  dlg.value = false
  load()
}

onMounted(async () => {
  await load()
  try {
    const r = await systemApi.users({ page: 1, size: 50 })
    users.value = r.records || []
  } catch (e) { /* 无权限忽略 */ }
})
</script>
