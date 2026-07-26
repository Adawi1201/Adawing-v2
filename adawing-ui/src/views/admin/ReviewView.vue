<script setup>
import { ref, onMounted, reactive } from 'vue'
import { listTasks, approve, reject, ignoreTask } from '@/api/review.js'
import ResourcePicker from '@/components/ResourcePicker.vue'
import Pagination from '@/components/Pagination.vue'
import { formatDate } from '@/utils/formatDate.js'
import NoteReviewCard from '@/components/NoteReviewCard.vue'
import ArticleReviewCard from '@/components/ArticleReviewCard.vue'
import MessageReviewCard from '@/components/MessageReviewCard.vue'

const tasks = ref([])
const page = ref(1)
const total = ref(0)
const size = 10
const loading = ref(false)
const filter = ref('')
const expanded = ref(null)

// 父级持有 picker 选择结果（按 taskId），子卡片通过 prop 读取
const picks = reactive({})
const avatarPicker = ref(null)
const coverPicker = ref(null)
let pendingTaskId = null
let coverTaskId = null

function getPick(taskId) {
  if (!picks[taskId]) picks[taskId] = { avatarResourceId: null, coverResourceId: null }
  return picks[taskId]
}

function isArticle(task) { return task.contentType === 'article' }
function isMessage(task) { return task.contentType === 'message' }
function isNote(task) { return task.contentType === 'note' }

function statusText(s) {
  const map = { 0: 'Pending', 1: 'Approved', 2: 'Rejected' }
  return map[s] || s
}

function contentTypeIcon(type) {
  if (type === 'message') return 'M'
  if (type === 'note') return 'N'
  return 'A'
}

function toggleDetail(task) {
  expanded.value = expanded.value === task.id ? null : task.id
}

function onPickCover(task) {
  coverTaskId = task.id
  coverPicker.value.open()
}
function onPickAvatar(task) {
  pendingTaskId = task.id
  avatarPicker.value.open()
}
function pickCover(r) {
  if (coverTaskId) { getPick(coverTaskId).coverResourceId = r.id; coverTaskId = null }
}
function pickAvatar(r) {
  if (pendingTaskId) { getPick(pendingTaskId).avatarResourceId = r.id; pendingTaskId = null }
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

// 统一处理三种卡片的 emit（子组件已组装好 payload）
async function onApprove(task, payload) {
  await approve(task.id, payload)
  delete picks[task.id]
  await load()
}
async function onReject(task, payload) {
  await reject(task.id, payload)
  delete picks[task.id]
  await load()
}
async function onIgnore(task) {
  const label = task.contentType || 'content'
  if (!confirm(`Ignore this pending ${label} review and delete the underlying content?`)) return
  await ignoreTask(task.id)
  delete picks[task.id]
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

        <!-- Detail panel — dispatch by contentType -->
        <Transition name="slide">
          <div v-if="expanded === task.id" class="review-detail">
            <NoteReviewCard
              v-if="isNote(task)"
              :task="task"
              @approve="onApprove" @reject="onReject" @ignore="onIgnore"
            />
            <ArticleReviewCard
              v-else-if="isArticle(task)"
              :task="task"
              :cover-resource-id="getPick(task.id).coverResourceId"
              @approve="onApprove" @reject="onReject" @ignore="onIgnore"
              @pick-cover="onPickCover"
            />
            <MessageReviewCard
              v-else
              :task="task"
              :avatar-resource-id="getPick(task.id).avatarResourceId"
              @approve="onApprove" @reject="onReject" @ignore="onIgnore"
              @pick-avatar="onPickAvatar"
            />
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
.rs-icon.note { background: #e0872f; }

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

/* ── detail wrapper（卡片内容样式在各自组件内） ── */
.review-detail {
  border-top: 1px solid var(--line);
  padding: 18px 18px 18px 66px;
  background: var(--bg-warm);
}

/* ── transition ── */
.slide-enter-active { transition: all 0.2s ease-out; }
.slide-leave-active { transition: all 0.15s ease-in; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; overflow: hidden; }
.slide-enter-to, .slide-leave-from { max-height: 600px; }
</style>
