<template>
  <div class="page" v-loading="loading">
    <div class="card" v-if="o.id">
      <div class="head">
        <h2 class="num">{{ o.orderCode }}</h2>
        <StatusTag :value="o.status" />
        <el-tag v-if="o.urgency === '紧急'" type="danger" size="small">紧急</el-tag>
        <span class="muted">{{ o.careType }} · {{ o.treeCode }} {{ o.species }}</span>
        <div class="acts">
          <el-button v-if="o.status === '待方案'" type="primary" @click="act('submit')">提交方案</el-button>
          <el-button v-if="o.status === '待审核'" type="primary" @click="auditPass">审核通过</el-button>
          <el-button v-if="o.status === '待审核'" type="danger" plain @click="reject">驳回</el-button>
          <el-button v-if="o.status === '执行中'" @click="progress">上报进度</el-button>
          <el-button v-if="o.status === '执行中'" type="primary" @click="finish">提交验收</el-button>
          <el-button v-if="o.status === '待验收'" type="primary" @click="accept">验收通过</el-button>
          <el-button v-if="o.status === '待验收'" plain @click="back">验收退回</el-button>
        </div>
      </div>

      <el-alert v-if="o.tempMeasure" type="warning" :closable="false" show-icon style="margin: 12px 0">
        紧急抢救临时措施：{{ o.tempMeasure }}，须于 {{ fmtDateTime(o.tempMeasureDeadline) }} 前补全方案与审批
      </el-alert>

      <el-descriptions :column="3" border size="small" style="margin-top: 12px">
        <el-descriptions-item label="养护单位">{{ o.executorOrg }}</el-descriptions-item>
        <el-descriptions-item label="执行人">{{ o.executorName }}</el-descriptions-item>
        <el-descriptions-item label="方案审核人">{{ o.planAuditorName || '待审核' }}</el-descriptions-item>
        <el-descriptions-item label="验收人">{{ o.acceptorName || '待验收' }}</el-descriptions-item>
        <el-descriptions-item label="截止日期"><span class="num">{{ o.deadline }}</span></el-descriptions-item>
        <el-descriptions-item label="进度">{{ o.progress }}%</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ fmtDateTime(o.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="最近更新">{{ fmtDateTime(o.updateTime) }}</el-descriptions-item>
        <el-descriptions-item label="来源问题">{{ o.issue ? o.issue.issueCode + ' · ' + o.issue.issueType : '无（计划养护）' }}</el-descriptions-item>
        <el-descriptions-item label="方案内容" :span="3">{{ o.planContent || '待养护单位填写' }}</el-descriptions-item>
        <el-descriptions-item v-if="o.rejectReason" label="驳回/退回原因" :span="3">
          <span style="color: #a62b32">{{ o.rejectReason }}</span>
        </el-descriptions-item>
        <el-descriptions-item v-if="o.resultDesc" label="执行结果" :span="3">{{ o.resultDesc }}</el-descriptions-item>
      </el-descriptions>
    </div>

    <div class="card">
      <div class="card-title">
        用药记录<span class="sub">登记剂量不得超过方案核定总量</span>
        <el-button size="small" type="primary" :disabled="o.status !== '执行中'" @click="dlg = true">登记用药</el-button>
      </div>
      <el-table :data="drugs" border size="small">
        <el-table-column prop="drugName" label="药品" min-width="120" />
        <el-table-column label="剂量" min-width="100"><template #default="{ row }"><span class="num">{{ row.dosage }} {{ row.unit }}</span></template></el-table-column>
        <el-table-column prop="applyDate" label="施用日期" min-width="110" />
        <el-table-column prop="operatorName" label="操作人" min-width="90" />
        <el-table-column label="方案核定" min-width="100"><template #default="{ row }"><span class="num">{{ row.planQuota }} {{ row.unit }}</span></template></el-table-column>
        <el-table-column label="累计用量" min-width="110"><template #default="{ row }"><span class="num">{{ row.usedQty }} {{ row.unit }}</span></template></el-table-column>
        <el-table-column prop="safetyMeasure" label="安全措施" min-width="260" show-overflow-tooltip />
        <template #empty><div class="empty-hint">暂无用药记录，工单进入执行中后方可登记</div></template>
      </el-table>
    </div>

    <div class="card">
      <div class="card-title">流转时间轴</div>
      <div v-for="(e, i) in o.timeline || []" :key="i" class="timeline-item">
        <div class="t">{{ fmtDateTime(e.time) }} · {{ e.operator }}</div>
        <div class="c"><b>{{ e.type }}</b> {{ e.content }}</div>
      </div>
    </div>

    <el-dialog v-model="dlg" title="登记用药" width="480px">
      <el-form :model="drug" label-width="96px">
        <el-form-item label="药品名称"><el-input v-model="drug.drugName" placeholder="如：多菌灵" /></el-form-item>
        <el-form-item label="本次剂量"><el-input-number v-model="drug.dosage" :min="0" :precision="2" :step="0.5" /></el-form-item>
        <el-form-item label="单位">
          <el-select v-model="drug.unit" style="width: 140px">
            <el-option label="千克" value="千克" /><el-option label="升" value="升" /><el-option label="克" value="克" />
          </el-select>
        </el-form-item>
        <el-form-item label="方案核定总量"><el-input-number v-model="drug.planQuota" :min="0" :precision="2" :step="1" /></el-form-item>
        <el-form-item label="施用日期"><el-date-picker v-model="drug.applyDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="安全措施"><el-input v-model="drug.safetyMeasure" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dlg = false">取消</el-button>
        <el-button type="primary" @click="saveDrug">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import StatusTag from '../components/StatusTag.vue'
import { careApi } from '../api'
import { fmtDateTime } from '../utils/format'

const route = useRoute()
const o = ref({})
const drugs = ref([])
const loading = ref(false)
const dlg = ref(false)
const drug = reactive({ drugName: '', dosage: 1, unit: '千克', planQuota: 5, applyDate: '', safetyMeasure: '' })

async function load() {
  loading.value = true
  try {
    o.value = await careApi.detail(route.params.id)
    drugs.value = o.value.drugs || []
  } finally { loading.value = false }
}

async function act(action, extra = {}) {
  await careApi.transition(route.params.id, { action, ...extra })
  ElMessage.success('操作成功')
  load()
}

async function auditPass() { act('audit') }

async function reject() {
  try {
    const { value } = await ElMessageBox.prompt('驳回原因（不少于 5 个字）', '驳回方案', { inputPlaceholder: '如：防治频次与方案不符' })
    if (!value || value.trim().length < 5) { ElMessage.warning('驳回原因不少于 5 个字'); return }
    await act('reject', { reason: value })
  } catch (e) { /* 取消 */ }
}

async function progress() {
  try {
    const { value } = await ElMessageBox.prompt('当前进度（0-100）', '上报进度', { inputValue: '60' })
    const n = Number(value)
    if (isNaN(n) || n < 0 || n > 100) { ElMessage.warning('进度需为 0-100 的数字'); return }
    await act('progress', { progress: n })
  } catch (e) { /* 取消 */ }
}

async function finish() {
  try {
    const { value } = await ElMessageBox.prompt('执行结果说明', '提交验收', { inputPlaceholder: '如：支撑加固完成，树洞已封堵' })
    await act('finish', { resultDesc: value })
  } catch (e) { /* 取消 */ }
}

async function accept() {
  try {
    const { value } = await ElMessageBox.prompt('验收意见', '验收通过', { inputPlaceholder: '如：现场核验合格，同意验收' })
    await act('accept', { resultDesc: value })
  } catch (e) { /* 取消 */ }
}

async function back() {
  try {
    const { value } = await ElMessageBox.prompt('退回原因（不少于 5 个字）', '验收退回', { inputPlaceholder: '如：支撑杆间距不符合方案要求' })
    if (!value || value.trim().length < 5) { ElMessage.warning('退回原因不少于 5 个字'); return }
    await act('back', { reason: value })
  } catch (e) { /* 取消 */ }
}

async function saveDrug() {
  try {
    const r = await careApi.addDrug(route.params.id, drug)
    ElMessage.success(`登记成功，累计用量 ${r.used}，剩余额度 ${r.remain}`)
    dlg.value = false
    load()
  } catch (e) { /* 超配额提示由拦截器给出 */ }
}

onMounted(load)
</script>

<style scoped>
.head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.head h2 { margin: 0; font-size: 18px; color: #174a5c; }
.acts { margin-left: auto; display: flex; gap: 8px; }
</style>
