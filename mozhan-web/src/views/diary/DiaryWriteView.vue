<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createDiary, getDiaryDetail, updateDiary } from '@/api/diary'
import { useUserStore } from '@/stores/user'
import { redirectToLogin } from '@/utils/auth'
import { InkMessage } from '@/utils/message'
import WeatherIcon from '@/components/WeatherIcon.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const submitting = ref(false)
const isEditMode = computed(() => !!route.params.id)
const diaryId = computed(() => route.params.id)
const weatherOptions = ['晴', '多云', '阴', '小雨', '大雨', '雷阵雨', '雪']

function todayStr() {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const form = reactive({
  diaryDate: todayStr(),
  weather: '晴',
  content: '',
})

const canSubmit = computed(() => {
  return form.diaryDate && form.weather && form.content.trim().length > 0
})

function disableFuture(date) {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date.getTime() > today.getTime()
}

async function loadDiary() {
  loading.value = true
  try {
    const data = await getDiaryDetail(diaryId.value)
    form.diaryDate = data.diaryDate || todayStr()
    form.weather = data.weather || '晴'
    form.content = data.content || ''
  } catch (e) {
    InkMessage.error('加载日记失败')
    router.push({ name: 'DiaryHome' })
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    const payload = {
      diaryDate: form.diaryDate,
      weather: form.weather,
      content: form.content.trim(),
    }
    if (isEditMode.value) {
      payload.id = Number(diaryId.value)
      await updateDiary(payload)
    } else {
      await createDiary(payload)
    }
    InkMessage.success(isEditMode.value ? '修改成功' : '已记下今日')
    router.push({ name: 'DiaryHome' })
  } catch (e) {
    InkMessage.error(isEditMode.value ? '修改失败' : '保存失败')
  } finally {
    submitting.value = false
  }
}

function handleCancel() {
  router.push({ name: 'DiaryHome' })
}

onMounted(() => {
  if (!userStore.isLoggedIn) {
    redirectToLogin('请先登录后再写日记')
    return
  }
  if (isEditMode.value) {
    loadDiary()
  }
})
</script>

<template>
  <div class="diary-editor">
    <div class="editor-content" v-loading="loading">
      <h2 class="editor-page-title">{{ isEditMode ? '修改日记' : '写日记' }}</h2>
      <div class="meta-row">
        <div class="meta-item date-item">
          <label class="meta-label">日期</label>
          <el-date-picker
            v-model="form.diaryDate"
            type="date"
            value-format="YYYY-MM-DD"
            :disabled-date="disableFuture"
            placeholder="选择日期"
            class="date-picker"
          />
        </div>
        <div class="meta-item weather-item">
          <label class="meta-label">天气</label>
          <div class="weather-options">
            <button
              v-for="w in weatherOptions"
              :key="w"
              type="button"
              class="weather-option"
              :class="{ active: form.weather === w }"
              @click="form.weather = w"
            >
              <WeatherIcon :type="w" />
              <span>{{ w }}</span>
            </button>
          </div>
        </div>
      </div>

      <div class="content-section">
        <label class="meta-label">正文</label>
        <textarea
          v-model="form.content"
          class="content-input"
          placeholder="今天发生了什么，心情如何……"
          rows="12"
        ></textarea>
      </div>

      <div class="actions">
        <button class="cancel-btn" @click="handleCancel" :disabled="submitting">取消</button>
        <button class="submit-btn" @click="handleSubmit" :disabled="submitting || !canSubmit">
          {{ submitting ? '保存中...' : (isEditMode ? '保存修改' : '保存日记') }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.diary-editor {
  max-width: 980px;
  margin: 0 auto 0;
  padding: 0 20px;
}

.editor-content {
  background: var(--paper-card);
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 4px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(26, 26, 26, 0.05);
}

.editor-page-title {
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: 20px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.1em;
  margin: 0 0 18px;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(26, 26, 26, 0.08);
}

.meta-row {
  display: flex;
  gap: 40px;
  margin-bottom: 24px;
  align-items: flex-start;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.date-item {
  flex-shrink: 0;
}

.weather-item {
  flex: 1;
  min-width: 0;
}

.meta-label {
  font-size: 13px;
  color: var(--ink-light);
  letter-spacing: 0.1em;
  font-weight: 500;
}

.date-picker {
  width: 200px;
}

.date-picker :deep(.el-input__wrapper) {
  border-radius: 2px;
  background: rgba(255, 252, 247, 0.6);
}

.weather-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.weather-option {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  background: rgba(255, 252, 247, 0.6);
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 2px;
  color: var(--ink-light);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.03em;
}

.weather-option :deep(.weather-icon) {
  width: 16px;
  height: 16px;
  font-size: 16px;
}

.weather-option:hover {
  border-color: rgba(26, 26, 26, 0.3);
  color: var(--ink);
}

.weather-option.active {
  background: var(--ink);
  border-color: var(--ink);
  color: #f0ebe3;
}

.content-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 28px;
}

.content-input {
  width: 100%;
  height: calc(100vh - 420px);
  min-height: 160px;
  max-height: 380px;
  padding: 14px 16px;
  font-size: 15px;
  line-height: 1.9;
  color: var(--ink);
  border: 1px solid rgba(26, 26, 26, 0.12);
  border-radius: 2px;
  background: rgba(255, 252, 247, 0.5);
  box-sizing: border-box;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.02em;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s ease;
}

.content-input::placeholder {
  color: var(--ink-muted);
  font-style: italic;
}

.content-input:focus {
  border-color: var(--ink);
  background: rgba(255, 252, 247, 0.8);
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
  border-top: 1px solid rgba(26, 26, 26, 0.06);
}

.cancel-btn {
  padding: 9px 24px;
  font-size: 14px;
  border: 1px solid rgba(26, 26, 26, 0.18);
  border-radius: 2px;
  background: transparent;
  color: var(--ink-light);
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.08em;
}

.cancel-btn:hover:not(:disabled) {
  border-color: var(--ink);
  color: var(--ink);
}

.cancel-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.submit-btn {
  padding: 9px 28px;
  font-size: 14px;
  border: none;
  border-radius: 2px;
  background: var(--ink);
  color: #f0ebe3;
  cursor: pointer;
  transition: all 0.2s ease;
  font-family: 'Noto Serif SC', serif;
  letter-spacing: 0.08em;
}

.submit-btn:hover:not(:disabled) {
  background: var(--accent);
}

.submit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 响应式 */
@media (max-width: 640px) {
  .meta-row {
    flex-direction: column;
    gap: 18px;
  }
  .date-picker {
    width: 100%;
  }
  .editor-content {
    padding: 20px 16px;
  }
}
</style>
