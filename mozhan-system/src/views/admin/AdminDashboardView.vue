<template>
  <PageShell title="工作台" :loading="loading">
    <template #actions>
      <el-radio-group v-model="preset" size="default" @change="onPresetChange">
        <el-radio-button value="today">今天</el-radio-button>
        <el-radio-button value="week">近 7 天</el-radio-button>
        <el-radio-button value="month">近 30 天</el-radio-button>
        <el-radio-button value="custom">自定义</el-radio-button>
      </el-radio-group>
      <el-date-picker
        v-if="preset === 'custom'"
        v-model="customRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :clearable="false"
        class="range-picker"
        @change="fetchData"
      />
    </template>

    <p v-if="overview" class="range-caption">
      统计区间：{{ overview.startDate }} 至 {{ overview.endDate }}
    </p>

    <!--
      这三个数是全站累计，不随上面的区间变化——和后端 AdminStatisticVO 的口径一致。
      所以脚注写「全站累计」而不是「区间内新增」：不写清楚，读者会以为改区间没生效。
    -->
    <div class="kpi-row">
      <div v-for="tile in tiles" :key="tile.key" class="kpi-tile">
        <div class="kpi-label">{{ tile.label }}</div>
        <div class="kpi-value">{{ overview ? overview[tile.key] : '—' }}</div>
        <div class="kpi-foot">全站累计</div>
      </div>
    </div>

    <div class="panel-card">
      <div class="panel-card-head">
        <h3>每日新增趋势</h3>
        <el-button link type="primary" @click="showTable = !showTable">
          {{ showTable ? '看图表' : '看表格' }}
        </el-button>
      </div>

      <TrendChart v-if="!showTable && trend.length" :trend="trend" />
      <div v-else-if="!showTable" class="empty-hint">该区间没有数据</div>

      <!--
        表格视图不只是"另一种展示"：折线只给形状，具体到某天的数值要靠准星 tooltip 才拿得到，
        而 tooltip 是要悬停的。表格让每个数都能被读到、被选中复制，也不依赖颜色和指针。
      -->
      <el-table v-else :data="trend" size="small" max-height="360">
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="userCount" label="新增用户" />
        <el-table-column prop="articleCount" label="新增文章" />
        <el-table-column prop="essayCount" label="新增随笔" />
      </el-table>
    </div>
  </PageShell>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageShell from '@/components/PageShell.vue'
import { getStatisticOverview } from '@/api/admin'
import TrendChart from '@/components/TrendChart.vue'

const preset = ref('week')
const customRange = ref([])
const overview = ref(null)
const loading = ref(false)
const showTable = ref(false)

const tiles = [
  { key: 'totalUserCount', label: '用户总数' },
  { key: 'totalArticleCount', label: '文章总数' },
  { key: 'totalEssayCount', label: '随笔总数' }
]

const trend = computed(() => overview.value?.dailyTrend || [])

function formatDate(d) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/**
 * 区间一律在前端算成两个具体日期再传给后端，
 * 「自定义」和三个预设因此走的是同一条请求路径，不会出现预设能查、自定义查不出的偏差。
 */
function resolveRange() {
  const today = new Date()
  const end = formatDate(today)
  if (preset.value === 'today') return { start: end, end }
  if (preset.value === 'week') {
    const s = new Date(today)
    s.setDate(s.getDate() - 6)
    return { start: formatDate(s), end }
  }
  if (preset.value === 'month') {
    const s = new Date(today)
    s.setDate(s.getDate() - 29)
    return { start: formatDate(s), end }
  }
  const [s, e] = customRange.value || []
  return s && e ? { start: s, end: e } : null
}

function onPresetChange(val) {
  if (val === 'custom') {
    // 切到自定义时先给一段默认区间，否则 picker 是空的、请求也没法发
    const today = new Date()
    const s = new Date(today)
    s.setDate(s.getDate() - 6)
    customRange.value = [formatDate(s), formatDate(today)]
  }
  fetchData()
}

async function fetchData() {
  const range = resolveRange()
  if (!range) return
  loading.value = true
  try {
    overview.value = await getStatisticOverview(range)
  } catch (error) {
    ElMessage.error(error.message || '获取统计数据失败')
  } finally {
    loading.value = false
  }
}

fetchData()
</script>

<style scoped>
.range-picker {
  width: 260px;
}

.range-caption {
  margin: 0 0 var(--sp-5, 24px);
  font-size: 13px;
  color: var(--ink-muted, #8a8580);
  letter-spacing: 0.04em;
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--sp-4, 16px);
  margin-bottom: var(--sp-5, 24px);
}

.kpi-tile {
  border: 1px solid var(--paper-deep, #ebe6dc);
  border-radius: 4px;
  padding: 18px 20px;
  transition: border-color var(--dur-base, 200ms) var(--ease-out);
}

.kpi-tile:hover {
  border-color: rgba(26, 26, 26, 0.22);
}

.kpi-label {
  font-size: 13px;
  color: var(--ink-muted, #8a8580);
  margin-bottom: 8px;
  letter-spacing: 0.08em;
}

/* 数字是这一屏的主角，字号要压过标题；等宽数字让三个磁贴的数位对齐 */
.kpi-value {
  font-size: 30px;
  font-weight: 600;
  line-height: 1.1;
  color: var(--ink, #1a1a1a);
  font-variant-numeric: tabular-nums;
}

.kpi-foot {
  margin-top: 6px;
  font-size: 12px;
  color: var(--ink-faint, #b5b0a8);
}

.panel-card {
  border: 1px solid var(--paper-deep, #ebe6dc);
  border-radius: 4px;
  padding: 20px;
  margin-bottom: 20px;
}

.panel-card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.panel-card-head h3 {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: var(--ink, #1a1a1a);
  letter-spacing: 0.08em;
}

.empty-hint {
  padding: 32px 0;
  text-align: center;
  color: var(--ink-faint, #b5b0a8);
  font-size: 13px;
}

@media (max-width: 720px) {
  .kpi-row {
    grid-template-columns: 1fr;
  }

  .range-picker {
    width: 100%;
  }
}
</style>
