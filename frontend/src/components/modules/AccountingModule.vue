<script setup>
/**
 * 记账模块组件
 * 负责收支记录的增删改查、按日期/类型/分类筛选，以及收入与支出的统计汇总。
 * 通过 mode 切换三种视图：overview（记账管理，可编辑）、monthly（月度账单，只读）、yearly（年度账单，只读）。
 */
import { computed, onMounted, reactive, ref, watch } from "vue";
import { dashboardApi } from "../../services/dashboardApi";
import { useConfirmDialog } from "../../composables/useConfirmDialog";
import { usePeriodFilter } from "../../composables/usePeriodFilter";
import ConfirmDialog from "../ConfirmDialog.vue";
import ProfileBadge from "../ProfileBadge.vue";

const props = defineProps({
  mode: { type: String, default: "overview" },
  refreshKey: { type: Number, default: 0 },
  profile: { type: Object, default: () => ({}) }
});

const records = ref([]); // 全部记账记录原始数据
const loading = ref(false); // 列表加载状态
const submitting = ref(false); // 新增表单提交中状态
const editOpen = ref(false); // 编辑弹窗是否打开
const editId = ref(null); // 当前正在编辑的记录 id

const form = reactive({ type: "EXPENSE", amount: "", category: "", accountDate: "", note: "" }); // 新增记账表单
const editForm = reactive({ type: "EXPENSE", amount: "", category: "", accountDate: "", note: "" }); // 编辑记账表单
const filter = reactive({ start: "", end: "", type: "", category: "", month: "", year: "" }); // 列表筛选条件
const { dialog, openDialog, closeDialog, showMessage } = useConfirmDialog();

// 根据 mode 计算页面标题
const pageTitle = computed(() => (props.mode === "monthly" ? "月度账单" : props.mode === "yearly" ? "年度账单" : "记账管理"));
// 金额格式化为带￥符号、保留两位小数的字符串
const money = (value) => `¥${Number(value || 0).toFixed(2)}`;

// 基于记录的 accountDate 字段生成可选月份/年份，并提供同步方法
const { monthOptions, yearOptions, syncPeriodOptions } = usePeriodFilter(records, "accountDate", filter);

// 分类下拉选项：从当前记录中去重提取分类，受类型筛选影响并按中文排序
const categoryOptions = computed(() => {
  let source = records.value;
  if (filter.type) source = source.filter((i) => i.type === filter.type);
  return [...new Set(source.map((i) => (i.category || "").trim()).filter(Boolean))].sort((a, b) => a.localeCompare(b, 'zh'));
});

// 经过筛选与排序后的最终展示列表
const list = computed(() => {
  let data = [...records.value];
  // 月度/年度视图按对应的月份或年份过滤
  if (props.mode === "monthly") data = data.filter((i) => String(i.accountDate || "").slice(0, 7) === filter.month);
  if (props.mode === "yearly") data = data.filter((i) => String(i.accountDate || "").slice(0, 4) === filter.year);

  // 概览视图按起止日期范围过滤
  if (props.mode === "overview") {
    if (filter.start) data = data.filter((i) => String(i.accountDate || "").slice(0, 10) >= filter.start);
    if (filter.end) data = data.filter((i) => String(i.accountDate || "").slice(0, 10) <= filter.end);
  }

  // 按类型与分类过滤
  if (filter.type) data = data.filter((i) => i.type === filter.type);
  if (filter.category.trim()) data = data.filter((i) => (i.category || "").trim() === filter.category.trim());
  // 按记账时间倒序排列
  return data.sort((a, b) => new Date(b.accountDate || 0).getTime() - new Date(a.accountDate || 0).getTime());
});

// 当前列表的收入合计
const income = computed(() => list.value.filter((i) => i.type === "INCOME").reduce((s, i) => s + Number(i.amount || 0), 0));
// 当前列表的支出合计
const expense = computed(() => list.value.filter((i) => i.type === "EXPENSE").reduce((s, i) => s + Number(i.amount || 0), 0));

// 将表单数据规整为提交给后端的请求体（数值转换、去除首尾空白）
function buildBody(source) {
  return {
    type: source.type,
    amount: Number(source.amount || 0),
    category: source.category.trim(),
    accountDate: source.accountDate,
    note: source.note.trim()
  };
}

// 重置新增表单为默认值
function resetForm() {
  Object.assign(form, { type: "EXPENSE", amount: "", category: "", accountDate: "", note: "" });
}

// 打开编辑弹窗并以选中记录填充编辑表单
function openEdit(item) {
  editId.value = item.id;
  Object.assign(editForm, {
    type: item.type || "EXPENSE",
    amount: String(item.amount || ""),
    category: item.category || "",
    accountDate: String(item.accountDate || "").slice(0, 16),
    note: item.note || ""
  });
  editOpen.value = true;
}

// 关闭编辑弹窗并清空编辑状态
function closeEdit() {
  editOpen.value = false;
  editId.value = null;
}

/** 加载全部记账记录，并同步月份/年份筛选选项 */
async function load() {
  loading.value = true;
  try {
    records.value = await dashboardApi.getAccounting(); // 调用接口获取记账数据
    syncPeriodOptions(); // 根据最新数据刷新可选周期
  } finally {
    loading.value = false;
  }
}

/** 提交新增记账记录 */
async function submit() {
  submitting.value = true;
  try {
    await dashboardApi.createAccounting(buildBody(form)); // 调用创建接口
    resetForm(); // 提交成功后清空表单
    await showMessage("创建成功", "记账记录已创建。");
    await load(); // 重新加载列表
  } finally {
    submitting.value = false;
  }
}

/** 保存编辑后的记账记录（需用户二次确认） */
async function saveEdit() {
  if (!(await openDialog({ title: "确认修改", message: "确认保存对这条记录的修改？" }))) return;
  await dashboardApi.updateAccounting(editId.value, buildBody(editForm)); // 调用更新接口
  closeEdit();
  await showMessage("修改成功", "记账记录已更新。");
  await load();
}

/** 删除指定记账记录（危险操作，需用户二次确认） */
async function remove(id) {
  if (!(await openDialog({ title: "确认删除", message: "确认删除这条记账记录？删除后不可恢复。", danger: true, confirmText: "删除" }))) return;
  await dashboardApi.deleteAccounting(id); // 调用删除接口
  await showMessage("删除成功", "记账记录已删除。");
  await load();
}

// 重置筛选条件，月份/年份回退到首个可选项
function resetFilter() {
  Object.assign(filter, { start: "", end: "", type: "", category: "", month: monthOptions.value[0] || "", year: yearOptions.value[0] || "" });
}

watch(() => props.refreshKey, load); // 外部 refreshKey 变化时重新加载数据
onMounted(load); // 组件挂载后首次加载
</script>

<template>
  <section class="accounting-module accounting-fixed-mode" :class="{ 'accounting-overview-mode': mode === 'overview' }">
    <!-- 顶部汇总面板：标题、收入/支出 KPI 与用户信息 -->
    <article class="summary-overview module-overview accounting-summary-panel">
      <div class="summary-overview-top">
        <div>
          <h2>{{ pageTitle }}</h2>
          <p>当前数据 {{ loading ? "加载中..." : "已更新" }}</p>
        </div>
        <div class="module-summary-right">
          <div class="module-kpi">
            <span>收入：<b class="amount-income">{{ money(income) }}</b></span>
            <span>支出：<b class="amount-expense">{{ money(expense) }}</b></span>
          </div>
          <ProfileBadge :profile="props.profile" compact />
        </div>
      </div>
    </article>

    <!-- 概览视图：左侧新增表单 + 右侧可编辑账单列表 -->
    <div class="accounting-overview-panel" v-if="mode === 'overview'">
      <div class="accounting-overview-grid">
        <!-- 新增记账表单 -->
        <aside class="accounting-create-panel">
          <div class="accounting-panel-head">
            <h3>新增记账</h3>
            <span>录入收入或支出</span>
          </div>
          <form class="module-form accounting-create-form" @submit.prevent="submit">
            <label>类型
              <select v-model="form.type">
                <option value="EXPENSE">支出</option>
                <option value="INCOME">收入</option>
              </select>
            </label>
            <label>金额<input v-model="form.amount" type="number" step="0.01" required /></label>
            <label>分类<input v-model="form.category" type="text" required /></label>
            <label>日期时间<input v-model="form.accountDate" type="datetime-local" required /></label>
            <label>备注<textarea v-model="form.note" rows="2" /></label>
            <button type="submit" :disabled="submitting">{{ submitting ? "保存中..." : "保存记录" }}</button>
          </form>
        </aside>

        <!-- 账单列表面板 -->
        <section class="accounting-list-panel">
          <div class="accounting-panel-head">
            <h3>账单列表</h3>
            <span>共 {{ list.length }} 条</span>
          </div>
          <!-- 筛选栏：起止日期、类型、分类 -->
          <div class="accounting-filter">
            <label>开始<input v-model="filter.start" type="date" /></label>
            <label>结束<input v-model="filter.end" type="date" /></label>
            <label>类型
              <select v-model="filter.type">
                <option value="">全部</option>
                <option value="INCOME">收入</option>
                <option value="EXPENSE">支出</option>
              </select>
            </label>
            <label>分类
              <select v-model="filter.category">
                <option value="">全部</option>
                <option v-for="cat in categoryOptions" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </label>
            <button type="button" @click="resetFilter">重置</button>
          </div>

          <!-- 账单数据表格，支持编辑与删除操作 -->
          <div class="accounting-table-scroll">
            <table>
              <thead>
                <tr><th>时间</th><th>类型</th><th>分类</th><th>金额</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="item in list" :key="item.id">
                  <td>{{ String(item.accountDate || "").replace("T", " ").slice(0, 16) }}</td>
                  <td>{{ item.type === "INCOME" ? "收入" : "支出" }}</td>
                  <td>{{ item.category || "-" }}</td>
                  <td :class="item.type === 'INCOME' ? 'amount-income' : 'amount-expense'">{{ money(item.amount) }}</td>
                  <td>
                    <button class="table-action-btn" type="button" @click="openEdit(item)">编辑</button>
                    <button class="table-action-btn secondary" type="button" @click="remove(item.id)">删除</button>
                  </td>
                </tr>
                <tr v-if="!list.length"><td colspan="5">暂无数据</td></tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>
    </div>

    <!-- 月度/年度只读视图：仅展示账单明细，不可编辑 -->
    <section class="panel accounting-readonly-panel" v-else>
      <div class="accounting-panel-head">
        <h3>{{ mode === "monthly" ? "月度账单明细" : "年度账单明细" }}</h3>
        <span>共 {{ list.length }} 条</span>
      </div>

      <!-- 周期筛选栏：按月份或年份及类型、分类筛选 -->
      <div class="accounting-filter accounting-bill-filter">
        <label v-if="mode === 'monthly'">月份
          <select v-model="filter.month"><option v-for="item in monthOptions" :key="item" :value="item">{{ item }}</option></select>
        </label>
        <label v-if="mode === 'yearly'">年份
          <select v-model="filter.year"><option v-for="item in yearOptions" :key="item" :value="item">{{ item }}</option></select>
        </label>
        <label>类型
          <select v-model="filter.type">
            <option value="">全部</option>
            <option value="INCOME">收入</option>
            <option value="EXPENSE">支出</option>
          </select>
        </label>
        <label>分类
          <select v-model="filter.category">
            <option value="">全部</option>
            <option v-for="cat in categoryOptions" :key="cat" :value="cat">{{ cat }}</option>
          </select>
        </label>
        <button type="button" @click="resetFilter">重置</button>
      </div>

      <div class="accounting-table-scroll">
        <table>
          <thead><tr><th>时间</th><th>类型</th><th>分类</th><th>金额</th><th>备注</th></tr></thead>
          <tbody>
            <tr v-for="item in list" :key="item.id">
              <td>{{ String(item.accountDate || "").replace("T", " ").slice(0, 16) }}</td>
              <td>{{ item.type === "INCOME" ? "收入" : "支出" }}</td>
              <td>{{ item.category || "-" }}</td>
              <td :class="item.type === 'INCOME' ? 'amount-income' : 'amount-expense'">{{ money(item.amount) }}</td>
              <td>{{ item.note || "-" }}</td>
            </tr>
            <tr v-if="!list.length"><td colspan="5">暂无数据</td></tr>
          </tbody>
        </table>
      </div>
    </section>

    <!-- 编辑记录弹窗：mode 为 overview 时通过列表操作触发 -->
    <div v-if="editOpen" class="edit-modal-mask">
      <section class="edit-modal-panel">
        <div class="edit-modal-head"><h3>编辑记录</h3><button type="button" @click="closeEdit">×</button></div>
        <form class="module-form edit-modal-form" @submit.prevent="saveEdit">
          <label>类型
            <select v-model="editForm.type"><option value="EXPENSE">支出</option><option value="INCOME">收入</option></select>
          </label>
          <label>金额<input v-model="editForm.amount" type="number" step="0.01" required /></label>
          <label>分类<input v-model="editForm.category" type="text" required /></label>
          <label>日期时间<input v-model="editForm.accountDate" type="datetime-local" required /></label>
          <label>备注<textarea v-model="editForm.note" rows="3" /></label>
          <div class="edit-modal-actions"><button class="secondary" type="button" @click="closeEdit">取消</button><button type="submit">保存修改</button></div>
        </form>
      </section>
    </div>

    <!-- 全局二次确认对话框 -->
    <ConfirmDialog v-bind="dialog" @confirm="closeDialog(true)" @cancel="closeDialog(false)" />
  </section>
</template>
