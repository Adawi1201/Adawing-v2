<script setup>
import { RouterLink } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { useRouteActive } from '@/composables/useRouteActive.js'

const { isActive } = useRouteActive()
const auth = useAuthStore()

const menus = [
  { name: 'Dashboard', label: 'Dashboard', path: '/yusal/admin' },
  { name: 'AdminArticles', label: 'Articles', path: '/yusal/admin/articles' },
  { name: 'AdminReview', label: 'Review', path: '/yusal/admin/review' },
  { name: 'AdminMessages', label: 'Messages', path: '/yusal/admin/messages' },
  { name: 'AdminMoments', label: 'Moments', path: '/yusal/admin/moments' },
  { name: 'AdminTags', label: 'Tags', path: '/yusal/admin/tags' },
  { name: 'AdminResources', label: 'Resources', path: '/yusal/admin/resources' },
  { name: 'AdminSettings', label: 'Settings', path: '/yusal/admin/settings' },
  { name: 'AdminAccount', label: 'Account', path: '/yusal/admin/account' }
]

function logout() {
  auth.logout()
  window.location.href = '/yusal/admin/login'
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
