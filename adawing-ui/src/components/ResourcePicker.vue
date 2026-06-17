<script setup>
import { ref } from 'vue'
import { listResources } from '@/api/resources.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'

const USAGE_CONFIG = {
  avatar: {
    pool: 'AVATAR',
    allowFallback: true,
    title: 'Choose Avatar'
  },
  article: {
    pool: 'ARTICLE',
    allowFallback: true,
    title: 'Choose Article Resource'
  },
  emoji: {
    pool: 'EMOJI',
    allowFallback: false,
    title: 'Choose Emoji'
  },
  icon: {
    pool: 'AVATAR',
    allowFallback: true,
    title: 'Choose Icon'
  }
}

const props = defineProps({
  pool: { type: String, default: '' },
  usage: { type: String, default: '' },
  allowFallback: { type: Boolean, default: false },
  title: { type: String, default: 'Choose Resource' },
  emptyText: { type: String, default: '' }
})

const emit = defineEmits(['pick'])
defineExpose({ open })

const visible = ref(false)
const resources = ref([])
const loading = ref(false)
const error = ref('')
const bubbleKey = ref(0)
const currentTitle = ref(props.title)
const currentPool = ref(props.pool)
const currentAllowFallback = ref(props.allowFallback)
const currentUsage = ref(props.usage)

function resolveConfig(override = {}) {
  const usage = override.usage || props.usage || ''
  const usageConfig = usage ? USAGE_CONFIG[usage] : null
  currentUsage.value = usage
  currentPool.value = override.pool || usageConfig?.pool || props.pool || ''
  currentAllowFallback.value = override.allowFallback ?? usageConfig?.allowFallback ?? props.allowFallback ?? false
  currentTitle.value = override.title || usageConfig?.title || props.title
}

async function open(override = {}) {
  resolveConfig(override)
  visible.value = true
  loading.value = true
  error.value = ''
  bubbleKey.value++
  try {
    const params = { page: 1, size: 100 }
    if (currentPool.value) params.pool = currentPool.value
    if (currentAllowFallback.value) params.allowFallback = true
    const res = await listResources(params)
    const data = res.data || res
    resources.value = data.list || []
  } catch (e) {
    error.value = e.message || 'Failed to load'
  } finally {
    loading.value = false
  }
}

function select(r) {
  emit('pick', r)
  visible.value = false
}
</script>

<template>
  <Teleport to="body">
    <Transition name="rp-fade">
      <div v-if="visible" class="rp-overlay" @click.self="visible = false">
        <Transition name="rp-drop" appear>
          <div v-if="visible" class="rp-card">
            <div class="rp-header">
              <div class="rp-header-main">
                <h3>{{ currentTitle }}</h3>
                <p v-if="currentUsage" class="rp-usage-tip">
                  usage={{ currentUsage }} / pool={{ currentPool || 'ALL' }}<span v-if="currentAllowFallback"> / misc fallback</span>
                </p>
              </div>
              <button class="rp-close" @click="visible = false" title="Close">&times;</button>
            </div>

            <div v-if="loading" class="rp-state">
              <span class="rp-spinner"></span>
              <p>Loading...</p>
            </div>

            <template v-else>
              <Transition name="rp-bubble-slide">
                <div v-if="error" :key="'err-'+bubbleKey" class="rp-bubble rp-bubble-error">
                  <span class="rp-bubble-msg">{{ error }}</span>
                  <button class="rp-retry" @click="open">Retry</button>
                </div>
              </Transition>

              <Transition name="rp-bubble-slide">
                <div v-if="!error && resources.length === 0" :key="'empty-'+bubbleKey" class="rp-bubble rp-bubble-empty">
                  <span class="rp-bubble-msg">{{ props.emptyText || 'No resources yet. Upload files on the Resources page first.' }}</span>
                </div>
              </Transition>

              <div v-if="resources.length > 0" class="rp-grid">
                <div
                  v-for="r in resources"
                  :key="r.id"
                  class="rp-item"
                  @click="select(r)"
                >
                  <div class="rp-thumb">
                    <img
                      v-if="r.mimeType && r.mimeType.startsWith('image/')"
                      :src="resourceContentUrl(r.id)"
                      :alt="r.originalName"
                    />
                    <template v-else>
                      <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                      </svg>
                    </template>
                    <span class="rp-pool-tag">{{ r.pool }}</span>
                  </div>
                  <div class="rp-info">
                    <span class="rp-name" :title="r.originalName">{{ r.originalName }}</span>
                    <span class="rp-meta">{{ r.mimeType || 'unknown' }}</span>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* ── overlay ── */
.rp-fade-enter-active { transition: opacity 0.2s ease; }
.rp-fade-leave-active { transition: opacity 0.15s ease; }
.rp-fade-enter-from, .rp-fade-leave-to { opacity: 0; }

.rp-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.22);
  display: flex; align-items: flex-start; justify-content: center;
  padding: 80px 24px 24px;
}

/* ── card ── */
.rp-drop-enter-active { transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1); }
.rp-drop-leave-active { transition: all 0.15s ease-in; }
.rp-drop-enter-from { opacity: 0; transform: translateY(-20px) scale(0.97); }
.rp-drop-leave-to   { opacity: 0; transform: translateY(-12px) scale(0.98); }

.rp-card {
  width: 100%; max-width: 480px; max-height: 60vh;
  background: var(--bg);
  border: 1px solid var(--line);
  box-shadow: 0 8px 40px rgba(0,0,0,0.12);
  display: flex; flex-direction: column;
}

.rp-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px 14px;
  border-bottom: 1px solid var(--line);
  flex-shrink: 0;
}

.rp-header-main {
  min-width: 0;
}

.rp-header h3 {
  font-size: 13px; font-weight: 500;
  letter-spacing: 0.06em; color: var(--ink);
}

.rp-usage-tip {
  margin-top: 4px;
  font-size: 10px;
  color: var(--ink-faint);
  letter-spacing: 0.05em;
}

.rp-close {
  width: 26px; height: 26px;
  border: 1px solid var(--line); background: none;
  color: var(--ink-faint); font-size: 15px; line-height: 1;
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  transition: all 0.2s;
}
.rp-close:hover { border-color: var(--accent); color: var(--accent); }

/* ── loading ── */
.rp-state {
  padding: 56px 24px; text-align: center;
  display: flex; flex-direction: column; align-items: center; gap: 12px;
}

.rp-spinner {
  width: 26px; height: 26px;
  border: 2px solid var(--line);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: rp-spin 0.6s linear infinite;
}
@keyframes rp-spin { to { transform: rotate(360deg); } }
.rp-state p { font-size: 12px; color: var(--ink-faint); }

/* ── bubble ── */
.rp-bubble {
  margin: 12px 20px;
  padding: 9px 14px;
  display: flex; align-items: center; gap: 10px;
  font-size: 11px; letter-spacing: 0.04em;
  border-radius: 4px;
}

.rp-bubble-msg {
  flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.rp-bubble-error {
  background: rgba(196,92,92,0.06); border: 1px solid rgba(196,92,92,0.3); color: #c45c5c;
}
.rp-bubble-empty {
  background: var(--accent-faint); border: 1px solid var(--accent); color: var(--accent);
}

.rp-retry {
  flex-shrink: 0;
  background: none; border: none;
  font-size: 11px; color: inherit; cursor: pointer;
  letter-spacing: 0.05em; padding: 1px 6px;
  text-decoration: underline;
}
.rp-retry:hover { opacity: 0.7; }

/* bubble slide-in from top */
.rp-bubble-slide-enter-active { transition: all 0.25s ease-out; }
.rp-bubble-slide-leave-active { transition: all 0.15s ease-in; }
.rp-bubble-slide-enter-from { opacity: 0; transform: translateY(-12px); }
.rp-bubble-slide-leave-to   { opacity: 0; transform: translateY(-8px); }

/* ── grid ── */
.rp-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px; padding: 16px 20px;
  overflow-y: auto; flex: 1;
  min-height: 100px;
}

.rp-item {
  border: 1px solid var(--line);
  cursor: pointer;
  transition: border-color 0.2s;
}
.rp-item:hover {
  border-color: var(--accent);
}

/* ── thumb ── */
.rp-thumb {
  position: relative;
  width: 100%; height: 88px;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-warm);
}
.rp-thumb img { width: 100%; height: 100%; object-fit: cover; display: block; }
.rp-thumb svg { color: var(--ink-faint); }

.rp-pool-tag {
  position: absolute; top: 3px; right: 3px;
  font-size: 8px; letter-spacing: 0.06em;
  background: var(--bg); color: var(--ink-faint);
  padding: 1px 5px; border: 1px solid var(--line);
}

/* ── info ── */
.rp-info { padding: 7px 9px; display: flex; flex-direction: column; gap: 1px; }
.rp-name {
  font-size: 10px; color: var(--ink);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.rp-meta {
  font-size: 9px; color: var(--ink-faint);
  letter-spacing: 0.03em;
}

@media (max-width: 640px) {
  .rp-card { max-width: 100%; max-height: 70vh; }
  .rp-grid { grid-template-columns: repeat(2, 1fr); }
  .rp-overlay { padding: 40px 16px 16px; }
}
</style>
