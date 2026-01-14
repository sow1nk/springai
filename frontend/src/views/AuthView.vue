<template>
  <div class="auth-container">
    <div class="auth-card">
      <div class="auth-header">
        <img :src="logo" alt="Logo" class="logo" />
        <h1>Spring AI Chat</h1>
        <p class="subtitle">智能对话助手</p>
      </div>

      <div class="auth-form">
        <a-tabs v-model:activeKey="activeTab" centered>
          <a-tab-pane key="login" tab="登录">
            <a-form
              :model="loginForm"
              @finish="handleLogin"
              layout="vertical"
              class="form-content"
            >
              <a-form-item
                label="用户名"
                name="username"
                :rules="[{ required: true, message: '请输入用户名' }]"
              >
                <a-input
                  v-model:value="loginForm.username"
                  placeholder="输入用户名"
                  size="large"
                  :disabled="loading"
                />
              </a-form-item>

              <a-form-item
                label="密码"
                name="password"
                :rules="[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码至少6个字符' }
                ]"
              >
                <a-input-password
                  v-model:value="loginForm.password"
                  placeholder="输入密码"
                  size="large"
                  :disabled="loading"
                />
              </a-form-item>

              <a-button
                type="primary"
                html-type="submit"
                size="large"
                block
                :loading="loading"
                class="submit-btn"
              >
                登录
              </a-button>
            </a-form>
          </a-tab-pane>

          <a-tab-pane key="register" tab="注册">
            <a-form
              :model="registerForm"
              @finish="handleRegister"
              layout="vertical"
              class="form-content"
            >
              <a-form-item
                label="用户名"
                name="username"
                :rules="[
                  { required: true, message: '请输入用户名' },
                  { min: 2, max: 20, message: '用户名长度为2-20个字符' }
                ]"
              >
                <a-input
                  v-model:value="registerForm.username"
                  placeholder="输入用户名"
                  size="large"
                  :disabled="loading"
                />
              </a-form-item>

              <a-form-item
                label="密码"
                name="password"
                :rules="[
                  { required: true, message: '请输入密码' },
                  { min: 6, message: '密码至少6个字符' }
                ]"
              >
                <a-input-password
                  v-model:value="registerForm.password"
                  placeholder="输入密码"
                  size="large"
                  :disabled="loading"
                />
              </a-form-item>

              <a-form-item
                label="确认密码"
                name="confirmPassword"
                :rules="[
                  { required: true, message: '请再次输入密码' },
                  { validator: validateConfirmPassword }
                ]"
              >
                <a-input-password
                  v-model:value="registerForm.confirmPassword"
                  placeholder="再次输入密码"
                  size="large"
                  :disabled="loading"
                />
              </a-form-item>

              <a-form-item
                label="邮箱（可选）"
                name="email"
                :rules="[
                  { type: 'email', message: '请输入有效的邮箱地址' }
                ]"
              >
                <a-input
                  v-model:value="registerForm.email"
                  placeholder="输入邮箱"
                  size="large"
                  :disabled="loading"
                />
              </a-form-item>

              <a-button
                type="primary"
                html-type="submit"
                size="large"
                block
                :loading="loading"
                class="submit-btn"
              >
                注册
              </a-button>
            </a-form>
          </a-tab-pane>
        </a-tabs>
      </div>

      <div class="theme-toggle">
        <a-button
          type="text"
          @click="toggleTheme"
          class="theme-btn"
          aria-label="切换主题"
        >
          <template #icon>
            <component :is="themeIcon" />
          </template>
        </a-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { BulbOutlined, BulbFilled } from '@ant-design/icons-vue'
import { login, register } from '../api/auth'

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)
const logo = ref('/images/panda.svg')

const currentTheme = ref(localStorage.getItem('springai-theme') || 'light')

const loginForm = ref({
  username: '',
  password: ''
})

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
  email: ''
})

// 确认密码验证器
const validateConfirmPassword = (rule, value) => {
  if (!value) {
    return Promise.resolve()
  }
  if (value !== registerForm.value.password) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

const themeIcon = computed(() => {
  return currentTheme.value === 'dark' ? BulbOutlined : BulbFilled
})

const toggleTheme = () => {
  const newTheme = currentTheme.value === 'light' ? 'dark' : 'light'
  currentTheme.value = newTheme
  document.documentElement.setAttribute('data-theme', newTheme)
  localStorage.setItem('springai-theme', newTheme)
}

const handleLogin = async () => {
  loading.value = true
  try {
    const response = await login({
      username: loginForm.value.username,
      password: loginForm.value.password
    })

    localStorage.setItem('token', response.token)
    localStorage.setItem('userId', response.userId)
    localStorage.setItem('username', response.username || loginForm.value.username)

    message.success('登录成功')
    router.push('/')
  } catch (error) {
    console.error('登录失败:', error)
    message.error(error.message || '登录失败，请重试')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  loading.value = true
  try {
    const response = await register({
      username: registerForm.value.username,
      password: registerForm.value.password,
      email: registerForm.value.email
    })

    localStorage.setItem('token', response.token)
    localStorage.setItem('userId', response.userId)
    localStorage.setItem('username', response.username || registerForm.value.username)

    message.success('注册成功')
    router.push('/')
  } catch (error) {
    console.error('注册失败:', error)
    message.error(error.message || '注册失败，请重试')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary);
  padding: 20px;
  transition: background var(--transition-theme);
}

.auth-card {
  width: 100%;
  max-width: 420px;
  background: var(--bg-primary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--border-color);
  box-shadow: var(--card-shadow);
  padding: 40px 32px;
  position: relative;
  transition: all var(--transition-theme);
}

.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  width: 64px;
  height: 64px;
  margin-bottom: 16px;
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-8px);
  }
}

.auth-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0 0 8px 0;
  transition: color var(--transition-theme);
}

.subtitle {
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
  transition: color var(--transition-theme);
}

.auth-form {
  margin-bottom: 24px;
}

.form-content {
  margin-top: 24px;
}

.submit-btn {
  margin-top: 8px;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  border-radius: var(--radius-md);
  background: var(--accent-color);
  border-color: var(--accent-color);
  transition: all var(--transition-fast);
}

.submit-btn:hover:not(:disabled) {
  background: var(--accent-hover);
  border-color: var(--accent-hover);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.theme-toggle {
  position: absolute;
  top: 16px;
  right: 16px;
}

.theme-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-secondary);
  transition: all var(--transition-fast);
}

.theme-btn:hover {
  color: var(--accent-color);
  background: var(--accent-light);
  transform: rotate(15deg);
}

/* Ant Design 组件样式覆盖 */
:deep(.ant-tabs) {
  color: var(--text-primary);
}

:deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

:deep(.ant-tabs-tab) {
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
  padding: 12px 0;
  transition: color var(--transition-fast);
}

:deep(.ant-tabs-tab:hover) {
  color: var(--text-primary);
}

:deep(.ant-tabs-tab-active .ant-tabs-tab-btn) {
  color: var(--accent-color);
  font-weight: 600;
}

:deep(.ant-tabs-ink-bar) {
  background: var(--accent-color);
  height: 3px;
  border-radius: 3px 3px 0 0;
}

:deep(.ant-form-item-label > label) {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 500;
  transition: color var(--transition-theme);
}

:deep(.ant-input) {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  color: var(--text-primary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
  font-size: 14px;
}

:deep(.ant-input:hover) {
  border-color: var(--accent-color);
  background: var(--bg-primary);
}

:deep(.ant-input:focus) {
  border-color: var(--accent-color);
  box-shadow: 0 0 0 2px var(--accent-light);
  background: var(--bg-primary);
}

:deep(.ant-input::placeholder) {
  color: var(--text-tertiary);
}

/* 密码输入框样式 */
:deep(.ant-input-password) {
  background: var(--bg-secondary);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

:deep(.ant-input-password:hover) {
  border-color: var(--accent-color);
  background: var(--bg-primary);
}

:deep(.ant-input-password:focus-within) {
  border-color: var(--accent-color);
  box-shadow: 0 0 0 2px var(--accent-light);
  background: var(--bg-primary);
}

:deep(.ant-input-password .ant-input) {
  background: transparent;
  border: none;
  box-shadow: none;
}

:deep(.ant-input-password .ant-input:focus) {
  box-shadow: none;
}

:deep(.ant-input-password-icon) {
  color: var(--text-tertiary);
  transition: color var(--transition-fast);
}

:deep(.ant-input-password-icon:hover) {
  color: var(--accent-color);
}

:deep(.ant-form-item-explain-error) {
  font-size: 13px;
  margin-top: 4px;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .auth-card {
    padding: 32px 24px;
  }

  .auth-header h1 {
    font-size: 22px;
  }

  .logo {
    width: 56px;
    height: 56px;
  }
}
</style>
