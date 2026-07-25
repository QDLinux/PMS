<script setup>
/**
 * 仪表盘汇总区。
 * 展示本月收支统计、计划与目标的完成进度环及未完成清单；
 * 点击进度卡片通过 jump 事件通知父级跳转到对应模块。
 */
import { computed } from "vue";
import ProfileBadge from "./ProfileBadge.vue";
import RingProgress from "./RingProgress.vue";

const props = defineProps({
  summary: { type: Object, required: true },      // 汇总统计数据
  profile: { type: Object, required: true },      // 当前用户信息
  pendingPlans: { type: Array, required: true },  // 待完成计划列表
  pendingGoals: { type: Array, required: true }   // 待完成目标列表
});

const emit = defineEmits(["jump"]);

// 金额格式化为两位小数的人民币显示
const money = (value) => `¥${Number(value || 0).toFixed(2)}`;

// 未完成计划/目标数量（总数减去已完成，且不为负）
const pendingPlanCount = computed(() => Math.max(0, Number(props.summary.totalPlans || 0) - Number(props.summary.donePlans || 0)));
const pendingGoalCount = computed(() => Math.max(0, Number(props.summary.totalGoals || 0) - Number(props.summary.achievedGoals || 0)));

// 计划完成百分比（无计划时为 0）
const planPercent = computed(() => {
  const total = Number(props.summary.totalPlans || 0);
  if (total <= 0) return 0;
  return Math.round((Number(props.summary.donePlans || 0) / total) * 100);
});

// 目标达成百分比（无目标时为 0）
const goalPercent = computed(() => {
  const total = Number(props.summary.totalGoals || 0);
  if (total <= 0) return 0;
  return Math.round((Number(props.summary.achievedGoals || 0) / total) * 100);
});

// 通知父级跳转到指定模块
function jumpTo(section) {
  emit("jump", section);
}

// 卡片键盘可访问性：回车或空格触发跳转
function handleCardKeydown(event, section) {
  if (event.key === "Enter" || event.key === " ") {
    event.preventDefault();
    jumpTo(section);
  }
}
</script>

<template>
  <section>
    <!-- 顶部欢迎区：问候语与用户徽标 -->
    <article class="summary-overview">
      <div class="summary-overview-top">
        <div>
          <h2>你好，{{ profile.nickname || profile.username || "用户" }}</h2>
          <p>统一查看你的收支、计划与目标进度</p>
        </div>
        <ProfileBadge :profile="profile" />
      </div>
      <div class="summary-overview-bottom">
        <div>
          <h3>{{ profile.nickname || profile.username || "欢迎回来" }}</h3>
          <p>{{ profile.email || "当前账号" }}</p>
        </div>
      </div>
    </article>

    <!-- 统计卡片区：收入、支出及可点击跳转的计划/目标进度卡 -->
    <div class="cards four">
      <article class="stat-card stat-card-income">
        <h3>本月收入</h3>
        <p class="num amount-income">{{ money(summary.monthIncome) }}</p>
      </article>
      <article class="stat-card stat-card-expense">
        <h3>本月支出</h3>
        <p class="num amount-expense">{{ money(summary.monthExpense) }}</p>
      </article>
      <!-- 计划完成进度卡：点击/回车跳转到规划模块 -->
      <article class="jump-card ring-card" role="button" tabindex="0" @click="jumpTo('plans')" @keydown="handleCardKeydown($event, 'plans')">
        <h3>计划完成</h3>
        <div class="ring-card-body">
          <RingProgress :percent="planPercent" :color="planPercent >= 80 ? '#0b8b5f' : '#0f766e'" />
          <div class="ring-card-stats">
            <p class="num">{{ summary.donePlans }}<span class="num-sep"> / </span><span class="num-total">{{ summary.totalPlans }}</span></p>
            <p class="mini-note">未完成 {{ pendingPlanCount }} 项</p>
          </div>
        </div>
        <ul class="mini-list">
          <li v-for="item in pendingPlans.slice(0, 3)" :key="item.id">{{ item.title }}（{{ item.endDate || "-" }}）</li>
          <li v-if="!pendingPlans.length">暂无待完成计划</li>
        </ul>
      </article>
      <!-- 目标达成进度卡：点击/回车跳转到目标模块 -->
      <article class="jump-card ring-card" role="button" tabindex="0" @click="jumpTo('goals')" @keydown="handleCardKeydown($event, 'goals')">
        <h3>目标达成</h3>
        <div class="ring-card-body">
          <RingProgress :percent="goalPercent" :color="goalPercent >= 80 ? '#0b8b5f' : '#f59e0b'" />
          <div class="ring-card-stats">
            <p class="num">{{ summary.achievedGoals }}<span class="num-sep"> / </span><span class="num-total">{{ summary.totalGoals }}</span></p>
            <p class="mini-note">未完成 {{ pendingGoalCount }} 项</p>
          </div>
        </div>
        <ul class="mini-list">
          <li v-for="item in pendingGoals.slice(0, 3)" :key="item.id">{{ item.name }}（{{ item.deadline || "-" }}）</li>
          <li v-if="!pendingGoals.length">暂无待完成目标</li>
        </ul>
      </article>
    </div>
  </section>
</template>
