<script setup>
/**
 * 通用确认/提示对话框。
 * 受控组件：由父级通过 open 控制显隐，通过 confirm / cancel 事件回传结果；
 * 打开时锁定页面滚动并自动聚焦确认按钮，支持 Esc 取消。
 */
import { nextTick, onBeforeUnmount, ref, watch } from "vue";

const props = defineProps({
  open: { type: Boolean, default: false },        // 是否显示对话框
  title: { type: String, default: "提示" },        // 标题
  message: { type: String, default: "" },          // 正文内容
  confirmText: { type: String, default: "确认" },   // 确认按钮文案
  cancelText: { type: String, default: "取消" },    // 取消按钮文案
  showCancel: { type: Boolean, default: true },     // 是否显示取消按钮
  danger: { type: Boolean, default: false }         // 确认按钮是否使用危险样式
});

const emit = defineEmits(["confirm", "cancel"]);
const confirmBtn = ref(null); // 确认按钮引用，用于自动聚焦

// 按下 Esc 视为取消
function onKeydown(event) {
  if (event.key === "Escape") emit("cancel");
}

// 监听显隐：打开时锁滚动并聚焦确认按钮，关闭时还原并解绑监听
watch(
  () => props.open,
  async (visible) => {
    if (!visible) {
      document.body.style.overflow = "";
      window.removeEventListener("keydown", onKeydown);
      return;
    }
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", onKeydown);
    await nextTick();
    confirmBtn.value?.focus();
  },
  { immediate: true }
);

// 组件卸载前清理副作用，避免残留滚动锁定与事件监听
onBeforeUnmount(() => {
  document.body.style.overflow = "";
  window.removeEventListener("keydown", onKeydown);
});
</script>

<template>
  <div v-if="open" class="dialog-mask" role="presentation">
    <section class="dialog-panel" role="dialog" aria-modal="true" :aria-label="title">
      <h3>{{ title }}</h3>
      <p>{{ message }}</p>
      <div class="dialog-actions">
        <button v-if="showCancel" class="dialog-cancel" type="button" @click="emit('cancel')">{{ cancelText }}</button>
        <button ref="confirmBtn" class="dialog-confirm" :class="{ danger }" type="button" @click="emit('confirm')">{{ confirmText }}</button>
      </div>
    </section>
  </div>
</template>
