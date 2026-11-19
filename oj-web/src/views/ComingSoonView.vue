<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppIcon from '../components/AppIcon.vue'

const route = useRoute()
const isQuestions = computed(() => route.meta.module === 'questions')
const content = computed(() =>
  isQuestions.value
    ? {
        title: '题目管理',
        english: 'PROBLEM LIBRARY',
        icon: 'document',
        heading: '每一道好题，都值得认真打磨。',
        text: '未来在这里组织题目、维护测试用例，为编程练习提供更多可能。',
        items: ['题库检索与分类', '题目内容与样例维护', '测试用例与判题配置'],
      }
    : {
        title: '竞赛管理',
        english: 'CONTEST SPACE',
        icon: 'trophy',
        heading: '让思考，在挑战中发生。',
        text: '未来在这里组织编程竞赛，连接热爱代码的伙伴。',
        items: ['竞赛创建与时间安排', '竞赛题目编排', '报名与竞赛状态管理'],
      },
)
</script>

<template>
  <div class="page coming-soon-page">
    <div class="page-heading">
      <div>
        <span class="eyebrow">{{ content.english }}</span>
        <h1>{{ content.title }}</h1>
        <p>为下一步，留出更多可能。</p>
      </div>
      <span class="pill neutral">待开放</span>
    </div>

    <section class="panel coming-soon-panel">
      <div class="empty-illustration" aria-hidden="true">
        <span class="empty-orbit" />
        <span class="empty-icon"><AppIcon :name="content.icon" :size="40" /></span>
        <span class="empty-star">✦</span>
      </div>
      <span class="eyebrow">THE NEXT CHAPTER</span>
      <h2>{{ content.heading }}</h2>
      <p>{{ content.text }}</p>
      <div class="planned-features">
        <span v-for="item in content.items" :key="item">
          <AppIcon name="clock" :size="13" /> {{ item }}
        </span>
      </div>
      <p class="upcoming-caption">功能正在规划中，当前暂未开放操作。</p>
      <RouterLink to="/dashboard" class="button button-primary">
        返回工作台 <AppIcon name="arrow" :size="15" />
      </RouterLink>
    </section>
  </div>
</template>
