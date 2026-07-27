<script setup>
import { computed } from 'vue'
import {
  Sun,
  Cloud,
  CloudSun,
  CloudDrizzle,
  CloudRain,
  CloudRainWind,
  CloudSnow,
  CloudLightning,
  CloudFog,
  Wind,
} from 'lucide-vue-next'

const props = defineProps({
  type: {
    type: String,
    default: '',
  },
})

// 业务天气文案 -> lucide-vue-next 图标组件
// 全部为 24x24 viewBox / stroke 2 / round-linecap 风格，保持统一
const ICONS = {
  '晴': Sun,
  '多云': CloudSun,
  '阴': Cloud,
  '小雨': CloudDrizzle,
  '大雨': CloudRainWind,
  '雷阵雨': CloudLightning,
  '雪': CloudSnow,
  '雾': CloudFog,
  '风': Wind,
  '雷': CloudLightning,
  '雨': CloudRain,
}

const IconComp = computed(() => ICONS[props.type] || Cloud)
</script>

<template>
  <span class="weather-icon" aria-hidden="true">
    <component :is="IconComp" />
  </span>
</template>

<style scoped>
.weather-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 1em;
  height: 1em;
  font-size: 16px;
  line-height: 1;
  color: currentColor;
  vertical-align: -0.125em;
}
.weather-icon :deep(svg) {
  width: 100%;
  height: 100%;
}
</style>
