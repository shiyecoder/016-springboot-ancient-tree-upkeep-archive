<template>
  <div class="page">
    <div class="grid2">
      <div class="card">
        <div class="card-title">角色列表<span class="sub">选择角色后在右侧分配菜单</span></div>
        <el-table :data="roles" border highlight-current-row @current-change="pick">
          <el-table-column prop="roleCode" label="编码" min-width="100" />
          <el-table-column prop="roleName" label="角色名称" min-width="140" />
          <el-table-column prop="dataScope" label="数据范围" min-width="100" />
          <el-table-column prop="description" label="说明" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>

      <div class="card">
        <div class="card-title">
          分配菜单<span class="sub">{{ curRole ? curRole.roleName : '请先选择角色' }}</span>
          <el-button size="small" type="primary" :disabled="!curRole" @click="save">保存授权</el-button>
        </div>
        <el-tree ref="tree" :data="treeData" show-checkbox node-key="id" :props="{ label: 'label', children: 'children' }"
                 default-expand-all style="max-height: 460px; overflow: auto" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { systemApi } from '../../api'

const roles = ref([])
const treeData = ref([])
const tree = ref(null)
const curRole = ref(null)

async function pick(row) {
  if (!row) return
  curRole.value = row
  const ids = await systemApi.roleMenus(row.roleCode)
  await nextTick()
  // 只勾选叶子节点，父节点由树组件自动推导，避免半选状态被误存
  const leafIds = []
  const walk = (nodes) => nodes.forEach((n) => {
    if (!n.children || !n.children.length) { if (ids.includes(n.id)) leafIds.push(n.id) }
    else walk(n.children)
  })
  walk(treeData.value)
  tree.value.setCheckedKeys(leafIds)
}

async function save() {
  const checked = tree.value.getCheckedKeys()
  const half = tree.value.getHalfCheckedKeys()
  await systemApi.assignMenus(curRole.value.roleCode, [...half, ...checked])
  ElMessage.success('授权已保存，该角色下次登录生效')
}

onMounted(async () => {
  roles.value = await systemApi.roles()
  treeData.value = await systemApi.menuTree()
})
</script>

<style scoped>
.grid2 { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
</style>
