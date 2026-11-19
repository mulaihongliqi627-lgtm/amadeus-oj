<script setup lang="ts">
defineProps<{ compact?: boolean }>()
</script>

<template>
  <div class="brand">
    <span class="brand-symbol" aria-hidden="true">&lt;/&gt;</span>
    <span v-if="!compact" class="brand-wordmark">
      Amadeus OJ
      <small>编程测评管理平台</small>
    </span>
  </div>
</template>
