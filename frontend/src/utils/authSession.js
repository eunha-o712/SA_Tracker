const STORAGE_KEY = 'satrk.auth'
const CHANGE_EVENT = 'satrk-auth-change'

export function readAuthSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(STORAGE_KEY))
    if (!session?.token || !session?.user) return null
    return session
  } catch {
    return null
  }
}

export function saveAuthSession(session) {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  localStorage.removeItem(STORAGE_KEY)
  window.dispatchEvent(new Event(CHANGE_EVENT))
}

export function clearAuthSession() {
  sessionStorage.removeItem(STORAGE_KEY)
  localStorage.removeItem(STORAGE_KEY)
  window.dispatchEvent(new Event(CHANGE_EVENT))
}

export function subscribeToAuthSession(listener) {
  window.addEventListener(CHANGE_EVENT, listener)
  window.addEventListener('storage', listener)
  return () => {
    window.removeEventListener(CHANGE_EVENT, listener)
    window.removeEventListener('storage', listener)
  }
}
