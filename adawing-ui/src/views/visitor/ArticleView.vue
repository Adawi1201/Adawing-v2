<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublished } from '@/api/articles.js'
import { useWatchRevealChildren } from '@/composables/useScrollReveal.js'
import { formatDate } from '@/utils/formatDate.js'
import { sourceLabel } from '@/utils/source.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import { tagColor } from '@/utils/tagColor.js'
import MarkdownContent from '@/components/MarkdownContent.vue'

const route = useRoute()
const router = useRouter()
const article = ref(null)

function referThisArticle() {
  if (!article.value) return
  router.push({
    name: 'Messages',
    query: { refId: article.value.id, refTitle: article.value.title }
  })
}

function goToTag(name) {
  router.push({ name: 'TagArticles', params: { name } })
}
const loading = ref(false)
const containerRef = ref(null)

useWatchRevealChildren(containerRef, '.article-body', article, {
  start: 'top 88%',
  duration: 0.7,
  y: 24,
  stagger: 0.08
})

function sourceTag() {
  return sourceLabel(article.value)
}

async function load() {
  loading.value = true
  try {
    const res = await getPublished(route.params.id)
    article.value = res.data || res
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <article ref="containerRef" class="article-ori">
    <div v-if="loading" class="loading-ori">Loading...</div>
    <template v-else-if="article">
      <header class="article-header">
        <div class="article-date">{{ formatDate(article.createTime) }} / {{ sourceTag() }} · 阅读 {{ (article.viewCount ?? 0).toLocaleString() }}</div>
        <img v-if="article.coverResourceId" :src="resourceContentUrl(article.coverResourceId)" class="article-cover" />
        <h1>{{ article.title }}</h1>
        <div class="article-divider"></div>
      </header>

      <MarkdownContent class="article-body" :source="article.content" />

      <div v-if="article.sourceAgent" class="agent-note">
        <div class="note-label">Agent Information</div>
        <p>Co-created with {{ article.sourceAgent }} and reviewed before publishing.</p>
      </div>

      <div class="article-refer">
        <button class="refer-cta" @click="referThisArticle">✎ Write feelings refer to this article</button>
      </div>

      <div v-if="article.tags && article.tags.length" class="article-tags">
        <span
          v-for="t in article.tags"
          :key="t.id"
          class="tag-pill"
          :style="{ '--tc': tagColor(t.color) }"
          @click="goToTag(t.name)"
        >{{ t.name }}</span>
      </div>
    </template>
    <div v-else class="empty-ori">Article not found</div>
  </article>
</template>
