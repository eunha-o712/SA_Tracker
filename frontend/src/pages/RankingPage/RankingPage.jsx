import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/api'
import { cachedGet } from '../../api/apiCache'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import RankSummary from '../../components/PlayerProfile/RankSummary'
import { addRecentSearch } from '../../utils/recentSearches'
import ComparePanel from '../ComparePage/ComparePanel'
import '../PlayerPage/PlayerPage.css'
import './RankingPage.css'

function RankingPage() {
  const { name } = useParams()

  return <RankingPageContent key={name ?? 'empty'} name={name} />
}

function RankingPageContent({ name }) {
  const navigate = useNavigate()
  const [query, setQuery] = useState(name ?? '')
  const [rankingData, setRankingData] = useState(null)
  const [loading, setLoading] = useState(Boolean(name))
  const [error, setError] = useState('')

  useEffect(() => {
    if (!name) return undefined

    let active = true

    const fetchRanking = async () => {
      try {
        setLoading(true)
        setError('')

        const response = await cachedGet('/api/ranking', {
          params: { userName: name },
        })

        if (active) setRankingData(response.data)
      } catch (requestError) {
        if (active) {
          setRankingData(null)
          setError(getApiErrorMessage(requestError, '랭킹 정보를 불러오지 못했습니다.'))
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchRanking()

    return () => {
      active = false
    }
  }, [name])

  const handleSubmit = (event) => {
    event.preventDefault()
    const trimmed = query.trim()

    if (!trimmed) {
      setError('닉네임을 입력해 주세요.')
      return
    }

    addRecentSearch(trimmed)
    navigate(`/ranking/${encodeURIComponent(trimmed)}`)
  }

  const rank = rankingData
    ? {
        grade: rankingData.grade,
        grade_ranking: rankingData.gradeRanking,
        season_grade: rankingData.seasonGrade,
        season_grade_ranking: rankingData.seasonGradeRanking,
      }
    : null

  const tier = rankingData
    ? {
        solo_rank_match_tier: rankingData.soloRankMatchTier,
        solo_rank_match_score: rankingData.soloRankMatchScore,
        party_rank_match_tier: rankingData.partyRankMatchTier,
        party_rank_match_score: rankingData.partyRankMatchScore,
      }
    : null

  return (
    <div className="player-shell">
      <Header />
      <NavBar />

      <main className="player-page ranking-page">
        <div className="player-container banner-content-layout">
          <div className="record-banner ranking-banner">
            <img src="/sa-assets/banner-preview/no-outer-frame/sa-ranking-banner-no-outer-frame.png" alt="SA 랭킹 룸" />
          </div>

          <section className="record-section ranking-search-section">
            <div className="record-section-header">
              <h1 className="record-section-title">RANKING DATA</h1>
              <span className="record-section-sub">등급 / 티어 검색</span>
            </div>

            <form className="ranking-search-form" onSubmit={handleSubmit}>
              <label className="ranking-visually-hidden" htmlFor="ranking-player-name">
                닉네임
              </label>
              <input
                id="ranking-player-name"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="닉네임 입력"
                autoComplete="off"
              />
              <button type="submit">검색</button>
            </form>
          </section>

          {!name && !error && <RankingEmptyState />}
          {loading && <RankingLoadingState />}

          {!loading && error && (
            <section className="ranking-page-state is-error" role="alert">
              {error}
            </section>
          )}

          {!loading && rank && tier && (
            <>
              <section className="record-section ranking-player-strip">
                <div>
                  <span>경쟁전 프로필</span>
                  <h2>{rankingData.userName || name}</h2>
                </div>
                <p>등급, 시즌 랭킹과 솔로·파티 티어 정보를 함께 표시합니다.</p>
              </section>

              <RankSummary rank={rank} tier={tier} images={rankingData.images ?? {}} />
            </>
          )}

          <ComparePanel
            key={name ?? 'empty'}
            initialLeft={rankingData?.userName || name || ''}
            showEmptyState={false}
          />
        </div>
      </main>

      <Footer />
    </div>
  )
}

function RankingEmptyState() {
  return (
    <section className="ranking-page-state">
      <span>랭킹 검색</span>
      <strong>플레이어 닉네임을 검색해 랭킹 정보를 확인하세요.</strong>
      <p>검색 후 비교 영역에서 두 플레이어의 기록을 비교할 수 있습니다.</p>
    </section>
  )
}

function RankingLoadingState() {
  return (
    <section className="record-section ranking-loading" aria-busy="true">
      <span className="ranking-visually-hidden">랭킹 정보를 불러오는 중입니다.</span>
      {[0, 1, 2, 3].map((index) => (
        <div className="ranking-loading-card" key={index}>
          <div className="ranking-loading-line label" />
          <div className="ranking-loading-image" />
          <div className="ranking-loading-copy">
            <div className="ranking-loading-line value" />
            <div className="ranking-loading-line sub" />
          </div>
        </div>
      ))}
    </section>
  )
}

export default RankingPage
