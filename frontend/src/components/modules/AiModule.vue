<script setup>
/**
 * AI 智能助手模块组件
 *
 * 职责：
 * - 提供三种工作模式：智能对话(chat)、理财建议(finance)、智能提醒(remind)，由 props.mode 决定当前展示。
 * - 智能对话模式下管理多会话：会话列表加载/新建/选择/删除，以及当前会话历史记录的拉取与展示。
 * - 负责消息收发：将用户问题连同业务上下文(buildContext)发送给 AI 接口，并把回复渲染为 Markdown。
 * - 基于账单/计划/目标数据派生本月结余、待办计划、进行中目标等概览指标及理财/提醒文案。
 */
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { dashboardApi } from "../../services/dashboardApi";
import ProfileBadge from "../ProfileBadge.vue";
import ConfirmDialog from "../ConfirmDialog.vue";

const props = defineProps({
  mode: { type: String, default: "chat" }, // 当前工作模式：chat / finance / remind
  refreshKey: { type: Number, default: 0 }, // 外部刷新信号，变化时重新计算概览数据
  profile: { type: Object, default: () => ({}) } // 用户档案信息，用于顶部 ProfileBadge 展示
});

const monthBalance = ref(0); // 本月结余（收入-支出）
const pendingPlans = ref(0); // 待办计划数量
const ongoingGoals = ref(0); // 进行中/未开始目标数量
const financeAdvice = ref([]); // 理财建议文案列表
const reminders = ref([]); // 智能提醒文案列表
const sessions = ref([]); // 会话列表
const currentSessionId = ref(""); // 当前选中的会话 ID
const chatRecords = ref([]); // 当前会话的聊天记录（含 role/text）
const historyEl = ref(null); // 聊天历史滚动容器的 DOM 引用（用于自动滚动到底部）
const chatInput = ref(""); // 输入框内容
const sending = ref(false); // 是否正在发送消息
const loadingChat = ref(false); // 是否正在加载会话历史
const deletingSessionId = ref(""); // 正在删除中的会话 ID（用于禁用按钮）
const pendingDeleteSessionId = ref(""); // 待确认删除的会话 ID
const confirmDeleteOpen = ref(false); // 删除确认弹窗是否打开

// 根据 props.mode 归一化得到当前激活模式
const activeMode = computed(() => {
  if (props.mode === "finance") return "finance";
  if (props.mode === "remind") return "remind";
  return "chat";
});

// 当前模式对应的标题文案
const modeTitle = computed(() => {
  if (activeMode.value === "finance") return "理财建议";
  if (activeMode.value === "remind") return "智能提醒";
  return "智能对话";
});

// 将数值格式化为带￥符号的金额字符串
const money = (value) => `¥${Number(value || 0).toFixed(2)}`;

// 提取日期的年月（YYYY-MM），格式不合法返回空串
function monthKeyOf(value) {
  const key = String(value || "").slice(0, 7);
  return /^\d{4}-\d{2}$/.test(key) ? key : "";
}

// 计算目标日期距今天的天数差（正数为未来，负数为过去），非法日期返回极大值
function dayDiff(dateValue) {
  const raw = String(dateValue || "").slice(0, 10);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(raw)) return Number.MAX_SAFE_INTEGER;
  const target = new Date(`${raw}T00:00:00`).getTime();
  if (!Number.isFinite(target)) return Number.MAX_SAFE_INTEGER;
  const now = new Date();
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  return Math.floor((target - today) / (24 * 60 * 60 * 1000));
}

/**
 * 规整 AI 回复文本，便于后续 Markdown 渲染
 * 统一换行符、清理空的列表项、压缩多余空行、规范有序/无序列表前的换行
 */
function formatAssistantText(text) {
  const raw = String(text || "").replace(/\r\n/g, "\n").trim();
  if (!raw) return "AI 暂无回复，请稍后重试。";

  return raw
    .replace(/^\s*[*-]\s*$/gm, "")
    .replace(/^\s*[*-]\s+(?=\n|$)/gm, "")
    .replace(/\n{3,}/g, "\n\n")
    .replace(/(?:^|\n)\s*(\d+[.、]\s*)/gm, "\n$1")
    .replace(/(?:^|\n)\s*([-*]\s+)/gm, "\n$1")
    .replace(/\n{3,}/g, "\n\n")
    .trim();
}

// 转义 HTML 特殊字符，防止 v-html 渲染时被注入
function escapeHtml(text) {
  return String(text || "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/\"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

// 渲染行内 Markdown：先转义再处理代码、加粗、斜体
function renderInlineMarkdown(text) {
  return escapeHtml(text)
    .replace(/`([^`]+)`/g, "<code>$1</code>")
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*([^*]+)\*/g, "<em>$1</em>");
}

/**
 * 将 AI 回复文本渲染为 HTML 字符串
 * 按空行切分为块，逐块识别无序列表、有序列表、表格、标题或普通段落
 */
function renderMarkdown(text) {
  const blocks = formatAssistantText(text).split(/\n{2,}/);
  const html = [];
  let listType = ""; // 当前打开的列表类型（ul/ol），用于跨块闭合

  // 闭合当前未关闭的列表标签
  function closeList() {
    if (!listType) return;
    html.push(`</${listType}>`);
    listType = "";
  }

  for (const block of blocks) {
    const lines = block.split("\n").map((line) => line.trim()).filter(Boolean);
    if (!lines.length) continue;

    // 整块均为无序列表项
    if (lines.every((line) => /^[-*]\s+/.test(line))) {
      if (listType !== "ul") {
        closeList();
        html.push("<ul>");
        listType = "ul";
      }
      lines.forEach((line) => html.push(`<li>${renderInlineMarkdown(line.replace(/^[-*]\s+/, ""))}</li>`));
      continue;
    }

    // 整块均为有序列表项
    if (lines.every((line) => /^\d+[.、]\s+/.test(line))) {
      if (listType !== "ol") {
        closeList();
        html.push("<ol>");
        listType = "ol";
      }
      lines.forEach((line) => html.push(`<li>${renderInlineMarkdown(line.replace(/^\d+[.、]\s+/, ""))}</li>`));
      continue;
    }

    closeList();

    // 识别 Markdown 表格：首行表头、次行为分隔行
    const isTable = lines.length >= 2
        && lines.every((l) => l.includes("|"))
        && /^[\s|:\-]+$/.test(lines[1]);
    if (isTable) {
      const parseCells = (line) => line.split("|").slice(1, -1).map((c) => c.trim());
      const header = parseCells(lines[0]);
      // 依据分隔行的冒号位置解析每列对齐方式
      const aligns = parseCells(lines[1]).map((c) => {
        if (c.startsWith(":") && c.endsWith(":")) return "center";
        if (c.endsWith(":")) return "right";
        return "left";
      });
      let tbl = "<table><thead><tr>";
      header.forEach((h, i) => { tbl += `<th style="text-align:${aligns[i] || "left"}">${renderInlineMarkdown(h)}</th>`; });
      tbl += "</tr></thead><tbody>";
      // 从第三行起为表格数据行
      for (let r = 2; r < lines.length; r++) {
        const cells = parseCells(lines[r]);
        tbl += "<tr>";
        cells.forEach((c, j) => { tbl += `<td style="text-align:${aligns[j] || "left"}">${renderInlineMarkdown(c)}</td>`; });
        tbl += "</tr>";
      }
      tbl += "</tbody></table>";
      html.push(tbl);
      continue;
    }

    // 识别 1~3 级标题（单行块）
    const first = lines[0] || "";
    if (/^#{1,3}\s+/.test(first) && lines.length === 1) {
      const level = Math.min(first.match(/^#+/)[0].length + 2, 5);
      html.push(`<h${level}>${renderInlineMarkdown(first.replace(/^#{1,3}\s+/, ""))}</h${level}>`);
      continue;
    }
    // 其余作为普通段落，行内以 <br> 连接
    html.push(`<p>${lines.map(renderInlineMarkdown).join("<br>")}</p>`);
  }

  closeList();
  return html.join("");
}

/**
 * 拉取账单/计划/目标数据，派生概览指标与理财、提醒文案
 */
async function buildInsights() {
  // 并行获取三类业务数据
  const [accounting, plans, goals] = await Promise.all([
    dashboardApi.getAccounting(),
    dashboardApi.getPlans(),
    dashboardApi.getGoals()
  ]);

  // 统计本月收支与结余
  const nowMonth = new Date().toISOString().slice(0, 7);
  const monthList = (accounting || []).filter((item) => monthKeyOf(item.accountDate) === nowMonth);
  const income = monthList.filter((item) => item.type === "INCOME").reduce((sum, item) => sum + Number(item.amount || 0), 0);
  const expense = monthList.filter((item) => item.type === "EXPENSE").reduce((sum, item) => sum + Number(item.amount || 0), 0);
  const balance = income - expense;

  monthBalance.value = balance;
  pendingPlans.value = (plans || []).filter((item) => item.status !== "DONE" && item.status !== "ENDED").length;
  ongoingGoals.value = (goals || []).filter((item) => item.status === "ONGOING" || item.status === "NOT_STARTED").length;

  // 找出最临近到期的待办计划与目标
  const nextPlan = (plans || [])
    .filter((item) => item.status !== "DONE" && item.status !== "ENDED")
    .map((item) => ({ ...item, leftDays: dayDiff(item.endDate) }))
    .sort((a, b) => a.leftDays - b.leftDays)[0];
  const nextGoal = (goals || [])
    .filter((item) => item.status !== "DONE" && item.status !== "ACHIEVED")
    .map((item) => ({ ...item, leftDays: dayDiff(item.deadline) }))
    .sort((a, b) => a.leftDays - b.leftDays)[0];

  // 组装理财建议文案
  const advice = [];
  if (!monthList.length) advice.push("本月暂无记账数据，建议先补齐固定支出与日常开销。");
  advice.push(`本月收入 ${money(income)}，支出 ${money(expense)}，净额 ${money(balance)}。`);
  advice.push(balance < 0 ? "当前收支为赤字，建议优先控制可选消费并降低预算。" : "当前收支较平衡，建议按周复盘预算执行。");
  advice.push("可设置到账自动转储蓄，降低冲动消费影响。");
  financeAdvice.value = advice;

  // 组装智能提醒文案：临近到期的计划/目标
  const remind = [`当前待办计划 ${pendingPlans.value} 项，进行中目标 ${ongoingGoals.value} 项。`];
  if (nextPlan && nextPlan.leftDays <= 7) remind.push(`计划“${nextPlan.title || "-"}”将在 ${Math.max(0, nextPlan.leftDays)} 天后到期。`);
  if (nextGoal && nextGoal.leftDays <= 14) remind.push(`目标“${nextGoal.name || "-"}”将在 ${Math.max(0, nextGoal.leftDays)} 天后截止。`);
  reminders.value = remind;
}

// 加载会话列表，无选中会话时默认选中第一个
async function loadSessions() {
  const data = await dashboardApi.getAiSessions(50);
  sessions.value = Array.isArray(data) ? data : [];
  if (!currentSessionId.value && sessions.value.length) currentSessionId.value = sessions.value[0].sessionId || "";
}

// 新建会话并切换为当前会话，同时清空聊天记录、刷新列表
async function createSession() {
  const data = await dashboardApi.createAiSession();
  if (data && data.sessionId) {
    currentSessionId.value = data.sessionId;
    chatRecords.value = [];
    await loadSessions();
  }
}

// 请求删除会话：记录待删 ID 并打开确认弹窗（删除进行中时忽略）
function askDeleteSession(sessionId) {
  if (!sessionId || deletingSessionId.value) return;
  pendingDeleteSessionId.value = sessionId;
  confirmDeleteOpen.value = true;
}

// 取消删除：关闭弹窗并清空待删 ID
function cancelDeleteSession() {
  confirmDeleteOpen.value = false;
  pendingDeleteSessionId.value = "";
}

/**
 * 确认删除会话
 * 调用接口删除后从列表移除；若删除的是当前会话，则切换到剩余首个会话或清空记录
 */
async function deleteSessionConfirmed() {
  const sessionId = pendingDeleteSessionId.value;
  confirmDeleteOpen.value = false;
  pendingDeleteSessionId.value = "";
  if (!sessionId || deletingSessionId.value) return;
  deletingSessionId.value = sessionId; // 标记删除中，禁用对应按钮
  try {
    await dashboardApi.deleteAiSession(sessionId);

    // 从本地列表移除该会话
    sessions.value = sessions.value.filter((item) => item.sessionId !== sessionId);

    // 删除的是当前会话时，自动切换或清空
    if (currentSessionId.value === sessionId) {
      const next = sessions.value[0]?.sessionId || "";
      currentSessionId.value = next;
      if (!next) {
        chatRecords.value = [];
      }
    }
  } finally {
    deletingSessionId.value = "";
  }
}

/**
 * 加载指定会话的历史聊天记录
 * 归一化每条记录的角色，AI 回复经 formatAssistantText 处理，结束后滚动到底部
 */
async function loadHistory(sessionId) {
  if (!sessionId) return;
  loadingChat.value = true;
  try {
    const data = await dashboardApi.getAiHistory(sessionId, 200);
    chatRecords.value = Array.isArray(data)
      ? data.map((item) => {
          const role = item && item.role === "user" ? "user" : "assistant";
          const text = (item && item.text) || "";
          return { role, text: role === "assistant" ? formatAssistantText(text) : text };
        })
      : [];
  } finally {
    loadingChat.value = false;
    await scrollToBottom(true);
  }
}

// 构造发送给 AI 的业务上下文文本（计划/目标/理财/提醒摘要）
function buildContext() {
  return [
    `待办计划数: ${pendingPlans.value}`,
    `进行中目标数: ${ongoingGoals.value}`,
    `理财建议: ${financeAdvice.value.join("；")}`,
    `提醒事项: ${reminders.value.join("；")}`
  ].join("\n");
}

// 将聊天历史容器滚动到底部（smooth 控制是否平滑滚动）
async function scrollToBottom(smooth = false) {
  await nextTick();
  if (!historyEl.value) return;
  historyEl.value.scrollTo({ top: historyEl.value.scrollHeight, behavior: smooth ? "smooth" : "auto" });
}

/**
 * 发送聊天消息
 * 校验输入并确保存在会话，先本地追加用户消息，再调用 AI 接口追加回复，失败时展示错误提示
 */
async function sendChat() {
  const message = chatInput.value.trim();
  if (!message || sending.value) return;
  if (!currentSessionId.value) await createSession(); // 无会话时先创建
  if (!currentSessionId.value) return;

  sending.value = true;
  chatRecords.value.push({ role: "user", text: message }); // 乐观渲染用户消息
  chatInput.value = "";
  await scrollToBottom(true);
  try {
    // 调用 AI 接口获取回复
    const response = await dashboardApi.askAi({ sessionId: currentSessionId.value, message, context: buildContext() });
    if (response && response.sessionId) currentSessionId.value = response.sessionId;
    chatRecords.value.push({ role: "assistant", text: formatAssistantText((response && response.answer) || "") });
    await loadSessions(); // 刷新会话列表（标题/时间可能更新）
  } catch (err) {
    // 失败时以助手气泡形式展示错误信息
    chatRecords.value.push({ role: "assistant", text: `AI 调用失败：${err.message || "未知错误"}` });
  } finally {
    sending.value = false;
    await scrollToBottom(true);
  }
}

// 输入框键盘事件：Enter 发送，Shift+Enter 换行
function handleInputKeydown(event) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    sendChat();
  }
}

// 选择并切换当前会话
function selectSession(sessionId) {
  currentSessionId.value = sessionId;
}

// 会话项键盘可达性：Enter / 空格 等同点击选中
function onSessionKeydown(event, sessionId) {
  if (event.key === "Enter" || event.key === " ") {
    event.preventDefault();
    selectSession(sessionId);
  }
}

// 外部刷新信号变化时重新计算概览数据
watch(() => props.refreshKey, buildInsights);
// 当前会话切换时加载对应历史记录
watch(currentSessionId, loadHistory);

// 初始化：计算概览 -> 加载会话列表 -> 无会话则新建 -> 加载历史
onMounted(async () => {
  await buildInsights();
  await loadSessions();
  if (!currentSessionId.value) await createSession();
  await loadHistory(currentSessionId.value);
});
</script>

<template>
  <section class="ai-module-fixed">
    <!-- 顶部概览：标题、当前模式、本月结余/待办计划/进行中目标等 KPI 及用户徽标 -->
    <article class="summary-overview module-overview">
      <div class="summary-overview-top">
        <div>
          <h2>AI 智能助手</h2>
          <p>当前页面：{{ modeTitle }}</p>
        </div>
        <div class="module-summary-right">
          <div class="module-kpi">
            <span>本月结余：<b>{{ money(monthBalance) }}</b></span>
            <span>待办计划：<b>{{ pendingPlans }}</b></span>
            <span>进行中目标：<b>{{ ongoingGoals }}</b></span>
          </div>
          <ProfileBadge :profile="props.profile" compact />
        </div>
      </div>
    </article>

    <div class="panel ai-panel ai-panel-fill">
      <!-- 智能对话模式：左侧会话列表 + 右侧聊天主区 -->
      <div v-if="activeMode === 'chat'" class="ai-chat-wrap">
        <!-- 会话侧栏：新建会话与会话列表项（含删除按钮） -->
        <aside class="ai-session-list">
          <button class="secondary" @click="createSession">+ 新建会话</button>
          <div
            v-for="item in sessions"
            :key="item.sessionId"
            class="ai-session-item"
            role="button"
            tabindex="0"
            :class="{ active: item.sessionId === currentSessionId }"
            @click="selectSession(item.sessionId)"
            @keydown="onSessionKeydown($event, item.sessionId)"
          >
            <div class="ai-session-item-head">
              <b>{{ item.title || "新对话" }}</b>
              <button
                class="ai-session-delete"
                type="button"
                :disabled="deletingSessionId === item.sessionId"
                @click.stop="askDeleteSession(item.sessionId)"
              >
                {{ deletingSessionId === item.sessionId ? "..." : "删" }}
              </button>
            </div>
            <span>{{ item.updatedAt || "" }}</span>
          </div>
        </aside>

        <div class="ai-chat-main">
          <!-- 聊天历史区：加载/空态/消息列表三态，AI 消息以 Markdown 渲染 -->
          <div class="ai-chat-history-wrap" ref="historyEl">
            <div v-if="loadingChat" class="ai-chat-empty">正在加载会话内容...</div>
            <div v-else-if="!chatRecords.length" class="ai-chat-empty">有什么可以帮你？可以直接问我预算、目标、计划相关问题。</div>
            <div v-else class="ai-chat-history">
              <div v-for="(item, index) in chatRecords" :key="index" class="ai-msg-row" :class="item.role">
                <div class="ai-msg-avatar">{{ item.role === "user" ? "你" : "AI" }}</div>
                <div class="ai-msg-bubble" :class="item.role">
                  <div v-if="item.role === 'assistant'" class="ai-markdown" v-html="renderMarkdown(item.text)"></div>
                  <template v-else>{{ item.text }}</template>
                </div>
              </div>
            </div>
          </div>
          <!-- 输入区：多行输入框 + 发送按钮，Enter 发送 / Shift+Enter 换行 -->
          <div class="ai-chat-input-row">
            <textarea
              v-model="chatInput"
              rows="3"
              placeholder="输入你的问题，例如：我本月如何优化预算？（Enter 发送，Shift+Enter 换行）"
              @keydown="handleInputKeydown"
            />
            <button @click="sendChat" :disabled="sending || !chatInput.trim()">{{ sending ? "发送中..." : "发送" }}</button>
          </div>
        </div>
      </div>

      <!-- 理财建议模式：逐条展示理财文案 -->
      <ul v-else-if="activeMode === 'finance'" class="ai-list ai-list-fill">
        <li v-for="(item, index) in financeAdvice" :key="index">{{ item }}</li>
      </ul>
      <!-- 智能提醒模式：逐条展示提醒文案 -->
      <ul v-else class="ai-list ai-list-fill">
        <li v-for="(item, index) in reminders" :key="index">{{ item }}</li>
      </ul>
    </div>

    <!-- 删除会话二次确认弹窗 -->
    <ConfirmDialog
      :open="confirmDeleteOpen"
      title="删除会话"
      message="确认删除这个会话吗？会同时删除 logs/ai-chat 下该会话本地聊天记录。"
      confirm-text="确认删除"
      cancel-text="取消"
      danger
      @confirm="deleteSessionConfirmed"
      @cancel="cancelDeleteSession"
    />
  </section>
</template>
