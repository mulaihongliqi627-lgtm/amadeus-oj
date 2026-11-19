<script setup lang="ts">
import { computed } from 'vue'
import {
  ArrowRight,
  Check,
  CircleCheck,
  Clock,
  Close,
  Document,
  Grid,
  Lock,
  Menu,
  Plus,
  Setting,
  SwitchButton,
  Trophy,
  User,
  UserFilled,
  Warning,
} from '@element-plus/icons-vue'

const props = defineProps<{ name: string; size?: number }>()
const icons = {
  arrow: ArrowRight,
  check: Check,
  success: CircleCheck,
  clock: Clock,
  close: Close,
  document: Document,
  grid: Grid,
  lock: Lock,
  menu: Menu,
  plus: Plus,
  settings: Setting,
  logout: SwitchButton,
  trophy: Trophy,
  user: User,
  users: UserFilled,
  warning: Warning,
}
const icon = computed(() => icons[props.name as keyof typeof icons] || Document)
</script>

<template>
  <component
    :is="icon"
    class="app-icon"
    :style="{ width: `${size || 20}px`, height: `${size || 20}px` }"
    aria-hidden="true"
  />
</template>
