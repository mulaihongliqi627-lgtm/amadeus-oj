import type { RouteLocationNormalized } from 'vue-router'
import type { useAuthStore } from '../stores/auth'
import { ApiError } from '../api/client'

// Only return to known local admin routes; query strings cannot become redirect URLs.
export function getLoginDestination(value: unknown): string {
  return typeof value === 'string' &&
    ['/dashboard', '/admins', '/questions', '/contests'].includes(value)
    ? value
    : '/dashboard'
}

type AuthStore = Pick<
  ReturnType<typeof useAuthStore>,
  'session' | 'isLoggedIn' | 'fetchAndPersistUserInfo' | 'clear'
>

export async function guardRoute(to: RouteLocationNormalized, auth: AuthStore) {
  const isLogin = to.name === 'login'
  if (to.meta.public && !isLogin) return
  if (!auth.isLoggedIn) {
    if (!isLogin) return { name: 'login', query: { redirect: to.path } }
    return
  }

  const token = auth.session?.token
  try {
    // The server checks both the JWT signature and the Redis session before mounting the page.
    // This navigation owns its redirect; the request handler must not start another navigation.
    await auth.fetchAndPersistUserInfo({ redirectOnUnauthorized: false })
  } catch (cause) {
    // An old request must never clear a newer login or admit an unverified session.
    if (auth.session?.token !== token) return false
    const expired = cause instanceof ApiError && cause.code === 3001
    if (expired) auth.clear()
    const query = {
      redirect: isLogin ? getLoginDestination(to.query.redirect) : to.path,
      ...(expired ? { expired: '1' } : { unavailable: '1' }),
    }
    if (isLogin && to.query[expired ? 'expired' : 'unavailable'] === '1') return
    return { name: 'login', query }
  }
  if (auth.session?.token !== token) return false
  if (isLogin) return getLoginDestination(to.query.redirect)
}
