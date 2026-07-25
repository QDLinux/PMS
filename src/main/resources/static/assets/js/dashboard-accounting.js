(function () {
    const registry = window.DashboardModules || (window.DashboardModules = {});

    registry.createAccountingModule = function createAccountingModule(ctx) {
        const state = ctx.state;
        const api = ctx.api;
        const $ = ctx.$;
        const {
            toDisplayDateTime,
            toDateTimeLocalValue,
            toTypeLabel,
            fmtAmount,
            pad2,
            nowMonthValue,
            nowYearValue,
            splitMonthKey,
            isFutureDateTimeValue,
            syncAccountingDateMax
        } = ctx.utils;

        const accountingForm = $("accountingForm");
        const accountingTableBody = $("accountingTableBody");
        const accountingTotalIncomeEl = $("accountingTotalIncome");
        const accountingTotalExpenseEl = $("accountingTotalExpense");
        const monthlyBillYearSel = $("monthlyBillYearSel");
        const monthlyBillMonthSel = $("monthlyBillMonthSel");
        const monthlyBillResetBtn = $("monthlyBillResetBtn");
        const monthlyBillIncome = $("monthlyBillIncome");
        const monthlyBillExpense = $("monthlyBillExpense");
        const monthlyBillBalance = $("monthlyBillBalance");
        const monthlyBillCount = $("monthlyBillCount");
        const monthlyBillTableBody = $("monthlyBillTableBody");
        const yearlyBillYearSel = $("yearlyBillYearSel");
        const yearlyBillResetBtn = $("yearlyBillResetBtn");
        const yearlyBillIncome = $("yearlyBillIncome");
        const yearlyBillExpense = $("yearlyBillExpense");
        const yearlyBillBalance = $("yearlyBillBalance");
        const yearlyBillCount = $("yearlyBillCount");
        const yearlyBillTableBody = $("yearlyBillTableBody");
        const accFilterStart = $("accFilterStart");
        const accFilterEnd = $("accFilterEnd");
        const accFilterType = $("accFilterType");
        const accFilterBtn = $("accFilterBtn");
        const accFilterResetBtn = $("accFilterResetBtn");
        const accDate = $("accDate");
        const accEditModal = $("accEditModal");
        const accEditForm = $("accEditForm");
        const accEditId = $("accEditId");
        const accEditType = $("accEditType");
        const accEditAmount = $("accEditAmount");
        const accEditCategory = $("accEditCategory");
        const accEditDate = $("accEditDate");
        const accEditNote = $("accEditNote");
        const accEditDeleteBtn = $("accEditDeleteBtn");
        const accEditCancelBtn = $("accEditCancelBtn");
        const accDetailModal = $("accDetailModal");
        const accDetailId = $("accDetailId");
        const accDetailDate = $("accDetailDate");
        const accDetailType = $("accDetailType");
        const accDetailCategory = $("accDetailCategory");
        const accDetailAmount = $("accDetailAmount");
        const accDetailNote = $("accDetailNote");
        const accDetailCloseBtn = $("accDetailCloseBtn");

        const applyAccountingFilter = (list) => {
            const s = (state.currentAccountingFilter.startDate || "").slice(0, 10);
            const e = (state.currentAccountingFilter.endDate || "").slice(0, 10);
            const t = (state.currentAccountingFilter.type || "").trim();
            return (list || []).filter((item) => {
                const d = String(item.accountDate || "").slice(0, 10);
                if (s && d < s) return false;
                if (e && d > e) return false;
                if (t && item.type !== t) return false;
                return true;
            });
        };

        const renderAccountingModuleStats = () => {
            if (!accountingTotalIncomeEl || !accountingTotalExpenseEl) return;
            const income = state.latestAccountingAllList.filter((i) => i.type === "INCOME").reduce((sum, i) => sum + Number(i.amount || 0), 0);
            const expense = state.latestAccountingAllList.filter((i) => i.type === "EXPENSE").reduce((sum, i) => sum + Number(i.amount || 0), 0);
            accountingTotalIncomeEl.textContent = fmtAmount(income);
            accountingTotalExpenseEl.textContent = fmtAmount(expense);
        };

        const renderBillTable = (tbodyEl, list) => {
            if (!tbodyEl) return;
            tbodyEl.innerHTML = "";
            if (!list.length) {
                tbodyEl.innerHTML = "<tr><td colspan='6'>暂无数据</td></tr>";
                return;
            }
            const fragment = document.createDocumentFragment();
            list.forEach((item, idx) => {
                const tr = document.createElement("tr");
                tr.innerHTML = `<td>${idx + 1}</td><td>${toDisplayDateTime(item.accountDate)}</td><td>${toTypeLabel(item.type)}</td><td>${item.category || "-"}</td><td><span class="${item.type === "INCOME" ? "amount-income" : "amount-expense"}">${item.type === "INCOME" ? "+" : "-"}${Number(item.amount || 0).toFixed(2)}</span></td><td>${item.note || "-"}</td>`;
                fragment.appendChild(tr);
            });
            tbodyEl.appendChild(fragment);
        };

        const syncMonthlyPicker = () => {
            if (!monthlyBillYearSel || !monthlyBillMonthSel) return;
            const records = state.latestAccountingAllList;
            const yearSet = new Set();
            const monthSet = new Set();
            records.forEach((i) => {
                const d = String(i.accountDate || "");
                const y = d.slice(0, 4);
                const m = d.slice(5, 7);
                if (/^\d{4}$/.test(y)) yearSet.add(y);
                if (/^\d{4}$/.test(y) && /^(0[1-9]|1[0-2])$/.test(m)) monthSet.add(`${y}-${m}`);
            });
            const currentYear = nowYearValue();
            const numericYears = Array.from(yearSet).map((y) => Number(y)).filter((n) => Number.isFinite(n));
            const minYear = numericYears.length ? Math.min(...numericYears, Number(currentYear)) : Number(currentYear);
            const maxYear = numericYears.length ? Math.max(...numericYears, Number(currentYear)) : Number(currentYear);
            const years = [];
            for (let y = maxYear; y >= minYear; y -= 1) years.push(String(y));
            const selected = splitMonthKey(state.currentMonthlyBillMonth || nowMonthValue());
            const firstEnabledYear = years.find((y) => yearSet.has(y));
            const nextYear = yearSet.has(selected.year) ? selected.year : (firstEnabledYear || selected.year);
            monthlyBillYearSel.innerHTML = years.map((y) => `<option value="${y}" ${yearSet.has(y) ? "" : "disabled"}>${y}</option>`).join("");
            monthlyBillYearSel.value = nextYear;

            const months = Array.from({ length: 12 }, (_, i) => pad2(i + 1));
            const firstEnabledMonth = months.find((m) => monthSet.has(`${nextYear}-${m}`));
            const nextMonth = monthSet.has(`${nextYear}-${selected.month}`) ? selected.month : (firstEnabledMonth || selected.month);
            monthlyBillMonthSel.innerHTML = months.map((m) => `<option value="${m}" ${monthSet.has(`${nextYear}-${m}`) ? "" : "disabled"}>${m}</option>`).join("");
            monthlyBillMonthSel.value = nextMonth;
            state.currentMonthlyBillMonth = `${nextYear}-${nextMonth}`;
        };

        const syncYearlyPicker = () => {
            if (!yearlyBillYearSel) return;
            const yearSet = new Set(state.latestAccountingAllList.map((i) => String(i.accountDate || "").slice(0, 4)).filter((v) => /^\d{4}$/.test(v)));
            const currentYear = nowYearValue();
            const numericYears = Array.from(yearSet).map((y) => Number(y)).filter((n) => Number.isFinite(n));
            const minYear = numericYears.length ? Math.min(...numericYears, Number(currentYear)) : Number(currentYear);
            const maxYear = numericYears.length ? Math.max(...numericYears, Number(currentYear)) : Number(currentYear);
            const years = [];
            for (let y = maxYear; y >= minYear; y -= 1) years.push(String(y));
            const selectedYear = /^\d{4}$/.test(state.currentYearlyBillYear || "") ? state.currentYearlyBillYear : currentYear;
            const firstEnabledYear = years.find((y) => yearSet.has(y));
            const nextYear = yearSet.has(selectedYear) ? selectedYear : (firstEnabledYear || selectedYear);
            yearlyBillYearSel.innerHTML = years.map((y) => `<option value="${y}" ${yearSet.has(y) ? "" : "disabled"}>${y}</option>`).join("");
            yearlyBillYearSel.value = nextYear;
            state.currentYearlyBillYear = nextYear;
        };

        const renderMonthlyBill = () => {
            if (!monthlyBillTableBody) return;
            const month = state.currentMonthlyBillMonth || nowMonthValue();
            const list = state.latestAccountingAllList.filter((i) => String(i.accountDate || "").slice(0, 7) === month)
                .sort((a, b) => new Date(b.accountDate || 0).getTime() - new Date(a.accountDate || 0).getTime());
            const income = list.filter((i) => i.type === "INCOME").reduce((s, i) => s + Number(i.amount || 0), 0);
            const expense = list.filter((i) => i.type === "EXPENSE").reduce((s, i) => s + Number(i.amount || 0), 0);
            if (monthlyBillIncome) monthlyBillIncome.textContent = fmtAmount(income);
            if (monthlyBillExpense) monthlyBillExpense.textContent = fmtAmount(expense);
            if (monthlyBillBalance) monthlyBillBalance.textContent = fmtAmount(income - expense);
            if (monthlyBillCount) monthlyBillCount.textContent = String(list.length);
            renderBillTable(monthlyBillTableBody, list);
        };

        const renderYearlyBill = () => {
            if (!yearlyBillTableBody) return;
            const year = state.currentYearlyBillYear || nowYearValue();
            const list = state.latestAccountingAllList.filter((i) => String(i.accountDate || "").slice(0, 4) === year)
                .sort((a, b) => new Date(b.accountDate || 0).getTime() - new Date(a.accountDate || 0).getTime());
            const income = list.filter((i) => i.type === "INCOME").reduce((s, i) => s + Number(i.amount || 0), 0);
            const expense = list.filter((i) => i.type === "EXPENSE").reduce((s, i) => s + Number(i.amount || 0), 0);
            if (yearlyBillIncome) yearlyBillIncome.textContent = fmtAmount(income);
            if (yearlyBillExpense) yearlyBillExpense.textContent = fmtAmount(expense);
            if (yearlyBillBalance) yearlyBillBalance.textContent = fmtAmount(income - expense);
            if (yearlyBillCount) yearlyBillCount.textContent = String(list.length);
            renderBillTable(yearlyBillTableBody, list);
        };

        const openAccEditModal = (r) => {
            syncAccountingDateMax();
            accEditId.value = String(r.id);
            accEditType.value = r.type || "EXPENSE";
            accEditAmount.value = Number(r.amount || 0);
            accEditCategory.value = r.category || "";
            accEditDate.value = toDateTimeLocalValue(r.accountDate);
            accEditNote.value = r.note || "";
            accEditModal.classList.remove("is-hidden");
        };
        const closeAccEditModal = () => {
            accEditForm.reset();
            accEditId.value = "";
            accEditModal.classList.add("is-hidden");
        };
        const openAccDetailModal = (r) => {
            accDetailId.textContent = String(r.displayNo || "-");
            accDetailDate.textContent = toDisplayDateTime(r.accountDate);
            accDetailType.textContent = toTypeLabel(r.type);
            accDetailCategory.textContent = r.category || "-";
            accDetailAmount.textContent = (r.type === "INCOME" ? "+" : "-") + Number(r.amount || 0).toFixed(2);
            accDetailAmount.className = r.type === "INCOME" ? "amount-income" : "amount-expense";
            accDetailNote.textContent = r.note || "-";
            accDetailModal.classList.remove("is-hidden");
        };
        const closeAccDetailModal = () => accDetailModal.classList.add("is-hidden");

        const loadAccountingList = async () => {
            state.latestAccountingAllList = await ctx.cachedGet("/api/accounting", 1200) || [];
            const filteredList = applyAccountingFilter(state.latestAccountingAllList);
            state.latestAccountingList = filteredList.map((i, idx) => ({ ...i, displayNo: idx + 1 }));
            state.latestAccountingById = new Map(state.latestAccountingList.map((item) => [Number(item.id), item]));
            renderAccountingModuleStats();
            syncMonthlyPicker();
            syncYearlyPicker();
            renderMonthlyBill();
            renderYearlyBill();
            accountingTableBody.innerHTML = "";
            if (!state.latestAccountingList.length) {
                accountingTableBody.innerHTML = "<tr><td colspan='4'>暂无记账记录</td></tr>";
                ctx.renderFinanceCharts();
                return;
            }
            const fragment = document.createDocumentFragment();
            state.latestAccountingList.forEach((i) => {
                const tr = document.createElement("tr");
                tr.innerHTML = `<td>${i.displayNo}</td><td>${toDisplayDateTime(i.accountDate)}</td><td><span class="${i.type === "INCOME" ? "amount-income" : "amount-expense"}">${i.type === "INCOME" ? "+" : "-"}${Number(i.amount || 0).toFixed(2)}</span></td><td><button type='button' class='table-action-btn' data-action='edit' data-id='${i.id}'>编辑</button><button type='button' class='table-action-btn secondary' data-action='detail' data-id='${i.id}'>详情</button></td>`;
                fragment.appendChild(tr);
            });
            accountingTableBody.appendChild(fragment);
            ctx.renderFinanceCharts();
        };

        const bindEvents = () => {
            accountingForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                syncAccountingDateMax();
                if (isFutureDateTimeValue(accDate.value)) return ctx.notifyResult("Date cannot be in the future", true);
                try {
                    await api.request("/api/accounting", {
                        method: "POST",
                        body: JSON.stringify({
                            type: $("accType").value,
                            amount: Number($("accAmount").value),
                            category: $("accCategory").value.trim(),
                            accountDate: accDate.value,
                            note: $("accNote").value.trim()
                        })
                    });
                    ctx.invalidateCache(["/api/accounting", "/api/dashboard/summary"]);
                    accountingForm.reset();
                    await ctx.refreshAll();
                    ctx.notifyResult("Operation completed");
                } catch (err) { ctx.notifyResult(ctx.getErrorMessage(err), true); }
            });

            accEditForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                syncAccountingDateMax();
                if (isFutureDateTimeValue(accEditDate.value)) return ctx.notifyResult("Date cannot be in the future", true);
                try {
                    await api.request(`/api/accounting/${accEditId.value}`, {
                        method: "PUT",
                        body: JSON.stringify({
                            type: accEditType.value,
                            amount: Number(accEditAmount.value),
                            category: accEditCategory.value.trim(),
                            accountDate: accEditDate.value,
                            note: accEditNote.value.trim()
                        })
                    });
                    ctx.invalidateCache(["/api/accounting", "/api/dashboard/summary"]);
                    closeAccEditModal();
                    await ctx.refreshAll();
                    ctx.notifyResult("Operation completed");
                } catch (err) { ctx.notifyResult(ctx.getErrorMessage(err), true); }
            });

            accEditCancelBtn.addEventListener("click", closeAccEditModal);
            accEditDeleteBtn.addEventListener("click", async () => {
                if (!accEditId.value) return;
                const confirmed = await ctx.confirmAction("确认删除这条记账记录吗？");
                if (!confirmed) return;
                try {
                    await api.request(`/api/accounting/${accEditId.value}`, { method: "DELETE" });
                    ctx.invalidateCache(["/api/accounting", "/api/dashboard/summary"]);
                    closeAccEditModal();
                    await ctx.refreshAll();
                    ctx.notifyResult("Operation completed");
                } catch (err) { ctx.notifyResult(ctx.getErrorMessage(err), true); }
            });
            Array.from(document.querySelectorAll("[data-modal-close='true']")).forEach((el) => el.addEventListener("click", closeAccEditModal));
            accDetailCloseBtn.addEventListener("click", closeAccDetailModal);
            Array.from(document.querySelectorAll("[data-detail-modal-close='true']")).forEach((el) => el.addEventListener("click", closeAccDetailModal));

            accFilterBtn.addEventListener("click", async () => {
                state.currentAccountingFilter = { startDate: (accFilterStart.value || "").trim(), endDate: (accFilterEnd.value || "").trim(), type: (accFilterType.value || "").trim() };
                ctx.setStatus("筛选条件已应用");
                await loadAccountingList();
                ctx.setStatus("已更新");
            });
            accFilterResetBtn.addEventListener("click", async () => {
                accFilterStart.value = "";
                accFilterEnd.value = "";
                if (accFilterType) accFilterType.value = "";
                state.currentAccountingFilter = { startDate: "", endDate: "", type: "" };
                await loadAccountingList();
                ctx.setStatus("已更新");
            });

            accountingTableBody.addEventListener("click", (e) => {
                const btn = e.target.closest("button[data-action]");
                if (!btn || !accountingTableBody.contains(btn)) return;
                const target = state.latestAccountingById.get(Number(btn.dataset.id));
                if (!target) return;
                if (btn.dataset.action === "edit") openAccEditModal(target); else openAccDetailModal(target);
            });

            if (monthlyBillYearSel) monthlyBillYearSel.addEventListener("change", () => {
                const year = String(monthlyBillYearSel.value || "").trim();
                const month = /^(0[1-9]|1[0-2])$/.test(String(monthlyBillMonthSel.value || "").trim()) ? String(monthlyBillMonthSel.value).trim() : "01";
                if (!/^\d{4}$/.test(year)) return;
                state.currentMonthlyBillMonth = `${year}-${month}`;
                syncMonthlyPicker();
                renderMonthlyBill();
                ctx.setStatus("已更新");
            });
            if (monthlyBillMonthSel) monthlyBillMonthSel.addEventListener("change", () => {
                const year = String(monthlyBillYearSel.value || "").trim();
                const month = String(monthlyBillMonthSel.value || "").trim();
                if (!/^\d{4}$/.test(year) || !/^(0[1-9]|1[0-2])$/.test(month)) return;
                state.currentMonthlyBillMonth = `${year}-${month}`;
                renderMonthlyBill();
                ctx.setStatus("已更新");
            });
            if (monthlyBillResetBtn) monthlyBillResetBtn.addEventListener("click", () => {
                state.currentMonthlyBillMonth = nowMonthValue();
                syncMonthlyPicker();
                renderMonthlyBill();
                ctx.setStatus("已更新");
            });
            if (yearlyBillYearSel) yearlyBillYearSel.addEventListener("change", () => {
                const year = String(yearlyBillYearSel.value || "").trim();
                if (!/^\d{4}$/.test(year)) return;
                state.currentYearlyBillYear = year;
                renderYearlyBill();
                ctx.setStatus("已更新");
            });
            if (yearlyBillResetBtn) yearlyBillResetBtn.addEventListener("click", () => {
                state.currentYearlyBillYear = nowYearValue();
                syncYearlyPicker();
                renderYearlyBill();
                ctx.setStatus("已更新");
            });
        };

        return {
            bindEvents,
            loadAccountingList,
            renderMonthlyBill,
            renderYearlyBill,
            syncMonthlyPicker,
            syncYearlyPicker
        };
    };
})();
