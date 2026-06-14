<script setup>
import { ref, onMounted, watch } from 'vue'
import { listTags, createTag, suggestTags, mergeTags } from '@/api/tags.js'

const tags = ref([])
const loading = ref(false)
const newTag = ref({ name: '', description: '', color: '' })
const suggestions = ref([])
const sourceId = ref('')
const targetId = ref('')
const merging = ref(false)
const suggestTimer = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listTags()
    tags.value = res.data || res
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!newTag.value.name) return
  await createTag(newTag.value)
  newTag.value = { name: '', description: '', color: '' }
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

async function doMerge() {
  if (!sourceId.value || !targetId.value) return
  if (sourceId.value === targetId.value) {
    alert('Cannot merge a tag into itself')
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
    alert(e.message)
  } finally {
    merging.value = false
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
        <input v-model="newTag.color" class="input-ori" style="width: 120px;" placeholder="Color" />
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
      <span v-for="tag in tags" :key="tag.id" class="tag-pill-ori">
        {{ tag.name }}
        <small v-if="tag.articleCount">({{ tag.articleCount }})</small>
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
  align-items: flex-end;
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
</style>
