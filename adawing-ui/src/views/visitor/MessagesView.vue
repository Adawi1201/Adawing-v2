<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { storeToRefs } from 'pinia'
import { useRoute, useRouter } from 'vue-router'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { listMessages, submitMessage, likeMessage } from '@/api/messages.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import { toast } from '@/utils/toast.js'
import { formatDate } from '@/utils/formatDate.js'
import { useSiteStore } from '@/stores/site.js'
import ResourcePicker from '@/components/ResourcePicker.vue'
import Pagination from '@/components/Pagination.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'

gsap.registerPlugin(ScrollTrigger)

const site = useSiteStore()
const { config } = storeToRefs(site)
const route = useRoute()
const router = useRouter()

const messages = ref([])
const page = ref(1)
const total = ref(0)
const size = 10
const loading = ref(false)
const submitting = ref(false)
const form = ref({ nickname: '', email: '', content: '' })
// Article being quoted, prefilled from the article page (query.refId/refTitle).
const reference = ref(null)
const emojiPicker = ref(null)
const letterRef = ref(null)
const notesRef = ref(null)

const LIKED_KEY = 'adawing:liked-messages'
const likedIds = ref(loadLikedIds())

let revealTriggers = []

function loadLikedIds() {
  try {
    return new Set(JSON.parse(localStorage.getItem(LIKED_KEY) || '[]'))
  } catch {
    return new Set()
  }
}

function persistLikedIds() {
  localStorage.setItem(LIKED_KEY, JSON.stringify([...likedIds.value]))
}

function hasLiked(id) {
  return likedIds.value.has(id)
}

// Deterministic small tilt per note (±1.6°), stable across re-renders.
function noteRotation(id, index) {
  const key = Number(id) || index
  return ((key % 5) - 2) * 0.8
}

async function load() {
  loading.value = true
  try {
    const res = await listMessages({ page: page.value, size })
    const data = res.data || res
    messages.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
    await nextTick()
    pinIntro()
  }
}

// Notes pinned to the board, revealed one by one as they scroll into view.
function pinIntro() {
  revealTriggers.forEach((t) => t.kill())
  revealTriggers = []
  const board = notesRef.value
  if (!board) return
  const items = board.querySelectorAll('.note')
  if (!items.length) return

  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (reduce) {
    items.forEach((el) => gsap.set(el, { clearProps: 'opacity,transform' }))
    return
  }

  items.forEach((el) => {
    const r = parseFloat(el.dataset.rot || '0')
    gsap.set(el, { opacity: 0, y: -44, rotation: r - 9 })
    const st = ScrollTrigger.create({
      trigger: el,
      start: 'top 92%',
      once: true,
      onEnter: () => {
        gsap.to(el, { opacity: 1, y: 0, rotation: r, duration: 0.6, ease: 'back.out(1.7)' })
      }
    })
    revealTriggers.push(st)
  })
}

// Compose a letter, seal it and send it off for review — the note does not
// appear on the board until an admin approves it, so we only play the
// "letter flying away" animation and inform the visitor.
async function submit() {
  if (!form.value.nickname || !form.value.email || !form.value.content) {
    toast('请把名字、邮箱和信的内容都填上', 'warn')
    return
  }
  submitting.value = true
  try {
    const payload = { ...form.value }
    if (reference.value) {
      payload.refId = reference.value.id
      payload.refTitle = reference.value.title
    }
    await submitMessage(payload)
    await flyLetterAway()
    form.value = { nickname: '', email: '', content: '' }
    reference.value = null
    toast('信已寄出，等待审核后钉上墙 · Sent for review', 'info')
  } catch (e) {
    toast(e.message, 'error')
  } finally {
    submitting.value = false
  }
}

function clearReference() {
  reference.value = null
}

function goToArticle(refId) {
  if (refId) router.push({ name: 'Article', params: { id: refId } })
}

// Like a published note: optimistic +1, remembered in localStorage so the same
// browser cannot inflate the count. Heart beats and one ghost heart floats up.
async function likeNote(msg, event) {
  if (hasLiked(msg.id)) return
  likedIds.value.add(msg.id)
  persistLikedIds()
  msg.likeCount = (msg.likeCount || 0) + 1
  playHeart(event)
  try {
    const res = await likeMessage(msg.id)
    const server = res.data ?? res
    if (typeof server === 'number') msg.likeCount = server
  } catch (e) {
    // revert on failure
    likedIds.value.delete(msg.id)
    persistLikedIds()
    msg.likeCount = Math.max(0, (msg.likeCount || 1) - 1)
    toast(e.message || '点赞失败', 'error')
  }
}

function playHeart(event) {
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  const btn = event?.currentTarget
  if (reduce || !btn) return
  const heart = btn.querySelector('.heart')
  if (heart) {
    gsap.fromTo(heart, { scale: 1 }, { scale: 1.5, duration: 0.18, yoyo: true, repeat: 1, ease: 'power2.out' })
  }
  const ghost = document.createElement('span')
  ghost.className = 'fly-heart'
  ghost.textContent = '♥'
  btn.appendChild(ghost)
  gsap.fromTo(
    ghost,
    { y: 0, opacity: 0.9, scale: 1, filter: 'blur(0px)' },
    { y: -34, opacity: 0, scale: 1.6, filter: 'blur(3px)', duration: 0.9, ease: 'power1.out', onComplete: () => ghost.remove() }
  )
}

function flyLetterAway() {
  const el = letterRef.value
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  if (!el || reduce) return Promise.resolve()
  return new Promise((resolve) => {
    gsap.timeline({
      defaults: { ease: 'sine.inOut' },
      onComplete: () => {
        gsap.set(el, { clearProps: 'all' })
        resolve()
      }
    })
      // seal up: lift, tilt, slightly shrink
      .to(el, { y: -14, scale: 0.985, rotation: -1.4, duration: 0.32, ease: 'power2.out' })
      .to(el, { duration: 0.15 })
      // drift up-right and fade
      .to(el, { x: 150, y: -230, rotation: 11, scale: 0.62, opacity: 0, duration: 1.25, ease: 'power2.in' })
  })
}

function insertEmoji(r) {
  form.value.content = (form.value.content || '') + `![emoji](resource://${r.id})`
}

function changePage(p) { page.value = p; load() }

onMounted(() => {
  const { refId, refTitle } = route.query
  if (refId) {
    reference.value = { id: Number(refId), title: refTitle || '' }
  }
  load()
})
onUnmounted(() => {
  revealTriggers.forEach((t) => t.kill())
  revealTriggers = []
})
</script>

<template>
  <div class="messages-ori">
    <h2 class="page-title reveal">Correspondence</h2>
    <p class="page-sub reveal">Seed a letter, and let it find its wall.</p>

    <!-- 写信输入区：格孔撕纸 + 折角 -->
    <div ref="letterRef" class="letter reveal">
      <span class="tear"></span>
      <span class="holes"></span>
      <span class="dogear-cut"></span>
      <span class="dogear"></span>

      <div class="letter-h">Seed a letter</div>
      <div class="letter-dear">Dear friend, 见字如面 —</div>

      <div v-if="reference" class="ref-banner">
        <span class="rb-label">REF</span>
        <span class="rb-title">In reply to «{{ reference.title }}»</span>
        <button class="rb-x" title="Remove reference" @click="clearReference">×</button>
      </div>

      <div class="frow">
        <input v-model="form.nickname" type="text" class="letter-field" placeholder="Your name" autocomplete="off" />
        <input v-model="form.email" type="email" class="letter-field" placeholder="Email" autocomplete="off" />
      </div>
      <textarea v-model="form.content" class="letter-field letter-body" rows="4" placeholder="Write your letter… (Markdown supported)"></textarea>

      <div v-if="form.content" class="letter-preview">
        <div class="letter-preview-label">Preview</div>
        <MarkdownContent :source="form.content" />
      </div>

      <div class="send-row">
        <button class="btn-ori btn-ori-sm" @click="emojiPicker.open()">Emoji</button>
        <span class="live-sign">— yours, {{ form.nickname || '…' }}</span>
        <div class="spacer"></div>
        <button class="btn-ori btn-ori-sm btn-ori-primary" :disabled="submitting" @click="submit">
          {{ submitting ? 'Sending…' : 'Seal & send ✉' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="loading-ori">Loading...</div>

    <!-- 便签墙 -->
    <div v-else class="board">
      <div v-if="messages.length === 0" class="empty-ori">No letters yet. Be the first to write.</div>
      <div ref="notesRef" class="notes">
        <div
          v-for="(msg, i) in messages"
          :key="msg.id"
          class="note"
          :data-rot="noteRotation(msg.id, i)"
          :style="{ transform: `rotate(${noteRotation(msg.id, i)}deg)` }"
        >
          <span class="pin"></span>
          <span class="dogear-cut"></span>
          <span class="dogear"></span>

          <div
            v-if="msg.refId && msg.refTitle"
            class="ref-chip"
            :title="msg.refTitle"
            @click="goToArticle(msg.refId)"
          >✎ In reply to «<span class="rc-t">{{ msg.refTitle }}</span>»</div>

          <div class="dear">Dear friend,</div>
          <div class="mc-head">
            <div class="mc-avatar">
              <img v-if="msg.avatarResourceId" :src="resourceContentUrl(msg.avatarResourceId)" :alt="msg.nickname" />
              <span v-else class="mc-avatar-text">{{ (msg.nickname || '?')[0].toUpperCase() }}</span>
            </div>
            <span class="mc-name">{{ msg.nickname }}</span>
          </div>

          <MarkdownContent class="mc-body" :source="msg.content" />
          <div class="mc-foot">— {{ msg.nickname }}, {{ formatDate(msg.createTime) }}</div>

          <div v-if="msg.reply" class="mc-reply">
            <span class="mc-reply-tag">{{ config.name || 'Admin' }}</span>
            <span class="mc-reply-text">{{ msg.reply }}</span>
          </div>

          <div class="like-row">
            <button
              class="like-btn"
              :class="{ liked: hasLiked(msg.id) }"
              :disabled="hasLiked(msg.id)"
              @click="likeNote(msg, $event)"
            >
              <span class="heart">{{ hasLiked(msg.id) ? '♥' : '♡' }}</span>
              <span class="cnt">{{ msg.likeCount || 0 }}</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <Pagination :current="page" :total="total" :size="size" @change="changePage" />
    <ResourcePicker
      ref="emojiPicker"
      usage="emoji"
      title="Choose Emoji"
      empty-text="No emoji resources available yet."
      @pick="insertEmoji"
    />
  </div>
</template>

<style scoped>
.messages-ori {
  padding-bottom: 60px;
  /* board / paper tokens — aligned with the site theme */
  --board: #ece4d4;
  --board-edge: rgba(120, 95, 60, 0.22);
  --paper: #ffffff;
  --paper-shadow: rgba(60, 50, 35, 0.26);
  --hole: #eee9df;
  --line-rule: rgba(184, 115, 51, 0.16);
  --fold-back: #efe9dc;
  --fold-edge: rgba(0, 0, 0, 0.16);
  --like: #c2185b;
}
:global([data-theme="dark"] .messages-ori) {
  --board: #1e1e1e;
  --board-edge: rgba(255, 255, 255, 0.10);
  --paper: #242424;
  --paper-shadow: rgba(0, 0, 0, 0.55);
  --hole: #161616;
  --line-rule: rgba(212, 150, 106, 0.18);
  --fold-back: #2c2c2c;
  --fold-edge: rgba(0, 0, 0, 0.5);
  --like: #e8567f;
}

/* ── 缺角上折（真折角）：缺口暗影 + 翻起纸背三角 ── */
.dogear {
  position: absolute; right: 0; bottom: 0; width: 0; height: 0; z-index: 3;
  border-style: solid; border-width: 0 0 26px 26px;
  border-color: transparent transparent var(--fold-back) transparent;
  filter: drop-shadow(-2px -2px 3px rgba(0, 0, 0, 0.22));
}
.dogear-cut {
  position: absolute; right: 0; bottom: 0; width: 26px; height: 26px; z-index: 2;
  background: linear-gradient(315deg, var(--fold-edge) 0%, transparent 60%);
}

/* ── 写信输入区 ── */
.letter {
  position: relative; background: var(--paper);
  padding: 34px 30px 26px 48px; margin-bottom: 56px; border-radius: 2px;
  box-shadow: 0 16px 34px -20px var(--paper-shadow), 0 2px 6px -3px rgba(0, 0, 0, 0.14);
}
.letter .tear {
  position: absolute; left: 0; right: 0; top: -5px; height: 9px; z-index: 2;
  background: radial-gradient(circle at 6px 9px, transparent 5px, var(--paper) 5.5px) repeat-x;
  background-size: 13px 9px;
}
.letter .holes {
  position: absolute; left: 16px; top: 42px; bottom: 24px; width: 13px; z-index: 2;
  background: radial-gradient(circle at 6.5px 6.5px, var(--hole) 4px, transparent 4.5px);
  background-size: 13px 32px; background-repeat: repeat-y;
}
.letter-h {
  font-family: var(--font-display); font-size: 26px; font-weight: 500;
  letter-spacing: 0.02em; color: var(--accent); margin-bottom: 2px;
}
.letter-dear { font-size: 14px; color: var(--ink-faint); opacity: 0.7; margin-bottom: 18px; font-style: italic; }

.frow { display: flex; gap: 16px; margin-bottom: 6px; }
.frow .letter-field { flex: 1; }
.letter-field {
  width: 100%; font-family: var(--font-body); font-size: 15px; color: var(--ink);
  background: transparent; border: none; outline: none; line-height: 32px;
  background-image: linear-gradient(to bottom, transparent 31px, var(--line-rule) 31px, var(--line-rule) 32px);
  background-size: 100% 32px;
}
input.letter-field { height: 32px; }
.letter-body { min-height: 96px; resize: vertical; }
.letter-field::placeholder { color: var(--ink-faint); opacity: 0.5; }
.letter-field:focus { --line-rule: var(--accent); }

.send-row { display: flex; align-items: center; gap: 12px; margin-top: 16px; }
.live-sign { font-size: 13px; color: var(--ink-faint); opacity: 0.75; font-style: italic; }
.spacer { flex: 1; }

.letter-preview {
  margin-top: 16px; padding: 16px; border: 1px dashed var(--line); background: var(--bg-warm);
}
.letter-preview-label {
  font-size: 10px; color: var(--ink-faint); letter-spacing: 0.08em; text-transform: uppercase; margin-bottom: 10px;
}
.letter-preview :deep(img) { max-width: 32px; max-height: 32px; vertical-align: middle; margin: 0 2px; }

/* ── 米色纯板 ── */
.board {
  position: relative; border-radius: 10px; padding: 44px 32px 50px; margin-bottom: 20px;
  background: var(--board); border: 1px solid var(--board-edge);
  box-shadow: inset 0 1px 6px rgba(0, 0, 0, 0.08), 0 10px 30px -18px rgba(0, 0, 0, 0.4);
}
.notes {
  display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 44px 28px; padding-top: 6px;
}
@media (max-width: 560px) { .notes { grid-template-columns: 1fr; } }

/* ── 纸条（不裁剪，图钉完整浮出） ── */
.note {
  position: relative; background: var(--paper); padding: 20px 18px 16px; border-radius: 2px;
  box-shadow: 0 10px 22px -10px var(--paper-shadow), 0 2px 4px -2px rgba(0, 0, 0, 0.10);
}
.note .dogear { border-width: 0 0 20px 20px; }
.note .dogear-cut { width: 20px; height: 20px; }

/* 亮麻图钉 */
.pin {
  position: absolute; top: -11px; left: 50%; transform: translateX(-50%);
  width: 20px; height: 20px; border-radius: 50%; z-index: 6;
  background:
    radial-gradient(circle at 34% 28%, rgba(255, 255, 255, 0.95) 0 2px, transparent 3px),
    radial-gradient(circle at 38% 32%, color-mix(in srgb, var(--accent) 55%, #fff), var(--accent) 55%, var(--accent-hover) 100%);
  box-shadow: 0 4px 7px -1px rgba(0, 0, 0, 0.45), inset -2px -2px 3px rgba(0, 0, 0, 0.32), inset 2px 2px 3px rgba(255, 255, 255, 0.5);
}
.pin::after {
  content: ""; position: absolute; top: 15px; left: 50%; transform: translateX(-50%);
  width: 3px; height: 8px; background: linear-gradient(180deg, rgba(0, 0, 0, 0.28), transparent);
  border-radius: 2px; filter: blur(0.3px);
}

.dear { font-size: 12px; opacity: 0.5; margin: 6px 0 6px; font-style: italic; color: var(--ink-faint); }
.mc-head { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.mc-avatar {
  width: 34px; height: 34px; border-radius: 50%; overflow: hidden; flex-shrink: 0; border: 1px solid var(--line);
}
.mc-avatar img { width: 100%; height: 100%; object-fit: cover; display: block; }
.mc-avatar-text {
  width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;
  background: var(--bg-warm); color: var(--ink-faint); font-size: 14px; font-weight: 600;
}
.mc-name { font-size: 13px; font-weight: 600; color: var(--ink); }
.mc-body { font-size: 14px; line-height: 1.8; color: var(--ink); word-break: break-word; }
.mc-foot { margin-top: 12px; font-size: 12px; opacity: 0.6; text-align: right; font-style: italic; color: var(--ink-faint); }

.mc-reply {
  margin-top: 12px; padding: 8px 10px; background: var(--accent-faint); border-left: 2px solid var(--accent);
}
.mc-reply-tag {
  display: block; font-size: 9px; font-weight: 700; letter-spacing: 0.08em; color: var(--accent); margin-bottom: 4px;
}
.mc-reply-text { font-size: 12px; color: var(--ink); line-height: 1.7; }

/* ── 引用条（输入区） ── */
.ref-banner {
  display: flex; align-items: center; gap: 10px; margin-bottom: 16px;
  padding: 8px 12px; background: var(--accent-faint); border-left: 2px solid var(--accent);
  border-radius: 0 3px 3px 0;
}
.ref-banner .rb-label {
  font-size: 11px; font-weight: 600; letter-spacing: 0.06em; color: var(--accent);
}
.ref-banner .rb-title {
  flex: 1; font-size: 13px; font-style: italic; color: var(--ink);
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.ref-banner .rb-x {
  cursor: pointer; border: none; background: transparent; color: var(--ink-faint);
  font-size: 18px; line-height: 1; padding: 0 2px;
}
.ref-banner .rb-x:hover { color: var(--accent); }

/* ── 引用标签（纸条） ── */
.ref-chip {
  display: inline-flex; align-items: center; max-width: 100%; margin-bottom: 10px;
  padding: 3px 8px; font-size: 11px; color: var(--accent);
  background: var(--accent-faint); border-radius: 3px; cursor: pointer;
  transition: background 0.18s;
}
.ref-chip:hover { background: color-mix(in srgb, var(--accent) 22%, transparent); }
.ref-chip .rc-t {
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 170px;
}

/* ── 点赞 ── */
.like-row { display: flex; align-items: center; margin-top: 10px; }
.like-btn {
  position: relative; display: inline-flex; align-items: center; gap: 6px;
  font-family: var(--font-body); font-size: 13px; cursor: pointer;
  color: var(--ink-faint); background: transparent; border: none; padding: 4px 6px;
  border-radius: 4px; transition: color 0.2s;
}
.like-btn:hover:not(:disabled) { color: var(--like); }
.like-btn.liked { color: var(--like); }
.like-btn:disabled { cursor: default; }
.like-btn .heart { font-size: 15px; line-height: 1; }
.like-btn .cnt { font-variant-numeric: tabular-nums; }
.like-btn :deep(.fly-heart) {
  position: absolute; left: 8px; top: 2px; font-size: 15px; color: var(--like);
  pointer-events: none; z-index: 8;
}
</style>
