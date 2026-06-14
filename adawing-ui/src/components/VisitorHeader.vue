<script setup>
import { RouterLink } from 'vue-router'
import { useSiteStore } from '@/stores/site.js'
import { onMounted, ref } from 'vue'
import gsap from 'gsap'
import { visitorLinks } from '@/constants/nav.js'
import { useRouteActive } from '@/composables/useRouteActive.js'

const { isActive } = useRouteActive()
const site = useSiteStore()
const navRef = ref(null)

onMounted(() => {
  if (!navRef.value) return
  const anchors = navRef.value.querySelectorAll('a')
  if (!anchors.length) return

  const tl = gsap.timeline({ delay: 1.5 })
  anchors.forEach((el) => {
    tl.to(el, {
      color: 'var(--accent)',
      duration: 0.5,
      ease: 'power2.out'
    }).to(el, {
      color: '',
      duration: 0.5,
      ease: 'power2.in'
    }, '+=0.2')
  })
})
</script>

<template>
  <header class="v-header">
    <RouterLink to="/" class="logo">{{ site.config.name || 'void' }}</RouterLink>
    <nav ref="navRef">
      <RouterLink
        v-for="link in visitorLinks"
        :key="link.name"
        :to="link.path"
        :class="{ active: isActive(link.path) }"
      >
        {{ link.label }}
      </RouterLink>
    </nav>
  </header>
</template>
