<script setup>
import { computed } from 'vue'
import { useScrollProgress } from '@/composables/useScrollProgress.js'

const raw = useScrollProgress()
const pct = computed(() => raw.value * 100)
</script>

<template>
  <div class="reading-progress" aria-hidden="true">
    <div class="reading-progress-track">
      <div class="reading-progress-bar" :style="{ height: pct + '%' }"></div>
    </div>
  </div>
</template>

<style scoped>
.reading-progress {
  position: fixed;
  left: 24px;
  top: 18%;
  height: 64%;
  z-index: 900;
  pointer-events: none;
}

.reading-progress-track {
  width: 2px;
  height: 100%;
  background: var(--line);
  opacity: 0.5;
  border-radius: 1px;
  overflow: hidden;
}

.reading-progress-bar {
  width: 100%;
  background: var(--accent);
  border-radius: 1px;
  transition: height 0.1s linear;
}

@media (max-width: 768px) {
  .reading-progress {
    display: none;
  }
}
</style>
