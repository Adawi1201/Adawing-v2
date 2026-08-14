<script setup>
import { ref } from 'vue'
import { useThemeStore, CUSTOM_PRESETS } from '@/stores/theme.js'

const theme = useThemeStore()
const open = ref(false)

const PALETTE_OPTIONS = [
  { name: 'oriental', label: '东方美学', desc: '直角 · 衬线 · 留白' },
  { name: 'anthropic', label: 'Anthropic', desc: '纸感 · 圆角 · 珊瑚橙' },
  { name: 'kimi', label: 'Kimi', desc: '冷调 · Kimi蓝 · 几何' },
  { name: 'catppuccin', label: 'Catppuccin', desc: 'Mocha · 粉彩' },
  { name: 'glass', label: '透明底板', desc: '毛玻璃 · 渐变' },
  { name: 'cli', label: '现代终端', desc: '等宽 · 语法色' },
  { name: 'custom', label: '自定义', desc: '双色 · 6 预设' },
]

// 自定义双色（自由选择）
const freeBg = ref('#1A1A1D')
const freeAccent = ref('#E6397C')

function pick(name) {
  open.value = false
  if (name === 'custom') {
    // 进入自定义时给默认预设2
    theme.setCustom({ preset: 1 })
  } else {
    theme.setPalette(name)
  }
}

function pickPreset(i) {
  theme.setCustom({ preset: i })
}

function applyFree() {
  theme.setCustom({ bg: freeBg.value, accent: freeAccent.value })
}

function swatchBg(p) {
  const c = CUSTOM_PRESETS[p]
  return c.gradient ? `linear-gradient(135deg, ${c.gradient[0]}, ${c.gradient[1]})` : c.bg
}
</script>

<template>
  <div class="theme-panel-wrap">
    <button
      class="theme-panel-trigger"
      aria-label="Choose theme"
      title="主题"
      @click="open = !open"
    >
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6">
        <circle cx="12" cy="12" r="9" />
        <circle cx="8.5" cy="9.5" r="1" fill="currentColor" stroke="none" />
        <circle cx="12" cy="7.5" r="1" fill="currentColor" stroke="none" />
        <circle cx="15.5" cy="9.5" r="1" fill="currentColor" stroke="none" />
        <path d="M12 21a9 9 0 0 0 9-9c0-1.5-1.2-2.6-2.7-2.4-2 .2-2.8-1.2-2.8-2.6" />
      </svg>
    </button>

    <transition name="tp-fade">
      <div v-if="open" class="theme-panel" @click.self="open = false">
        <div class="tp-card">
          <div class="tp-head">
            <span class="tp-title">主题风格</span>
            <button class="tp-close" @click="open = false">×</button>
          </div>

          <div class="tp-grid">
            <button
              v-for="p in PALETTE_OPTIONS"
              :key="p.name"
              class="tp-item"
              :class="{ active: theme.palette === p.name }"
              @click="pick(p.name)"
            >
              <span class="tp-swatch" :data-p="p.name"></span>
              <span class="tp-item-name">{{ p.label }}</span>
              <span class="tp-item-desc">{{ p.desc }}</span>
            </button>
          </div>

          <div v-if="theme.palette === 'custom'" class="tp-custom">
            <div class="tp-sub">预设配色</div>
            <div class="tp-presets">
              <button
                v-for="(c, i) in CUSTOM_PRESETS"
                :key="i"
                class="tp-preset"
                :class="{ active: theme.custom && theme.custom.preset === i }"
                @click="pickPreset(i)"
              >
                <span class="tp-preset-dot" :style="{ background: swatchBg(i) }"></span>
                <span class="tp-preset-dot" :style="{ background: c.accent }"></span>
              </button>
            </div>
            <div class="tp-sub">自选双色</div>
            <div class="tp-free">
              <label>底 <input type="color" v-model="freeBg"></label>
              <label>强调 <input type="color" v-model="freeAccent"></label>
              <button class="tp-apply" @click="applyFree">应用</button>
            </div>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<style scoped>
.theme-panel-wrap {
  position: fixed;
  top: 20px;
  right: 68px;
  z-index: 1001;
}
.theme-panel-trigger {
  width: 32px; height: 32px;
  border-radius: var(--radius-btn, 2px);
  border: var(--border-w, 1px) solid var(--line);
  background: var(--panel, var(--bg));
  color: var(--ink-faint);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  transition: all 0.3s;
  backdrop-filter: blur(var(--panel-blur, 0));
}
.theme-panel-trigger:hover { color: var(--accent); border-color: var(--accent); }
.theme-panel-trigger svg { width: 15px; height: 15px; }

.theme-panel {
  position: fixed; inset: 0; z-index: 1002;
  background: rgba(0, 0, 0, 0.3);
  display: flex; align-items: flex-start; justify-content: flex-end;
  padding: 64px 24px 24px;
}
.tp-card {
  width: 340px; max-height: 80vh; overflow-y: auto;
  background: var(--panel, var(--bg));
  border: var(--border-w, 1px) solid var(--line);
  border-radius: var(--radius, 4px);
  box-shadow: var(--shadow-card, 0 8px 32px rgba(0,0,0,.18));
  backdrop-filter: blur(var(--panel-blur, 12px));
  -webkit-backdrop-filter: blur(var(--panel-blur, 12px));
  padding: 20px;
}
.tp-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.tp-title { font-size: 13px; font-weight: 600; color: var(--ink); letter-spacing: .05em; }
.tp-close { background: none; border: none; font-size: 20px; color: var(--ink-faint); cursor: pointer; line-height: 1; }

.tp-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.tp-item {
  display: flex; flex-direction: column; align-items: flex-start; gap: 4px;
  padding: 12px; cursor: pointer; text-align: left;
  background: var(--panel-2, transparent);
  border: var(--border-w, 1px) solid var(--line);
  border-radius: var(--radius, 4px);
  transition: border-color .2s;
}
.tp-item:hover { border-color: var(--accent); }
.tp-item.active { border-color: var(--accent); box-shadow: 0 0 0 1px var(--accent); }
.tp-swatch { width: 100%; height: 34px; border-radius: calc(var(--radius, 4px) * .6); margin-bottom: 4px; border: 1px solid var(--line); }
.tp-swatch[data-p="oriental"] { background: linear-gradient(135deg,#FEFEFE 50%,#B87333 100%); }
.tp-swatch[data-p="anthropic"] { background: linear-gradient(135deg,#FAF9F5 50%,#D97757 100%); }
.tp-swatch[data-p="kimi"] { background: linear-gradient(135deg,#F7F8FA 50%,#0066FF 100%); }
.tp-swatch[data-p="catppuccin"] { background: linear-gradient(135deg,#1E1E2E 50%,#CBA6F7 100%); }
.tp-swatch[data-p="glass"] { background: linear-gradient(135deg,#a8c8ec,#e8b8c8,#7A6FF0); }
.tp-swatch[data-p="cli"] { background: linear-gradient(135deg,#0D1117 50%,#9ECE6A 100%); }
.tp-swatch[data-p="custom"] { background: linear-gradient(135deg,#1A1A1D 50%,#E6397C 100%); }
.tp-item-name { font-size: 12px; font-weight: 600; color: var(--ink); }
.tp-item-desc { font-size: 10.5px; color: var(--ink-faint); }

.tp-custom { margin-top: 18px; border-top: 1px solid var(--line); padding-top: 16px; }
.tp-sub { font-size: 10px; color: var(--ink-faint); letter-spacing: .12em; text-transform: uppercase; margin-bottom: 10px; }
.tp-presets { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
.tp-preset {
  display: flex; gap: 0; padding: 3px; cursor: pointer;
  background: var(--panel-2, transparent); border: var(--border-w,1px) solid var(--line);
  border-radius: var(--radius, 4px);
}
.tp-preset.active { border-color: var(--accent); box-shadow: 0 0 0 1px var(--accent); }
.tp-preset-dot { width: 18px; height: 18px; border-radius: 50%; display: block; }
.tp-free { display: flex; gap: 12px; align-items: center; }
.tp-free label { font-size: 11px; color: var(--ink-light); display: flex; align-items: center; gap: 6px; }
.tp-free input[type="color"] { width: 28px; height: 28px; border: 1px solid var(--line); border-radius: 4px; background: none; cursor: pointer; padding: 2px; }
.tp-apply {
  margin-left: auto; font-size: 11px; padding: 6px 14px; cursor: pointer;
  background: var(--accent); color: #fff; border: none;
  border-radius: var(--radius-btn, 4px);
}

.tp-fade-enter-active, .tp-fade-leave-active { transition: opacity .2s; }
.tp-fade-enter-from, .tp-fade-leave-to { opacity: 0; }
</style>
