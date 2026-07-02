import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../api/api'
import './PlayerFavorites.css'

const mockFavorites = [
  {
    id: 1,
    userName: '원장',
    titleName: 'NO TITLE',
    seasonGrade: '',
    seasonGradeImage: null,
    clanName: '다봄',
    latestMatchDate: null,
    kda: { kill: 48.32, death: 23.21, assist: 11.5 },
  },
]

function PlayerFavorites() {
  const navigate = useNavigate()
  const [favorites, setFavorites] = useState(mockFavorites)
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    let active = true

    const loadFavoriteData = async () => {
      const updatedFavorites = await Promise.all(
        mockFavorites.map(async (favorite) => {
          try {
            const [playerResult, summaryResult] = await Promise.allSettled([
              api.get('/api/player', { params: { userName: favorite.userName } }),
              api.get('/api/match/summary', { params: { userName: favorite.userName } }),
            ])
            const playerData = playerResult.status === 'fulfilled'
              ? playerResult.value.data
              : null
            const summaryData = summaryResult.status === 'fulfilled'
              ? summaryResult.value.data
              : null
            const recentSummary = summaryData?.summaries?.find(
              (summary) => summary.key === 'RECENT'
            )
            const basic = playerData?.basic ?? {}
            const rank = playerData?.rank ?? {}
            const images = playerData?.images ?? {}

            return {
              ...favorite,
              titleName: basic.title_name || 'NO TITLE',
              seasonGrade: rank.season_grade || '시즌 계급',
              seasonGradeImage: images.seasonGradeImage || null,
              clanName: basic.clan_name || 'NO CLAN',
              latestMatchDate: recentSummary?.latestMatchDate || null,
            }
          } catch {
            return favorite
          }
        })
      )
      if (active) setFavorites(updatedFavorites)
    }

    loadFavoriteData()
    const timerId = window.setInterval(() => setNow(Date.now()), 60_000)
    return () => {
      active = false
      window.clearInterval(timerId)
    }
  }, [])

  const handleDelete = (id) => {
    setFavorites((current) => current.filter((favorite) => favorite.id !== id))
  }

  return (
    <section className="player-favorites">
      <div className="player-favorites-header">
        <span>FAVORITE</span>
        <h2>PLAYERS</h2>
      </div>

      {favorites.length === 0 && (
        <div className="player-favorites-message">등록된 즐겨찾기가 없습니다.</div>
      )}

      {favorites.length > 0 && (
        <div className="player-card-grid">
          {favorites.map((favorite) => {
            const matchStatus = getMatchStatus(favorite.latestMatchDate, now)
            return (
              <article className="favorite-player-card" key={favorite.id}>
                <div className="favorite-top">
                  <div className="favorite-rank">
                    {favorite.seasonGradeImage ? (
                      <img src={favorite.seasonGradeImage} alt={favorite.seasonGrade} />
                    ) : (
                      <span className="favorite-image-placeholder" aria-hidden="true">?</span>
                    )}
                  </div>

                  <div className="favorite-name">
                    <h3>{favorite.userName}</h3>
                    <p>{favorite.titleName}</p>
                  </div>

                  <div className="favorite-login">
                    <span>
                      <i className={matchStatus.recent ? 'active' : ''} />
                      LAST MATCH
                    </span>
                    <strong className={matchStatus.recent ? 'active' : ''}>
                      {matchStatus.label}
                    </strong>
                  </div>
                </div>

                <div className="favorite-divider" />

                <div className="favorite-clan">
                  <div className="favorite-clan-mark">
                    <img src="/sa-assets/sa-clan-basic.png" alt="클랜 기본 이미지" />
                  </div>
                  <strong>{favorite.clanName}</strong>
                </div>

                <div className="favorite-divider" />

                <div className="favorite-kda">
                  <span>K/D/A</span>
                  <div>
                    <b>{favorite.kda.kill.toFixed(2)}</b><em>/</em>
                    <b className="death">{favorite.kda.death.toFixed(2)}</b><em>/</em>
                    <b className="assist">{favorite.kda.assist.toFixed(2)}</b>
                  </div>
                </div>

                <div className="favorite-actions">
                  <button
                    type="button"
                    onClick={() => navigate(`/player/${encodeURIComponent(favorite.userName)}`)}
                  >
                    VIEW PROFILE
                  </button>
                  <button type="button" className="remove" onClick={() => handleDelete(favorite.id)}>
                    REMOVE
                  </button>
                </div>
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}

function getMatchStatus(latestMatchDate, now) {
  if (!latestMatchDate) return { label: '-', recent: false }
  const matchTime = new Date(latestMatchDate).getTime()
  if (!Number.isFinite(matchTime)) return { label: '-', recent: false }

  const elapsedMs = Math.max(0, now - matchTime)
  const elapsedMinutes = Math.floor(elapsedMs / 60_000)
  const elapsedHours = Math.floor(elapsedMs / 3_600_000)
  const elapsedDays = Math.floor(elapsedMs / 86_400_000)

  if (elapsedMinutes < 1) return { label: 'JUST NOW', recent: true }
  if (elapsedMinutes < 60) return { label: `${elapsedMinutes}M AGO`, recent: true }
  if (elapsedHours < 24) return { label: `${elapsedHours}H AGO`, recent: true }
  return { label: `${elapsedDays}D AGO`, recent: false }
}

export default PlayerFavorites
