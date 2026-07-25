<script setup>
/**
 * 目标管理模块组件。
 * 提供目标的增删改查、进度（金额）更新与状态筛选，支持总览 / 月度 / 年度三种展示模式。
 */
import { computed, onMounted, reactive, ref, watch } from "vue";
import { dashboardApi } from "../../services/dashboardApi";
import { useConfirmDialog } from "../../composables/useConfirmDialog";
import { usePeriodFilter } from "../../composables/usePeriodFilter";
import ConfirmDialog from "../ConfirmDialog.vue";
import ProfileBadge from "../ProfileBadge.vue";

const props = defineProps({ mode: { type: String, default: "overview" }, refreshKey: { type: Number, default: 0 }, profile: { type: Object, default: () => ({}) } });
const goals = ref([]); // 目标列表数据
const loading = ref(false); // 列表加载中状态
const submitting = ref(false); // 新增目标提交中状态
const editOpen = ref(false); // 编辑弹窗是否打开
const editId = ref(null); // 当前正在编辑的目标 id
const form = reactive({ name: "", description: "", targetAmount: "", currentAmount: "0", startDate: "", deadline: "" }); // 新增目标表单
const editForm = reactive({ name: "", description: "", targetAmount: "", currentAmount: "0", startDate: "", deadline: "" }); // 编辑目标表单
const filter = reactive({ status: "", deadline: "", keyword: "", month: "", year: "" }); // 列表筛选条件
const { dialog, openDialog, closeDialog, showMessage } = useConfirmDialog();
const amountDrafts = reactive({}); // 各目标卡片金额输入草稿，按目标 id 缓存
const savingAmountId = ref(null); // 正在保存金额的目标 id

// 根据展示模式动态生成模块标题
const title = computed(() => (props.mode === "monthly" ? "月度目标" : props.mode === "yearly" ? "年度目标" : "目标管理"));
const money = (value) => `¥${Number(value || 0).toFixed(2)}`; // 金额格式化为人民币字符串

// 后端多种状态码到中文状态文案的映射表
const statusAliasMap = {
  NOT_STARTED: "未开始",
  ONGOING: "进行中",
  DONE: "已完成",
  ACHIEVED: "已达成",
  EXPIRED: "已过期",
  TODO: "未开始",
  IN_PROGRESS: "进行中",
  ENDED: "已结束",
  CANCELLED: "已取消",
  CANCELED: "已取消"
};

// 将原始状态码归一化为中文文案，无映射时回退原值
function normalizeStatus(status) {
  const key = String(status || "").trim();
  return statusAliasMap[key] || key || "-";
}

const statusText = (s) => normalizeStatus(s); // 模板中使用的状态展示函数
const { monthOptions, yearOptions, syncPeriodOptions } = usePeriodFilter(goals, "deadline", filter);

// 按当前模式与筛选条件过滤、排序后的目标列表
const list = computed(() => {
  let data = [...goals.value];
  if (props.mode === "monthly") data = data.filter((i) => String(i.deadline || "").slice(0, 7) === filter.month); // 月度模式按截止月份过滤
  if (props.mode === "yearly") data = data.filter((i) => String(i.deadline || "").slice(0, 4) === filter.year); // 年度模式按截止年份过滤
  if (props.mode === "overview" && filter.deadline) data = data.filter((i) => String(i.deadline || "") <= filter.deadline); // 总览模式按截止日期上限过滤
  if (filter.status) data = data.filter((i) => normalizeStatus(i.status) === filter.status); // 按状态过滤
  if (filter.keyword.trim()) data = data.filter((i) => `${i.name || ""} ${i.description || ""}`.toLowerCase().includes(filter.keyword.trim().toLowerCase())); // 按名称/描述关键词过滤
  return data.sort((a, b) => String(a.deadline || "9999-12-31").localeCompare(String(b.deadline || "9999-12-31"))); // 按截止日期升序排序
});

const doneCount = computed(() => list.value.filter((i) => ["已完成", "已达成"].includes(normalizeStatus(i.status))).length); // 已达成目标数量
const pendingCount = computed(() => list.value.filter((i) => !["已完成", "已达成"].includes(normalizeStatus(i.status))).length); // 未完成目标数量

// 计算单个目标的完成进度百分比（0~100）
const percent = (item) => {
  const target = Number(item.targetAmount || 0);
  if (target <= 0) return 0;
  return Math.min(100, Math.round((Number(item.currentAmount || 0) / target) * 100));
};

// 根据状态与进度返回目标卡片的视觉色调标识
function goalTone(item) {
  const status = normalizeStatus(item.status);
  if (status === "已达成" || status === "已完成") return "done";
  if (status === "已过期") return "ended";
  if (percent(item) >= 70) return "high";
  return "normal";
}

// 获取/初始化指定目标的金额输入草稿，避免直接修改列表数据
function getDraft(item) {
  const id = item?.id;
  if (!id) return { currentAmount: "", targetAmount: "" };
  if (!amountDrafts[id]) {
    amountDrafts[id] = {
      currentAmount: String(item.currentAmount ?? ""),
      targetAmount: String(item.targetAmount ?? "")
    };
  }
  return amountDrafts[id];
}

// 重置新增目标表单
function resetForm() {
  Object.assign(form, { name: "", description: "", targetAmount: "", currentAmount: "0", startDate: "", deadline: "" });
}

// 将表单数据转换为提交给后端的请求体（金额转数字，空日期转 null）
function buildBody(source) {
  return {
    name: source.name.trim(),
    description: source.description.trim(),
    targetAmount: Number(source.targetAmount || 0),
    currentAmount: Number(source.currentAmount || 0),
    startDate: source.startDate || null,
    deadline: source.deadline || null
  };
}

// 打开编辑弹窗并用选中目标的数据回填编辑表单
function openEdit(item) {
  editId.value = item.id;
  Object.assign(editForm, {
    name: item.name || "",
    description: item.description || "",
    targetAmount: String(item.targetAmount || ""),
    currentAmount: String(item.currentAmount || 0),
    startDate: item.startDate || "",
    deadline: item.deadline || ""
  });
  editOpen.value = true;
}

// 关闭编辑弹窗并清空编辑目标 id
function closeEdit() {
  editOpen.value = false;
  editId.value = null;
}

/** 加载目标列表并同步月份/年份筛选选项 */
async function load() {
  loading.value = true;
  try {
    goals.value = await dashboardApi.getGoals(); // 拉取目标数据
    syncPeriodOptions(); // 根据数据刷新可选周期
  } finally {
    loading.value = false;
  }
}

/** 提交新增目标 */
async function submit() {
  submitting.value = true;
  try {
    await dashboardApi.createGoal(buildBody(form)); // 创建目标
    resetForm(); // 清空表单
    await showMessage("创建成功", "目标已创建。");
    await load(); // 重新加载列表
  } finally {
    submitting.value = false;
  }
}

/** 保存编辑弹窗中的目标修改 */
async function saveEdit() {
  if (!(await openDialog({ title: "确认修改", message: "确认保存对这条目标的修改？" }))) return; // 二次确认
  await dashboardApi.updateGoal(editId.value, buildBody(editForm)); // 更新目标
  closeEdit();
  await showMessage("修改成功", "目标已更新。");
  await load();
}

/** 在卡片上直接保存目标的当前/目标金额 */
async function saveCardAmount(item) {
  if (!item?.id) return;
  const draft = getDraft(item);
  const currentAmount = Number(draft.currentAmount || 0);
  const targetAmount = Number(draft.targetAmount || 0);
  // 校验金额合法性（非 NaN 且非负）
  if (Number.isNaN(currentAmount) || Number.isNaN(targetAmount) || targetAmount < 0 || currentAmount < 0) {
    await showMessage("输入无效", "请填写有效的金额。");
    return;
  }

  savingAmountId.value = item.id; // 标记该卡片为保存中
  try {
    // 仅更新金额，其余字段沿用原值
    await dashboardApi.updateGoal(item.id, {
      name: item.name || "",
      description: item.description || "",
      startDate: item.startDate || null,
      deadline: item.deadline || null,
      targetAmount,
      currentAmount
    });
    await load();
  } finally {
    savingAmountId.value = null;
  }
}

/** 删除指定目标（删除前需确认） */
async function remove(id) {
  if (!(await openDialog({ title: "确认删除", message: "确认删除这条目标？删除后不可恢复。", confirmText: "删除", danger: true }))) return; // 危险操作确认
  await dashboardApi.deleteGoal(id);
  await showMessage("删除成功", "目标已删除。");
  await load();
}

// 重置筛选条件，周期类筛选回退到首个可选项
function resetFilter() {
  Object.assign(filter, { status: "", deadline: "", keyword: "", month: monthOptions.value[0] || "", year: yearOptions.value[0] || "" });
}

watch(() => props.refreshKey, load); // 外部刷新信号变化时重新加载
onMounted(load); // 组件挂载时初始化加载
</script>

<template>
  <section class="overview-split-module overview-fixed-mode" :class="{ 'overview-split-mode': mode === 'overview' }">
    <!-- 顶部概览：标题、KPI 统计与刷新 -->
    <article class="summary-overview module-overview overview-summary-panel">
      <div class="summary-overview-top">
        <div><h2>{{ title }}</h2><p>管理目标进度与达成情况</p></div>
        <div class="module-summary-right">
          <div class="module-kpi">
            <span>总目标：<b>{{ list.length }}</b></span>
            <span>未完成：<b>{{ pendingCount }}</b></span>
            <span>已达成：<b>{{ doneCount }}</b></span>
            <button type="button" :disabled="loading" @click="load">{{ loading ? "刷新中..." : "刷新数据" }}</button>
          </div>
          <ProfileBadge :profile="props.profile" compact />
        </div>
      </div>
    </article>

    <!-- 总览模式：左侧新增表单 + 右侧目标卡片列表 -->
    <div class="overview-split-panel" v-if="mode === 'overview'">
      <div class="overview-split-grid goal-workbench-grid">
        <!-- 新增目标表单 -->
        <aside class="overview-create-panel goal-workbench-form">
          <div class="overview-panel-head"><h3>新增目标</h3><span>记录目标进度</span></div>
          <form class="module-form overview-create-form" @submit.prevent="submit">
            <label>目标名称<input v-model="form.name" type="text" placeholder="如：旅行基金" required /></label>
            <label>目标金额<input v-model="form.targetAmount" type="number" step="0.01" required /></label>
            <label>当前金额<input v-model="form.currentAmount" type="number" step="0.01" required /></label>
            <label>开始日期<input v-model="form.startDate" type="date" /></label>
            <label>截止日期<input v-model="form.deadline" type="date" /></label>
            <label>描述<textarea v-model="form.description" rows="4" placeholder="目标说明" /></label>
            <button type="submit" :disabled="submitting">{{ submitting ? "保存中..." : "保存目标" }}</button>
          </form>
        </aside>

        <!-- 目标总览：筛选栏 + 卡片滚动列表 -->
        <section class="overview-list-panel goal-workbench-list">
          <div class="overview-panel-head"><h3>目标总览</h3><span>{{ loading ? "加载中..." : `共 ${list.length} 条` }}</span></div>
          <!-- 筛选栏：截止日期 / 状态 / 关键词 -->
          <div class="accounting-filter goal-filter-row">
            <label>截止日期<input v-model="filter.deadline" type="date" /></label>
            <label>状态<select v-model="filter.status"><option value="">全部</option><option value="未开始">未开始</option><option value="进行中">进行中</option><option value="已完成">已完成</option><option value="已达成">已达成</option><option value="已过期">已过期</option></select></label>
            <label>关键词<input v-model="filter.keyword" type="text" placeholder="名称或描述" /></label>
            <button type="button" @click="resetFilter">重置</button>
          </div>

          <!-- 目标卡片列表：含进度条、金额快速编辑与操作按钮 -->
          <div class="overview-list-scroll goal-card-scroll">
            <article v-for="item in list" :key="item.id" class="goal-board-card">
              <div class="goal-board-head">
                <b>#{{ item.id }}</b>
                <span>{{ statusText(item.status) }}</span>
              </div>
              <div class="goal-board-body" :class="`tone-${goalTone(item)}`">
                <h4>{{ item.name || "未命名目标" }}</h4>
                <p>~ {{ item.deadline || "-" }}</p>
                <!-- 进度条：宽度反映完成百分比 -->
                <div class="goal-progress-track">
                  <div class="goal-progress-bar" :style="{ width: `${percent(item)}%` }"></div>
                </div>
                <!-- 金额快速编辑：已完成/总目标金额 -->
                <div class="goal-amount-edit">
                  <span>已完成/总目标：</span>
                  <input v-model="getDraft(item).currentAmount" type="number" step="0.01" />
                  <span>/</span>
                  <input v-model="getDraft(item).targetAmount" type="number" step="0.01" />
                  <button class="table-action-btn" type="button" :disabled="savingAmountId === item.id" @click="saveCardAmount(item)">
                    {{ savingAmountId === item.id ? "保存中..." : "保存金额" }}
                  </button>
                </div>
                <p v-if="item.description">{{ item.description }}</p>
              </div>
              <div class="goal-board-actions">
                <button class="table-action-btn" type="button" @click="openEdit(item)">编辑</button>
                <button class="table-action-btn secondary" type="button" @click="remove(item.id)">删除</button>
              </div>
            </article>
            <article v-if="!list.length" class="plan-board-empty">暂无目标数据</article>
          </div>
        </section>
      </div>
    </div>

    <!-- 月度 / 年度模式：只读明细表格 -->
    <section class="panel overview-readonly-panel" v-else>
      <div class="overview-panel-head"><h3>{{ mode === "monthly" ? "月度目标明细" : "年度目标明细" }}</h3><span>共 {{ list.length }} 条</span></div>
      <div class="accounting-filter overview-bill-filter">
        <label v-if="mode === 'monthly'">月份<select v-model="filter.month"><option v-for="item in monthOptions" :key="item" :value="item">{{ item }}</option></select></label>
        <label v-if="mode === 'yearly'">年份<select v-model="filter.year"><option v-for="item in yearOptions" :key="item" :value="item">{{ item }}</option></select></label>
        <label>状态<select v-model="filter.status"><option value="">全部</option><option value="未开始">未开始</option><option value="进行中">进行中</option><option value="已完成">已完成</option><option value="已达成">已达成</option><option value="已过期">已过期</option></select></label>
        <label>关键词<input v-model="filter.keyword" type="text" placeholder="名称或描述" /></label>
        <button type="button" @click="resetFilter">重置</button>
      </div>

      <div class="overview-list-scroll">
        <table>
          <thead><tr><th>名称</th><th>截止</th><th>目标金额</th><th>当前金额</th><th>状态</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ item.name || "-" }}</td><td>{{ item.deadline || "-" }}</td><td>{{ money(item.targetAmount) }}</td><td>{{ money(item.currentAmount) }}</td><td>{{ statusText(item.status) }}</td>
            </tr>
            <tr v-if="!list.length"><td colspan="5">暂无数据</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 编辑目标弹窗 -->
    <div v-if="editOpen" class="edit-modal-mask">
      <section class="edit-modal-panel">
        <div class="edit-modal-head"><h3>编辑目标</h3><button type="button" @click="closeEdit">×</button></div>
        <form class="module-form edit-modal-form" @submit.prevent="saveEdit">
          <label>名称<input v-model="editForm.name" type="text" required /></label>
          <label>目标金额<input v-model="editForm.targetAmount" type="number" step="0.01" required /></label>
          <label>当前金额<input v-model="editForm.currentAmount" type="number" step="0.01" required /></label>
          <label>开始日期<input v-model="editForm.startDate" type="date" /></label>
          <label>截止日期<input v-model="editForm.deadline" type="date" /></label>
          <label>描述<textarea v-model="editForm.description" rows="3" /></label>
          <div class="edit-modal-actions"><button class="secondary" type="button" @click="closeEdit">取消</button><button type="submit">保存修改</button></div>
        </form>
      </section>
    </div>

    <!-- 全局确认/提示对话框 -->
    <ConfirmDialog v-bind="dialog" @confirm="closeDialog(true)" @cancel="closeDialog(false)" />
  </section>
</template>
