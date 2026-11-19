<script setup lang="ts">
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import AppIcon from '../components/AppIcon.vue'

const auth = useAuthStore()
const date = computed(() =>
  new Intl.DateTimeFormat('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' }).format(
    new Date(),
  ),
)
const modules = [
  {
    number: '01',
    icon: 'users',
    title: '管理员管理',
    description: '添加协作伙伴，共同维护平台。',
    path: '/admins',
    ready: true,
    action: '管理团队',
  },
  {
    number: '02',
    icon: 'document',
    title: '题目管理',
    description: '组织题目与测试用例，打磨每一道好题。',
    path: '/questions',
    ready: false,
    action: '即将开放',
  },
  {
    number: '03',
    icon: 'trophy',
    title: '竞赛管理',
    description: '组织编程竞赛，见证思考与代码的力量。',
    path: '/contests',
    ready: false,
    action: '即将开放',
  },
]
</script>

<template>
  <div class="page dashboard-page">
    <div class="page-heading">
      <div>
        <span class="eyebrow">OVERVIEW</span>
        <h1>工作台</h1>
        <p>保持专注，让每一次管理都井然有序。</p>
      </div>
      <span class="date-label">
        <AppIcon name="clock" :size="14" />{{ date }}
      </span>
    </div>

    <!-- Welcome Banner -->
    <section class="welcome-banner">
      <div class="welcome-text">
        <span class="welcome-kicker">YOUR WORKSPACE</span>
        <h2>你好，{{ auth.displayName }}</h2>
        <p>欢迎回到 Amadeus OJ 管理控制台。<br />从这里开始，构建更好的编程测评体验。</p>
        <RouterLink to="/admins" class="button button-white">
          添加管理员 <AppIcon name="arrow" :size="16" />
        </RouterLink>
      </div>
      <div class="welcome-art" aria-hidden="true">
        <div class="art-code-block">
          <div>public class Solution {</div>
          <div>&nbsp;&nbsp;// 每一次尝试，都在向前</div>
          <div>&nbsp;&nbsp;return "Hello, OJ.";</div>
          <div>}</div>
        </div>
      </div>
    </section>

    <!-- Module Grid -->
    <div class="section-heading">
      <div>
        <h2>管理模块</h2>
        <p>从日常维护，到下一场挑战。</p>
      </div>
      <span class="eyebrow" style="font-size:10px;">WORKSPACE MODULES</span>
    </div>

    <div class="module-grid">
      <RouterLink
        v-for="module in modules"
        :key="module.path"
        :to="module.path"
        class="module-card"
      >
        <div class="module-card-top">
          <span class="module-icon" :class="{ muted: !module.ready }">
            <AppIcon :name="module.icon" :size="22" />
          </span>
          <span class="pill" :class="module.ready ? 'green' : 'neutral'">
            {{ module.ready ? '已开放' : '待开放' }}
          </span>
        </div>
        <h3>{{ module.title }}</h3>
        <p>{{ module.description }}</p>
        <div class="module-card-bottom">
          <span>{{ module.action }} <AppIcon name="arrow" :size="14" /></span>
          <small>{{ module.number }}</small>
        </div>
      </RouterLink>
    </div>

    <!-- Bottom Panels -->
    <div class="dashboard-bottom">
      <section class="panel getting-started">
        <div class="panel-heading">
          <h2>快速开始</h2>
          <span class="eyebrow" style="font-size:10px;">QUICK START</span>
        </div>
        <RouterLink to="/admins" class="start-row">
          <span class="step-number">01</span>
          <div>
            <h3>邀请一位管理伙伴</h3>
            <p>创建管理员账号，共同管理平台。</p>
          </div>
          <AppIcon name="arrow" :size="17" />
        </RouterLink>
        <RouterLink to="/questions" class="start-row">
          <span class="step-number">02</span>
          <div>
            <h3>准备你的第一道题目</h3>
            <p>题目管理开放后，在这里维护题库。</p>
          </div>
          <span class="pill neutral">待开放</span>
        </RouterLink>
      </section>

      <section class="panel workspace-info">
        <div class="panel-heading">
          <h2>工作空间</h2>
          <AppIcon name="settings" :size="16" style="color: var(--text-muted);" />
        </div>
        <dl>
          <div>
            <dt>平台名称</dt>
            <dd>Amadeus OJ</dd>
          </div>
          <div>
            <dt>当前账号</dt>
            <dd>{{ auth.session?.account }}</dd>
          </div>
          <div>
            <dt>登录会话</dt>
            <dd><span class="small-dot"></span> 当前标签页</dd>
          </div>
          <div>
            <dt>平台版本</dt>
            <dd class="mono">v0.1.0</dd>
          </div>
        </dl>
        <p class="info-footnote">业务统计将在相关功能开放后展示。</p>
      </section>
    </div>
  </div>
</template>
