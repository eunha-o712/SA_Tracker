import { useEffect, useState } from 'react'
import api, { getApiErrorMessage } from '../../api/api'
import { cachedGet } from '../../api/apiCache'
import DataRefreshStatus from './DataRefreshStatus'
import './MapPerformance.css'

const CACHE_PREFIX = 'satrk:map-performance:'

function MapPerformance({ userName }) {
  const [maps, setMaps] = useState([])
  const [sampleSize, setSampleSize] = useState(0)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const loadMapPerformance = async () => {
      if (!userName) {
        if (active) setLoading(false)
        return
      }

      try {
        const cached = readMapCache(userName)
        if (cached) {
          setMaps(cached.maps)
          setSampleSize(cached.sampleSize)
          setLoading(false)
        } else {
          setMaps([])
          setSampleSize(0)
          setLoading(true)
        }

        setError('')

        if (cached?.version) {
          const statusResponse = await api.get('/api/map/stats/status', {
            params: { userName, version: cached.version },
          })
          if (!active || statusResponse.data?.upToDate) return
        }

        setRefreshing(true)
        const response = cached
          ? await api.get('/api/map/stats', { params: { userName } })
          : await cachedGet('/api/map/stats', { params: { userName } })
        if (!active) return
        setMaps(Array.isArray(response.data?.maps) ? response.data.maps : [])
        setSampleSize(Number(response.data?.sampleSize) || 0)
        writeMapCache(userName, response.data)
      } catch (requestError) {
        if (!active) return
        setError(getApiErrorMessage(requestError, '맵 성과를 불러오지 못했습니다.'))
      } finally {
        if (active) {
          setLoading(false)
          setRefreshing(false)
        }
      }
    }

    loadMapPerformance()
    return () => { active = false }
  }, [userName])

  const visibleMaps = maps.slice(0, 6)

  return (
    <section className="record-section map-performance-section" aria-busy={loading || refreshing}>
      <div className="record-section-header">
        <h2 className="record-section-title">MAP PERFORMANCE</h2>
        <span className="record-section-sub">최근 {sampleSize || 20}경기 기준</span>
      </div>

      {refreshing && <DataRefreshStatus />}
      {loading && visibleMaps.length === 0 && <MapPerformanceLoading />}
      {!loading && error && visibleMaps.length === 0 && (
        <div className="map-performance-state error">{error}</div>
      )}
      {!loading && !error && visibleMaps.length === 0 && (
        <div className="map-performance-state">분석 가능한 맵 기록이 없습니다.</div>
      )}
      {visibleMaps.length > 0 && (
        <div className="map-performance-grid" aria-live="polite">
          {visibleMaps.map((map, index) => (
            <article className="map-performance-card" key={map.mapName}>
              <div className="map-performance-card-head">
                <span>{String(index + 1).padStart(2, '0')}</span>
                <div>
                  <strong>{map.mapName}</strong>
                  <em>{map.matchCount} MATCHES</em>
                </div>
              </div>

              <div className="map-performance-metrics">
                <MapMetric label="WIN RATE" value={`${decimal(map.winRate)}%`} accent />
                <MapMetric label="K / D" value={decimal(map.killDeathRatio)} />
                <MapMetric label="AVG KILL" value={decimal(map.averageKill)} />
              </div>

              <div className="map-performance-result">
                <span className="win">{number(map.winCount)}W</span>
                <span className="draw">{number(map.drawCount)}D</span>
                <span className="lose">{number(map.loseCount)}L</span>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  )
}

function MapMetric({ label, value, accent = false }) {
  return <div className={accent ? 'accent' : ''}><span>{label}</span><strong>{value}</strong></div>
}

function MapPerformanceLoading() {
  return <div className="map-performance-grid" aria-busy="true" aria-label="맵 성과를 분석하는 중입니다.">{[0, 1, 2].map((item) => <div className="map-performance-skeleton" key={item} />)}</div>
}

function number(value) {
  return Number(value) || 0
}

function decimal(value) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed.toFixed(1) : '0.0'
}

function readMapCache(userName) {
  try {
    const key = `${CACHE_PREFIX}${userName.toLocaleLowerCase('ko-KR')}`
    const rawCache = localStorage.getItem(key) || sessionStorage.getItem(key)
    const cached = JSON.parse(rawCache)
    if (!cached) return null
    return {
      maps: Array.isArray(cached.maps) ? cached.maps : [],
      sampleSize: Number(cached.sampleSize) || 0,
      version: typeof cached.version === 'string' ? cached.version : '',
    }
  } catch {
    return null
  }
}

function writeMapCache(userName, data) {
  try {
    const key = `${CACHE_PREFIX}${userName.toLocaleLowerCase('ko-KR')}`
    localStorage.setItem(key, JSON.stringify({
      maps: Array.isArray(data?.maps) ? data.maps : [],
      sampleSize: Number(data?.sampleSize) || 0,
      version: typeof data?.version === 'string' ? data.version : '',
      cachedAt: Date.now(),
    }))
    sessionStorage.removeItem(key)
  } catch {
    // Storage can be unavailable in private or restricted browser contexts.
  }
}

export default MapPerformance
