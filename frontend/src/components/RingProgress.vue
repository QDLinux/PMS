<script setup>
import { computed } from "vue";

const props = defineProps({
  percent: { type: Number, required: true },
  size: { type: Number, default: 96 },
  strokeWidth: { type: Number, default: 8 },
  color: { type: String, default: "#0f766e" },
  trackColor: { type: String, default: "#dbebea" }
});

const radius = computed(() => (props.size - props.strokeWidth) / 2);
const circumference = computed(() => 2 * Math.PI * radius.value);
const offset = computed(() => circumference.value * (1 - Math.min(1, Math.max(0, props.percent / 100))));
const center = computed(() => props.size / 2);
const viewBox = computed(() => `0 0 ${props.size} ${props.size}`);
const transform = computed(() => `rotate(-90 ${center.value} ${center.value})`);
const textSize = computed(() => `${Math.round(props.size * 0.22)}px`);
</script>

<template>
  <div class="ring-progress" :style="{ width: size + 'px', height: size + 'px' }">
    <svg :width="size" :height="size" :viewBox="viewBox">
      <circle
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        :stroke="trackColor"
        :stroke-width="strokeWidth"
      />
      <circle
        class="ring-progress-arc"
        :cx="center"
        :cy="center"
        :r="radius"
        fill="none"
        :stroke="color"
        :stroke-width="strokeWidth"
        stroke-linecap="round"
        :stroke-dasharray="circumference"
        :stroke-dashoffset="offset"
        :transform="transform"
      />
    </svg>
    <span class="ring-progress-text" :style="{ fontSize: textSize }">
      {{ Math.round(percent) }}%
    </span>
  </div>
</template>
