<template>
  <div class="trend-chart" ref="wrapRef">
    <div class="legend">
      <span v-for="s in series" :key="s.key" class="legend-item">
        <i class="legend-swatch" :style="{ background: s.color }"></i>
        <span class="legend-label">{{ s.label }}</span>
      </span>
    </div>

    <!--
      三个指标画成小倍数（每个指标一行、共用同一把 y 轴刻度），而不是每天挤三根柱子。
      原因是区间可以拉到 30 天甚至自定义到一年：30 天 × 3 系列 = 90 根柱子，
      每根只有几个像素宽，读不出任何东西。拆成三行后每行都有完整宽度。
      共用刻度而不是各用各的，是因为三个指标同为「新增条数」，同一把尺子才能横向比较。
    -->
    <div class="panel" v-for="s in series" :key="s.key">
      <div class="panel-head">
        <span class="panel-title">{{ s.label }}</span>
        <span class="panel-total">{{ totalOf(s.key) }}</span>
      </div>
      <svg :width="width" :height="panelHeight" role="img" :aria-label="ariaLabel(s)">
        <!-- 网格线：贴纸色一档的灰、1px 实线，不抢数据的视觉重量 -->
        <line
          v-for="t in ticks"
          :key="`g${t}`"
          :x1="padLeft"
          :x2="Math.max(padLeft, width - padRight)"
          :y1="yFor(t)"
          :y2="yFor(t)"
          class="gridline"
        />
        <text
          v-for="t in ticks"
          :key="`t${t}`"
          :x="padLeft - 8"
          :y="yFor(t) + 3.5"
          class="axis-text"
          text-anchor="end"
        >{{ t }}</text>

        <g v-for="(bar, i) in bars" :key="bar.date">
          <path
            v-if="bar[s.key] > 0"
            :d="barPath(i, bar[s.key])"
            :fill="s.color"
            class="bar"
            :class="{ 'is-hover': isHovered(s.key, bar.date) }"
          />
          <!--
            整条 band 都是命中区，且画在柱子之后（即上层）：
            柱子最窄时只有 2px，靠柱子自己根本 hover 不到。
          -->
          <rect
            :x="padLeft + i * band"
            :y="0"
            :width="band"
            :height="plotHeight"
            fill="transparent"
            @mouseenter="onBarEnter($event, s, bar)"
            @mouseleave="hover = null"
          />
        </g>

        <!--
          只给峰值柱直接标数值（选择性直标，不是每根都标）。
          它同时满足绿柱对比度不足时的「补偿」要求：读者不依赖颜色也能拿到关键值，
          完整数值还有 tooltip 和下方的表格视图兜底。
        -->
        <text
          v-if="peakIndex(s.key) >= 0"
          :x="peakLabelX(s.key)"
          :y="yFor(peakValue(s.key)) - 5"
          class="peak-label"
          text-anchor="middle"
        >{{ peakValue(s.key) }}</text>

        <text
          v-for="t in xTicks"
          :key="`x${t.i}`"
          :x="t.x"
          :y="plotHeight + 16"
          class="axis-text"
          text-anchor="middle"
        >{{ t.label }}</text>
      </svg>
    </div>

    <div
      v-if="hover"
      class="chart-tooltip"
      :style="{ left: hover.x + 'px', top: hover.y + 'px' }"
    >
      <div class="tt-date">{{ hover.bar.fullDate }}</div>
      <div class="tt-row">
        <i class="legend-swatch" :style="{ background: hover.color }"></i>
        <span>{{ hover.label }}</span>
        <b>{{ hover.bar.value }}</b>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  trend: { type: Array, default: () => [] }
})

// 这三支颜色不是随手挑的。它们是按 dataviz 的校验脚本在纸色 #f6f3ed 上跑出来的：
// 全配对最差 CVD ΔE 10.7、常视觉 ΔE 21.3，均高于 8 / 15 的硬门槛，色度也都过了 0.10 下限。
// 改颜色前请重新跑校验，别只凭肉眼判断（尤其别把花青和石绿调近）。
const series = [
  { key: 'userCount', label: '新增用户', color: '#2a6ea8' },
  { key: 'articleCount', label: '新增文章', color: '#a3542a' },
  { key: 'essayCount', label: '新增随笔', color: '#6aa03c' }
]

const wrapRef = ref(null)
const width = ref(760)
const hover = ref(null)

const panelHeight = 84
const padLeft = 34
const padRight = 14
const plotHeight = panelHeight - 24

const bars = computed(() => props.trend)
const band = computed(() => {
  const n = bars.value.length || 1
  return Math.max(0, (width.value - padLeft - padRight) / n)
})

// 三个面板共用一把 y 轴：取全局最大值向上取整到好看的数字
const maxValue = computed(() => {
  const m = Math.max(1, ...bars.value.map(d => Math.max(d.userCount || 0, d.articleCount || 0, d.essayCount || 0)))
  return m <= 4 ? m : Math.ceil(m / 5) * 5
})

const ticks = computed(() => {
  const m = maxValue.value
  return m <= 2 ? [0, m] : [0, Math.round(m / 2), m]
})

function yFor(v) {
  const ratio = maxValue.value === 0 ? 0 : v / maxValue.value
  return plotHeight - ratio * (plotHeight - 10)
}

function totalOf(key) {
  return bars.value.reduce((sum, d) => sum + (d[key] || 0), 0)
}

function peakIndex(key) {
  let best = -1
  bars.value.forEach((d, i) => {
    if ((d[key] || 0) > 0 && (best < 0 || (d[key] || 0) > (bars.value[best][key] || 0))) best = i
  })
  return best
}

function peakValue(key) {
  const i = peakIndex(key)
  return i < 0 ? 0 : bars.value[i][key]
}

function peakLabelX(key) {
  return padLeft + peakIndex(key) * band.value + band.value / 2
}

function isHovered(key, date) {
  return !!hover.value && hover.value.key === key && hover.value.bar.date === date
}

function barWidth() {
  // 柱子最粗 24px：不要让柱子填满整个 band，留白由 band 自己承担
  return Math.max(2, Math.min(24, band.value * 0.62))
}

function barPath(i, value) {
  const w = barWidth()
  const x = padLeft + i * band.value + (band.value - w) / 2
  const y = yFor(value)
  // 非零值至少给 2px 高度，否则在大量级刻度下会消失成 0
  const h = Math.max(2, plotHeight - y)
  const r = Math.min(4, w / 2, h)
  const top = plotHeight - h
  return [
    `M ${x} ${plotHeight}`,
    `L ${x} ${top + r}`,
    `Q ${x} ${top} ${x + r} ${top}`,
    `L ${x + w - r} ${top}`,
    `Q ${x + w} ${top} ${x + w} ${top + r}`,
    `L ${x + w} ${plotHeight}`,
    'Z'
  ].join(' ')
}

// x 轴标签：按可用宽度算出最多能放几个，避免 30 天时挤成一团
const xTicks = computed(() => {
  const n = bars.value.length
  if (!n) return []
  const plotW = width.value - padLeft - padRight
  const maxLabels = Math.max(2, Math.floor(plotW / 52))
  const step = Math.max(1, Math.ceil(n / maxLabels))
  const out = []
  for (let i = 0; i < n; i += step) {
    out.push({ i, x: padLeft + i * band.value + band.value / 2, label: shortDate(bars.value[i].date) })
  }
  const lastIdx = n - 1
  if (out.length === 0 || out[out.length - 1].i !== lastIdx) {
    out.push({ i: lastIdx, x: padLeft + lastIdx * band.value + band.value / 2, label: shortDate(bars.value[lastIdx].date) })
  }
  return out
})

function shortDate(d) {
  return d ? String(d).slice(5) : ''
}

function ariaLabel(s) {
  const t = totalOf(s.key)
  return `${s.label}每日趋势，区间内合计 ${t} 条`
}

function onBarEnter(event, s, bar) {
  const rect = wrapRef.value.getBoundingClientRect()
  hover.value = {
    key: s.key,
    label: s.label,
    color: s.color,
    bar,
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  }
}

let observer = null
onMounted(() => {
  // 文本跟着 viewBox 缩放会失真，所以按容器真实宽度渲染，而不是用固定 viewBox 拉伸
  observer = new ResizeObserver(entries => {
    for (const e of entries) {
      const w = Math.round(e.contentRect.width)
      if (w > 0) width.value = w
    }
  })
  if (wrapRef.value) observer.observe(wrapRef.value)
})

onBeforeUnmount(() => {
  if (observer) observer.disconnect()
})
</script>

<style scoped>
.trend-chart {
  position: relative;
  width: 100%;
}

.legend {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

/* 文字一律用墨色，颜色只由旁边的色块承担——浅色系列作文字会失去可读性 */
.legend-label {
  font-size: 12px;
  color: var(--ink-light, #4a4a4a);
}

.legend-swatch {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}

.panel + .panel {
  margin-top: 6px;
}

.panel-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding-left: 34px;
}

.panel-title {
  font-size: 12px;
  color: var(--ink-muted, #8a8580);
}

.panel-total {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink, #1a1a1a);
}

.gridline {
  stroke: var(--paper-deep, #ebe6dc);
  stroke-width: 1;
}

.axis-text {
  font-size: 11px;
  fill: var(--ink-muted, #8a8580);
}

.bar {
  transition: opacity 0.15s ease;
}

.bar.is-hover {
  opacity: 0.78;
}

.peak-label {
  font-size: 11px;
  font-weight: 600;
  fill: var(--ink, #1a1a1a);
}

.hit {
  cursor: default;
}

.chart-tooltip {
  position: absolute;
  transform: translate(-50%, calc(-100% - 10px));
  background: var(--ink, #1a1a1a);
  color: #fff;
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 2px;
  pointer-events: none;
  white-space: nowrap;
  z-index: 10;
}

.tt-date {
  opacity: 0.7;
  margin-bottom: 3px;
}

.tt-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

.tt-row b {
  margin-left: auto;
  padding-left: 8px;
}
</style>
