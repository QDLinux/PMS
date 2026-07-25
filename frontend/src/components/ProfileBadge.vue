<script setup>
/**
 * 用户信息徽标。
 * 展示头像（带鉴权拉取）、昵称与邮箱；无头像时回退为昵称首字母。
 * 头像通过带 Bearer token 的 fetch 获取并转为 Blob URL，需在销毁/切换时释放。
 */
import { computed, onUnmounted, ref, watch } from "vue";
import { getToken } from "../services/http";

const props = defineProps({
  profile: { type: Object, default: () => ({}) },  // 用户信息对象
  compact: { type: Boolean, default: false }        // 紧凑模式（隐藏邮箱）
});

const avatarBlobUrl = ref(""); // 头像的 Blob URL，避免直接暴露需鉴权的源地址

const displayName = computed(() => props.profile.nickname || props.profile.username || "用户");
const email = computed(() => props.profile.email || "当前账号");
const initial = computed(() => displayName.value.slice(0, 1)); // 无头像时显示的首字母

// 监听头像地址变化：先释放旧 Blob URL，再带鉴权头拉取并生成新的 Blob URL
watch(
  () => props.profile.avatarUrl,
  async (url) => {
    if (avatarBlobUrl.value) URL.revokeObjectURL(avatarBlobUrl.value);
    avatarBlobUrl.value = "";
    if (!url) return;
    try {
      const headers = {};
      const token = getToken();
      if (token) headers.Authorization = `Bearer ${token}`;
      // 附加时间戳参数避免浏览器缓存旧头像
      const response = await fetch(`${url}?t=${Date.now()}`, { headers });
      if (!response.ok) return;
      avatarBlobUrl.value = URL.createObjectURL(await response.blob());
    } catch (err) {
      avatarBlobUrl.value = "";
    }
  },
  { immediate: true }
);

// 卸载时释放 Blob URL，防止内存泄漏
onUnmounted(() => {
  if (avatarBlobUrl.value) URL.revokeObjectURL(avatarBlobUrl.value);
});
</script>

<template>
  <div class="profile-badge" :class="{ compact }">
    <img v-if="avatarBlobUrl" class="profile-badge-avatar" :src="avatarBlobUrl" alt="用户头像" />
    <div v-else class="profile-badge-fallback">{{ initial }}</div>
    <div class="profile-badge-text">
      <b>{{ displayName }}</b>
      <span v-if="!compact">{{ email }}</span>
    </div>
  </div>
</template>
