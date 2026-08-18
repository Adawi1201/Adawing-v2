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

function emptyAbout() {
  return {
    pin: { ownerName: '', avatar: '', signature: '', job: '', unit: '', experience: [] },
    ability: { devStack: [] },
    contact: { email: '', otherSocialPlatform: [] },
    links: { items: [] },
    siteInfo: { license: { enabled: false, name: '', url: '' } },
    siteContent: { intro: '' }
  }
}

function defaultConfig() {
  return {
    name: '', description: '', subtitle: '', logo: '', favicon: '', icp: '',
    publicSecurityRecord: '', footerText: '',
    seo: { keywords: '', description: '' },
    about: emptyAbout()
  }
}

function normalizeConfig(raw) {
  const source = raw?.data || raw || {}
  const base = defaultConfig()
  const aboutSource = source.about || {}
  const pinSource = aboutSource.pin || {}
  const abilitySource = aboutSource.ability || {}
  const contactSource = aboutSource.contact || {}
  const linksSource = aboutSource.links || {}
  const siteInfoSource = aboutSource.siteInfo || {}
  const licenseSource = siteInfoSource.license || {}
  const contentSource = aboutSource.siteContent || {}

  const ability = {
    devStack: Array.isArray(abilitySource.devStack) ? abilitySource.devStack.map(item => item && typeof item === 'object' ? item : {}) : []
  }
  const siteContent = { intro: contentSource.intro || '' }

  const result = {
    ...base,
    ...source,
    seo: { ...base.seo, ...(source.seo || {}) },
    about: {
      pin: {
        ...base.about.pin,
        ...pinSource,
        experience: Array.isArray(pinSource.experience) ? pinSource.experience.map(item => item && typeof item === 'object' ? item : {}) : []
      },
      ability,
      contact: {
        ...base.about.contact,
        ...contactSource,
        otherSocialPlatform: Array.isArray(contactSource.otherSocialPlatform) ? contactSource.otherSocialPlatform.map(item => item && typeof item === 'object' ? item : {}) : []
      },
      links: {
        ...base.about.links,
        ...linksSource,
        items: Array.isArray(linksSource.items) ? linksSource.items.map(item => item && typeof item === 'object' ? item : {}) : []
      },
      siteInfo: { ...base.about.siteInfo, ...siteInfoSource, license: { ...base.about.siteInfo.license, ...licenseSource } },
      siteContent
    }
  }

  return result
}

const config = ref(defaultConfig())
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const previewJson = computed(() => JSON.stringify(config.value, null, 2))

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

async function load() {
  loading.value = true
  try {
    config.value = normalizeConfig(await getSiteConfig())
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
let pickTarget = null

function openResourcePickerFor(target, usage = 'icon', title = 'Choose Icon') {
  pickTarget = target
  resourcePicker.value.open({ usage, title })
}

function assignResource(id) {
  if (pickTarget === 'logo') config.value.logo = id
  else if (pickTarget === 'favicon') config.value.favicon = id
  else if (pickTarget === 'avatar') config.value.about.pin.avatar = id
  else if (pickTarget?.kind === 'experience') config.value.about.pin.experience[pickTarget.index].icon = id
  else if (pickTarget?.kind === 'devStack') config.value.about.ability.devStack[pickTarget.index].icon = id
  else if (pickTarget?.kind === 'social') config.value.about.contact.otherSocialPlatform[pickTarget.index].icon = id
  else if (pickTarget?.kind === 'link') config.value.about.links.items[pickTarget.index].icon = id
  pickTarget = null
}

function onResourcePicked(resource) { assignResource(String(resource.id)) }

function clearResource(target) {
  if (target === 'logo') config.value.logo = ''
  else if (target === 'favicon') config.value.favicon = ''
  else if (target === 'avatar') config.value.about.pin.avatar = ''
  else if (target?.kind === 'experience') config.value.about.pin.experience[target.index].icon = ''
  else if (target?.kind === 'devStack') config.value.about.ability.devStack[target.index].icon = ''
  else if (target?.kind === 'social') config.value.about.contact.otherSocialPlatform[target.index].icon = ''
  else if (target?.kind === 'link') config.value.about.links.items[target.index].icon = ''
}

const factories = {
  experience: () => ({ period: '', title: '', unit: '', description: '', icon: '' }),
  devStack: () => ({ name: '', category: '', icon: '' }),
  social: () => ({ name: '', url: '', icon: '' }),
  link: () => ({ section: '', name: '', url: '', description: '', icon: '' }),
}

function listFor(kind) {
  const about = config.value.about
  if (kind === 'experience') return about.pin.experience
  if (kind === 'devStack') return about.ability.devStack
  if (kind === 'social') return about.contact.otherSocialPlatform
  if (kind === 'link') return about.links.items
}

function addItem(kind) { listFor(kind).push(factories[kind]()) }
function removeItem(kind, index) { listFor(kind).splice(index, 1) }
function moveItem(kind, index, direction) {
  const list = listFor(kind)
  const next = index + direction
  if (next < 0 || next >= list.length) return
  const item = list.splice(index, 1)[0]
  list.splice(next, 0, item)
}

onMounted(load)
</script>

<template>
  <div class="admin-settings">
    <div class="admin-header-ori">
      <div>
        <h1>Settings</h1>
        <p class="sub">Manage site configuration, About content and display order.</p>
      </div>
      <span v-if="message" class="message-ori">{{ message }}</span>
    </div>

    <div v-if="loading" class="loading-ori">Loading...</div>

    <div v-else class="settings-form">
      <section class="form-section">
        <h3>01 / 外观 · Appearance</h3>
        <div class="form-row"><label>主题风格</label><select class="input-ori" :value="theme.palette" @change="onPaletteChange"><option v-for="p in PALETTE_OPTIONS" :key="p.name" :value="p.name">{{ p.label }}</option></select></div>
        <div v-if="theme.palette === 'custom'" class="form-row"><label>预设配色</label><div class="appearance-presets"><button v-for="(c, i) in CUSTOM_PRESETS" :key="i" class="ap-preset" :class="{ active: theme.custom && theme.custom.preset === i }" @click.prevent="pickPreset(i)"><span class="ap-dot" :style="{ background: swatchBg(i) }"></span><span class="ap-dot" :style="{ background: c.accent }"></span></button></div></div>
      </section>

      <section class="form-section">
        <h3>02 / 站点信息 · Site Info</h3>
        <div class="form-row"><label>Site Name</label><input v-model="config.name" class="input-ori" /></div>
        <div class="form-row"><label>Site Description</label><textarea v-model="config.description" class="input-ori" rows="3" /></div>
        <div class="form-row"><label>Subtitle</label><textarea v-model="config.subtitle" class="input-ori" rows="2" /></div>
        <div class="form-row"><label>Footer Text</label><input v-model="config.footerText" class="input-ori" /></div>
        <div class="form-row"><label>ICP</label><input v-model="config.icp" class="input-ori" /></div>
        <div class="form-row"><label>Public Security Record</label><input v-model="config.publicSecurityRecord" class="input-ori" /></div>

        <div class="form-row"><label>Logo</label><div class="s-resource-field"><div class="s-resource-thumb s-thumb-logo" @click="openResourcePickerFor('logo', 'icon', 'Choose Logo')"><AuthImage v-if="config.logo" :src="config.logo" class="s-resource-img" @error="clearResource('logo')" /><span v-else class="s-resource-plus">+</span></div><div class="s-resource-body"><code v-if="config.logo" class="s-resource-id">ID: {{ config.logo }}</code><button v-if="config.logo" class="s-resource-clear" @click="clearResource('logo')">Remove</button><span v-else class="s-resource-hint">Click to choose from resources</span></div></div></div>
        <div class="form-row"><label>Favicon</label><div class="s-resource-field"><div class="s-resource-thumb" @click="openResourcePickerFor('favicon', 'icon', 'Choose Favicon')"><AuthImage v-if="config.favicon" :src="config.favicon" class="s-resource-img" @error="clearResource('favicon')" /><span v-else class="s-resource-plus">+</span></div><div class="s-resource-body"><code v-if="config.favicon" class="s-resource-id">ID: {{ config.favicon }}</code><button v-if="config.favicon" class="s-resource-clear" @click="clearResource('favicon')">Remove</button><span v-else class="s-resource-hint">Click to choose from resources</span></div></div></div>

        <div class="subsection-heading">内容授权 / License</div>
        <div class="form-row form-row-inline"><label>启用 CC 授权</label><input v-model="config.about.siteInfo.license.enabled" type="checkbox" /></div>
        <template v-if="config.about.siteInfo.license.enabled"><div class="form-row"><label>许可证名称</label><input v-model="config.about.siteInfo.license.name" class="input-ori" placeholder="CC BY-NC-SA 4.0" /></div><div class="form-row"><label>许可证 URL</label><input v-model="config.about.siteInfo.license.url" class="input-ori" placeholder="https://creativecommons.org/licenses/" /></div></template>
      </section>

      <section class="form-section">
        <h3>03 / 站点内容 · Site Content</h3>
        <div class="form-row"><label>内容介绍 / Bio</label><textarea v-model="config.about.siteContent.intro" class="input-ori" rows="5" placeholder="介绍自己，以及这个网站记录的内容。" /></div>
      </section>

      <section class="form-section">
        <h3>04 / 身份与经历 · Pin</h3>
        <div class="form-row"><label>Owner Name</label><input v-model="config.about.pin.ownerName" class="input-ori" /></div>
        <div class="form-row"><label>Job</label><input v-model="config.about.pin.job" class="input-ori" /></div>
        <div class="form-row"><label>Unit</label><input v-model="config.about.pin.unit" class="input-ori" /></div>
        <div class="form-row"><label>Signature</label><input v-model="config.about.pin.signature" class="input-ori" /></div>
        <div class="form-row"><label>Avatar</label><div class="s-resource-field"><div class="s-resource-thumb s-thumb-avatar" @click="openResourcePickerFor('avatar', 'avatar', 'Choose Avatar')"><AuthImage v-if="config.about.pin.avatar" :src="config.about.pin.avatar" class="s-resource-img" @error="clearResource('avatar')" /><span v-else class="s-resource-plus">+</span></div><div class="s-resource-body"><code v-if="config.about.pin.avatar" class="s-resource-id">ID: {{ config.about.pin.avatar }}</code><button v-if="config.about.pin.avatar" class="s-resource-clear" @click="clearResource('avatar')">Remove</button><span v-else class="s-resource-hint">Click to choose from resources</span></div></div></div>
        <div class="subsection-heading">经历 / Experience</div>
        <div v-for="(item, index) in config.about.pin.experience" :key="`experience-${index}`" class="repeatable-item"><div class="repeatable-grid repeatable-grid-wide repeatable-grid-experience"><input v-model="item.period" class="input-ori" placeholder="时间" /><input v-model="item.title" class="input-ori" placeholder="职位 / 项目" /><input v-model="item.unit" class="input-ori" placeholder="单位" /><div class="inline-resource" @click="openResourcePickerFor({ kind: 'experience', index })"><AuthImage v-if="item.icon" :src="item.icon" class="inline-icon" /><span v-else>icon</span></div><textarea v-model="item.description" class="input-ori experience-description" rows="4" placeholder="经历说明" /></div><div class="repeatable-actions"><button @click="moveItem('experience', index, -1)" :disabled="index === 0">↑</button><button @click="moveItem('experience', index, 1)" :disabled="index === config.about.pin.experience.length - 1">↓</button><button @click="removeItem('experience', index)">×</button></div></div>
        <button class="btn-ori" @click="addItem('experience')">+ 添加经历</button>
      </section>

      <section class="form-section">
        <h3>05 / 能力 · Ability</h3>
        <div class="subsection-heading">开发技术栈 / Dev Stack</div>
        <div v-for="(item, index) in config.about.ability.devStack" :key="`dev-${index}`" class="repeatable-item"><div class="repeatable-grid"><input v-model="item.name" class="input-ori" placeholder="技术名称" /><input v-model="item.category" class="input-ori" placeholder="分类，如 Language / Framework" /><div class="inline-resource" @click="openResourcePickerFor({ kind: 'devStack', index })"><AuthImage v-if="item.icon" :src="item.icon" class="inline-icon" /><span v-else>icon</span></div></div><div class="repeatable-actions"><button @click="moveItem('devStack', index, -1)" :disabled="index === 0">↑</button><button @click="moveItem('devStack', index, 1)" :disabled="index === config.about.ability.devStack.length - 1">↓</button><button @click="removeItem('devStack', index)">×</button></div></div>
        <button class="btn-ori" @click="addItem('devStack')">+ 添加技术</button>
      </section>

      <section class="form-section">
        <h3>06 / 联系方式 · Contact</h3>
        <div class="form-row"><label>Email</label><input v-model="config.about.contact.email" class="input-ori" placeholder="mailto@example.com" /></div>
        <div class="subsection-heading">其它联系方式 / Other Contacts</div>
        <div v-for="(item, index) in config.about.contact.otherSocialPlatform" :key="`social-${index}`" class="repeatable-item"><div class="repeatable-grid"><input v-model="item.name" class="input-ori" placeholder="平台名称" /><input v-model="item.url" class="input-ori" placeholder="URL" /><div class="inline-resource" @click="openResourcePickerFor({ kind: 'social', index })"><AuthImage v-if="item.icon" :src="item.icon" class="inline-icon" /><span v-else>icon</span></div></div><div class="repeatable-actions"><button @click="moveItem('social', index, -1)" :disabled="index === 0">↑</button><button @click="moveItem('social', index, 1)" :disabled="index === config.about.contact.otherSocialPlatform.length - 1">↓</button><button @click="removeItem('social', index)">×</button></div></div>
        <button class="btn-ori" @click="addItem('social')">+ 添加平台</button>
      </section>

      <section class="form-section">
        <h3>07 / 外部链接 · Links</h3>
        <div class="subsection-heading">链接列表 / Link Items</div>
        <p class="section-hint">分组只是展示标题，可填写任意名称；链接类型不做预设限制。</p>
        <div v-for="(item, index) in config.about.links.items" :key="`link-${index}`" class="repeatable-item"><div class="repeatable-grid repeatable-grid-link"><input v-model="item.section" class="input-ori" placeholder="展示分组（可选）" /><input v-model="item.name" class="input-ori" placeholder="链接名称" /><input v-model="item.url" class="input-ori" placeholder="URL" /><input v-model="item.description" class="input-ori" placeholder="说明（可选）" /><button type="button" class="inline-resource" title="选择链接图标" aria-label="选择链接图标" @click="openResourcePickerFor({ kind: 'link', index })"><AuthImage v-if="item.icon" :src="item.icon" class="inline-icon" /><span v-else>+</span></button></div><div class="repeatable-actions"><button @click="moveItem('link', index, -1)" :disabled="index === 0">↑</button><button @click="moveItem('link', index, 1)" :disabled="index === config.about.links.items.length - 1">↓</button><button @click="removeItem('link', index)">×</button></div></div>
        <button class="btn-ori" @click="addItem('link')">+ 添加链接</button>

      </section>

      <section class="form-section">
        <h3>08 / 搜索优化 · SEO</h3>
        <div class="form-row"><label>Keywords</label><input v-model="config.seo.keywords" class="input-ori" /></div>
        <div class="form-row"><label>Description</label><input v-model="config.seo.description" class="input-ori" /></div>
      </section>

      <section class="form-section"><h3>09 / JSON Preview</h3><pre class="json-preview font-mono">{{ previewJson }}</pre></section>

      <div class="form-actions"><button class="btn-ori btn-ori-primary" :disabled="saving" @click="save">{{ saving ? 'Saving...' : 'Save' }}</button></div>
    </div>

    <ResourcePicker ref="resourcePicker" @pick="onResourcePicked" />
  </div>
</template>

<style scoped>
.admin-settings .admin-header-ori { align-items: flex-start; }
.admin-settings .sub { font-size: 13px; color: var(--ink-faint); margin-top: 8px; letter-spacing: 0.05em; }
.message-ori { color: var(--accent); font-size: 13px; letter-spacing: 0.05em; }
.settings-form { display: flex; flex-direction: column; gap: 20px; }
.form-section { border: var(--border-w, 1px) solid var(--line); border-radius: var(--radius, 0); background: var(--panel, transparent); box-shadow: var(--shadow-soft, none); padding: 20px; }
.form-section h3 { font-size: 12px; color: var(--ink-faint); letter-spacing: 0.15em; text-transform: uppercase; margin-bottom: 12px; font-weight: 500; }
.form-row { display: flex; flex-direction: column; gap: 4px; margin-bottom: 10px; }
.form-row:last-child { margin-bottom: 0; }
.form-row label { font-size: 11px; color: var(--ink-light); letter-spacing: 0.05em; }
.form-row-inline { flex-direction: row; align-items: center; gap: 10px; }
.form-row-inline input { width: 16px; height: 16px; accent-color: var(--accent); }
.subsection-heading { margin: 20px 0 8px; color: var(--ink-faint); font-size: 11px; letter-spacing: .12em; text-transform: uppercase; }
.section-hint { margin: -2px 0 12px; color: var(--ink-faint); font-size: 12px; line-height: 1.7; }
.s-resource-field { display: flex; gap: 12px; align-items: center; }
.s-resource-thumb { width: 48px; height: 48px; flex-shrink: 0; border: var(--border-w, 1px) dashed var(--line); cursor: pointer; display: flex; align-items: center; justify-content: center; background: var(--panel-2, var(--bg-warm)); border-radius: var(--radius, 0); transition: border-color .2s; }
.s-resource-thumb:hover { border-color: var(--accent); }
.s-thumb-avatar { border-radius: 50%; overflow: hidden; }
.s-resource-img { width: 100%; height: 100%; object-fit: cover; }
.s-resource-plus { font-size: 20px; color: var(--ink-faint); }
.s-resource-body { display: flex; flex-direction: column; gap: 4px; }
.s-resource-id { font-family: 'JetBrains Mono', monospace; font-size: 11px; color: var(--ink-light); background: var(--panel-2, var(--bg-warm)); padding: 2px 8px; border: var(--border-w, 1px) solid var(--line); border-radius: var(--radius-badge, 0); }
.s-resource-hint { font-size: 11px; color: var(--ink-faint); letter-spacing: .04em; }
.s-resource-clear { font-size: 10px; color: var(--ink-faint); background: none; border: none; cursor: pointer; padding: 0; letter-spacing: .05em; text-align: left; }
.s-resource-clear:hover { color: var(--danger); }
.appearance-presets { display: flex; gap: 8px; flex-wrap: wrap; }
.ap-preset { display: flex; gap: 0; padding: 3px; cursor: pointer; background: var(--panel-2, transparent); border: var(--border-w, 1px) solid var(--line); border-radius: var(--radius, 4px); }
.ap-preset.active { border-color: var(--accent); box-shadow: 0 0 0 1px var(--accent); }
.ap-dot { width: 18px; height: 18px; border-radius: 50%; display: block; }
.repeatable-item { display: flex; gap: 8px; align-items: flex-start; margin-bottom: 9px; padding: 9px; border: var(--border-w, 1px) solid var(--line); border-radius: var(--radius, 0); background: var(--panel-2, transparent); box-shadow: var(--shadow-soft, none); font-family: var(--font-body); }
.repeatable-grid { flex: 1; min-width: 0; display: grid; grid-template-columns: minmax(0, 1fr) minmax(0, 1.4fr) 38px; gap: 8px; align-items: center; }
.repeatable-grid-wide { grid-template-columns: minmax(90px, .7fr) minmax(0, 1fr) minmax(0, 1fr) 38px; }
.repeatable-grid-experience .experience-description { grid-column: 1 / -1; }
.repeatable-grid-link { grid-template-columns: minmax(100px, .8fr) minmax(0, 1fr) minmax(0, 1.3fr) minmax(0, 1.2fr) 38px; }
.repeatable-grid textarea { resize: vertical; }
.repeatable-actions { display: flex; gap: 2px; }
.repeatable-actions button { padding: 2px 6px; font-size: 12px; }
.inline-resource { width: 36px; height: 36px; padding: 0; border: var(--border-w, 1px) dashed var(--line); border-radius: var(--radius-badge, 0); background: var(--panel); cursor: pointer; display: flex; align-items: center; justify-content: center; color: var(--ink-faint); font-size: 14px; }
.inline-resource:hover { border-color: var(--accent); color: var(--accent); }
.inline-icon { width: 100%; height: 100%; object-fit: cover; }
.json-preview { background: var(--panel-2, var(--bg)); border: var(--border-w, 1px) solid var(--line); border-radius: var(--radius, 0); padding: 12px; font-size: 11px; line-height: 1.5; overflow-x: auto; max-height: 480px; }
.font-mono { font-family: 'JetBrains Mono', 'SF Mono', 'Menlo', 'Consolas', 'Roboto Mono', monospace; font-weight: 500; }
.form-actions { display: flex; justify-content: flex-end; }
@media (max-width: 768px) {
  .repeatable-item { flex-direction: column; }
  .repeatable-grid, .repeatable-grid-wide { grid-template-columns: 1fr; }
  .repeatable-actions { width: 100%; justify-content: flex-end; }
  .s-resource-field { flex-direction: column; align-items: flex-start; }
}
</style>
