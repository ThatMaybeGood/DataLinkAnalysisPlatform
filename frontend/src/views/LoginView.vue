<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import Icon from '@/components/Icon.vue';
import { DISPLAY_NAME_KEY, login, ROLES_KEY, setToken } from '@/api';

const router = useRouter();
const username = ref('');
const password = ref('');
const loading = ref(false);
const error = ref('');

async function onSubmit() {
  if (loading.value) return;
  if (!username.value.trim() || !password.value) {
    error.value = '请输入用户名和密码';
    return;
  }
  loading.value = true;
  error.value = '';
  try {
    const data = await login(username.value.trim(), password.value);
    setToken(data.token);
    localStorage.setItem(DISPLAY_NAME_KEY, data.displayName);
    localStorage.setItem(ROLES_KEY, JSON.stringify(data.roles));
    router.push('/');
  } catch (e) {
    error.value = (e as Error).message || '登录失败，请稍后重试';
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="login-brand">
        <div class="brand-mark"><Icon name="flow" :size="22" /></div>
        <h1 class="login-title">DataLink 平台</h1>
        <p class="login-subtitle">数据关联与业务流程监控分析</p>
      </div>

      <form class="login-form" @submit.prevent="onSubmit">
        <div class="field">
          <label class="label" for="username">用户名</label>
          <input
            id="username"
            v-model="username"
            class="input"
            placeholder="请输入用户名"
            autocomplete="username"
            @keyup.enter="onSubmit"
          />
        </div>
        <div class="field">
          <label class="label" for="password">密码</label>
          <input
            id="password"
            v-model="password"
            type="password"
            class="input"
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter="onSubmit"
          />
        </div>

        <div v-if="error" class="alert alert--danger mb-md">
          <Icon name="alert" :size="15" />{{ error }}
        </div>

        <button type="submit" class="btn btn-primary btn-block" :disabled="loading">
          {{ loading ? '登录中…' : '登 录' }}
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex; align-items: center; justify-content: center;
  padding: 24px; background: var(--bg);
}
.login-card {
  width: 100%; max-width: 380px; padding: 36px 32px;
  box-shadow: var(--shadow-md);
}
.login-brand { text-align: center; margin-bottom: 24px; }
.brand-mark {
  width: 46px; height: 46px; margin: 0 auto 12px; border-radius: 12px;
  background: var(--accent); color: #fff;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.35);
}
.login-title { font-size: 20px; font-weight: 700; letter-spacing: -0.01em; }
.login-subtitle { font-size: 12px; color: var(--fg-faint); margin-top: 4px; }
.login-form .btn-block { height: 38px; font-size: 14px; }
/* 局部错误条（与 DataSourceView 同款，复用状态色变量） */
.alert { display: flex; align-items: center; gap: 8px; padding: 10px 14px; border-radius: var(--radius-sm); font-size: 13px; }
.alert--danger { color: var(--danger); background: var(--danger-soft); }
</style>
