<template>
  <div class="page">
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="评估年度">
          <el-select v-model="q.year" clearable placeholder="全部" @change="reload">
            <el-option label="2026" :value="2026" /><el-option label="2025" :value="2025" />
          </el-select>
        </el-form-item>
        <el-form-item label="健康等级">
          <el-select v-model="q.healthLevel" clearable placeholder="全部" @change="reload">
            <el-option v-for="h in ['优', '良', '中', '差', '濒危']" :key="h" :label="h" :value="h" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" @click="reload">查询</el-button></el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <span class="muted">评分口径：生长势 30% + 结构安全 30% + 病虫害 20% + 生境 20%，同一输入结果一致</span>
        <el-button type="primary" @click="dlg = true">新增评估</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="treeCode" label="档案编号" min-width="120" />
        <el-table-column prop="species" label="树种" min-width="80" />
        <el-table-column prop="protectLevel" label="保护等级" min-width="100" />
        <el-table-column prop="region" label="区域" min-width="110" />
        <el-table-column prop="assessYear" label="年度" min-width="80" />
        <el-table-column label="生长势" min-width="80"><template #default="{ row }"><span class="num">{{ row.growthScore }}</span></template></el-table-column>
        <el-table-column label="结构安全" min-width="90"><template #default="{ row }"><span class="num">{{ row.structureScore }}</span></template></el-table-column>
        <el-table-column label="病虫害" min-width="80"><template #default="{ row }"><span class="num">{{ row.pestScore }}</span></template></el-table-column>
        <el-table-column label="生境" min-width="70"><template #default="{ row }"><span class="num">{{ row.habitatScore }}</span></template></el-table-column>
        <el-table-column label="综合分" min-width="80"><template #default="{ row }"><b class="num">{{ row.totalScore }}</b></template></el-table-column>
        <el-table-column label="健康等级" min-width="90"><template #default="{ row }"><StatusTag :value="row.healthLevel" /></template></el-table-column>
        <el-table-column prop="assessorName" label="评估人" min-width="90" />
        <el-table-column prop="suggestion" label="下年建议" min-width="320" show-overflow-tooltip />
        <template #empty><div class="empty-hint">当前条件下没有评估记录，点击「新增评估」开始</div></template>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="q.page"
                       v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @current-change="load" @size-change="reload" />
      </div>
    </div>

    <el-drawer v-model="dlg" title="新增年度评估" size="520px">
      <el-form :model="form" label-width="104px">
        <el-form-item label="古树" required>
          <el-select v-model="form.treeId" filterable style="width: 300px">
            <el-option v-for="t in trees" :key="t.id" :label="`${t.treeCode} ${t.species}`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评估年度"><el-input-number v-model="form.assessYear" :min="2020" :max="2035" /></el-form-item>
        <el-form-item label="生长势(30%)"><el-slider v-model="form.growthScore" :min="0" :max="100" show-input /></el-form-item>
        <el-form-item label="结构安全(30%)"><el-slider v-model="form.structureScore" :min="0" :max="100" show-input /></el-form-item>
        <el-form-item label="病虫害(20%)"><el-slider v-model="form.pestScore" :min="0" :max="100" show-input /></el-form-item>
        <el-form-item label="生境(20%)"><el-slider v-model="form.habitatScore" :min="0" :max="100" show-input /></el-form-item>
        <el-form-item label="综合分预览">
          <b class="num" style="font-size: 20px; color: #174a5c">{{ preview }}</b>
          <span class="muted" style="margin-left: 10px">健康等级 {{ previewLevel }}</span>
        </el-form-item>
        <el-form-item label="下年建议"><el-input v-model="form.suggestion" type="textarea" :rows="3" placeholder="留空由系统按等级生成" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="save">提交评估</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import { analysisApi, treeApi } from '../api'

const rows = ref([])
const trees = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const q = reactive({ page: 1, size: 10, year: null, healthLevel: '' })
const form = reactive({ treeId: null, assessYear: 2026, growthScore: 80, structureScore: 80, pestScore: 80, habitatScore: 80, suggestion: '' })

const preview = computed(() => Math.round((form.growthScore * 30 + form.structureScore * 30 + form.pestScore * 20 + form.habitatScore * 20) / 100))
const previewLevel = computed(() => {
  const t = preview.value
  return t >= 85 ? '优' : t >= 70 ? '良' : t >= 55 ? '中' : t >= 40 ? '差' : '濒危'
})

async function load() {
  loading.value = true
  try {
    const r = await analysisApi.assessPage(q)
    rows.value = r.records
    total.value = r.total
  } finally { loading.value = false }
}
function reload() { q.page = 1; load() }

async function save() {
  if (!form.treeId) { ElMessage.warning('请选择古树'); return }
  const r = await analysisApi.createAssess(form)
  ElMessage.success(`评估完成：${r.totalScore} 分，健康等级「${r.healthLevel}」`)
  dlg.value = false
  load()
}

onMounted(async () => {
  await load()
  const r = await treeApi.page({ page: 1, size: 100 })
  trees.value = r.records || []
})
</script>
