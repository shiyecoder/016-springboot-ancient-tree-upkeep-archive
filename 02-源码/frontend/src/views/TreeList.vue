<template>
  <div class="page">
    <!-- 筛选区：常用条件外露，其余放高级筛选抽屉 -->
    <div class="filter-bar">
      <el-form inline>
        <el-form-item label="树种">
          <el-select v-model="q.species" placeholder="全部树种" clearable @change="reload">
            <el-option v-for="s in speciesList" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="保护等级">
          <el-select v-model="q.protectLevel" placeholder="全部等级" clearable @change="reload">
            <el-option label="一级" value="一级" /><el-option label="二级" value="二级" />
            <el-option label="三级" value="三级" /><el-option label="后续资源" value="后续资源" />
          </el-select>
        </el-form-item>
        <el-form-item label="区域">
          <el-select v-model="q.region" placeholder="全部区域" clearable @change="reload">
            <el-option v-for="r in regions" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="生长状况">
          <el-select v-model="q.growthStatus" placeholder="全部" clearable @change="reload">
            <el-option v-for="g in growthList" :key="g" :label="g" :value="g" />
          </el-select>
        </el-form-item>
        <el-form-item label="最近巡查">
          <el-date-picker v-model="dateRange" type="daterange" start-placeholder="开始日期"
                          end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="reload" />
        </el-form-item>
        <el-form-item label="关键字">
          <el-input v-model="q.keyword" placeholder="编号 / 位置 / 树种" clearable @keyup.enter="reload" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="reload">查询</el-button>
          <el-button @click="reset">重置</el-button>
          <el-button link type="primary" @click="adv = true">高级筛选</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="card">
      <div class="toolbar">
        <div class="tabs">
          <el-button v-for="t in tabs" :key="t" size="small" :type="tab === t ? 'primary' : 'default'"
                     @click="switchTab(t)">{{ t }}</el-button>
        </div>
        <div>
          <el-button @click="exportCsv">导出当前结果</el-button>
          <el-button type="primary" @click="openForm()">新增档案</el-button>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" border stripe style="width: 100%" table-layout="auto">
        <el-table-column label="照片" width="76">
          <template #default="{ row }">
            <img v-if="row.coverImage" class="thumb" :src="row.coverImage" :alt="row.species" />
            <span v-else class="muted">无图</span>
          </template>
        </el-table-column>
        <el-table-column prop="treeCode" label="档案编号" min-width="120" show-overflow-tooltip />
        <el-table-column prop="species" label="树种" min-width="80" />
        <el-table-column prop="ageEstimate" label="树龄(年)" min-width="90" align="right">
          <template #default="{ row }"><span class="num">{{ row.ageEstimate }}</span></template>
        </el-table-column>
        <el-table-column prop="protectLevel" label="保护等级" min-width="96">
          <template #default="{ row }">
            <el-tag size="small" :type="row.protectLevel === '一级' ? 'danger' : row.protectLevel === '二级' ? 'warning' : 'info'">
              {{ row.protectLevel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="所在位置" min-width="220" show-overflow-tooltip />
        <el-table-column prop="healthLevel" label="健康等级" min-width="92">
          <template #default="{ row }"><StatusTag :value="row.healthLevel" /></template>
        </el-table-column>
        <el-table-column prop="managerOrg" label="责任单位" min-width="160" show-overflow-tooltip />
        <el-table-column prop="lastPatrolDate" label="最近巡查" min-width="112">
          <template #default="{ row }"><span class="num">{{ row.lastPatrolDate || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="archiveStatus" label="档案状态" min-width="96">
          <template #default="{ row }"><StatusTag :value="row.archiveStatus" /></template>
        </el-table-column>
        <el-table-column label="操作" width="188" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/tree/detail/' + row.id)">查看</el-button>
            <el-button link type="primary" @click="openForm(row)">编辑</el-button>
            <el-dropdown trigger="click" @command="(c) => onCommand(c, row)">
              <el-button link type="primary">更多<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="change">申请变更</el-dropdown-item>
                  <el-dropdown-item command="evaluate">生成养护建议</el-dropdown-item>
                  <el-dropdown-item command="archive">状态流转</el-dropdown-item>
                  <el-dropdown-item divided command="delete">删除档案</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
        <template #empty>
          <div class="empty-hint">当前筛选条件下无记录，清空筛选条件或点击右上角「新增档案」</div>
        </template>
      </el-table>

      <div class="pager">
        <el-pagination background layout="total, sizes, prev, pager, next, jumper" :total="total"
                       v-model:current-page="q.page" v-model:page-size="q.size" :page-sizes="[10, 20, 50]"
                       @size-change="onSizeChange" @current-change="load" />
      </div>
    </div>

    <!-- 新增/编辑：右侧抽屉，关闭前检测未保存内容 -->
    <el-drawer v-model="formDrawer" :title="form.id ? '编辑档案' : '新增古树档案'" size="560px" :before-close="beforeClose">
      <el-form :model="form" label-width="104px" ref="fref" :rules="formRules">
        <el-form-item label="档案编号" prop="treeCode">
          <el-input v-model="form.treeCode" :disabled="!!form.id" placeholder="留空由系统自动生成" />
        </el-form-item>
        <el-form-item label="树种" prop="species">
          <el-select v-model="form.species" style="width: 240px" placeholder="请选择或输入">
            <el-option v-for="s in speciesList" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="估测树龄" prop="ageEstimate">
          <el-input-number v-model="form.ageEstimate" :min="1" :max="3000" />
          <span class="muted" style="margin-left: 8px">年</span>
        </el-form-item>
        <el-form-item label="保护等级" prop="protectLevel">
          <el-select v-model="form.protectLevel" style="width: 240px">
            <el-option label="一级" value="一级" /><el-option label="二级" value="二级" />
            <el-option label="三级" value="三级" /><el-option label="后续资源" value="后续资源" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在位置" prop="location">
          <el-input v-model="form.location" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="所属区域">
          <el-select v-model="form.region" style="width: 240px">
            <el-option v-for="r in regions" :key="r" :label="r" :value="r" />
          </el-select>
        </el-form-item>
        <el-form-item label="所有权">
          <el-select v-model="form.ownership" style="width: 240px">
            <el-option label="国家所有" value="国家所有" /><el-option label="集体所有" value="集体所有" />
            <el-option label="私人所有" value="私人所有" />
          </el-select>
        </el-form-item>
        <el-form-item label="树高 / 胸径">
          <el-input-number v-model="form.treeHeight" :min="0" :precision="2" :step="0.5" style="width: 120px" />
          <span class="muted" style="margin: 0 8px">米 /</span>
          <el-input-number v-model="form.diameter" :min="0" :precision="2" :step="5" style="width: 120px" />
        </el-form-item>
        <el-form-item label="冠幅">
          <el-input-number v-model="form.crownWidth" :min="0" :precision="2" :step="0.5" style="width: 120px" />
          <span class="muted" style="margin-left: 8px">米</span>
        </el-form-item>
        <el-form-item label="健康等级">
          <el-select v-model="form.healthLevel" style="width: 240px">
            <el-option v-for="h in healthList" :key="h" :label="h" :value="h" />
          </el-select>
        </el-form-item>
        <el-form-item label="生长状况" prop="growthStatus">
          <el-input v-model="form.growthStatus" maxlength="50" />
        </el-form-item>
        <el-form-item label="责任单位">
          <el-select v-model="form.managerOrg" style="width: 240px" clearable>
            <el-option v-for="o in orgs" :key="o.id" :label="o.orgName" :value="o.orgName" />
          </el-select>
        </el-form-item>
        <el-form-item label="最近巡查">
          <el-date-picker v-model="form.lastPatrolDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="树木照片">
          <UploadField v-model="form.coverImage" biz-type="tree" />
        </el-form-item>
        <el-form-item label="历史故事">
          <el-input v-model="form.story" type="textarea" :rows="3" maxlength="1000" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDrawer = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-drawer>

    <!-- 变更申请 -->
    <el-dialog v-model="changeDlg" title="重要信息变更申请" width="520px">
      <p class="muted" style="margin-top: 0">保护级别、责任单位、树体状态变更需经审核，原档案保留历史版本。</p>
      <el-form label-width="90px">
        <el-form-item label="变更类型">
          <el-select v-model="change.changeType" style="width: 220px">
            <el-option label="保护级别" value="保护级别" /><el-option label="责任单位" value="责任单位" />
            <el-option label="树体状态" value="树体状态" />
          </el-select>
        </el-form-item>
        <el-form-item label="变更后值">
          <el-input v-model="change.afterValue" placeholder="例如：一级 / 城西园林工程有限公司 / 差" />
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="change.reason" type="textarea" :rows="3" placeholder="不少于 5 个字" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDlg = false">取消</el-button>
        <el-button type="primary" @click="submitChange">提交申请</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="adv" title="高级筛选" size="360px">
      <el-form label-width="100px">
        <el-form-item label="档案状态">
          <el-select v-model="q.archiveStatus" clearable placeholder="全部" style="width: 200px">
            <el-option label="正常" value="正常" /><el-option label="已迁移" value="已迁移" />
            <el-option label="已死亡" value="已死亡" /><el-option label="已销号" value="已销号" />
          </el-select>
        </el-form-item>
        <el-form-item label="公开审核">
          <el-select v-model="q.auditStatus" clearable placeholder="全部" style="width: 200px">
            <el-option label="已通过" value="已通过" /><el-option label="待审核" value="待审核" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="adv = false; reload()">应用筛选</el-button>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import UploadField from '../components/UploadField.vue'
import { treeApi, systemApi, analysisApi } from '../api'

const router = useRouter()
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const saving = ref(false)
const adv = ref(false)
const tab = ref('全部')
const tabs = ['全部', '一级古树', '待审核', '异常']
const q = reactive({ page: 1, size: 10, species: '', protectLevel: '', region: '', growthStatus: '', archiveStatus: '', auditStatus: '', keyword: '' })
const dateRange = ref([])
const speciesList = ['银杏', '国槐', '侧柏', '香樟', '皂荚', '朴树', '黄连木', '枫杨']
const regions = ['城东片区', '城西片区', '城南片区', '城北片区', '老城中心区']
const growthList = ['枝叶繁茂，长势稳定', '长势较好', '长势平稳', '长势良好', '生长正常']
const healthList = ['优', '良', '中', '差', '濒危']
const orgs = ref([])

const formDrawer = ref(false)
const fref = ref(null)
const form = reactive(emptyForm())
const changeDlg = ref(false)
const change = reactive({ treeId: null, changeType: '保护级别', afterValue: '', reason: '' })
const formRules = {
  species: [{ required: true, message: '请选择树种', trigger: 'change' }],
  protectLevel: [{ required: true, message: '请选择保护等级', trigger: 'change' }],
  location: [{ required: true, message: '请填写所在位置', trigger: 'blur' }],
  growthStatus: [{ required: true, message: '请填写生长状况', trigger: 'blur' }]
}

function emptyForm() {
  return {
    id: null, treeCode: '', species: '', ageEstimate: 100, location: '', ownership: '国家所有',
    protectLevel: '', treeHeight: null, diameter: null, crownWidth: null, healthLevel: '良',
    growthStatus: '', story: '', region: '', coverImage: '', managerOrg: '', lastPatrolDate: '', version: null
  }
}

async function load() {
  loading.value = true
  try {
    const p = { ...q, startDate: dateRange.value?.[0] || '', endDate: dateRange.value?.[1] || '' }
    const r = await treeApi.page(p)
    rows.value = r.records || []
    total.value = r.total || 0
  } finally {
    loading.value = false
  }
}

// 切换筛选条件后页码回到第 1 页
function reload() {
  q.page = 1
  load()
}

function reset() {
  Object.assign(q, { page: 1, species: '', protectLevel: '', region: '', growthStatus: '', archiveStatus: '', auditStatus: '', keyword: '' })
  dateRange.value = []
  tab.value = '全部'
  load()
}

function switchTab(t) {
  tab.value = t
  if (t === '一级古树') { q.protectLevel = '一级'; q.auditStatus = '' }
  else if (t === '待审核') { q.auditStatus = '待审核'; q.protectLevel = '' }
  else if (t === '异常') { q.protectLevel = ''; q.auditStatus = '' }
  else { q.protectLevel = ''; q.auditStatus = '' }
  reload()
}

function onSizeChange() {
  q.page = 1
  load()
}

function openForm(row) {
  Object.assign(form, emptyForm())
  if (row) Object.assign(form, row)
  formDrawer.value = true
}

async function save() {
  await fref.value.validate(async (ok) => {
    if (!ok) return
    saving.value = true
    try {
      if (form.id) await treeApi.update(form.id, form)
      else await treeApi.create(form)
      ElMessage.success('保存成功')
      formDrawer.value = false
      load()
    } catch (e) { /* 已提示 */ } finally {
      saving.value = false
    }
  })
}

async function beforeClose(done) {
  try {
    await ElMessageBox.confirm('关闭后未保存的内容将丢失，确认关闭？', '提示', { type: 'warning' })
    done()
  } catch (e) { /* 取消关闭 */ }
}

function onCommand(cmd, row) {
  if (cmd === 'change') {
    Object.assign(change, { treeId: row.id, changeType: '保护级别', afterValue: '', reason: '' })
    changeDlg.value = true
  } else if (cmd === 'evaluate') {
    analysisApi.evaluate(row.id, {}).then((r) => {
      ElMessage.success(`计算完成：${r.resultLevel}（${r.totalScore} 分），已保存计算留痕`)
      router.push('/season')
    })
  } else if (cmd === 'archive') {
    ElMessageBox.prompt('请输入流转原因（不少于 5 个字）', '档案状态流转', {
      inputPlaceholder: '如：经审批实施整体迁移保护'
    }).then(async ({ value }) => {
      await treeApi.change(row.id, {
        changeType: '树体状态', reason: value,
        afterJson: JSON.stringify({ archiveStatus: '已迁移' })
      })
      ElMessage.success('流转申请已提交，等待审核')
      load()
    }).catch(() => {})
  } else if (cmd === 'delete') {
    ElMessageBox.confirm('删除后该档案将从列表移除，确认继续？', '删除确认', { type: 'warning' })
      .then(async () => {
        try {
          await treeApi.remove(row.id)
          ElMessage.success('已删除')
          load()
        } catch (e) { /* 已提示引用限制 */ }
      }).catch(() => {})
  }
}

async function submitChange() {
  if (!change.reason || change.reason.trim().length < 5) {
    ElMessage.warning('申请原因不少于 5 个字')
    return
  }
  const key = change.changeType === '保护级别' ? 'protectLevel' : change.changeType === '责任单位' ? 'managerOrg' : 'growthStatus'
  await treeApi.change(change.treeId, {
    changeType: change.changeType,
    reason: change.reason,
    afterJson: JSON.stringify({ [key]: change.afterValue })
  })
  ElMessage.success('变更申请已提交，等待审核')
  changeDlg.value = false
  load()
}

/** 按当前筛选结果导出 CSV，记录发起人与筛选条件 */
function exportCsv() {
  const head = ['档案编号', '树种', '树龄', '保护等级', '位置', '区域', '健康等级', '责任单位', '最近巡查', '档案状态']
  const lines = [head.join(',')]
  rows.value.forEach((r) => lines.push([r.treeCode, r.species, r.ageEstimate, r.protectLevel, `"${r.location}"`, r.region, r.healthLevel, r.managerOrg, r.lastPatrolDate, r.archiveStatus].join(',')))
  const blob = new Blob(['\ufeff' + lines.join('\n')], { type: 'text/csv;charset=utf-8' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `古树档案_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  ElMessage.success(`已导出 ${rows.value.length} 条当前筛选结果`)
}

onMounted(async () => {
  await load()
  try {
    orgs.value = await systemApi.orgs()
  } catch (e) { /* 无权限时忽略 */ }
})
</script>

<style scoped>
.tabs .el-button { border-radius: 14px; }
</style>
