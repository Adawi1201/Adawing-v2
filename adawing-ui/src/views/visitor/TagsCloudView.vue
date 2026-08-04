<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { listAllTags } from '@/api/tags.js'
import { useScrollReveal } from '@/composables/useScrollReveal.js'

const tags = ref([])
const loading = ref(false)
const containerRef = ref(null)

useScrollReveal(containerRef, '.reveal', { stagger: 0.03 })

async function load() {
  loading.value = true
  try {
    const res = await listAllTags()
    tags.value = res.data || res || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div ref="containerRef" class="tags-cloud-ori">
    <h2 class="page-title reveal">Tags</h2>
    <p class="page-sub reveal">Browse writing by topic.</p>

    <div v-if="loading" class="loading-ori">Loading...</div>

    <div v-else-if="tags.length" class="tag-bubbles">
      <RouterLink
        v-for="tag in tags"
        :key="tag.id"
        :to="`/tags/${encodeURIComponent(tag.name)}`"
        class="tag-bubble reveal"
        :style="tag.color ? { '--bubble-accent': tag.color } : null"
      >
        {{ tag.name }}
      </RouterLink>
    </div>

    <div v-else class="empty-ori">No tags yet.</div>
  </div>
</template>
