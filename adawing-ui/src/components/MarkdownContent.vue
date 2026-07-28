<script setup>
import { nextTick, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import Vditor from 'vditor'
import { useThemeStore } from '@/stores/theme.js'
import { buildPreviewOptions } from '@/utils/vditorOptions.js'

const props = defineProps({
  source: { type: String, default: '' }
})

const rootRef = ref(null)
const { isDark } = storeToRefs(useThemeStore())

async function renderMarkdown() {
  await nextTick()
  if (!rootRef.value) return
  rootRef.value.innerHTML = ''
  Vditor.preview(rootRef.value, props.source || '', buildPreviewOptions(isDark.value))
}

watch([() => props.source, isDark], () => {
  renderMarkdown()
}, { immediate: true })

onMounted(() => {
  renderMarkdown()
})
</script>

<template>
  <div ref="rootRef" class="markdown-content"></div>
</template>

<style scoped>
.markdown-content {
  color: inherit;
}

.markdown-content :deep(.vditor-reset) {
  color: inherit;
  font-family: inherit;
}

.markdown-content :deep(img) {
  max-width: 100%;
}

.markdown-content :deep(pre) {
  overflow-x: auto;
}
</style>
