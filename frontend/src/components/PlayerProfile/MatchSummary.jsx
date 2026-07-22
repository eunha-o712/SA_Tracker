import { useEffect, useState } from 'react'
import { getApiErrorMessage } from '../../api/api'
import { cachedGet } from '../../api/apiCache'
import MatchForm from './MatchForm'
import './MatchSummary.css'

const SUMMARY_CARDS = [
  { key: 'RECENT', label: '최근 매치' },
  { key: 'CLAN', label: '클랜전' },
  { key: 'RANKED', label: '랭크전' },
  { key: 'GENERAL', label: '일반전' },
]

function MatchSummary({ userName, recent = {} }) {
  const [summaries, setSummaries] = useState([])
  const [analysis, setAnalysis] = useState({
    primaryMode: '-',
    primaryType: '-',
    playStyle: '분석 대기',
    killDeathTrend: [],
  })
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    let active = true

    const fetchMatchSummary = async () => {
      if (!userName) {
        setLoading(false)
        return
      }

      try {
        setLoading(true)
        setError('')

        const response = await cachedGet('/api/match/summary', {
          params: { userName },
        })

        if (active) {
          setSummaries(
            Array.isArray(response.data?.summaries)
              ? response.data.summaries
              : []
          )
          setAnalysis({
            primaryMode: response.data?.primaryMode || '-',
            primaryType: response.data?.primaryType || '-',
            playStyle: response.data?.playStyle || '분석 대기',
            killDeathTrend: Array.isArray(response.data?.killDeathTrend)
              ? response.data.killDeathTrend
              : [],
          })
        }
      } catch (requestError) {
        if (active) {
          setSummaries([])
          setAnalysis({
            primaryMode: '-',
            primaryType: '-',
            playStyle: '분석 대기',
            killDeathTrend: [],
          })
          setError(getApiErrorMessage(requestError, '매치 요약을 불러오지 못했습니다.'))
        }
      } finally {
        if (active) setLoading(false)
      }
    }

    fetchMatchSummary()

    return () => {
      active = false
    }
  }, [userName])

  const summaryByKey = new Map(
    summaries.map((summary) => [summary.key, summary])
  )

  return (
    <>
      <section className="record-section">
      <div className="record-section-header">
        <h2 className="record-section-title">MATCH K/D/A</h2>
        <span className="record-section-sub">전투동향</span>
      </div>

      <div className="match-summary-grid" aria-live="polite">
        {SUMMARY_CARDS.map((card) => {
          const summary = summaryByKey.get(card.key)
          const matchCount = Number(summary?.matchCount) || 0
          const showValues = !loading && !error && matchCount > 0

          return (
            <article className="match-summary-card" key={card.key}>
              <div className="match-summary-title">{card.label}</div>
              <div className="match-summary-sub">
                {getSummaryMessage(loading, error, matchCount)}
              </div>

              <div className="match-summary-values">
                <strong>{showValues ? formatAverage(summary.averageKill) : '-'}</strong>
                <em>/</em>
                <strong className="death">
                  {showValues ? formatAverage(summary.averageDeath) : '-'}
                </strong>
                <em>/</em>
                <strong className="assist">
                  {showValues ? formatAverage(summary.averageAssist) : '-'}
                </strong>
              </div>
            </article>
          )
        })}
      </div>
      </section>

      <MatchForm summaries={summaries} loading={loading} error={error} />

      <PlayStyleAnalysis
        analysis={analysis}
        recent={recent}
        loading={loading}
        error={error}
      />
    </>
  )
}

function PlayStyleAnalysis({ analysis, recent, loading, error }) {
  const trend = Array.isArray(analysis.killDeathTrend)
    ? analysis.killDeathTrend
    : []
  const weaponStyle = getWeaponStyle(recent)
  const showValues = !loading && !error && trend.length > 0

  const styleCards = [
    { label: '주 모드', value: analysis.primaryMode },
    { label: '주 유형', value: analysis.primaryType },
    { label: '플레이 성향', value: analysis.playStyle },
    { label: '무기 성향', value: weaponStyle },
  ]

  return (
    <section className="record-section play-style-section">
      <div className="record-section-header">
        <h2 className="record-section-title">PLAY STYLE</h2>
        <span className="record-section-sub">전투 성향 분석</span>
      </div>

      <div className="play-style-grid" aria-live="polite">
        {styleCards.map((card) => (
          <article className="play-style-card" key={card.label}>
            <span>{card.label}</span>
            <strong>{showValues ? card.value : '-'}</strong>
          </article>
        ))}
      </div>

      <div className="kd-trend-panel">
        <div className="kd-trend-head">
          <div>
            <span>K/D TREND</span>
            <strong>최근 {trend.length || 20}경기 킬데스 흐름</strong>
          </div>
          <div className="kd-trend-legend">
            <span className="kill">킬</span>
            <span className="death">데스</span>
          </div>
        </div>

        {showValues ? (
          <KillDeathTrendChart trend={trend} />
        ) : (
          <div className="kd-trend-empty">
            {loading ? '매치 기록 집계 중' : error || '표시할 경기 기록이 없습니다.'}
          </div>
        )}
      </div>
    </section>
  )
}

function KillDeathTrendChart({ trend }) {
  const [hoveredIndex, setHoveredIndex] = useState(null)
  const width = 1000
  const height = 270
  const paddingX = 48
  const paddingY = 34
  const values = trend.flatMap((point) => [
    Number(point.kill) || 0,
    Number(point.death) || 0,
  ])
  const observedMax = Math.max(...values)
  const chartMax = Math.min(30, Math.max(5, Math.ceil(observedMax / 5) * 5))
  const plotWidth = width - paddingX * 2
  const plotHeight = height - paddingY * 2
  const toY = (value) => (
    paddingY + plotHeight - (Math.min(value, chartMax) / chartMax) * plotHeight
  )
  const points = trend.map((point, index) => {
    const x = trend.length === 1
      ? width / 2
      : paddingX + (index / (trend.length - 1)) * plotWidth
    const kill = Number(point.kill) || 0
    const death = Number(point.death) || 0
    const ratio = Number(point.killDeathRatio) || 0
    return { ...point, x, kill, death, ratio, killY: toY(kill), deathY: toY(death) }
  })
  const killLinePoints = points
    .map((point) => `${point.x},${point.killY}`)
    .join(' ')
  const deathLinePoints = points
    .map((point) => `${point.x},${point.deathY}`)
    .join(' ')
  const areaPoints = points.length > 0
    ? `${points[0].x},${paddingY + plotHeight} ${killLinePoints} ${points.at(-1).x},${paddingY + plotHeight}`
    : ''
  const gridValues = Array.from({ length: chartMax + 1 }, (_, index) => index)
  const hoveredPoint = hoveredIndex === null ? null : points[hoveredIndex]
  const tooltipWidth = 176
  const tooltipHeight = 78
  const tooltipX = hoveredPoint
    ? Math.min(hoveredPoint.x + 12, width - tooltipWidth - 4)
    : 0
  const tooltipY = hoveredPoint
    ? Math.min(hoveredPoint.killY, hoveredPoint.deathY) > 110
      ? Math.min(hoveredPoint.killY, hoveredPoint.deathY) - tooltipHeight - 12
      : Math.max(hoveredPoint.killY, hoveredPoint.deathY) + 14
    : 0

  return (
    <div className="kd-trend-chart-wrap">
      <svg
        className="kd-trend-chart"
        viewBox={`0 0 ${width} ${height}`}
        role="img"
        aria-label="최근 경기별 킬과 데스 추이 그래프"
      >
        <defs>
          <linearGradient id="kd-kill-gradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#9aff68" stopOpacity="0.42" />
            <stop offset="58%" stopColor="#78d957" stopOpacity="0.14" />
            <stop offset="100%" stopColor="#78d957" stopOpacity="0" />
          </linearGradient>
        </defs>

        {gridValues.map((value) => {
          const y = paddingY + plotHeight - (value / chartMax) * plotHeight
          return (
            <g key={value}>
              <line
                className={`kd-grid-line ${value % 5 === 0 ? 'major' : 'minor'}`}
                x1={paddingX}
                x2={width - paddingX}
                y1={y}
                y2={y}
              />
              {value % 5 === 0 && (
                <text className="kd-grid-label" x="6" y={y + 4}>
                  {value}
                </text>
              )}
            </g>
          )
        })}

        {points.length > 1 && <polygon className="kd-kill-area" points={areaPoints} />}
        {points.length > 1 && <polyline className="kd-result-line kill" points={killLinePoints} />}
        {points.length > 1 && <polyline className="kd-result-line death" points={deathLinePoints} />}

        {points.map((point, index) => (
          <g
            className={`kd-trend-point-group${hoveredIndex === index ? ' active' : ''}`}
            key={`${point.dateMatch}-${index}`}
            tabIndex="0"
            role="img"
            aria-label={`${index + 1}경기 ${point.kill}킬 ${point.death}데스, K/D ${point.ratio.toFixed(2)}`}
            onMouseEnter={() => setHoveredIndex(index)}
            onMouseLeave={() => setHoveredIndex(null)}
            onPointerEnter={() => setHoveredIndex(index)}
            onPointerLeave={() => setHoveredIndex(null)}
            onFocus={() => setHoveredIndex(index)}
            onBlur={() => setHoveredIndex(null)}
          >
            {hoveredIndex === index && (
              <line
                className="kd-hover-guide"
                x1={point.x}
                x2={point.x}
                y1={paddingY}
                y2={paddingY + plotHeight}
              />
            )}
            <circle
              className="kd-point-hit-area death"
              cx={point.x}
              cy={point.deathY}
              r="12"
              onMouseEnter={() => setHoveredIndex(index)}
              onPointerEnter={() => setHoveredIndex(index)}
              onClick={() => setHoveredIndex(index)}
            />
            <circle
              className="kd-point-hit-area kill"
              cx={point.x}
              cy={point.killY}
              r="12"
              onMouseEnter={() => setHoveredIndex(index)}
              onPointerEnter={() => setHoveredIndex(index)}
              onClick={() => setHoveredIndex(index)}
            />
            <circle className="kd-trend-point-ring death" cx={point.x} cy={point.deathY} r="5.5" />
            <circle className="kd-trend-point death" cx={point.x} cy={point.deathY} r="3" />
            <circle className="kd-trend-point-ring kill" cx={point.x} cy={point.killY} r="6" />
            <circle className="kd-trend-point kill" cx={point.x} cy={point.killY} r="3.3" />
          </g>
        ))}

        {hoveredPoint && (
          <g className="kd-tooltip" transform={`translate(${tooltipX} ${tooltipY})`}>
            <rect width={tooltipWidth} height={tooltipHeight} rx="4" />
            <text className="kd-tooltip-title" x="12" y="20">
              {`${hoveredIndex + 1}경기 · ${getResultLabel(hoveredPoint.result)}`}
            </text>
            <text className="kd-tooltip-detail" x="12" y="42">
              {`${hoveredPoint.kill}K / ${hoveredPoint.death}D`}
            </text>
            <text className="kd-tooltip-average" x="12" y="62">
              {`K/D ${hoveredPoint.ratio.toFixed(2)}`}
            </text>
          </g>
        )}
      </svg>

      <div className="kd-trend-axis">
        <span>이전</span>
        <strong>최근 경기로 갈수록 →</strong>
        <span>최신</span>
      </div>
    </div>
  )
}

function getWeaponStyle(recent) {
  const weapons = [
    { label: '돌격형', value: Number(recent?.recent_assault_rate) || 0 },
    { label: '저격형', value: Number(recent?.recent_sniper_rate) || 0 },
    { label: '특수무기형', value: Number(recent?.recent_special_rate) || 0 },
  ]
  const values = weapons.map((weapon) => weapon.value)
  const maxValue = Math.max(...values)
  const minValue = Math.min(...values)

  if (maxValue <= 0) return '분석 대기'
  if (maxValue - minValue < 5) return '올라운더'
  return weapons.find((weapon) => weapon.value === maxValue)?.label || '올라운더'
}

function getResultLabel(result) {
  if (result === 'W') return '승리'
  if (result === 'D') return '무승부'
  if (result === 'L') return '패배'
  return '결과 없음'
}

function getSummaryMessage(loading, error, matchCount) {
  if (loading) return '매치 기록 집계 중'
  if (error) return error
  if (matchCount === 0) return '기록 없음'
  return `최근 ${matchCount}경기 평균`
}

function formatAverage(value) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue.toFixed(1) : '-'
}

export default MatchSummary
