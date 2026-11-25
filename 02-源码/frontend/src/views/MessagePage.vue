<template>
  <div class="page">
    <div class="card">
      <div class="card-title">消息中心<span class="sub">待审批 / 即将超时 / 已退回 / 待验收 / 处理完成</span>
        <el-button size="small" @click="readAll">全部标为已读</el-button>
      </div>
      <div class="msg" v-for="m in rows" :key="m.id" :class="{ unread: m.isRead === 0 }" @click="open(m)">
        <div class="top">
          <el-tag size="small" :type="m.msgType === '即将超时' ? 'warning' : m.msgType === '已退回' ? 'danger' : m.msgType === '处理完成' ? 'success' : 'primary'">
            {{ m.msgType }}
          </el-tag>
          <b>{{ m.title }}</b>
          <span class="time">{{ fromNow(m.createTime) }}</span>
        </div>
        <p>{{ m.content }}</p>
        <span class="code num">业务编号 {{ m.bizNo || '-' }}</span>
      </div>
      <div v-if="!rows.length" class="empty-hint">暂无消息</div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { systemApi } from '../api'
import { fromNow } from '../utils/format'

const rows = ref([])
const router = useRouter()

async function load() { rows.value = await systemApi.messages() }

async function open(m) {
  if (m.isRead === 0) await systemApi.readMsg(m.id)
  if (m.linkUrl) router.push(m.linkUrl)
  load()
}

async function readAll() {
  await systemApi.readAll()
  load()
}

onMounted(load)
</script>

<style scoped>
.msg { border-bottom: 1px dashed #e7eff0; padding: 12px 4px; cursor: pointer; }
.msg:hover { background: #f7fbfb; }
.msg.unread b::after { content: ''; display: inline-block; width: 6px; height: 6px; background: #a62b32; border-radius: 50%; margin-left: 6px; vertical-align: middle; }
.top { display: flex; align-items: center; gap: 10px; }
.top b { font-size: 14px; }
.time { margin-left: auto; font-size: 12px; color: #8ea3a7; }
.msg p { margin: 6px 0 4px; color: #33474c; font-size: 13px; }
.code { font-size: 12px; color: #5b6b70; }
</style>
