import { onMounted, onUnmounted, nextTick, watch } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const defaultOptions = {
  start: 'top 95%',
  duration: 0.7,
  y: 24,
  stagger: 0.05,
  once: true
}

export function useScrollReveal(containerRef, selector, opts = {}) {
  const options = { ...defaultOptions, ...opts }
  let triggers = []

  const run = () => {
    nextTick(() => {
      triggers.forEach((t) => t.kill())
      triggers = []
      const container = containerRef.value
      if (!container) return
      const items = container.querySelectorAll(selector)
      if (!items.length) return

      gsap.set(items, { opacity: 0, y: options.y })

      // Use a single ScrollTrigger.batch for better performance
      ScrollTrigger.batch(items, {
        start: options.start,
        once: options.once,
        onEnter: (batch) => {
          gsap.to(batch, {
            opacity: 1,
            y: 0,
            duration: options.duration,
            stagger: options.stagger,
            ease: 'power2.out'
          })
        }
      })

      // Keep one trigger as handle for cleanup
      triggers = [ScrollTrigger.getAll().find(t => t.vars.trigger === items[items.length - 1])].filter(Boolean)
    })
  }

  onMounted(run)
  onUnmounted(() => {
    triggers.forEach((t) => t.kill())
  })

  return { run }
}

export function useScrollRevealChildren(containerRef, childSelector, opts = {}) {
  const options = { ...defaultOptions, ...opts }
  let triggers = []

  const run = () => {
    nextTick(() => {
      triggers.forEach((t) => t.kill())
      triggers = []
      const container = containerRef.value
      if (!container) return
      const parent = container.querySelector(childSelector)
      if (!parent) return
      const items = Array.from(parent.children)
      if (!items.length) return

      gsap.set(items, { opacity: 0, y: options.y })

      ScrollTrigger.batch(items, {
        start: options.start,
        once: options.once,
        onEnter: (batch) => {
          gsap.to(batch, {
            opacity: 1,
            y: 0,
            duration: options.duration,
            stagger: options.stagger,
            ease: 'power2.out'
          })
        }
      })

      triggers = [ScrollTrigger.getAll().find(t => t.vars.trigger === items[items.length - 1])].filter(Boolean)
    })
  }

  onMounted(run)
  onUnmounted(() => {
    triggers.forEach((t) => t.kill())
  })

  return { run }
}

export function useWatchReveal(containerRef, selector, watchSource, opts = {}) {
  const { run } = useScrollReveal(containerRef, selector, opts)
  watch(watchSource, run)
  return { run }
}

export function useWatchRevealChildren(containerRef, childSelector, watchSource, opts = {}) {
  const { run } = useScrollRevealChildren(containerRef, childSelector, opts)
  watch(watchSource, run)
  return { run }
}
