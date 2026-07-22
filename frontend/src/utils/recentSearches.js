const STORAGE_KEY = 'satrk:recent-player-searches'
const MAX_RECENT_SEARCHES = 6

export function readRecentSearches() {
  try {
    const stored = JSON.parse(window.localStorage.getItem(STORAGE_KEY) || '[]')
    return Array.isArray(stored)
      ? stored
          .filter((item) => typeof item === 'string' && item.trim())
          .map((item) => item.trim())
          .slice(0, MAX_RECENT_SEARCHES)
      : []
  } catch {
    return []
  }
}

export function addRecentSearch(userName) {
  const trimmed = userName.trim()
  if (!trimmed) return readRecentSearches()

  const normalized = normalizeSearch(trimmed)
  const nextSearches = [
    trimmed,
    ...readRecentSearches().filter(
      (item) => normalizeSearch(item) !== normalized
    ),
  ].slice(0, MAX_RECENT_SEARCHES)

  writeRecentSearches(nextSearches)
  return nextSearches
}

export function removeRecentSearch(userName) {
  const normalized = normalizeSearch(userName)
  const nextSearches = readRecentSearches().filter(
    (item) => normalizeSearch(item) !== normalized
  )
  writeRecentSearches(nextSearches)
  return nextSearches
}

export function clearRecentSearches() {
  writeRecentSearches([])
  return []
}

function writeRecentSearches(searches) {
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(searches))
  } catch {
    // 검색은 계속 동작해야 하므로 저장소 사용이 막혀도 무시한다.
  }
}

function normalizeSearch(value) {
  return value.trim().toLocaleLowerCase('ko-KR')
}
