<script setup>
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { useRouteActive } from '@/composables/useRouteActive.js'

const { isActive } = useRouteActive()
const auth = useAuthStore()

const menus = [
  { name: 'Dashboard', label: 'Dashboard', path: '/v2/ren/admin' },
  { name: 'AdminArticles', label: 'Articles', path: '/v2/ren/admin/articles' },
  { name: 'AdminReview', label: 'Review', path: '/v2/ren/admin/review' },
  { name: 'AdminMessages', label: 'Messages', path: '/v2/ren/admin/messages' },
  { name: 'AdminMoments', label: 'Moments', path: '/v2/ren/admin/moments' },
  { name: 'AdminTags', label: 'Tags', path: '/v2/ren/admin/tags' },
  { name: 'AdminResources', label: 'Resources', path: '/v2/ren/admin/resources' },
  { name: 'AdminSettings', label: 'Settings', path: '/v2/ren/admin/settings' },
  { name: 'AdminAccount', label: 'Account', path: '/v2/ren/admin/account' }
]

function logout() {
  auth.logout()
  window.location.href = '/v2/ren/admin/login'
}
</script>

<template>
  <aside class="admin-sidebar-ori">
    <RouterLink to="/" class="logo">AdaWing</RouterLink>
    <nav>
      <RouterLink
        v-for="menu in menus"
        :key="menu.name"
        :to="menu.path"
        :class="{ active: isActive(menu.path, menu.name === 'Dashboard') }"
      >
        {{ menu.label }}
      </RouterLink>
    </nav>
    <div style="margin-top: auto; padding-top: 40px;">
      <a href="#" style="font-size: 11px; color: var(--ink-faint); letter-spacing: 0.1em;" @click.prevent="logout">Logout</a>
    </div>
  </aside>
</template>
