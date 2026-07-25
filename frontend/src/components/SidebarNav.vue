<script setup>
/**
 * 侧边导航栏。
 * 展示总览、记账/规划/目标/AI 四个可展开分组及账号管理、退出入口；
 * 通过 activate 事件通知父级切换页面，通过 logout 事件触发登出。
 */
import { reactive, watch } from "vue";

const props = defineProps({
  activeSection: { type: String, required: true } // 当前激活的页面标识
});

const emit = defineEmits(["activate", "logout"]);

// 各分组包含的子页面标识，用于判断分组是否处于激活态
const groups = {
  accounting: ["accounting", "accountingMonthly", "accountingYearly"],
  plans: ["plans", "plansMonthly", "plansYearly"],
  goals: ["goals", "goalsMonthly", "goalsYearly"],
  ai: ["aiChat", "aiFinance", "aiRemind"]
};

// 各分组的展开/收起状态
const openGroups = reactive({ accounting: false, plans: false, goals: false, ai: false });

const isGroupActive = (key) => groups[key].includes(props.activeSection); // 分组下是否有激活的子项
const isActive = (key) => props.activeSection === key;                    // 指定页面是否激活

// 根据当前激活页面自动展开对应分组
function syncOpenByActive() {
  if (isGroupActive("accounting")) openGroups.accounting = true;
  if (isGroupActive("plans")) openGroups.plans = true;
  if (isGroupActive("goals")) openGroups.goals = true;
  if (isGroupActive("ai")) openGroups.ai = true;
}

// 切换分组展开状态
function toggleGroup(key) {
  openGroups[key] = !openGroups[key];
}

// 激活页面变化时同步展开对应分组（初始化即执行一次）
watch(() => props.activeSection, syncOpenByActive, { immediate: true });
</script>

<template>
  <aside class="sidebar">
    <h1>PM</h1>
    <p class="side-sub">Personal Manager</p>
    <nav>
      <a href="#" :class="{ active: isActive('summary') }" @click.prevent="emit('activate', 'summary')">总览看板</a>

      <!-- 记账管理分组：父链接切换展开，子项弹层切换具体页面 -->
      <a href="#" class="side-parent-link" :class="{ active: isGroupActive('accounting'), expanded: openGroups.accounting }" @click.prevent="toggleGroup('accounting')"><span class="side-link-label">记账管理</span><span class="side-toggle-hit" aria-hidden="true"></span></a>
      <div class="side-popup" :class="{ 'is-hidden': !openGroups.accounting }">
        <a href="#" class="side-popup-item" :class="{ active: isActive('accounting') }" @click.prevent="emit('activate', 'accounting')">记账总览</a>
        <a href="#" class="side-popup-item" :class="{ active: isActive('accountingMonthly') }" @click.prevent="emit('activate', 'accountingMonthly')">月度账单</a>
        <a href="#" class="side-popup-item" :class="{ active: isActive('accountingYearly') }" @click.prevent="emit('activate', 'accountingYearly')">年度账单</a>
      </div>

      <!-- 规划管理分组 -->
      <a href="#" class="side-parent-link" :class="{ active: isGroupActive('plans'), expanded: openGroups.plans }" @click.prevent="toggleGroup('plans')"><span class="side-link-label">规划管理</span><span class="side-toggle-hit" aria-hidden="true"></span></a>
      <div class="side-popup" :class="{ 'is-hidden': !openGroups.plans }">
        <a href="#" class="side-popup-item" :class="{ active: isActive('plans') }" @click.prevent="emit('activate', 'plans')">规划总览</a>
        <a href="#" class="side-popup-item" :class="{ active: isActive('plansMonthly') }" @click.prevent="emit('activate', 'plansMonthly')">月度规划</a>
        <a href="#" class="side-popup-item" :class="{ active: isActive('plansYearly') }" @click.prevent="emit('activate', 'plansYearly')">年度规划</a>
      </div>

      <!-- 目标管理分组 -->
      <a href="#" class="side-parent-link" :class="{ active: isGroupActive('goals'), expanded: openGroups.goals }" @click.prevent="toggleGroup('goals')"><span class="side-link-label">目标管理</span><span class="side-toggle-hit" aria-hidden="true"></span></a>
      <div class="side-popup" :class="{ 'is-hidden': !openGroups.goals }">
        <a href="#" class="side-popup-item" :class="{ active: isActive('goals') }" @click.prevent="emit('activate', 'goals')">目标总览</a>
        <a href="#" class="side-popup-item" :class="{ active: isActive('goalsMonthly') }" @click.prevent="emit('activate', 'goalsMonthly')">月度目标</a>
        <a href="#" class="side-popup-item" :class="{ active: isActive('goalsYearly') }" @click.prevent="emit('activate', 'goalsYearly')">年度目标</a>
      </div>

      <!-- AI 助手分组 -->
      <a href="#" class="side-parent-link" :class="{ active: isGroupActive('ai'), expanded: openGroups.ai }" @click.prevent="toggleGroup('ai')"><span class="side-link-label">AI 助手</span><span class="side-toggle-hit" aria-hidden="true"></span></a>
      <div class="side-popup" :class="{ 'is-hidden': !openGroups.ai }">
        <a href="#" class="side-popup-item" :class="{ active: isActive('aiChat') }" @click.prevent="emit('activate', 'aiChat')">智能对话</a>
        <a href="#" class="side-popup-item" :class="{ active: isActive('aiFinance') }" @click.prevent="emit('activate', 'aiFinance')">理财建议</a>
        <a href="#" class="side-popup-item" :class="{ active: isActive('aiRemind') }" @click.prevent="emit('activate', 'aiRemind')">智能提醒</a>
      </div>

      <!-- 账号管理与退出登录 -->
      <a href="#" :class="{ active: isActive('account') }" @click.prevent="emit('activate', 'account')">账号管理</a>
      <a href="#" @click.prevent="emit('logout')">退出登录</a>
    </nav>
  </aside>
</template>
