<script setup lang="ts">
import BrandMark from '../components/BrandMark.vue'
import AppIcon from '../components/AppIcon.vue'
import { useAuthStore } from '../stores/auth'
const auth = useAuthStore()
</script>

<template>
  <main class="not-found">
    <BrandMark />
    <div class="not-found-code">404<span>✦</span></div>
    <h1>这个页面走丢了</h1>
    <p>页面可能已移动，或你输入了一个不存在的地址。</p>
    <RouterLink
      :to="auth.isLoggedIn ? '/dashboard' : '/login'"
      class="button button-primary"
    >
      {{ auth.isLoggedIn ? '返回工作台' : '返回登录' }}
      <AppIcon name="arrow" :size="15" />
    </RouterLink>
    <span class="text-muted" style="font-size:12px;letter-spacing:.5px;">THERE IS ALWAYS ANOTHER SOLUTION.</span>
  </main>
</template>
