<script setup>
import { RouterView } from 'vue-router'
import { useThemeStore } from '@/stores/theme.js'
import { useSiteStore } from '@/stores/site.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'
import { onMounted, watch } from 'vue'

const theme = useThemeStore()
const site = useSiteStore()

onMounted(() => {
  theme.init()
  site.load()
})

watch(() => site.config.favicon, (faviconId) => {
  let link = document.querySelector('link[rel="icon"]')
  if (!link) {
    link = document.createElement('link')
    link.rel = 'icon'
    document.head.appendChild(link)
  }
  link.href = faviconId ? resourceContentUrl(faviconId) : '/favicon.ico'
}, { immediate: true })
</script>

<template>
  <RouterView />
</template>
