<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, RouterLink } from 'vue-router'
import { listArticlesByTag } from '@/api/tags.js'
import Pagination from '@/components/Pagination.vue'
import { formatDate } from '@/utils/formatDate.js'
import { sourceLabel } from '@/utils/source.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'

const route = useRoute()
const articles = ref([])
const page = ref(1)
const total = ref(0)
const size = 10
const loading = ref(false)

const tagName = ref(route.params.name)

async function load() {
  loading.value = true
  try {
    const res = await listArticlesByTag(tagName.value, { page: page.value, size })
    const data = res.data || res
    articles.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function changePage(p) { page.value = p; load() }

watch(() => route.params.name, (n) => {
  tagName.value = n
  page.value = 1
  load()
})

onMounted(load)
</script>

<template>
  <div class="tag-articles">
    <div class="tag-head reveal">
      <div class="th-label">Tag</div>
      <h2 class="th-name">{{ tagName }}</h2>
    </div>

    <section class="article-list-ori">
      <div v-if="loading" class="loading-ori">Loading...</div>
      <RouterLink
        v-for="article in articles"
        :key="article.id"
        :to="`/articles/${article.id}`"
        class="article-item-ori"
      >
        <div class="meta-row">
          <span class="date">{{ formatDate(article.createTime) }}</span>
          <span class="tag">{{ sourceLabel(article) }}</span>
        </div>
        <div style="display: flex; gap: 20px;">
          <img v-if="article.coverResourceId" :src="resourceContentUrl(article.coverResourceId)" class="article-card-cover" />
          <div style="flex: 1;">
            <h3>{{ article.title }}</h3>
            <p class="excerpt">{{ article.summary }}</p>
          </div>
        </div>
      </RouterLink>
      <div v-if="!loading && articles.length === 0" class="empty-ori">No articles under this tag.</div>
      <Pagination :current="page" :total="total" :size="size" @change="changePage" />
    </section>
  </div>
</template>

<style scoped>
.tag-articles {
  max-width: 960px;
  margin: 0 auto;
  padding: 80px 48px 60px;
}
/* 页面已提供居中容器与内边距，避免与全局 .article-list-ori 的 padding 叠加 */
.tag-articles :deep(.article-list-ori) {
  max-width: 100%;
  padding: 0;
}
.tag-head {
  border-bottom: 1px solid var(--line);
  padding-bottom: 20px;
  margin-bottom: 8px;
}
.th-label {
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--ink-faint);
  margin-bottom: 6px;
}
.th-name {
  font-family: var(--font-display);
  font-size: 2.2rem;
  font-weight: 500;
  color: var(--ink);
}
@media (max-width: 768px) {
  .tag-articles {
    padding: 60px 24px;
  }
}
</style>
