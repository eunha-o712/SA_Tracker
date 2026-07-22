import { useEffect, useState } from 'react'
import api, { getApiErrorMessage } from '../../api/api'
import { cachedGet } from '../../api/apiCache'
import DataRefreshStatus from './DataRefreshStatus'
import './CombatSummary.css'

const CACHE_PREFIX = 'satrk:ai-combat-report:'

function CombatSummary({ userName }) {
  const [report, setReport] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const loadReport = async () => {
      const cached = readReportCache(userName)
      setReport(cached)
      setLoading(!cached)
      setRefreshing(!cached)

      try {
        setError('')

        if (cached) {
          const statusResponse = await api.get('/api/ai/record-room/status', {
            params: { userName },
          })
          if (!active || statusResponse.data?.upToDate) return
          setRefreshing(true)
        }

        const response = cached
          ? await api.get('/api/ai/record-room', { params: { userName } })
          : await cachedGet('/api/ai/record-room', { params: { userName } })
        if (active) {
          setReport(response.data)
          writeReportCache(userName, response.data)
        }
      } catch (requestError) {
        if (active) {
          setError(getApiErrorMessage(requestError, 'AI 분석 리포트를 불러오지 못했습니다.'))
        }
      } finally {
        if (active) {
          setLoading(false)
          setRefreshing(false)
        }
      }
    }

    loadReport()
    return () => { active = false }
  }, [userName])

  return (
    <section className="record-section">
      <div className="record-section-header">
        <h2 className="record-section-title">AI COMBAT REPORT</h2>
        <span className="record-section-sub">OpenAI 전적 분석</span>
      </div>

      {refreshing && <DataRefreshStatus />}
      <div className={loading ? 'combat-summary-box loading' : 'combat-summary-box'} aria-live="polite" aria-busy={loading || refreshing}>
        {loading && !report && <CombatSummaryLoading />}
        {!loading && error && !report && <p className="combat-summary-error">{error}</p>}
        {report && <CombatSummaryReport report={report} />}
      </div>
    </section>
  )
}

function CombatSummaryReport({ report }) {
  return (
    <div className="combat-ai-report">
      <div className="combat-ai-head">
        <div>
          <span>PLAY STYLE</span>
          <strong>{report.playStyle || 'AI 분석'}</strong>
        </div>
        <em>{report.sampleSize ? `최근 ${report.sampleSize}경기 기준` : '최근 전적 기준'}</em>
      </div>

      <p className="combat-ai-summary">{report.summary}</p>

      <div className="combat-ai-grid">
        <CombatList title="STRENGTH" items={report.strengths} />
        <CombatList title="WATCH OUT" items={report.risks} tone="danger" />
        <CombatList title="NEXT MOVE" items={report.recommendations} />
      </div>

      {report.model && <span className="combat-ai-meta">AI MODEL · {report.model}</span>}
    </div>
  )
}

function CombatList({ title, items, tone = 'default' }) {
  const safeItems = Array.isArray(items) ? items.filter(Boolean) : []
  if (safeItems.length === 0) return null

  return (
    <article className={tone === 'danger' ? 'combat-ai-list danger' : 'combat-ai-list'}>
      <span>{title}</span>
      <ul>
        {safeItems.map((item) => <li key={item}>{item}</li>)}
      </ul>
    </article>
  )
}

function CombatSummaryLoading() {
  return (
    <>
      <span className="combat-summary-line wide" />
      <span className="combat-summary-line" />
      <span className="combat-summary-line short" />
    </>
  )
}

function readReportCache(userName) {
  if (!userName) return null
  try {
    const key = `${CACHE_PREFIX}${userName.toLocaleLowerCase('ko-KR')}`
    const cached = JSON.parse(localStorage.getItem(key))
    return cached?.report || null
  } catch {
    return null
  }
}

function writeReportCache(userName, report) {
  if (!userName || !report) return
  try {
    const key = `${CACHE_PREFIX}${userName.toLocaleLowerCase('ko-KR')}`
    localStorage.setItem(key, JSON.stringify({ report, cachedAt: Date.now() }))
  } catch {
    // Storage can be unavailable in private or restricted browser contexts.
  }
}

export default CombatSummary
