<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  ElButton,
  ElForm,
  ElFormItem,
  ElInput,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { createAdmin } from '../api/admin'
import AppIcon from '../components/AppIcon.vue'

const formRef = ref<FormInstance>()
const form = reactive({ userAccount: '', nickName: '', password: '', confirmation: '' })
const rules: FormRules = {
  userAccount: [
    {
      required: true,
      validator: (_rule, value: string, callback) =>
        callback(value?.trim() ? undefined : new Error('请输入管理员账号')),
      trigger: 'blur',
    },
  ],
  nickName: [
    {
      required: true,
      validator: (_rule, value: string, callback) =>
        callback(value?.trim() ? undefined : new Error('请输入管理员昵称')),
      trigger: 'blur',
    },
  ],
  password: [
    {
      required: true,
      validator: (_rule, value: string, callback) =>
        callback(value?.trim() ? undefined : new Error('请输入密码')),
      trigger: 'blur',
    },
  ],
  confirmation: [
    {
      required: true,
      validator: (_rule, value: string, callback) =>
        callback(value && value === form.password ? undefined : new Error('两次输入的密码需一致')),
      trigger: 'blur',
    },
  ],
}
const busy = ref(false)
const error = ref('')
const created = ref<{ account: string; nickname: string } | null>(null)

function resetForm() {
  formRef.value?.resetFields()
  error.value = ''
  created.value = null
}

async function submit() {
  if (busy.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || busy.value) return
  busy.value = true
  error.value = ''
  created.value = null
  try {
    const userAccount = form.userAccount.trim()
    const nickName = form.nickName.trim()
    await createAdmin({ userAccount, nickName, password: form.password })
    created.value = { account: userAccount, nickname: nickName }
    formRef.value?.resetFields()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '创建失败，请重试。'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="page admins-page">
    <div class="page-heading">
      <div>
        <span class="eyebrow">TEAM &amp; ACCESS</span>
        <h1>管理员管理</h1>
        <p>与值得信任的伙伴，共同维护平台。</p>
      </div>
      <span class="pill green"><span class="small-dot" /> 管理员空间</span>
    </div>

    <div class="admin-form-grid">
      <!-- Create Form -->
      <section class="panel create-admin-panel">
        <div class="panel-heading form-panel-heading">
          <div class="heading-with-icon">
            <span class="module-icon">
              <AppIcon name="users" :size="20" />
            </span>
            <div>
              <h2>新增管理员</h2>
              <p>为团队添加新的协作伙伴。</p>
            </div>
          </div>
          <span class="eyebrow" style="font-size:10px;">NEW MEMBER</span>
        </div>

        <div v-if="created" class="notice success creation-success" role="status">
          <AppIcon name="success" />
          <div>
            <strong>管理员创建成功</strong>
            <p>{{ created.nickname }}（{{ created.account }}）现在可以使用新账号登录。</p>
          </div>
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
          class="admin-form"
          @submit.prevent="submit"
        >
          <div class="form-section-label"><span>01</span> 基本信息</div>
          <div class="form-two-columns">
            <ElFormItem label="管理员账号" prop="userAccount">
              <ElInput
                v-model="form.userAccount"
                name="admin-account"
                autocomplete="off"
                placeholder="输入登录账号"
                :disabled="busy"
              />
            </ElFormItem>
            <ElFormItem label="管理员昵称" prop="nickName">
              <ElInput
                v-model="form.nickName"
                name="admin-nickname"
                autocomplete="off"
                placeholder="输入显示昵称"
                :disabled="busy"
              />
            </ElFormItem>
          </div>

          <div class="form-section-label"><span>02</span> 登录凭证</div>
          <div class="form-two-columns">
            <ElFormItem label="登录密码" prop="password">
              <ElInput
                v-model="form.password"
                name="new-password"
                type="password"
                autocomplete="new-password"
                show-password
                placeholder="设置初始密码"
                :disabled="busy"
              />
            </ElFormItem>
            <ElFormItem label="确认密码" prop="confirmation">
              <ElInput
                v-model="form.confirmation"
                name="confirm-password"
                type="password"
                autocomplete="new-password"
                show-password
                placeholder="再次输入密码"
                :disabled="busy"
              />
            </ElFormItem>
          </div>

          <p class="password-tip">
            <AppIcon name="lock" :size="13" /> 建议使用字母、数字和符号组合的独立密码。
          </p>

          <div class="form-actions">
            <span>标 * 字段为必填项</span>
            <div>
              <ElButton :disabled="busy" @click="resetForm">重置</ElButton>
              <ElButton type="primary" native-type="submit" :loading="busy">
                <AppIcon v-if="!busy" name="plus" :size="15" />
                {{ busy ? '正在创建…' : '创建管理员' }}
              </ElButton>
            </div>
          </div>
        </ElForm>
      </section>

      <!-- Side Notes -->
      <aside class="admin-side-notes">
        <section class="panel">
          <span class="note-icon"><AppIcon name="lock" :size="20" /></span>
          <h2>让协作有序发生</h2>
          <p>管理员账号用于访问平台管理功能，请仅为需要参与维护的成员创建。</p>
          <ul class="note-list">
            <li><AppIcon name="check" :size="14" /> 账号须唯一，不可重复创建</li>
            <li><AppIcon name="check" :size="14" /> 昵称用于界面中的身份展示</li>
            <li><AppIcon name="check" :size="14" /> 密码请通过安全方式交付本人</li>
          </ul>
        </section>

        <section class="availability-note">
          <span class="eyebrow">COMING NEXT</span>
          <h3>更完整的团队管理</h3>
          <p>管理员列表、详情和删除功能将在后续版本开放。</p>
        </section>
      </aside>
    </div>
  </div>
</template>
