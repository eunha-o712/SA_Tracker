import api from './api'
import { readAuthSession } from '../utils/authSession'

const DEFAULT_TTL_MS = 5 * 60 * 1000
const requestCache = new Map()

export function cachedGet(url, config = {}, ttlMs = DEFAULT_TTL_MS) {
  const key = createCacheKey(url, config.params)
  const cached = requestCache.get(key)

  if (cached && cached.expiresAt > Date.now()) {
    return cached.promise
  }

  const promise = api.get(url, config).catch((error) => {
    if (requestCache.get(key)?.promise === promise) requestCache.delete(key)
    throw error
  })

  requestCache.set(key, { promise, expiresAt: Date.now() + ttlMs })
  return promise
}

export function invalidateCachedGet(url) {
  for (const key of requestCache.keys()) {
    if (key.includes(`:${url}?`)) requestCache.delete(key)
  }
}

export async function preloadSessionData(userName) {
  const normalizedName = String(userName || '').trim()
  if (!normalizedName) return

  await Promise.allSettled([
    cachedGet('/api/player', { params: { userName: normalizedName } }),
    cachedGet('/api/ranking', { params: { userName: normalizedName } }),
    cachedGet('/api/weapon', { params: { userName: normalizedName } }),
    cachedGet('/api/match/summary', { params: { userName: normalizedName } }),
    cachedGet('/api/map/stats', { params: { userName: normalizedName } }),
    cachedGet('/api/ai/record-room', { params: { userName: normalizedName } }),
    cachedGet('/api/favorite'),
  ])
}

function createCacheKey(url, params = {}) {
  const session = readAuthSession()
  const owner = session?.user?.id || session?.user?.email || session?.token || 'guest'
  const normalizedParams = Object.entries(params || {})
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([key, value]) => `${key}=${String(value)}`)
    .join('&')
  return `${owner}:${url}?${normalizedParams}`
}
