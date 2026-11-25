<template>
  <div class="page">
    <div class="card">
      <div class="card-title">菜单管理<span class="sub">左侧为目录/菜单结构；「菜单管理」入口对业务角色隐藏</span></div>
      <el-table :data="treeRows" border row-key="id" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column prop="menuName" label="菜单名称" min-width="220" />
        <el-table-column prop="path" label="路由" min-width="180" />
        <el-table-column prop="icon" label="图标" min-width="120" />
        <el-table-column prop="menuType" label="类型" min-width="90" />
        <el-table-column prop="sortNo" label="排序" min-width="80" />
        <el-table-column label="可见" min-width="80">
          <template #default="{ row }"><el-tag size="small" :type="row.visible === 1 ? 'success' : 'info'">{{ row.visible === 1 ? '显示' : '隐藏' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="说明" min-width="260">
          <template #default="{ row }">
            <span v-if="row.path === '/system/menu'" class="muted">仅管理员可用，业务角色菜单不下发</span>
            <span v-else class="muted">-</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { systemApi } from '../../api'

const treeRows = ref([])

/** 菜单表按父子结构展示，便于核对层级 */
function buildTree(list) {
  const map = new Map()
  list.forEach((m) => map.set(m.id, { ...m, children: [] }))
  const roots = []
  map.forEach((m) => {
    const p = map.get(m.parentId)
    if (p) p.children.push(m)
    else roots.push(m)
  })
  return roots
}

onMounted(async () => {
  const list = await systemApi.menus()
  treeRows.value = buildTree(list)
})
</script>
