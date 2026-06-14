<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAdmin, saveArticle } from '@/api/articles.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import { toast } from '@/utils/toast.js'
import ResourcePicker from '@/components/ResourcePicker.vue'

const route = useRoute()
const router = useRouter()
const isNew = !route.params.id

const saving = ref(false)
const form = ref({ id: null, title: '', summary: '', content: '', coverResourceId: null, tagNames: [] })
const tagInput = ref('')
const coverPicker = ref(null)
const contentPicker = ref(null)

async function loadArticle() {
  if (isNew) return
  try {
    const res = await getAdmin(route.params.id)
    const article = res.data || res
    form.value = {
      id: article.id, title: article.title || '', summary: article.summary || '',
      content: article.content || '', coverResourceId: article.coverResourceId, tagNames: []
    }
  } catch (e) { toast(e.message, 'error') }
}

async function submit() {
  saving.value = true
  try {
    const data = {
      id: form.value.id, title: form.value.title, summary: form.value.summary,
      content: form.value.content,
      coverResourceId: form.value.coverResourceId,
      tagNames: tagInput.value ? tagInput.value.split(',').map(t => t.trim()).filter(Boolean) : []
    }
    await saveArticle(data)
    router.push({ name: 'AdminArticles' })
  } catch (e) { toast(e.message, 'error') }
  finally { saving.value = false }
}

function removeCover() { form.value.coverResourceId = null }

function insertContentRef(r) {
  form.value.content += `\n![](${resourceContentUrl(r.id)})\n`
}

function goBack() { router.push({ name: 'AdminArticles' }) }

onMounted(() => { loadArticle() })
</script>

<template>
  <div class="article-editor">
    <div class="ae-header">
      <button class="btn-ori btn-ori-sm" @click="goBack">← Back</button>
      <h1>{{ isNew ? 'New Article' : 'Edit Article' }}</h1>
      <div class="spacer" />
      <button class="btn-ori btn-ori-sm btn-ori-primary" :disabled="saving" @click="submit">
        {{ saving ? 'Saving...' : 'Save' }}
      </button>
    </div>

    <div class="ae-form">
      <div class="ae-fields">
        <input v-model="form.title" class="input-ori" placeholder="Title" />

        <div class="ae-cover-row">
          <div class="ae-cover-thumb" @click="coverPicker.open()">
            <img v-if="form.coverResourceId" :src="resourceContentUrl(form.coverResourceId)" />
            <span v-else>+</span>
          </div>
          <div class="ae-cover-info">
            <span class="ae-label">Cover</span>
            <button v-if="form.coverResourceId" class="btn-ori btn-ori-xs" @click="removeCover">Remove</button>
          </div>
        </div>

        <textarea v-model="form.summary" class="input-ori" rows="2" placeholder="Summary (optional)" />

        <div class="ae-body-area">
          <div class="ae-body-label">
            <span>Body <span style="font-weight:400;font-size:11px;color:var(--ink-faint)">— Markdown</span></span>
            <button class="btn-ori btn-ori-xs" @click="contentPicker.open()">Insert Image</button>
          </div>
          <textarea
            v-model="form.content"
            class="ae-textarea"
            rows="20"
            placeholder="Start writing Markdown..."
            spellcheck="false"
          ></textarea>
          <div class="ae-body-footer">
            <span>{{ (form.content || '').length }} chars</span>
            <span style="color:var(--ink-faint)">—</span>
            <span>Markdown</span>
          </div>
        </div>

        <input v-model="tagInput" class="input-ori" placeholder="Tags (comma separated)" />
      </div>
    </div>

    <ResourcePicker ref="coverPicker" @pick="r => form.coverResourceId = r.id" />
    <ResourcePicker ref="contentPicker" @pick="r => insertContentRef(r)" />
  </div>
</template>

<style scoped>
.article-editor { padding-bottom: 60px; }

.ae-header {
  display: flex; align-items: center; gap: 16px;
  padding-bottom: 24px; border-bottom: 1px solid var(--line);
  margin-bottom: 28px;
}
.ae-header h1 { font-size: 16px; font-weight: 500; color: var(--ink); }
.spacer { flex: 1; }

.ae-form { max-width: 860px; }
.ae-fields { display: flex; flex-direction: column; gap: 20px; }

.ae-cover-row { display: flex; gap: 14px; align-items: center; }
.ae-cover-thumb {
  width: 100px; height: 66px; border: 1px dashed var(--line);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  background: var(--bg-warm); transition: border-color 0.2s;
  flex-shrink: 0;
}
.ae-cover-thumb:hover { border-color: var(--accent); }
.ae-cover-thumb img { width: 100%; height: 100%; object-fit: cover; }
.ae-cover-thumb span { font-size: 22px; color: var(--ink-faint); }
.ae-cover-info { display: flex; flex-direction: column; gap: 4px; }
.ae-label { font-size: 11px; color: var(--ink-faint); letter-spacing: 0.05em; text-transform: uppercase; font-weight: 500; }

.ae-body-area { border: 1px solid var(--line); }
.ae-body-label {
  display: flex; align-items: center; justify-content: space-between;
  padding: 10px 14px; background: var(--bg-warm);
  border-bottom: 1px solid var(--line);
  font-size: 12px; font-weight: 500; color: var(--ink);
}
.ae-textarea {
  width: 100%; padding: 16px 18px; border: none; outline: none;
  background: var(--bg); color: var(--ink);
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  font-size: 14px; line-height: 1.8; resize: vertical;
}
.ae-textarea::placeholder { color: var(--ink-faint); }
.ae-body-footer {
  display: flex; gap: 8px; align-items: center;
  padding: 6px 14px; background: var(--bg-warm);
  border-top: 1px solid var(--line);
  font-size: 10px; color: var(--ink-faint);
}
</style>
