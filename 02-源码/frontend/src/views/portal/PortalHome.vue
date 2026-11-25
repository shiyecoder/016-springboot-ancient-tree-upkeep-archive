<template>
  <div class="portal-home">
    <section class="hero">
      <div class="hero-inner">
        <div class="txt">
          <h1>古树名木公开档案</h1>
          <p>本市已建档古树 {{ list.length }} 株 · 按保护等级与树龄分级管理 · 仅展示已审核通过的档案摘要</p>
          <div class="filters">
            <el-select v-model="species" placeholder="全部树种" clearable size="large" style="width: 180px" @change="load">
              <el-option v-for="s in ['银杏', '国槐', '侧柏', '香樟', '皂荚', '朴树', '黄连木', '枫杨']" :key="s" :label="s" :value="s" />
            </el-select>
            <el-select v-model="region" placeholder="全部区域" clearable size="large" style="width: 180px" @change="load">
              <el-option v-for="r in ['城东片区', '城西片区', '城南片区', '城北片区', '老城中心区']" :key="r" :label="r" :value="r" />
            </el-select>
            <el-button size="large" type="primary" @click="load">查询</el-button>
          </div>
        </div>
        <div class="badge">
          <b class="num">{{ list.length }}</b>
          <span>已公开档案</span>
        </div>
      </div>
    </section>

    <section class="list">
      <div class="card-grid">
        <article class="tcard" v-for="t in list" :key="t.id" @click="cur = t">
          <div class="cover">
            <img :src="t.coverImage" :alt="t.species" loading="lazy" />
            <span class="lvl">{{ t.protectLevel }}</span>
          </div>
          <div class="body">
            <h3>{{ t.species }}<span class="num">{{ t.treeCode }}</span></h3>
            <p class="age num">估测树龄 {{ t.ageEstimate }} 年</p>
            <p class="loc">{{ t.location }}</p>
            <p class="story">{{ (t.story || '').slice(0, 42) }}{{ (t.story || '').length > 42 ? '…' : '' }}</p>
          </div>
        </article>
      </div>
      <div v-if="!list.length" class="empty-hint">当前条件下没有公开档案，试试清空筛选条件</div>
    </section>

    <el-dialog v-model="dlg" :title="cur.species + ' · ' + cur.treeCode" width="720px">
      <div class="detail" v-if="cur.id">
        <img class="big" :src="cur.coverImage" :alt="cur.species" />
        <div class="meta">
          <p><span>保护等级</span>{{ cur.protectLevel }}</p>
          <p><span>估测树龄</span>{{ cur.ageEstimate }} 年</p>
          <p><span>所在位置</span>{{ cur.location }}</p>
          <p><span>健康等级</span>{{ cur.healthLevel }}</p>
          <p><span>历史故事</span>{{ cur.story }}</p>
        </div>
      </div>
      <div class="shots">
        <img v-for="img in cur.images || []" :key="img.id" :src="img.url" :alt="img.category" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { treeApi } from '../../api'

const list = ref([])
const species = ref('')
const region = ref('')
const cur = ref({})
const dlg = ref(false)

async function load() {
  list.value = await treeApi.publicList({ species: species.value, region: region.value })
}

onMounted(load)
</script>

<style scoped>
.hero { background: linear-gradient(120deg, #174a5c, #2a7f86); color: #fff; padding: 48px 24px; }
.hero-inner { max-width: 1200px; margin: 0 auto; display: flex; align-items: center; gap: 40px; }
.txt { flex: 1; }
.txt h1 { margin: 0 0 10px; font-size: 30px; letter-spacing: 1px; }
.txt p { margin: 0 0 20px; color: #cfe6e8; font-size: 13px; }
.filters { display: flex; gap: 10px; }
.badge { text-align: center; border: 1px solid rgba(255,255,255,0.35); border-radius: 12px; padding: 18px 30px; }
.badge b { font-size: 40px; display: block; font-variant-numeric: tabular-nums; }
.badge span { font-size: 12px; color: #cfe6e8; }

.list { max-width: 1200px; margin: 0 auto; padding: 24px; }
.card-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; }
.tcard { background: #fff; border: 1px solid #dbe7e8; border-radius: 8px; overflow: hidden; cursor: pointer; transition: box-shadow .2s; }
.tcard:hover { box-shadow: 0 6px 18px rgba(23,74,92,0.12); }
.cover { position: relative; }
.cover img { width: 100%; height: 150px; object-fit: cover; display: block; }
.lvl { position: absolute; left: 10px; top: 10px; background: rgba(23,74,92,0.9); color: #fff; font-size: 11px; padding: 2px 8px; border-radius: 10px; }
.body { padding: 12px 14px 14px; }
.body h3 { margin: 0 0 4px; font-size: 15px; color: #174a5c; display: flex; justify-content: space-between; align-items: baseline; }
.body h3 span { font-size: 11px; color: #8ea3a7; font-weight: 400; }
.age { margin: 0 0 4px; font-size: 12px; color: #2a7f86; }
.loc { margin: 0 0 6px; font-size: 12px; color: #5b6b70; }
.story { margin: 0; font-size: 12px; color: #7b8b90; line-height: 1.6; height: 38px; overflow: hidden; }
.detail { display: flex; gap: 16px; }
.big { width: 300px; height: 200px; object-fit: cover; border-radius: 8px; }
.meta p { margin: 0 0 8px; font-size: 13px; }
.meta span { color: #5b6b70; display: inline-block; width: 72px; }
.shots { display: flex; gap: 8px; margin-top: 14px; flex-wrap: wrap; }
.shots img { width: 132px; height: 88px; object-fit: cover; border-radius: 6px; border: 1px solid #dbe7e8; }
</style>
