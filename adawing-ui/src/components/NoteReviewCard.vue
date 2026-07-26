<script setup>
import { reactive } from 'vue'
import MarkdownContent from '@/components/MarkdownContent.vue'

const props = defineProps({
  task: { type: Object, required: true }
})

const emit = defineEmits(['approve', 'reject', 'ignore'])

const form = reactive({ note: '', reason: '' })

function doApprove() {
  emit('approve', props.task, { reviewerNote: form.note || '' })
}

function doReject() {
  emit('reject', props.task, { reason: form.reason || '', reviewerNote: form.note || '' })
}

function doIgnore() {
  emit('ignore', props.task)
}
</script>

<template>
  <div class="note-review-card">
    <div class="rd-section">
      <div class="rd-label">Content</div>
      <MarkdownContent class="rd-body" :source="task.contentBody || '—'" />
    </div>

    <div v-if="task.status === 0" class="rd-actions">
      <div class="rd-action-row">
        <input v-model="form.note" class="input-ori action-input" placeholder="Review note (optional)" />
        <button class="btn-ori btn-ori-sm" @click="doApprove">Approve</button>
        <input v-model="form.reason" class="input-ori action-input" placeholder="Rejection reason" />
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
.rd-body :deep(img) { max-width: 48px; max-height: 48px; vertical-align: middle; }

.rd-actions {
  margin-top: 14px; padding-top: 14px; border-top: 1px solid var(--line);
}
.rd-action-row {
  display: flex; align-items: center; gap: 6px; flex-wrap: wrap;
}
.action-input {
  flex: 1; min-width: 120px; padding: 5px 0;
  font-size: 11px; height: 28px;
}

.rd-resolved {
  font-size: 13px; color: var(--ink-faint);
  padding: 10px 14px; background: var(--bg); border: 1px solid var(--line);
  display: flex; align-items: center; gap: 8px; margin-top: 8px;
}
.resolved-icon { font-size: 14px; }
.resolved-icon.approved { color: #10b981; }
.resolved-icon.rejected { color: #ef4444; }
</style>
