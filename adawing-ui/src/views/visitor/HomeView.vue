<script setup>
import { ref, onMounted, computed, watch, nextTick } from 'vue'
import { RouterLink } from 'vue-router'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'
import { listPublished } from '@/api/articles.js'
import { useSiteStore } from '@/stores/site.js'
import Pagination from '@/components/Pagination.vue'
import TypewriterText from '@/components/TypewriterText.vue'
import { formatDate } from '@/utils/formatDate.js'
import { sourceLabel } from '@/utils/source.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'

gsap.registerPlugin(ScrollTrigger)

const site = useSiteStore()
const articles = ref([])
const page = ref(1)
const total = ref(0)
const size = 10
const loading = ref(false)
const revealTriggers = ref([])
const listRef = ref(null)

const heroText = computed(() => {
  const desc = site.config.description || 'void'
  return desc.replace(/<[^>]*>/g, '')
})

const heroSubtitle = computed(() => {
  return site.config.subtitle || 'void'
})

async function load() {
  loading.value = true
  try {
    const res = await listPublished({ page: page.value, size })
    const data = res.data || res
    articles.value = data.list || []
    total.value = data.total || 0
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

function changePage(p) {
  page.value = p
  load()
}

function initReveal() {
  nextTick(() => {
    revealTriggers.value.forEach((t) => t.kill())
    revealTriggers.value = []
    const el = listRef.value
    if (!el) return
    const items = el.querySelectorAll('.article-item-ori')
    items.forEach((item) => {
      gsap.set(item, { opacity: 0, y: 30, scaleY: 0.92, transformOrigin: 'top center' })
      const st = ScrollTrigger.create({
        trigger: item,
        start: 'top 85%',
        once: true,
        onEnter: () => {
          gsap.to(item, {
            opacity: 1,
            y: 0,
            scaleY: 1,
            duration: 0.8,
            ease: 'power2.out'
          })
        }
      })
      revealTriggers.value.push(st)
    })
  })
}

watch(articles, initReveal, { deep: true })

onMounted(() => {
  site.load()
  load()
})
</script>

<template>
  <div class="home">
    <section class="hero-oriental">
      <div class="eyebrow">TECHNOLOGY & THOUGHTS / {{ new Date().getFullYear() }}</div>
      <h1><TypewriterText :text="heroText" :speed="0.08" /></h1>
      <p class="desc">{{ heroSubtitle }}</p>
      <div class="scroll-hint">SCROLL</div>
    </section>

    <section ref="listRef" class="article-list-ori">
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
      <div v-if="!loading && articles.length === 0" class="empty-ori">No articles yet.</div>
      <Pagination :current="page" :total="total" :size="size" @change="changePage" />
    </section>
  </div>
</template>
