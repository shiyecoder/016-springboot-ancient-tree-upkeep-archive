<template>
  <div class="page" v-loading="loading">
    <div class="card head-card" v-if="t.id">
      <img class="cover" :src="t.coverImage" alt="古树封面" />
      <div class="info">
        <div class="line1">
          <h2 class="num">{{ t.treeCode }}</h2>
          <el-tag size="small">{{ t.protectLevel }}</el-tag>
          <StatusTag :value="t.healthLevel" />
          <StatusTag :value="t.archiveStatus" />
          <span class="muted">版本 v{{ t.version }}</span>
        </div>
        <div class="grid2">
          <div><span>树种</span><b>{{ t.species }}</b></div>
          <div><span>估测树龄</span><b class="num">{{ t.ageEstimate }} 年</b></div>
          <div><span>树高</span><b class="num">{{ t.treeHeight }} 米</b></div>
          <div><span>胸径</span><b class="num">{{ t.diameter }} 厘米</b></div>
          <div><span>冠幅</span><b class="num">{{ t.crownWidth }} 米</b></div>
          <div><span>所有权</span><b>{{ t.ownership }}</b></div>
          <div class="wide"><span>所在位置</span><b>{{ t.location }}</b></div>
          <div><span>责任单位</span><b>{{ t.managerOrg }}</b></div>
          <div><span>最近巡查</span><b class="num">{{ t.lastPatrolDate || '-' }}</b></div>
          <div><span>坐标</span><b class="num">{{ t.longitude }}, {{ t.latitude }}</b></div>
          <div><span>建档</span><b>{{ t.createBy }} {{ fmtDateTime(t.createTime) }}</b></div>
        </div>
      </div>
      <div class="ops">
        <el-button type="primary" @click="$router.push('/season?tree=' + t.id)">生成养护建议</el-button>
        <el-button @click="$router.push('/tree/list')">返回列表</el-button>
      </div>
    </div>

    <el-tabs v-model="tab" class="card tabs">
      <el-tab-pane label="基础信息与故事" name="basic">
        <div class="story">
          <h4>历史故事</h4>
          <p>{{ t.story || '暂无历史故事记录' }}</p>
        </div>
        <div class="imgs">
          <div v-for="img in t.images || []" :key="img.id" class="img-item">
            <img :src="img.url" :alt="img.category" />
            <span>{{ img.category }}</span>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`关联工单 (${(t.orders || []).length})`" name="orders">
        <el-table :data="t.orders || []" border size="small">
          <el-table-column prop="orderCode" label="工单编号" min-width="120" />
          <el-table-column prop="careType" label="养护类型" min-width="110" />
          <el-table-column prop="executorName" label="执行人" min-width="90" />
          <el-table-column prop="deadline" label="截止日期" min-width="110" />
          <el-table-column label="状态" min-width="96"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="{ row }"><el-button link type="primary" @click="$router.push('/care/order/' + row.id)">查看</el-button></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`巡查记录 (${(t.patrols || []).length})`" name="patrols">
        <el-table :data="t.patrols || []" border size="small">
          <el-table-column prop="patrolCode" label="巡查编号" min-width="130" />
          <el-table-column prop="patrolUserName" label="巡查人" min-width="90" />
          <el-table-column label="巡查时间" min-width="160"><template #default="{ row }">{{ fmtDateTime(row.patrolTime) }}</template></el-table-column>
          <el-table-column label="问题分级" min-width="100"><template #default="{ row }"><StatusTag :value="row.issueLevel" /></template></el-table-column>
          <el-table-column prop="result" label="结论" min-width="240" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>

      <el-tab-pane :label="`变更版本 (${(t.versions || []).length})`" name="versions">
        <div v-for="v in t.versions || []" :key="v.id" class="ver">
          <div class="ver-head">
            <b>v{{ v.versionNo }} · {{ v.changeType }}</b>
            <StatusTag :value="v.auditStatus" />
            <span class="muted">{{ v.applicantName }} 申请于 {{ fmtDateTime(v.applyTime) }}</span>
          </div>
          <div class="ver-body">
            <p><span>原因：</span>{{ v.reason }}</p>
            <p><span>变更前：</span><code>{{ v.beforeJson }}</code></p>
            <p><span>变更后：</span><code>{{ v.afterJson }}</code></p>
            <p v-if="v.auditTime"><span>审核：</span>{{ v.auditorName }} {{ fmtDateTime(v.auditTime) }} · {{ v.auditRemark }}</p>
          </div>
        </div>
        <div v-if="!(t.versions || []).length" class="empty-hint">暂无变更记录</div>
      </el-tab-pane>

      <el-tab-pane :label="`年度评估 (${(t.assessments || []).length})`" name="assess">
        <el-table :data="t.assessments || []" border size="small">
          <el-table-column prop="assessYear" label="年度" min-width="80" />
          <el-table-column prop="growthScore" label="生长势" min-width="80" />
          <el-table-column prop="structureScore" label="结构安全" min-width="90" />
          <el-table-column prop="pestScore" label="病虫害" min-width="80" />
          <el-table-column prop="habitatScore" label="生境" min-width="70" />
          <el-table-column prop="totalScore" label="综合分" min-width="80" />
          <el-table-column label="健康等级" min-width="90"><template #default="{ row }"><StatusTag :value="row.healthLevel" /></template></el-table-column>
          <el-table-column prop="suggestion" label="下年建议" min-width="300" show-overflow-tooltip />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="操作时间轴" name="timeline">
        <div class="tl">
          <div v-for="(e, i) in t.timeline || []" :key="i" class="timeline-item">
            <div class="t">{{ fmtDateTime(e.time) }} · {{ e.operator }}</div>
            <div class="c"><b>{{ e.type }}</b> {{ e.content }}</div>
          </div>
          <div v-if="!(t.timeline || []).length" class="empty-hint">暂无操作记录</div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import StatusTag from '../components/StatusTag.vue'
import { treeApi } from '../api'
import { fmtDateTime } from '../utils/format'

const route = useRoute()
const t = ref({})
const loading = ref(false)
const tab = ref('basic')

onMounted(async () => {
  loading.value = true
  try {
    t.value = await treeApi.detail(route.params.id)
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.head-card { display: flex; gap: 20px; align-items: flex-start; margin-bottom: 16px; }
.cover { width: 220px; height: 158px; object-fit: cover; border-radius: 8px; border: 1px solid #dbe7e8; }
.info { flex: 1; }
.line1 { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.line1 h2 { margin: 0; font-size: 20px; color: #174a5c; }
.grid2 { display: grid; grid-template-columns: repeat(3, minmax(150px, 1fr)); gap: 8px 20px; font-size: 13px; }
.grid2 .wide { grid-column: span 2; }
.grid2 span { color: #5b6b70; margin-right: 8px; }
.ops { display: flex; flex-direction: column; gap: 8px; }
.tabs :deep(.el-tabs__header) { margin-bottom: 12px; }
.story h4 { margin: 0 0 6px; color: #174a5c; font-size: 14px; }
.story p { line-height: 1.9; color: #33474c; margin: 0 0 16px; }
.imgs { display: grid; grid-template-columns: repeat(6, 1fr); gap: 10px; }
.img-item img { width: 100%; height: 92px; object-fit: cover; border-radius: 6px; border: 1px solid #dbe7e8; }
.img-item span { font-size: 11px; color: #5b6b70; display: block; text-align: center; margin-top: 2px; }
.ver { border: 1px solid #dbe7e8; border-radius: 8px; padding: 12px 14px; margin-bottom: 10px; }
.ver-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.ver-body p { margin: 4px 0; font-size: 12px; color: #33474c; }
.ver-body span { color: #5b6b70; }
.ver-body code { background: #f3f8f8; padding: 1px 6px; border-radius: 4px; font-size: 11px; }
.tl { padding-top: 6px; }
</style>
