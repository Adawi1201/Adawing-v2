<script setup>
import { ref, onMounted } from 'vue'
import { listNotes } from '@/api/notes.js'
import { useScrollReveal } from '@/composables/useScrollReveal.js'
import Pagination from '@/components/Pagination.vue'

const notes = ref([])
const page = ref(1)
const total = ref(0)
const size = 10
const loading = ref(false)
const containerRef = ref(null)

useScrollReveal(containerRef, '.reveal', { stagger: 0.04 })

import { formatDateTime } from '@/utils/formatDate.js'

function typeLabel(type) {
  return type === 'TECH' ? 'TECH' : 'PERSONAL'
}

async function load() {
  loading.value = true
  try {
    const res = await listNotes(null, page.value, size)
    const data = res.data || res
    notes.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  page.value = p
  load()
}

onMounted(load)
</script>

<template>
  <div ref="containerRef" class="notes-ori">
    <h2 class="page-title reveal">Notes</h2>
    <p class="page-sub reveal">A timeline of notes, thoughts and tech fragments.</p>

    <div v-if="loading" class="loading-ori">Loading...</div>
    <div v-for="note in notes" :key="note.id" class="note-item-ori reveal">
      <div class="note-meta">
        <span class="note-type">{{ typeLabel(note.type) }}</span>
        <span class="note-time">{{ formatDateTime(note.createTime) }}</span>
      </div>
      <h3 v-if="note.title" class="note-title">{{ note.title }}</h3>
      <p class="note-content" v-html="note.content" />
    </div>
    <div v-if="!loading && notes.length === 0" class="empty-ori">No notes yet.</div>

    <Pagination :current="page" :total="total" :size="size" @change="changePage" />
  </div>
</template>
