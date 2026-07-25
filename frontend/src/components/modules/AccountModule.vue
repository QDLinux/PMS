<script setup>
/**
 * 账号管理模块
 * 提供修改昵称、用户名、邮箱、密码及上传头像的功能。
 * 通过 props 接收当前用户资料，修改成功后向父组件回传更新事件。
 * 头像以带鉴权的 fetch 拉取并转为 Blob URL 预览，组件销毁时释放对象 URL。
 */
import { computed, onBeforeUnmount, reactive, ref, watch } from "vue";
import { dashboardApi } from "../../services/dashboardApi";
import { clearAuth, getToken } from "../../services/http";

const props = defineProps({ profile: { type: Object, default: () => ({}) } });
const emit = defineEmits(["profile-updated"]);

// 表单数据：基础信息与密码修改字段
const form = reactive({ nickname: "", username: "", email: "", oldPassword: "", newPassword: "", confirmPassword: "" });
const avatarFile = ref(null); // 用户选择的待上传头像文件
const avatarPreview = ref(""); // 本地选择文件生成的预览 URL
const avatarBlobUrl = ref(""); // 从服务器拉取的头像 Blob URL
const saving = ref(""); // 当前正在保存的操作标识，用于禁用对应按钮
const message = ref(""); // 操作成功提示
const error = ref(""); // 操作失败提示

// 头像展示地址：优先使用本地预览，其次使用服务器头像
const avatarUrl = computed(() => avatarPreview.value || avatarBlobUrl.value);

// 监听父组件传入的资料变化，同步到表单字段
watch(
  () => props.profile,
  (profile) => {
    form.nickname = profile.nickname || "";
    form.username = profile.username || "";
    form.email = profile.email || "";
  },
  { immediate: true, deep: true }
);

// 监听头像地址变化，带鉴权拉取图片并转为 Blob URL 预览
watch(
  () => props.profile.avatarUrl,
  async (url) => {
    // 释放旧的 Blob URL，避免内存泄漏
    if (avatarBlobUrl.value) URL.revokeObjectURL(avatarBlobUrl.value);
    avatarBlobUrl.value = "";
    if (!url) return;
    try {
      // 携带 token 请求头像，附带时间戳防缓存
      const headers = {};
      const token = getToken();
      if (token) headers.Authorization = `Bearer ${token}`;
      const response = await fetch(`${url}?t=${Date.now()}`, { headers });
      if (!response.ok) return;
      avatarBlobUrl.value = URL.createObjectURL(await response.blob());
    } catch {
      avatarBlobUrl.value = "";
    }
  },
  { immediate: true }
);

// 组件卸载前释放所有 Blob 对象 URL
onBeforeUnmount(() => {
  if (avatarBlobUrl.value) URL.revokeObjectURL(avatarBlobUrl.value);
  if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value);
});

// 设置操作结果提示：根据 isError 写入成功或失败信息（互斥）
function setResult(text, isError = false) {
  message.value = isError ? "" : text;
  error.value = isError ? text : "";
}

/**
 * 统一执行异步操作的包装器
 * @param {Function} action 实际执行的异步操作
 * @param {string} label 操作标识，用于标记保存状态与禁用按钮
 */
async function run(action, label) {
  saving.value = label;
  setResult("");
  try {
    await action();
  } catch (err) {
    setResult(err.message || "操作失败", true);
  } finally {
    saving.value = "";
  }
}

// 保存昵称：校验非空后调用接口，成功则回传更新
async function saveNickname() {
  const nickname = form.nickname.trim();
  if (!nickname) return setResult("昵称不能为空", true);
  await run(async () => {
    const profile = await dashboardApi.updateNickname(nickname);
    emit("profile-updated", profile);
    setResult("昵称已更新");
  }, "nickname");
}

// 修改用户名：成功后清除登录态并跳转登录页（需重新登录）
async function saveUsername() {
  const username = form.username.trim();
  if (!username) return setResult("用户名不能为空", true);
  await run(async () => {
    await dashboardApi.updateUsername(username);
    setResult("用户名已更新，请重新登录");
    // 延迟跳转，给用户留出查看提示的时间
    setTimeout(() => {
      clearAuth();
      window.location.href = "/";
    }, 900);
  }, "username");
}

// 保存邮箱：校验非空后调用接口，成功则回传更新
async function saveEmail() {
  const email = form.email.trim();
  if (!email) return setResult("邮箱不能为空", true);
  await run(async () => {
    const profile = await dashboardApi.updateEmail(email);
    emit("profile-updated", profile);
    setResult("邮箱已更新");
  }, "email");
}

// 选择头像文件：记录文件并生成本地预览 URL
function pickAvatar(event) {
  const [file] = event.target.files || [];
  avatarFile.value = file || null;
  if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value); // 释放上一张预览
  avatarPreview.value = file ? URL.createObjectURL(file) : "";
}

// 上传头像：提交文件，成功后清理本地预览并回传更新
async function uploadAvatar() {
  if (!avatarFile.value) return setResult("请先选择头像文件", true);
  await run(async () => {
    const profile = await dashboardApi.uploadAvatar(avatarFile.value);
    avatarFile.value = null;
    if (avatarPreview.value) URL.revokeObjectURL(avatarPreview.value);
    avatarPreview.value = "";
    emit("profile-updated", profile);
    setResult("头像已上传");
  }, "avatar");
}

// 修改密码：校验旧/新密码及两次输入一致后提交，成功则清空表单
async function savePassword() {
  if (!form.oldPassword || !form.newPassword) return setResult("请输入旧密码和新密码", true);
  if (form.newPassword !== form.confirmPassword) return setResult("两次输入的新密码不一致", true);
  await run(async () => {
    await dashboardApi.changePassword({ oldPassword: form.oldPassword, newPassword: form.newPassword });
    form.oldPassword = "";
    form.newPassword = "";
    form.confirmPassword = "";
    setResult("密码已更新");
  }, "password");
}
</script>

<template>
  <section class="account-module">
    <!-- 顶部概览：标题与当前用户头像/昵称/邮箱卡片 -->
    <article class="summary-overview module-overview">
      <div class="summary-overview-top">
        <div>
          <h2>账号管理</h2>
          <p>维护头像、昵称、登录账号、邮箱与密码</p>
        </div>
        <div class="account-profile-card">
          <img v-if="avatarUrl" class="avatar-preview" :src="avatarUrl" alt="头像预览" />
          <!-- 无头像时使用昵称/用户名首字符作为占位 -->
          <div v-else class="account-avatar-fallback">{{ (profile.nickname || profile.username || "用").slice(0, 1) }}</div>
          <div>
            <b>{{ profile.nickname || profile.username || "用户" }}</b>
            <span>{{ profile.email || "未绑定邮箱" }}</span>
          </div>
        </div>
      </div>
    </article>

    <div class="account-grid">
      <!-- 头像设置：选择文件并上传 -->
      <article class="panel account-panel account-avatar-row">
        <div class="panel-head"><h3>头像</h3><span>支持常见图片格式</span></div>
        <div class="account-avatar-inline">
          <img v-if="avatarUrl" class="avatar-preview account-avatar-medium" :src="avatarUrl" alt="头像预览" />
          <div v-else class="account-avatar-fallback account-avatar-medium">{{ (profile.nickname || profile.username || "用").slice(0, 1) }}</div>
          <div class="account-avatar-actions">
            <label>选择文件<input type="file" accept="image/*" @change="pickAvatar" /></label>
            <button type="button" :disabled="saving === 'avatar'" @click="uploadAvatar">{{ saving === "avatar" ? "上传中..." : "上传头像" }}</button>
          </div>
        </div>
      </article>

      <!-- 基础信息设置：昵称、用户名、邮箱（各自独立保存） -->
      <article class="panel account-panel">
        <div class="panel-head"><h3>基础信息</h3><span>修改个人展示信息</span></div>
        <div class="account-form-stack">
          <label>昵称<input v-model="form.nickname" type="text" placeholder="请输入昵称" /></label>
          <button type="button" :disabled="saving === 'nickname'" @click="saveNickname">{{ saving === "nickname" ? "保存中..." : "保存昵称" }}</button>
          <label>用户名<input v-model="form.username" type="text" placeholder="请输入用户名" /></label>
          <button type="button" class="secondary" :disabled="saving === 'username'" @click="saveUsername">{{ saving === "username" ? "保存中..." : "修改用户名" }}</button>
          <label>邮箱<input v-model="form.email" type="email" placeholder="请输入邮箱" /></label>
          <button type="button" :disabled="saving === 'email'" @click="saveEmail">{{ saving === "email" ? "保存中..." : "保存邮箱" }}</button>
        </div>
      </article>

      <!-- 密码安全设置：校验旧密码与两次新密码后提交 -->
      <article class="panel account-panel">
        <div class="panel-head"><h3>密码安全</h3><span>修改后请使用新密码登录</span></div>
        <form class="account-form-stack" @submit.prevent="savePassword">
          <label>旧密码<input v-model="form.oldPassword" type="password" autocomplete="current-password" /></label>
          <label>新密码<input v-model="form.newPassword" type="password" autocomplete="new-password" /></label>
          <label>确认新密码<input v-model="form.confirmPassword" type="password" autocomplete="new-password" /></label>
          <button type="submit" :disabled="saving === 'password'">{{ saving === "password" ? "保存中..." : "修改密码" }}</button>
        </form>
      </article>
    </div>

    <!-- 操作结果提示：成功与失败信息 -->
    <p v-if="message" class="account-result">{{ message }}</p>
    <p v-if="error" class="account-result error">{{ error }}</p>
  </section>
</template>
