<script setup>
import { useSiteStore } from '@/stores/site.js'

const site = useSiteStore()
</script>

<template>
  <footer class="visitor-footer">
    <div class="footer-content">
      <p v-if="site.config.footerText" class="footer-text">{{ site.config.footerText }}</p>
      <div class="footer-records">
        <span v-if="site.config.icp" class="record-item">{{ site.config.icp }}</span>
        <span v-if="site.config.publicSecurityRecord" class="record-item">{{ site.config.publicSecurityRecord }}</span>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.visitor-footer {
  width: 100%;
  padding: 48px 24px 32px;
  border-top: 1px solid var(--line);
  margin-top: auto;
}

.footer-content {
  max-width: var(--content-max, 960px);
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  text-align: center;
}

.footer-text {
  font-size: 13px;
  color: var(--ink-faint);
  letter-spacing: 0.05em;
  line-height: 1.6;
}

.footer-records {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 16px;
}

.record-item {
  font-size: 12px;
  color: var(--ink-light);
  letter-spacing: 0.05em;
}

@media (max-width: 768px) {
  .visitor-footer {
    padding: 32px 16px 24px;
  }

  .footer-records {
    flex-direction: column;
    gap: 6px;
  }
}
</style>
