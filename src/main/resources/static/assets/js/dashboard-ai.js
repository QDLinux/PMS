(function () {
    const registry = window.DashboardModules || (window.DashboardModules = {});

    registry.createAiModule = function createAiModule(ctx) {
        const state = ctx.state;
        const api = ctx.api;
        const $ = ctx.$;
        const { nowMonthValue, fmtAmount } = ctx.utils;

        const aiMonthBalance = $("aiMonthBalance");
        const aiPlanPendingCount = $("aiPlanPendingCount");
        const aiGoalOngoingCount = $("aiGoalOngoingCount");
        const aiTabChat = $("aiTabChat");
        const aiTabFinance = $("aiTabFinance");
        const aiTabRemind = $("aiTabRemind");
        const aiFinanceAdviceList = $("aiFinanceAdviceList");
        const aiReminderList = $("aiReminderList");
        const aiChatHistory = $("aiChatHistory");
        const aiChatInput = $("aiChatInput");
        const aiChatSendBtn = $("aiChatSendBtn");
        const aiNewSessionBtn = $("aiNewSessionBtn");
        const aiSessionList = $("aiSessionList");

        const getMonthKey = (v) => {
            const raw = String(v || "").slice(0, 7);
            return /^\d{4}-\d{2}$/.test(raw) ? raw : "";
        };
        const getDateKey = (v) => {
            const raw = String(v || "").slice(0, 10);
            return /^\d{4}-\d{2}-\d{2}$/.test(raw) ? raw : "";
        };
        const daysBetweenToday = (dateValue) => {
            const key = getDateKey(dateValue);
            if (!key) return Number.MAX_SAFE_INTEGER;
            const target = new Date(`${key}T00:00:00`);
            if (Number.isNaN(target.getTime())) return Number.MAX_SAFE_INTEGER;
            const now = new Date();
            const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
            return Math.floor((target.getTime() - today.getTime()) / (24 * 60 * 60 * 1000));
        };
        const escapeHtml = (text) => {
            return String(text || "")
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;")
                .replace(/"/g, "&quot;");
        };
        const renderMarkdownToHtml = (text) => {
            const raw = String(text || "").replace(/\r\n/g, "\n").replace(/\r/g, "\n");
            let html = escapeHtml(raw);

            html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) => {
                return `<pre><code>${code.trim()}</code></pre>`;
            });
            html = html.replace(/`([^`]+)`/g, "<code>$1</code>");
            html = html.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
            html = html.replace(/\*([^*]+)\*/g, "<em>$1</em>");

            const blocks = html.split(/\n{2,}/);
            const result = [];

            for (const block of blocks) {
                const lines = block.split("\n").map((l) => l.trim()).filter(Boolean);
                if (!lines.length) continue;

                if (lines.every((l) => /^[-*]\s+/.test(l))) {
                    result.push("<ul>" + lines.map((l) => `<li>${l.replace(/^[-*]\s+/, "")}</li>`).join("") + "</ul>");
                    continue;
                }

                if (lines.every((l) => /^\d+[.、]\s+/.test(l))) {
                    result.push("<ol>" + lines.map((l) => `<li>${l.replace(/^\d+[.、]\s+/, "")}</li>`).join("") + "</ol>");
                    continue;
                }

                const headingMatch = lines[0].match(/^(#{1,6})\s+(.+)$/);
                if (headingMatch && lines.length === 1) {
                    const level = Math.min(headingMatch[1].length + 2, 6);
                    result.push(`<h${level}>${headingMatch[2]}</h${level}>`);
                    continue;
                }

                const isTable = lines.length >= 2
                    && lines.every((l) => l.includes("|"))
                    && /^[\s|:\-]+$/.test(lines[1]);
                if (isTable) {
                    const parseCells = (line) => line.split("|").slice(1, -1).map((c) => c.trim());
                    const header = parseCells(lines[0]);
                    const aligns = parseCells(lines[1]).map((c) => {
                        if (c.startsWith(":") && c.endsWith(":")) return "center";
                        if (c.endsWith(":")) return "right";
                        return "left";
                    });
                    let tbl = "<table><thead><tr>";
                    header.forEach((h, i) => { tbl += `<th style="text-align:${aligns[i] || "left"}">${h}</th>`; });
                    tbl += "</tr></thead><tbody>";
                    for (let r = 2; r < lines.length; r++) {
                        const cells = parseCells(lines[r]);
                        tbl += "<tr>";
                        cells.forEach((c, j) => { tbl += `<td style="text-align:${aligns[j] || "left"}">${c}</td>`; });
                        tbl += "</tr>";
                    }
                    tbl += "</tbody></table>";
                    result.push(tbl);
                    continue;
                }

                result.push("<p>" + lines.join("<br>") + "</p>");
            }

            return result.join("");
        };
        const normalizeHistoryTextForDisplay = (text) => {
            const raw = String(text || "");
            if (!raw.includes("\n") && raw.includes(" | ")) return raw.replace(/ \| /g, "\n");
            return raw;
        };

        const renderAiList = (el, items, emptyText) => {
            if (!el) return;
            el.innerHTML = "";
            if (!items || !items.length) {
                el.innerHTML = `<li>${emptyText}</li>`;
                return;
            }
            const fragment = document.createDocumentFragment();
            items.forEach((text) => {
                const li = document.createElement("li");
                li.textContent = text;
                fragment.appendChild(li);
            });
            el.appendChild(fragment);
        };

        const buildAiInsights = () => {
            const monthKey = nowMonthValue();
            const thisMonthItems = state.latestAccountingAllList.filter((i) => getMonthKey(i.accountDate) === monthKey);
            const monthIncome = thisMonthItems.filter((i) => i.type === "INCOME").reduce((sum, i) => sum + Number(i.amount || 0), 0);
            const monthExpense = thisMonthItems.filter((i) => i.type === "EXPENSE").reduce((sum, i) => sum + Number(i.amount || 0), 0);
            const monthBalance = monthIncome - monthExpense;
            const today = new Date().getDate();
            const avgDailyExpense = monthExpense > 0 ? monthExpense / Math.max(1, today) : 0;
            const expenseTrendToMonthEnd = avgDailyExpense * 30;

            const expenseByCategory = {};
            thisMonthItems.filter((i) => i.type === "EXPENSE").forEach((i) => {
                const key = (i.category || "未分类").trim() || "未分类";
                expenseByCategory[key] = (expenseByCategory[key] || 0) + Number(i.amount || 0);
            });
            const topCategory = Object.entries(expenseByCategory).sort((a, b) => b[1] - a[1])[0];
            const topCategoryRatio = topCategory && monthExpense > 0 ? topCategory[1] / monthExpense : 0;

            const pendingPlans = state.latestPlanList.filter((i) => i.status !== "DONE" && i.status !== "ENDED");
            const ongoingGoals = state.latestGoalList.filter((i) => i.status === "ONGOING" || i.status === "NOT_STARTED");
            const pendingPlansWithLeftDays = pendingPlans
                .map((i) => ({ ...i, leftDays: daysBetweenToday(i.endDate) }))
                .filter((i) => Number.isFinite(i.leftDays))
                .sort((a, b) => a.leftDays - b.leftDays);
            const ongoingGoalsWithLeftDays = ongoingGoals
                .map((i) => ({ ...i, leftDays: daysBetweenToday(i.deadline) }))
                .filter((i) => Number.isFinite(i.leftDays))
                .sort((a, b) => a.leftDays - b.leftDays);
            const upcomingPlan = pendingPlansWithLeftDays[0];
            const upcomingGoal = ongoingGoalsWithLeftDays[0];
            const highPriorityPending = pendingPlans.filter((i) => i.priority === "HIGH").length;
            const overduePlanCount = pendingPlans.filter((i) => daysBetweenToday(i.endDate) < 0).length;
            const dueSoonPlans = pendingPlansWithLeftDays.filter((i) => i.leftDays >= 0 && i.leftDays <= 7);
            const dueSoonGoals = ongoingGoalsWithLeftDays.filter((i) => i.leftDays >= 0 && i.leftDays <= 14);
            const nearingPlanCount = dueSoonPlans.length;
            const nearingGoalCount = dueSoonGoals.length;
            const overduePlans = pendingPlansWithLeftDays.filter((i) => i.leftDays < 0);
            const lowProgressGoals = dueSoonGoals
                .map((i) => {
                    const targetAmount = Number(i.targetAmount || 0);
                    const currentAmount = Number(i.currentAmount || 0);
                    const progress = targetAmount > 0 ? Math.max(0, Math.min(1, currentAmount / targetAmount)) : 1;
                    return { ...i, targetAmount, currentAmount, progress };
                })
                .filter((i) => i.progress < 0.6);
            const savingsRate = monthIncome > 0 ? monthBalance / monthIncome : null;

            const financeAdvice = [];
            if (!thisMonthItems.length) {
                financeAdvice.push("本月暂无记账数据，建议先补齐固定支出与日常开销，再生成更准确建议。");
            } else {
                financeAdvice.push(`本月收入 ${fmtAmount(monthIncome)}，支出 ${fmtAmount(monthExpense)}，净额 ${fmtAmount(monthBalance)}。`);
                if (monthBalance < 0) financeAdvice.push(`当前赤字 ${fmtAmount(Math.abs(monthBalance))}，建议先下调可选消费预算 10%-20%。`);
                else if (savingsRate !== null && savingsRate < 0.1) financeAdvice.push("结余率偏低（<10%），建议优先控制高频小额支出并提高自动储蓄比例。");
                else if (savingsRate !== null && savingsRate >= 0.35) financeAdvice.push("结余率表现良好（>=35%），可将新增结余分配到目标与应急金。");
                else financeAdvice.push("当前收支较平衡，建议延续 50/30/20 分配并按周复盘预算执行。");

                if (topCategory) {
                    financeAdvice.push(`支出最高分类为「${topCategory[0]}」(${fmtAmount(topCategory[1])})，占总支出约 ${(topCategoryRatio * 100).toFixed(1)}%。`);
                    if (topCategoryRatio >= 0.4) financeAdvice.push(`「${topCategory[0]}」占比偏高，建议拆分子类并设定分类预算上限。`);
                }
                if (monthIncome > 0 && expenseTrendToMonthEnd > monthIncome * 1.05) {
                    financeAdvice.push("按当前支出节奏推算，月底可能超出收入，建议提前冻结非必要支出。");
                }
                if (monthIncome <= 0 && monthExpense > 0) {
                    financeAdvice.push("本月暂无收入记录但有支出，建议核对收入入账时间并预留现金流。");
                }
            }
            financeAdvice.push("可开启收入到账后自动转入储蓄，减少冲动消费影响。");

            const reminders = [];
            if (!pendingPlans.length) reminders.push("当前无待办计划，可新增 1-2 个有明确截止日期的短期计划。");
            else reminders.push(`当前待办计划 ${pendingPlans.length} 项，其中高优先级 ${highPriorityPending} 项，建议先处理高优先级与近截止事项。`);

            if (highPriorityPending >= 3) reminders.push("高优先级事项较多，建议采用“1个核心任务 + 2个辅助任务”的日计划节奏，避免同时推进过多任务。");
            if (overduePlanCount > 0) {
                const overdueNames = overduePlans.slice(0, 2).map((i) => `「${i.title || "-"}」`).join("、");
                reminders.push(`有 ${overduePlanCount} 项计划已逾期${overdueNames ? `（如 ${overdueNames}）` : ""}，建议今天先完成最小闭环或重新排期。`);
            }
            if (upcomingPlan && upcomingPlan.leftDays <= 7) reminders.push(`提醒：计划「${upcomingPlan.title || "-"}」将在 ${Math.max(0, upcomingPlan.leftDays)} 天后到期。`);
            if (upcomingGoal && upcomingGoal.leftDays <= 14) reminders.push(`提醒：目标「${upcomingGoal.name || "-"}」将在 ${Math.max(0, upcomingGoal.leftDays)} 天后截止。`);
            if (nearingPlanCount >= 2) {
                const dueSoonPlanNames = dueSoonPlans.slice(0, 3).map((i) => `「${i.title || "-"}」`).join("、");
                reminders.push(`未来 7 天内有 ${nearingPlanCount} 项计划到期${dueSoonPlanNames ? `（${dueSoonPlanNames}）` : ""}，建议提前锁定每日完成时段。`);
            }
            if (nearingGoalCount >= 2) reminders.push("近期目标截止较集中，建议将目标资金投入与执行动作提前到本周完成。");
            if (lowProgressGoals.length > 0) {
                const focusGoal = lowProgressGoals[0];
                const remainAmount = Math.max(0, focusGoal.targetAmount - focusGoal.currentAmount);
                const remainWeeks = Math.max(1, Math.ceil(Math.max(1, focusGoal.leftDays) / 7));
                const weeklyNeed = remainAmount / remainWeeks;
                reminders.push(`目标「${focusGoal.name || "-"}」当前进度 ${(focusGoal.progress * 100).toFixed(0)}%，按截止日期估算每周需推进约 ${fmtAmount(weeklyNeed)}。`);
            }
            if (monthBalance < 0 && (overduePlanCount > 0 || nearingGoalCount > 0)) reminders.push("当前收支为赤字且近期事项密集，建议本周暂缓非必要支出，优先保障关键计划与目标投入。");
            if (!reminders.length) reminders.push("近期无紧急到期事项，建议保持每周固定复盘。");

            return { monthIncome, monthExpense, monthBalance, pendingPlansCount: pendingPlans.length, ongoingGoalsCount: ongoingGoals.length, financeAdvice, reminders };
        };

        const appendAiChatMessage = (role, text) => {
            if (!aiChatHistory) return;
            const item = document.createElement("div");
            item.className = `ai-chat-item ${role === "user" ? "user" : "assistant"}`;
            if (role === "assistant") {
                item.innerHTML = renderMarkdownToHtml(text);
            } else {
                item.textContent = text;
            }
            aiChatHistory.appendChild(item);
            aiChatHistory.scrollTop = aiChatHistory.scrollHeight;
        };
        const renderAiChat = () => {
            if (!aiChatHistory) return;
            aiChatHistory.innerHTML = "";
            if (!state.aiChatRecords.length) state.aiChatRecords = [{ role: "assistant", text: "你好，我是你的 AI 助手。请直接输入你的问题。" }];
            state.aiChatRecords.forEach((item) => appendAiChatMessage(item.role, item.text));
        };
        const renderAiSessionList = () => {
            if (!aiSessionList) return;
            aiSessionList.innerHTML = "";
            if (!state.aiSessions.length) {
                aiSessionList.innerHTML = "<div class='ai-session-time'>暂无历史对话</div>";
                return;
            }
            const fragment = document.createDocumentFragment();
            state.aiSessions.forEach((s) => {
                const btn = document.createElement("button");
                btn.type = "button";
                btn.className = `ai-session-item${s.sessionId === state.aiCurrentSessionId ? " active" : ""}`;
                btn.dataset.sessionId = s.sessionId;
                btn.innerHTML = `<div class='ai-session-title'>${s.title || "新对话"}</div><div class='ai-session-time'>${s.updatedAt || ""}</div>`;
                fragment.appendChild(btn);
            });
            aiSessionList.appendChild(fragment);
        };

        const loadAiSessions = async () => {
            if (state.aiSessionsLoaded || state.aiSessionsLoading) return;
            state.aiSessionsLoading = true;
            try {
                const list = await ctx.cachedGet("/api/ai/sessions?limit=50", 5000);
                state.aiSessions = Array.isArray(list) ? list.filter((i) => i && i.sessionId) : [];
            } catch (err) {
                state.aiSessions = [];
            } finally {
                state.aiSessionsLoaded = true;
                state.aiSessionsLoading = false;
                if (!state.aiCurrentSessionId && state.aiSessions.length) state.aiCurrentSessionId = state.aiSessions[0].sessionId;
                renderAiSessionList();
            }
        };
        const loadAiHistory = async (sessionId, force) => {
            if (!sessionId || state.aiHistoryLoading) return;
            if (!force && state.aiChatRecords.length) return;
            state.aiHistoryLoading = true;
            try {
                const list = await ctx.cachedGet(`/api/ai/history?sessionId=${encodeURIComponent(sessionId)}&limit=200`, 2500);
                if (Array.isArray(list) && list.length) {
                    state.aiChatRecords = list.map((i) => ({ role: i && i.role === "user" ? "user" : "assistant", text: i && i.text ? normalizeHistoryTextForDisplay(i.text) : "" }))
                        .filter((i) => i.text.trim().length > 0);
                } else state.aiChatRecords = [];
            } catch (err) {
            } finally {
                state.aiHistoryLoading = false;
                renderAiChat();
            }
        };
        const createAiSession = async () => {
            try {
                const s = await api.request("/api/ai/sessions/new", { method: "PUT" });
                if (s && s.sessionId) {
                    state.aiCurrentSessionId = s.sessionId;
                    state.aiChatRecords = [];
                    state.aiSessionsLoaded = false;
                    ctx.invalidateCache("/api/ai/sessions");
                    await loadAiSessions();
                    renderAiChat();
                }
            } catch (err) {
                ctx.notifyResult(ctx.getErrorMessage(err), true);
            }
        };
        const switchAiSession = async (sessionId) => {
            if (!sessionId || state.aiCurrentSessionId === sessionId) return;
            state.aiCurrentSessionId = sessionId;
            state.aiChatRecords = [];
            renderAiSessionList();
            renderAiChat();
            await loadAiHistory(sessionId, true);
        };
        const buildAiChatContext = () => {
            const insights = buildAiInsights();
            return [
                `待办计划数: ${insights.pendingPlansCount}`,
                `进行中目标数: ${insights.ongoingGoalsCount}`,
                `理财建议: ${insights.financeAdvice.join("；")}`,
                `提醒事项: ${insights.reminders.join("；")}`
            ].join("\n");
        };
        const askAiAssistant = async (text) => {
            const message = String(text || "").trim();
            if (!message || state.aiChatSending) return;
            if (!state.aiCurrentSessionId) {
                await createAiSession();
                if (!state.aiCurrentSessionId) return;
            }
            state.aiChatSending = true;
            if (aiChatSendBtn) {
                aiChatSendBtn.disabled = true;
                aiChatSendBtn.textContent = "发送中...";
            }
            state.aiChatRecords.push({ role: "user", text: message });
            renderAiChat();
            if (aiChatInput) aiChatInput.value = "";
            try {
                const res = await api.request("/api/ai/chat", {
                    method: "POST",
                    body: JSON.stringify({ sessionId: state.aiCurrentSessionId, message, context: buildAiChatContext() })
                });
                if (res && res.sessionId) state.aiCurrentSessionId = res.sessionId;
                state.aiChatRecords.push({ role: "assistant", text: (res && res.answer) || "AI 暂无回复，请稍后重试。" });
                state.aiSessionsLoaded = false;
                ctx.invalidateCache(["/api/ai/sessions", "/api/ai/history"]);
                await loadAiSessions();
            } catch (err) {
                state.aiChatRecords.push({ role: "assistant", text: `AI 调用失败：${ctx.getErrorMessage(err)}` });
            } finally {
                state.aiChatSending = false;
                if (aiChatSendBtn) {
                    aiChatSendBtn.disabled = false;
                    aiChatSendBtn.textContent = "发送";
                }
                renderAiChat();
            }
        };

        const switchAiTab = (key) => {
            const normalized = key === "consume" ? "finance" : (key === "plan" ? "remind" : key);
            state.aiActiveTab = normalized;
            if (aiTabChat) aiTabChat.classList.toggle("is-hidden", normalized !== "chat");
            if (aiTabFinance) aiTabFinance.classList.toggle("is-hidden", normalized !== "finance");
            if (aiTabRemind) aiTabRemind.classList.toggle("is-hidden", normalized !== "remind");
        };

        const renderAiAssistant = () => {
            const insights = buildAiInsights();
            if (aiMonthBalance) aiMonthBalance.textContent = fmtAmount(insights.monthBalance);
            if (aiPlanPendingCount) aiPlanPendingCount.textContent = String(insights.pendingPlansCount);
            if (aiGoalOngoingCount) aiGoalOngoingCount.textContent = String(insights.ongoingGoalsCount);
            renderAiList(aiFinanceAdviceList, insights.financeAdvice, "暂无理财建议");
            renderAiList(aiReminderList, insights.reminders, "暂无提醒事项");
            renderAiChat();
            loadAiSessions().then(async () => {
                if (!state.aiCurrentSessionId) await createAiSession();
                else await loadAiHistory(state.aiCurrentSessionId, true);
            });
            switchAiTab(state.aiActiveTab || "chat");
        };

        const bindEvents = () => {
            if (aiChatSendBtn) {
                aiChatSendBtn.addEventListener("click", async () => {
                    const text = (aiChatInput && aiChatInput.value || "").trim();
                    if (!text) return;
                    await askAiAssistant(text);
                });
            }
            if (aiChatInput) {
                aiChatInput.addEventListener("keydown", (e) => {
                    if (e.key === "Enter") {
                        e.preventDefault();
                        if (aiChatSendBtn) aiChatSendBtn.click();
                    }
                });
            }
            if (aiNewSessionBtn) {
                aiNewSessionBtn.addEventListener("click", async () => {
                    await createAiSession();
                });
            }
            if (aiSessionList) {
                aiSessionList.addEventListener("click", async (e) => {
                    const btn = e.target.closest("button[data-session-id]");
                    if (!btn || !aiSessionList.contains(btn)) return;
                    await switchAiSession(btn.dataset.sessionId);
                });
            }
        };

        return {
            bindEvents,
            renderAiAssistant,
            switchAiTab
        };
    };
})();
