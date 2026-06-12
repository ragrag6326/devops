<script setup>
import { ref } from 'vue'
import { loginApi } from '@/api/login'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()

const loginForm = ref({
  username: '',
  password: ''
})

const errors = ref({
  username: '',
  password: ''
})

const loading = ref(false)
const showPassword = ref(false)

const validate = () => {
  errors.value = { username: '', password: '' }
  let valid = true

  if (!loginForm.value.username.trim()) {
    errors.value.username = '請輸入用戶帳號'
    valid = false
  }
  if (!loginForm.value.password) {
    errors.value.password = '請輸入密碼'
    valid = false
  }
  return valid
}

const handleLogin = async () => {
  if (!validate()) return

  loading.value = true
  try {
    const result = await loginApi(loginForm.value)

    if (result.code === 1) {
      ElMessage.success('登入成功')
      localStorage.setItem('current_username', loginForm.value.username)
      localStorage.setItem('jwt_token', result.data.token)
      localStorage.setItem('current_id', result.data.id)
      localStorage.setItem('current_role', result.data.role)
      router.push('/homepage')
    } else {
      ElMessage.error(result.msg || '帳號或密碼錯誤')
    }
  } catch {
    ElMessage.error('網路或伺服器錯誤，請稍後再試')
  } finally {
    loading.value = false
  }
}

const handleClear = () => {
  loginForm.value = { username: '', password: '' }
  errors.value = { username: '', password: '' }
}

const onKeydown = (e) => {
  if (e.key === 'Enter') handleLogin()
}
</script>

<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <div class="login-card" @keydown="onKeydown">
      <div class="brand-area">
        <div class="brand-logo">
          <svg viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="9" fill="url(#lg)" />
            <path d="M8 22 L16 10 L24 22" stroke="#fff" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M11 18 L21 18" stroke="#fff" stroke-width="2" stroke-linecap="round" opacity="0.7" />
            <defs>
              <linearGradient id="lg" x1="0" y1="0" x2="32" y2="32">
                <stop offset="0%" stop-color="#4f46e5" />
                <stop offset="100%" stop-color="#e11d48" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <div>
          <h1 class="brand-title">版本控制管理系統</h1>
          <p class="brand-sub">DevOps Platform v2</p>
        </div>
      </div>

      <div class="form-body">
        <div class="field-group" :class="{ 'has-error': errors.username }">
          <label class="field-label">帳號</label>
          <div class="input-wrap">
            <span class="input-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                <circle cx="12" cy="7" r="4" />
              </svg>
            </span>
            <input
              v-model="loginForm.username"
              type="text"
              class="field-input"
              placeholder="請輸入用戶帳號"
              autocomplete="username"
              :disabled="loading"
            />
          </div>
          <span v-if="errors.username" class="field-error">{{ errors.username }}</span>
        </div>

        <div class="field-group" :class="{ 'has-error': errors.password }">
          <label class="field-label">密碼</label>
          <div class="input-wrap">
            <span class="input-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            </span>
            <input
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              class="field-input"
              placeholder="請輸入密碼"
              autocomplete="current-password"
              :disabled="loading"
            />
            <button
              class="eye-btn"
              type="button"
              tabindex="-1"
              @click="showPassword = !showPassword"
            >
              <svg v-if="showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                <line x1="1" y1="1" x2="23" y2="23" />
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </button>
          </div>
          <span v-if="errors.password" class="field-error">{{ errors.password }}</span>
        </div>

        <div class="btn-row">
          <button class="btn-login" :disabled="loading" @click="handleLogin">
            <span v-if="loading" class="btn-spinner"></span>
            <span>{{ loading ? '登入中...' : '登入' }}</span>
          </button>
          <button class="btn-clear" :disabled="loading" @click="handleClear">清除</button>
        </div>
      </div>

      <div class="card-footer">© 2026 版本控制管理系統 · All rights reserved</div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #0d0f1a;
  overflow: hidden;
  position: relative;
  font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  animation: drift 12s ease-in-out infinite alternate;
}

.orb-1 {
  width: 480px;
  height: 480px;
  top: -160px;
  left: -120px;
  background: radial-gradient(circle, rgba(79, 70, 229, 0.3), transparent 65%);
  animation-delay: 0s;
}

.orb-2 {
  width: 360px;
  height: 360px;
  bottom: -80px;
  right: -80px;
  background: radial-gradient(circle, rgba(225, 29, 72, 0.22), transparent 65%);
  animation-delay: -4s;
}

.orb-3 {
  width: 280px;
  height: 280px;
  top: 40%;
  right: 20%;
  background: radial-gradient(circle, rgba(129, 140, 248, 0.15), transparent 65%);
  animation-delay: -8s;
}

.login-card {
  position: relative;
  z-index: 10;
  width: 420px;
  max-width: calc(100vw - 40px);
  border-radius: 20px;
  border: 1px solid rgba(129, 140, 248, 0.15);
  background: rgba(19, 22, 42, 0.88);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 24px 64px #00000080, 0 0 0 1px #ffffff0a inset;
  overflow: hidden;
  animation: card-in 0.5s cubic-bezier(0.34, 1.56, 0.64, 1) both;
}

.brand-area {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 28px 28px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.brand-logo svg {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: block;
  filter: drop-shadow(0 4px 12px rgba(79, 70, 229, 0.4));
}

.brand-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #e8e9f3;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.brand-sub {
  margin: 3px 0 0;
  font-size: 12px;
  color: #6b7a99;
  letter-spacing: 0.04em;
}

.form-body {
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  font-weight: 600;
  color: #8b8fa8;
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 14px;
  color: #4a4e68;
  display: flex;
  pointer-events: none;
  transition: color 0.2s;
}

.input-icon svg {
  width: 17px;
  height: 17px;
}

.field-input {
  width: 100%;
  padding: 12px 44px 12px 44px;
  border-radius: 11px;
  border: 1px solid rgba(129, 140, 248, 0.15);
  background: rgba(255, 255, 255, 0.04);
  color: #e8e9f3;
  font-size: 15px;
  font-family: inherit;
  outline: none;
  transition: all 0.2s ease;
  -webkit-font-smoothing: antialiased;
}

.field-input::placeholder {
  color: #4a4e68;
}

.field-input:focus {
  border-color: #818cf8;
  background: rgba(129, 140, 248, 0.06);
  box-shadow: 0 0 0 3px #818cf81f;
}

.field-input:focus ~ .input-icon,
.input-wrap:focus-within .input-icon {
  color: #818cf8;
}

.field-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.has-error .field-input {
  border-color: #f8717180;
}

.has-error .field-input:focus {
  box-shadow: 0 0 0 3px #f871711f;
}

.field-error {
  font-size: 12px;
  color: #f87171;
  padding-left: 2px;
  animation: shake 0.3s ease;
}

.eye-btn {
  position: absolute;
  right: 12px;
  background: none;
  border: none;
  color: #4a4e68;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  transition: color 0.2s;
  border-radius: 5px;
}

.eye-btn svg {
  width: 18px;
  height: 18px;
}

.eye-btn:hover {
  color: #818cf8;
  background: rgba(129, 140, 248, 0.1);
}

.btn-row {
  display: flex;
  gap: 10px;
  margin-top: 4px;
}

.btn-login {
  flex: 1;
  padding: 13px 0;
  border-radius: 11px;
  border: none;
  background: linear-gradient(135deg, #4f46e5, #818cf8);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
  box-shadow: 0 4px 16px #4f46e559;
  letter-spacing: 0.02em;
}

.btn-login:hover:not(:disabled) {
  filter: brightness(1.1);
  transform: translateY(-1px);
  box-shadow: 0 6px 20px #4f46e573;
}

.btn-login:active:not(:disabled) {
  transform: translateY(0);
}

.btn-login:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
  flex-shrink: 0;
}

.btn-clear {
  padding: 13px 22px;
  border-radius: 11px;
  border: 1px solid rgba(129, 140, 248, 0.2);
  background: rgba(255, 255, 255, 0.04);
  color: #8b8fa8;
  font-size: 15px;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-clear:hover:not(:disabled) {
  border-color: #818cf866;
  color: #818cf8;
  background: rgba(129, 140, 248, 0.06);
}

.btn-clear:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.card-footer {
  padding: 14px 28px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  font-size: 12px;
  color: #4a4e68;
  text-align: center;
  letter-spacing: 0.01em;
}

@keyframes drift {
  0% { transform: translate(0) scale(1); }
  100% { transform: translate(30px, 20px) scale(1.05); }
}

@keyframes card-in {
  0% { opacity: 0; transform: translateY(24px) scale(0.97); }
  100% { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes shake {
  0%, 100% { transform: translate(0); }
  25% { transform: translate(-4px); }
  75% { transform: translate(4px); }
}

@keyframes spin {
  0% { transform: rotate(0); }
  100% { transform: rotate(360deg); }
}
</style>
