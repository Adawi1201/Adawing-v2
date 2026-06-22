<script setup>
import { ref, onMounted } from 'vue'
import { listResources, uploadResource, allocateResource, deleteResource } from '@/api/resources.js'
import AuthImage from '@/components/AuthImage.vue'
import { toast } from '@/utils/toast.js'
import { formatDate } from '@/utils/formatDate.js'
import Pagination from '@/components/Pagination.vue'

const POOLS = ['MISC', 'AVATAR', 'ARTICLE', 'EMOJI']
const ALLOCATE_TARGETS = ['MISC', 'AVATAR', 'ARTICLE', 'EMOJI']

const resources = ref([])
const loading = ref(false)
const uploading = ref(false)
const page = ref(1)
const total = ref(0)
const size = 20
const poolFilter = ref('')
const uploadPool = ref('MISC')

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (poolFilter.value) params.pool = poolFilter.value
    const res = await listResources(params)
    const data = res.data || res
    resources.value = data.list || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

async function onFileChange(e) {
  const file = e.target.files[0]
  if (!file) return
  uploading.value = true
  try {
    await uploadResource(file, uploadPool.value)
    e.target.value = ''
    await load()
  } catch (err) { toast(err.message, 'error') }
  finally { uploading.value = false }
}

async function doAllocate(id, e) {
  if (!e.target.value) return
  try { await allocateResource(id, e.target.value); await load() }
  catch (err) { toast(err.message, 'error') }
}

async function doDelete(id) {
  if (!confirm('Delete this resource?')) return
  await deleteResource(id)
  await load()
}

function changePage(p) { page.value = p; load() }
function applyFilter() { page.value = 1; load() }

function formatSize(bytes) {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`
}

function isImage(mime) { return mime && mime.startsWith('image/') }

onMounted(load)
</script>

<template>
  <div class="resources-page">
    <div class="admin-header-ori">
      <h1>Resources</h1>
      <div class="header-actions">
        <select v-model="poolFilter" class="input-ori" style="width:130px" @change="applyFilter">
          <option value="">All Pools</option>
          <option v-for="p in POOLS" :key="p" :value="p">{{ p }}</option>
        </select>
        <select v-model="uploadPool" class="input-ori" style="width:110px">
          <option v-for="p in POOLS" :key="p" :value="p">{{ p }}</option>
        </select>
        <label class="btn-ori btn-ori-sm">
          {{ uploading ? 'Uploading...' : 'Upload' }}
          <input type="file" hidden @change="onFileChange" />
        </label>
      </div>
    </div>

    <div v-if="loading" class="loading-ori">Loading...</div>

    <div v-else-if="resources.length === 0" class="empty-ori" style="padding:60px 0">No resources yet.</div>

    <div v-else class="res-grid">
      <div v-for="r in resources" :key="r.id" class="res-card" :title="r.originalName">
        <div class="rc-thumb">
          <AuthImage v-if="isImage(r.mimeType)" :src="r.id" :alt="r.originalName" />
          <div v-else class="rc-thumb-file">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
          </div>
          <span class="rc-pool">{{ r.pool }}</span>
        </div>
        <div class="rc-info">
          <div class="rc-name" :title="r.originalName">{{ r.originalName }}</div>
          <div class="rc-meta">
            <span>{{ formatSize(r.size) }}</span>
            <span class="rc-dot">·</span>
            <span class="rc-mime">{{ (r.mimeType || '').split('/').pop() }}</span>
            <span class="rc-dot">·</span>
            <span>{{ r.refCount }} ref</span>
          </div>
          <div class="rc-actions">
            <select class="input-ori" style="width:100px;font-size:10px" @change="doAllocate(r.id, $event)">
              <option value="">Move…</option>
              <option v-for="p in ALLOCATE_TARGETS" :key="p" :value="p">{{ p }}</option>
            </select>
            <button class="btn-ori btn-ori-xs btn-ori-danger" @click="doDelete(r.id)">Del</button>
          </div>
        </div>
      </div>
    </div>

    <Pagination :current="page" :total="total" :size="size" @change="changePage" />
  </div>
</template>

<style scoped>
.resources-page { --gap: 1px; }

.header-actions {
  display: flex; align-items: center; gap: 8px;
}

.res-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(144px, 1fr));
  gap: 10px;
}

.res-card {
  border: 1px solid var(--line);
  background: var(--bg);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.res-card:hover {
  border-color: var(--accent);
  box-shadow: 0 2px 12px rgba(0,0,0,.06);
}

.rc-thumb {
  position: relative;
  width: 100%; aspect-ratio: 1;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-warm);
  overflow: hidden;
}
.rc-thumb img {
  width: 100%; height: 100%; object-fit: cover; display: block;
}
.rc-thumb-file {
  color: var(--ink-faint);
}

.rc-pool {
  position: absolute; top: 4px; right: 4px;
  font-size: 8px; letter-spacing: 0.07em; font-weight: 600;
  background: var(--bg); color: var(--ink-faint);
  padding: 1px 5px; border: 1px solid var(--line);
}

.rc-info {
  padding: 8px 10px 10px;
  display: flex; flex-direction: column; gap: 4px;
}

.rc-name {
  font-size: 11px; font-weight: 500; color: var(--ink);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}

.rc-meta {
  font-size: 10px; color: var(--ink-faint);
  display: flex; align-items: center; flex-wrap: wrap; gap: 2px;
}
.rc-dot { opacity: 0.4; }
.rc-mime {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 80px;
}

.rc-actions {
  display: flex; gap: 4px; margin-top: 4px;
  align-items: center;
}
</style>
