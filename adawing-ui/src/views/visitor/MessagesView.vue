<script setup>
import { ref, onMounted } from 'vue'
import { listMessages, submitMessage } from '@/api/messages.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import { toast } from '@/utils/toast.js'
import { formatDate } from '@/utils/formatDate.js'
import ResourcePicker from '@/components/ResourcePicker.vue'
import Pagination from '@/components/Pagination.vue'

const messages = ref([])
const page = ref(1)
const total = ref(0)
const size = 10
const loading = ref(false)
const submitting = ref(false)
const form = ref({ nickname: '', email: '', content: '' })
const emojiPicker = ref(null)

async function load() {
  loading.value = true
  try {
    const res = await listMessages({ page: page.value, size })
    const data = res.data || res
    messages.value = data.list || []
    total.value = data.total || 0
  } finally { loading.value = false }
}

async function submit() {
  if (!form.value.nickname || !form.value.email || !form.value.content) {
    toast('Please fill in all fields', 'warn')
    return
  }
  submitting.value = true
  try {
    await submitMessage(form.value)
    form.value = { nickname: '', email: '', content: '' }
    page.value = 1
    await load()
  } catch (e) { toast(e.message, 'error') }
  finally { submitting.value = false }
}

function insertEmoji(r) {
  form.value.content = (form.value.content || '') + `resource://${r.id}`
}

function changePage(p) { page.value = p; load() }

onMounted(load)
</script>

<template>
  <div class="messages-ori">
    <h2 class="page-title reveal">Correspondence</h2>
    <p class="page-sub reveal">Leave a note, like a stone cast into water.</p>

    <div class="msg-form reveal">
      <div class="form-row">
        <input v-model="form.nickname" type="text" class="input-ori" placeholder="Name" autocomplete="off" />
        <input v-model="form.email" type="email" class="input-ori" placeholder="Email" autocomplete="off" />
      </div>
      <textarea v-model="form.content" class="input-ori" rows="4" placeholder="Your message..."></textarea>
      <div class="form-actions">
        <button class="btn-ori btn-ori-sm" @click="emojiPicker.open()">Emoji</button>
        <div class="spacer" />
        <button class="btn-ori btn-ori-sm btn-ori-primary" :disabled="submitting" @click="submit">
          {{ submitting ? 'Submitting...' : 'Submit' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-ori">Loading...</div>

    <TransitionGroup v-else tag="div" name="msg-fade" class="msg-list">
      <div v-if="messages.length === 0" key="empty" class="empty-ori">No messages yet.</div>

      <div v-for="msg in messages" :key="msg.id" class="msg-card reveal">
        <div class="mc-head">
          <div class="mc-avatar">
            <img v-if="msg.avatarResourceId" :src="resourceContentUrl(msg.avatarResourceId)" :alt="msg.nickname" />
            <span v-else class="mc-avatar-text">{{ (msg.nickname || '?')[0].toUpperCase() }}</span>
          </div>
          <div class="mc-author">
            <span class="mc-name">{{ msg.nickname }}</span>
            <span class="mc-time">{{ formatDate(msg.createTime) }}</span>
          </div>
        </div>
        <div class="mc-body" v-html="msg.content" />
        <div v-if="msg.reply" class="mc-reply">
          <span class="mc-reply-tag">Admin</span>
          <span class="mc-reply-text">{{ msg.reply }}</span>
        </div>
      </div>
    </TransitionGroup>

    <Pagination :current="page" :total="total" :size="size" @change="changePage" />
    <ResourcePicker ref="emojiPicker" pool="EMOJI" title="Choose Emoji" @pick="insertEmoji" />
  </div>
</template>

<style scoped>
.messages-ori {
  padding-bottom: 60px;
}

/* ── form ── */
.msg-form {
  border: 1px solid var(--line);
  padding: 24px;
  margin-bottom: 40px;
}

.form-row {
  display: flex; gap: 14px; margin-bottom: 14px;
}
.form-row .input-ori { flex: 1; }
.msg-form textarea.input-ori { min-height: 100px; }

.form-actions {
  display: flex; align-items: center; gap: 10px; margin-top: 14px;
}
.spacer { flex: 1; }

/* ── list card ── */
.msg-list { margin-bottom: 20px; }

.msg-card {
  border-bottom: 1px solid var(--line);
  padding: 28px 0;
}
.msg-card:first-child { border-top: 1px solid var(--line); }

.mc-head {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 16px;
}

.mc-avatar {
  width: 40px; height: 40px; border-radius: 50%; overflow: hidden;
  flex-shrink: 0; border: 1px solid var(--line);
}
.mc-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.mc-avatar-text {
  width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
  background: var(--bg-warm); color: var(--ink-faint);
  font-size: 16px; font-weight: 600;
}

.mc-author { display: flex; flex-direction: column; gap: 2px; }
.mc-name { font-size: 14px; font-weight: 500; color: var(--ink); }
.mc-time { font-size: 12px; color: var(--ink-faint); }

.mc-body {
  font-size: 14px; line-height: 1.8; color: var(--ink);
  word-break: break-word;
}

/* ── admin reply ── */
.mc-reply {
  margin-top: 16px; padding: 12px 15px;
  background: var(--bg-warm); border-left: 2px solid var(--accent);
  display: flex; gap: 10px; align-items: flex-start;
}
.mc-reply-tag {
  font-size: 10px; font-weight: 600; letter-spacing: 0.06em;
  color: var(--accent); flex-shrink: 0; padding-top: 2px;
}
.mc-reply-text { font-size: 13px; color: var(--ink); line-height: 1.7; }

/* ── transition ── */
.msg-fade-enter-active { transition: opacity 0.5s ease, transform 0.5s ease; }
.msg-fade-leave-active { transition: opacity 0.25s ease; }
.msg-fade-enter-from { opacity: 0; transform: translateY(16px); }
.msg-fade-leave-to { opacity: 0; }
</style>
