<script setup>
import { ref, onMounted, watch } from 'vue'
import { listTagsWithCount, createTag, suggestTags, mergeTags, deleteTag } from '@/api/tags.js'
import { toast } from '@/utils/toast.js'

const tags = ref([])
const loading = ref(false)
const newTag = ref({ name: '', description: '', color: '' })
const useColor = ref(false)
const suggestions = ref([])
const sourceId = ref('')
const targetId = ref('')
const merging = ref(false)
const suggestTimer = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listTagsWithCount()
    tags.value = res.data || res
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!newTag.value.name) return
  const payload = {
    name: newTag.value.name,
    description: newTag.value.description,
    color: useColor.value ? newTag.value.color : null
  }
  await createTag(payload)
  newTag.value = { name: '', description: '', color: '' }
  useColor.value = false
  suggestions.value = []
  await load()
}

async function fetchSuggestions() {
  if (!newTag.value.name) {
    suggestions.value = []
    return
  }
  try {
    const res = await suggestTags(newTag.value.name)
    suggestions.value = res.data || res || []
  } catch {
    suggestions.value = []
  }
}

function debouncedFetchSuggestions() {
  if (suggestTimer.value) clearTimeout(suggestTimer.value)
  if (!newTag.value.name) {
    suggestions.value = []
    return
  }
  suggestTimer.value = setTimeout(fetchSuggestions, 300)
}

function pickColor(e) {
  newTag.value.color = e.target.value
  useColor.value = true
}
function clearColor() {
  newTag.value.color = ''
  useColor.value = false
}

async function doMerge() {
  if (!sourceId.value || !targetId.value) return
  if (sourceId.value === targetId.value) {
    toast('Cannot merge a tag into itself', 'warn')
    return
  }
  if (!confirm('The source tag will be deleted. Continue?')) return
  merging.value = true
  try {
    await mergeTags(sourceId.value, targetId.value)
    sourceId.value = ''
    targetId.value = ''
    await load()
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    merging.value = false
  }
}

async function doDelete(tag) {
  if (!confirm(`Delete tag "${tag.name}"?`)) return
  try {
    await deleteTag(tag.id)
    await load()
  } catch (e) {
    toast(e.message, 'error')
  }
}

watch(() => newTag.value.name, debouncedFetchSuggestions)

onMounted(load)
</script>

<template>
  <div class="admin-tags">
    <div class="admin-header-ori">
      <h1>Tags</h1>
    </div>

    <section class="tag-section-ori">
      <h2>New Tag</h2>
      <div class="tag-form-row">
        <input v-model="newTag.name" class="input-ori" style="flex: 1; min-width: 140px;" placeholder="Name" />
        <input v-model="newTag.description" class="input-ori" style="flex: 2; min-width: 200px;" placeholder="Description" />
        <div class="color-field">
          <input type="color" :value="newTag.color || '#B87333'" class="color-input" @input="pickColor" />
          <span class="color-hex">{{ useColor ? newTag.color : '(none)' }}</span>
          <button v-if="useColor" class="color-clear" @click="clearColor">clear</button>
        </div>
        <button class="btn-ori btn-ori-primary" @click="submit">Create</button>
      </div>
      <div v-if="suggestions.length" class="suggest-ori">
        Similar tags:
        <span v-for="tag in suggestions" :key="tag.id" class="tag-pill-ori">{{ tag.name }}</span>
      </div>
    </section>

    <section class="tag-section-ori">
      <h2>Merge Tags</h2>
      <div class="merge-row-ori">
        <select v-model="sourceId" class="input-ori">
          <option value="">Source tag</option>
          <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
        </select>
        <span class="merge-arrow">→</span>
        <select v-model="targetId" class="input-ori">
          <option value="">Target tag</option>
          <option v-for="tag in tags" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
        </select>
        <button class="btn-ori" :disabled="merging" @click="doMerge">Merge</button>
      </div>
    </section>

    <div v-if="loading" class="loading-ori">Loading...</div>
    <div v-else class="tag-list-ori">
      <span v-for="tag in tags" :key="tag.id" class="tag-chip">
        <span v-if="tag.color" class="tag-dot" :style="{ background: tag.color }"></span>
        {{ tag.name }}
        <small v-if="tag.articleCount">({{ tag.articleCount }})</small>
        <button class="tag-del" title="Delete" @click="doDelete(tag)">×</button>
      </span>
      <span v-if="tags.length === 0" class="empty-ori" style="padding: 0;">No tags yet.</span>
    </div>
  </div>
</template>

<style scoped>
.tag-section-ori {
  margin-bottom: 36px;
}

.tag-section-ori h2 {
  font-size: 1.2rem;
  margin-bottom: 24px;
  color: var(--ink);
}

.tag-form-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.color-field {
  display: flex;
  align-items: center;
  gap: 8px;
}
.color-input {
  width: 42px;
  height: 36px;
  border: 1px solid var(--line);
  border-radius: 3px;
  background: var(--bg);
  cursor: pointer;
  padding: 2px;
}
.color-hex {
  font-size: 12px;
  color: var(--ink-faint);
  font-family: monospace;
  min-width: 56px;
}
.color-clear {
  font-size: 11px;
  color: var(--ink-faint);
  border: none;
  background: transparent;
  cursor: pointer;
  text-decoration: underline;
}

.suggest-ori {
  margin-top: 16px;
  font-size: 13px;
  color: var(--ink-light);
}

.merge-row-ori {
  display: flex;
  gap: 16px;
  align-items: center;
  flex-wrap: wrap;
}

.merge-row-ori select {
  width: 180px;
}

.merge-arrow {
  color: var(--ink-faint);
  font-size: 14px;
}

.tag-list-ori {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 5px 10px;
  border: 1px solid var(--line);
  border-radius: 4px;
  font-size: 13px;
  color: var(--ink);
}
.tag-chip small { color: var(--ink-faint); }
.tag-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  flex-shrink: 0;
}
.tag-del {
  border: none;
  background: transparent;
  color: var(--ink-faint);
  cursor: pointer;
  font-size: 15px;
  line-height: 1;
  padding: 0 2px;
}
.tag-del:hover { color: var(--accent); }
</style>
