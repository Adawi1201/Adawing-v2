<script setup>
import { ref, onMounted, computed } from 'vue'
import { getSiteConfig, saveSiteConfig } from '@/api/config.js'
import AuthImage from '@/components/AuthImage.vue'
import ResourcePicker from '@/components/ResourcePicker.vue'
import { useThemeStore, CUSTOM_PRESETS } from '@/stores/theme.js'

const theme = useThemeStore()

const PALETTE_OPTIONS = [
  { name: 'oriental', label: '东方美学' },
  { name: 'anthropic', label: 'Anthropic' },
  { name: 'kimi', label: 'Kimi' },
  { name: 'catppuccin', label: 'Catppuccin' },
  { name: 'glass', label: '透明底板' },
  { name: 'cli', label: '现代终端' },
  { name: 'custom', label: '自定义' },
]

function onPaletteChange(e) {
  const name = e.target.value
  if (name === 'custom') theme.setCustom({ preset: 1 })
  else theme.setPalette(name)
}
function pickPreset(i) { theme.setCustom({ preset: i }) }
function swatchBg(i) {
  const c = CUSTOM_PRESETS[i]
  return c.gradient ? `linear-gradient(135deg, ${c.gradient[0]}, ${c.gradient[1]})` : c.bg
}

const config = ref({
  name: '',
  description: '',
  subtitle: '',
  logo: '',
  favicon: '',
  icp: '',
  publicSecurityRecord: '',
  footerText: '',
  seo: { keywords: '', description: '' },
  profile: { ownerName: '', avatar: '', bio: '', signature: '' },
  links: []
})

const loading = ref(false)
const saving = ref(false)
const message = ref('')

const previewJson = computed(() => JSON.stringify(config.value, null, 2))

async function load() {
  loading.value = true
  try {
    const res = await getSiteConfig()
    config.value = { ...config.value, ...(res.data || res) }
  } finally { loading.value = false }
}

async function save() {
  saving.value = true
  message.value = ''
  try {
    await saveSiteConfig(config.value)
    message.value = 'Saved'
    await load()
  } catch (e) {
    message.value = e.message || 'Save failed'
  } finally { saving.value = false }
}

const resourcePicker = ref(null)
const linkIconPicker = ref(null)
let pickTarget = ''
let pendingLinkIndex = -1

function openResourcePickerFor(target) {
  pickTarget = target
  if (target === 'avatar') {
    resourcePicker.value.open({ usage: 'avatar', title: 'Choose Avatar' })
    return
  }
  resourcePicker.value.open({ usage: 'icon', title: target === 'logo' ? 'Choose Logo' : 'Choose Favicon' })
}

function openLinkIconPicker(index) {
  pendingLinkIndex = index
  linkIconPicker.value.open({ usage: 'icon', title: 'Choose Link Icon' })
}

function onResourcePicked(r) {
  const id = String(r.id)
  if (pickTarget === 'logo') config.value.logo = id
  else if (pickTarget === 'favicon') config.value.favicon = id
  else if (pickTarget === 'avatar') config.value.profile.avatar = id
}

function onLinkIconPicked(r) {
  if (pendingLinkIndex >= 0) {
    config.value.links[pendingLinkIndex].icon = String(r.id)
    pendingLinkIndex = -1
  }
}

function clearResource(target) {
  if (target === 'logo') config.value.logo = ''
  else if (target === 'favicon') config.value.favicon = ''
  else if (target === 'avatar') config.value.profile.avatar = ''
}

function addLink() { config.value.links.push({ name: '', url: '', type: 'social', icon: '' }) }
function removeLink(index) { config.value.links.splice(index, 1) }
function moveLink(index, direction) {
  const list = config.value.links
  const ni = index + direction
  if (ni < 0 || ni >= list.length) return
  const item = list.splice(index, 1)[0]
  list.splice(ni, 0, item)
}

onMounted(load)
</script>

<template>
  <div class="admin-settings">
    <div class="admin-header-ori">
      <div>
        <h1>Settings</h1>
        <p class="sub">Manage site configuration. Edit above, preview JSON below.</p>
      </div>
      <span v-if="message" class="message-ori">{{ message }}</span>
    </div>

    <div v-if="loading" class="loading-ori">Loading...</div>

    <div v-else class="settings-form">
      <section class="form-section">
        <h3>外观 / Appearance</h3>
        <div class="form-row">
          <label>主题风格</label>
          <select class="input-ori" :value="theme.palette" @change="onPaletteChange">
            <option v-for="p in PALETTE_OPTIONS" :key="p.name" :value="p.name">{{ p.label }}</option>
          </select>
        </div>
        <div v-if="theme.palette === 'custom'" class="form-row">
          <label>预设配色</label>
          <div class="appearance-presets">
            <button
              v-for="(c, i) in CUSTOM_PRESETS"
              :key="i"
              class="ap-preset"
              :class="{ active: theme.custom && theme.custom.preset === i }"
              @click.prevent="pickPreset(i)"
            >
              <span class="ap-dot" :style="{ background: swatchBg(i) }"></span>
              <span class="ap-dot" :style="{ background: c.accent }"></span>
            </button>
          </div>
        </div>
      </section>

      <section class="form-section">
        <h3>Basic</h3>
        <div class="form-row">
          <label>Site Name</label>
          <input v-model="config.name" class="input-ori" />
        </div>
        <div class="form-row">
          <label>ICP</label>
          <input v-model="config.icp" class="input-ori" />
        </div>
        <div class="form-row">
          <label>Public Security Record</label>
          <input v-model="config.publicSecurityRecord" class="input-ori" />
        </div>
        <div class="form-row">
          <label>Description</label>
          <textarea v-model="config.description" class="input-ori" rows="3" />
        </div>
        <div class="form-row">
          <label>Subtitle</label>
          <textarea v-model="config.subtitle" class="input-ori" rows="2" />
        </div>

        <!-- Logo - pure picker -->
        <div class="form-row">
          <label>Logo</label>
          <div class="s-resource-field">
            <div class="s-resource-thumb s-thumb-logo" @click="openResourcePickerFor('logo')">
              <AuthImage v-if="config.logo" :src="config.logo" class="s-resource-img" @error="clearResource('logo')" />
              <span v-else class="s-resource-plus">+</span>
            </div>
            <div class="s-resource-body">
              <template v-if="config.logo">
                <code class="s-resource-id">ID: {{ config.logo }}</code>
                <button class="s-resource-clear" @click="clearResource('logo')">Remove</button>
              </template>
              <span v-else class="s-resource-hint">Click to choose from resources</span>
            </div>
          </div>
        </div>

        <!-- Favicon - pure picker -->
        <div class="form-row">
          <label>Favicon</label>
          <div class="s-resource-field">
            <div class="s-resource-thumb" @click="openResourcePickerFor('favicon')">
              <AuthImage v-if="config.favicon" :src="config.favicon" class="s-resource-img" @error="clearResource('favicon')" />
              <span v-else class="s-resource-plus">+</span>
            </div>
            <div class="s-resource-body">
              <template v-if="config.favicon">
                <code class="s-resource-id">ID: {{ config.favicon }}</code>
                <button class="s-resource-clear" @click="clearResource('favicon')">Remove</button>
              </template>
              <span v-else class="s-resource-hint">Click to choose from resources</span>
            </div>
          </div>
        </div>

        <div class="form-row">
          <label>Footer Text</label>
          <input v-model="config.footerText" class="input-ori" />
        </div>
      </section>

      <section class="form-section">
        <h3>SEO</h3>
        <div class="form-row">
          <label>Keywords</label>
          <input v-model="config.seo.keywords" class="input-ori" />
        </div>
        <div class="form-row">
          <label>Description</label>
          <input v-model="config.seo.description" class="input-ori" />
        </div>
      </section>

      <section class="form-section">
        <h3>Profile</h3>
        <div class="form-row">
          <label>Owner Name</label>
          <input v-model="config.profile.ownerName" class="input-ori" />
        </div>

        <!-- Avatar - pure picker -->
        <div class="form-row">
          <label>Avatar</label>
          <div class="s-resource-field">
            <div class="s-resource-thumb s-thumb-avatar" @click="openResourcePickerFor('avatar')">
              <AuthImage v-if="config.profile.avatar" :src="config.profile.avatar" class="s-resource-img" @error="clearResource('avatar')" />
              <span v-else class="s-resource-plus">+</span>
            </div>
            <div class="s-resource-body">
              <template v-if="config.profile.avatar">
                <code class="s-resource-id">ID: {{ config.profile.avatar }}</code>
                <button class="s-resource-clear" @click="clearResource('avatar')">Remove</button>
              </template>
              <span v-else class="s-resource-hint">Click to choose from resources</span>
            </div>
          </div>
        </div>

        <div class="form-row">
          <label>Signature</label>
          <input v-model="config.profile.signature" class="input-ori" />
        </div>
        <div class="form-row">
          <label>Bio</label>
          <textarea v-model="config.profile.bio" class="input-ori" rows="4" />
        </div>
      </section>

      <section class="form-section">
        <h3>Links</h3>
        <div v-for="(link, index) in config.links" :key="index" class="link-row">
          <input v-model="link.name" class="input-ori" placeholder="Name" />
          <input v-model="link.url" class="input-ori" placeholder="URL" />
          <select v-model="link.type" class="input-ori">
            <option value="social">Social</option>
            <option value="friend">Friend</option>
          </select>
          <div class="link-icon-field" @click="openLinkIconPicker(index)">
            <AuthImage v-if="link.icon" :src="link.icon" class="link-icon-img" />
            <span v-else class="link-icon-placeholder">icon</span>
          </div>
          <div class="link-actions">
            <button @click="moveLink(index, -1)" :disabled="index === 0">↑</button>
            <button @click="moveLink(index, 1)" :disabled="index === config.links.length - 1">↓</button>
            <button @click="removeLink(index)">×</button>
          </div>
        </div>
        <button class="btn-ori" @click="addLink">+ Add Link</button>
      </section>

      <section class="form-section">
        <h3>JSON Preview</h3>
        <pre class="json-preview font-mono">{{ previewJson }}</pre>
      </section>

      <div class="form-actions">
        <button class="btn-ori btn-ori-primary" :disabled="saving" @click="save">
          {{ saving ? 'Saving...' : 'Save' }}
        </button>
      </div>
    </div>

    <ResourcePicker ref="resourcePicker" @pick="onResourcePicked" />
    <ResourcePicker ref="linkIconPicker" @pick="onLinkIconPicked" />
  </div>
</template>

<style scoped>
.admin-settings .admin-header-ori {
  align-items: flex-start;
}

.admin-settings .sub {
  font-size: 13px;
  color: var(--ink-faint);
  margin-top: 8px;
  letter-spacing: 0.05em;
}

.message-ori {
  color: var(--accent);
  font-size: 13px;
  letter-spacing: 0.05em;
}

.settings-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-section {
  border: var(--border-w, 1px) solid var(--line);
  border-radius: var(--radius, 0);
  background: var(--panel, transparent);
  box-shadow: var(--shadow-soft, none);
  padding: 20px;
}

.form-section h3 {
  font-size: 12px;
  color: var(--ink-faint);
  letter-spacing: 0.15em;
  text-transform: uppercase;
  margin-bottom: 12px;
  font-weight: 500;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 10px;
}

.form-row:last-child {
  margin-bottom: 0;
}

.form-row label {
  font-size: 11px;
  color: var(--ink-light);
  letter-spacing: 0.05em;
}

/* ── Resource field ── */
.s-resource-field {
  display: flex; gap: 12px; align-items: center;
}

.s-resource-thumb {
  width: 48px; height: 48px;
  flex-shrink: 0;
  border: var(--border-w, 1px) dashed var(--line);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  background: var(--panel-2, var(--bg-warm));
  border-radius: var(--radius, 0);
  transition: border-color 0.2s;
}

.s-resource-thumb:hover {
  border-color: var(--accent);
}

.s-thumb-logo {
  /* rectangle */
}

.s-thumb-avatar {
  border-radius: 50%;
  overflow: hidden;
}

.s-resource-img {
  width: 100%; height: 100%; object-fit: cover;
}

.s-resource-plus {
  font-size: 20px; color: var(--ink-faint);
}

.s-resource-body {
  display: flex; flex-direction: column; gap: 4px;
}

.s-resource-id {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px; color: var(--ink-light);
  background: var(--panel-2, var(--bg-warm));
  padding: 2px 8px; border: var(--border-w, 1px) solid var(--line); border-radius: var(--radius-badge, 0);
}

.s-resource-hint {
  font-size: 11px; color: var(--ink-faint);
  letter-spacing: 0.04em;
}

.s-resource-clear {
  font-size: 10px; color: var(--ink-faint);
  background: none; border: none; cursor: pointer;
  padding: 0; letter-spacing: 0.05em; text-align: left;
}

.s-resource-clear:hover { color: var(--danger); }

.appearance-presets { display: flex; gap: 8px; flex-wrap: wrap; }
.ap-preset {
  display: flex; gap: 0; padding: 3px; cursor: pointer;
  background: var(--panel-2, transparent);
  border: var(--border-w, 1px) solid var(--line); border-radius: var(--radius, 4px);
}
.ap-preset.active { border-color: var(--accent); box-shadow: 0 0 0 1px var(--accent); }
.ap-dot { width: 18px; height: 18px; border-radius: 50%; display: block; }

.link-row {
  display: grid;
  grid-template-columns: 1fr 1.6fr 0.8fr 48px auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.link-icon-field {
  width: 36px; height: 36px;
  border: var(--border-w, 1px) dashed var(--line);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  background: var(--panel-2, var(--bg-warm));
  border-radius: var(--radius, 4px);
  transition: border-color 0.2s;
}

.link-icon-field:hover { border-color: var(--accent); }

.link-icon-img {
  width: 100%; height: 100%; object-fit: cover; border-radius: var(--radius, 3px);
}

.link-icon-placeholder {
  font-size: 9px; color: var(--ink-faint);
  letter-spacing: 0.06em; text-transform: uppercase;
}

.link-actions {
  display: flex;
  gap: 2px;
}

.link-actions button {
  padding: 2px 6px;
  font-size: 12px;
}

.json-preview {
  background: var(--panel-2, var(--bg));
  border: var(--border-w, 1px) solid var(--line);
  border-radius: var(--radius, 0);
  padding: 12px;
  font-size: 11px;
  line-height: 1.5;
  overflow-x: auto;
}

.font-mono {
  font-family: 'JetBrains Mono', 'SF Mono', 'Menlo', 'Consolas', 'Roboto Mono', monospace;
  font-weight: 500;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 768px) {
  .link-row {
    grid-template-columns: 1fr;
  }

  .s-resource-field {
    flex-direction: column; align-items: flex-start;
  }
}
</style>
