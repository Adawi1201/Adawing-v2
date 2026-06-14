<script setup>
import { ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import { getStats as getArticleStats, listAdmin } from '@/api/articles.js'
import { getStats as getReviewStats } from '@/api/review.js'

const stats = ref({
  total: 0,
  publishedCount: 0,
  pendingCount: 0,
  draftCount: 0,
  totalViewCount: 0
})
const reviewPending = ref(0)
const recent = ref([])
const loading = ref(false)

import { formatDate } from '@/utils/formatDate.js'
import { sourceLabel } from '@/utils/source.js'

async function load() {
  loading.value = true
  try {
    const [articleRes, reviewRes, listRes] = await Promise.all([
      getArticleStats(),
      getReviewStats(),
      listAdmin({ page: 1, size: 5 })
    ])
    stats.value = articleRes.data || articleRes
    reviewPending.value = reviewRes.data != null ? reviewRes.data : reviewRes
    const data = listRes.data || listRes
    recent.value = data.list || []
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="dashboard">
    <div class="admin-header-ori">
      <h1>Dashboard</h1>
      <span class="sub">Admin / {{ new Date().getFullYear() }}</span>
    </div>

    <div v-if="loading" class="loading-ori">Loading...</div>
    <template v-else>
      <div class="stats-ori">
        <div class="stat-ori">
          <div class="number">{{ stats.total }}</div>
          <div class="label">ARTICLES</div>
        </div>
        <div class="stat-ori">
          <div class="number">{{ stats.totalViewCount }}</div>
          <div class="label">VIEWS</div>
        </div>
        <div class="stat-ori">
          <div class="number" :style="reviewPending > 0 ? 'color: var(--accent)' : ''">{{ reviewPending }}</div>
          <div class="label">PENDING</div>
        </div>
        <div class="stat-ori">
          <div class="number">{{ stats.agentCount || 0 }}</div>
          <div class="label">AGENT</div>
        </div>
      </div>

      <table class="table-ori">
        <thead>
          <tr>
            <th>Content</th>
            <th>Type</th>
            <th>Source</th>
            <th>When</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="article in recent" :key="article.id">
            <td class="title">
              <RouterLink :to="`/articles/${article.id}`" target="_blank">{{ article.title }}</RouterLink>
            </td>
            <td :style="sourceLabel(article) === 'Agent' ? 'color: var(--accent);' : ''">{{ sourceLabel(article) }}</td>
            <td>{{ article.sourceAgent || 'Admin' }}</td>
            <td>{{ formatDate(article.createTime) }}</td>
          </tr>
          <tr v-if="recent.length === 0">
            <td colspan="4" class="empty-ori" style="padding: 40px 0;">No recent content.</td>
          </tr>
        </tbody>
      </table>
    </template>
  </div>
</template>
