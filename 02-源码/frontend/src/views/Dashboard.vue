<template>
  <div class="dash">
    <!-- 首页：6 个核心指标 + 7 个图表，1440x900 一屏内不出现滚动条 -->
    <div class="stat-grid">
      <div class="stat" v-for="s in statCards" :key="s.label">
        <div class="label">{{ s.label }}</div>
        <div class="value">{{ s.value }}<small>{{ s.unit }}</small></div>
      </div>
    </div>

    <div class="grid3">
      <div class="card">
        <div class="card-title">树种分布<span class="sub">按在册株数</span></div>
        <ChartBox :option="speciesOpt" :height="196" />
      </div>
      <div class="card">
        <div class="card-title">树龄区间<span class="sub">古树分级依据</span></div>
        <ChartBox :option="ageOpt" :height="196" />
      </div>
      <div class="card">
        <div class="card-title">健康等级分布<span class="sub">最新评估结果</span></div>
        <ChartBox :option="healthOpt" :height="196" />
      </div>
    </div>

    <div class="grid4">
      <div class="card">
        <div class="card-title">问题类型分布<span class="sub">巡查发现问题</span></div>
        <ChartBox :option="issueOpt" :height="168" />
      </div>
      <div class="card">
        <div class="card-title">养护时效<span class="sub">近 6 个月工单</span></div>
        <ChartBox :option="timeOpt" :height="168" />
      </div>
      <div class="card">
        <div class="card-title">年度投入<span class="sub">工单折算投入（元）</span></div>
        <ChartBox :option="investOpt" :height="168" />
      </div>
      <div class="card">
        <div class="card-title">年度评分趋势<span class="sub">平均综合分</span></div>
        <ChartBox :option="scoreOpt" :height="168" />
      </div>
    </div>

    <div class="todo-row">
      <div class="card todo">
        <div class="card-title">待办工单<span class="sub">点击进入处理</span></div>
        <div class="todo-list">
          <div v-for="t in todo" :key="t.id" class="todo-item" @click="$router.push('/care/order/' + t.id)">
            <span class="code num">{{ t.orderCode }}</span>
            <span class="type">{{ t.careType }}</span>
            <StatusTag :value="t.status" />
            <span class="dl" :class="{ od: t.overdue }">{{ t.overdue ? '已超时' : '截止 ' + (t.deadline || '-') }}</span>
          </div>
          <div v-if="!todo.length" class="empty-hint">当前没有待处理工单，全部工单已完成</div>
        </div>
      </div>
      <div class="card recent">
        <div class="card-title">最近巡查<span class="sub">现场记录</span></div>
        <div class="recent-list">
          <div v-for="(r, i) in recent" :key="i" class="rv">
            <span class="num">{{ r.treeCode }}</span>
            <span class="who">{{ r.patrolUserName }}</span>
            <StatusTag :value="r.issueLevel" />
            <span class="tm">{{ fromNow(r.patrolTime) }}</span>
          </div>
          <div v-if="!recent.length" class="empty-hint">最近没有巡查记录</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, reactive } from 'vue'
import ChartBox from '../components/ChartBox.vue'
import StatusTag from '../components/StatusTag.vue'
import { treeApi } from '../api'
import { fromNow } from '../utils/format'

const d = ref({})
const todo = ref([])
const recent = ref([])

const metrics = computed(() => d.value.metrics || {})

const statCards = computed(() => [
  { label: '在册古树档案', value: metrics.value.treeCount ?? 0, unit: '株' },
  { label: '巡查覆盖率', value: metrics.value.patrolCoverage ?? 0, unit: '%' },
  { label: '养护完成率', value: metrics.value.careDoneRate ?? 0, unit: '%' },
  { label: '健康等级提升', value: metrics.value.healthChange ?? 0, unit: '株' },
  { label: '异常树木', value: metrics.value.abnormalCount ?? 0, unit: '株' },
  { label: '覆盖区域', value: metrics.value.regionCount ?? 0, unit: '个' }
])

/** 统一图表基座：去掉多余网格与边框，保证一屏可读 */
const base = { grid: { left: 42, right: 14, top: 18, bottom: 26 }, tooltip: { trigger: 'axis' } }

const speciesOpt = computed(() => {
  const data = d.value.speciesDist || {}
  const keys = Object.keys(data)
  return {
    ...base,
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: keys, axisLabel: { fontSize: 10, interval: 0, rotate: keys.length > 5 ? 28 : 0 }, axisTick: { show: false } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#eef3f3' } }, axisLabel: { fontSize: 10 } },
    series: [{ type: 'bar', data: keys.map((k) => data[k]), barWidth: '46%', itemStyle: { borderRadius: [4, 4, 0, 0], color: '#2a7f86' } }]
  }
})

const ageOpt = computed(() => {
  const data = d.value.ageDist || {}
  const keys = Object.keys(data)
  return {
    ...base,
    xAxis: { type: 'category', data: keys, axisLabel: { fontSize: 10, interval: 0, rotate: 26 }, axisTick: { show: false } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#eef3f3' } }, axisLabel: { fontSize: 10 } },
    series: [{ type: 'bar', data: keys.map((k) => data[k]), barWidth: '46%', itemStyle: { borderRadius: [4, 4, 0, 0], color: '#174a5c' } }]
  }
})

const healthOpt = computed(() => {
  const data = d.value.healthDist || {}
  const map = { 优: '#247a58', 良: '#2a7f86', 中: '#c9a227', 差: '#a67c2b', 濒危: '#a62b32' }
  const rows = Object.keys(data).map((k) => ({ name: k, value: data[k], itemStyle: { color: map[k] || '#316b74' } }))
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 11 } },
    series: [{
      type: 'pie', radius: ['42%', '66%'], center: ['50%', '44%'], data: rows,
      label: { formatter: '{b} {c}', fontSize: 11 }
    }]
  }
})

const issueOpt = computed(() => {
  const data = d.value.issueDist || {}
  const keys = Object.keys(data).reverse()
  return {
    grid: { left: 74, right: 18, top: 10, bottom: 20 },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef3f3' } }, axisLabel: { fontSize: 10 } },
    yAxis: { type: 'category', data: keys, axisLabel: { fontSize: 10 }, axisTick: { show: false } },
    series: [{ type: 'bar', data: keys.map((k) => data[k]), barWidth: '52%', itemStyle: { color: '#316b74', borderRadius: [0, 3, 3, 0] } }]
  }
})

const timeOpt = computed(() => {
  const rows = d.value.timeliness || []
  return {
    ...base,
    legend: { right: 0, top: 0, itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 10 } },
    xAxis: { type: 'category', data: rows.map((r) => r.month.slice(5)), axisLabel: { fontSize: 10 }, axisTick: { show: false } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#eef3f3' } }, axisLabel: { fontSize: 10 } },
    series: [
      { name: '平均处理天数', type: 'line', smooth: true, symbolSize: 5, data: rows.map((r) => r.avgDays), lineStyle: { width: 2 }, itemStyle: { color: '#174a5c' } },
      { name: '完成工单', type: 'bar', barWidth: '34%', data: rows.map((r) => r.done), itemStyle: { color: '#a9cdd0' } }
    ]
  }
})

const investOpt = computed(() => {
  const rows = d.value.invest || []
  return {
    ...base,
    xAxis: { type: 'category', data: rows.map((r) => r.month.slice(5)), axisLabel: { fontSize: 10 }, axisTick: { show: false } },
    yAxis: { type: 'value', minInterval: 1, splitLine: { lineStyle: { color: '#eef3f3' } }, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'line', smooth: true, symbolSize: 5, data: rows.map((r) => r.amount),
      areaStyle: { color: 'rgba(42,127,134,0.18)' }, lineStyle: { color: '#2a7f86', width: 2 }
    }]
  }
})

const scoreOpt = computed(() => {
  const rows = d.value.scoreTrend || []
  return {
    ...base,
    xAxis: { type: 'category', data: rows.map((r) => r.year), axisLabel: { fontSize: 10 }, axisTick: { show: false } },
    yAxis: { type: 'value', min: 50, max: 100, splitLine: { lineStyle: { color: '#eef3f3' } }, axisLabel: { fontSize: 10 } },
    series: [{ type: 'bar', data: rows.map((r) => r.avgScore), barWidth: '38%', itemStyle: { color: '#247a58', borderRadius: [4, 4, 0, 0] } }]
  }
})

onMounted(async () => {
  const r = await treeApi.dashboard(30)
  d.value = r
  todo.value = r.todo || []
  recent.value = r.recentPatrols || []
})
</script>

<style scoped>
.dash { padding: 16px 24px 20px; }
.grid3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 12px; }
.grid4 { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 12px; }
.dash .card { padding: 12px 14px; }
.dash .card-title { margin-bottom: 6px; font-size: 13px; }
.todo-row { display: grid; grid-template-columns: 1.15fr 1fr; gap: 12px; margin-top: 12px; }
.todo-list, .recent-list { max-height: 132px; overflow: auto; }
.todo-item, .rv { display: flex; align-items: center; gap: 10px; padding: 5px 0; border-bottom: 1px dashed #eef3f3; font-size: 12px; }
.todo-item { cursor: pointer; }
.todo-item:hover { background: #f7fbfb; }
.code { color: #174a5c; font-weight: 600; }
.type { color: #5b6b70; }
.dl { margin-left: auto; color: #5b6b70; }
.dl.od { color: #a62b32; font-weight: 600; }
.who { color: #5b6b70; }
.tm { margin-left: auto; color: #8ea3a7; }
.stat-grid { grid-template-columns: repeat(6, 1fr); }
.stat .value { font-size: 22px; }
</style>
