<script setup>
import { ref, onMounted, computed } from 'vue'
import { useSiteStore } from '@/stores/site.js'
import { useScrollReveal } from '@/composables/useScrollReveal.js'
import { resourceContentUrl } from '@/utils/resourceUrl.js'

const site = useSiteStore()
const loading = ref(false)
const containerRef = ref(null)

useScrollReveal(containerRef, '.reveal')

const text = (value) => value == null ? '' : String(value).trim()
const list = (value) => Array.isArray(value) ? value : []

function normalizeAbout(config) {
  const raw = config.about || {}
  const rawPin = raw.pin || {}
  const rawAbility = raw.ability || {}
  const rawContact = raw.contact || {}
  const rawLinks = raw.links || {}
  const rawSiteInfo = raw.siteInfo || {}
  const rawLicense = rawSiteInfo.license || {}
  const rawContent = raw.siteContent || {}

  return {
    pin: {
      ownerName: text(rawPin.ownerName),
      avatar: text(rawPin.avatar),
      signature: text(rawPin.signature),
      job: text(rawPin.job),
      unit: text(rawPin.unit),
      experience: list(rawPin.experience)
    },
    ability: {
      devStack: list(rawAbility.devStack)
    },
    contact: {
      email: text(rawContact.email),
      otherSocialPlatform: list(rawContact.otherSocialPlatform)
    },
    links: {
      items: list(rawLinks.items)
    },
    siteInfo: {
      license: {
        enabled: Boolean(rawLicense.enabled),
        name: text(rawLicense.name),
        url: text(rawLicense.url)
      }
    },
    siteContent: {
      intro: text(rawContent.intro)
    }
  }
}

const about = computed(() => normalizeAbout(site.config))
const validExperience = computed(() => about.value.pin.experience.filter(item => item && (text(item.period) || text(item.title) || text(item.unit) || text(item.description))))
const validDevStack = computed(() => about.value.ability.devStack.filter(item => item && (text(item.name) || text(item.category))))
const validContacts = computed(() => about.value.contact.otherSocialPlatform.filter(item => item && text(item.url)))
const validLinks = computed(() => about.value.links.items.filter(item => item && text(item.url)))

const hasPin = computed(() => Boolean(
  about.value.pin.ownerName || about.value.pin.avatar || about.value.pin.signature ||
  about.value.pin.job || about.value.pin.unit || about.value.siteContent.intro || validExperience.value.length
))
const hasAbility = computed(() => validDevStack.value.length > 0)
const hasLinks = computed(() => validLinks.value.length > 0)
const hasContact = computed(() => Boolean(about.value.contact.email || validContacts.value.length))
const hasSiteInfo = computed(() => Boolean(about.value.siteInfo.license.enabled && (about.value.siteInfo.license.name || about.value.siteInfo.license.url)))

function itemLabel(item, fallback) {
  return text(item.name) || text(item.title) || fallback
}

function linkSecondary(item) {
  return text(item.description) || text(item.section)
}

function contactSecondary(item) {
  const url = text(item.url)
  if (!url) return ''
  try {
    return new URL(url).hostname.replace(/^www\./, '')
  } catch {
    return url
  }
}

onMounted(() => {
  loading.value = true
  site.load().finally(() => {
    loading.value = false
  })
})
</script>

<template>
  <main ref="containerRef" class="about-ori about-page">
    <div v-if="loading" class="loading-ori">Loading...</div>

    <template v-else>
      <section v-if="hasPin" class="about-section about-profile reveal">
        <div v-if="about.pin.avatar" class="about-avatar">
          <img :src="resourceContentUrl(about.pin.avatar)" :alt="about.pin.ownerName || 'Avatar'" />
        </div>
        <h1 v-if="about.pin.ownerName" class="about-name">{{ about.pin.ownerName }}</h1>
        <p v-if="about.pin.signature" class="about-signature">{{ about.pin.signature }}</p>
        <div v-if="about.pin.job || about.pin.unit" class="about-meta">
          <span v-if="about.pin.job">{{ about.pin.job }}</span>
          <span v-if="about.pin.unit">{{ about.pin.unit }}</span>
        </div>
        <p v-if="about.siteContent.intro" class="about-bio">{{ about.siteContent.intro }}</p>

        <template v-if="validExperience.length">
          <div class="about-divider about-divider-labeled"><span>Experience</span></div>
          <ol class="experience-list">
            <li v-for="(item, index) in validExperience" :key="`experience-${index}`" class="experience-item">
              <span class="experience-marker">
                <img v-if="item.icon" :src="resourceContentUrl(item.icon)" :alt="itemLabel(item, 'Experience')" class="experience-icon" />
                <span v-else></span>
              </span>
              <div class="experience-content">
                <div class="experience-topline">
                  <time v-if="text(item.period)">{{ item.period }}</time>
                  <strong v-if="text(item.title)">{{ item.title }}</strong>
                  <span v-if="text(item.unit)" class="experience-unit">{{ item.unit }}</span>
                </div>
                <p v-if="text(item.description)">{{ item.description }}</p>
              </div>
            </li>
          </ol>
        </template>
      </section>

      <section v-if="hasAbility" class="about-section about-ability reveal">
        <div class="about-divider about-divider-labeled"><span>Ability</span></div>
        <div class="about-ability-list">
          <span v-for="(item, index) in validDevStack" :key="`ability-${index}`" class="about-ability-item">
            <img v-if="item.icon" :src="resourceContentUrl(item.icon)" :alt="itemLabel(item, 'Ability')" />
            <strong>{{ itemLabel(item, 'Ability') }}</strong>
            <small v-if="text(item.category)">{{ item.category }}</small>
          </span>
        </div>
      </section>

      <section v-if="hasLinks" class="about-section about-links reveal">
        <div class="about-divider about-divider-labeled"><span>Links</span></div>
        <div class="about-link-list">
          <a v-for="(item, index) in validLinks" :key="`link-${index}`" :href="item.url" target="_blank" rel="noopener noreferrer" class="about-link-item">
            <img v-if="item.icon" :src="resourceContentUrl(item.icon)" :alt="itemLabel(item, 'Link')" />
            <span>{{ itemLabel(item, 'Link') }}</span>
            <small v-if="linkSecondary(item)">{{ linkSecondary(item) }}</small>
          </a>
        </div>
      </section>

      <section v-if="hasContact" class="about-section about-contact reveal">
        <div class="about-divider about-divider-labeled"><span>Contact</span></div>
        <div class="about-contact-list">
          <span v-if="about.contact.email" class="about-contact-email">
            <span class="contact-email-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
                <rect x="3" y="5" width="18" height="14" rx="1.5" />
                <path d="m4 7 8 6 8-6" />
              </svg>
            </span>
            <span>{{ about.contact.email }}</span>
          </span>
          <a v-for="(item, index) in validContacts" :key="`contact-${index}`" :href="item.url" target="_blank" rel="noopener noreferrer" class="about-contact-item">
            <img v-if="item.icon" :src="resourceContentUrl(item.icon)" :alt="itemLabel(item, 'Contact')" />
            <span>{{ itemLabel(item, 'Contact') }}</span>
            <small v-if="contactSecondary(item)">{{ contactSecondary(item) }}</small>
          </a>
        </div>
      </section>

      <section v-if="hasSiteInfo" class="about-section about-site-info reveal">
        <div class="about-divider about-divider-labeled"><span>Content-license</span></div>
        <a v-if="about.siteInfo.license.url" class="license-link" :href="about.siteInfo.license.url" target="_blank" rel="noopener noreferrer">{{ about.siteInfo.license.name || 'Creative Commons' }}</a>
        <span v-else class="license-name">{{ about.siteInfo.license.name }}</span>
      </section>
    </template>
  </main>
</template>
