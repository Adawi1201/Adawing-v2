<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listAdmin, publish, hide, submitForReview, deleteArticle } from '@/api/articles.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import Pagination from '@/components/Pagination.vue'
import { formatDate } from '@/utils/formatDate.js'

const router = useRouter()
const articles = ref([])
const page = ref(1)
const total = ref(0)
const size = 10
const loading = ref(false)

const CS_DRAFT = 0, CS_PENDING = 1, CS_PUBLISHED = 2, CS_REJECTED = 3, CS_HIDDEN = 4

function statusText(s) {
  return { [CS_DRAFT]: 'Draft', [CS_PENDING]: 'Pending', [CS_PUBLISHED]: 'Published', [CS_REJECTED]: 'Rejected', [CS_HIDDEN]: 'Hidden' }[s] || s
}

function sourceText(s) {
  return s === 1 ? 'Agent' : 'Original'
}

async function load() {
  loading.value = true
  try {
    const res = await listAdmin({ page: page.value, size })
    const data = res.data || res
    articles.value = data.list || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

function goNew() { router.push({ name: 'AdminArticleNew' }) }
function goEdit(id) { router.push({ name: 'AdminArticleEdit', params: { id } }) }

async function doPublish(id) { await publish(id); await load() }
async function doHide(id) { await hide(id); await load() }
async function doReview(id) { await submitForReview(id); await load() }

function deleteMessageForStatus(status) {
  if (status === CS_PENDING) {
    return 'Delete this pending article and clear its pending review task?'
  }
  if (status === CS_PUBLISHED) {
    return 'Delete this published article permanently? It will not return to draft.'
  }
  if (status === CS_HIDDEN) {
    return 'Delete this hidden article permanently?'
  }
  return 'Delete this article permanently?'
}

async function doDelete(article) {
  if (!confirm(deleteMessageForStatus(article.status))) return
  await deleteArticle(article.id)
  await load()
}

function changePage(p) { page.value = p; load() }

onMounted(load)
</script>

<template>
  <div class="admin-articles">
    <div class="admin-header-ori">
      <h1>Articles</h1>
      <button class="btn-ori" @click="goNew">+ New</button>
    </div>

    <div v-if="loading" class="loading-ori">Loading...</div>
    <template v-else>
      <div v-if="articles.length === 0" class="empty-ori" style="padding:60px 0">No articles yet.</div>

      <div v-for="a in articles" :key="a.id" class="art-row" @click="goEdit(a.id)">
        <div class="art-cover">
          <img v-if="a.coverResourceId" :src="resourceContentUrl(a.coverResourceId)" />
          <span v-else class="art-cover-empty">—</span>
        </div>
        <div class="art-main">
          <div class="art-title">{{ a.title }}</div>
          <div class="art-meta">
            <span :class="'art-badge s-' + a.status">{{ statusText(a.status) }}</span>
            <span>{{ sourceText(a.source) }}</span>
            <span>{{ formatDate(a.createTime) }}</span>
          </div>
        </div>
        <div class="art-actions" @click.stop>
          <template v-if="a.status === CS_DRAFT || a.status === CS_REJECTED">
            <button class="btn-ori btn-ori-xs" @click="goEdit(a.id)">Edit</button>
            <button class="btn-ori btn-ori-xs" @click="doPublish(a.id)">Publish</button>
            <button class="btn-ori btn-ori-xs btn-ori-danger" @click="doDelete(a)">Del</button>
          </template>
          <template v-else-if="a.status === CS_PENDING">
            <button class="btn-ori btn-ori-xs" @click="goEdit(a.id)">Edit</button>
            <button class="btn-ori btn-ori-xs btn-ori-danger" @click="doDelete(a)">Del</button>
          </template>
          <template v-else-if="a.status === CS_PUBLISHED">
            <button class="btn-ori btn-ori-xs" @click="goEdit(a.id)">Edit</button>
            <button class="btn-ori btn-ori-xs" @click="doHide(a.id)">Hide</button>
            <button class="btn-ori btn-ori-xs btn-ori-danger" @click="doDelete(a)">Del</button>
          </template>
          <template v-else-if="a.status === CS_HIDDEN">
            <button class="btn-ori btn-ori-xs" @click="doPublish(a.id)">Publish</button>
            <button class="btn-ori btn-ori-xs btn-ori-danger" @click="doDelete(a)">Del</button>
          </template>
        </div>
      </div>
    </template>

    <Pagination :current="page" :total="total" :size="size" @change="changePage" />
  </div>
</template>

<style scoped>
.art-row {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 0; border-bottom: 1px solid var(--line);
  cursor: pointer; transition: padding-left 0.25s, border-color 0.2s;
}
.art-row:hover { padding-left: 8px; border-color: var(--accent); }

.art-cover {
  width: 48px; height: 32px; flex-shrink: 0;
  border: 1px solid var(--line); overflow: hidden;
  display: flex; align-items: center; justify-content: center;
}
.art-cover img { width: 100%; height: 100%; object-fit: cover; }
.art-cover-empty { font-size: 11px; color: var(--ink-faint); }

.art-main { flex: 1; min-width: 0; }
.art-title { font-size: 14px; font-weight: 500; color: var(--ink); margin-bottom: 4px; }
.art-meta { display: flex; gap: 10px; align-items: center; font-size: 11px; color: var(--ink-faint); }

.art-badge {
  font-size: 10px; font-weight: 600; padding: 1px 8px; letter-spacing: 0.05em; border-radius: 2px;
}
.s-0 { background: rgba(100,116,139,0.1); color: #475569; }
.s-1 { background: rgba(245,158,11,0.1); color: #b45309; }
.s-2 { background: rgba(16,185,129,0.1); color: #065f46; }
.s-3 { background: rgba(239,68,68,0.1); color: #991b1b; }
.s-4 { background: rgba(100,116,139,0.1); color: #64748b; }

.art-actions {
  display: flex; gap: 4px; flex-shrink: 0; align-items: center;
}
</style>
