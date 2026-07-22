import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import { cachedGet, invalidateCachedGet } from '../../api/apiCache'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import { addRecentSearch } from '../../utils/recentSearches'
import '../PlayerPage/PlayerPage.css'
import './WeaponsPage.css'

function WeaponsPage() {
  const { name } = useParams()

  return <WeaponsPageContent key={name ?? 'empty'} name={name} />
}

function WeaponsPageContent({ name }) {
  const navigate = useNavigate()
  const [query, setQuery] = useState(name ?? '')
  const [weaponData, setWeaponData] = useState(null)
  const [loading, setLoading] = useState(Boolean(name))
  const [error, setError] = useState('')

  useEffect(() => {
    if (!name) return undefined

    let active = true

    const fetchWeaponStats = async () => {
      try {
        setLoading(true)
        setError('')

        let response = await cachedGet('/api/weapon', {
          params: { userName: name },
        })

        if (response.data?.sampleMatchCount == null) {
          invalidateCachedGet('/api/weapon')
          response = await api.get('/api/weapon', {
            params: { userName: name },
          })
        }

        if (active) setWeaponData(response.data)
      } catch (err) {
        if (active) {
          setWeaponData(null)
          setError(getApiErrorMessage(err, '무기 통계를 불러오지 못했습니다.'))
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchWeaponStats()

    return () => {
      active = false
    }
  }, [name])

  const handleSubmit = (event) => {
    event.preventDefault()
    const trimmed = query.trim()

    if (!trimmed) {
      setError('닉네임을 입력해주세요.')
      return
    }

    addRecentSearch(trimmed)
    navigate(`/weapons/${encodeURIComponent(trimmed)}`)
  }

  return (
    <div className="player-shell">
      <Header />
      <NavBar />

      <main className="player-page">
        <div className="player-container banner-content-layout">
          <div className="record-banner weapons-banner">
            <img src="/sa-assets/banner-preview/no-outer-frame/sa-weapons-banner-no-outer-frame.png" alt="SA Weapon Room" />
          </div>

          <section className="record-section weapons-search-section">
            <div className="record-section-header">
              <h1 className="record-section-title">WEAPON DATA</h1>
              <span className="record-section-sub">무기 통계 검색</span>
            </div>

            <form className="weapons-search-form" onSubmit={handleSubmit}>
              <label className="weapons-visually-hidden" htmlFor="weapons-player-name">
                닉네임
              </label>
              <input
                id="weapons-player-name"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="닉네임을 입력하세요"
                autoComplete="off"
              />
              <button type="submit">검색</button>
            </form>
          </section>

          {!name && !error && <WeaponsEmptyState />}
          {loading && <WeaponsLoadingState />}

          {!loading && error && (
            <section className="weapon-page-state is-error" role="alert">
              {error}
            </section>
          )}

          {!loading && weaponData && (
            <>
              <section className="record-section weapon-player-strip">
                <div>
                  <span>PLAYER LOADOUT</span>
                  <h2>{weaponData.userName || name}</h2>
                </div>
                <p>무기 계열 간 격차와 최근 매치의 헤드샷 성향을 분석합니다.</p>
              </section>

              <WeaponClassAnalysis data={weaponData} />
              <AccuracyProfile data={weaponData} />
            </>
          )}
        </div>
      </main>

      <Footer />
    </div>
  )
}

function WeaponClassAnalysis({ data }) {
  const classes = [
    { key: 'assault', label: '돌격', value: numberValue(data.assaultRate) },
    { key: 'sniper', label: '저격', value: numberValue(data.sniperRate) },
    { key: 'special', label: '특수', value: numberValue(data.specialRate) },
  ].sort((left, right) => right.value - left.value)
  const maximum = Math.max(...classes.map((item) => item.value), 1)
  const primaryGap = numberValue(data.primaryGap, classes[0].value - classes[1].value)
  const specializationIndex = numberValue(
    data.specializationIndex,
    classes[0].value ? ((classes[0].value - classes[2].value) / classes[0].value) * 100 : 0
  )
  const primaryClass = data.primaryClass || classes[0].label
  const combatType = data.combatType || (primaryGap < 3 ? '균형형' : `${primaryClass} ${primaryGap >= 8 ? '특화형' : '우세형'}`)

  return (
    <section className="record-section weapon-analysis-section">
      <div className="record-section-header">
        <h2 className="record-section-title">WEAPON CLASS GAP</h2>
        <span className="record-section-sub">계열별 강점 비교</span>
      </div>

      <div className="weapon-analysis-overview">
        <AnalysisMetric label="주력 계열" value={primaryClass} sub={combatType} accent />
        <AnalysisMetric label="1·2위 격차" value={`${formatOne(primaryGap)}%p`} sub={`${classes[1].label} 계열 대비`} />
        <AnalysisMetric label="전문화 지수" value={`${formatOne(specializationIndex)}%`} sub="최고·최저 계열 격차" />
      </div>

      <div className="weapon-class-ranking">
        {classes.map((item, index) => (
          <article className={`weapon-class-row${index === 0 ? ' is-primary' : ''}`} key={item.key}>
            <strong>{String(index + 1).padStart(2, '0')}</strong>
            <div className="weapon-class-copy">
              <span>{item.label}</span>
              <div className="weapon-class-track">
                <i style={{ width: `${Math.max((item.value / maximum) * 100, 2)}%` }} />
              </div>
            </div>
            <b>{formatOne(item.value)}%</b>
          </article>
        ))}
      </div>
    </section>
  )
}

function AccuracyProfile({ data }) {
  const sampleCount = Number(data.sampleMatchCount) || 0
  const totalKills = Number(data.totalKills) || 0
  const totalHeadshots = Number(data.totalHeadshots) || 0
  const headshotRate = numberValue(data.headshotRate)
  const averageHeadshots = numberValue(data.averageHeadshots)
  const accuracyType = resolveAccuracyType(data.accuracyType, sampleCount, totalKills, headshotRate)

  return (
    <section className="record-section weapon-accuracy-section">
      <div className="record-section-header">
        <h2 className="record-section-title">ACCURACY PROFILE</h2>
        <span className="record-section-sub">최근 {sampleCount || 20}경기 명중 성향</span>
      </div>

      <div className="weapon-accuracy-grid">
        <div className="weapon-accuracy-hero">
          <span>HEADSHOT RATE</span>
          <strong>{sampleCount ? `${formatOne(headshotRate)}%` : '-'}</strong>
          <div className="weapon-accuracy-track"><i style={{ width: `${Math.min(headshotRate, 100)}%` }} /></div>
          <p>{sampleCount ? `총 ${totalKills}킬 중 ${totalHeadshots}헤드샷` : '최근 매치 상세 데이터가 없습니다.'}</p>
        </div>

        <div className="weapon-accuracy-metrics">
          <AnalysisMetric label="명중 성향" value={accuracyType} sub="헤드샷 비율 기반" accent />
          <AnalysisMetric label="경기당 헤드샷" value={sampleCount ? formatOne(averageHeadshots) : '-'} sub={`${sampleCount}경기 표본`} />
          <AnalysisMetric label="헤드샷 합계" value={sampleCount ? totalHeadshots.toLocaleString() : '-'} sub={`총 ${totalKills.toLocaleString()}킬`} />
        </div>
      </div>
    </section>
  )
}

function AnalysisMetric({ label, value, sub, accent = false }) {
  return (
    <article className={`weapon-analysis-metric${accent ? ' is-accent' : ''}`}>
      <span>{label}</span>
      <strong>{value}</strong>
      <small>{sub}</small>
    </article>
  )
}

function numberValue(value, fallback = 0) {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

function formatOne(value) {
  return numberValue(value).toFixed(1)
}

function resolveAccuracyType(serverType, sampleCount, totalKills, headshotRate) {
  if (serverType && serverType !== '분석 대기') return serverType
  if (!sampleCount || !totalKills) return '분석 대기'
  if (headshotRate >= 35) return '정밀형'
  if (headshotRate >= 20) return '안정형'
  return '교전형'
}

function WeaponsEmptyState() {
  return (
    <section className="weapon-page-state">
      <span>LOADOUT SEARCH</span>
      <strong>닉네임으로 무기 통계를 조회하세요.</strong>
      <p>돌격·저격·특수 무기의 최근 K/D 지표를 비교할 수 있습니다.</p>
    </section>
  )
}

function WeaponsLoadingState() {
  return (
    <section className="record-section weapons-loading" aria-busy="true">
      <span className="weapons-visually-hidden">무기 통계를 불러오는 중입니다.</span>
      {[0, 1, 2].map((index) => (
        <div className="weapons-loading-card" key={index}>
          <div className="weapons-loading-line short" />
          <div className="weapons-loading-line value" />
          <div className="weapons-loading-image" />
          <div className="weapons-loading-line progress" />
        </div>
      ))}
    </section>
  )
}

export default WeaponsPage
