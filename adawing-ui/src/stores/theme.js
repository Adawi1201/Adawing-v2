import { ref, watch } from 'vue'
import { defineStore } from 'pinia'

const THEME_KEY = 'adawing:theme'

export const useThemeStore = defineStore('theme', () => {
  const isDark = ref(false)

  function init() {
    const stored = localStorage.getItem(THEME_KEY)
    if (stored) {
      isDark.value = stored === 'dark'
    } else {
      isDark.value = window.matchMedia('(prefers-color-scheme: dark)').matches
    }
    apply()
  }

  function apply() {
    document.documentElement.dataset.theme = isDark.value ? 'dark' : 'light'
  }

  function toggle() {
    isDark.value = !isDark.value
    localStorage.setItem(THEME_KEY, isDark.value ? 'dark' : 'light')
    apply()
  }

  watch(isDark, apply)

  return { isDark, init, toggle }
})
