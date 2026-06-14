import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getSiteConfig } from '@/api/config.js'

export const useSiteStore = defineStore('site', () => {
  const config = ref({
    name: '',
    description: '',
    subtitle: '',
    logo: '',
    favicon: '',
    icp: '',
    publicSecurityRecord: '',
    footerText: '',
    seo: { keywords: '', description: '' },
    profile: { ownerName: '', avatar: '', bio: '', signature: '' },
    links: []
  })

  let pending = null

  async function load() {
    if (pending) return pending
    pending = (async () => {
      try {
        const res = await getSiteConfig()
        config.value = { ...config.value, ...(res.data || res) }
      } catch (e) {
        // ignore, use defaults
      } finally {
        pending = null
      }
    })()
    return pending
  }

  return { config, load }
})
