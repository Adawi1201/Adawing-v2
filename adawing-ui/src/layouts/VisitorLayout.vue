<script setup>
import VisitorHeader from '@/components/VisitorHeader.vue'
import VisitorFooter from '@/components/VisitorFooter.vue'
import ThemeToggle from '@/components/ThemeToggle.vue'
import FloatingNav from '@/components/FloatingNav.vue'
import ReadingProgress from '@/components/ReadingProgress.vue'
import CursorTrail from '@/components/CursorTrail.vue'
import { useSiteStore } from '@/stores/site.js'
import { onMounted, ref, onUnmounted } from 'vue'

const site = useSiteStore()
const headerRef = ref(null)
const showHeader = ref(true)

let lastY = 0
let ticking = false

function onScroll() {
  if (ticking) return
  ticking = true
  requestAnimationFrame(() => {
    ticking = false
    showHeader.value = window.scrollY <= 120 || window.scrollY < lastY
    lastY = window.scrollY
  })
}

onMounted(() => {
  site.load()
  window.addEventListener('scroll', onScroll, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<template>
  <div class="visitor-layout">
    <ThemeToggle />
    <CursorTrail />
    <ReadingProgress />
    <transition name="header-fade">
      <VisitorHeader v-show="showHeader" ref="headerRef" />
    </transition>
    <FloatingNav />
    <main class="visitor-main">
      <RouterView v-slot="{ Component }">
        <transition name="page" mode="out-in">
          <component :is="Component" />
        </transition>
      </RouterView>
    </main>
    <VisitorFooter />
  </div>
</template>

<style scoped>
.visitor-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--bg);
}

.visitor-main {
  flex: 1 0 auto;
}

.header-fade-enter-active,
.header-fade-leave-active {
  transition: transform 0.4s ease, opacity 0.4s ease;
}

.header-fade-enter-from,
.header-fade-leave-to {
  transform: translateY(-20px);
  opacity: 0;
}

.page-enter-active,
.page-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.page-enter-from {
  opacity: 0;
  transform: translateY(8px);
}

.page-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
