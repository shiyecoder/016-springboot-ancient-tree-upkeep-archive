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
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="巡查编号/巡查人" clearable @keyup.enter="reload" />
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
        <el-button type="primary" @click="openForm">现场巡查录入</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" border stripe>
        <el-table-column prop="patrolCode" label="巡查编号" min-width="130" />
        <el-table-column label="古树" min-width="150">
          <template #default="{ row }">
            <span class="num">{{ row.treeCode }}</span>
            <div class="muted">{{ row.species }} · {{ row.protectLevel }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="位置" min-width="200" show-overflow-tooltip />
        <el-table-column prop="patrolUserName" label="巡查人" min-width="90" />
        <el-table-column label="巡查时间" min-width="160"><template #default="{ row }">{{ fmtDateTime(row.patrolTime) }}</template></el-table-column>
        <el-table-column prop="trunkHollow" label="树干" min-width="110" show-overflow-tooltip />
        <el-table-column prop="leanDegree" label="倾斜" min-width="90" />
        <el-table-column prop="pestDesc" label="病虫害" min-width="140" show-overflow-tooltip />
        <el-table-column label="分级" min-width="100"><template #default="{ row }"><StatusTag :value="row.issueLevel" /></template></el-table-column>
        <el-table-column label="状态" min-width="110"><template #default="{ row }"><StatusTag :value="row.status" /></template></el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }"><el-button link type="primary" @click="show(row)">详情</el-button></template>
        </el-table-column>
        <template #empty><div class="empty-hint">当前筛选条件下无记录，清空筛选或点击「现场巡查录入」</div></template>
      </el-table>
      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total" v-model:current-page="q.page"
                       v-model:page-size="q.size" :page-sizes="[10, 20, 50]" @current-change="load" @size-change="reload" />
      </div>
    </div>

    <el-drawer v-model="dlg" title="现场巡查录入" size="620px">
      <el-form :model="form" label-width="104px">
        <el-form-item label="古树" required>
          <el-select v-model="form.treeId" filterable style="width: 300px" placeholder="输入编号或树种检索">
            <el-option v-for="t in trees" :key="t.id" :label="`${t.treeCode} ${t.species}（${t.protectLevel}）`" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="巡查时间">
          <el-date-picker v-model="form.patrolTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="树干空洞"><el-input v-model="form.trunkHollow" placeholder="如：中度空洞 / 无明显空洞" /></el-form-item>
        <el-form-item label="倾斜程度">
          <el-select v-model="form.leanDegree" style="width: 200px">
            <el-option label="无" value="无" /><el-option label="轻微" value="轻微" /><el-option label="明显" value="明显" />
          </el-select>
        </el-form-item>
        <el-form-item label="枯枝情况"><el-input v-model="form.deadBranch" placeholder="如：大量枯枝 / 少量枯枝" /></el-form-item>
        <el-form-item label="树冠偏冠">
          <el-select v-model="form.crownDeviation" style="width: 200px">
            <el-option label="无明显偏冠" value="无明显偏冠" /><el-option label="轻度偏冠" value="轻度偏冠" /><el-option label="明显偏冠" value="明显偏冠" />
          </el-select>
        </el-form-item>
        <el-form-item label="病虫害"><el-input v-model="form.pestDesc" placeholder="如：有蛀干害虫痕迹" /></el-form-item>
        <el-form-item label="土壤生境"><el-input v-model="form.soilDesc" placeholder="如：土壤板结 / 树盘硬化" /></el-form-item>
        <el-form-item label="支撑设施">
          <el-select v-model="form.supportFacility" style="width: 200px">
            <el-option label="完好" value="完好" /><el-option label="松动" value="松动" /><el-option label="失效" value="失效" /><el-option label="缺失" value="缺失" />
          </el-select>
        </el-form-item>
        <el-form-item label="避雷设施">
          <el-select v-model="form.lightningFacility" style="width: 200px">
            <el-option label="完好" value="完好" /><el-option label="失效" value="失效" /><el-option label="缺失" value="缺失" />
          </el-select>
        </el-form-item>
        <el-form-item label="现场照片">
          <UploadField v-model="photo" biz-type="patrol" />
          <div class="muted" style="margin-top: 6px">照片按现场实拍上传，系统只保存相对路径</div>
        </el-form-item>
        <el-form-item label="巡查结论"><el-input v-model="form.result" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="submit">提交并自动分级</el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="viewDlg" title="巡查记录详情" width="640px">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="巡查编号">{{ cur.patrolCode }}</el-descriptions-item>
        <el-descriptions-item label="古树">{{ cur.treeCode }} {{ cur.species }}</el-descriptions-item>
        <el-descriptions-item label="巡查人">{{ cur.patrolUserName }}</el-descriptions-item>
        <el-descriptions-item label="巡查时间">{{ fmtDateTime(cur.patrolTime) }}</el-descriptions-item>
        <el-descriptions-item label="树干">{{ cur.trunkHollow || '-' }}</el-descriptions-item>
        <el-descriptions-item label="倾斜">{{ cur.leanDegree || '-' }}</el-descriptions-item>
        <el-descriptions-item label="枯枝">{{ cur.deadBranch || '-' }}</el-descriptions-item>
        <el-descriptions-item label="树冠">{{ cur.crownDeviation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="病虫害">{{ cur.pestDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item label="土壤">{{ cur.soilDesc || '-' }}</el-descriptions-item>
        <el-descriptions-item label="支撑">{{ cur.supportFacility || '-' }}</el-descriptions-item>
        <el-descriptions-item label="避雷">{{ cur.lightningFacility || '-' }}</el-descriptions-item>
        <el-descriptions-item label="分级" :span="2"><StatusTag :value="cur.issueLevel" /></el-descriptions-item>
        <el-descriptions-item label="结论" :span="2">{{ cur.result }}</el-descriptions-item>
      </el-descriptions>
      <div class="shots" v-if="cur.photos">
        <img v-for="(p, i) in String(cur.photos).split(',')" :key="i" :src="p" alt="现场照片" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import UploadField from '../components/UploadField.vue'
import { patrolApi, treeApi } from '../api'
import { fmtDateTime } from '../utils/format'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const dlg = ref(false)
const viewDlg = ref(false)
const cur = ref({})
const trees = ref([])
const photo = ref('')
const tabs = ['全部', '待办', '处理中', '已完成', '异常']
const q = reactive({ page: 1, size: 10, tab: '全部', issueLevel: '', keyword: '' })
const form = reactive({})

function blank() {
  return { treeId: null, patrolTime: '', trunkHollow: '', leanDegree: '无', deadBranch: '', crownDeviation: '无明显偏冠',
    pestDesc: '', soilDesc: '', supportFacility: '完好', lightningFacility: '完好', result: '' }
}

async function load() {
  loading.value = true
  try {
    const r = await patrolApi.recordPage(q)
    rows.value = r.records
    total.value = r.total
  } finally { loading.value = false }
}

function reload() { q.page = 1; load() }

function openForm() {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, blank())
  photo.value = ''
  dlg.value = true
}

async function submit() {
  if (!form.treeId) { ElMessage.warning('请选择古树'); return }
  form.photos = photo.value
  const r = await patrolApi.submit(form)
  ElMessage.success(`提交成功，系统判定为「${r.issueLevel}」${r.orderCode ? '，已生成抢救工单 ' + r.orderCode : ''}`)
  dlg.value = false
  load()
}

function show(row) { cur.value = row; viewDlg.value = true }

onMounted(async () => {
  await load()
  const r = await treeApi.page({ page: 1, size: 100 })
  trees.value = r.records || []
})
</script>

<style scoped>
.shots { display: flex; gap: 8px; margin-top: 12px; }
.shots img { width: 150px; height: 100px; object-fit: cover; border-radius: 6px; border: 1px solid #dbe7e8; }
.tabs .el-button { border-radius: 14px; }
</style>
