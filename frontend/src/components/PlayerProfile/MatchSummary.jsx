import { useEffect, useState } from 'react'
import api from '../../api/api'
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

        const response = await api.get('/api/match/summary', {
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
          setError('매치 요약을 불러오지 못했습니다.')
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
            <span className="win">승리</span>
            <span className="draw">무승부</span>
            <span className="lose">패배</span>
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
  const ratios = trend.map((point) => Number(point.killDeathRatio) || 0)
  const maxRatio = Math.max(30, Math.ceil(Math.max(...ratios) / 5) * 5)
  const plotWidth = width - paddingX * 2
  const plotHeight = height - paddingY * 2
  const points = trend.map((point, index) => {
    const x = trend.length === 1
      ? width / 2
      : paddingX + (index / (trend.length - 1)) * plotWidth
    const ratio = Number(point.killDeathRatio) || 0
    const y = paddingY + plotHeight - (Math.min(ratio, maxRatio) / maxRatio) * plotHeight
    return { ...point, x, y, ratio }
  })
  const winPoints = points.filter((point) => point.result === 'W')
  const losePoints = points.filter((point) => point.result === 'L')
  const winLinePoints = winPoints.map((point) => `${point.x},${point.y}`).join(' ')
  const loseLinePoints = losePoints.map((point) => `${point.x},${point.y}`).join(' ')
  const gridValues = Array.from({ length: maxRatio + 1 }, (_, index) => index)
  const averageRatio = ratios.reduce((sum, ratio) => sum + ratio, 0) / ratios.length
  const hoveredPoint = hoveredIndex === null ? null : points[hoveredIndex]
  const tooltipWidth = 176
  const tooltipHeight = 74
  const tooltipX = hoveredPoint
    ? Math.min(hoveredPoint.x + 12, width - tooltipWidth - 4)
    : 0
  const tooltipY = hoveredPoint
    ? hoveredPoint.y > 104
      ? hoveredPoint.y - tooltipHeight - 12
      : hoveredPoint.y + 14
    : 0

  return (
    <div className="kd-trend-chart-wrap">
      <svg
        className="kd-trend-chart"
        viewBox={`0 0 ${width} ${height}`}
        role="img"
        aria-label="최근 경기별 K/D 추이 그래프"
      >
        {gridValues.map((value) => {
          const y = paddingY + plotHeight - (value / maxRatio) * plotHeight
          const isMajor = value % 5 === 0
          return (
            <g key={value}>
              <line
                className={`kd-grid-line ${isMajor ? 'major' : 'minor'}`}
                x1={paddingX}
                x2={width - paddingX}
                y1={y}
                y2={y}
              />
              {isMajor && (
                <text className="kd-grid-label" x="6" y={y + 4}>
                  {value}
                </text>
              )}
            </g>
          )
        })}

        {winPoints.length > 1 && (
          <polyline className="kd-result-line win" points={winLinePoints} />
        )}
        {losePoints.length > 1 && (
          <polyline className="kd-result-line lose" points={loseLinePoints} />
        )}

        {points.map((point, index) => (
          <circle
            className={`kd-trend-point ${getResultClass(point.result)}`}
            cx={point.x}
            cy={point.y}
            r="7"
            key={`${point.dateMatch}-${index}`}
            tabIndex="0"
            aria-label={`${index + 1}경기 K/D ${point.ratio.toFixed(2)}`}
            onMouseEnter={() => setHoveredIndex(index)}
            onMouseLeave={() => setHoveredIndex(null)}
            onFocus={() => setHoveredIndex(index)}
            onBlur={() => setHoveredIndex(null)}
          />
        ))}

        {hoveredPoint && (
          <g className="kd-tooltip" transform={`translate(${tooltipX} ${tooltipY})`}>
            <rect width={tooltipWidth} height={tooltipHeight} rx="4" />
            <text className="kd-tooltip-title" x="12" y="20">
              {`${hoveredIndex + 1}경기 · K/D ${hoveredPoint.ratio.toFixed(2)}`}
            </text>
            <text className="kd-tooltip-detail" x="12" y="42">
              {`${hoveredPoint.kill}K / ${hoveredPoint.death}D`}
            </text>
            <text className="kd-tooltip-average" x="12" y="62">
              {`최근 평균 K/D ${averageRatio.toFixed(2)}`}
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

function getResultClass(result) {
  if (result === 'W') return 'win'
  if (result === 'D') return 'draw'
  if (result === 'L') return 'lose'
  return 'unknown'
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
