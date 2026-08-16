import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getApiErrorMessage } from '../../api/api'
import { cachedGet } from '../../api/apiCache'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import { addRecentSearch } from '../../utils/recentSearches'
import ComparePanel from '../ComparePage/ComparePanel'
import '../PlayerPage/PlayerPage.css'
import './RankingPage.css'
import RankingRoadmap from './RankingRoadmap'

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

          {!loading && rankingData && (
            <>
              <section className="record-section ranking-player-strip">
                <div>
                  <span>경쟁전 프로필</span>
                  <h2>{rankingData.userName || name}</h2>
                </div>
                <p>등급, 시즌 랭킹과 솔로·파티 티어 정보를 함께 표시합니다.</p>
              </section>

              <RankingRoadmap data={rankingData} />
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
    <section className="record-section ranking-roadmap-section ranking-roadmap-empty">
      <div className="record-section-header">
        <h2 className="record-section-title">RANKING ROAD</h2>
        <span className="record-section-sub">현재 위치와 다음 단계</span>
      </div>

      <div className="ranking-page-state">
        <span>랭킹 검색</span>
        <strong>플레이어 닉네임을 검색해 랭킹 정보를 확인하세요.</strong>
        <p>검색하면 통합 계급, 시즌 계급과 랭크전 티어의 현재 위치가 표시됩니다.</p>
      </div>
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
