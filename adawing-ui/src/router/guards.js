import { useAuthStore } from '@/stores/auth.js'

export function setupRouterGuards(router) {
  router.beforeEach((to, from, next) => {
    const auth = useAuthStore()
    if (to.meta.requiresAuth && !auth.isLoggedIn) {
      next({ name: 'AdminLogin', query: { redirect: to.fullPath } })
    } else if (to.name === 'AdminLogin' && auth.isLoggedIn) {
      next({ name: 'Dashboard' })
    } else {
      next()
    }
  })
}
