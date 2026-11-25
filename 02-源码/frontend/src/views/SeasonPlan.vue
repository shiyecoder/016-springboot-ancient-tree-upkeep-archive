<template>
  <div class="page">
    <div class="card">
      <div class="card-title">
        季节性养护计划生成
        <span class="sub">可解释的业务辅助工具：结合树种、树龄、保护等级、健康等级、季节与上次养护时间生成建议任务</span>
      </div>

      <div class="row">
        <el-form inline>
          <el-form-item label="古树">
            <el-select v-model="treeId" filterable placeholder="选择古树" style="width: 240px" @change="loadScores">
              <el-option v-for="t in trees" :key="t.id" :label="`${t.treeCode} ${t.species}（${t.protectLevel}）`" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="规则版本">
            <el-select v-model="ruleId" style="width: 200px">
              <el-option v-for="r in rules" :key="r.id" :label="`v${r.version}${r.enabled ? '（启用）' : ''}`" :value="r.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="季节">
            <el-select v-model="season" style="width: 120px">
              <el-option v-for="s in ['春季', '夏季', '秋季', '冬季']" :key="s" :label="s" :value="s" />
            </el-select>
          </el-form-item>
          <el-form-item><el-button type="primary" :loading="computing" @click="compute">执行计算</el-button></el-form-item>
        </el-form>
      </div>

      <div v-if="result" class="result">
        <div class="summary">
          <div class="score">
            <div class="lbl">加权总分</div>
            <div class="val">{{ result.totalScore }}</div>
            <StatusTag :value="result.resultLevel" />
          </div>
          <div class="explain">
            <h4>结果解释</h4>
            <p>{{ result.explanation }}</p>
            <div class="chips" v-if="result.unmet && result.unmet.length">
              <span class="chip warn">未满足条件：</span>
              <el-tag v-for="u in result.unmet" :key="u" type="warning" size="small">{{ u }}</el-tag>
            </div>
          </div>
        </div>

        <h4 class="h4">计算明细（可追溯）</h4>
        <el-table :data="result.details" border size="small">
          <el-table-column prop="factor" label="输入因素" min-width="130" />
          <el-table-column label="权重" min-width="90"><template #default="{ row }"><span class="num">{{ row.weight }}%</span></template></el-table-column>
          <el-table-column label="得分" min-width="90">
            <template #default="{ row }"><span class="num">{{ row.score === null ? '未参与' : row.score }}</span></template>
          </el-table-column>
          <el-table-column label="加权贡献" min-width="100"><template #default="{ row }"><span class="num">{{ row.contribution }}</span></template></el-table-column>
          <el-table-column prop="desc" label="依据说明" min-width="380" show-overflow-tooltip />
        </el-table>

        <h4 class="h4">推荐任务</h4>
        <ul class="tasks">
          <li v-for="(t, i) in result.tasks" :key="i">{{ t }}</li>
        </ul>

        <div class="adjust">
          <span class="muted">业务负责人可人工调整最终结论，原始计算结果不会被覆盖：</span>
          <el-button size="small" @click="adjust(result.resultLevel === '优先养护' ? '常规养护' : '优先养护')">调整结论</el-button>
          <el-button size="small" type="success" @click="adopt(result.scoreId, true)">采纳建议</el-button>
          <el-button size="small" @click="adopt(result.scoreId, false)">不采纳</el-button>
        </div>
      </div>
      <div v-else class="empty-hint">选择古树后点击「执行计算」，系统会给出推荐等级、影响因素、未满足条件与可改进项</div>
    </div>

    <div class="grid2">
      <div class="card">
        <div class="card-title">规则权重配置<span class="sub">六项权重合计必须等于 100%</span></div>
        <el-form label-width="120px">
          <el-form-item v-for="w in weightFields" :key="w.key" :label="w.label">
            <el-input-number v-model="rule[w.key]" :min="0" :max="100" :step="5" />
            <span class="muted" style="margin-left: 6px">%</span>
          </el-form-item>
          <el-form-item label="权重合计">
            <b :class="{ bad: sum !== 100 }" class="num">{{ sum }}%</b>
            <span class="muted" style="margin-left: 8px">{{ sum === 100 ? '校验通过' : '合计必须为 100%' }}</span>
          </el-form-item>
          <el-form-item label="高分阈值">
            <el-input-number v-model="rule.thresholdHigh" :min="50" :max="95" />
            <span class="muted" style="margin-left: 8px">≥ 该分数判为优先养护</span>
          </el-form-item>
          <el-form-item label="低分阈值">
            <el-input-number v-model="rule.thresholdLow" :min="20" :max="60" />
            <span class="muted" style="margin-left: 8px">&lt; 该分数判为暂缓</span>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :disabled="sum !== 100" @click="saveRule">保存为新版本</el-button>
            <span class="muted" style="margin-left: 8px">保存后仅影响新计算</span>
          </el-form-item>
        </el-form>
      </div>

      <div class="card">
        <div class="card-title">历史计算记录<span class="sub">含人工调整与采纳情况</span></div>
        <el-table :data="scores" border size="small" max-height="330">
          <el-table-column label="时间" min-width="150"><template #default="{ row }">{{ fmtDateTime(row.createTime) }}</template></el-table-column>
          <el-table-column label="规则" min-width="70"><template #default="{ row }">v{{ row.ruleVersion }}</template></el-table-column>
          <el-table-column label="总分" min-width="70"><template #default="{ row }"><span class="num">{{ row.totalScore }}</span></template></el-table-column>
          <el-table-column label="原始结论" min-width="96"><template #default="{ row }"><StatusTag :value="row.resultLevel" /></template></el-table-column>
          <el-table-column label="人工调整" min-width="110">
            <template #default="{ row }">
              <span v-if="row.manualLevel">{{ row.manualLevel }}</span>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="采纳" min-width="70">
            <template #default="{ row }">
              <el-tag v-if="row.adopted === 1" type="success" size="small">已采纳</el-tag>
              <el-tag v-else-if="row.adopted === 0" type="info" size="small">未采纳</el-tag>
              <span v-else class="muted">未定</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="stats">
          <div><b class="num">{{ stats.total || 0 }}</b><span>计算次数</span></div>
          <div><b class="num">{{ stats.adoptRate || 0 }}%</b><span>推荐采纳率</span></div>
          <div><b class="num">{{ stats.adjustRate || 0 }}%</b><span>人工调整率</span></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import { analysisApi, treeApi } from '../api'
import { fmtDateTime } from '../utils/format'

const route = useRoute()
const trees = ref([])
const rules = ref([])
const scores = ref([])
const stats = ref({})
const treeId = ref(null)
const ruleId = ref(null)
const season = ref('秋季')
const result = ref(null)
const computing = ref(false)

const weightFields = [
  { key: 'weightSpecies', label: '树种' },
  { key: 'weightAge', label: '树龄区间' },
  { key: 'weightLevel', label: '保护等级' },
  { key: 'weightHealth', label: '健康等级' },
  { key: 'weightSeason', label: '季节' },
  { key: 'weightLastCare', label: '上次养护时间' }
]
const rule = reactive({ ruleName: '季节性养护计划规则', weightSpecies: 25, weightAge: 20, weightLevel: 20,
  weightHealth: 20, weightSeason: 10, weightLastCare: 5, thresholdHigh: 70, thresholdLow: 45 })

const sum = computed(() => weightFields.reduce((s, f) => s + (Number(rule[f.key]) || 0), 0))

async function compute() {
  if (!treeId.value) { ElMessage.warning('请先选择古树'); return }
  computing.value = true
  try {
    result.value = await analysisApi.evaluate(treeId.value, { ruleId: ruleId.value, season: season.value })
    await loadScores()
    await loadStats()
  } finally { computing.value = false }
}

async function loadScores() {
  if (!treeId.value) return
  scores.value = await analysisApi.scores(treeId.value)
}

async function loadStats() {
  stats.value = await analysisApi.stats()
}

async function saveRule() {
  const r = await analysisApi.saveRule({ ...rule })
  ElMessage.success(`已保存规则 v${r.version}，仅影响新计算`)
  rules.value = await analysisApi.rules()
}

async function adjust(level) {
  try {
    const { value } = await ElMessageBox.prompt('人工调整原因（不少于 5 个字）', '调整结论', {
      inputPlaceholder: '如：现场复核发现周边施工影响，暂缓专项养护'
    })
    if (!value || value.trim().length < 5) { ElMessage.warning('原因不少于 5 个字'); return }
    await analysisApi.adjust(result.value.scoreId, { level, reason: value })
    ElMessage.success('已记录人工调整，原始计算结果保留')
    await loadScores()
    await loadStats()
  } catch (e) { /* 取消 */ }
}

async function adopt(scoreId, ok) {
  await analysisApi.adopt(scoreId, ok)
  ElMessage.success(ok ? '已标记采纳' : '已标记不采纳')
  await loadScores()
  await loadStats()
}

onMounted(async () => {
  const r = await treeApi.page({ page: 1, size: 100 })
  trees.value = r.records || []
  rules.value = await analysisApi.rules()
  const active = rules.value.find((x) => x.enabled === 1)
  if (active) {
    ruleId.value = active.id
    Object.assign(rule, {
      weightSpecies: active.weightSpecies, weightAge: active.weightAge, weightLevel: active.weightLevel,
      weightHealth: active.weightHealth, weightSeason: active.weightSeason, weightLastCare: active.weightLastCare,
      thresholdHigh: active.thresholdHigh, thresholdLow: active.thresholdLow
    })
  }
  if (route.query.tree) {
    treeId.value = Number(route.query.tree)
    await compute()
  } else if (trees.value.length) {
    // 默认选中首株古树并加载其历史留痕：避免右侧「历史计算记录」在未选树时误显示为无数据
    treeId.value = trees.value[0].id
    await loadScores()
  }
  await loadStats()
})
</script>

<style scoped>
.row { margin-bottom: 6px; }
.result { border-top: 1px dashed #dbe7e8; padding-top: 14px; }
.summary { display: flex; gap: 24px; align-items: stretch; }
.score {
  width: 190px; flex-shrink: 0; background: #f4fafa; border: 1px solid #dbe7e8; border-radius: 8px;
  padding: 14px; text-align: center;
}
.score .lbl { font-size: 12px; color: #5b6b70; }
.score .val { font-size: 40px; font-weight: 700; color: #174a5c; line-height: 1.2; font-variant-numeric: tabular-nums; }
.explain { flex: 1; }
.explain h4 { margin: 0 0 8px; font-size: 14px; color: #174a5c; }
.explain p { margin: 0 0 8px; line-height: 1.9; color: #33474c; }
.chips { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.chip.warn { font-size: 12px; color: #a67c2b; }
.h4 { font-size: 13px; color: #174a5c; margin: 16px 0 8px; }
.tasks { margin: 0; padding-left: 20px; line-height: 2; color: #33474c; }
.adjust { margin-top: 14px; display: flex; gap: 10px; align-items: center; }
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-top: 16px; }
.bad { color: #a62b32; }
.stats { display: flex; gap: 28px; margin-top: 12px; }
.stats b { font-size: 20px; color: #174a5c; display: block; }
.stats span { font-size: 12px; color: #5b6b70; }
</style>
