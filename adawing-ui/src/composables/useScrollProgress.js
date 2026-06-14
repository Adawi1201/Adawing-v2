import { ref, onMounted, onUnmounted } from 'vue'

const progress = ref(0)
let ticking = false
let listeners = 0

function onScroll() {
  if (!ticking) {
    requestAnimationFrame(() => {
      const scrollHeight = document.documentElement.scrollHeight - window.innerHeight
      progress.value = scrollHeight > 0 ? window.scrollY / scrollHeight : 0
      ticking = false
    })
    ticking = true
  }
}

export function useScrollProgress() {
  onMounted(() => {
    if (listeners++ === 0) {
      window.addEventListener('scroll', onScroll, { passive: true })
    }
  })

  onUnmounted(() => {
    if (--listeners <= 0) {
      listeners = 0
      window.removeEventListener('scroll', onScroll)
    }
  })

  return progress
}
