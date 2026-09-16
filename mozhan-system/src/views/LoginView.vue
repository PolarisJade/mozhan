<template>
  <div class="login-page">
    <!-- 背景是纸的质感：两道极淡的墨色晕染，不是一个纯色板 -->
    <div class="wash wash-a" aria-hidden="true"></div>
    <div class="wash wash-b" aria-hidden="true"></div>

    <div class="login-card">
      <div class="login-header">
        <span class="seal font-display" aria-hidden="true">墨</span>
        <h1 class="font-display">墨栈管理</h1>
        <p>落笔为墨，栈藏文章</p>
      </div>

      <el-form
        :model="form"
        :rules="rules"
        ref="formRef"
        label-position="top"
        class="login-form ink-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
            autocomplete="current-password"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item class="submit-item">
          <!-- 用 native-type=submit，让回车提交走表单而不是靠额外监听 keyup -->
          <el-button type="primary" native-type="submit" :loading="loading" class="submit-btn">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { login } from '@/api/admin'
import { setToken } from '@/utils/auth'
import router from '@/router'

const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  admin: true
})

const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function handleLogin() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
    loading.value = true
    const res = await login(form)
    setToken(res.token, res.expireTime)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    // validate() 校验失败时抛的是 false（不是 Error），那种情况不该弹提示——
    // 表单里已经有红字了，再弹一个"登录失败"是误导
    if (error !== false) {
      ElMessage.error(error?.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px;
  background: var(--paper, #f6f3ed);
  overflow: hidden;
}

/* 墨晕：两团低透明度的径向渐变，给这张"纸"一点深浅，而不是死平 */
.wash {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  pointer-events: none;
}

.wash-a {
  width: 520px;
  height: 520px;
  top: -160px;
  left: -120px;
  background: radial-gradient(circle, rgba(26, 26, 26, 0.09), transparent 70%);
}

.wash-b {
  width: 460px;
  height: 460px;
  bottom: -180px;
  right: -100px;
  background: radial-gradient(circle, rgba(139, 69, 19, 0.11), transparent 70%);
}

.login-card {
  position: relative;
  z-index: 1;
  width: 380px;
  padding: 40px 36px 32px;
  background: var(--paper-solid, #fffcf7);
  border: 1px solid rgba(26, 26, 26, 0.08);
  border-radius: 4px;
  box-shadow: 0 16px 48px rgba(26, 26, 26, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 28px;
}

.seal {
  display: inline-grid;
  place-items: center;
  width: 46px;
  height: 46px;
  background: var(--seal-red, #8b4513);
  color: #f6f3ed;
  border-radius: 4px;
  font-size: 28px;
  line-height: 1;
  margin-bottom: 14px;
  box-shadow: inset 0 0 0 1px rgba(246, 243, 237, 0.25);
}

.login-header h1 {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 400;
  color: var(--ink, #1a1a1a);
  /* 汉字标题拉开字距，和拉丁文相反 */
  letter-spacing: 0.2em;
  /* 字距会在最后一个字后面留白，居中时看起来偏左，往右推回来 */
  text-indent: 0.2em;
}

.login-header p {
  margin: 0;
  color: var(--ink-muted, #8a8580);
  font-size: 13px;
  letter-spacing: 0.12em;
}

.login-form :deep(.el-form-item__label) {
  padding-bottom: 2px;
}

.submit-item {
  margin-top: 28px;
  margin-bottom: 0;
}

.submit-btn {
  width: 100%;
  height: 42px;
  font-size: 15px;
}

@media (prefers-reduced-motion: no-preference) {
  .login-card {
    animation: card-in var(--dur-slow, 320ms) var(--ease-out) both;
  }

  @keyframes card-in {
    from {
      opacity: 0;
      transform: translateY(10px);
    }
    to {
      opacity: 1;
      transform: none;
    }
  }
}

@media (max-width: 420px) {
  .login-card {
    width: 100%;
    padding: 32px 22px 26px;
  }
}
</style>
