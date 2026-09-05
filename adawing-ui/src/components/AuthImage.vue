<script setup>
import { ref, watch, onUnmounted } from 'vue'
import request from '@/api/request.js'

const props = defineProps({
  src: [String, Number]
})

const emit = defineEmits(['error'])

const TRANSPARENT_PIXEL = 'data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///yH5BAEAAAAALAAAAAABAAEAAAIBRAA7'

const blobUrl = ref('')
const loading = ref(false)
const error = ref(false)

let currentObjectUrl = ''

function revokeCurrent() {
  if (currentObjectUrl && currentObjectUrl.startsWith('blob:')) {
    URL.revokeObjectURL(currentObjectUrl)
    currentObjectUrl = ''
  }
}

async function load(id) {
  revokeCurrent()
  error.value = false

  if (id == null || id === '') {
    blobUrl.value = ''
    loading.value = false
    return
  }

  loading.value = true
  try {
    // 本组件以 XHR 取 blob，只能消费字节流：proxy=1 要求后端回源转发。
    // 该参数也使本组件与访客端 <img> 落在不同的缓存键上。
    const blob = await request.get(`/resource/${id}/content`, {
      params: { proxy: 1 },
      responseType: 'blob'
    })
    currentObjectUrl = URL.createObjectURL(blob)
    blobUrl.value = currentObjectUrl
  } catch (e) {
    error.value = true
    blobUrl.value = ''
    console.error('AuthImage load failed:', e)
    emit('error', e)
  } finally {
    loading.value = false
  }
}

watch(() => props.src, (newId) => load(newId), { immediate: true })

onUnmounted(() => {
  revokeCurrent()
})

function onError(e) {
  emit('error', e)
}
</script>

<template>
  <img
    v-bind="$attrs"
    :src="blobUrl || TRANSPARENT_PIXEL"
    :data-loading="loading"
    :data-error="error"
    @error="onError"
  />
</template>

<style scoped>
img[data-loading='true'] {
  opacity: 0.4;
}

img[data-error='true'] {
  opacity: 0.2;
}
</style>
