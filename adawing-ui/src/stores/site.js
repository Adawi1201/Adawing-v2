import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getSiteConfig } from '@/api/config.js'

export const useSiteStore = defineStore('site', () => {
  const emptyAbout = () => ({
    pin: { ownerName: '', avatar: '', signature: '', job: '', unit: '', experience: [] },
    ability: { devStack: [] },
    contact: { email: '', otherSocialPlatform: [] },
    links: { items: [] },
    siteInfo: { license: { enabled: false, name: '', url: '' } },
    siteContent: { intro: '' }
  })

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
    about: emptyAbout()
  })

  let pending = null

  async function load() {
    if (pending) return pending
    pending = (async () => {
      try {
        const res = await getSiteConfig()
        const source = res.data || res || {}
        const abilitySource = source.about?.ability || {}
        const ability = { devStack: Array.isArray(abilitySource.devStack) ? abilitySource.devStack : [] }
        const contentSource = source.about?.siteContent || {}
        const siteContent = { intro: contentSource.intro || '' }
        config.value = {
          ...config.value,
          ...source,
          seo: { ...config.value.seo, ...(source.seo || {}) },
          about: {
            ...config.value.about,
            ...(source.about || {}),
            pin: { ...config.value.about.pin, ...(source.about?.pin || {}) },
            ability,
            contact: { ...config.value.about.contact, ...(source.about?.contact || {}) },
            links: {
              ...config.value.about.links,
              ...(source.about?.links || {}),
              items: Array.isArray(source.about?.links?.items) ? source.about.links.items : []
            },
            siteInfo: {
              ...config.value.about.siteInfo,
              ...(source.about?.siteInfo || {}),
              license: { ...config.value.about.siteInfo.license, ...(source.about?.siteInfo?.license || {}) }
            },
            siteContent
          }
        }
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
