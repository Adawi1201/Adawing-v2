<script setup>
import { RouterView } from 'vue-router'
import { useThemeStore } from '@/stores/theme.js'
import { onMounted } from 'vue'

const theme = useThemeStore()
onMounted(() => {
  theme.init()
})
</script>

<template>
  <RouterView />
</template>
