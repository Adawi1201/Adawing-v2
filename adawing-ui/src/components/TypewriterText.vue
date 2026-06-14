<script setup>
import { ref, watch, onMounted } from 'vue'
import gsap from 'gsap'

const props = defineProps({
  text: {
    type: String,
    default: ''
  },
  speed: {
    type: Number,
    default: 0.06
  },
  delay: {
    type: Number,
    default: 0.3
  }
})

const displayText = ref('')
const cursorVisible = ref(true)
let tween = null

function type(newText) {
  if (tween) tween.kill()
  displayText.value = ''
  const chars = (newText || '').split('')
  const obj = { index: 0 }

  tween = gsap.to(obj, {
    index: chars.length,
    duration: chars.length * props.speed,
    delay: props.delay,
    ease: 'none',
    onUpdate: () => {
      const i = Math.round(obj.index)
      displayText.value = chars.slice(0, i).join('')
    },
    onComplete: () => {
      displayText.value = newText || ''
    }
  })
}

watch(() => props.text, (newText) => {
  type(newText)
}, { immediate: true })

onMounted(() => {
  gsap.to(cursorVisible, {
    value: false,
    duration: 0.6,
    repeat: -1,
    yoyo: true,
    ease: 'steps(1)'
  })
})
</script>

<template>
  <span class="typewriter">
    <span class="typewriter-text">{{ displayText }}</span>
    <span class="typewriter-cursor" :class="{ blink: cursorVisible }">|</span>
  </span>
</template>

<style scoped>
.typewriter {
  display: inline;
}

.typewriter-cursor {
  display: inline-block;
  width: 2px;
  margin-left: 2px;
  color: var(--accent);
  opacity: 0;
}

.typewriter-cursor.blink {
  opacity: 1;
}
</style>
