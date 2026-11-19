<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { login } from '../api/admin'
import { useAuthStore } from '../stores/auth'
import { getLoginDestination } from '../router'
import BrandMark from '../components/BrandMark.vue'
import AppIcon from '../components/AppIcon.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const form = reactive({ userAccount: '', password: '' })
const rules: FormRules = {
  userAccount: [
    {
      required: true,
      validator: (_rule, value: string, callback) =>
        callback(value?.trim() ? undefined : new Error('请输入管理员账号')),
      trigger: 'blur',
    },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}
const busy = ref(false)
const error = ref('')
const expired = computed(() => route.query.expired === '1')

async function submit() {
  if (busy.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || busy.value) return
  busy.value = true
  error.value = ''
  try {
    const token = await login({ userAccount: form.userAccount.trim(), password: form.password })
    auth.signIn(token, form.userAccount.trim())
    form.password = ''
    await auth.fetchAndPersistUserInfo()
    await router.replace(getLoginDestination(route.query.redirect))
  } catch (cause) {
    auth.clear()
    error.value = cause instanceof Error ? cause.message : '登录失败，请重试。'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-card-header">
        <BrandMark />
        <div style="margin-top: 28px">
          <h2>欢迎回来</h2>
          <p>登录管理工作台，开始今天的工作。</p>
        </div>
      </div>

      <div v-if="expired" class="notice warning" role="status">
        <AppIcon name="warning" /> 登录状态已失效，请重新登录。
      </div>
      <div v-if="error" class="notice error" role="alert">
        <AppIcon name="warning" /> {{ error }}
      </div>

      <ElForm
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        size="large"
        @submit.prevent="submit"
      >
        <ElFormItem label="管理员账号" prop="userAccount">
          <ElInput
            v-model="form.userAccount"
            name="username"
            autocomplete="username"
            placeholder="请输入管理员账号"
            :disabled="busy"
          >
            <template #prefix><AppIcon name="user" :size="17" /></template>
          </ElInput>
        </ElFormItem>

        <ElFormItem label="密码" prop="password">
          <ElInput
            v-model="form.password"
            name="password"
            type="password"
            autocomplete="current-password"
            show-password
            placeholder="请输入登录密码"
            :disabled="busy"
          >
            <template #prefix><AppIcon name="lock" :size="17" /></template>
          </ElInput>
        </ElFormItem>

        <div class="login-session-note">
          <AppIcon name="lock" :size="13" /> 登录状态仅保留在当前浏览器标签页
        </div>

        <ElButton class="login-submit" type="primary" native-type="submit" :loading="busy">
          {{ busy ? '正在登录…' : '登 录' }}
          <AppIcon v-if="!busy" name="arrow" :size="17" />
        </ElButton>
      </ElForm>

      <div class="login-help">
        仅限授权管理员访问<br />
        <span>如需账号，请联系平台管理员</span>
      </div>
    </div>
  </div>
</template>
