import { useCallback, useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import { invalidateCachedGet } from '../../api/apiCache'
import { readAuthSession, subscribeToAuthSession } from '../../utils/authSession'
import './PlayerFavorites.css'

function PlayerFavorites() {
  const navigate = useNavigate()
  const [favorites, setFavorites] = useState(readInitialFavoriteCache)
  const [loading, setLoading] = useState(shouldShowInitialFavoriteLoader)
  const [refreshing, setRefreshing] = useState(false)
  const [lastRefreshedAt, setLastRefreshedAt] = useState(readInitialFavoriteRefreshTime)
  const [error, setError] = useState('')
  const [removingId, setRemovingId] = useState(null)
  const [now, setNow] = useState(() => Date.now())
  const [authSession, setAuthSession] = useState(readAuthSession)
  const carouselRef = useRef(null)
  const [carouselState, setCarouselState] = useState({
    canPrevious: false,
    canNext: false,
    thumbSize: 100,
    thumbOffset: 0,
  })
  const isLoggedIn = Boolean(authSession)

  const syncCarousel = useCallback(() => {
    const viewport = carouselRef.current
    if (!viewport) return

    const maxScroll = Math.max(0, viewport.scrollWidth - viewport.clientWidth)
    const thumbSize = viewport.scrollWidth > 0
      ? Math.max(12, Math.min(100, (viewport.clientWidth / viewport.scrollWidth) * 100))
      : 100
    const thumbOffset = maxScroll > 0
      ? (viewport.scrollLeft / maxScroll) * (100 - thumbSize)
      : 0

    setCarouselState({
      canPrevious: viewport.scrollLeft > 2,
      canNext: viewport.scrollLeft < maxScroll - 2,
      thumbSize,
      thumbOffset,
    })
  }, [])

  useEffect(() => subscribeToAuthSession(() => {
    const nextSession = readAuthSession()
    setAuthSession(nextSession)

    if (!nextSession) {
      setFavorites([])
      setError('')
      setRemovingId(null)
      setLoading(false)
      setRefreshing(false)
      setLastRefreshedAt(null)
      return
    }

    const cacheKey = getFavoriteCacheKey(nextSession)
    const cached = readFavoriteCache(cacheKey)
    setFavorites(applyOwnClanEvidence(cached.items, nextSession))
    setLastRefreshedAt(cached.refreshedAt)
    setLoading(!hasFavoriteCache(cacheKey))
  }), [])

  useEffect(() => {
    let active = true
    const sessionToken = authSession?.token

    const loadFavorites = async () => {
      if (!sessionToken) {
        setFavorites([])
        setLoading(false)
        return
      }

      try {
        setError('')
        const cacheKey = getFavoriteCacheKey(authSession)
        const cached = readFavoriteCache(cacheKey)

        if (cached.exists) {
          setFavorites(applyOwnClanEvidence(cached.items, authSession))
          setLastRefreshedAt(cached.refreshedAt)
          setLoading(false)
          return
        }

        setLoading(true)
        const snapshot = await fetchFavoriteSnapshot(authSession)
        writeFavoriteCache(cacheKey, snapshot.items, snapshot.refreshedAt)
        if (active && isSameSession(sessionToken)) {
          setFavorites(snapshot.items)
          setLastRefreshedAt(snapshot.refreshedAt)
        }
      } catch (requestError) {
        if (active && isSameSession(sessionToken)) setError(getApiErrorMessage(requestError, 'Failed to load favorites.'))
      } finally {
        if (active && isSameSession(sessionToken)) setLoading(false)
      }
    }

    loadFavorites()
    const timerId = window.setInterval(() => setNow(Date.now()), 60_000)
    return () => {
      active = false
      window.clearInterval(timerId)
    }
  }, [authSession])

  const handleRefresh = async () => {
    const session = readAuthSession()
    if (!session || refreshing) return

    try {
      setRefreshing(true)
      setError('')
      invalidateCachedGet('/api/favorite')
      invalidateCachedGet('/api/player')
      invalidateCachedGet('/api/match/summary')
      invalidateCachedGet('/api/clan/members')
      const snapshot = await fetchFavoriteSnapshot(session)
      if (!isSameSession(session.token)) return

      writeFavoriteCache(getFavoriteCacheKey(session), snapshot.items, snapshot.refreshedAt)
      setFavorites(snapshot.items)
      setLastRefreshedAt(snapshot.refreshedAt)
    } catch (requestError) {
      if (isSameSession(session.token)) {
        setError(getApiErrorMessage(requestError, '즐겨찾기 전적을 새로고침하지 못했습니다.'))
      }
    } finally {
      if (isSameSession(session.token)) setRefreshing(false)
    }
  }

  useEffect(() => {
    const viewport = carouselRef.current
    if (!viewport) return undefined

    syncCarousel()
    viewport.addEventListener('scroll', syncCarousel, { passive: true })
    const resizeObserver = new ResizeObserver(syncCarousel)
    resizeObserver.observe(viewport)

    return () => {
      viewport.removeEventListener('scroll', syncCarousel)
      resizeObserver.disconnect()
    }
  }, [favorites.length, loading, syncCarousel])

  const slideFavorites = (direction) => {
    const viewport = carouselRef.current
    if (!viewport) return
    viewport.scrollBy({
      left: direction * Math.max(320, viewport.clientWidth * 0.82),
      behavior: 'smooth',
    })
  }

  const handleDelete = async (id) => {
    try {
      setRemovingId(id)
      setError('')
      await api.delete(`/api/favorite/${id}`)
      invalidateCachedGet('/api/favorite')
      setFavorites((current) => {
        const nextFavorites = current.filter((favorite) => favorite.id !== id)
        writeFavoriteCache(getFavoriteCacheKey(authSession), nextFavorites, lastRefreshedAt)
        return nextFavorites
      })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Failed to remove favorite.'))
    } finally {
      setRemovingId(null)
    }
  }

  return (
    <section className="player-favorites">
      <div className="player-favorites-header">
        <div className="player-favorites-title">
          <span>FAVORITE</span>
          <h2>PLAYERS</h2>
        </div>
        {isLoggedIn && (
          <div className="player-favorites-refresh">
            <small>{formatRefreshTime(lastRefreshedAt)}</small>
            <button type="button" disabled={loading || refreshing} onClick={handleRefresh}>
              <span aria-hidden="true">↻</span>
              {refreshing ? '새로고침 중...' : '전적 새로고침'}
            </button>
          </div>
        )}
      </div>

      {loading && <FavoritesSkeleton />}
      {!loading && error && <div className="player-favorites-message error" role="alert">{error}</div>}

      {!loading && !error && !isLoggedIn && (
        <div className="player-favorites-message">
          로그인하면 즐겨찾기한 플레이어를 확인할 수 있습니다.
          <small><Link to="/login">로그인하기</Link></small>
        </div>
      )}

      {!loading && !error && isLoggedIn && favorites.length === 0 && (
        <div className="player-favorites-message">
          No favorite players yet.
          <small>Add favorites from a player profile.</small>
        </div>
      )}

      {!loading && favorites.length > 0 && (
        <div className="favorite-carousel">
          <div
            className="player-card-grid"
            ref={carouselRef}
            tabIndex="0"
            aria-label="즐겨찾기 플레이어 목록"
          >
            {favorites.map((favorite) => {
            const matchStatus = getMatchStatus(favorite.latestMatchDate, now)
            return (
              <article className="favorite-player-card" key={favorite.id}>
                <div className="favorite-top">
                  <div className="favorite-rank">
                    {favorite.seasonGradeImage
                      ? <img src={favorite.seasonGradeImage} alt={favorite.seasonGrade} />
                      : <img src="/sa-assets/sa-profile-basic.png" alt="" />}
                  </div>
                  <div className="favorite-name">
                    <h3>{favorite.userName}</h3>
                    <p>{favorite.titleName}</p>
                  </div>
                  <div className="favorite-login">
                    <span><i className={matchStatus.recent ? 'active' : ''} />LAST MATCH</span>
                    <strong className={matchStatus.recent ? 'active' : ''}>{matchStatus.label}</strong>
                  </div>
                </div>
                <div className="favorite-divider" />
                <div className="favorite-clan">
                  <div className="favorite-clan-mark"><img src="/sa-assets/sa-clan-basic.png" alt="" /></div>
                  <strong>{favorite.clanName}</strong>
                </div>
                <div className="favorite-divider" />
                <div className="favorite-kda">
                  <span>AVG K / D / A</span>
                  <div>
                    <b>{formatStat(favorite.kda.kill)}</b>
                    <em>/</em>
                    <b className="death">{formatStat(favorite.kda.death)}</b>
                    <em>/</em>
                    <b className="assist">{formatStat(favorite.kda.assist)}</b>
                  </div>
                </div>
                <div className="favorite-actions">
                  <button type="button" onClick={() => navigate(`/player/${encodeURIComponent(favorite.userName)}`)}>프로필 보기</button>
                  <button type="button" className="remove" disabled={removingId === favorite.id} onClick={() => handleDelete(favorite.id)}>
                    {removingId === favorite.id ? '삭제 중' : '삭제'}
                  </button>
                </div>
              </article>
            )
            })}
          </div>
          <div className="favorite-carousel-controls">
            <button
              type="button"
              aria-label="이전 즐겨찾기"
              disabled={!carouselState.canPrevious}
              onClick={() => slideFavorites(-1)}
            >
              이전
            </button>
            <div className="favorite-carousel-track" aria-hidden="true">
              <span
                style={{
                  width: `${carouselState.thumbSize}%`,
                  left: `${carouselState.thumbOffset}%`,
                }}
              />
            </div>
            <button
              type="button"
              aria-label="다음 즐겨찾기"
              disabled={!carouselState.canNext}
              onClick={() => slideFavorites(1)}
            >
              다음
            </button>
          </div>
        </div>
      )}
    </section>
  )
}

async function fetchFavoriteSnapshot(session) {
  const response = await api.get('/api/favorite')
  const storedFavorites = Array.isArray(response.data) ? response.data : []
  let roster

  if (containsOwnFavorite(storedFavorites, session)) {
    try {
      const rosterResponse = await api.get('/api/clan/members')
      roster = Array.isArray(rosterResponse.data) ? rosterResponse.data : []
    } catch {
      roster = undefined
    }
  }

  const items = applyOwnClanEvidence(
    await Promise.all(storedFavorites.map(enrichFavorite)),
    session,
    roster,
  )

  return { items, refreshedAt: Date.now() }
}

async function enrichFavorite(favorite) {
  const [playerResult, summaryResult] = await Promise.allSettled([
    api.get('/api/player', { params: { userName: favorite.userName } }),
    api.get('/api/match/summary', { params: { userName: favorite.userName } }),
  ])
  const player = playerResult.status === 'fulfilled' ? playerResult.value.data : null
  const summary = summaryResult.status === 'fulfilled' ? summaryResult.value.data : null
  const recent = summary?.summaries?.find((item) => item.key === 'RECENT')

  return {
    ...favorite,
    titleName: cleanText(player?.basic?.title_name) || 'NO TITLE',
    seasonGrade: player?.rank?.season_grade || 'SEASON GRADE',
    seasonGradeImage: player?.images?.seasonGradeImage || null,
    clanName: cleanText(player?.basic?.clan_name) || 'NO CLAN',
    latestMatchDate: recent?.latestMatchDate || null,
    kda: { kill: recent?.averageKill, death: recent?.averageDeath, assist: recent?.averageAssist },
  }
}

function isSameSession(sessionToken) {
  return readAuthSession()?.token === sessionToken
}

function getFavoriteCacheKey(session) {
  const user = session?.user
  const ownerKey = user?.id || user?.email || user?.loginId || 'guest'
  return `satrk.favorite.details.${ownerKey}`
}

function readFavoriteCache(cacheKey) {
  try {
    const cached = JSON.parse(localStorage.getItem(cacheKey))
    if (!Array.isArray(cached?.items)) return { exists: false, refreshedAt: null, items: [] }
    return {
      exists: true,
      refreshedAt: Number(cached.refreshedAt || cached.cachedAt) || null,
      items: cached.items,
    }
  } catch {
    return { exists: false, refreshedAt: null, items: [] }
  }
}

function hasFavoriteCache(cacheKey) {
  return readFavoriteCache(cacheKey).exists
}

function writeFavoriteCache(cacheKey, items, refreshedAt = null) {
  try {
    localStorage.setItem(cacheKey, JSON.stringify({
      refreshedAt: Number(refreshedAt) || null,
      updatedAt: Date.now(),
      items,
    }))
  } catch {
    // Local cache is optional.
  }
}

function normalizeFavoriteName(userName) {
  return String(userName || '').trim().toLowerCase()
}

function readInitialFavoriteCache() {
  const session = readAuthSession()
  if (!session) return []
  return applyOwnClanEvidence(readFavoriteCache(getFavoriteCacheKey(session)).items, session)
}

function readInitialFavoriteRefreshTime() {
  const session = readAuthSession()
  if (!session) return null
  return readFavoriteCache(getFavoriteCacheKey(session)).refreshedAt
}

function shouldShowInitialFavoriteLoader() {
  const session = readAuthSession()
  return Boolean(session && !hasFavoriteCache(getFavoriteCacheKey(session)))
}

function containsOwnFavorite(favorites, session) {
  const accountName = session?.user?.suddenNickname || session?.user?.displayName || ''
  if (!accountName) return false
  return favorites.some((favorite) => normalizeFavoriteName(favorite.userName) === normalizeFavoriteName(accountName))
}

function applyOwnClanEvidence(favorites, session, roster) {
  const accountName = session?.user?.suddenNickname || session?.user?.displayName || ''
  if (!accountName) return favorites

  return favorites.map((favorite) => {
    if (normalizeFavoriteName(favorite.userName) !== normalizeFavoriteName(accountName)) return favorite

    if (!Array.isArray(roster)) {
      return favorite.clanVerified
        ? favorite
        : { ...favorite, clanName: 'NO CLAN', clanVerified: false }
    }

    const reportedClanName = cleanText(favorite.clanName)
    const membership = roster.find((member) => (
      normalizeFavoriteName(member.userName) === normalizeFavoriteName(accountName)
      && normalizeFavoriteName(member.clanName) === normalizeFavoriteName(reportedClanName)
    ))

    return {
      ...favorite,
      clanName: membership?.clanName || 'NO CLAN',
      clanVerified: true,
    }
  })
}

function FavoritesSkeleton() {
  return (
    <div className="player-card-grid favorite-skeleton-grid" aria-busy="true" aria-label="Loading favorites.">
      {[0, 1].map((item) => (
        <div className="favorite-skeleton-card" key={item}>
          <div className="favorite-skeleton-avatar favorite-skeleton-shimmer" />
          <div className="favorite-skeleton-lines">
            <span className="favorite-skeleton-shimmer" />
            <span className="favorite-skeleton-shimmer" />
          </div>
          <div className="favorite-skeleton-block favorite-skeleton-shimmer" />
        </div>
      ))}
    </div>
  )
}

function cleanText(value) {
  if (value === null || value === undefined) return ''
  const normalized = String(value).trim()
  if (!normalized) return ''
  if (['-', 'null', 'undefined', 'none', 'no clan', 'no title'].includes(normalized.toLowerCase())) return ''
  return normalized
}

function formatStat(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(1) : '-'
}

function formatRefreshTime(value) {
  if (!value) return '아직 새로고침하지 않았습니다.'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '마지막 새로고침 시간 없음'

  return `마지막 새로고침 ${new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date)}`
}

function getMatchStatus(value, now) {
  if (!value) return { label: '-', recent: false }
  const elapsed = Math.max(0, now - new Date(value).getTime())
  if (!Number.isFinite(elapsed)) return { label: '-', recent: false }
  const minutes = Math.floor(elapsed / 60_000)
  const hours = Math.floor(elapsed / 3_600_000)
  const days = Math.floor(elapsed / 86_400_000)
  if (minutes < 1) return { label: 'JUST NOW', recent: true }
  if (minutes < 60) return { label: `${minutes}M AGO`, recent: true }
  if (hours < 24) return { label: `${hours}H AGO`, recent: true }
  return { label: `${days}D AGO`, recent: false }
}

export default PlayerFavorites
