<script setup>
import { ref, onMounted, reactive } from 'vue'
import { listAdminMessages, approveMessage, rejectMessage } from '@/api/messages.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import { toast } from '@/utils/toast.js'
import ResourcePicker from '@/components/ResourcePicker.vue'
import Pagination from '@/components/Pagination.vue'
import { formatDate } from '@/utils/formatDate.js'

const messages = ref([])
const page = ref(1)
const total = ref(0)
const size = 10
const loading = ref(false)
const filter = ref('')
const expanded = ref(null)

const forms = reactive({})
const avatarPicker = ref(null)
let pendingMsgId = null

function getForm(msgId) {
  if (!forms[msgId]) forms[msgId] = { reply: '', reason: '', avatarResourceId: null }
  return forms[msgId]
}

function statusText(s) {
  const map = { 0: 'Pending', 1: 'Approved', 2: 'Rejected' }
  return map[s] || s
}

function toggleDetail(msg) {
  expanded.value = expanded.value === msg.id ? null : msg.id
}

function pickAvatar(r) {
  if (pendingMsgId) {
    getForm(pendingMsgId).avatarResourceId = r.id
    pendingMsgId = null
  }
}

async function load() {
  loading.value = true
  try {
    const s = filter.value === '' ? null : parseInt(filter.value)
    const res = await listAdminMessages({ status: s, page: page.value, size })
    const data = res.data || res
    messages.value = data.list || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

async function doApprove(msgId) {
  const f = getForm(msgId)
  await approveMessage(msgId, f.reply || undefined, f.avatarResourceId || undefined)
  delete forms[msgId]
  await load()
}

async function doReject(msgId) {
  const f = getForm(msgId)
  if (!f.reason) { toast('Please enter a rejection reason', 'warn'); return }
  await rejectMessage(msgId, f.reason)
  delete forms[msgId]
  await load()
}

function changePage(p) { page.value = p; load() }
function applyFilter() { page.value = 1; load() }

function stripHtml(html) {
  if (!html) return ''
  const div = document.createElement('div')
  div.innerHTML = html
  const text = div.textContent || ''
  return text.length > 100 ? text.slice(0, 100) + '…' : text
}

onMounted(load)
</script>

<template>
  <div class="admin-messages">
    <div class="admin-header-ori">
      <h1>Messages</h1>
      <select v-model="filter" class="input-ori" style="width: 160px; padding: 8px 0;" @change="applyFilter">
        <option value="">All</option><option value="0">Pending</option><option value="1">Approved</option><option value="2">Rejected</option>
      </select>
    </div>

    <div v-if="loading" class="loading-ori">Loading...</div>
    <template v-else>
      <div v-if="messages.length === 0" class="empty-ori" style="padding: 60px 0;">No messages.</div>

      <div v-for="msg in messages" :key="msg.id" class="msg-row" :class="{ expanded: expanded === msg.id }">
        <!-- Summary row -->
        <div class="msg-summary" @click="toggleDetail(msg)">
          <div class="ms-avatar">
            <img v-if="msg.avatarResourceId" :src="resourceContentUrl(msg.avatarResourceId)" />
            <span v-else class="ms-avatar-placeholder">{{ (msg.nickname || '?')[0].toUpperCase() }}</span>
          </div>
          <div class="ms-main">
            <div class="ms-name">
              {{ msg.nickname }}
              <span class="ms-email">{{ msg.email }}</span>
            </div>
            <div class="ms-preview" v-text="stripHtml(msg.content)" />
          </div>
          <div class="ms-status">
            <span class="ms-badge" :class="'s-' + msg.status">{{ statusText(msg.status) }}</span>
            <span class="ms-date">{{ formatDate(msg.createTime) }}</span>
            <svg class="ms-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="6 9 12 15 18 9"/></svg>
          </div>
        </div>

        <!-- Detail panel -->
        <Transition name="slide">
          <div v-if="expanded === msg.id" class="msg-detail">
            <!-- Full content -->
            <div class="md-section">
              <div class="md-label">Content</div>
              <div class="md-body" v-html="msg.content || '—'" />
            </div>

            <!-- Reply / rejection note (already processed) -->
            <div v-if="msg.status !== 0" class="md-resolved">
              <template v-if="msg.status === 1">
                <span class="resolved-icon approved">&#10003;</span>
                {{ msg.reply || 'Approved' }}
              </template>
              <template v-else>
                <span class="resolved-icon rejected">&#10005;</span>
                {{ msg.rejectReason || 'Rejected' }}
              </template>
            </div>

            <!-- Actions (only for pending) -->
            <div v-if="msg.status === 0" class="md-actions">
              <div class="md-action-row">
                <div
                  @click="pendingMsgId = msg.id; avatarPicker.open()"
                  class="avatar-btn"
                  :title="getForm(msg.id).avatarResourceId ? 'Avatar ID: ' + getForm(msg.id).avatarResourceId : 'Choose avatar'"
                >
                  <img v-if="getForm(msg.id).avatarResourceId" :src="resourceContentUrl(getForm(msg.id).avatarResourceId)" class="avatar-btn-img" />
                  <span v-else>+</span>
                </div>
                <input v-model="getForm(msg.id).reply" class="input-ori action-input" placeholder="Reply" />
                <button class="btn-ori btn-ori-sm" @click="doApprove(msg.id)">Approve</button>
                <input v-model="getForm(msg.id).reason" class="input-ori action-input" placeholder="Reason" />
                <button class="btn-ori btn-ori-sm btn-ori-danger" @click="doReject(msg.id)">Reject</button>
              </div>
            </div>
          </div>
        </Transition>
      </div>
    </template>

    <Pagination :current="page" :total="total" :size="size" @change="changePage" />
    <ResourcePicker ref="avatarPicker" @pick="pickAvatar" />
  </div>
</template>

<style scoped>
.msg-row {
  border: 1px solid var(--line);
  margin-bottom: 8px;
  transition: border-color 0.2s;
}
.msg-row.expanded { border-color: var(--accent); }

.msg-summary {
  display: flex; align-items: center; gap: 14px;
  padding: 14px 18px;
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}
.msg-summary:hover { background: var(--bg-warm); }

.ms-avatar {
  width: 34px; height: 34px; border-radius: 50%; overflow: hidden;
  flex-shrink: 0; border: 1px solid var(--line);
}
.ms-avatar img { width: 100%; height: 100%; object-fit: cover; }
.ms-avatar-placeholder {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-warm); color: var(--ink-faint);
  font-size: 13px; font-weight: 600;
}

.ms-main { flex: 1; min-width: 0; }
.ms-name {
  font-size: 14px; font-weight: 500; color: var(--ink);
}
.ms-email {
  font-size: 11px; color: var(--ink-faint); font-weight: 400; margin-left: 8px;
}
.ms-preview {
  font-size: 12px; color: var(--ink-faint);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  margin-top: 2px;
}

.ms-status {
  display: flex; align-items: center; gap: 10px; flex-shrink: 0;
}

.ms-badge {
  font-size: 11px; font-weight: 600; padding: 2px 10px; letter-spacing: 0.05em;
}
.s-0 { background: rgba(245,158,11,0.1); color: #b45309; }
.s-1 { background: rgba(16,185,129,0.1); color: #065f46; }
.s-2 { background: rgba(100,116,139,0.1); color: #475569; }

.ms-date { font-size: 11px; color: var(--ink-faint); }

.ms-chevron {
  color: var(--ink-faint); transition: transform 0.2s;
}
.expanded .ms-chevron { transform: rotate(180deg); }

/* ── detail ── */
.msg-detail {
  border-top: 1px solid var(--line);
  padding: 18px 18px 18px 66px;
  background: var(--bg-warm);
}

.md-section { margin-bottom: 14px; }
.md-label {
  font-size: 10px; font-weight: 600; text-transform: uppercase;
  letter-spacing: 0.07em; color: var(--ink-faint); margin-bottom: 6px;
}
.md-body {
  font-size: 13px; line-height: 1.7; color: var(--ink);
  max-height: 240px; overflow-y: auto;
  padding: 12px 16px; background: var(--bg); border: 1px solid var(--line);
  word-break: break-word;
}

.md-actions {
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid var(--line);
}

.md-action-row {
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

.md-resolved {
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
