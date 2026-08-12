import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import SearchBar from '../../components/SearchBar/SearchBar'
import PlayerProfile from '../../components/PlayerProfile/PlayerProfile'
import RankSummary from '../../components/PlayerProfile/RankSummary'
import WeaponSummary from '../../components/PlayerProfile/WeaponSummary'
import MapPerformance from '../../components/PlayerProfile/MapPerformance'
import MatchSummary from '../../components/PlayerProfile/MatchSummary'
import CombatSummary from '../../components/PlayerProfile/CombatSummary'
import api, { getApiErrorMessage } from '../../api/api'
import { cachedGet, invalidateCachedGet } from '../../api/apiCache'
import { readAuthSession, saveAuthSession } from '../../utils/authSession'
import PlayerPageSkeleton from './PlayerPageSkeleton'
import './PlayerPage.css'

function PlayerPage() {
  const { name } = useParams()
  const navigate = useNavigate()
  const [playerData, setPlayerData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [favoriteId, setFavoriteId] = useState(null)
  const [favoritePending, setFavoritePending] = useState(false)
  const [favoriteMessage, setFavoriteMessage] = useState('')
  const [profileClanName, setProfileClanName] = useState('')
  const [profileImagePending, setProfileImagePending] = useState(false)
  const [profileImageMessage, setProfileImageMessage] = useState('')

  useEffect(() => {
    if (![
      '프로필 이미지가 변경되었습니다.',
      '기본 이미지로 변경되었습니다.',
    ].includes(profileImageMessage)) return undefined

    const timerId = window.setTimeout(() => setProfileImageMessage(''), 2_800)
    return () => window.clearTimeout(timerId)
  }, [profileImageMessage])

  useEffect(() => {
    let active = true

    const fetchPlayerPageData = async () => {
      try {
        setLoading(true)
        setError('')
        setPlayerData(null)
        setFavoriteId(null)
        setFavoriteMessage('')
        setProfileClanName('')
        setProfileImageMessage('')

        const response = await cachedGet('/api/player', {
          params: { userName: name },
        })
        if (!active) return

        const session = readAuthSession()
        const currentName = response.data?.basic?.user_name || name
        const accountName = session?.user?.suddenNickname || session?.user?.displayName || ''
        const isOwnAccount = Boolean(
          session?.user?.ouid
          && normalizeFavoriteName(currentName) === normalizeFavoriteName(accountName),
        )
        const reportedClanName = cleanFavoriteText(response.data?.basic?.clan_name)
        const resolvedClanName = isOwnAccount && session?.user?.clanNone
          ? ''
          : reportedClanName

        setProfileClanName(resolvedClanName)
        setPlayerData(response.data)

        try {
          if (session) {
            const favoriteResponse = await cachedGet('/api/favorite')
            if (!active) return
            const favorite = (favoriteResponse.data ?? []).find((item) => (
              response.data?.ouid
                ? item.ouid === response.data.ouid
                : item.userName?.toLowerCase() === currentName?.toLowerCase()
            ))
            setFavoriteId(favorite?.id ?? null)
          } else {
            setFavoriteId(null)
          }
        } catch {
          if (active) setFavoriteId(null)
        }
      } catch (requestError) {
        if (active) setError(getApiErrorMessage(requestError, 'Failed to load player profile.'))
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchPlayerPageData()

    return () => {
      active = false
    }
  }, [name])

  const basic = playerData?.basic ?? {}
  const rank = playerData?.rank ?? {}
  const tier = playerData?.tier ?? {}
  const recent = playerData?.recent ?? {}
  const images = playerData?.images ?? {}
  const userName = basic.user_name || name
  const session = readAuthSession()
  const canEditProfileImage = Boolean(
    session?.user?.ouid
    && playerData?.ouid
    && session.user.ouid === playerData.ouid,
  )

  const applyProfileImageUpdate = (nextUser) => {
    const currentSession = readAuthSession()
    if (!currentSession || !nextUser) return
    saveAuthSession({ ...currentSession, user: nextUser })
    setPlayerData((current) => current ? {
      ...current,
      profileImageUrl: nextUser.profileImageUrl || null,
    } : current)
    updateFavoriteProfileImageCache(
      currentSession,
      playerData?.ouid,
      nextUser.profileImageUrl || null,
    )
    invalidateCachedGet('/api/player')
    invalidateCachedGet('/api/player/favorite-card')
  }

  const handleProfileImageChange = async (file) => {
    if (!canEditProfileImage || profileImagePending) return
    if (file.size > 3 * 1024 * 1024) {
      setProfileImageMessage('3MB 이하 이미지를 선택해 주세요.')
      return
    }

    try {
      setProfileImagePending(true)
      setProfileImageMessage('')
      const formData = new FormData()
      formData.append('image', file)
      const { data } = await api.post('/api/auth/profile-image', formData)
      applyProfileImageUpdate(data)
      setProfileImageMessage('프로필 이미지가 변경되었습니다.')
    } catch (requestError) {
      setProfileImageMessage(getApiErrorMessage(requestError, '프로필 이미지를 변경하지 못했습니다.'))
    } finally {
      setProfileImagePending(false)
    }
  }

  const handleProfileImageReset = async () => {
    if (!canEditProfileImage || profileImagePending) return
    try {
      setProfileImagePending(true)
      setProfileImageMessage('')
      const { data } = await api.delete('/api/auth/profile-image')
      applyProfileImageUpdate(data)
      setProfileImageMessage('기본 이미지로 변경되었습니다.')
    } catch (requestError) {
      setProfileImageMessage(getApiErrorMessage(requestError, '기본 이미지로 변경하지 못했습니다.'))
    } finally {
      setProfileImagePending(false)
    }
  }

  const handleFavoriteToggle = async () => {
    if (!readAuthSession()) {
      navigate('/login')
      return
    }

    try {
      setFavoritePending(true)
      setFavoriteMessage('')
      if (favoriteId) {
        await api.delete(`/api/favorite/${favoriteId}`)
        invalidateCachedGet('/api/favorite')
        removeFavoriteSnapshot(readAuthSession(), userName, playerData?.ouid)
        setFavoriteId(null)
        setFavoriteMessage('즐겨찾기에서 삭제했습니다.')
      } else {
        const response = await api.post('/api/favorite', null, { params: { userName } })
        invalidateCachedGet('/api/favorite')
        await cacheFavoriteSnapshot(readAuthSession(), response.data, playerData, userName)
        setFavoriteId(response.data?.id ?? null)
        setFavoriteMessage('즐겨찾기에 추가했습니다.')
      }
    } catch (requestError) {
      setFavoriteMessage(getApiErrorMessage(requestError, '즐겨찾기를 변경하지 못했습니다.'))
    } finally {
      setFavoritePending(false)
    }
  }

  return (
    <div className="player-shell">
      <Header />
      <NavBar />

      <main className="player-page">
        <div className="player-container banner-content-layout">
          <div className="record-banner">
            <img src="/sa-assets/banner-preview/no-outer-frame/sa-recordroom-banner-no-outer-frame.png" alt="SA Record Room" />
          </div>

          <div className="player-compact-search" aria-label="다른 플레이어 검색">
            <SearchBar compact suggestionsUp />
          </div>

          {loading ? (
            <PlayerPageSkeleton />
          ) : error ? (
            <p className="player-error">{error}</p>
          ) : (
            <>
              <nav className="profile-section-nav" aria-label="프로필 상세 영역 바로가기">
                <a href="#profile-overview">기본정보</a>
                <a href="#profile-ranking">랭킹</a>
                <a href="#profile-weapons">웨폰</a>
                <a href="#profile-maps">맵</a>
                <a href="#profile-matches">매치·성향</a>
                <a href="#profile-ai">AI 분석</a>
              </nav>

              <div id="profile-overview" className="profile-section-anchor">
                <PlayerProfile
                  basic={basic}
                  recent={recent}
                  name={name}
                  clanName={profileClanName}
                  profileImageUrl={playerData?.profileImageUrl}
                  canEditProfileImage={canEditProfileImage}
                  profileImagePending={profileImagePending}
                  profileImageMessage={profileImageMessage}
                  favoriteId={favoriteId}
                  favoritePending={favoritePending}
                  favoriteMessage={favoriteMessage}
                  onProfileImageChange={handleProfileImageChange}
                  onProfileImageReset={handleProfileImageReset}
                  onFavoriteToggle={handleFavoriteToggle}
                  onCompare={() => navigate(`/compare?left=${encodeURIComponent(userName)}`)}
                />
              </div>
              <div id="profile-ranking" className="profile-section-anchor">
                <RankSummary rank={rank} tier={tier} images={images} />
              </div>
              <div id="profile-weapons" className="profile-section-anchor">
                <WeaponSummary recent={recent} />
              </div>
              <div id="profile-maps" className="profile-section-anchor">
                <MapPerformance userName={basic.user_name || name} />
              </div>
              <div id="profile-matches" className="profile-section-anchor">
                <MatchSummary
                  userName={basic.user_name || name}
                  ouid={playerData?.ouid}
                  recent={recent}
                />
              </div>
              <div id="profile-ai" className="profile-section-anchor">
                <CombatSummary userName={basic.user_name || name} />
              </div>
            </>
          )}
        </div>
      </main>

      <Footer />
    </div>
  )
}

async function cacheFavoriteSnapshot(session, favorite, playerData, fallbackName) {
  if (!session || !favorite?.id) return

  const userName = favorite.userName || playerData?.basic?.user_name || fallbackName
  let summary = null
  try {
    const response = await api.get(`/api/favorite/${favorite.id}/match-summary`)
    summary = response.data
  } catch {
    // The card can still use the profile data that is already on screen.
  }

  const recent = summary?.summaries?.find((item) => item.key === 'RECENT')
  const accountName = session.user?.suddenNickname || session.user?.displayName || ''
  const isOwnAccount = normalizeFavoriteName(userName) === normalizeFavoriteName(accountName)
  const snapshot = {
    id: favorite.id,
    ouid: favorite.ouid || playerData?.ouid || null,
    userName,
    titleName: cleanFavoriteText(playerData?.basic?.title_name) || 'NO TITLE',
    seasonGrade: playerData?.rank?.season_grade || 'SEASON GRADE',
    seasonGradeImage: playerData?.images?.seasonGradeImage || null,
    clanName: isOwnAccount
      ? 'NO CLAN'
      : cleanFavoriteText(playerData?.basic?.clan_name) || 'NO CLAN',
    clanVerified: !isOwnAccount,
    profileImageUrl: playerData?.profileImageUrl || null,
    latestMatchDate: recent?.latestMatchDate || null,
    kda: {
      kill: recent?.averageKill,
      death: recent?.averageDeath,
      assist: recent?.averageAssist,
    },
  }

  const cacheKey = getFavoriteSnapshotCacheKey(session)
  const cached = readFavoriteSnapshots(cacheKey)
  const next = [snapshot, ...cached.items.filter((item) => snapshot.ouid
    ? item.ouid !== snapshot.ouid
    : normalizeFavoriteName(item.userName) !== normalizeFavoriteName(userName))]
  writeFavoriteSnapshots(cacheKey, next, cached.refreshedAt || Date.now())
}

function removeFavoriteSnapshot(session, userName, ouid) {
  if (!session) return
  const cacheKey = getFavoriteSnapshotCacheKey(session)
  const cached = readFavoriteSnapshots(cacheKey)
  const next = cached.items
    .filter((item) => ouid
      ? item.ouid !== ouid
      : normalizeFavoriteName(item.userName) !== normalizeFavoriteName(userName))
  writeFavoriteSnapshots(cacheKey, next, cached.refreshedAt)
}

function getFavoriteSnapshotCacheKey(session) {
  const user = session?.user
  const ownerKey = user?.id || user?.email || user?.loginId || 'guest'
  return `satrk.favorite.details.${ownerKey}`
}

function readFavoriteSnapshots(cacheKey) {
  try {
    const cached = JSON.parse(localStorage.getItem(cacheKey))
    return {
      refreshedAt: Number(cached?.refreshedAt || cached?.cachedAt) || null,
      items: Array.isArray(cached?.items) ? cached.items : [],
    }
  } catch {
    return { refreshedAt: null, items: [] }
  }
}

function writeFavoriteSnapshots(cacheKey, items, refreshedAt) {
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

function updateFavoriteProfileImageCache(session, ouid, profileImageUrl) {
  if (!session || !ouid) return
  const cacheKey = getFavoriteSnapshotCacheKey(session)
  const cached = readFavoriteSnapshots(cacheKey)
  const items = cached.items.map((item) => item.ouid === ouid
    ? { ...item, profileImageUrl }
    : item)
  writeFavoriteSnapshots(cacheKey, items, cached.refreshedAt)
}

function normalizeFavoriteName(value) {
  return String(value || '').trim().toLocaleLowerCase('ko-KR')
}

function cleanFavoriteText(value) {
  const normalized = String(value ?? '').trim()
  if (!normalized || ['-', 'null', 'undefined', 'none', 'no clan', 'no title'].includes(normalized.toLowerCase())) return ''
  return normalized
}

export default PlayerPage
