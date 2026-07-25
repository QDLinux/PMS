// HTTP 请求封装：统一处理 token 注入、Content-Type 设置与响应/错误解析。

// 登录令牌在 localStorage 中的存储键名
const TOKEN_KEY = "pm_token";

/**
 * 读取本地保存的登录令牌。
 * @return {string} token 字符串，不存在时返回空串
 */
export function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

/**
 * 清除登录态（令牌与用户名），用于登出或鉴权失效场景。
 */
export function clearAuth() {
  localStorage.removeItem("pm_token");
  localStorage.removeItem("pm_username");
}

/**
 * 统一的 fetch 封装：自动注入鉴权头、解析 JSON 响应并抛出业务错误。
 * @param {string} url 请求地址
 * @param {Object} [options] fetch 配置（method、body、headers 等）
 * @return {Promise<*>} 解析后的 payload.data；无数据时为 null
 */
export async function request(url, options = {}) {
  const headers = { ...(options.headers || {}) };
  // FormData（如文件上传）由浏览器自动设置带 boundary 的 Content-Type，不能手动覆盖
  const isFormData = options.body instanceof FormData;
  if (!isFormData && !headers["Content-Type"]) {
    headers["Content-Type"] = "application/json";
  }

  // 有令牌时注入 Bearer 鉴权头
  const token = getToken();
  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  const response = await fetch(url, {
    ...options,
    headers
  });

  // 仅当响应为 JSON 时才尝试解析
  const contentType = response.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");
  let payload = null;
  if (isJson) {
    try {
      payload = await response.json();
    } catch (err) {
      payload = null;
    }
  }

  // HTTP 错误或业务标记 success=false 时统一抛出携带状态码的 Error
  if (!response.ok || (payload && payload.success === false)) {
    const message = (payload && (payload.message || payload.error)) || "请求失败";
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return payload ? payload.data : null;
}
