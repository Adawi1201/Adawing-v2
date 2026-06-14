<script setup>
import { ref, onMounted } from 'vue'
import { listNotes, getNote, saveNote } from '@/api/notes.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import ResourcePicker from '@/components/ResourcePicker.vue'
import { formatDate } from '@/utils/formatDate.js'

const TYPE_OPTIONS = ['PERSONAL', 'TECH']
const type = ref('PERSONAL')
const notes = ref([])
const loading = ref(false)
const form = ref({ id: null, title: '', content: '', type: 'PERSONAL' })
const saving = ref(false)
const emojiPicker = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listNotes(type.value, 1, 100)
    const data = res.data || res
    notes.value = data.list || []
  } finally { loading.value = false }
}

async function edit(id) {
  const res = await getNote(id)
  const data = res.data || res
  form.value = { id: data.id, title: data.title, content: data.content, type: data.type }
  type.value = data.type
}

function reset() {
  form.value = { id: null, title: '', content: '', type: type.value }
}

async function submit() {
  saving.value = true
  try {
    await saveNote({ ...form.value, type: type.value })
    reset()
    await load()
  } catch (e) {
    alert(e.message)
  } finally { saving.value = false }
}

function insertEmoji(r) {
  form.value.content = (form.value.content || '') + '\n![](' + resourceContentUrl(r.id) + ')\n'
}

onMounted(load)
</script>

<template>
  <div class="admin-moments">
    <div class="admin-header-ori">
      <h1>Moments</h1>
      <div style="display: flex; gap: 8px;">
        <button
          v-for="t in TYPE_OPTIONS"
          :key="t"
          class="btn-ori"
          :style="type === t ? 'background: var(--accent); color: #fff;' : ''"
          @click="type = t; load(); reset()"
        >
          {{ t }}
        </button>
      </div>
    </div>

    <section class="moment-form-ori">
      <h2>{{ form.id ? 'Edit' : 'New' }} Moment</h2>
      <input v-model="form.title" class="input-ori" placeholder="Title (optional)" />
      <textarea v-model="form.content" class="input-ori moment-textarea" rows="6" placeholder="Write a moment... Markdown supported." spellcheck="false" />
      <div class="moment-actions">
        <button class="btn-ori btn-ori-xs" @click="emojiPicker.open()">Insert Emoji</button>
        <div style="flex: 1;" />
        <button class="btn-ori btn-ori-sm btn-ori-primary" :disabled="saving" @click="submit">
          {{ saving ? 'Saving...' : 'Save' }}
        </button>
        <button v-if="form.id" class="btn-ori btn-ori-sm" @click="reset">Cancel</button>
      </div>
    </section>

    <div v-if="loading" class="loading-ori">Loading...</div>
    <div v-else class="moment-list-ori">
      <div v-for="note in notes" :key="note.id" class="moment-item-ori" @click="edit(note.id)">
        <div class="moment-title-ori">{{ note.title || '(no title)' }}</div>
        <div class="moment-content-ori" v-html="note.content" />
        <div class="moment-meta-ori">{{ formatDate(note.createTime) }}</div>
      </div>
      <div v-if="notes.length === 0" class="empty-ori">No moments yet.</div>
    </div>

    <ResourcePicker ref="emojiPicker" pool="EMOJI" @pick="insertEmoji" />
  </div>
</template>

<style scoped>
.moment-form-ori {
  margin-bottom: 36px;
  padding: 20px;
  border: 1px solid var(--line);
}

.moment-form-ori h2 {
  font-size: 1.1rem;
  margin-bottom: 20px;
  color: var(--ink);
}

.moment-form-ori .input-ori {
  margin-bottom: 14px;
}

.moment-textarea {
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 14px;
  line-height: 1.7;
  resize: vertical;
  min-height: 120px;
}

.moment-actions {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-top: 4px;
}

.moment-list-ori {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.moment-item-ori {
  padding: 32px 0;
  border-top: 1px solid var(--line);
  cursor: pointer;
  transition: padding-left 0.4s;
}

.moment-item-ori:hover {
  padding-left: 16px;
}

.moment-item-ori:hover .moment-title-ori {
  color: var(--accent);
}

.moment-title-ori {
  font-weight: 500;
  margin-bottom: 12px;
  transition: color 0.4s;
}

.moment-content-ori {
  color: var(--ink-light);
  margin-bottom: 12px;
  line-height: 1.8;
}

.moment-content-ori :deep(img) {
  max-width: 48px;
  max-height: 48px;
  vertical-align: middle;
}

.moment-meta-ori {
  font-size: 11px;
  color: var(--ink-faint);
  letter-spacing: 0.1em;
}
</style>
