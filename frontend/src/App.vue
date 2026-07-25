<script setup>
/**
 * 应用根组件
 * 负责整体布局（侧边导航 + 内容区）、模块切换、总览数据加载与登录态校验。
 * 根据当前选中的导航项动态渲染对应业务模块（记账 / 计划 / 目标 / AI / 账号）。
 */
import { computed, onMounted, ref } from "vue";
import SidebarNav from "./components/SidebarNav.vue";
import SummarySection from "./components/SummarySection.vue";
import AccountingModule from "./components/modules/AccountingModule.vue";
import PlansModule from "./components/modules/PlansModule.vue";
import GoalsModule from "./components/modules/GoalsModule.vue";
import AiModule from "./components/modules/AiModule.vue";
import AccountModule from "./components/modules/AccountModule.vue";
import { dashboardApi } from "./services/dashboardApi";
import { clearAuth, getToken } from "./services/http";

const activeSection = ref("summary"); // 当前选中的导航区块标识
const refreshKey = ref(0); // 刷新计数，递增以触发子模块重新加载
const loading = ref(false); // 总览数据加载中标志
const errorText = ref(""); // 加载错误提示文本

// 总览汇总数据：本月收支、计划数与目标数等统计
const summary = ref({ monthIncome: 0, monthExpense: 0, totalPlans: 0, donePlans: 0, totalGoals: 0, achievedGoals: 0 });
const profile = ref({ username: "", nickname: "", email: "" }); // 当前登录用户信息
const pendingPlans = ref([]); // 待办计划列表
const pendingGoals = ref([]); // 待达成目标列表

// 根据当前导航标识解析出要渲染的业务模块组件及其展示模式
const currentModule = computed(() => {
  if (activeSection.value.startsWith("accounting")) {
    if (activeSection.value === "accountingMonthly") return { name: AccountingModule, mode: "monthly" };
    if (activeSection.value === "accountingYearly") return { name: AccountingModule, mode: "yearly" };
    return { name: AccountingModule, mode: "overview" };
  }
  if (activeSection.value.startsWith("plans")) {
    if (activeSection.value === "plansMonthly") return { name: PlansModule, mode: "monthly" };
    if (activeSection.value === "plansYearly") return { name: PlansModule, mode: "yearly" };
    return { name: PlansModule, mode: "overview" };
  }
  if (activeSection.value.startsWith("goals")) {
    if (activeSection.value === "goalsMonthly") return { name: GoalsModule, mode: "monthly" };
    if (activeSection.value === "goalsYearly") return { name: GoalsModule, mode: "yearly" };
    return { name: GoalsModule, mode: "overview" };
  }
  if (activeSection.value.startsWith("ai")) {
    if (activeSection.value === "aiFinance") return { name: AiModule, mode: "finance" };
    if (activeSection.value === "aiRemind") return { name: AiModule, mode: "remind" };
    return { name: AiModule, mode: "chat" };
  }
  if (activeSection.value === "account") {
    return { name: AccountModule, mode: "account" };
  }
  return null;
});

const currentModuleComponent = computed(() => (currentModule.value ? currentModule.value.name : null)); // 当前模块对应的组件
const currentModuleMode = computed(() => (currentModule.value ? currentModule.value.mode : "")); // 当前模块的展示模式

/**
 * 加载总览所需的汇总数据与用户信息
 * 并行请求统计、用户资料、计划与目标，过滤并排序出待办项；
 * 遇到 401 未授权时清除登录态并跳转登录页。
 */
async function loadSummaryAndProfile() {
  loading.value = true;
  errorText.value = "";
  try {
    // 并行拉取四类数据，减少等待时间
    const [summaryData, profileData, plansData, goalsData] = await Promise.all([
      dashboardApi.getSummary(),
      dashboardApi.getProfile(),
      dashboardApi.getPlans(),
      dashboardApi.getGoals()
    ]);
    summary.value = { ...summary.value, ...(summaryData || {}) };
    profile.value = { ...profile.value, ...(profileData || {}) };
    // 过滤掉已完成/已结束的计划，并按截止日期升序排列
    pendingPlans.value = (plansData || [])
      .filter((i) => i.status !== "DONE" && i.status !== "ENDED")
      .sort((a, b) => String(a.endDate || "9999-12-31").localeCompare(String(b.endDate || "9999-12-31")));
    // 过滤掉已完成/已达成的目标，并按截止日期升序排列
    pendingGoals.value = (goalsData || [])
      .filter((i) => i.status !== "DONE" && i.status !== "ACHIEVED")
      .sort((a, b) => String(a.deadline || "9999-12-31").localeCompare(String(b.deadline || "9999-12-31")));
  } catch (err) {
    // 登录态失效则清除凭据并返回登录页
    if (err.status === 401) {
      clearAuth();
      window.location.href = "/";
      return;
    }
    errorText.value = err.message || "加载失败";
  } finally {
    loading.value = false;
  }
}

// 递增刷新计数并重新加载总览数据，同时触发子模块刷新
async function refreshAll() {
  refreshKey.value += 1;
  await loadSummaryAndProfile();
}

// 切换当前激活的导航区块
function activate(section) {
  activeSection.value = section;
}

// 合并更新本地用户信息（子模块修改资料后回传）
function updateProfile(nextProfile) {
  profile.value = { ...profile.value, ...(nextProfile || {}) };
}

// 退出登录：清除凭据并跳转登录页
function logout() {
  clearAuth();
  window.location.href = "/";
}

// 挂载时校验登录态，未登录跳转登录页，否则加载数据
onMounted(async () => {
  if (!getToken()) {
    window.location.href = "/";
    return;
  }
  await refreshAll();
});
</script>

<template>
  <!-- 整体外壳：左侧导航 + 右侧内容区 -->
  <div class="app-shell">
    <SidebarNav :active-section="activeSection" @activate="activate" @logout="logout" />

    <!-- 内容区：固定型业务模块（记账/计划/目标/AI）使用专用布局类 -->
    <main
      class="content"
      :class="{ 'content-fixed-module': activeSection.startsWith('accounting') || activeSection.startsWith('plans') || activeSection.startsWith('goals') || activeSection.startsWith('ai') }"
    >
      <!-- 总览页顶部栏：标题、加载状态与刷新/退出操作 -->
      <header v-if="activeSection === 'summary'" class="topbar">
        <div>
          <h2>{{ activeSection === "summary" ? "总览看板" : "控制台" }}</h2>
          <p>{{ loading ? "正在加载数据..." : "数据已同步" }}<span v-if="errorText"> · {{ errorText }}</span></p>
        </div>
        <div class="topbar-actions">
          <button :disabled="loading" @click="refreshAll">{{ loading ? "刷新中..." : "刷新数据" }}</button>
          <button class="topbar-logout" @click="logout">退出登录</button>
        </div>
      </header>

      <!-- 总览看板：展示汇总统计与待办计划/目标 -->
      <SummarySection
        v-if="activeSection === 'summary'"
        :summary="summary"
        :profile="profile"
        :pending-plans="pendingPlans"
        :pending-goals="pendingGoals"
        @jump="activate"
      />

      <!-- 动态业务模块：根据当前导航渲染对应组件与模式 -->
      <component
        :is="currentModuleComponent"
        v-if="currentModule"
        :mode="currentModuleMode"
        :profile="profile"
        :refresh-key="refreshKey"
        @profile-updated="updateProfile"
      />
    </main>
  </div>
</template>
