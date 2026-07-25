(function () {
    const registry = window.DashboardModules || (window.DashboardModules = {});

    registry.createGoalsModule = function createGoalsModule(ctx) {
        const state = ctx.state;
        const api = ctx.api;
        const $ = ctx.$;
        const {
            nowMonthValue,
            nowYearValue,
            splitMonthKey,
            pad2,
            fmtAmount,
            toDisplayDate,
            toGoalStatusLabel,
            goalStatusRank
        } = ctx.utils;

        const goalForm = $("goalForm");
        const goalCardList = $("goalCardList");
        const goalFilterDeadline = $("goalFilterDeadline");
        const goalFilterStatus = $("goalFilterStatus");
        const goalFilterBtn = $("goalFilterBtn");
        const goalFilterResetBtn = $("goalFilterResetBtn");
        const goalEditModal = $("goalEditModal");
        const goalEditForm = $("goalEditForm");
        const goalEditId = $("goalEditId");
        const goalEditName = $("goalEditName");
        const goalEditTargetAmount = $("goalEditTargetAmount");
        const goalEditCurrentAmount = $("goalEditCurrentAmount");
        const goalEditStartDate = $("goalEditStartDate");
        const goalEditDeadline = $("goalEditDeadline");
        const goalEditDescription = $("goalEditDescription");
        const goalEditDeleteBtn = $("goalEditDeleteBtn");
        const goalEditCancelBtn = $("goalEditCancelBtn");
        const goalDetailModal = $("goalDetailModal");
        const goalDetailNo = $("goalDetailNo");
        const goalDetailName = $("goalDetailName");
        const goalDetailStatus = $("goalDetailStatus");
        const goalDetailStartDate = $("goalDetailStartDate");
        const goalDetailDeadline = $("goalDetailDeadline");
        const goalDetailTargetAmount = $("goalDetailTargetAmount");
        const goalDetailCurrentAmount = $("goalDetailCurrentAmount");
        const goalDetailPercent = $("goalDetailPercent");
        const goalDetailDescription = $("goalDetailDescription");
        const goalDetailCloseBtn = $("goalDetailCloseBtn");
        const goalTotalCountEl = $("goalTotalCount");
        const goalPendingCountEl = $("goalPendingCount");
        const goalMonthlyYearSel = $("goalMonthlyYearSel");
        const goalMonthlyMonthSel = $("goalMonthlyMonthSel");
        const goalMonthlyResetBtn = $("goalMonthlyResetBtn");
        const goalMonthlyTotal = $("goalMonthlyTotal");
        const goalMonthlyPending = $("goalMonthlyPending");
        const goalMonthlyDone = $("goalMonthlyDone");
        const goalMonthlyExpired = $("goalMonthlyExpired");
        const goalMonthlyTableBody = $("goalMonthlyTableBody");
        const goalYearlyYearSel = $("goalYearlyYearSel");
        const goalYearlyResetBtn = $("goalYearlyResetBtn");
        const goalYearlyTotal = $("goalYearlyTotal");
        const goalYearlyPending = $("goalYearlyPending");
        const goalYearlyDone = $("goalYearlyDone");
        const goalYearlyExpired = $("goalYearlyExpired");
        const goalYearlyTableBody = $("goalYearlyTableBody");

        const getYearFrom = (v) => String(v || "").slice(0, 4);
        const getMonthFrom = (v) => String(v || "").slice(5, 7);
        const buildYearRange = (yearSet, fallbackYear) => {
            const numericYears = Array.from(yearSet).map((y) => Number(y)).filter((n) => Number.isFinite(n));
            const minYear = numericYears.length ? Math.min(...numericYears, Number(fallbackYear)) : Number(fallbackYear);
            const maxYear = numericYears.length ? Math.max(...numericYears, Number(fallbackYear)) : Number(fallbackYear);
            const years = [];
            for (let y = maxYear; y >= minYear; y -= 1) years.push(String(y));
            return years;
        };
        const goalBeforeDeadline = (i, deadline) => {
            if (!deadline) return true;
            const end = (i.deadline || "").slice(0, 10);
            if (!end) return false;
            return end <= deadline;
        };
        const calcGoalPercent = (i) => {
            const target = Number(i.targetAmount || 0);
            const current = Number(i.currentAmount || 0);
            if (target <= 0) return 0;
            return Math.min(100, Math.round((current / target) * 100));
        };

        const renderGoalModuleStats = () => {
            if (!goalTotalCountEl || !goalPendingCountEl) return;
            const total = state.latestGoalList.length;
            const pending = state.latestGoalList.filter((i) => i.status !== "DONE" && i.status !== "ACHIEVED").length;
            goalTotalCountEl.textContent = String(total);
            goalPendingCountEl.textContent = String(pending);
        };

        const renderGoalBillTable = (tbodyEl, list) => {
            if (!tbodyEl) return;
            tbodyEl.innerHTML = "";
            if (!list.length) {
                tbodyEl.innerHTML = "<tr><td colspan='6'>暂无数据</td></tr>";
                return;
            }
            const fragment = document.createDocumentFragment();
            list.forEach((item, idx) => {
                const tr = document.createElement("tr");
                tr.innerHTML = `<td>${idx + 1}</td><td>${item.name || "-"}</td><td>${toDisplayDate(item.deadline)}</td><td>${fmtAmount(item.targetAmount)}</td><td>${fmtAmount(item.currentAmount)}</td><td>${toGoalStatusLabel(item.status)}</td>`;
                fragment.appendChild(tr);
            });
            tbodyEl.appendChild(fragment);
        };

        const syncGoalMonthlyPicker = () => {
            if (!goalMonthlyYearSel || !goalMonthlyMonthSel) return;
            const yearSet = new Set();
            const monthSet = new Set();
            state.latestGoalList.forEach((i) => {
                const y = getYearFrom(i.deadline);
                const m = getMonthFrom(i.deadline);
                if (/^\d{4}$/.test(y)) yearSet.add(y);
                if (/^\d{4}$/.test(y) && /^(0[1-9]|1[0-2])$/.test(m)) monthSet.add(`${y}-${m}`);
            });
            const selected = splitMonthKey(state.currentGoalMonthlyMonth || nowMonthValue());
            const years = buildYearRange(yearSet, nowYearValue());
            const firstEnabledYear = years.find((y) => yearSet.has(y));
            const nextYear = yearSet.has(selected.year) ? selected.year : (firstEnabledYear || selected.year);
            goalMonthlyYearSel.innerHTML = years.map((y) => `<option value="${y}" ${yearSet.has(y) ? "" : "disabled"}>${y}</option>`).join("");
            goalMonthlyYearSel.value = nextYear;
            const months = Array.from({ length: 12 }, (_, i) => pad2(i + 1));
            const firstEnabledMonth = months.find((m) => monthSet.has(`${nextYear}-${m}`));
            const nextMonth = monthSet.has(`${nextYear}-${selected.month}`) ? selected.month : (firstEnabledMonth || selected.month);
            goalMonthlyMonthSel.innerHTML = months.map((m) => `<option value="${m}" ${monthSet.has(`${nextYear}-${m}`) ? "" : "disabled"}>${m}</option>`).join("");
            goalMonthlyMonthSel.value = nextMonth;
            state.currentGoalMonthlyMonth = `${nextYear}-${nextMonth}`;
        };

        const syncGoalYearlyPicker = () => {
            if (!goalYearlyYearSel) return;
            const yearSet = new Set(state.latestGoalList.map((i) => getYearFrom(i.deadline)).filter((v) => /^\d{4}$/.test(v)));
            const selectedYear = /^\d{4}$/.test(state.currentGoalYearlyYear || "") ? state.currentGoalYearlyYear : nowYearValue();
            const years = buildYearRange(yearSet, nowYearValue());
            const firstEnabledYear = years.find((y) => yearSet.has(y));
            const nextYear = yearSet.has(selectedYear) ? selectedYear : (firstEnabledYear || selectedYear);
            goalYearlyYearSel.innerHTML = years.map((y) => `<option value="${y}" ${yearSet.has(y) ? "" : "disabled"}>${y}</option>`).join("");
            goalYearlyYearSel.value = nextYear;
            state.currentGoalYearlyYear = nextYear;
        };

        const renderGoalMonthlyBill = () => {
            if (!goalMonthlyTableBody) return;
            const ym = state.currentGoalMonthlyMonth || nowMonthValue();
            const list = state.latestGoalList.filter((i) => String(i.deadline || "").slice(0, 7) === ym)
                .sort((a, b) => new Date(b.deadline || 0).getTime() - new Date(a.deadline || 0).getTime());
            if (goalMonthlyTotal) goalMonthlyTotal.textContent = String(list.length);
            if (goalMonthlyPending) goalMonthlyPending.textContent = String(list.filter((i) => i.status !== "DONE" && i.status !== "ACHIEVED").length);
            if (goalMonthlyDone) goalMonthlyDone.textContent = String(list.filter((i) => i.status === "DONE" || i.status === "ACHIEVED").length);
            if (goalMonthlyExpired) goalMonthlyExpired.textContent = String(list.filter((i) => i.status === "ENDED" || i.status === "EXPIRED").length);
            renderGoalBillTable(goalMonthlyTableBody, list);
        };

        const renderGoalYearlyBill = () => {
            if (!goalYearlyTableBody) return;
            const year = state.currentGoalYearlyYear || nowYearValue();
            const list = state.latestGoalList.filter((i) => String(i.deadline || "").slice(0, 4) === year)
                .sort((a, b) => new Date(b.deadline || 0).getTime() - new Date(a.deadline || 0).getTime());
            if (goalYearlyTotal) goalYearlyTotal.textContent = String(list.length);
            if (goalYearlyPending) goalYearlyPending.textContent = String(list.filter((i) => i.status !== "DONE" && i.status !== "ACHIEVED").length);
            if (goalYearlyDone) goalYearlyDone.textContent = String(list.filter((i) => i.status === "DONE" || i.status === "ACHIEVED").length);
            if (goalYearlyExpired) goalYearlyExpired.textContent = String(list.filter((i) => i.status === "ENDED" || i.status === "EXPIRED").length);
            renderGoalBillTable(goalYearlyTableBody, list);
        };

        const openGoalEditModal = (i) => {
            goalEditId.value = String(i.id);
            goalEditName.value = i.name || "";
            goalEditTargetAmount.value = Number(i.targetAmount || 0);
            goalEditCurrentAmount.value = Number(i.currentAmount || 0);
            goalEditStartDate.value = i.startDate || "";
            goalEditDeadline.value = i.deadline || "";
            goalEditDescription.value = i.description || "";
            goalEditModal.classList.remove("is-hidden");
        };
        const closeGoalEditModal = () => {
            goalEditForm.reset();
            goalEditId.value = "";
            goalEditModal.classList.add("is-hidden");
        };
        const openGoalDetailModal = (i) => {
            goalDetailNo.textContent = String(i.displayNo || "-");
            goalDetailName.textContent = i.name || "-";
            goalDetailStatus.textContent = toGoalStatusLabel(i.status);
            goalDetailStartDate.textContent = toDisplayDate(i.startDate);
            goalDetailDeadline.textContent = toDisplayDate(i.deadline);
            goalDetailTargetAmount.textContent = fmtAmount(i.targetAmount);
            goalDetailCurrentAmount.textContent = fmtAmount(i.currentAmount);
            goalDetailPercent.textContent = `${calcGoalPercent(i)}%`;
            goalDetailDescription.textContent = i.description || "-";
            goalDetailModal.classList.remove("is-hidden");
        };
        const closeGoalDetailModal = () => goalDetailModal.classList.add("is-hidden");

        const updateGoalCurrentAmount = async (input) => {
            const id = Number(input.dataset.id);
            const target = state.latestVisibleGoalById.get(id);
            if (!target) return;

            const prev = Number(target.currentAmount || 0);
            const raw = (input.value || "").trim();
            if (raw === "") { input.value = prev.toFixed(2); return; }

            const next = Number(raw);
            if (!Number.isFinite(next) || next < 0) {
                ctx.notifyResult("Current amount must be a number >= 0", true);
                input.value = prev.toFixed(2);
                return;
            }
            const max = Number(target.targetAmount || 0);
            if (max > 0 && next > max) {
                ctx.notifyResult("Current amount cannot exceed target amount", true);
                input.value = prev.toFixed(2);
                return;
            }
            if (Math.abs(next - prev) < 0.000001) { input.value = next.toFixed(2); return; }

            input.disabled = true;
            try {
                await api.request(`/api/goals/${id}`, {
                    method: "PUT",
                    body: JSON.stringify({
                        name: target.name,
                        description: target.description || "",
                        targetAmount: Number(target.targetAmount || 0),
                        currentAmount: next,
                        startDate: target.startDate || null,
                        deadline: target.deadline || null
                    })
                });
                ctx.invalidateCache(["/api/goals", "/api/dashboard/summary"]);
                ctx.notifyResult("Operation completed");
                await Promise.all([loadGoalList(), ctx.actions.loadSummary()]);
            } catch (err) {
                ctx.notifyResult(ctx.getErrorMessage(err), true);
                input.value = prev.toFixed(2);
            } finally {
                input.disabled = false;
            }
        };

        const loadGoalList = async () => {
            state.latestGoalList = await ctx.cachedGet("/api/goals", 1200) || [];
            renderGoalModuleStats();
            syncGoalMonthlyPicker();
            syncGoalYearlyPicker();
            renderGoalMonthlyBill();
            renderGoalYearlyBill();

            const byCreatedAt = [...state.latestGoalList].sort((a, b) => new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime());
            const noMap = new Map();
            byCreatedAt.forEach((item, idx) => noMap.set(Number(item.id), idx + 1));
            const visible = state.latestGoalList
                .filter((i) => goalBeforeDeadline(i, state.currentGoalFilter.deadline))
                .filter((i) => !state.currentGoalFilter.status || i.status === state.currentGoalFilter.status)
                .sort((a, b) => {
                    const ad = (a.deadline || "9999-12-31").slice(0, 10);
                    const bd = (b.deadline || "9999-12-31").slice(0, 10);
                    if (ad !== bd) return ad.localeCompare(bd);
                    const as = goalStatusRank(a.status);
                    const bs = goalStatusRank(b.status);
                    if (as !== bs) return as - bs;
                    return new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime();
                })
                .map((i) => ({ ...i, displayNo: noMap.get(Number(i.id)) || 0 }));

            state.latestVisibleGoalById = new Map(visible.map((item) => [Number(item.id), item]));
            goalCardList.innerHTML = "";
            if (!visible.length) {
                goalCardList.innerHTML = "<article class='goal-nest-card empty'><p>暂无目标数据</p></article>";
                ctx.renderPendingSummaryLists();
                return;
            }
            const fragment = document.createDocumentFragment();
            visible.forEach((i) => {
                const p = calcGoalPercent(i);
                const article = document.createElement("article");
                const statusClass = String(i.status || "").toLowerCase();
                article.className = "goal-nest-card";
                article.innerHTML = `<div class='goal-nest-head'><b>#${i.displayNo}</b><span>${toGoalStatusLabel(i.status)}</span></div><div class='goal-mini-card ${statusClass}'><h4>${i.name || "-"}</h4><p>${toDisplayDate(i.startDate)} ~ ${toDisplayDate(i.deadline)}</p><div class='bar'><i style='width:${p}%'></i></div></div><div class='goal-card-footer'><div class='goal-complete-line'><span>已完成/总目标：</span><input type='number' step='0.01' min='0' class='goal-current-input' data-goal-current-input='true' data-id='${i.id}' value='${Number(i.currentAmount || 0).toFixed(2)}'><em>/ ¥${Number(i.targetAmount || 0).toFixed(2)}</em></div><div class='goal-nest-actions'><button type='button' class='table-action-btn' data-goal-action='edit' data-id='${i.id}'>编辑</button><button type='button' class='table-action-btn secondary' data-goal-action='detail' data-id='${i.id}'>详情</button></div></div>`;
                fragment.appendChild(article);
            });
            goalCardList.appendChild(fragment);
            ctx.renderPendingSummaryLists();
        };

        const bindEvents = () => {
            goalForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                try {
                    await api.request("/api/goals", {
                        method: "POST",
                        body: JSON.stringify({
                            name: $("goalName").value.trim(),
                            description: $("goalDescription").value.trim(),
                            targetAmount: Number($("goalTargetAmount").value),
                            currentAmount: Number($("goalCurrentAmount").value),
                            startDate: $("goalStartDate").value || null,
                            deadline: $("goalDeadline").value || null
                        })
                    });
                    ctx.invalidateCache(["/api/goals", "/api/dashboard/summary"]);
                    goalForm.reset();
                    await ctx.refreshAll();
                    ctx.notifyResult("Operation completed");
                } catch (err) { ctx.notifyResult(ctx.getErrorMessage(err), true); }
            });

            goalEditForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                try {
                    await api.request(`/api/goals/${goalEditId.value}`, {
                        method: "PUT",
                        body: JSON.stringify({
                            name: goalEditName.value.trim(),
                            description: goalEditDescription.value.trim(),
                            targetAmount: Number(goalEditTargetAmount.value),
                            currentAmount: Number(goalEditCurrentAmount.value),
                            startDate: goalEditStartDate.value || null,
                            deadline: goalEditDeadline.value || null
                        })
                    });
                    ctx.invalidateCache(["/api/goals", "/api/dashboard/summary"]);
                    closeGoalEditModal();
                    await ctx.refreshAll();
                    ctx.notifyResult("Operation completed");
                } catch (err) { ctx.notifyResult(ctx.getErrorMessage(err), true); }
            });

            goalEditCancelBtn.addEventListener("click", closeGoalEditModal);
            goalEditDeleteBtn.addEventListener("click", async () => {
                if (!goalEditId.value) return;
                const confirmed = await ctx.confirmAction("确认删除当前目标吗？");
                if (!confirmed) return;
                try {
                    await api.request(`/api/goals/${goalEditId.value}`, { method: "DELETE" });
                    ctx.invalidateCache(["/api/goals", "/api/dashboard/summary"]);
                    closeGoalEditModal();
                    await ctx.refreshAll();
                    ctx.notifyResult("Operation completed");
                } catch (err) { ctx.notifyResult(ctx.getErrorMessage(err), true); }
            });
            Array.from(document.querySelectorAll("[data-goal-modal-close='true']")).forEach((el) => el.addEventListener("click", closeGoalEditModal));
            goalDetailCloseBtn.addEventListener("click", closeGoalDetailModal);
            Array.from(document.querySelectorAll("[data-goal-detail-modal-close='true']")).forEach((el) => el.addEventListener("click", closeGoalDetailModal));

            goalFilterBtn.addEventListener("click", async () => {
                state.currentGoalFilter = { deadline: (goalFilterDeadline.value || "").trim(), status: (goalFilterStatus.value || "").trim() };
                ctx.setStatus("筛选条件已应用");
                await loadGoalList();
                ctx.setStatus("已更新");
            });
            goalFilterResetBtn.addEventListener("click", async () => {
                goalFilterDeadline.value = "";
                goalFilterStatus.value = "";
                state.currentGoalFilter = { deadline: "", status: "" };
                await loadGoalList();
                ctx.setStatus("已更新");
            });

            goalCardList.addEventListener("click", (e) => {
                const btn = e.target.closest("button[data-goal-action]");
                if (!btn || !goalCardList.contains(btn)) return;
                const target = state.latestVisibleGoalById.get(Number(btn.dataset.id));
                if (!target) return;
                if (btn.dataset.goalAction === "edit") openGoalEditModal(target); else openGoalDetailModal(target);
            });
            goalCardList.addEventListener("keydown", (e) => {
                if (e.key !== "Enter") return;
                const input = e.target.closest("input[data-goal-current-input='true']");
                if (!input || !goalCardList.contains(input)) return;
                e.preventDefault();
                input.blur();
            });
            goalCardList.addEventListener("focusout", async (e) => {
                const input = e.target.closest("input[data-goal-current-input='true']");
                if (!input || !goalCardList.contains(input)) return;
                await updateGoalCurrentAmount(input);
            });

            if (goalMonthlyYearSel) goalMonthlyYearSel.addEventListener("change", () => {
                const year = String(goalMonthlyYearSel.value || "").trim();
                const month = /^(0[1-9]|1[0-2])$/.test(String(goalMonthlyMonthSel.value || "").trim()) ? String(goalMonthlyMonthSel.value).trim() : "01";
                if (!/^\d{4}$/.test(year)) return;
                state.currentGoalMonthlyMonth = `${year}-${month}`;
                syncGoalMonthlyPicker();
                renderGoalMonthlyBill();
                ctx.setStatus("已更新");
            });
            if (goalMonthlyMonthSel) goalMonthlyMonthSel.addEventListener("change", () => {
                const year = String(goalMonthlyYearSel.value || "").trim();
                const month = String(goalMonthlyMonthSel.value || "").trim();
                if (!/^\d{4}$/.test(year) || !/^(0[1-9]|1[0-2])$/.test(month)) return;
                state.currentGoalMonthlyMonth = `${year}-${month}`;
                renderGoalMonthlyBill();
                ctx.setStatus("已更新");
            });
            if (goalMonthlyResetBtn) goalMonthlyResetBtn.addEventListener("click", () => {
                state.currentGoalMonthlyMonth = nowMonthValue();
                syncGoalMonthlyPicker();
                renderGoalMonthlyBill();
                ctx.setStatus("已更新");
            });
            if (goalYearlyYearSel) goalYearlyYearSel.addEventListener("change", () => {
                const year = String(goalYearlyYearSel.value || "").trim();
                if (!/^\d{4}$/.test(year)) return;
                state.currentGoalYearlyYear = year;
                renderGoalYearlyBill();
                ctx.setStatus("已更新");
            });
            if (goalYearlyResetBtn) goalYearlyResetBtn.addEventListener("click", () => {
                state.currentGoalYearlyYear = nowYearValue();
                syncGoalYearlyPicker();
                renderGoalYearlyBill();
                ctx.setStatus("已更新");
            });
        };

        return {
            bindEvents,
            loadGoalList,
            renderGoalMonthlyBill,
            renderGoalYearlyBill,
            syncGoalMonthlyPicker,
            syncGoalYearlyPicker
        };
    };
})();
