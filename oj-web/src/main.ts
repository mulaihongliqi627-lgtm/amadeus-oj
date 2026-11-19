import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './router'
import { onUnauthorized } from './api/client'
import { useAuthStore } from './stores/auth'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-button.css'
import 'element-plus/theme-chalk/el-input.css'
import 'element-plus/theme-chalk/el-form.css'
import 'element-plus/theme-chalk/el-form-item.css'
import 'element-plus/theme-chalk/el-icon.css'
import './styles/main.scss'

const app = createApp(App)
app.use(createPinia())
app.use(router)

onUnauthorized(() => {
  const current = router.currentRoute.value
  useAuthStore().clear()
  if (current.name !== 'login') {
    void router.replace({ name: 'login', query: { expired: '1', redirect: current.path } })
  }
})

app.mount('#app')
