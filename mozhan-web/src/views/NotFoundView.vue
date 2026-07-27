<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="not-found-card ink-card">
      <p class="code font-display">404</p>
      <p class="hint">此页如墨迹消散，未能寻得</p>
      <el-button class="ink-btn" @click="router.push('/')">返回首页</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.not-found-card {
  padding: 48px 56px;
  text-align: center;
}

.code {
  margin: 0;
  font-size: 72px;
  color: var(--ink);
  letter-spacing: 0.1em;
  opacity: 0.15;
}

.hint {
  margin: 0 0 28px;
  font-size: 15px;
  color: var(--ink-muted);
  letter-spacing: 0.12em;
}
</style>
