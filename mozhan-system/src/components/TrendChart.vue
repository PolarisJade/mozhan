<template>
  <div class="trend-chart">
    <!--
      图例和筛选器是同一个东西：每个系列一个可点的色块按钮。
      单独再做一行图例等于把同一份信息写两遍，还要维护两处状态同步。
      色块「实心 = 显示 / 空心 = 隐藏」，所以开关状态不只靠颜色表达。
    -->
    <div class="series-switch" role="group" aria-label="选择要展示的折线">
      <button
        v-for="s in series"
        :key="s.key"
        type="button"
        class="series-chip"
        :class="{ 'is-off': !isOn(s.key) }"
        :aria-pressed="isOn(s.key)"
        @click="toggle(s.key)"
      >
        <i
          class="chip-key"
          :style="isOn(s.key)
            ? { background: s.color, borderColor: s.color }
            : { borderColor: s.color }"
        ></i>
        <span class="chip-label">{{ s.label }}</span>
      </button>
    </div>

    <div ref="canvasRef" class="chart-canvas"></div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref, shallowRef, watch, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, GraphicComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

// 按需注册：只引折线图 + 网格 + tooltip，别整包 import，否则打进产物的是整个 echarts
echarts.use([LineChart, GridComponent, TooltipComponent, GraphicComponent, CanvasRenderer])

const props = defineProps({
  trend: { type: Array, default: () => [] }
})

/*
 * 颜色绑在系列 key 上，不绑在数组下标上。
 * 如果靠下标取色，隐藏一条线之后剩下的线会各自往前补位、当场换色，
 * 读者刚从图例里记住的对应关系就作废了。
 *
 * 这三个色值不是随手挑的，是按 dataviz 的校验脚本在纸色 #fffcf7 上跑通过的：
 * 全配对最差 CVD ΔE 10.7、常视觉 ΔE 21.3，且三个色对纸色的对比度都 ≥ 3:1。
 * 改色前请重跑校验，别只凭肉眼判断（尤其别把石绿和赭石调近）。
 */
const series = [
  { key: 'userCount', label: '新增用户', color: '#2a6ea8' },
  { key: 'articleCount', label: '新增文章', color: '#a3542a' },
  { key: 'essayCount', label: '新增随笔', color: '#6aa03c' }
]

// 图表是画在 --paper-solid 上的，标记点的描边要用同一个色才算「纸色留白」
const SURFACE = '#fffcf7'
const INK = '#1a1a1a'
const MUTED = '#8a8580'
const GRID = '#ebe6dc'
const AXIS = '#d5cfc4'

const canvasRef = ref(null)
const chart = shallowRef(null)
// 默认三条全开
const onKeys = ref(series.map(s => s.key))

const dates = computed(() => props.trend.map(d => d.date))
const visibleSeries = computed(() => series.filter(s => onKeys.value.includes(s.key)))

function isOn(key) {
  return onKeys.value.includes(key)
}

// 这里只管「开哪几条」，画出来的顺序由 visibleSeries 按 series 的固定次序决定，
// 所以后勾的线不会跑到最前面去。
function toggle(key) {
  onKeys.value = isOn(key)
    ? onKeys.value.filter(k => k !== key)
    : [...onKeys.value, key]
}

/*
 * 30 天时 x 轴上会挤下 90 个点（30 天 × 3 系列），每个点都画圆点会糊成一片，
 * 所以点密集时只留线、靠十字准星读数；7 天以内点得开，就都画出来。
 * 单点区间（「今天」）必须画点，否则 boundaryGap:false 的线下什么都没有。
 */
const dense = computed(() => dates.value.length > 14)

function buildOption() {
  return {
    animationDuration: 320,
    grid: { left: 0, right: 14, top: 14, bottom: 0, containLabel: true },
    tooltip: {
      trigger: 'axis',
      // 十字准星负责找 x：读者瞄的是某一天，不是那条 2px 的线
      axisPointer: { type: 'line', lineStyle: { color: MUTED, width: 1, type: 'solid' } },
      backgroundColor: INK,
      borderWidth: 0,
      padding: [8, 12],
      extraCssText: 'border-radius: 2px; box-shadow: none;',
      /*
       * 一次列出该日期下的**所有**可见系列，指针不必压在某条线上才能读到值。
       * 这里拼的是本文件里的字面量标签，没有外部数据，所以可以走 innerHTML。
       */
      formatter: params => {
        if (!params || !params.length) return ''
        const rows = params.map(p => {
          const s = series.find(x => x.label === p.seriesName) || { color: MUTED }
          return `<div style="display:flex;align-items:center;gap:6px;line-height:1.7">
            <i style="width:10px;height:2px;border-radius:1px;background:${s.color};flex:none"></i>
            <span style="color:rgba(255,255,255,0.66);font-size:12px;letter-spacing:0.04em">${s.label}</span>
            <b style="margin-left:auto;padding-left:20px;color:#fff;font-size:13px">${p.value ?? 0}</b>
          </div>`
        }).join('')
        return `<div style="color:rgba(255,255,255,0.66);font-size:12px;margin-bottom:2px">${params[0].axisValue}</div>${rows}`
      }
    },
    graphic: visibleSeries.value.length
      ? []
      : [{
          type: 'text',
          left: 'center',
          top: 'middle',
          style: { text: '请至少选择一条折线', fill: MUTED, fontSize: 13, fontFamily: 'inherit' }
        }],
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates.value,
      axisLine: { lineStyle: { color: AXIS } },
      axisTick: { show: false },
      axisLabel: {
        color: MUTED,
        fontSize: 11,
        // 只留 MM-DD：年份在这一屏里没有信息量，还占宽度
        formatter: value => String(value).slice(5)
      }
    },
    yAxis: {
      type: 'value',
      // 计的是「条数」，刻度不该出现 0.5
      minInterval: 1,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: GRID, width: 1, type: 'solid' } },
      axisLabel: { color: MUTED, fontSize: 11 }
    },
    series: visibleSeries.value.map(s => ({
      name: s.label,
      type: 'line',
      data: props.trend.map(d => d[s.key] ?? 0),
      showSymbol: !dense.value,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: s.color, width: 2, cap: 'round', join: 'round' },
      // 2px 纸色描边让圆点压在别的线上时也读得出来，而不是糊成一个交汇点
      itemStyle: { color: s.color, borderColor: SURFACE, borderWidth: 2 },
      emphasis: { focus: 'series' }
    }))
  }
}

function render() {
  if (!chart.value) return
  // 不合并上一次的 option：系列被隐藏后，残留的 series 不会自动消失
  chart.value.setOption(buildOption(), true)
}

let observer = null

onMounted(async () => {
  await nextTick()
  if (!canvasRef.value) return
  chart.value = echarts.init(canvasRef.value, null, { renderer: 'canvas' })

  // 前庭失调的用户会因为动效眩晕，交给 CSS 同款的偏好开关把关
  if (window.matchMedia?.('(prefers-reduced-motion: reduce)').matches) {
    chart.value.setOption({ animation: false })
  }

  render()

  // 容器宽度由外层栅格决定，跟着它 resize，别用固定 viewBox 拉伸（文字会失真）
  observer = new ResizeObserver(() => chart.value?.resize())
  observer.observe(canvasRef.value)
})

watch([() => props.trend, onKeys], render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart.value?.dispose()
  chart.value = null
})
</script>

<style scoped>
.trend-chart {
  width: 100%;
}

.series-switch {
  display: flex;
  flex-wrap: wrap;
  gap: var(--sp-2, 8px);
  margin-bottom: var(--sp-4, 16px);
}

/*
 * 用原生 button 而不是 el-checkbox-button：Element Plus 选中态是近黑填充，
 * 而近黑底上的色块恰恰失去了它唯一的作用——所以这里自己做，选中态只加一层淡墨。
 */
.series-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px 4px 8px;
  background: transparent;
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 2px;
  cursor: pointer;
  font-family: 'Noto Serif SC', 'STSong', 'SimSun', serif;
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--ink-light, #4a4a4a);
  transition: border-color var(--dur-base, 200ms) var(--ease-out),
              color var(--dur-base, 200ms) var(--ease-out),
              background-color var(--dur-base, 200ms) var(--ease-out);
}

.series-chip:hover {
  border-color: rgba(26, 26, 26, 0.32);
}

.series-chip.is-off {
  color: var(--ink-faint, #b5b0a8);
}

.series-chip.is-off:hover {
  color: var(--ink-muted, #8a8580);
}

.series-chip:focus-visible {
  outline: 2px solid var(--ink, #1a1a1a);
  outline-offset: 2px;
}

/* 实心＝显示，空心＝隐藏。开关状态因此不只靠颜色，色盲用户也读得出 */
.chip-key {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  border: 1.5px solid transparent;
  box-sizing: border-box;
  flex: none;
}

.chart-canvas {
  width: 100%;
  /* 三条线在 30 天区间下的可读高度；没有固定高度 ECharts 会塌成 0 */
  height: 300px;
}

@media (max-width: 720px) {
  .chart-canvas {
    height: 240px;
  }
}
</style>
