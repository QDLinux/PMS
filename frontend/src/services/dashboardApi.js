// 后端 API 封装：基于 request 提供仪表盘、账号、记账、规划、目标与 AI 相关接口。
import { request } from "./http";

/**
 * 仪表盘及各业务模块的接口集合，统一通过 request 发起请求。
 */
export const dashboardApi = {
  // 获取仪表盘汇总数据（收支、计划、目标统计）
  getSummary() {
    return request("/api/dashboard/summary", { method: "GET" });
  },
  // 获取当前登录用户信息
  getProfile() {
    return request("/api/account/me", { method: "GET" });
  },
  // 修改昵称
  updateNickname(nickname) {
    return request("/api/account/me/nickname", { method: "PUT", body: JSON.stringify({ nickname }) });
  },
  // 修改用户名
  updateUsername(username) {
    return request("/api/account/me/username", { method: "PUT", body: JSON.stringify({ username }) });
  },
  // 修改邮箱
  updateEmail(email) {
    return request("/api/account/me/email", { method: "PUT", body: JSON.stringify({ email }) });
  },
  // 上传头像（以 multipart/form-data 形式提交文件）
  uploadAvatar(file) {
    const formData = new FormData();
    formData.append("file", file);
    return request("/api/account/me/avatar", { method: "POST", body: formData });
  },
  // 修改密码
  changePassword(body) {
    return request("/api/account/me/password", { method: "PUT", body: JSON.stringify(body) });
  },

  // ===== 记账 =====
  getAccounting() {
    return request("/api/accounting", { method: "GET" });
  },
  createAccounting(body) {
    return request("/api/accounting", { method: "POST", body: JSON.stringify(body) });
  },
  updateAccounting(id, body) {
    return request(`/api/accounting/${id}`, { method: "PUT", body: JSON.stringify(body) });
  },
  deleteAccounting(id) {
    return request(`/api/accounting/${id}`, { method: "DELETE" });
  },

  // ===== 规划 =====
  getPlans() {
    return request("/api/plans", { method: "GET" });
  },
  createPlan(body) {
    return request("/api/plans", { method: "POST", body: JSON.stringify(body) });
  },
  updatePlan(id, body) {
    return request(`/api/plans/${id}`, { method: "PUT", body: JSON.stringify(body) });
  },
  deletePlan(id) {
    return request(`/api/plans/${id}`, { method: "DELETE" });
  },

  // ===== 目标 =====
  getGoals() {
    return request("/api/goals", { method: "GET" });
  },
  createGoal(body) {
    return request("/api/goals", { method: "POST", body: JSON.stringify(body) });
  },
  updateGoal(id, body) {
    return request(`/api/goals/${id}`, { method: "PUT", body: JSON.stringify(body) });
  },
  deleteGoal(id) {
    return request(`/api/goals/${id}`, { method: "DELETE" });
  },

  // ===== AI 助手 =====
  // 新建一个 AI 会话
  createAiSession() {
    return request("/api/ai/sessions/new", { method: "PUT" });
  },
  // 获取会话列表
  getAiSessions(limit = 50) {
    return request(`/api/ai/sessions?limit=${encodeURIComponent(limit)}`, { method: "GET" });
  },
  // 获取指定会话的历史消息
  getAiHistory(sessionId, limit = 200) {
    return request(`/api/ai/history?sessionId=${encodeURIComponent(sessionId)}&limit=${encodeURIComponent(limit)}`, { method: "GET" });
  },
  // 删除指定会话
  deleteAiSession(sessionId) {
    return request(`/api/ai/sessions/${encodeURIComponent(sessionId)}`, { method: "DELETE" });
  },
  // 发送对话消息并获取 AI 回复
  askAi(body) {
    return request("/api/ai/chat", { method: "POST", body: JSON.stringify(body) });
  }
};
