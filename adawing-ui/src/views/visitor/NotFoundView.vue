<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push({ name: 'Home' })
}
</script>

<template>
  <div class="notfound">
    <div class="notfound-code">404</div>
    <div class="notfound-divider"></div>
    <p class="notfound-text">No human-trace · page not found</p>
    <button class="notfound-home" @click="goHome">返回首页</button>
  </div>
</template>

<style scoped>
.notfound {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  padding: 48px 24px;
  text-align: center;
}

.notfound-code {
  font-family: var(--font-display);
  font-size: clamp(56px, 10vw, 96px);
  letter-spacing: 0.15em;
  color: var(--ink);
}

.notfound-divider {
  width: 64px;
  height: 1px;
  margin: 24px 0;
  background: var(--line);
}

.notfound-text {
  font-family: var(--font-body);
  font-size: 15px;
  color: var(--ink-faint);
  margin: 0 0 32px;
}

.notfound-home {
  font-family: var(--font-body);
  font-size: 14px;
  padding: 10px 32px;
  color: var(--accent);
  background: transparent;
  border: var(--border-w) solid var(--accent);
  border-radius: var(--radius-btn);
  cursor: pointer;
  transition: color 0.2s, background 0.2s;
}

.notfound-home:hover {
  color: var(--bg);
  background: var(--accent);
}
</style>
