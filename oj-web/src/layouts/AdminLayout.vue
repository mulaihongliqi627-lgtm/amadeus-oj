<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import BrandMark from '../components/BrandMark.vue'
import AppIcon from '../components/AppIcon.vue'

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)
const loggingOut = ref(false)
const logoutError = ref('')
const isMobile = ref(window.matchMedia('(max-width: 760px)').matches)
const mobileQuery = window.matchMedia('(max-width: 760px)')
const menuButton = ref<HTMLButtonElement>()
const closeButton = ref<HTMLButtonElement>()

const syncViewport = () => {
  isMobile.value = mobileQuery.matches
  if (!isMobile.value) menuOpen.value = false
}
onMounted(() => mobileQuery.addEventListener('change', syncViewport))
onUnmounted(() => mobileQuery.removeEventListener('change', syncViewport))

async function openMenu() {
  menuOpen.value = true
  await nextTick()
  closeButton.value?.focus()
}
function closeMenu() {
  menuOpen.value = false
  menuButton.value?.focus()
}

const links = [
  { path: '/dashboard', title: '工作台', icon: 'grid' },
  { path: '/admins', title: '管理员管理', icon: 'users' },
  { path: '/questions', title: '题目管理', icon: 'document', upcoming: true },
  { path: '/contests', title: '竞赛管理', icon: 'trophy', upcoming: true },
]

watch(
  () => route.path,
  () => {
    menuOpen.value = false
  },
)

async function logout() {
  if (loggingOut.value) return
  loggingOut.value = true
  logoutError.value = ''
  try {
    await auth.signOut()
    if (!auth.isLoggedIn) await router.replace('/login')
  } catch (cause) {
    if (auth.isLoggedIn) {
      logoutError.value = cause instanceof Error ? cause.message : '退出失败，请重试。'
    }
  } finally {
    loggingOut.value = false
  }
}
</script>

<template>
  <div class="admin-shell" @keydown.esc="closeMenu">
    <button
      v-if="menuOpen"
      class="sidebar-backdrop"
      aria-label="关闭导航菜单"
      tabindex="-1"
      @click="closeMenu"
    />

    <aside
      id="main-navigation"
      class="sidebar"
      :class="{ 'is-open': menuOpen }"
      :inert="isMobile && !menuOpen"
      aria-label="主导航"
    >
      <button
        ref="closeButton"
        class="icon-button mobile-sidebar-close"
        aria-label="关闭导航菜单"
        @click="closeMenu"
      >
        <AppIcon name="close" />
      </button>

      <RouterLink to="/dashboard" class="sidebar-brand" aria-label="Amadeus OJ 工作台">
        <BrandMark />
      </RouterLink>

      <div class="workspace-label">
        <span class="status-dot" />
        管理工作空间
        <span class="workspace-version">V0.1</span>
      </div>

      <span class="nav-label">导航</span>
      <nav>
        <RouterLink
          v-for="link in links"
          :key="link.path"
          :to="link.path"
          class="nav-link"
          :class="{ active: route.path === link.path }"
          :aria-current="route.path === link.path ? 'page' : undefined"
        >
          <AppIcon :name="link.icon" :size="18" />
          <span>{{ link.title }}</span>
          <span v-if="link.upcoming" class="nav-soon">待开放</span>
          <span v-else-if="route.path === link.path" class="nav-active-dot" />
        </RouterLink>
      </nav>

      <div class="sidebar-bottom">
        <div v-if="logoutError" class="notice error" role="alert">{{ logoutError }}</div>
        <div class="sidebar-account">
          <span class="avatar">{{ auth.displayName.slice(0, 1).toUpperCase() }}</span>
          <div>
            <strong>{{ auth.displayName }}</strong>
            <small>管理员</small>
          </div>
          <button
            class="icon-button sidebar-logout"
            aria-label="退出登录"
            title="退出登录"
            :disabled="loggingOut"
            :aria-busy="loggingOut"
            @click="logout"
          >
            <AppIcon name="logout" :size="17" />
          </button>
        </div>
      </div>
    </aside>

    <div class="admin-body" :inert="isMobile && menuOpen">
      <a class="skip-link" href="#main-content">跳转到内容</a>

      <header class="topbar">
        <div class="topbar-left">
          <button
            ref="menuButton"
            class="icon-button mobile-menu"
            aria-label="展开导航菜单"
            aria-controls="main-navigation"
            :aria-expanded="menuOpen"
            @click="openMenu"
          >
            <AppIcon name="menu" />
          </button>
          <span class="breadcrumb-root">管理控制台</span>
          <span class="breadcrumb-separator">/</span>
          <span class="breadcrumb-current">{{ route.meta.title }}</span>
        </div>
        <div class="topbar-right">
          <span class="workspace-chip"> <span class="status-dot" /> 在线 </span>
          <span class="topbar-divider" />
          <span class="topbar-avatar">{{ auth.displayName.slice(0, 1).toUpperCase() }}</span>
          <span class="topbar-name">{{ auth.displayName }}</span>
        </div>
      </header>

      <main id="main-content" class="main-content">
        <RouterView />
      </main>

      <footer class="app-footer">
        <span>© {{ new Date().getFullYear() }} Amadeus OJ</span>
        <span>THINK. CODE. GROW.</span>
      </footer>
    </div>
  </div>
</template>
