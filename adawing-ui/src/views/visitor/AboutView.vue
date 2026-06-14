<script setup>
import { ref, onMounted, computed } from 'vue'
import { useSiteStore } from '@/stores/site.js'
import { useScrollReveal } from '@/composables/useScrollReveal.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'

const site = useSiteStore()
const loading = ref(false)
const containerRef = ref(null)

useScrollReveal(containerRef, '.reveal')

const profile = computed(() => site.config.profile || {})
const links = computed(() => site.config.links || [])

onMounted(() => {
  loading.value = true
  site.load().finally(() => {
    loading.value = false
  })
})
</script>

<template>
  <div ref="containerRef" class="about-ori">
    <div v-if="loading" class="loading-ori">Loading...</div>
    <template v-else>
      <div class="about-header reveal">
        <div v-if="profile.avatar" class="about-avatar">
          <img :src="resolveResourceUrl(profile.avatar)" :alt="profile.ownerName" />
        </div>
        <h1 class="about-name">{{ profile.ownerName || 'void' }}</h1>
        <p class="about-signature">{{ profile.signature || 'void' }}</p>
      </div>

      <div class="about-divider reveal"></div>

      <div class="about-body reveal">
        <p>{{ profile.bio || 'void' }}</p>
      </div>

      <div v-if="links.length > 0" class="about-links reveal">
        <div class="about-divider"></div>
        <h2 class="links-heading">Links</h2>
        <ul class="links-list">
          <li v-for="(link, index) in links" :key="index" class="link-item">
            <a :href="link.url" target="_blank" rel="noopener noreferrer">
              <img v-if="link.icon" :src="resourceContentUrl(link.icon)" class="link-icon" />
              <span class="link-name">{{ link.name }}</span>
              <span class="link-type">{{ link.type === 'friend' ? 'Friend' : 'Social' }}</span>
            </a>
          </li>
        </ul>
      </div>
    </template>
  </div>
</template>
