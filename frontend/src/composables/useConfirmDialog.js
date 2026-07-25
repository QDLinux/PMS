import { reactive } from "vue";

/**
 * 确认/提示对话框逻辑。
 * 统一管理 ConfirmDialog 的状态与 Promise 化的确认流程，
 * 供各业务模块复用，避免重复实现 openDialog/closeDialog/showMessage。
 *
 * 用法：
 *   const { dialog, openDialog, closeDialog, showMessage } = useConfirmDialog();
 *   绑定到模板：<ConfirmDialog v-bind="dialog" @confirm="closeDialog(true)" @cancel="closeDialog(false)" />
 */
export function useConfirmDialog() {
  // 对话框响应式状态，可直接 v-bind 到 ConfirmDialog 组件
  const dialog = reactive({
    open: false,
    title: "",
    message: "",
    confirmText: "确认",
    cancelText: "取消",
    showCancel: true,
    danger: false,
    resolver: null // 当前 Promise 的 resolve 引用，用于关闭时回传结果
  });

  /**
   * 打开对话框并返回一个 Promise，用户操作后 resolve 对应结果。
   * @param {Object} options 覆盖默认配置（title/message/confirmText 等）
   * @return {Promise<boolean>} 确认为 true，取消为 false
   */
  function openDialog(options) {
    // 每次打开重置为默认配置，再合并调用方传入的选项
    Object.assign(dialog, {
      open: true,
      confirmText: "确认",
      cancelText: "取消",
      showCancel: true,
      danger: false,
      ...options
    });
    return new Promise((resolve) => {
      dialog.resolver = resolve;
    });
  }

  /**
   * 关闭对话框并以指定结果 resolve 等待中的 Promise。
   * @param {boolean} result 用户选择结果（确认/取消）
   */
  function closeDialog(result) {
    dialog.open = false;
    if (dialog.resolver) dialog.resolver(result);
    dialog.resolver = null;
  }

  // 仅提示用的快捷方法：隐藏取消按钮，确认文案为“知道了”
  const showMessage = (title, message) =>
    openDialog({ title, message, showCancel: false, confirmText: "知道了" });

  return { dialog, openDialog, closeDialog, showMessage };
}
