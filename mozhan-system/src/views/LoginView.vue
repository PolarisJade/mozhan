<template>
  <div class="login-page">
    <div class="login-container">
      <div class="login-header">
        <h1>墨栈管理系统</h1>
        <p>欢迎登录后台管理</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px" class="login-form">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading">登 录</el-button>
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
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
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
    if (error !== false) {
      ElMessage.error(error.message || '登录失败')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-container {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
  width: 400px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-header h1 {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #333;
}

.login-header p {
  margin: 0;
  color: #999;
  font-size: 14px;
}

.login-form {
  margin-top: 16px;
}

.login-form .el-button {
  width: 100%;
}
</style>
