<script setup>
import { onMounted, ref, onUnmounted } from 'vue'
import gsap from 'gsap'
import { ScrollTrigger } from 'gsap/ScrollTrigger'

gsap.registerPlugin(ScrollTrigger)

const plantRef = ref(null)
const petals = ref([])
let petalTweens = []
let triggers = []

onMounted(() => {
  if (!plantRef.value) return
  const svg = plantRef.value
  const seedling = svg.querySelector('.stage-seedling')
  const young = svg.querySelector('.stage-young')
  const mature = svg.querySelector('.stage-mature')

  gsap.set(seedling, { opacity: 1, scale: 1, transformOrigin: '20px 440px' })
  gsap.set(young, { opacity: 0, scale: 0.8, transformOrigin: '20px 440px' })
  gsap.set(mature, { opacity: 0, scale: 0.8, transformOrigin: '20px 440px' })

  const tl = gsap.timeline({
    scrollTrigger: {
      trigger: document.body,
      start: 'top top',
      end: 'bottom bottom',
      scrub: 1,
      onLeave: () => startFalling()
    }
  })

  // 0% - 35% : seedling grows and holds
  tl.to(seedling, { scale: 1.05, duration: 0.35, ease: 'none' }, 0)

  // 25% - 45% : seedling fades, young appears
  tl.to(seedling, { opacity: 0, scale: 0.92, duration: 0.2, ease: 'power1.in' }, 0.25)
  tl.to(young, { opacity: 1, scale: 1, duration: 0.25, ease: 'power2.out' }, 0.28)

  // 45% - 70% : young holds
  tl.to(young, { scale: 1.03, duration: 0.25, ease: 'none' }, 0.5)

  // 65% - 85% : young fades, mature appears
  tl.to(young, { opacity: 0, scale: 0.92, duration: 0.2, ease: 'power1.in' }, 0.65)
  tl.to(mature, { opacity: 1, scale: 1, duration: 0.25, ease: 'back.out(1.4)' }, 0.68)

  triggers.push(tl.scrollTrigger)
})

onUnmounted(() => {
  triggers.forEach((t) => t.kill())
  petalTweens.forEach((t) => t.kill())
  petals.value.forEach((p) => p.remove())
})

function startFalling() {
  if (petalTweens.length) return
  if (!plantRef.value) return
  const svg = plantRef.value
  const bounds = svg.getBoundingClientRect()

  for (let i = 0; i < 18; i++) {
    const petal = document.createElementNS('http://www.w3.org/2000/svg', 'path')
    const size = 3 + Math.random() * 3
    const d = `M0 0 C${size} -${size} ${size * 2} 0 0 ${size} C-${size} 0 -${size} -${size} 0 0`
    petal.setAttribute('d', d)
    petal.setAttribute('fill', i % 3 === 0 ? 'var(--accent)' : '#D4966A')
    petal.setAttribute('opacity', 0.55)
    const startX = 20 + Math.random() * 160
    petal.setAttribute('transform', `translate(${startX}, -20)`)
    svg.appendChild(petal)
    petals.value.push(petal)

    const tween = gsap.to(petal, {
      y: bounds.height + 40,
      x: `+=${(Math.random() - 0.5) * 120}`,
      rotation: Math.random() * 720 - 360,
      duration: 5 + Math.random() * 5,
      delay: Math.random() * 4,
      repeat: -1,
      ease: 'none',
      opacity: 0,
      onRepeat: function () {
        gsap.set(this.targets()[0], {
          x: startX,
          y: -20,
          rotation: 0,
          opacity: 0.55
        })
      }
    })
    petalTweens.push(tween)
  }
}
</script>

<template>
  <div class="plant-growth">
    <svg ref="plantRef" viewBox="0 0 240 440" class="plant-svg" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="stemGradient" x1="0%" y1="100%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="#8B5A2B" />
          <stop offset="50%" stop-color="#B87333" />
          <stop offset="100%" stop-color="#D4966A" />
        </linearGradient>
        <linearGradient id="leafGradient" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stop-color="#B87333" />
          <stop offset="100%" stop-color="#8B9A6B" />
        </linearGradient>
        <radialGradient id="flowerGradient">
          <stop offset="0%" stop-color="#F5D0A9" />
          <stop offset="100%" stop-color="#D4966A" />
        </radialGradient>
      </defs>

      <!-- Roots / emergence from side -->
      <g class="plant-roots" opacity="0.5">
        <path
          d="M20 440 Q10 420 25 400 M20 440 Q35 425 40 405 M20 440 Q0 430 5 410"
          fill="none"
          stroke="url(#stemGradient)"
          stroke-width="1.2"
          stroke-linecap="round"
        />
      </g>

      <!-- SEEDLING STAGE -->
      <g class="stage-seedling">
        <path
          d="M20 440 Q28 400 22 360 Q18 330 24 300"
          fill="none"
          stroke="url(#stemGradient)"
          stroke-width="2"
          stroke-linecap="round"
        />
        <path d="M24 300 Q36 288 30 278 Q18 286 24 300" fill="url(#leafGradient)" opacity="0.9" />
        <path d="M22 340 Q10 328 16 318 Q28 326 22 340" fill="url(#leafGradient)" opacity="0.85" />
      </g>

      <!-- YOUNG STAGE -->
      <g class="stage-young">
        <path
          d="M20 440 Q30 390 24 330 Q20 280 35 230"
          fill="none"
          stroke="url(#stemGradient)"
          stroke-width="2.2"
          stroke-linecap="round"
        />
        <path
          d="M20 440 Q8 390 18 340 Q24 300 12 270"
          fill="none"
          stroke="url(#stemGradient)"
          stroke-width="1.8"
          stroke-linecap="round"
        />
        <path d="M35 230 Q55 218 50 204 Q32 212 35 230" fill="url(#leafGradient)" opacity="0.9" />
        <path d="M12 270 Q30 258 26 244 Q8 252 12 270" fill="url(#leafGradient)" opacity="0.9" />
        <path d="M24 330 Q44 318 40 304 Q22 312 24 330" fill="url(#leafGradient)" opacity="0.9" />
        <path d="M24 330 Q4 318 8 304 Q22 312 24 330" fill="url(#leafGradient)" opacity="0.85" />
        <path d="M35 280 Q55 270 58 282 Q42 292 35 280" fill="url(#leafGradient)" opacity="0.85" />
      </g>

      <!-- MATURE STAGE -->
      <g class="stage-mature">
        <!-- Stems -->
        <path
          d="M20 440 Q34 380 28 310 Q22 240 45 170 Q56 120 40 70"
          fill="none"
          stroke="url(#stemGradient)"
          stroke-width="2.5"
          stroke-linecap="round"
        />
        <path
          d="M20 440 Q10 380 18 310 Q26 240 8 180 Q-2 140 15 100"
          fill="none"
          stroke="url(#stemGradient)"
          stroke-width="2"
          stroke-linecap="round"
        />
        <path
          d="M28 310 Q55 280 72 230 Q82 190 70 150"
          fill="none"
          stroke="url(#stemGradient)"
          stroke-width="2"
          stroke-linecap="round"
        />

        <!-- Branches -->
        <path d="M40 260 Q65 250 80 230" fill="none" stroke="url(#stemGradient)" stroke-width="1.5" stroke-linecap="round" />
        <path d="M45 170 Q70 158 88 140" fill="none" stroke="url(#stemGradient)" stroke-width="1.5" stroke-linecap="round" />
        <path d="M40 70 Q60 55 78 45" fill="none" stroke="url(#stemGradient)" stroke-width="1.5" stroke-linecap="round" />
        <path d="M15 100 Q35 88 52 75" fill="none" stroke="url(#stemGradient)" stroke-width="1.5" stroke-linecap="round" />
        <path d="M70 150 Q92 138 108 125" fill="none" stroke="url(#stemGradient)" stroke-width="1.2" stroke-linecap="round" />
        <path d="M8 180 Q-8 168 -18 155" fill="none" stroke="url(#stemGradient)" stroke-width="1.2" stroke-linecap="round" />

        <!-- Leaves -->
        <path class="plant-leaf" d="M80 230 Q100 214 94 200 Q74 210 80 230" fill="url(#leafGradient)" opacity="0.9" />
        <path class="plant-leaf" d="M88 140 Q112 124 106 110 Q86 120 88 140" fill="url(#leafGradient)" opacity="0.9" />
        <path class="plant-leaf" d="M78 45 Q102 32 108 46 Q88 60 78 45" fill="url(#leafGradient)" opacity="0.9" />
        <path class="plant-leaf" d="M52 75 Q72 60 78 74 Q60 88 52 75" fill="url(#leafGradient)" opacity="0.9" />
        <path class="plant-leaf" d="M108 125 Q130 112 136 126 Q116 140 108 125" fill="url(#leafGradient)" opacity="0.85" />
        <path class="plant-leaf" d="M-18 155 Q-36 142 -42 156 Q-24 170 -18 155" fill="url(#leafGradient)" opacity="0.85" />
        <path class="plant-leaf" d="M40 70 Q60 52 68 66 Q48 82 40 70" fill="url(#leafGradient)" opacity="0.9" />
        <path class="plant-leaf" d="M40 70 Q20 54 12 68 Q32 84 40 70" fill="url(#leafGradient)" opacity="0.9" />
        <path class="plant-leaf" d="M28 310 Q48 294 56 308 Q36 324 28 310" fill="url(#leafGradient)" opacity="0.85" />
        <path class="plant-leaf" d="M28 310 Q8 294 0 308 Q20 324 28 310" fill="url(#leafGradient)" opacity="0.85" />

        <!-- Flowers -->
        <g class="plant-flower">
          <circle cx="40" cy="70" r="5" fill="url(#flowerGradient)" />
          <circle cx="40" cy="65" r="3" fill="#F5D0A9" opacity="0.8" />
          <circle cx="45" cy="70" r="3" fill="#F5D0A9" opacity="0.8" />
          <circle cx="40" cy="75" r="3" fill="#F5D0A9" opacity="0.8" />
          <circle cx="35" cy="70" r="3" fill="#F5D0A9" opacity="0.8" />
        </g>
        <g class="plant-flower">
          <circle cx="15" cy="100" r="4" fill="url(#flowerGradient)" />
          <circle cx="15" cy="96" r="2.5" fill="#F5D0A9" opacity="0.8" />
          <circle cx="19" cy="100" r="2.5" fill="#F5D0A9" opacity="0.8" />
          <circle cx="15" cy="104" r="2.5" fill="#F5D0A9" opacity="0.8" />
          <circle cx="11" cy="100" r="2.5" fill="#F5D0A9" opacity="0.8" />
        </g>
        <g class="plant-flower">
          <circle cx="70" cy="150" r="4" fill="url(#flowerGradient)" />
          <circle cx="70" cy="146" r="2.5" fill="#F5D0A9" opacity="0.8" />
          <circle cx="74" cy="150" r="2.5" fill="#F5D0A9" opacity="0.8" />
          <circle cx="70" cy="154" r="2.5" fill="#F5D0A9" opacity="0.8" />
          <circle cx="66" cy="150" r="2.5" fill="#F5D0A9" opacity="0.8" />
        </g>
      </g>
    </svg>
  </div>
</template>

<style scoped>
.plant-growth {
  position: fixed;
  left: -20px;
  bottom: 0;
  width: 320px;
  height: 440px;
  pointer-events: none;
  z-index: 0;
  opacity: 0.65;
}

.plant-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
}

@media (max-width: 1200px) {
  .plant-growth {
    width: 220px;
    height: 340px;
    opacity: 0.45;
  }
}

@media (max-width: 768px) {
  .plant-growth {
    display: none;
  }
}
</style>
