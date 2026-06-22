<script setup>
import { ref, onMounted, reactive } from 'vue'
import { listTasks, approve, reject, ignoreTask } from '@/api/review.js'
import AuthImage from '@/components/AuthImage.vue'
import { toast } from '@/utils/toast.js'
import ResourcePicker from '@/components/ResourcePicker.vue'
import Pagination from '@/components/Pagination.vue'
import { formatDate } from '@/utils/formatDate.js'
import MarkdownContent from '@/components/MarkdownContent.vue'

const tasks = ref([])
const page = ref(1)
const total = ref(0)
const size = 10
const loading = ref(false)
const filter = ref('')
const expanded = ref(null)

const forms = reactive({})
const avatarPicker = ref(null)
const coverPicker = ref(null)
let pendingTaskId = null
let coverTaskId = null

function getForm(taskId) {
  if (!forms[taskId]) forms[taskId] = { note: '', reason: '', avatarResourceId: null, coverResourceId: null }
  return forms[taskId]
}

function isArticle(task) { return task.contentType === 'article' }
function isMessage(task) { return task.contentType === 'message' }

function statusText(s) {
  const map = { 0: 'Pending', 1: 'Approved', 2: 'Rejected' }
  return map[s] || s
}

function contentTypeIcon(type) {
  return type === 'message' ? 'M' : 'A'
}

function toggleDetail(task) {
  expanded.value = expanded.value === task.id ? null : task.id
}

function pickAvatar(r) {
  if (pendingTaskId) {
    getForm(pendingTaskId).avatarResourceId = r.id
    pendingTaskId = null
  }
}

function pickCover(r) {
  if (coverTaskId) {
    getForm(coverTaskId).coverResourceId = r.id
    coverTaskId = null
  }
}

async function load() {
  loading.value = true
  try {
    const s = filter.value === '' ? null : parseInt(filter.value)
    const res = await listTasks({ status: s, page: page.value, size })
    const data = res.data || res
    tasks.value = data.list || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

async function doApprove(task) {
  const f = getForm(task.id)
  const body = { reviewerNote: f.note || '' }
  if (isArticle(task) && f.coverResourceId) {
    body.coverResourceId = f.coverResourceId
  }
  if (isMessage(task) && f.avatarResourceId) {
    body.avatarResourceId = f.avatarResourceId
  }
  await approve(task.id, body)
  delete forms[task.id]
  await load()
}

async function doReject(task) {
  const f = getForm(task.id)
  if (isMessage(task) && !f.reason) { toast('Please enter a rejection reason', 'warn'); return }
  await reject(task.id, { reason: f.reason || '', reviewerNote: f.note || '' })
  delete forms[task.id]
  await load()
}

async function doIgnore(task) {
  const label = isArticle(task) ? 'article' : 'message'
  if (!confirm(`Ignore this pending ${label} review and delete the underlying content?`)) return
  await ignoreTask(task.id)
  delete forms[task.id]
  if (expanded.value === task.id) expanded.value = null
  await load()
}

function changePage(p) { page.value = p; load() }
function applyFilter() { page.value = 1; load() }

onMounted(load)
</script>

<template>
  <div class="review-admin">
    <div class="admin-header-ori">
      <h1>Review</h1>
      <select v-model="filter" class="input-ori" style="width: 160px; padding: 8px 0;" @change="applyFilter">
        <option value="">All</option><option value="0">Pending</option><option value="1">Approved</option><option value="2">Rejected</option>
      </select>
    </div>

    <div v-if="loading" class="loading-ori">Loading...</div>
    <template v-else>
      <div v-if="tasks.length === 0" class="empty-ori" style="padding: 60px 0;">No review tasks.</div>

      <div v-for="task in tasks" :key="task.id" class="review-row" :class="{ expanded: expanded === task.id }">
        <!-- Summary row -->
        <div class="review-summary" @click="toggleDetail(task)">
          <div class="rs-icon" :class="task.contentType">{{ contentTypeIcon(task.contentType) }}</div>
          <div class="rs-main">
            <div class="rs-title">{{ task.contentTitle || task.contentType + ' #' + task.contentId }}</div>
            <div class="rs-meta">
              <span class="rs-type">{{ task.contentType }}</span>
              <span>by {{ task.submitterName || task.submitterId || '—' }}</span>
              <span v-if="task.submitterEmail" class="rs-email">{{ task.submitterEmail }}</span>
              <span>{{ formatDate(task.submitTime) }}</span>
            </div>
          </div>
          <div class="rs-status">
            <span class="rs-badge" :class="'s-' + task.status">{{ statusText(task.status) }}</span>
            <svg class="rs-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
        </div>

        <!-- Detail panel -->
        <Transition name="slide">
          <div v-if="expanded === task.id" class="review-detail">
            <!-- Content preview -->
            <div class="rd-section">
              <div class="rd-label">Content</div>
              <MarkdownContent class="rd-body" :source="task.contentBody || '—'" />
            </div>

            <!-- Existing avatar -->
            <div v-if="task.avatarResourceId" class="rd-section">
              <div class="rd-label">Current Avatar</div>
              <AuthImage :src="task.avatarResourceId" style="width: 40px; height: 40px; object-fit: cover; border-radius: 50%; border: 1px solid var(--line);" />
            </div>

            <!-- Actions (only for pending) -->
            <div v-if="task.status === 0" class="rd-actions">
              <!-- Article: cover picker + approve / reject -->
              <div v-if="isArticle(task)" class="rd-action-row">
                <div
                  @click="coverTaskId = task.id; coverPicker.open()"
                  class="cover-btn"
                  :title="getForm(task.id).coverResourceId ? 'Cover ID: ' + getForm(task.id).coverResourceId : 'Pick cover from resources'"
                >
                  <AuthImage v-if="getForm(task.id).coverResourceId" :src="getForm(task.id).coverResourceId" class="cover-btn-img" />
                  <span v-else>Cover</span>
                </div>
                <button class="btn-ori btn-ori-sm" @click="doApprove(task)">Approve</button>
                <button class="btn-ori btn-ori-sm btn-ori-danger" @click="doReject(task)">Reject</button>
                <button class="btn-ori btn-ori-sm btn-ori-danger" @click="doIgnore(task)">Ignore</button>
              </div>

              <!-- Message: avatar + reply + approve / reject reason + reject -->
              <div v-else class="rd-action-row">
                <div
                  @click="pendingTaskId = task.id; avatarPicker.open()"
                  class="avatar-btn"
                  :title="getForm(task.id).avatarResourceId ? 'Avatar ID: ' + getForm(task.id).avatarResourceId : 'Choose avatar'"
                >
                  <AuthImage v-if="getForm(task.id).avatarResourceId" :src="getForm(task.id).avatarResourceId" class="avatar-btn-img" />
                  <span v-else>+</span>
                </div>
                <input v-model="getForm(task.id).note" class="input-ori action-input" placeholder="Reply to user" />
                <button class="btn-ori btn-ori-sm" @click="doApprove(task)">Approve</button>
                <input v-model="getForm(task.id).reason" class="input-ori action-input" placeholder="Rejection reason" />
                <button class="btn-ori btn-ori-sm btn-ori-danger" @click="doReject(task)">Reject</button>
                <button class="btn-ori btn-ori-sm btn-ori-danger" @click="doIgnore(task)">Ignore</button>
              </div>
            </div>
            <div v-else class="rd-resolved">
              <template v-if="task.status === 1">
                <span class="resolved-icon approved">&#10003;</span>
                {{ task.reviewerNote || 'Approved' }}
              </template>
              <template v-else>
                <span class="resolved-icon rejected">&#10005;</span>
                {{ task.rejectReason || task.reviewerNote || 'Rejected' }}
              </template>
            </div>
          </div>
        </Transition>
      </div>
    </template>

    <Pagination :current="page" :total="total" :size="size" @change="changePage" />
    <ResourcePicker ref="avatarPicker" usage="avatar" title="Choose Avatar" @pick="pickAvatar" />
    <ResourcePicker ref="coverPicker" usage="article" title="Choose Cover" @pick="pickCover" />
  </div>
</template>

<style scoped>
.review-row {
  border: 1px solid var(--line);
  margin-bottom: 8px;
  transition: border-color 0.2s;
}
.review-row.expanded { border-color: var(--accent); }

.review-summary {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}
.review-summary:hover { background: var(--bg-warm); }

.rs-icon {
  width: 34px; height: 34px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; flex-shrink: 0;
  color: #fff;
}
.rs-icon.message { background: #6366f1; }
.rs-icon.article { background: #10b981; }

.rs-main { flex: 1; min-width: 0; }
.rs-title {
  font-size: 14px; font-weight: 500; color: var(--ink);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.rs-meta {
  display: flex; gap: 12px; font-size: 11px; color: var(--ink-faint);
  margin-top: 3px; flex-wrap: wrap;
}
.rs-type {
  color: var(--accent); font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em;
}
.rs-email { color: var(--ink-faint); }

.rs-status {
  display: flex; align-items: center; gap: 8px; flex-shrink: 0;
}

.rs-badge {
  font-size: 11px; font-weight: 600; padding: 2px 10px; letter-spacing: 0.05em;
}
.s-0 { background: rgba(245,158,11,0.1); color: #b45309; }
.s-1 { background: rgba(16,185,129,0.1); color: #065f46; }
.s-2 { background: rgba(100,116,139,0.1); color: #475569; }

.rs-chevron {
  color: var(--ink-faint); transition: transform 0.2s;
}
.expanded .rs-chevron { transform: rotate(180deg); }

/* ── detail ── */
.review-detail {
  border-top: 1px solid var(--line);
  padding: 18px 18px 18px 66px;
  background: var(--bg-warm);
}

.rd-section { margin-bottom: 14px; }
.rd-label {
  font-size: 10px; font-weight: 600; text-transform: uppercase;
  letter-spacing: 0.07em; color: var(--ink-faint); margin-bottom: 6px;
}
.rd-body {
  font-size: 13px; line-height: 1.7; color: var(--ink);
  max-height: 240px; overflow-y: auto;
  padding: 12px 16px; background: var(--bg); border: 1px solid var(--line);
  word-break: break-word;
}

.rd-actions {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.rd-action-row {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
}

.action-input {
  flex: 1; min-width: 80px; padding: 5px 0;
  font-size: 11px; height: 28px;
}

.avatar-btn {
  width: 36px; height: 36px; border: 1px dashed var(--line);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; background: var(--bg);
  font-size: 16px; color: var(--ink-faint);
  transition: border-color 0.2s;
}
.avatar-btn:hover { border-color: var(--accent); }
.avatar-btn-img { width: 100%; height: 100%; object-fit: cover; border-radius: 50%; }

.cover-btn {
  height: 36px; padding: 0 12px; border: 1px dashed var(--line);
  cursor: pointer; display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; background: var(--bg);
  font-size: 12px; color: var(--ink-faint);
  transition: border-color 0.2s; border-radius: 2px; gap: 6px;
}
.cover-btn:hover { border-color: var(--accent); }
.cover-btn-img { height: 28px; min-width: 44px; object-fit: cover; }

.rd-resolved {
  font-size: 13px; color: var(--ink-faint);
  padding: 10px 14px; background: var(--bg); border: 1px solid var(--line);
  display: flex; align-items: center; gap: 8px;
  margin-top: 8px;
}
.resolved-icon { font-size: 14px; }
.resolved-icon.approved { color: #10b981; }
.resolved-icon.rejected { color: #ef4444; }

/* ── transition ── */
.slide-enter-active { transition: all 0.2s ease-out; }
.slide-leave-active { transition: all 0.15s ease-in; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; overflow: hidden; }
.slide-enter-to, .slide-leave-from { max-height: 600px; }
</style>
