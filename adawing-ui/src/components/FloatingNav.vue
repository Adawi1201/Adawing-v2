<script setup>
import { RouterLink } from 'vue-router'
import { onMounted, onUnmounted, ref } from 'vue'
import { visitorLinks } from '@/constants/nav.js'
import { useRouteActive } from '@/composables/useRouteActive.js'

const { isActive } = useRouteActive()
const visible = ref(false)

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

let ticking = false
function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    ticking = false
    visible.value = window.scrollY > 120
  })
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <transition name="float">
    <nav v-show="visible" class="floating-nav">
      <RouterLink
        v-for="link in visitorLinks"
        :key="link.name"
        :to="link.path"
        :class="['nav-bubble', { active: isActive(link.path) }]"
        :title="link.label"
      >
        <span class="bubble-icon" aria-hidden="true">
          <!-- Home -->
          <svg v-if="link.name === 'Home'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
            <polyline points="9 22 9 12 15 12 15 22" />
          </svg>
          <!-- Chronicle -->
          <svg v-else-if="link.name === 'Chronicle'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" />
            <polyline points="12 6 12 12 16 14" />
          </svg>
          <!-- Notes -->
          <svg v-else-if="link.name === 'Notes'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
            <polyline points="10 9 9 9 8 9" />
          </svg>
          <!-- Messages -->
          <svg v-else-if="link.name === 'Messages'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
          </svg>
          <!-- About -->
          <svg v-else-if="link.name === 'About'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </svg>
        </span>
        <span class="bubble-label">{{ link.label }}</span>
      </RouterLink>

      <button class="nav-bubble back-to-top" title="Back to top" @click="scrollToTop">
        <span class="bubble-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="19" x2="12" y2="5" />
            <polyline points="5 12 12 5 19 12" />
          </svg>
        </span>
        <span class="bubble-label">Top</span>
      </button>
    </nav>
  </transition>
</template>

<style scoped>
.floating-nav {
  position: fixed;
  right: 24px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1000;
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 14px;
  border-radius: 28px;
  background: rgba(var(--bg-rgb, 254, 254, 254), 0.75);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  border: 1px solid var(--line);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.06);
}

.nav-bubble {
  position: relative;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--ink-light);
  background: transparent;
  border: 1px solid var(--line);
  cursor: pointer;
  transition: all 0.35s ease;
  text-decoration: none;
}

.nav-bubble:hover,
.nav-bubble.active {
  color: var(--bg);
  background: var(--accent);
  border-color: var(--accent);
  transform: scale(1.08);
  box-shadow: 0 4px 16px rgba(184, 115, 51, 0.25);
}

.bubble-icon {
  width: 18px;
  height: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bubble-icon svg {
  width: 100%;
  height: 100%;
}

.bubble-label {
  position: absolute;
  right: 52px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 12px;
  letter-spacing: 0.06em;
  color: var(--ink-light);
  white-space: nowrap;
  opacity: 0;
  transition: opacity 0.3s ease, right 0.3s ease;
  pointer-events: none;
  background: rgba(var(--bg-rgb, 254, 254, 254), 0.9);
  padding: 4px 10px;
  border-radius: 6px;
  border: 1px solid var(--line);
}

.nav-bubble:hover .bubble-label {
  opacity: 1;
  right: 48px;
}

.back-to-top {
  border: none;
  border-top: 1px solid var(--line);
  border-radius: 0;
  padding-top: 14px;
  margin-top: 2px;
  width: 40px;
  height: auto;
  aspect-ratio: auto;
}

.back-to-top:hover {
  border-top-color: var(--accent);
}

.float-enter-active,
.float-leave-active {
  transition: all 0.5s ease;
}

.float-enter-from,
.float-leave-to {
  opacity: 0;
  transform: translateY(-50%) translateX(20px);
}

@media (max-width: 768px) {
  .floating-nav {
    display: none;
  }
}
</style>
