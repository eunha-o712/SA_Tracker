const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8085').replace(/\/$/, '')

export function resolveApiAssetUrl(value, fallback = '') {
  const url = String(value || '').trim()
  if (!url) return fallback
  if (/^(https?:|data:|blob:)/i.test(url)) return url
  return `${API_BASE_URL}${url.startsWith('/') ? url : `/${url}`}`
}
