<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { listArchive } from '@/api/articles.js'
import { useScrollReveal } from '@/composables/useScrollReveal.js'

const archives = ref({})
const loading = ref(false)
const containerRef = ref(null)

useScrollReveal(containerRef, '.reveal', {
  stagger: 0.04
})

import { sourceLabel } from '@/utils/source.js'

function formatDay(time) {
  if (!time) return ''
  const d = new Date(time)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${m}.${day}`
}

function formatYear(time) {
  if (!time) return ''
  return new Date(time).getFullYear()
}

function sourceTag(article) {
  return sourceLabel(article).toUpperCase()
}

async function load() {
  loading.value = true
  try {
    const res = await listArchive()
    archives.value = res.data || res
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div ref="containerRef" class="timeline-ori">
    <h2 class="page-title reveal">Chronicle</h2>

    <div v-if="loading" class="loading-ori">Loading...</div>
    <template v-else>
      <template v-for="(articles, month) in archives" :key="month">
        <div class="timeline-month-ori reveal">{{ month }}</div>
        <div v-for="article in articles" :key="article.id" class="timeline-entry-ori reveal">
          <div class="date">
            <div class="year">{{ formatYear(article.createTime) }}</div>
            <div class="day">{{ formatDay(article.createTime) }}</div>
          </div>
          <div class="content">
            <RouterLink :to="`/articles/${article.id}`">
              <h4>{{ article.title }}</h4>
            </RouterLink>
            <p>{{ article.summary }}</p>
            <div class="tag">{{ sourceTag(article) }}</div>
          </div>
        </div>
      </template>
      <div v-if="Object.keys(archives).length === 0" class="empty-ori">No chronicle yet.</div>
    </template>
  </div>
</template>
