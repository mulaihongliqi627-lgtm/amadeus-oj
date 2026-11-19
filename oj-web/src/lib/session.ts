const STORAGE_KEY = 'tyler-oj.admin.session'

export interface Session {
  token: string
  account: string
  nickName?: string
}

// Claims are only used for display. The server verifies the signature and Redis session.
export function getTokenNickname(token: string): string | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/')
    const bytes = Uint8Array.from(atob(base64), (char) => char.charCodeAt(0))
    const claims: unknown = JSON.parse(new TextDecoder().decode(bytes))
    if (typeof claims === 'object' && claims !== null && 'nickName' in claims) {
      return typeof claims.nickName === 'string' && claims.nickName.trim() ? claims.nickName : null
    }
  } catch {
    // Older accounts may have no nickname in their token.
  }
  return null
}

function restoreSession(): Session | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const value: unknown = JSON.parse(raw)
    if (
      typeof value === 'object' &&
      value !== null &&
      'token' in value &&
      'account' in value &&
      typeof value.token === 'string' &&
      value.token.split('.').length === 3 &&
      typeof value.account === 'string'
    ) {
      return {
        token: value.token,
        account: value.account as string,
        nickName: 'nickName' in value && typeof value.nickName === 'string' ? value.nickName : undefined,
      }
    }
  } catch {
    // A disabled browser storage still permits an in-memory login.
  }
  return null
}

let currentSession = restoreSession()

export function readSession(): Session | null {
  return currentSession
}

export function writeSession(session: Session | null) {
  currentSession = session
  try {
    if (session) sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
    else sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    // The active session remains available until this page is refreshed.
  }
}
