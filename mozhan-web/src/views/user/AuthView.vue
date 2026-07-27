<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { InkMessage } from '@/utils/message'
import { register } from '@/api/user'
import { sendVerificationCode } from '@/api/email'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isLogin = computed(() => route.name === 'Login')
const switching = ref(false)

const loginLoading = ref(false)
const registerLoading = ref(false)
const loginFormRef = ref()
const registerFormRef = ref()

const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({
  username: '',
  password: '',
  nickname: '',
  email: '',
  verificationCode: '',
})

const codeButtonText = ref('获取验证码')
const codeButtonDisabled = ref(false)
let codeTimer = null

const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

const registerRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' },
  ],
  verificationCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码长度为6位', trigger: 'blur' },
  ],
}

async function onLogin() {
  await loginFormRef.value.validate()
  loginLoading.value = true
  try {
    await userStore.login({ ...loginForm })
    InkMessage.success('登录成功')
    const redirect = route.query.redirect || '/'
    router.replace(redirect)
  } finally {
    loginLoading.value = false
  }
}

async function sendCode() {
  if (!registerForm.email) {
    InkMessage.warning('请先输入邮箱')
    return
  }
  
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(registerForm.email)) {
    InkMessage.warning('请输入正确的邮箱格式')
    return
  }
  
  codeButtonDisabled.value = true
  startCodeTimer()
  
  try {
    await sendVerificationCode(registerForm.email)
    InkMessage.success('验证码已发送，请查收邮件')
  } catch (error) {
    InkMessage.error('发送失败，请稍后重试')
    if (codeTimer) {
      clearInterval(codeTimer)
      codeTimer = null
    }
    codeButtonDisabled.value = false
    codeButtonText.value = '获取验证码'
  }
}

function startCodeTimer() {
  let seconds = 60
  codeButtonText.value = `${seconds}秒后重试`
  
  codeTimer = setInterval(() => {
    seconds--
    if (seconds <= 0) {
      clearInterval(codeTimer)
      codeButtonText.value = '获取验证码'
      codeButtonDisabled.value = false
    } else {
      codeButtonText.value = `${seconds}秒后重试`
    }
  }, 1000)
}

async function onRegister() {
  await registerFormRef.value.validate()
  registerLoading.value = true
  try {
    const data = await register({ ...registerForm })
    userStore.setAuth(data)
    InkMessage.success('注册成功，欢迎入驻墨栈')
    router.replace('/')
  } finally {
    registerLoading.value = false
  }
}

function switchMode() {
  if (switching.value) return
  switching.value = true
  const target = isLogin.value ? 'Register' : 'Login'
  const query = isLogin.value ? {} : route.query
  setTimeout(() => {
    router.push({ name: target, query })
  }, 320)
}

watch(
  () => route.name,
  () => {
    switching.value = false
  },
)
</script>

<template>
  <div class="auth-page" :class="{ 'is-register': !isLogin, switching }">
    <div class="ink-bg">
      <div class="ink-blot ink-blot-1" />
      <div class="ink-blot ink-blot-2" />
      <div class="ink-blot ink-blot-3" />
      <div class="mountain" />
    </div>

    <div class="ink-sweep" aria-hidden="true" />

    <div class="auth-shell">
      <aside class="brand-panel">
        <div class="seal">{{ isLogin ? '墨' : '栈' }}</div>
        <h1 class="brand-title">墨栈</h1>
        <p class="brand-subtitle">
          {{ isLogin ? '挥毫落纸 · 栈叙文心' : '初入墨栈 · 落笔成章' }}
        </p>
        <div class="brush-line" />
        <p class="brand-desc">
          <template v-if="isLogin">
            以墨为笔，以栈为居<br />记录思想，沉淀时光
          </template>
          <template v-else>
            开卷有益，执笔随心<br />在此留下你的第一笔
          </template>
        </p>

        <button type="button" class="switch-btn" @click="switchMode">
          <span class="switch-hint">
            {{ isLogin ? '初来墨栈？' : '已是栈中客？' }}
          </span>
          <span class="switch-action">
            {{ isLogin ? '点此入驻 →' : '点此归栈 →' }}
          </span>
        </button>
      </aside>

      <div class="ink-bridge" aria-hidden="true">
        <span class="bridge-dot" />
        <span class="bridge-line" />
        <span class="bridge-dot" />
      </div>

      <section class="form-panel">
        <Transition name="form-ink" mode="out-in">
          <div v-if="isLogin" key="login" class="form-block">
            <div class="form-header">
              <h2>登录</h2>
              <p>欢迎回来，请登录你的账号</p>
            </div>
            <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              class="ink-form auth-form"
              @submit.prevent="onLogin"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model="loginForm.username"
                  placeholder="请输入用户名"
                  size="large"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="loginForm.password"
                  type="password"
                  show-password
                  placeholder="请输入密码"
                  size="large"
                  @keyup.enter="onLogin"
                />
              </el-form-item>
              <el-button
                class="submit-btn ink-btn"
                type="primary"
                size="large"
                :loading="loginLoading"
                @click="onLogin"
              >
                登 录
              </el-button>
            </el-form>
          </div>

          <div v-else key="register" class="form-block">
            <div class="form-header">
              <h2>注册</h2>
              <p>填写信息，开启你的墨栈之旅</p>
            </div>
            <el-form
              ref="registerFormRef"
              :model="registerForm"
              :rules="registerRules"
              layout="vertical"
              class="ink-form auth-form"
              @submit.prevent="onRegister"
            >
              <el-form-item label="用户名" prop="username">
                <el-input v-model="registerForm.username"
                  placeholder="4-20位字母数字下划线"
                  size="large"
                />
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input v-model="registerForm.password"
                  type="password"
                  show-password
                  placeholder="6-20位"
                  size="large"
                />
              </el-form-item>
              <el-form-item label="昵称">
                <el-input v-model="registerForm.nickname"
                  placeholder="选填"
                  size="large"
                />
              </el-form-item>
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="registerForm.email"
                  placeholder="请输入邮箱（用于接收验证码）"
                  size="large"
                />
              </el-form-item>
              <el-form-item label="验证码" prop="verificationCode">
                <el-input v-model="registerForm.verificationCode"
                  placeholder="请输入邮箱验证码"
                  size="large"
                >
                  <template #append>
                    <el-button
                      size="large"
                      :disabled="codeButtonDisabled"
                      @click="sendCode"
                    >
                      {{ codeButtonText }}
                    </el-button>
                  </template>
                </el-input>
              </el-form-item>
              <el-button
                class="submit-btn ink-btn"
                type="primary"
                size="large"
                :loading="registerLoading"
                @click="onRegister"
              >
                入 栈
              </el-button>
            </el-form>
          </div>
        </Transition>
      </section>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background: var(--paper);
}

.ink-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.ink-blot {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  opacity: 0.18;
  background: radial-gradient(circle, #1a1a1a 0%, transparent 70%);
  transition: transform 0.8s ease, opacity 0.8s ease;
}

.ink-blot-1 {
  width: 420px;
  height: 420px;
  top: -120px;
  right: -80px;
}

.ink-blot-2 {
  width: 320px;
  height: 320px;
  bottom: -60px;
  left: -40px;
  opacity: 0.12;
}

.ink-blot-3 {
  width: 180px;
  height: 180px;
  top: 40%;
  left: 18%;
  opacity: 0.08;
}

.is-register .ink-blot-1 {
  transform: translate(-60%, 20%);
}

.is-register .ink-blot-2 {
  transform: translate(40%, -10%);
}

.mountain {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 180px;
  background:
    linear-gradient(165deg, transparent 40%, rgba(26, 26, 26, 0.04) 40%),
    linear-gradient(195deg, transparent 55%, rgba(26, 26, 26, 0.06) 55%),
    linear-gradient(175deg, transparent 70%, rgba(26, 26, 26, 0.03) 70%);
}

.ink-sweep {
  position: absolute;
  inset: 0;
  z-index: 2;
  pointer-events: none;
  background: linear-gradient(
    105deg,
    transparent 40%,
    rgba(26, 26, 26, 0.12) 50%,
    transparent 60%
  );
  transform: translateX(-120%);
  opacity: 0;
}

.switching .ink-sweep {
  animation: ink-sweep 0.65s ease-in-out;
}

@keyframes ink-sweep {
  0% {
    transform: translateX(-120%);
    opacity: 0;
  }
  30% {
    opacity: 1;
  }
  100% {
    transform: translateX(120%);
    opacity: 0;
  }
}

.auth-shell {
  position: relative;
  z-index: 1;
  display: flex;
  width: 100%;
  max-width: 920px;
  min-height: 540px;
  border-radius: 4px;
  overflow: hidden;
  box-shadow:
    0 24px 64px rgba(26, 26, 26, 0.08),
    0 0 0 1px rgba(26, 26, 26, 0.06);
  background: rgba(255, 252, 247, 0.92);
  backdrop-filter: blur(8px);
  transition: transform 0.65s cubic-bezier(0.4, 0, 0.2, 1);
}

.is-register .auth-shell {
  flex-direction: row-reverse;
}

.switching .auth-shell {
  transform: scale(0.98);
}

.brand-panel {
  flex: 1;
  padding: 48px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(
    160deg,
    rgba(26, 26, 26, 0.92) 0%,
    rgba(45, 45, 45, 0.88) 100%
  );
  color: #f0ebe3;
  position: relative;
  transition: transform 0.65s cubic-bezier(0.4, 0, 0.2, 1);
}

.brand-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 80% 20%, rgba(255, 255, 255, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 20% 80%, rgba(0, 0, 0, 0.15) 0%, transparent 50%);
  pointer-events: none;
}

.seal {
  width: 56px;
  height: 56px;
  border: 2px solid rgba(240, 235, 227, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: 28px;
  margin-bottom: 24px;
  opacity: 0.85;
  transition: transform 0.5s ease, border-color 0.5s ease;
}

.is-register .seal {
  transform: rotate(-8deg) scale(1.05);
  border-color: rgba(139, 69, 19, 0.6);
}

.brand-title {
  margin: 0;
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: 52px;
  font-weight: 400;
  letter-spacing: 0.2em;
  text-indent: 0.2em;
}

.brand-subtitle {
  margin: 12px 0 0;
  font-size: 14px;
  letter-spacing: 0.35em;
  color: rgba(240, 235, 227, 0.65);
}

.brush-line {
  width: 48px;
  height: 3px;
  margin: 28px 0;
  background: linear-gradient(90deg, rgba(240, 235, 227, 0.7), transparent);
  border-radius: 2px;
  transition: width 0.5s ease;
}

.is-register .brush-line {
  width: 72px;
  background: linear-gradient(90deg, rgba(139, 69, 19, 0.6), transparent);
}

.brand-desc {
  margin: 0;
  font-size: 13px;
  line-height: 2;
  color: rgba(240, 235, 227, 0.5);
  letter-spacing: 0.08em;
}

.switch-btn {
  margin-top: 36px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  text-align: left;
  color: rgba(240, 235, 227, 0.75);
  position: relative;
  z-index: 1;
  transition: color 0.3s;
}

.switch-btn:hover {
  color: #f0ebe3;
}

.switch-hint {
  display: block;
  font-size: 12px;
  letter-spacing: 0.15em;
  opacity: 0.6;
  margin-bottom: 4px;
}

.switch-action {
  display: block;
  font-size: 15px;
  letter-spacing: 0.2em;
  border-bottom: 1px solid rgba(240, 235, 227, 0.3);
  padding-bottom: 2px;
  transition: border-color 0.3s, letter-spacing 0.3s;
}

.switch-btn:hover .switch-action {
  border-bottom-color: rgba(240, 235, 227, 0.8);
  letter-spacing: 0.25em;
}

.ink-bridge {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 3;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  pointer-events: none;
}

.bridge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(26, 26, 26, 0.2);
}

.bridge-line {
  width: 2px;
  height: 40px;
  background: linear-gradient(180deg, transparent, rgba(26, 26, 26, 0.15), transparent);
}

.form-panel {
  flex: 1;
  padding: 44px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
}

.form-header h2 {
  margin: 0;
  font-family: 'ZCOOL XiaoWei', 'Noto Serif SC', serif;
  font-size: 28px;
  font-weight: 400;
  color: var(--ink);
  letter-spacing: 0.15em;
}

.form-header p {
  margin: 8px 0 28px;
  font-size: 13px;
  color: var(--ink-muted);
  letter-spacing: 0.05em;
}

.auth-form :deep(.el-form-item__label::before) {
  color: var(--seal-red) !important;
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 46px;
  letter-spacing: 0.3em;
  text-indent: 0.3em;
  transition: transform 0.2s, box-shadow 0.3s;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(26, 26, 26, 0.18);
}

.form-ink-enter-active,
.form-ink-leave-active {
  transition: opacity 0.35s ease, transform 0.35s ease;
}

.form-ink-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.form-ink-leave-to {
  opacity: 0;
  transform: translateY(-12px);
}

@media (max-width: 720px) {
  .auth-shell,
  .is-register .auth-shell {
    flex-direction: column;
    min-height: auto;
  }

  .brand-panel {
    padding: 32px 28px;
    align-items: center;
    text-align: center;
  }

  .brand-title {
    font-size: 40px;
  }

  .brush-line {
    margin: 20px auto;
  }

  .switch-btn {
    text-align: center;
  }

  .form-panel {
    padding: 28px 28px 36px;
  }

  .ink-bridge {
    display: none;
  }
}
</style>
