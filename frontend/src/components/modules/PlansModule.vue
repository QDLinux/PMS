<script setup>
/**
 * 计划管理模块组件
 * 负责计划项的增删改查、优先级与状态管理，并支持按截止日期、状态、优先级、关键词筛选。
 * 通过 mode 切换三种视图：overview（工作台，含表单与卡片列表）、monthly（月度明细）、yearly（年度明细）。
 */
import { computed, onMounted, reactive, ref, watch } from "vue";
import { dashboardApi } from "../../services/dashboardApi";
import { useConfirmDialog } from "../../composables/useConfirmDialog";
import { usePeriodFilter } from "../../composables/usePeriodFilter";
import ConfirmDialog from "../ConfirmDialog.vue";
import ProfileBadge from "../ProfileBadge.vue";

// mode：视图模式；refreshKey：父组件触发刷新的信号；profile：当前用户档案
const props = defineProps({ mode: { type: String, default: "overview" }, refreshKey: { type: Number, default: 0 }, profile: { type: Object, default: () => ({}) } });
const plans = ref([]); // 计划列表原始数据
const loading = ref(false); // 列表加载中状态
const submitting = ref(false); // 新增表单提交中状态
const editOpen = ref(false); // 编辑弹窗是否打开
const editId = ref(null); // 当前正在编辑的计划 id
const form = reactive({ title: "", description: "", startDate: "", endDate: "", priority: "MEDIUM", status: "TODO" }); // 新增表单
const editForm = reactive({ title: "", description: "", startDate: "", endDate: "", priority: "MEDIUM", status: "TODO" }); // 编辑表单
const filter = reactive({ status: "", priority: "", deadline: "", keyword: "", month: "", year: "" }); // 筛选条件
const { dialog, openDialog, closeDialog, showMessage } = useConfirmDialog(); // 确认/提示弹窗控制

// 标题随视图模式动态切换
const title = computed(() => (props.mode === "monthly" ? "月度规划" : props.mode === "yearly" ? "年度规划" : "规划管理"));
// 状态码转中文文案
const statusText = (s) => ({ TODO: "待开始", IN_PROGRESS: "进行中", DONE: "已完成", ENDED: "已结束" }[s] || s || "-");
// 优先级码转中文文案
const priorityText = (p) => ({ HIGH: "高", MEDIUM: "中", LOW: "低" }[p] || p || "-");
// 基于计划的 endDate 生成月份/年份下拉选项，并与 filter 同步
const { monthOptions, yearOptions, syncPeriodOptions } = usePeriodFilter(plans, "endDate", filter);

// 按当前视图模式与筛选条件过滤、排序后的计划列表
const list = computed(() => {
  let data = [...plans.value];
  if (props.mode === "monthly") data = data.filter((i) => String(i.endDate || "").slice(0, 7) === filter.month); // 月度：匹配 YYYY-MM
  if (props.mode === "yearly") data = data.filter((i) => String(i.endDate || "").slice(0, 4) === filter.year); // 年度：匹配 YYYY
  if (props.mode === "overview" && filter.deadline) data = data.filter((i) => String(i.endDate || "") <= filter.deadline); // 概览：截止日期不晚于所选
  if (filter.status) data = data.filter((i) => i.status === filter.status);
  if (filter.priority) data = data.filter((i) => i.priority === filter.priority);
  if (filter.keyword.trim()) data = data.filter((i) => `${i.title || ""} ${i.description || ""}`.toLowerCase().includes(filter.keyword.trim().toLowerCase())); // 关键词匹配标题或描述
  return data.sort((a, b) => String(a.endDate || "9999-12-31").localeCompare(String(b.endDate || "9999-12-31"))); // 按截止日期升序
});

// 未完成（非已完成且非已结束）计划数量
const pendingCount = computed(() => list.value.filter((i) => i.status !== "DONE" && i.status !== "ENDED").length);
// 已完成计划数量
const doneCount = computed(() => list.value.filter((i) => i.status === "DONE").length);

// 根据状态与优先级决定计划卡片的视觉色调
function planTone(item) {
  if (item.status === "DONE") return "done";
  if (item.status === "ENDED") return "ended";
  if (item.priority === "HIGH") return "high";
  return "normal";
}

// 重置新增表单为默认值
function resetForm() {
  Object.assign(form, { title: "", description: "", startDate: "", endDate: "", priority: "MEDIUM", status: "TODO" });
}

// 打开编辑弹窗并以选中计划数据回填编辑表单
function openEdit(item) {
  editId.value = item.id;
  Object.assign(editForm, {
    title: item.title || "",
    description: item.description || "",
    startDate: item.startDate || "",
    endDate: item.endDate || "",
    priority: item.priority || "MEDIUM",
    status: item.status || "TODO"
  });
  editOpen.value = true;
}

// 关闭编辑弹窗并清空编辑目标
function closeEdit() {
  editOpen.value = false;
  editId.value = null;
}

/** 加载计划列表，并同步周期筛选选项 */
async function load() {
  loading.value = true;
  try {
    plans.value = await dashboardApi.getPlans(); // 拉取全部计划
    syncPeriodOptions(); // 刷新月份/年份选项
  } finally {
    loading.value = false;
  }
}

/** 提交新增计划 */
async function submit() {
  submitting.value = true;
  try {
    await dashboardApi.createPlan({ ...form }); // 调用创建接口
    resetForm(); // 重置表单
    await showMessage("创建成功", "规划已创建。");
    await load(); // 重新加载列表
  } finally {
    submitting.value = false;
  }
}

/** 保存编辑修改，需用户二次确认 */
async function saveEdit() {
  if (!(await openDialog({ title: "确认修改", message: "确认保存对这条规划的修改？" }))) return; // 取消则中止
  await dashboardApi.updatePlan(editId.value, { ...editForm }); // 调用更新接口
  closeEdit();
  await showMessage("修改成功", "规划已更新。");
  await load();
}

/** 删除指定计划，需用户二次确认（不可恢复） */
async function remove(id) {
  if (!(await openDialog({ title: "确认删除", message: "确认删除这条规划？删除后不可恢复。", confirmText: "删除", danger: true }))) return; // 取消则中止
  await dashboardApi.deletePlan(id); // 调用删除接口
  await showMessage("删除成功", "规划已删除。");
  await load();
}

// 重置筛选条件，月份/年份回退到首个可选项
function resetFilter() {
  Object.assign(filter, { status: "", priority: "", deadline: "", keyword: "", month: monthOptions.value[0] || "", year: yearOptions.value[0] || "" });
}

watch(() => props.refreshKey, load); // 父组件刷新信号变化时重新加载
onMounted(load); // 组件挂载后首次加载
</script>

<template>
  <section class="overview-split-module overview-fixed-mode" :class="{ 'overview-split-mode': mode === 'overview' }">
    <!-- 顶部概览：标题、KPI 统计（总计划/未完成/已完成）、刷新按钮与用户档案 -->
    <article class="summary-overview module-overview overview-summary-panel">
      <div class="summary-overview-top">
        <div>
          <h2>{{ title }}</h2>
          <p>管理计划与进度，支持筛选和编辑</p>
        </div>
        <div class="module-summary-right">
          <div class="module-kpi">
            <span>总计划：<b>{{ list.length }}</b></span>
            <span>未完成：<b>{{ pendingCount }}</b></span>
            <span>已完成：<b>{{ doneCount }}</b></span>
            <button type="button" :disabled="loading" @click="load">{{ loading ? "刷新中..." : "刷新数据" }}</button>
          </div>
          <ProfileBadge :profile="props.profile" compact />
        </div>
      </div>
    </article>

    <!-- 概览模式：左侧新增表单 + 右侧筛选栏与计划卡片列表 -->
    <div class="overview-split-panel" v-if="mode === 'overview'">
      <div class="overview-split-grid plan-workbench-grid">
        <!-- 新增规划表单 -->
        <aside class="overview-create-panel plan-workbench-form">
          <div class="overview-panel-head"><h3>新增规划</h3><span>安排待办事项</span></div>
          <form class="module-form overview-create-form" @submit.prevent="submit">
            <label>标题<input v-model="form.title" type="text" required /></label>
            <label>优先级<select v-model="form.priority"><option value="HIGH">高</option><option value="MEDIUM">中</option><option value="LOW">低</option></select></label>
            <label>状态<select v-model="form.status"><option value="TODO">待开始</option><option value="IN_PROGRESS">进行中</option><option value="DONE">已完成</option><option value="ENDED">已结束</option></select></label>
            <label>开始日期<input v-model="form.startDate" type="date" /></label>
            <label>截止日期<input v-model="form.endDate" type="date" /></label>
            <label>描述<textarea v-model="form.description" rows="4" placeholder="计划说明" /></label>
            <button type="submit" :disabled="submitting">{{ submitting ? "保存中..." : "保存规划" }}</button>
          </form>
        </aside>

        <!-- 规划列表区：筛选栏 + 卡片滚动列表 -->
        <section class="overview-list-panel plan-workbench-list">
          <div class="overview-panel-head"><h3>规划总览</h3><span>{{ loading ? "加载中..." : `共 ${list.length} 条` }}</span></div>
          <!-- 筛选栏：截止日期、状态、优先级、关键词 -->
          <div class="accounting-filter plan-filter-row">
            <label>截止日期<input v-model="filter.deadline" type="date" /></label>
            <label>状态<select v-model="filter.status"><option value="">全部</option><option value="TODO">待开始</option><option value="IN_PROGRESS">进行中</option><option value="DONE">已完成</option><option value="ENDED">已结束</option></select></label>
            <label>优先级<select v-model="filter.priority"><option value="">全部</option><option value="HIGH">高</option><option value="MEDIUM">中</option><option value="LOW">低</option></select></label>
            <label>关键词<input v-model="filter.keyword" type="text" placeholder="标题或描述" /></label>
            <button type="button" @click="resetFilter">重置</button>
          </div>

          <!-- 计划卡片列表，按筛选结果渲染；无数据时显示占位 -->
          <div class="overview-list-scroll plan-card-scroll">
            <article v-for="item in list" :key="item.id" class="plan-board-card">
              <div class="plan-board-head">
                <b>#{{ item.id }}</b>
                <span>{{ statusText(item.status) }}</span>
              </div>
              <div class="plan-board-body" :class="`tone-${planTone(item)}`">
                <h4>{{ item.title || "未命名计划" }}</h4>
                <p>{{ item.startDate || "-" }} ~ {{ item.endDate || "-" }}</p>
                <p>优先级：{{ priorityText(item.priority) }}</p>
                <p v-if="item.description">{{ item.description }}</p>
              </div>
              <div class="plan-board-actions">
                <button class="table-action-btn" type="button" @click="openEdit(item)">编辑</button>
                <button class="table-action-btn secondary" type="button" @click="remove(item.id)">删除</button>
              </div>
            </article>
            <article v-if="!list.length" class="plan-board-empty">暂无规划数据</article>
          </div>
        </section>
      </div>
    </div>

    <!-- 月度/年度模式：只读明细表格 -->
    <section class="panel overview-readonly-panel" v-else>
      <div class="overview-panel-head"><h3>{{ mode === "monthly" ? "月度规划明细" : "年度规划明细" }}</h3><span>共 {{ list.length }} 条</span></div>
      <div class="accounting-filter overview-bill-filter">
        <label v-if="mode === 'monthly'">月份<select v-model="filter.month"><option v-for="item in monthOptions" :key="item" :value="item">{{ item }}</option></select></label>
        <label v-if="mode === 'yearly'">年份<select v-model="filter.year"><option v-for="item in yearOptions" :key="item" :value="item">{{ item }}</option></select></label>
        <label>状态<select v-model="filter.status"><option value="">全部</option><option value="TODO">待开始</option><option value="IN_PROGRESS">进行中</option><option value="DONE">已完成</option><option value="ENDED">已结束</option></select></label>
        <label>优先级<select v-model="filter.priority"><option value="">全部</option><option value="HIGH">高</option><option value="MEDIUM">中</option><option value="LOW">低</option></select></label>
        <label>关键词<input v-model="filter.keyword" type="text" placeholder="标题或描述" /></label>
        <button type="button" @click="resetFilter">重置</button>
      </div>

      <div class="overview-list-scroll">
        <table>
          <thead><tr><th>标题</th><th>开始</th><th>截止</th><th>优先级</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.title || "-" }}</td><td>{{ item.startDate || "-" }}</td><td>{{ item.endDate || "-" }}</td><td>{{ priorityText(item.priority) }}</td><td>{{ statusText(item.status) }}</td>
            </tr>
            <tr v-if="!list.length"><td colspan="5">暂无数据</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 编辑规划弹窗 -->
    <div v-if="editOpen" class="edit-modal-mask">
      <section class="edit-modal-panel">
        <div class="edit-modal-head"><h3>编辑规划</h3><button type="button" @click="closeEdit">×</button></div>
        <form class="module-form edit-modal-form" @submit.prevent="saveEdit">
          <label>标题<input v-model="editForm.title" type="text" required /></label>
          <label>优先级<select v-model="editForm.priority"><option value="HIGH">高</option><option value="MEDIUM">中</option><option value="LOW">低</option></select></label>
          <label>状态<select v-model="editForm.status"><option value="TODO">待开始</option><option value="IN_PROGRESS">进行中</option><option value="DONE">已完成</option><option value="ENDED">已结束</option></select></label>
          <label>开始日期<input v-model="editForm.startDate" type="date" /></label>
          <label>截止日期<input v-model="editForm.endDate" type="date" /></label>
          <label>描述<textarea v-model="editForm.description" rows="3" /></label>
          <div class="edit-modal-actions"><button class="secondary" type="button" @click="closeEdit">取消</button><button type="submit">保存修改</button></div>
        </form>
      </section>
    </div>

    <!-- 全局确认/提示对话框 -->
    <ConfirmDialog v-bind="dialog" @confirm="closeDialog(true)" @cancel="closeDialog(false)" />
  </section>
</template>
