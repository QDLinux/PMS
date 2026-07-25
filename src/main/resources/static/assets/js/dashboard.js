(function () {
    const api = window.AppApi;
    if (!api || !api.getToken()) {
        window.location.href = "/";
        return;
    }
    if (!window.DashboardModules) {
        window.location.href = "/";
        return;
    }

    const $ = (id) => document.getElementById(id);
    const statusText = $("statusText");
    const welcomeUser = $("welcomeUser");
    const summaryAvatar = $("summaryAvatar");
    const summaryName = $("summaryName");
    const summaryEmail = $("summaryEmail");
    const moduleRefreshBtns = Array.from(document.querySelectorAll(".module-refresh-btn"));
    const moduleAvatarEls = Array.from(document.querySelectorAll(".module-avatar"));
    const moduleUserNameEls = Array.from(document.querySelectorAll(".module-user-name"));
    const moduleUserEmailEls = Array.from(document.querySelectorAll(".module-user-email"));
    const monthIncomeEl = $("monthIncome");
    const monthExpenseEl = $("monthExpense");
    const planDoneEl = $("planDone");
    const goalDoneEl = $("goalDone");
    const planPendingEl = $("planPending");
    const goalPendingEl = $("goalPending");
    const planPendingListEl = $("planPendingList");
    const goalPendingListEl = $("goalPendingList");
    const incomeChart = $("incomeChart");
    const expenseChart = $("expenseChart");
    const accDate = $("accDate");
    const accEditDate = $("accEditDate");
    const planStartDate = $("planStartDate");
    const planEndDate = $("planEndDate");
    const planFilterDeadline = $("planFilterDeadline");
    const planEditStartDate = $("planEditStartDate");
    const planEditEndDate = $("planEditEndDate");
    const goalStartDate = $("goalStartDate");
    const goalDeadline = $("goalDeadline");
    const goalFilterDeadline = $("goalFilterDeadline");
    const goalEditStartDate = $("goalEditStartDate");
    const goalEditDeadline = $("goalEditDeadline");
    const refreshBtn = $("refreshBtn");
    const logoutLink = $("logoutLink");
    const planSummaryCard = $("planSummaryCard");
    const goalSummaryCard = $("goalSummaryCard");
    const accountingPopupMenu = $("accountingPopupMenu");
    const plansPopupMenu = $("plansPopupMenu");
    const goalsPopupMenu = $("goalsPopupMenu");
    const aiPopupMenu = $("aiPopupMenu");
    const avatarPreview = $("avatarPreview");
    const avatarFile = $("avatarFile");
    const avatarUploadBtn = $("avatarUploadBtn");
    const profileForm = $("profileForm");
    const passwordForm = $("passwordForm");
    const profileUsername = $("profileUsername");
    const profileNickname = $("profileNickname");
    const newUsername = $("newUsername");
    const profileEmail = $("profileEmail");
    const usernameUpdateBtn = $("usernameUpdateBtn");
    const emailUpdateBtn = $("emailUpdateBtn");
    const oldPassword = $("oldPassword");
    const newPassword = $("newPassword");
    const confirmNewPassword = $("confirmNewPassword");
    const resultModal = $("resultModal");
    const resultModalTitle = $("resultModalTitle");
    const resultModalTag = $("resultModalTag");
    const resultModalMessage = $("resultModalMessage");
    const resultModalConfirmBtn = $("resultModalConfirmBtn");
    const confirmModal = $("confirmModal");
    const confirmModalMessage = $("confirmModalMessage");
    const confirmModalCancelBtn = $("confirmModalCancelBtn");
    const confirmModalOkBtn = $("confirmModalOkBtn");

    const menuLinks = Array.from(document.querySelectorAll(".sidebar nav a[data-section]"));
    const sections = {
        summary: $("summary"),
        accounting: $("accounting"),
        accountingMonthly: $("accountingMonthly"),
        accountingYearly: $("accountingYearly"),
        plans: $("plans"),
        plansMonthly: $("plansMonthly"),
        plansYearly: $("plansYearly"),
        goals: $("goals"),
        goalsMonthly: $("goalsMonthly"),
        goalsYearly: $("goalsYearly"),
        aiAssistant: $("aiAssistant"),
        account: $("account")
    };
    const parentMenuLinks = {
        accounting: document.querySelector('.sidebar nav a[data-section="accounting"]'),
        plans: document.querySelector('.sidebar nav a[data-section="plans"]'),
        goals: document.querySelector('.sidebar nav a[data-section="goals"]'),
        aiAssistant: document.querySelector('.sidebar nav a[data-section="aiAssistant"]')
    };

    const state = {
        latestAccountingList: [],
        latestAccountingAllList: [],
        latestAccountingById: new Map(),
        latestPlanList: [],
        latestVisiblePlanById: new Map(),
        latestGoalList: [],
        latestVisibleGoalById: new Map(),
        currentAccountingFilter: { startDate: "", endDate: "", type: "" },
        currentMonthlyBillMonth: "",
        currentYearlyBillYear: "",
        currentPlanMonthlyMonth: "",
        currentPlanYearlyYear: "",
        currentPlanFilter: { deadline: "", status: "" },
        currentGoalMonthlyMonth: "",
        currentGoalYearlyYear: "",
        currentGoalFilter: { deadline: "", status: "" },
        aiActiveTab: "chat",
        aiChatRecords: [],
        aiChatSending: false,
        aiCurrentSessionId: "",
        aiSessions: [],
        aiSessionsLoaded: false,
        aiSessionsLoading: false,
        aiHistoryLoading: false,
        activeSectionKey: "summary"
    };

    const PLAN_STATUS_LABELS = { TODO: "待开始", IN_PROGRESS: "进行中", DONE: "已完成", ENDED: "已结束" };
    const PLAN_PRIORITY_LABELS = { HIGH: "高", MEDIUM: "中", LOW: "低" };
    const GOAL_STATUS_LABELS = { NOT_STARTED: "未开始", ONGOING: "进行中", DONE: "已完成", ENDED: "已结束", ACHIEVED: "已完成", EXPIRED: "已结束" };
    const PLAN_PRIORITY_RANK = { HIGH: 1, MEDIUM: 2, LOW: 3 };
    const GOAL_STATUS_RANK = { ONGOING: 1, NOT_STARTED: 2, ENDED: 3, EXPIRED: 3, DONE: 4, ACHIEVED: 4 };
    const BRANCH_SECTION_KEYS = {
        accounting: new Set(["accounting", "accountingMonthly", "accountingYearly"]),
        plans: new Set(["plans", "plansMonthly", "plansYearly"]),
        goals: new Set(["goals", "goalsMonthly", "goalsYearly"]),
        aiAssistant: new Set(["aiChat", "aiFinance", "aiRemind", "aiConsume", "aiPlan"])
    };

    const pad2 = (n) => String(n).padStart(2, "0");
    const nowMonthValue = () => {
        const d = new Date();
        return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}`;
    };
    const nowYearValue = () => String(new Date().getFullYear());
    const splitMonthKey = (key) => {
        const [y, m] = String(key || "").split("-");
        return { year: /^\d{4}$/.test(y || "") ? y : nowYearValue(), month: /^(0[1-9]|1[0-2])$/.test(m || "") ? m : pad2(new Date().getMonth() + 1) };
    };
    const toDateTimeLocalValue = (v) => (v ? String(v).replace(" ", "T").slice(0, 16) : "");
    const toDisplayDateTime = (v) => (v ? toDateTimeLocalValue(v).replace("T", " ") : "-");
    const toDisplayDate = (v) => (v ? String(v).slice(0, 10) : "-");
    const toSortableDate = (v) => {
        if (!v) return Number.MAX_SAFE_INTEGER;
        const ts = new Date(String(v).slice(0, 10)).getTime();
        return Number.isFinite(ts) ? ts : Number.MAX_SAFE_INTEGER;
    };
    const toTypeLabel = (t) => (t === "INCOME" ? "收入" : "支出");
    const toPlanStatusLabel = (s) => PLAN_STATUS_LABELS[s] || s || "-";
    const toPlanPriorityLabel = (p) => PLAN_PRIORITY_LABELS[p] || p || "-";
    const toGoalStatusLabel = (s) => GOAL_STATUS_LABELS[s] || s || "-";
    const planPriorityRank = (p) => PLAN_PRIORITY_RANK[p] || 9;
    const goalStatusRank = (s) => GOAL_STATUS_RANK[s] || 9;
    const fmtAmount = (n) => `￥${Number(n || 0).toFixed(2)}`;
    let profileAvatarObjectUrl = "";

    const applyAvatarToView = (src) => {
        if (src) {
            avatarPreview.src = src;
            summaryAvatar.src = src;
            moduleAvatarEls.forEach((el) => { el.src = src; });
            return;
        }
        avatarPreview.removeAttribute("src");
        summaryAvatar.removeAttribute("src");
        moduleAvatarEls.forEach((el) => el.removeAttribute("src"));
    };

    const releaseAvatarObjectUrl = () => {
        if (!profileAvatarObjectUrl) return;
        URL.revokeObjectURL(profileAvatarObjectUrl);
        profileAvatarObjectUrl = "";
    };

    const loadAvatarWithAuth = async (avatarUrl) => {
        if (!avatarUrl) {
            releaseAvatarObjectUrl();
            applyAvatarToView("");
            return;
        }
        const token = api.getToken();
        const headers = token ? { Authorization: `Bearer ${token}` } : {};
        const response = await fetch(`${avatarUrl}?t=${Date.now()}`, { method: "GET", headers });
        if (!response.ok) {
            throw new Error("头像加载失败");
        }
        const blob = await response.blob();
        releaseAvatarObjectUrl();
        profileAvatarObjectUrl = URL.createObjectURL(blob);
        applyAvatarToView(profileAvatarObjectUrl);
    };

    const nowDateTimeLocalMinute = () => {
        const d = new Date();
        return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())}T${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
    };
    const syncAccountingDateMax = () => {
        const max = nowDateTimeLocalMinute();
        if (accDate) {
            accDate.max = max;
            if (accDate.value && accDate.value > max) accDate.value = max;
        }
        if (accEditDate) {
            accEditDate.max = max;
            if (accEditDate.value && accEditDate.value > max) accEditDate.value = max;
        }
    };
    const isFutureDateTimeValue = (value) => {
        if (!value) return false;
        const ts = new Date(value).getTime();
        if (Number.isNaN(ts)) return false;
        return ts > Date.now();
    };
    const autoCloseDate = (el) => el && el.addEventListener("change", () => el.value && el.blur());

    const setStatus = (text, isError) => {
        statusText.textContent = text || "";
        statusText.style.color = isError ? "#b91c1c" : "#667873";
    };
    const normalizePopupText = (text, isError) => {
        const raw = String(text || "").trim();
        if (!raw) return isError ? "操作失败，请稍后重试" : "操作成功";
        if (/^error\s*[:：]/i.test(raw)) return raw.replace(/^error\s*[:：]\s*/i, "");
        return raw;
    };
    const showPopup = (text, isError) => {
        if (!text) return;
        if (!resultModal || !resultModalTitle || !resultModalMessage) return;
        const popupText = normalizePopupText(text, isError);
        const dialog = resultModal.querySelector(".result-modal-dialog");
        if (dialog) dialog.classList.toggle("is-error", !!isError);
        resultModalTitle.textContent = isError ? "操作失败" : "操作成功";
        if (resultModalTag) resultModalTag.textContent = isError ? "失败" : "成功";
        resultModalMessage.textContent = popupText;
        resultModal.classList.remove("is-hidden");
    };
    let confirmResolver = null;
    const closeConfirmModal = (result) => {
        if (!confirmModal) return;
        confirmModal.classList.add("is-hidden");
        if (confirmResolver) {
            const resolver = confirmResolver;
            confirmResolver = null;
            resolver(!!result);
        }
    };
    const confirmAction = (text) => {
        if (!confirmModal || !confirmModalMessage) return Promise.resolve(false);
        confirmModalMessage.textContent = normalizePopupText(text || "确定要继续此操作吗？", true);
        confirmModal.classList.remove("is-hidden");
        return new Promise((resolve) => {
            confirmResolver = resolve;
        });
    };
    const notifyResult = (text, isError) => {
        setStatus(normalizePopupText(text, isError), isError);
        showPopup(text, isError);
    };
    const getErrorMessage = (err, fallback) => {
        const dft = fallback || "操作失败";
        if (!err) return dft;
        if (typeof err === "string" && err.trim()) return err.trim();
        if (err.message && String(err.message).trim()) return String(err.message).trim();
        if (err.error && String(err.error).trim()) return String(err.error).trim();
        return dft;
    };

    const requestCache = new Map();
    const cachedGet = async (url, ttlMs) => {
        const key = `GET:${url}`;
        const now = Date.now();
        const cached = requestCache.get(key);
        if (cached && cached.value !== undefined && cached.expireAt > now) return cached.value;
        if (cached && cached.promise) return cached.promise;
        const promise = api.request(url, { method: "GET" }).then((value) => {
            requestCache.set(key, { value, expireAt: Date.now() + (ttlMs || 1000), promise: null });
            return value;
        }).catch((err) => {
            requestCache.delete(key);
            throw err;
        });
        requestCache.set(key, { value: undefined, expireAt: 0, promise });
        return promise;
    };
    const invalidateCache = (prefixes) => {
        if (!prefixes) {
            requestCache.clear();
            return;
        }
        const list = Array.isArray(prefixes) ? prefixes : [prefixes];
        Array.from(requestCache.keys()).forEach((key) => {
            if (list.some((prefix) => key.includes(prefix))) requestCache.delete(key);
        });
    };

    const drawSparkline = (canvas, series, color) => {
        if (!canvas) return;
        const rect = canvas.getBoundingClientRect();
        const width = Math.max(160, Math.floor(rect.width || 240));
        const values = (series && series.values) || [];
        const itemCount = Math.max(1, values.length);
        const rowHeight = 9;
        const gap = 3;
        const top = 8;
        const bottomPadding = 10;
        const dynamicHeight = top + bottomPadding + itemCount * rowHeight + Math.max(0, itemCount - 1) * gap;
        const height = Math.min(420, Math.max(240, dynamicHeight));
        canvas.width = width;
        canvas.height = height;
        const ctx2d = canvas.getContext("2d");
        if (!ctx2d) return;
        ctx2d.clearRect(0, 0, width, height);
        if (!series || !series.values || !series.values.length) return;
        const labels = series.labels || [];
        const max = Math.max(...values, 1);
        const left = 46;
        const amountArea = 76;
        const right = width - amountArea;
        const bottom = height - 8;
        const chartHeight = bottom - top;
        const fitRowHeight = (chartHeight - gap * (values.length - 1)) / values.length;
        ctx2d.font = "11px sans-serif";
        ctx2d.textBaseline = "middle";
        values.forEach((v, i) => {
            const y = top + i * (fitRowHeight + gap);
            const label = labels[i] || "--";
            ctx2d.fillStyle = "#6b7f79";
            ctx2d.fillText(label, 4, y + fitRowHeight / 2);
            ctx2d.fillStyle = "rgba(15, 118, 110, 0.12)";
            ctx2d.fillRect(left, y, right - left, fitRowHeight);
            const w = Math.max(2, Math.round((v / max) * (right - left)));
            ctx2d.fillStyle = color;
            ctx2d.fillRect(left, y, w, fitRowHeight);
            ctx2d.fillStyle = "#425752";
            ctx2d.fillText(`￥${Number(v || 0).toFixed(2)}`, right + 6, y + fitRowHeight / 2);
        });
    };
    const buildCurrentMonthSeries = (list, type) => {
        const now = new Date();
        const year = now.getFullYear();
        const month = now.getMonth() + 1;
        const map = {};
        const today = now.getDate();
        for (let day = 1; day <= today; day += 1) map[`${year}-${pad2(month)}-${pad2(day)}`] = 0;
        list.forEach((item) => {
            if (item.type !== type) return;
            const raw = String(item.accountDate || "").slice(0, 10);
            if (!/^\d{4}-\d{2}-\d{2}$/.test(raw)) return;
            const d = new Date(`${raw}T00:00:00`);
            if (Number.isNaN(d.getTime())) return;
            if (d.getFullYear() !== year || d.getMonth() + 1 !== month) return;
            map[raw] = (map[raw] || 0) + Number(item.amount || 0);
        });
        const keys = Object.keys(map).sort();
        return { labels: keys.map((k) => `${new Date(`${k}T00:00:00`).getMonth() + 1}/${new Date(`${k}T00:00:00`).getDate()}`), values: keys.map((k) => map[k]) };
    };
    const renderFinanceCharts = () => {
        drawSparkline(incomeChart, buildCurrentMonthSeries(state.latestAccountingAllList, "INCOME"), "#0f766e");
        drawSparkline(expenseChart, buildCurrentMonthSeries(state.latestAccountingAllList, "EXPENSE"), "#c2410c");
    };

    const renderPendingSummaryLists = () => {
        const pendingPlans = state.latestPlanList.filter((i) => i.status !== "DONE" && i.status !== "ENDED")
            .sort((a, b) => toSortableDate(a.endDate) - toSortableDate(b.endDate));
        const pendingGoals = state.latestGoalList.filter((i) => i.status !== "DONE" && i.status !== "ACHIEVED")
            .sort((a, b) => toSortableDate(a.deadline) - toSortableDate(b.deadline));

        planPendingListEl.innerHTML = pendingPlans.length ? "" : "<li>暂无待完成规划</li>";
        if (pendingPlans.length) {
            const fragment = document.createDocumentFragment();
            pendingPlans.slice(0, 3).forEach((i) => {
                const li = document.createElement("li");
                li.textContent = `${i.title || "未命名规划"}（截止：${toDisplayDate(i.endDate)}）`;
                li.title = li.textContent;
                fragment.appendChild(li);
            });
            planPendingListEl.appendChild(fragment);
        }

        goalPendingListEl.innerHTML = pendingGoals.length ? "" : "<li>暂无待完成目标</li>";
        if (pendingGoals.length) {
            const fragment = document.createDocumentFragment();
            pendingGoals.slice(0, 3).forEach((i) => {
                const li = document.createElement("li");
                li.textContent = `${i.name || "未命名目标"}（截止：${toDisplayDate(i.deadline)}）`;
                li.title = li.textContent;
                fragment.appendChild(li);
            });
            goalPendingListEl.appendChild(fragment);
        }
    };

    const loadSummary = async () => {
        const d = await cachedGet("/api/dashboard/summary", 1200);
        monthIncomeEl.textContent = fmtAmount(d.monthIncome || 0);
        monthExpenseEl.textContent = fmtAmount(d.monthExpense || 0);
        planDoneEl.textContent = `${d.donePlans} / ${d.totalPlans}`;
        goalDoneEl.textContent = `${d.achievedGoals} / ${d.totalGoals}`;
        planPendingEl.textContent = `未完成 ${Math.max(0, d.totalPlans - d.donePlans)} 项`;
        goalPendingEl.textContent = `未完成 ${Math.max(0, d.totalGoals - d.achievedGoals)} 项`;
    };

    const loadProfile = async () => {
        const p = await cachedGet("/api/account/me", 1500);
        profileUsername.value = p.username || "";
        newUsername.value = p.username || "";
        profileNickname.value = p.nickname || "";
        profileEmail.value = p.email || "";
        try {
            await loadAvatarWithAuth(p.avatarUrl || "");
        } catch (e) {
            releaseAvatarObjectUrl();
            applyAvatarToView("");
        }
        const display = p.nickname || p.username || "用户";
        welcomeUser.textContent = `你好，${display}`;
        summaryName.textContent = display;
        const accountLine = p.email || `@${p.username || ""}`;
        summaryEmail.textContent = accountLine;
        moduleUserNameEls.forEach((el) => { el.textContent = display; });
        moduleUserEmailEls.forEach((el) => { el.textContent = accountLine; });
    };

    const ctx = {
        api,
        $,
        state,
        setStatus,
        notifyResult,
        confirmAction,
        getErrorMessage,
        cachedGet,
        invalidateCache,
        renderFinanceCharts,
        renderPendingSummaryLists,
        actions: { loadSummary },
        utils: {
            pad2,
            nowMonthValue,
            nowYearValue,
            splitMonthKey,
            toDisplayDateTime,
            toDateTimeLocalValue,
            toDisplayDate,
            toTypeLabel,
            toPlanStatusLabel,
            toPlanPriorityLabel,
            toGoalStatusLabel,
            planPriorityRank,
            goalStatusRank,
            fmtAmount,
            isFutureDateTimeValue,
            syncAccountingDateMax
        }
    };

    const accountingModule = window.DashboardModules.createAccountingModule(ctx);
    const plansModule = window.DashboardModules.createPlansModule(ctx);
    const goalsModule = window.DashboardModules.createGoalsModule(ctx);
    const aiModule = window.DashboardModules.createAiModule(ctx);

    let refreshInFlight = null;
    let refreshQueued = false;
    let financeChartResizeRaf = 0;
    const runRefreshAll = async () => {
        setStatus("已更新");
        try {
            await Promise.all([
                loadSummary(),
                accountingModule.loadAccountingList(),
                plansModule.loadPlanList(),
                goalsModule.loadGoalList(),
                loadProfile()
            ]);
            aiModule.renderAiAssistant();
            setStatus("已更新");
        } catch (err) {
            if (err.status === 401) {
                api.clearAuth();
                window.location.href = "/";
                return;
            }
            setStatus(normalizePopupText(getErrorMessage(err), true), true);
        }
    };
    const refreshAll = async () => {
        if (refreshInFlight) {
            refreshQueued = true;
            return refreshInFlight;
        }
        refreshInFlight = (async () => {
            do {
                refreshQueued = false;
                await runRefreshAll();
            } while (refreshQueued);
        })();
        try {
            await refreshInFlight;
        } finally {
            refreshInFlight = null;
        }
        return null;
    };
    ctx.refreshAll = refreshAll;

    const collapsedBranches = new Set(["accounting", "plans", "goals", "aiAssistant"]);
    const isAccountingBranch = (key) => BRANCH_SECTION_KEYS.accounting.has(key);
    const isPlansBranch = (key) => BRANCH_SECTION_KEYS.plans.has(key);
    const isGoalsBranch = (key) => BRANCH_SECTION_KEYS.goals.has(key);
    const isAiBranch = (key) => BRANCH_SECTION_KEYS.aiAssistant.has(key);
    const syncSideMenus = () => {
        if (accountingPopupMenu) accountingPopupMenu.classList.toggle("is-hidden", collapsedBranches.has("accounting"));
        if (plansPopupMenu) plansPopupMenu.classList.toggle("is-hidden", collapsedBranches.has("plans"));
        if (goalsPopupMenu) goalsPopupMenu.classList.toggle("is-hidden", collapsedBranches.has("goals"));
        if (aiPopupMenu) aiPopupMenu.classList.toggle("is-hidden", collapsedBranches.has("aiAssistant"));
        Object.keys(parentMenuLinks).forEach((branch) => {
            const link = parentMenuLinks[branch];
            if (!link) return;
            link.classList.toggle("expanded", !collapsedBranches.has(branch));
        });
    };
    const activateSection = (key) => {
        const accountingBranch = isAccountingBranch(key);
        const plansBranch = isPlansBranch(key);
        const goalsBranch = isGoalsBranch(key);
        const aiBranch = isAiBranch(key);
        state.activeSectionKey = key;
        if (accountingBranch) collapsedBranches.delete("accounting");
        if (plansBranch) collapsedBranches.delete("plans");
        if (goalsBranch) collapsedBranches.delete("goals");
        if (aiBranch) collapsedBranches.delete("aiAssistant");

        menuLinks.forEach((l) => {
            const section = l.dataset.section || "";
            const isParent = l.classList.contains("side-parent-link");
            const active = section === key
                || (isParent && section === "accounting" && accountingBranch)
                || (isParent && section === "plans" && plansBranch)
                || (isParent && section === "goals" && goalsBranch)
                || (isParent && section === "aiAssistant" && aiBranch);
            l.classList.toggle("active", active);
        });
        Object.keys(sections).forEach((k) => sections[k] && sections[k].classList.toggle("is-hidden", k !== (aiBranch ? "aiAssistant" : key)));
        syncSideMenus();

        if (key === "accountingMonthly") accountingModule.renderMonthlyBill();
        if (key === "accountingYearly") accountingModule.renderYearlyBill();
        if (key === "plansMonthly") plansModule.renderPlanMonthlyBill();
        if (key === "plansYearly") plansModule.renderPlanYearlyBill();
        if (key === "goalsMonthly") goalsModule.renderGoalMonthlyBill();
        if (key === "goalsYearly") goalsModule.renderGoalYearlyBill();
        if (aiBranch) {
            aiModule.renderAiAssistant();
            if (key === "aiFinance") aiModule.switchAiTab("finance");
            else if (key === "aiRemind") aiModule.switchAiTab("remind");
            else if (key === "aiConsume") aiModule.switchAiTab("finance");
            else if (key === "aiPlan") aiModule.switchAiTab("remind");
            else aiModule.switchAiTab("chat");
        }
    };

    accountingModule.bindEvents();
    plansModule.bindEvents();
    goalsModule.bindEvents();
    aiModule.bindEvents();

    profileForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        try {
            await api.request("/api/account/me/nickname", { method: "PUT", body: JSON.stringify({ nickname: profileNickname.value.trim() }) });
            invalidateCache("/api/account/me");
            await loadProfile();
            notifyResult("操作成功");
        } catch (err) { notifyResult(getErrorMessage(err), true); }
    });
    avatarUploadBtn.addEventListener("click", async () => {
        if (!avatarFile.files || avatarFile.files.length === 0) return notifyResult("请先选择头像文件", true);
        const formData = new FormData();
        formData.append("file", avatarFile.files[0]);
        try {
            await api.request("/api/account/me/avatar", { method: "POST", body: formData });
            avatarFile.value = "";
            invalidateCache("/api/account/me");
            await loadProfile();
            notifyResult("操作成功");
        } catch (err) { notifyResult(getErrorMessage(err), true); }
    });
    usernameUpdateBtn.addEventListener("click", async () => {
        const username = (newUsername.value || "").trim();
        if (!username) return notifyResult("用户名不能为空", true);
        const confirmed = await confirmAction("修改用户名后将强制退出登录，请重新登录。是否继续？");
        if (!confirmed) return;
        try {
            await api.request("/api/account/me/username", { method: "PUT", body: JSON.stringify({ username }) });
            notifyResult("用户名修改成功，系统将退出当前登录状态，请重新登录");
            releaseAvatarObjectUrl();
            api.clearAuth();
            setTimeout(() => window.location.href = "/", 1200);
        } catch (err) { notifyResult(getErrorMessage(err), true); }
    });
    emailUpdateBtn.addEventListener("click", async () => {
        const email = (profileEmail.value || "").trim();
        if (!email) return notifyResult("邮箱不能为空", true);
        try {
            await api.request("/api/account/me/email", { method: "PUT", body: JSON.stringify({ email }) });
            invalidateCache("/api/account/me");
            notifyResult("操作成功");
        } catch (err) { notifyResult(getErrorMessage(err), true); }
    });
    passwordForm.addEventListener("submit", async (e) => {
        e.preventDefault();
        if (newPassword.value !== confirmNewPassword.value) return notifyResult("两次输入的新密码不一致", true);
        try {
            await api.request("/api/account/me/password", { method: "PUT", body: JSON.stringify({ oldPassword: oldPassword.value, newPassword: newPassword.value }) });
            passwordForm.reset();
            notifyResult("操作成功");
        } catch (err) { notifyResult(getErrorMessage(err), true); }
    });

    refreshBtn.addEventListener("click", refreshAll);
    moduleRefreshBtns.forEach((btn) => btn.addEventListener("click", refreshAll));
    window.addEventListener("resize", () => {
        if (financeChartResizeRaf) return;
        financeChartResizeRaf = window.requestAnimationFrame(() => {
            financeChartResizeRaf = 0;
            renderFinanceCharts();
        });
    });
    logoutLink.addEventListener("click", (e) => {
        e.preventDefault();
        releaseAvatarObjectUrl();
        api.clearAuth();
        window.location.href = "/";
    });
    if (resultModal) {
        const closeResultModal = () => resultModal.classList.add("is-hidden");
        if (resultModalConfirmBtn) resultModalConfirmBtn.addEventListener("click", closeResultModal);
        Array.from(document.querySelectorAll("[data-result-modal-close='true']")).forEach((el) => el.addEventListener("click", closeResultModal));
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && !resultModal.classList.contains("is-hidden")) closeResultModal();
        });
    }
    if (confirmModal) {
        if (confirmModalCancelBtn) confirmModalCancelBtn.addEventListener("click", () => closeConfirmModal(false));
        if (confirmModalOkBtn) confirmModalOkBtn.addEventListener("click", () => closeConfirmModal(true));
        Array.from(document.querySelectorAll("[data-confirm-modal-close='true']")).forEach((el) => el.addEventListener("click", () => closeConfirmModal(false)));
        document.addEventListener("keydown", (e) => {
            if (e.key === "Escape" && !confirmModal.classList.contains("is-hidden")) closeConfirmModal(false);
        });
    }
    menuLinks.forEach((link) => link.addEventListener("click", (e) => {
        e.preventDefault();
        const section = link.dataset.section || "summary";
        const isParent = link.classList.contains("side-parent-link");
        if (!isParent) {
            activateSection(section);
            return;
        }
        if (!collapsedBranches.has(section)) {
            collapsedBranches.add(section);
            syncSideMenus();
            return;
        }
        collapsedBranches.delete(section);
        syncSideMenus();
    }));

    const bindSummaryJump = (card, section) => {
        if (!card) return;
        const go = () => activateSection(section);
        card.addEventListener("click", go);
        card.addEventListener("keydown", (e) => {
            if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                go();
            }
        });
    };
    bindSummaryJump(planSummaryCard, "plans");
    bindSummaryJump(goalSummaryCard, "goals");

    [accDate, accEditDate, planStartDate, planEndDate, planFilterDeadline, planEditStartDate, planEditEndDate, goalStartDate, goalDeadline, goalFilterDeadline, goalEditStartDate, goalEditDeadline].forEach(autoCloseDate);
    state.currentMonthlyBillMonth = nowMonthValue();
    state.currentYearlyBillYear = nowYearValue();
    state.currentPlanMonthlyMonth = nowMonthValue();
    state.currentPlanYearlyYear = nowYearValue();
    state.currentGoalMonthlyMonth = nowMonthValue();
    state.currentGoalYearlyYear = nowYearValue();
    accountingModule.syncMonthlyPicker();
    accountingModule.syncYearlyPicker();
    plansModule.syncPlanMonthlyPicker();
    plansModule.syncPlanYearlyPicker();
    goalsModule.syncGoalMonthlyPicker();
    goalsModule.syncGoalYearlyPicker();
    syncAccountingDateMax();
    setInterval(syncAccountingDateMax, 30000);
    welcomeUser.textContent = `你好，${api.getUsername() || "用户"}`;
    activateSection("summary");
    refreshAll();
})();
