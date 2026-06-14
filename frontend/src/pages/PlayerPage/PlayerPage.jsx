import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import api from '../../api/api'
import './PlayerPage.css'

function PlayerPage() {
  const { name } = useParams()
  const [playerData, setPlayerData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [favoriteId, setFavoriteId] = useState(null)
  const [favLoading, setFavLoading] = useState(false)

  useEffect(() => {
    const fetchPlayerPageData = async () => {
      try {
        setLoading(true)
        setError('')

        const response = await api.get('/api/player', {
          params: { userName: name },
        })

        setPlayerData(response.data)
      } catch (err) {
        if (err.response?.status === 429) {
          setError('조회가 잠시 많아서 잠깐 후 다시 시도해주세요.')
        } else {
          setError('플레이어 정보를 불러오지 못했습니다.')
        }
      } finally {
        setLoading(false)
      }
    }

    fetchPlayerPageData()
  }, [name])

  useEffect(() => {
    const checkFavorite = async () => {
      try {
        const res = await api.get('/api/favorite')
        const found = (res.data ?? []).find((f) => f.userName === name)
        setFavoriteId(found ? found.id : null)
      } catch {
        // 무시
      }
    }
    checkFavorite()
  }, [name])

  const handleFavoriteToggle = async () => {
    if (favLoading) return
    setFavLoading(true)
    try {
      if (favoriteId !== null) {
        await api.delete(`/api/favorite/${favoriteId}`)
        setFavoriteId(null)
      } else {
        const res = await api.post('/api/favorite', null, {
          params: { userName: name },
        })
        setFavoriteId(res.data.id)
      }
    } catch {
      // 무시
    } finally {
      setFavLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="player-shell">
        <Header />
        <main className="player-page">
          <div className="player-container">
            <p className="player-message">불러오는 중...</p>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  if (error) {
    return (
      <div className="player-shell">
        <Header />
        <main className="player-page">
          <div className="player-container">
            <p className="player-error">{error}</p>
          </div>
        </main>
        <Footer />
      </div>
    )
  }

  return (
    <div className="player-shell">
      <Header />

      <main className="player-page">
        <div className="player-container">
          <div className="player-card">
            <div className="player-name-row">
              <h1 className="player-name">{playerData?.basic?.user_name}</h1>
              <button
                type="button"
                className={`favorite-btn${favoriteId !== null ? ' is-active' : ''}`}
                onClick={handleFavoriteToggle}
                disabled={favLoading}
              >
                {favoriteId !== null ? '★ 즐겨찾기 해제' : '☆ 즐겨찾기 추가'}
              </button>
            </div>
            <p className="player-clan">
              클랜: {playerData?.basic?.clan_name || '없음'}
            </p>
            <p className="player-rank">
              계급: {playerData?.rank?.grade} / 시즌: {playerData?.rank?.season_grade}
            </p>
            <p className="player-tier">
              솔랭 티어: {playerData?.tier?.solo_rank_match_tier}
            </p>
            <p className="player-manner">
              매너등급: {playerData?.basic?.manner_grade}
            </p>
          </div>

          <div className="stats-grid">
            <div className="stats-card">
              <span className="stats-label">승률</span>
              <strong className="stats-value">
                {playerData?.recent?.recent_win_rate?.toFixed(1)}%
              </strong>
            </div>

            <div className="stats-card">
              <span className="stats-label">K/D</span>
              <strong className="stats-value">
                {playerData?.recent?.recent_kill_death_rate?.toFixed(1)}
              </strong>
            </div>

            <div className="stats-card">
              <span className="stats-label">돌격 비율</span>
              <strong className="stats-value">
                {playerData?.recent?.recent_assault_rate?.toFixed(1)}%
              </strong>
            </div>

            <div className="stats-card">
              <span className="stats-label">저격 비율</span>
              <strong className="stats-value">
                {playerData?.recent?.recent_sniper_rate?.toFixed(1)}%
              </strong>
            </div>

            <div className="stats-card">
              <span className="stats-label">특수 비율</span>
              <strong className="stats-value">
                {playerData?.recent?.recent_special_rate?.toFixed(1)}%
              </strong>
            </div>
          </div>

          <div className="ranking-card">
            <h2 className="section-title">랭킹 요약</h2>

            <div className="ranking-grid">
              <div className="ranking-item">
                <span className="ranking-label">통합 계급</span>
                <strong className="ranking-value">
                  {playerData?.rank?.grade}
                </strong>
                <span className="ranking-sub">
                  전체 {playerData?.rank?.grade_ranking?.toLocaleString()}위
                </span>
              </div>

              <div className="ranking-item">
                <span className="ranking-label">시즌 계급</span>
                <strong className="ranking-value">
                  {playerData?.rank?.season_grade}
                </strong>
                <span className="ranking-sub">
                  전체 {playerData?.rank?.season_grade_ranking?.toLocaleString()}위
                </span>
              </div>

              <div className="ranking-item">
                <span className="ranking-label">솔로 랭크전</span>
                <strong className="ranking-value">
                  {playerData?.tier?.solo_rank_match_tier}
                </strong>
                <span className="ranking-sub">
                  점수 {playerData?.tier?.solo_rank_match_score}
                </span>
              </div>

              <div className="ranking-item">
                <span className="ranking-label">파티 랭크전</span>
                <strong className="ranking-value">
                  {playerData?.tier?.party_rank_match_tier}
                </strong>
                <span className="ranking-sub">
                  점수 {playerData?.tier?.party_rank_match_score}
                </span>
              </div>
            </div>
          </div>

          <div className="weapon-card">
            <h2 className="section-title">무기 성향</h2>

            <div className="weapon-list">
              <div className="weapon-item">
                <span className="weapon-name">돌격</span>
                <strong className="weapon-rate">
                  {playerData?.recent?.recent_assault_rate?.toFixed(1)}%
                </strong>
              </div>

              <div className="weapon-item">
                <span className="weapon-name">저격</span>
                <strong className="weapon-rate">
                  {playerData?.recent?.recent_sniper_rate?.toFixed(1)}%
                </strong>
              </div>

              <div className="weapon-item">
                <span className="weapon-name">특수</span>
                <strong className="weapon-rate">
                  {playerData?.recent?.recent_special_rate?.toFixed(1)}%
                </strong>
              </div>
            </div>
          </div>
        </div>
      </main>

      <Footer />
    </div>
  )
}

export default PlayerPage