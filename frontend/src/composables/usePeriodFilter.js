import { computed } from "vue";

/**
 * 周期（月份/年份）筛选选项逻辑。
 * 从数据集中按指定日期字段提取去重、降序的月份(yyyy-MM)与年份(yyyy)选项，
 * 并在选项变化时把 filter.month / filter.year 同步到合法的默认值。
 *
 * @param {import('vue').Ref<Array>} items 数据集 ref（每项含 dateField 字段）
 * @param {string} dateField 日期字段名，如 "endDate" / "deadline" / "accountDate"
 * @param {Object} filter 含 month / year 的 reactive 筛选对象
 */
export function usePeriodFilter(items, dateField, filter) {
  const monthOptions = computed(() =>
    [
      ...new Set(
        items.value
          .map((i) => String(i[dateField] || "").slice(0, 7))
          .filter((v) => /^\d{4}-\d{2}$/.test(v))
      )
    ]
      .sort()
      .reverse()
  );

  const yearOptions = computed(() =>
    [
      ...new Set(
        items.value
          .map((i) => String(i[dateField] || "").slice(0, 4))
          .filter((v) => /^\d{4}$/.test(v))
      )
    ]
      .sort()
      .reverse()
  );

  function syncPeriodOptions() {
    if (!monthOptions.value.includes(filter.month)) filter.month = monthOptions.value[0] || "";
    if (!yearOptions.value.includes(filter.year)) filter.year = yearOptions.value[0] || "";
  }

  return { monthOptions, yearOptions, syncPeriodOptions };
}
