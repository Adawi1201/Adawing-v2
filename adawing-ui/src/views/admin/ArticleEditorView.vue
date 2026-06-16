<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Vditor from 'vditor'
import { getAdmin, saveArticle } from '@/api/articles.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import { resourceReferenceImage, restoreResourceReferences } from '@/utils/resourceRef.js'
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
const editorRef = ref(null)
let vditor = null

function syncContentFromEditor() {
  if (vditor) {
    form.value.content = vditor.getValue()
  }
}

function initEditor() {
  if (!editorRef.value) return
  vditor = new Vditor(editorRef.value, {
    mode: 'ir',
    minHeight: 520,
    cache: { enable: false },
    placeholder: 'Start writing Markdown...',
    toolbar: [
      'headings',
      'bold',
      'italic',
      'strike',
      '|',
      'list',
      'ordered-list',
      'check',
      'outdent',
      'indent',
      '|',
      'quote',
      'line',
      'code',
      'inline-code',
      'table',
      'link',
      '|',
      'undo',
      'redo',
      '|',
      'edit-mode',
      'both',
      'preview',
      'fullscreen'
    ],
    input() {
      syncContentFromEditor()
    },
    after() {
      vditor.setValue(form.value.content || '')
    }
  })
}

async function loadArticle() {
  if (isNew) return
  try {
    const res = await getAdmin(route.params.id)
    const article = res.data || res
    form.value = {
      id: article.id, title: article.title || '', summary: article.summary || '',
      content: restoreResourceReferences(article.content || ''),
      coverResourceId: article.coverResourceId, tagNames: []
    }
    if (vditor) vditor.setValue(form.value.content || '')
  } catch (e) { toast(e.message, 'error') }
}

async function submit() {
  saving.value = true
  try {
    syncContentFromEditor()
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
  const markdown = `\n${resourceReferenceImage(r.id)}\n`
  if (vditor) {
    vditor.insertValue(markdown)
    syncContentFromEditor()
    return
  }
  form.value.content += markdown
}

function goBack() { router.push({ name: 'AdminArticles' }) }

onMounted(async () => {
  initEditor()
  await loadArticle()
})

onBeforeUnmount(() => {
  if (vditor) {
    vditor.destroy()
    vditor = null
  }
})
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
          <div ref="editorRef" class="ae-vditor"></div>
          <div class="ae-body-footer">
            <span>{{ (form.content || '').length }} chars</span>
            <span style="color:var(--ink-faint)">—</span>
            <span>Vditor IR</span>
          </div>
        </div>

        <input v-model="tagInput" class="input-ori" placeholder="Tags (comma separated)" />
      </div>
    </div>

    <ResourcePicker ref="coverPicker" usage="article" title="Choose Cover" @pick="r => form.coverResourceId = r.id" />
    <ResourcePicker ref="contentPicker" usage="article" title="Insert Article Image" @pick="r => insertContentRef(r)" />
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
.ae-vditor :deep(.vditor) {
  border: none;
}

.ae-vditor :deep(.vditor-toolbar) {
  border-bottom: 1px solid var(--line);
}

.ae-vditor :deep(.vditor-content) {
  background: var(--bg);
}

.ae-vditor :deep(.vditor-ir pre.vditor-reset),
.ae-vditor :deep(.vditor-ir) {
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
}
.ae-body-footer {
  display: flex; gap: 8px; align-items: center;
  padding: 6px 14px; background: var(--bg-warm);
  border-top: 1px solid var(--line);
  font-size: 10px; color: var(--ink-faint);
}
</style>
