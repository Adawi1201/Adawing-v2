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
  // Replace the <link> element instead of mutating href: some WebKit-based
  // browsers (e.g. Orion) only react to link add/remove, not href changes.
  document.querySelectorAll('link[rel="icon"]').forEach((el) => el.remove())
  const link = document.createElement('link')
  link.rel = 'icon'
  if (faviconId) {
    link.href = `${resourceContentUrl(faviconId)}?v=${faviconId}`
  } else {
    link.type = 'image/svg+xml'
    link.href = '/favicon.svg'
  }
  document.head.appendChild(link)
}, { immediate: true })
</script>

<template>
  <RouterView />
</template>
