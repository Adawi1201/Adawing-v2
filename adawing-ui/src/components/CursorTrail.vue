<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import gsap from 'gsap'

const dotRef = ref(null)
const ringRef = ref(null)
let xTo = null
let yTo = null
let ringXTo = null
let ringYTo = null
let raf = 0
let lastX = 0
let lastY = 0

onMounted(() => {
  if (!dotRef.value || !ringRef.value) return

  xTo = gsap.quickTo(dotRef.value, 'x', { duration: 0.15, ease: 'power2.out' })
  yTo = gsap.quickTo(dotRef.value, 'y', { duration: 0.15, ease: 'power2.out' })
  ringXTo = gsap.quickTo(ringRef.value, 'x', { duration: 0.35, ease: 'power2.out' })
  ringYTo = gsap.quickTo(ringRef.value, 'y', { duration: 0.35, ease: 'power2.out' })

  const tick = () => {
    xTo(lastX)
    yTo(lastY)
    ringXTo(lastX)
    ringYTo(lastY)
    raf = 0
  }

  const onMove = (e) => {
    lastX = e.clientX
    lastY = e.clientY
    if (!raf) {
      raf = requestAnimationFrame(tick)
    }
  }

  window.addEventListener('mousemove', onMove, { passive: true })

  onUnmounted(() => {
    window.removeEventListener('mousemove', onMove)
    if (raf) cancelAnimationFrame(raf)
  })
})
</script>

<template>
  <div class="cursor-trail" aria-hidden="true">
    <div ref="dotRef" class="cursor-dot"></div>
    <div ref="ringRef" class="cursor-ring"></div>
  </div>
</template>

<style scoped>
.cursor-trail {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
}

.cursor-dot,
.cursor-ring {
  position: absolute;
  top: 0;
  left: 0;
  border-radius: 50%;
  transform: translate(-50%, -50%);
  will-change: transform;
}

.cursor-dot {
  width: 5px;
  height: 5px;
  background: var(--accent);
  opacity: 0.35;
}

.cursor-ring {
  width: 24px;
  height: 24px;
  border: 1px solid var(--accent);
  opacity: 0.12;
}

@media (pointer: coarse) {
  .cursor-trail {
    display: none;
  }
}
</style>
