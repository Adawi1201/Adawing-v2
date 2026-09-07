<script setup>
import { ref, onMounted, computed } from 'vue'
import { RouterLink } from 'vue-router'
import { listArchive, searchArticles } from '@/api/articles.js'
import { useScrollReveal } from '@/composables/useScrollReveal.js'

const archives = ref({})
const loading = ref(false)
const containerRef = ref(null)

const keyword = ref('')
const searchResults = ref([])
const searchLoading = ref(false)
const searching = computed(() => keyword.value.trim() !== '')
let searchTimer = null

function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  if (!searching.value) {
    searchResults.value = []
    return
  }
  searchTimer = setTimeout(runSearch, 300)
}

async function runSearch() {
  searchLoading.value = true
  try {
    const res = await searchArticles({ keyword: keyword.value.trim(), limit: 20 })
    searchResults.value = res.data || res || []
  } catch {
    searchResults.value = []
  } finally {
    searchLoading.value = false
  }
}

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

    <div class="chronicle-search reveal">
      <input
        v-model="keyword"
        class="chronicle-search-input"
        type="search"
        placeholder="Search articles..."
        aria-label="Search articles"
        @input="onSearchInput"
      />
    </div>

    <template v-if="searching">
      <div v-if="searchLoading" class="loading-ori">Loading...</div>
      <template v-else>
        <div v-for="article in searchResults" :key="article.id" class="timeline-entry-ori">
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
        <div v-if="searchResults.length === 0" class="empty-ori">No articles match "{{ keyword.trim() }}".</div>
      </template>
    </template>

    <template v-else>
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
    </template>
  </div>
</template>

<style scoped>
.chronicle-search {
  margin-bottom: 28px;
}
.chronicle-search-input {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  font-family: inherit;
  color: var(--ink);
  background: transparent;
  border: 1px solid var(--line);
  border-radius: var(--radius, 0);
  outline: none;
  transition: border-color 0.2s;
}
.chronicle-search-input:focus {
  border-color: var(--accent);
}
.chronicle-search-input::placeholder {
  color: var(--ink-faint);
}
</style>
