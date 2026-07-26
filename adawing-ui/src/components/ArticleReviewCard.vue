<script setup>
import { reactive } from 'vue'
import AuthImage from '@/components/AuthImage.vue'
import MarkdownContent from '@/components/MarkdownContent.vue'

const props = defineProps({
  task: { type: Object, required: true },
  coverResourceId: { type: [String, Number], default: null }
})

const emit = defineEmits(['approve', 'reject', 'ignore', 'pick-cover'])

const form = reactive({ note: '', reason: '' })

function doApprove() {
  const payload = { reviewerNote: form.note || '' }
  if (props.coverResourceId) payload.coverResourceId = props.coverResourceId
  emit('approve', props.task, payload)
}

function doReject() {
  emit('reject', props.task, { reason: form.reason || '', reviewerNote: form.note || '' })
}

function doIgnore() {
  emit('ignore', props.task)
}
</script>

<template>
  <div class="article-review-card">
    <div class="rd-section">
      <div class="rd-label">Content</div>
      <MarkdownContent class="rd-body" :source="task.contentBody || '—'" />
    </div>

    <div v-if="task.status === 0" class="rd-actions">
      <div class="rd-action-row">
        <div
          class="cover-btn"
          :title="coverResourceId ? 'Cover ID: ' + coverResourceId : 'Pick cover from resources'"
          @click="emit('pick-cover', task)"
        >
          <AuthImage v-if="coverResourceId" :src="coverResourceId" class="cover-btn-img" />
          <span v-else>Cover</span>
        </div>
        <button class="btn-ori btn-ori-sm" @click="doApprove">Approve</button>
        <button class="btn-ori btn-ori-sm btn-ori-danger" @click="doReject">Reject</button>
        <button class="btn-ori btn-ori-sm btn-ori-danger" @click="doIgnore">Ignore</button>
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
</template>

<style scoped>
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
.rd-body :deep(img) { max-width: 100%; }

.rd-actions {
  margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--line);
}
.rd-action-row {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
}

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
  display: flex; align-items: center; gap: 8px; margin-top: 8px;
}
.resolved-icon { font-size: 14px; }
.resolved-icon.approved { color: #10b981; }
.resolved-icon.rejected { color: #ef4444; }
</style>
