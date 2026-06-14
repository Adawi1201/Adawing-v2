<script setup>
import { computed } from 'vue'

const props = defineProps({
  current: { type: Number, default: 1 },
  total: { type: Number, default: 1 },
  size: { type: Number, default: 10 }
})

const emit = defineEmits(['change'])

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

const pageList = computed(() => {
  const list = []
  for (let i = 1; i <= totalPages.value; i++) {
    list.push(i)
  }
  return list
})

function change(page) {
  if (page >= 1 && page <= totalPages.value) {
    emit('change', page)
  }
}
</script>

<template>
  <div v-if="totalPages > 1" class="pagination-ori">
    <button :disabled="current <= 1" @click="change(current - 1)">Prev</button>
    <button
      v-for="p in pageList"
      :key="p"
      :class="{ active: p === current }"
      @click="change(p)"
    >
      {{ p }}
    </button>
    <button :disabled="current >= totalPages" @click="change(current + 1)">Next</button>
  </div>
</template>
