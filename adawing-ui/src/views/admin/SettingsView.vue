<script setup>
import { ref, onMounted, computed } from 'vue'
import { getSiteConfig, saveSiteConfig } from '@/api/config.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import ResourcePicker from '@/components/ResourcePicker.vue'

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
  resourcePicker.value.open()
}

function openLinkIconPicker(index) {
  pendingLinkIndex = index
  linkIconPicker.value.open()
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
              <img v-if="config.logo" :src="resourceContentUrl(config.logo)" class="s-resource-img" @error="clearResource('logo')" />
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
              <img v-if="config.favicon" :src="resourceContentUrl(config.favicon)" class="s-resource-img" @error="clearResource('favicon')" />
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
              <img v-if="config.profile.avatar" :src="resourceContentUrl(config.profile.avatar)" class="s-resource-img" @error="clearResource('avatar')" />
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
            <img v-if="link.icon" :src="resourceContentUrl(link.icon)" class="link-icon-img" />
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
  border: 1px solid var(--line);
  padding: 16px;
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
  border: 1px dashed var(--line);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-warm);
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
  background: var(--bg-warm);
  padding: 2px 8px; border: 1px solid var(--line);
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

.s-resource-clear:hover { color: #c45c5c; }

.link-row {
  display: grid;
  grid-template-columns: 1fr 1.6fr 0.8fr 48px auto;
  gap: 8px;
  align-items: center;
  margin-bottom: 8px;
}

.link-icon-field {
  width: 36px; height: 36px;
  border: 1px dashed var(--line);
  cursor: pointer;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-warm);
  border-radius: 4px;
  transition: border-color 0.2s;
}

.link-icon-field:hover { border-color: var(--accent); }

.link-icon-img {
  width: 100%; height: 100%; object-fit: cover; border-radius: 3px;
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
  background: var(--bg);
  border: 1px solid var(--line);
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
