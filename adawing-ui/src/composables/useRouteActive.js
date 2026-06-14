import { useRoute } from 'vue-router'

export function useRouteActive() {
  const route = useRoute()

  function isActive(path, exact = false) {
    if (path === '/') return route.path === '/'
    if (exact) return route.path === path
    return route.path.startsWith(path)
  }

  return { isActive }
}
