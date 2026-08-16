import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import { formatRankDisplayName } from '../../utils/rankDisplayName'
import { addRecentSearch } from '../../utils/recentSearches'
import './ComparePage.css'

function ComparePanel({
  initialLeft = '',
  initialRight = '',
  syncUrl = false,
  showEmptyState = true,
}) {
  const navigate = useNavigate()
  const [leftName, setLeftName] = useState(initialLeft)
  const [rightName, setRightName] = useState(initialRight)
  const [submittedPair, setSubmittedPair] = useState(() => (
    initialLeft && initialRight ? { left: initialLeft, right: initialRight } : null
  ))
  const [players, setPlayers] = useState(null)
  const [loading, setLoading] = useState(Boolean(initialLeft && initialRight))
  const [error, setError] = useState('')

  useEffect(() => {
    if (!submittedPair?.left || !submittedPair?.right) return undefined

    let active = true
    Promise.all([loadCompetitor(submittedPair.left), loadCompetitor(submittedPair.right)])
      .then(([left, right]) => {
        if (active) setPlayers({ left, right })
      })
      .catch((requestError) => {
        if (active) setError(getApiErrorMessage(requestError, '비교 정보를 불러오지 못했습니다.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [submittedPair])

  const handleSubmit = (event) => {
    event.preventDefault()
    const left = leftName.trim()
    const right = rightName.trim()

    if (!left || !right) {
      setError('비교할 두 플레이어의 닉네임을 모두 입력해 주세요.')
      return
    }

    if (left.toLowerCase() === right.toLowerCase()) {
      setError('서로 다른 두 플레이어의 닉네임을 입력해 주세요.')
      return
    }

    addRecentSearch(left)
    addRecentSearch(right)

    if (syncUrl) {
      navigate(`/compare?left=${encodeURIComponent(left)}&right=${encodeURIComponent(right)}`)
      return
    }

    setLoading(true)
    setError('')
    setPlayers(null)
    setSubmittedPair({ left, right })
  }

  const rows = useMemo(() => players ? createRows(players.left, players.right) : [], [players])
  const verdict = useMemo(() => getVerdict(rows), [rows])

  return (
    <>
      <section className="record-section compare-search-section">
        <div className="record-section-header">
          <h1 className="record-section-title">PLAYER COMPARISON</h1>
          <span className="record-section-sub">플레이어 전적 비교</span>
        </div>
        <form className="compare-search-form" onSubmit={handleSubmit}>
          <label>
            <span>플레이어 A</span>
            <input
              value={leftName}
              onChange={(event) => setLeftName(event.target.value)}
              placeholder="첫 번째 닉네임"
              autoComplete="off"
            />
          </label>
          <div className="compare-versus" aria-hidden="true">VS</div>
          <label>
            <span>플레이어 B</span>
            <input
              value={rightName}
              onChange={(event) => setRightName(event.target.value)}
              placeholder="두 번째 닉네임"
              autoComplete="off"
            />
          </label>
          <button type="submit" disabled={loading}>비교하기</button>
        </form>
      </section>

      {!submittedPair && showEmptyState && <CompareEmpty />}
      {loading && <CompareLoading />}
      {!loading && error && <section className="compare-state error" role="alert">{error}</section>}
      {!loading && players && !error && (
        <>
          <section className="record-section compare-board">
            <CompetitorCard competitor={players.left} side="left" />
            <div className="compare-score">
              <span>전적 비교</span>
              <strong>{verdict.left} : {verdict.right}</strong>
              <em>{verdict.label}</em>
            </div>
            <CompetitorCard competitor={players.right} side="right" />
          </section>
          <section className="record-section compare-metrics-section">
            <div className="record-section-header">
              <h2 className="record-section-title">BATTLE METRICS</h2>
              <span className="record-section-sub">최근 전적 기준</span>
            </div>
            <div className="compare-metrics">
              {rows.map((row) => (
                <div className="compare-metric-row" key={row.label}>
                  <strong className={row.winner === 'left' ? 'winner' : ''}>{row.leftText}</strong>
                  <span>{row.label}</span>
                  <strong className={row.winner === 'right' ? 'winner' : ''}>{row.rightText}</strong>
                </div>
              ))}
            </div>
          </section>
        </>
      )}
    </>
  )
}

async function loadCompetitor(userName) {
  const [playerResponse, summaryResponse] = await Promise.all([
    api.get('/api/player', { params: { userName } }),
    api.get('/api/match/summary', { params: { userName } }),
  ])
  const player = playerResponse.data ?? {}
  const summary = summaryResponse.data ?? {}
  const recent = summary.summaries?.find((item) => item.key === 'RECENT') ?? {}
  const clan = summary.summaries?.find((item) => item.key === 'CLAN') ?? {}
  return { userName: player.basic?.user_name || userName, player, summary, recent, clan }
}

function CompetitorCard({ competitor, side }) {
  const { player } = competitor

  return (
    <article className={`competitor-card ${side}`}>
      <div className="competitor-image">
        <img src={player.images?.seasonGradeImage || '/sa-assets/sa-profile-basic.png'} alt="" />
      </div>
      <span>{side === 'left' ? '플레이어 A' : '플레이어 B'}</span>
      <h2>{competitor.userName}</h2>
      <p>{cleanText(player.basic?.clan_name) || '클랜 없음'}</p>
      <div className="competitor-rank">
        <strong>{formatRankDisplayName(player.rank?.season_grade || player.rank?.grade) || '-'}</strong>
        <em>{player.tier?.solo_rank_match_tier || '랭크 없음'}</em>
      </div>
    </article>
  )
}

function createRows(left, right) {
  const metric = (label, leftValue, rightValue, formatter, lowerWins = false) => {
    const l = Number(leftValue)
    const r = Number(rightValue)
    let winner = ''
    if (Number.isFinite(l) && Number.isFinite(r) && l !== r) {
      winner = lowerWins ? (l < r ? 'left' : 'right') : (l > r ? 'left' : 'right')
    }
    return { label, leftText: formatter(leftValue), rightText: formatter(rightValue), winner }
  }

  const kd = (item) => {
    const death = Number(item.recent?.averageDeath)
    const kill = Number(item.recent?.averageKill)
    return death > 0 ? kill / death : kill
  }

  return [
    metric('최근 승률', left.recent.winRate, right.recent.winRate, percent),
    metric('평균 K/D', kd(left), kd(right), decimal),
    metric('평균 킬', left.recent.averageKill, right.recent.averageKill, decimal),
    metric('시즌 계급 랭킹', left.player.rank?.season_grade_ranking, right.player.rank?.season_grade_ranking, ranking, true),
    metric('솔로 랭크 점수', left.player.tier?.solo_rank_match_score, right.player.tier?.solo_rank_match_score, integer),
    metric('클랜전 승률', left.clan.winRate, right.clan.winRate, percent),
  ]
}

function getVerdict(rows) {
  const left = rows.filter((row) => row.winner === 'left').length
  const right = rows.filter((row) => row.winner === 'right').length
  return { left, right, label: left === right ? '동률' : left > right ? '플레이어 A 우세' : '플레이어 B 우세' }
}

function CompareEmpty() {
  return (
    <section className="compare-state">
      <span>전적 비교</span>
      <strong>두 플레이어의 전적을 한눈에 비교해 보세요.</strong>
      <p>계급, 티어와 최근 매치 지표를 기준으로 비교합니다.</p>
    </section>
  )
}

function CompareLoading() {
  return (
    <section className="record-section compare-loading" aria-busy="true" aria-label="비교 정보를 불러오는 중입니다.">
      <div className="compare-loading-card compare-shimmer" />
      <div className="compare-loading-score compare-shimmer" />
      <div className="compare-loading-card compare-shimmer" />
    </section>
  )
}

function cleanText(value) {
  if (value === null || value === undefined) return ''
  const normalized = String(value).trim()
  if (!normalized) return ''
  if (['-', 'null', 'undefined', 'none', 'no clan'].includes(normalized.toLowerCase())) return ''
  return normalized
}

function percent(value) {
  const number = Number(value)
  return Number.isFinite(number) ? `${number.toFixed(1)}%` : '-'
}

function decimal(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(1) : '-'
}

function integer(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number.toLocaleString() : '-'
}

function ranking(value) {
  const number = Number(value)
  return Number.isFinite(number) && number > 0 ? `${number.toLocaleString()}위` : '-'
}

export default ComparePanel
