import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { getTokenNickname, readSession, writeSession, type Session } from '../lib/session'
import { getUserInfo, logout } from '../api/admin'

export const useAuthStore = defineStore('auth', () => {
  const session = ref<Session | null>(readSession())
  const isLoggedIn = computed(() => Boolean(session.value?.token))
  const displayName = computed(() => {
    if (!session.value) return '管理员'
    return session.value.nickName || getTokenNickname(session.value.token) || session.value.account
  })

  function signIn(token: string, account: string) {
    session.value = { token, account }
    writeSession(session.value)
  }

  async function fetchAndPersistUserInfo(options?: { redirectOnUnauthorized?: boolean }) {
    const currentSession = session.value
    if (!currentSession) return
    const info = await getUserInfo(options)
    if (session.value?.token !== currentSession.token) return
    session.value = { ...currentSession, account: info.userAccount, nickName: info.nickName }
    writeSession(session.value)
  }

  async function signOut() {
    const currentSession = session.value
    if (!currentSession) return
    await logout()
    if (session.value?.token === currentSession.token) clear()
  }

  function clear() {
    session.value = null
    writeSession(null)
  }

  return { session, isLoggedIn, displayName, signIn, fetchAndPersistUserInfo, signOut, clear }
})
