import { ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'

const THEME_KEY = 'adawing:theme-v2'
const LEGACY_KEY = 'adawing:theme'

// palette 元数据：固定明暗（oriental 除外）
const PALETTES = {
  oriental:   { dark: null },  // null = 可由用户切换
  anthropic:  { dark: false },
  kimi:       { dark: false },
  catppuccin: { dark: true },
  glass:      { dark: false },
  cli:        { dark: true },
  custom:     { dark: null },  // 由自定义背景色明度推导
}

// 纯色自定义 5 组预设：bg → accent
// 深浅一对时浅色做底、深色做强调；预设1为渐变（渐变仅铺 body，内容表面用实色 surface 保证文字可读）
export const CUSTOM_PRESETS = [
  { gradient: ['#134E5E', '#71B280'], bg: '#10261C', accent: '#71B280' }, // 渐变：青绿草木系，表面深绿实色
  { bg: '#1A1A1D', accent: '#E6397C' },
  { bg: '#F5EFEA', accent: '#122E8A' },
  { bg: '#F1DDDF', accent: '#E72D48' },
  { bg: '#F9D2E4', accent: '#01847F' },
]

function hexLuminance(hex) {
  const m = hex.replace('#', '')
  if (m.length < 6) return 1
  const r = parseInt(m.slice(0, 2), 16) / 255
  const g = parseInt(m.slice(2, 4), 16) / 255
  const b = parseInt(m.slice(4, 6), 16) / 255
  const lin = (c) => (c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4))
  return 0.2126 * lin(r) + 0.7152 * lin(g) + 0.0722 * lin(b)
}

function hexToRgbParts(hex) {
  const m = hex.replace('#', '')
  return `${parseInt(m.slice(0, 2), 16)}, ${parseInt(m.slice(2, 4), 16)}, ${parseInt(m.slice(4, 6), 16)}`
}

function shiftHex(hex, amount) {
  const m = hex.replace('#', '')
  const clamp = (v) => Math.max(0, Math.min(255, v))
  const r = clamp(parseInt(m.slice(0, 2), 16) + amount)
  const g = clamp(parseInt(m.slice(2, 4), 16) + amount)
  const b = clamp(parseInt(m.slice(4, 6), 16) + amount)
  return `#${[r, g, b].map((v) => v.toString(16).padStart(2, '0')).join('')}`
}

export const useThemeStore = defineStore('theme', () => {
  const palette = ref('oriental')
  const mode = ref('light') // 仅 oriental 可切换
  const custom = ref(null)  // { preset } 或 { bg, accent }

  const isDark = computed(() => {
    const meta = PALETTES[palette.value]
    if (palette.value === 'custom' && custom.value) {
      const bg = custom.value.bg || '#1A1A1D'
      return hexLuminance(bg) < 0.5
    }
    if (meta && meta.dark !== null) return meta.dark
    return mode.value === 'dark'
  })

  function apply() {
    const root = document.documentElement
    root.dataset.palette = palette.value
    root.dataset.theme = isDark.value ? 'dark' : 'light'
    applyCustomVars(root)
  }

  function applyCustomVars(root) {
    // 先清理，再按需注入
    const keys = ['--bg', '--bg-rgb', '--bg-shift', '--bg-warm', '--ink', '--ink-light', '--ink-faint', '--accent', '--accent-hover', '--accent-faint', '--line', '--grad-from', '--grad-to']
    keys.forEach((k) => root.style.removeProperty(k))
    delete root.dataset.gradient
    if (palette.value !== 'custom' || !custom.value) return

    const c = custom.value
    const preset = typeof c.preset === 'number' ? CUSTOM_PRESETS[c.preset] : null
    const bg = c.bg || (preset && preset.bg) || '#1A1A1D'
    const accent = c.accent || (preset && preset.accent) || '#E6397C'
    const dark = hexLuminance(bg) < 0.5

    const isGradient = preset && preset.gradient
    if (isGradient) {
      root.dataset.gradient = '1'
      root.style.setProperty('--grad-from', preset.gradient[0])
      root.style.setProperty('--grad-to', preset.gradient[1])
    }

    root.style.setProperty('--bg', bg)
    root.style.setProperty('--bg-rgb', hexToRgbParts(bg))
    root.style.setProperty('--bg-shift', hexToRgbParts(shiftHex(bg, dark ? 14 : -14)))
    // 渐变预设的表面需更亮以从渐变背景上浮起；普通预设微调即可
    const surfaceShift = isGradient ? (dark ? 28 : -28) : (dark ? 8 : -8)
    root.style.setProperty('--bg-warm', shiftHex(bg, surfaceShift))
    root.style.setProperty('--accent', accent)
    root.style.setProperty('--accent-hover', shiftHex(accent, dark ? 24 : -24))
    const a = accent.replace('#', '')
    root.style.setProperty('--accent-faint', `rgba(${parseInt(a.slice(0,2),16)}, ${parseInt(a.slice(2,4),16)}, ${parseInt(a.slice(4,6),16)}, 0.14)`)
    if (dark) {
      root.style.setProperty('--ink', '#F2F2F4')
      root.style.setProperty('--ink-light', '#D5D5DA')
      root.style.setProperty('--ink-faint', '#9A9AA2')
      root.style.setProperty('--line', 'rgba(255,255,255,0.14)')
    } else {
      // 浅色底：墨色从底色派生（深档 = 底色大幅压暗），带底色色调而非死黑
      root.style.setProperty('--ink', shiftHex(bg, -160))
      root.style.setProperty('--ink-light', shiftHex(bg, -120))
      root.style.setProperty('--ink-faint', shiftHex(bg, -80))
      root.style.setProperty('--line', 'rgba(0,0,0,0.16)')
    }
  }

  function persist() {
    localStorage.setItem(THEME_KEY, JSON.stringify({
      palette: palette.value,
      mode: mode.value,
      custom: custom.value,
    }))
  }

  function init() {
    const raw = localStorage.getItem(THEME_KEY)
    if (raw) {
      try {
        const s = JSON.parse(raw)
        palette.value = PALETTES[s.palette] ? s.palette : 'oriental'
        mode.value = s.mode === 'dark' ? 'dark' : 'light'
        custom.value = s.custom || null
      } catch { /* fall through to legacy */ }
    } else {
      const legacy = localStorage.getItem(LEGACY_KEY)
      if (legacy) {
        palette.value = 'oriental'
        mode.value = legacy === 'dark' ? 'dark' : 'light'
        localStorage.removeItem(LEGACY_KEY)
      } else {
        palette.value = 'oriental'
        mode.value = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
      }
    }
    persist()
    apply()
  }

  function setPalette(name) {
    if (!PALETTES[name]) return
    palette.value = name
    persist()
    apply()
  }

  function setCustom(value) {
    custom.value = value
    palette.value = 'custom'
    persist()
    apply()
  }

  function toggle() {
    if (palette.value !== 'oriental') return
    mode.value = mode.value === 'dark' ? 'light' : 'dark'
    persist()
    apply()
  }

  watch([palette, mode, custom], () => { persist(); apply() }, { deep: true })

  return { palette, mode, custom, isDark, init, setPalette, setCustom, toggle }
})
