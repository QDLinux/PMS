(function () {
    const registry = window.DashboardModules || (window.DashboardModules = {});

    registry.createPlansModule = function createPlansModule(ctx) {
        const state = ctx.state;
        const api = ctx.api;
        const $ = ctx.$;
        const {
            nowMonthValue,
            nowYearValue,
            splitMonthKey,
            pad2,
            toDisplayDate,
            toPlanStatusLabel,
            toPlanPriorityLabel,
            planPriorityRank
        } = ctx.utils;

        const planForm = $("planForm");
        const planCardList = $("planCardList");
        const planFilterDeadline = $("planFilterDeadline");
        const planFilterStatus = $("planFilterStatus");
        const planFilterBtn = $("planFilterBtn");
        const planFilterResetBtn = $("planFilterResetBtn");
        const planStartDate = $("planStartDate");
        const planEndDate = $("planEndDate");
        const planEditModal = $("planEditModal");
        const planEditForm = $("planEditForm");
        const planEditId = $("planEditId");
        const planEditTitle = $("planEditTitle");
        const planEditPriority = $("planEditPriority");
        const planEditStatus = $("planEditStatus");
        const planEditStartDate = $("planEditStartDate");
        const planEditEndDate = $("planEditEndDate");
        const planEditDescription = $("planEditDescription");
        const planEditDeleteBtn = $("planEditDeleteBtn");
        const planEditCancelBtn = $("planEditCancelBtn");
        const planDetailModal = $("planDetailModal");
        const planDetailNo = $("planDetailNo");
        const planDetailTitle = $("planDetailTitle");
        const planDetailStartDate = $("planDetailStartDate");
        const planDetailEndDate = $("planDetailEndDate");
        const planDetailPriority = $("planDetailPriority");
        const planDetailStatus = $("planDetailStatus");
        const planDetailDescription = $("planDetailDescription");
        const planDetailCloseBtn = $("planDetailCloseBtn");
        const planTotalCountEl = $("planTotalCount");
        const planPendingCountEl = $("planPendingCount");
        const planMonthlyYearSel = $("planMonthlyYearSel");
        const planMonthlyMonthSel = $("planMonthlyMonthSel");
        const planMonthlyResetBtn = $("planMonthlyResetBtn");
        const planMonthlyTotal = $("planMonthlyTotal");
        const planMonthlyPending = $("planMonthlyPending");
        const planMonthlyDone = $("planMonthlyDone");
        const planMonthlyEnded = $("planMonthlyEnded");
        const planMonthlyTableBody = $("planMonthlyTableBody");
        const planYearlyYearSel = $("planYearlyYearSel");
        const planYearlyResetBtn = $("planYearlyResetBtn");
        const planYearlyTotal = $("planYearlyTotal");
        const planYearlyPending = $("planYearlyPending");
        const planYearlyDone = $("planYearlyDone");
        const planYearlyEnded = $("planYearlyEnded");
        const planYearlyTableBody = $("planYearlyTableBody");

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
        const planBeforeDeadline = (i, deadline) => {
            if (!deadline) return true;
            const end = (i.endDate || "").slice(0, 10);
            if (!end) return false;
            return end <= deadline;
        };

        const renderPlanModuleStats = () => {
            if (!planTotalCountEl || !planPendingCountEl) return;
            const total = state.latestPlanList.length;
            const pending = state.latestPlanList.filter((i) => i.status !== "DONE" && i.status !== "ENDED").length;
            planTotalCountEl.textContent = String(total);
            planPendingCountEl.textContent = String(pending);
        };

        const renderPlanBillTable = (tbodyEl, list) => {
            if (!tbodyEl) return;
            tbodyEl.innerHTML = "";
            if (!list.length) {
                tbodyEl.innerHTML = "<tr><td colspan='5'>暂无数据</td></tr>";
                return;
            }
            const fragment = document.createDocumentFragment();
            list.forEach((item, idx) => {
                const tr = document.createElement("tr");
                tr.innerHTML = `<td>${idx + 1}</td><td>${item.title || "-"}</td><td>${toDisplayDate(item.endDate)}</td><td>${toPlanPriorityLabel(item.priority)}</td><td>${toPlanStatusLabel(item.status)}</td>`;
                fragment.appendChild(tr);
            });
            tbodyEl.appendChild(fragment);
        };

        const syncPlanMonthlyPicker = () => {
            if (!planMonthlyYearSel || !planMonthlyMonthSel) return;
            const yearSet = new Set();
            const monthSet = new Set();
            state.latestPlanList.forEach((i) => {
                const y = getYearFrom(i.endDate);
                const m = getMonthFrom(i.endDate);
                if (/^\d{4}$/.test(y)) yearSet.add(y);
                if (/^\d{4}$/.test(y) && /^(0[1-9]|1[0-2])$/.test(m)) monthSet.add(`${y}-${m}`);
            });
            const selected = splitMonthKey(state.currentPlanMonthlyMonth || nowMonthValue());
            const years = buildYearRange(yearSet, nowYearValue());
            const firstEnabledYear = years.find((y) => yearSet.has(y));
            const nextYear = yearSet.has(selected.year) ? selected.year : (firstEnabledYear || selected.year);
            planMonthlyYearSel.innerHTML = years.map((y) => `<option value="${y}" ${yearSet.has(y) ? "" : "disabled"}>${y}</option>`).join("");
            planMonthlyYearSel.value = nextYear;
            const months = Array.from({ length: 12 }, (_, i) => pad2(i + 1));
            const firstEnabledMonth = months.find((m) => monthSet.has(`${nextYear}-${m}`));
            const nextMonth = monthSet.has(`${nextYear}-${selected.month}`) ? selected.month : (firstEnabledMonth || selected.month);
            planMonthlyMonthSel.innerHTML = months.map((m) => `<option value="${m}" ${monthSet.has(`${nextYear}-${m}`) ? "" : "disabled"}>${m}</option>`).join("");
            planMonthlyMonthSel.value = nextMonth;
            state.currentPlanMonthlyMonth = `${nextYear}-${nextMonth}`;
        };

        const syncPlanYearlyPicker = () => {
            if (!planYearlyYearSel) return;
            const yearSet = new Set(state.latestPlanList.map((i) => getYearFrom(i.endDate)).filter((v) => /^\d{4}$/.test(v)));
            const selectedYear = /^\d{4}$/.test(state.currentPlanYearlyYear || "") ? state.currentPlanYearlyYear : nowYearValue();
            const years = buildYearRange(yearSet, nowYearValue());
            const firstEnabledYear = years.find((y) => yearSet.has(y));
            const nextYear = yearSet.has(selectedYear) ? selectedYear : (firstEnabledYear || selectedYear);
            planYearlyYearSel.innerHTML = years.map((y) => `<option value="${y}" ${yearSet.has(y) ? "" : "disabled"}>${y}</option>`).join("");
            planYearlyYearSel.value = nextYear;
            state.currentPlanYearlyYear = nextYear;
        };

        const renderPlanMonthlyBill = () => {
            if (!planMonthlyTableBody) return;
            const ym = state.currentPlanMonthlyMonth || nowMonthValue();
            const list = state.latestPlanList.filter((i) => String(i.endDate || "").slice(0, 7) === ym)
                .sort((a, b) => new Date(b.endDate || 0).getTime() - new Date(a.endDate || 0).getTime());
            if (planMonthlyTotal) planMonthlyTotal.textContent = String(list.length);
            if (planMonthlyPending) planMonthlyPending.textContent = String(list.filter((i) => i.status !== "DONE" && i.status !== "ENDED").length);
            if (planMonthlyDone) planMonthlyDone.textContent = String(list.filter((i) => i.status === "DONE").length);
            if (planMonthlyEnded) planMonthlyEnded.textContent = String(list.filter((i) => i.status === "ENDED").length);
            renderPlanBillTable(planMonthlyTableBody, list);
        };

        const renderPlanYearlyBill = () => {
            if (!planYearlyTableBody) return;
            const year = state.currentPlanYearlyYear || nowYearValue();
            const list = state.latestPlanList.filter((i) => String(i.endDate || "").slice(0, 4) === year)
                .sort((a, b) => new Date(b.endDate || 0).getTime() - new Date(a.endDate || 0).getTime());
            if (planYearlyTotal) planYearlyTotal.textContent = String(list.length);
            if (planYearlyPending) planYearlyPending.textContent = String(list.filter((i) => i.status !== "DONE" && i.status !== "ENDED").length);
            if (planYearlyDone) planYearlyDone.textContent = String(list.filter((i) => i.status === "DONE").length);
            if (planYearlyEnded) planYearlyEnded.textContent = String(list.filter((i) => i.status === "ENDED").length);
            renderPlanBillTable(planYearlyTableBody, list);
        };

        const openPlanEditModal = (i) => {
            planEditId.value = String(i.id);
            planEditTitle.value = i.title || "";
            planEditPriority.value = i.priority || "LOW";
            planEditStatus.value = i.status || "TODO";
            planEditStartDate.value = i.startDate || "";
            planEditEndDate.value = i.endDate || "";
            planEditDescription.value = i.description || "";
            planEditModal.classList.remove("is-hidden");
        };
        const closePlanEditModal = () => {
            planEditForm.reset();
            planEditId.value = "";
            planEditModal.classList.add("is-hidden");
        };
        const openPlanDetailModal = (i) => {
            planDetailNo.textContent = String(i.displayNo || "-");
            planDetailTitle.textContent = i.title || "-";
            planDetailStartDate.textContent = toDisplayDate(i.startDate);
            planDetailEndDate.textContent = toDisplayDate(i.endDate);
            planDetailPriority.textContent = toPlanPriorityLabel(i.priority);
            planDetailStatus.textContent = toPlanStatusLabel(i.status);
            planDetailDescription.textContent = i.description || "-";
            planDetailModal.classList.remove("is-hidden");
        };
        const closePlanDetailModal = () => planDetailModal.classList.add("is-hidden");

        const loadPlanList = async () => {
            state.latestPlanList = await ctx.cachedGet("/api/plans", 1200) || [];
            renderPlanModuleStats();
            syncPlanMonthlyPicker();
            syncPlanYearlyPicker();
            renderPlanMonthlyBill();
            renderPlanYearlyBill();

            const byCreatedAt = [...state.latestPlanList].sort((a, b) => new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime());
            const noMap = new Map();
            byCreatedAt.forEach((item, idx) => noMap.set(Number(item.id), idx + 1));

            const visible = state.latestPlanList
                .filter((i) => planBeforeDeadline(i, state.currentPlanFilter.deadline))
                .filter((i) => !state.currentPlanFilter.status || i.status === state.currentPlanFilter.status)
                .sort((a, b) => {
                    const ad = (a.endDate || "9999-12-31").slice(0, 10);
                    const bd = (b.endDate || "9999-12-31").slice(0, 10);
                    if (ad !== bd) return ad.localeCompare(bd);
                    const ap = planPriorityRank(a.priority);
                    const bp = planPriorityRank(b.priority);
                    if (ap !== bp) return ap - bp;
                    return new Date(a.createdAt || 0).getTime() - new Date(b.createdAt || 0).getTime();
                })
                .map((i) => ({ ...i, displayNo: noMap.get(Number(i.id)) || 0 }));

            state.latestVisiblePlanById = new Map(visible.map((item) => [Number(item.id), item]));
            planCardList.innerHTML = "";
            if (!visible.length) {
                planCardList.innerHTML = "<article class='plan-nest-card empty'><p>暂无计划数据</p></article>";
                ctx.renderPendingSummaryLists();
                return;
            }
            const fragment = document.createDocumentFragment();
            visible.forEach((i) => {
                const article = document.createElement("article");
                const priorityClass = (i.priority || "LOW").toLowerCase();
                const planStatusClass = String(i.status || "").toLowerCase();
                article.className = "plan-nest-card";
                article.innerHTML = `<div class='plan-nest-head'><b>#${i.displayNo}</b><span>${toPlanStatusLabel(i.status)}</span></div><div class='plan-mini-card ${priorityClass} ${planStatusClass}'><h4>${i.title || "-"}</h4><p>${toDisplayDate(i.startDate)} ~ ${toDisplayDate(i.endDate)}</p><p>优先级：${toPlanPriorityLabel(i.priority)}</p></div><div class='plan-nest-actions'><button type='button' class='table-action-btn' data-plan-action='edit' data-id='${i.id}'>编辑</button><button type='button' class='table-action-btn secondary' data-plan-action='detail' data-id='${i.id}'>详情</button></div>`;
                fragment.appendChild(article);
            });
            planCardList.appendChild(fragment);
            ctx.renderPendingSummaryLists();
        };

        const bindEvents = () => {
            planForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                try {
                    await api.request("/api/plans", {
                        method: "POST",
                        body: JSON.stringify({
                            title: $("planTitle").value.trim(),
                            description: $("planDescription").value.trim(),
                            startDate: planStartDate.value || null,
                            endDate: planEndDate.value || null,
                            priority: $("planPriority").value,
                            status: $("planStatus").value
                        })
                    });
                    ctx.invalidateCache(["/api/plans", "/api/dashboard/summary"]);
                    planForm.reset();
                    await ctx.refreshAll();
                    ctx.notifyResult("Operation completed");
                } catch (err) { ctx.notifyResult(ctx.getErrorMessage(err), true); }
            });

            planEditForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                try {
                    await api.request(`/api/plans/${planEditId.value}`, {
                        method: "PUT",
                        body: JSON.stringify({
                            title: planEditTitle.value.trim(),
                            description: planEditDescription.value.trim(),
                            startDate: planEditStartDate.value || null,
                            endDate: planEditEndDate.value || null,
                            priority: planEditPriority.value,
                            status: planEditStatus.value
                        })
                    });
                    ctx.invalidateCache(["/api/plans", "/api/dashboard/summary"]);
                    closePlanEditModal();
                    await ctx.refreshAll();
                    ctx.notifyResult("Operation completed");
                } catch (err) { ctx.notifyResult(ctx.getErrorMessage(err), true); }
            });

            planEditCancelBtn.addEventListener("click", closePlanEditModal);
            planEditDeleteBtn.addEventListener("click", async () => {
                if (!planEditId.value) return;
                const confirmed = await ctx.confirmAction("确认删除当前规划吗？");
                if (!confirmed) return;
                try {
                    await api.request(`/api/plans/${planEditId.value}`, { method: "DELETE" });
                    ctx.invalidateCache(["/api/plans", "/api/dashboard/summary"]);
                    closePlanEditModal();
                    await ctx.refreshAll();
                    ctx.notifyResult("Operation completed");
                } catch (err) { ctx.notifyResult(ctx.getErrorMessage(err), true); }
            });
            Array.from(document.querySelectorAll("[data-plan-modal-close='true']")).forEach((el) => el.addEventListener("click", closePlanEditModal));
            planDetailCloseBtn.addEventListener("click", closePlanDetailModal);
            Array.from(document.querySelectorAll("[data-plan-detail-modal-close='true']")).forEach((el) => el.addEventListener("click", closePlanDetailModal));

            planFilterBtn.addEventListener("click", async () => {
                state.currentPlanFilter = { deadline: (planFilterDeadline.value || "").trim(), status: (planFilterStatus.value || "").trim() };
                ctx.setStatus("筛选条件已应用");
                await loadPlanList();
                ctx.setStatus("已更新");
            });
            planFilterResetBtn.addEventListener("click", async () => {
                planFilterDeadline.value = "";
                planFilterStatus.value = "";
                state.currentPlanFilter = { deadline: "", status: "" };
                await loadPlanList();
                ctx.setStatus("已更新");
            });

            planCardList.addEventListener("click", (e) => {
                const btn = e.target.closest("button[data-plan-action]");
                if (!btn || !planCardList.contains(btn)) return;
                const target = state.latestVisiblePlanById.get(Number(btn.dataset.id));
                if (!target) return;
                if (btn.dataset.planAction === "edit") openPlanEditModal(target); else openPlanDetailModal(target);
            });

            if (planMonthlyYearSel) planMonthlyYearSel.addEventListener("change", () => {
                const year = String(planMonthlyYearSel.value || "").trim();
                const month = /^(0[1-9]|1[0-2])$/.test(String(planMonthlyMonthSel.value || "").trim()) ? String(planMonthlyMonthSel.value).trim() : "01";
                if (!/^\d{4}$/.test(year)) return;
                state.currentPlanMonthlyMonth = `${year}-${month}`;
                syncPlanMonthlyPicker();
                renderPlanMonthlyBill();
                ctx.setStatus("已更新");
            });
            if (planMonthlyMonthSel) planMonthlyMonthSel.addEventListener("change", () => {
                const year = String(planMonthlyYearSel.value || "").trim();
                const month = String(planMonthlyMonthSel.value || "").trim();
                if (!/^\d{4}$/.test(year) || !/^(0[1-9]|1[0-2])$/.test(month)) return;
                state.currentPlanMonthlyMonth = `${year}-${month}`;
                renderPlanMonthlyBill();
                ctx.setStatus("已更新");
            });
            if (planMonthlyResetBtn) planMonthlyResetBtn.addEventListener("click", () => {
                state.currentPlanMonthlyMonth = nowMonthValue();
                syncPlanMonthlyPicker();
                renderPlanMonthlyBill();
                ctx.setStatus("已更新");
            });
            if (planYearlyYearSel) planYearlyYearSel.addEventListener("change", () => {
                const year = String(planYearlyYearSel.value || "").trim();
                if (!/^\d{4}$/.test(year)) return;
                state.currentPlanYearlyYear = year;
                renderPlanYearlyBill();
                ctx.setStatus("已更新");
            });
            if (planYearlyResetBtn) planYearlyResetBtn.addEventListener("click", () => {
                state.currentPlanYearlyYear = nowYearValue();
                syncPlanYearlyPicker();
                renderPlanYearlyBill();
                ctx.setStatus("已更新");
            });
        };

        return {
            bindEvents,
            loadPlanList,
            renderPlanMonthlyBill,
            renderPlanYearlyBill,
            syncPlanMonthlyPicker,
            syncPlanYearlyPicker
        };
    };
})();
