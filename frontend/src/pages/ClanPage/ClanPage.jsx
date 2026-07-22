import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import MatchCard from '../../components/MatchCard/MatchCard'
import NavBar from '../../components/NavBar/NavBar'
import TeamTable from '../../components/TeamTable/TeamTable'
import { readAuthSession, subscribeToAuthSession } from '../../utils/authSession'
import { addRecentSearch } from '../../utils/recentSearches'
import '../PlayerPage/PlayerPage.css'
import '../MatchDetailPage/MatchDetailPage.css'
import './ClanPage.css'

const CLAN_TYPES = ['클랜전', '퀵매치 클랜전']
const TEAM_SIZE_OPTIONS = [2, 3, 4, 5]

function ClanPage() {
  const { name } = useParams()
  const [session, setSession] = useState(readAuthSession)

  useEffect(() => subscribeToAuthSession(() => setSession(readAuthSession())), [])

  const accountName = session?.user?.suddenNickname || session?.user?.displayName || ''
  const accountId = session?.user?.id || 'guest'
  const clanNone = Boolean(session?.user?.clanNone)
  const isOwnAccount = Boolean(
    session?.user?.ouid
    && (!name || name.trim().toLocaleLowerCase('ko-KR') === accountName.trim().toLocaleLowerCase('ko-KR')),
  )

  const pageKey = `${accountId}:${name ?? (isOwnAccount ? 'self' : 'empty')}`
  return <ClanPageContent key={pageKey} name={name} accountName={accountName} isOwnAccount={isOwnAccount} clanNone={clanNone} />
}

function ClanPageContent({ name, accountName, isOwnAccount, clanNone }) {
  const navigate = useNavigate()
  const [query, setQuery] = useState(name ?? (isOwnAccount ? accountName : ''))
  const [player, setPlayer] = useState(null)
  const [summary, setSummary] = useState(null)
  const [matches, setMatches] = useState([])
  const [loading, setLoading] = useState(Boolean(name || isOwnAccount))
  const [error, setError] = useState('')
  const [selectedId, setSelectedId] = useState('')
  const [detail, setDetail] = useState(null)
  const [detailCache, setDetailCache] = useState({})
  const [detailLoading, setDetailLoading] = useState(false)
  const [detailError, setDetailError] = useState('')
  const [roster, setRoster] = useState([])
  const [rosterLoading, setRosterLoading] = useState(true)
  const [rosterPending, setRosterPending] = useState('')
  const [rosterMessage, setRosterMessage] = useState('')
  const [rosterError, setRosterError] = useState('')
  const [dashboard, setDashboard] = useState(null)
  const [dashboardLoading, setDashboardLoading] = useState(true)
  const [dashboardError, setDashboardError] = useState('')

  useEffect(() => {
    let active = true
    api.get('/api/clan/members')
      .then((response) => { if (active) setRoster(Array.isArray(response.data) ? response.data : []) })
      .catch((requestError) => { if (active) setRosterError(getApiErrorMessage(requestError, '클랜 로스터를 불러오지 못했습니다.')) })
      .finally(() => { if (active) setRosterLoading(false) })
    return () => { active = false }
  }, [])

  useEffect(() => {
    if (!name && !isOwnAccount) return undefined
    let active = true

    const loadClan = async () => {
      try {
        const playerResponse = isOwnAccount
          ? await api.get('/api/player/me')
          : await api.get('/api/player', { params: { userName: name } })
        if (!active) return

        const playerData = playerResponse.data ?? null
        const resolvedName = playerData?.basic?.user_name || playerData?.userName || name || accountName
        const [summaryResponse, ...matchResponses] = await Promise.all([
          api.get('/api/match/summary', { params: { userName: resolvedName } }),
          ...CLAN_TYPES.map((matchType) => api.get('/api/match', { params: {
            userName: resolvedName, page: 1, scope: 'CLAN', matchType, matchMode: 'ALL', matchMap: 'ALL',
          } })),
        ])
        if (!active) return

        setPlayer(playerData)
        setSummary((summaryResponse.data?.summaries ?? []).find((item) => item.key === 'CLAN') ?? null)
        setMatches(mergeMatches(matchResponses.flatMap((item) => item.data?.matches ?? [])).slice(0, 10))
      } catch (requestError) {
        if (!active) return
        setError(getApiErrorMessage(requestError, '클랜 정보를 불러오지 못했습니다.'))
      } finally {
        if (active) setLoading(false)
      }
    }

    loadClan()

    return () => { active = false }
  }, [name, accountName, isOwnAccount])

  const handleSubmit = (event) => {
    event.preventDefault()
    const value = query.trim()
    if (!value) return setError('닉네임을 입력해주세요.')
    addRecentSearch(value)
    navigate(`/clan/${encodeURIComponent(value)}`)
  }

  const handleToggle = async (matchId) => {
    if (selectedId === matchId) {
      setSelectedId(''); setDetail(null); setDetailError(''); return
    }
    setSelectedId(matchId); setDetail(null); setDetailError('')
    if (detailCache[matchId]) return setDetail(detailCache[matchId])

    try {
      setDetailLoading(true)
      const response = await api.get('/api/match/detail', { params: { matchId } })
      setDetail(response.data)
      setDetailCache((cache) => ({ ...cache, [matchId]: response.data }))
    } catch (requestError) {
      setDetailError(getApiErrorMessage(requestError, '상세 기록을 불러오지 못했습니다.'))
    } finally { setDetailLoading(false) }
  }

  const handleAddMember = async () => {
    const userName = player?.basic?.user_name || name
    if (!userName) return
    try {
      setRosterPending('add')
      setRosterError('')
      setRosterMessage('')
      const response = await api.post('/api/clan/members', null, { params: { userName } })
      const member = response.data
      setDashboardLoading(true)
      setRoster((current) => [...current.filter((item) => item.id !== member.id), member].sort((a, b) => a.userName.localeCompare(b.userName, 'ko')))
      setRosterMessage(`${member.userName} 님을 로스터에 등록했습니다.`)
    } catch (requestError) {
      setRosterError(getApiErrorMessage(requestError, '클랜원을 등록하지 못했습니다.'))
    } finally { setRosterPending('') }
  }

  const handleDeleteMember = async (member) => {
    try {
      setRosterPending(String(member.id))
      setRosterError('')
      setRosterMessage('')
      await api.delete(`/api/clan/members/${member.id}`)
      setDashboardLoading(true)
      setRoster((current) => current.filter((item) => item.id !== member.id))
      setRosterMessage(`${member.userName} 님을 로스터에서 삭제했습니다.`)
    } catch (requestError) {
      setRosterError(getApiErrorMessage(requestError, '클랜원을 삭제하지 못했습니다.'))
    } finally { setRosterPending('') }
  }

  const reportedClanName = String(player?.basic?.clan_name ?? '').trim()
  const resolvedPlayerName = String(player?.basic?.user_name || player?.userName || name || '').trim()
  const rosterMembership = roster.find((member) => (
    sameText(member.userName, resolvedPlayerName)
    && sameText(member.clanName, reportedClanName)
  ))
  const hasClanEvidence = isOwnAccount
    ? !clanNone
    : Number(summary?.matchCount ?? 0) > 0 || Boolean(rosterMembership)
  const clanName = reportedClanName && hasClanEvidence ? reportedClanName : ''
  const clanDecisionPending = Boolean(isOwnAccount && player && rosterLoading)
  const teams = useMemo(() => splitTeams(detail?.matchDetail ?? []), [detail])
  const clanRoster = useMemo(() => roster.filter((member) => member.clanName === clanName), [roster, clanName])
  const registeredMember = clanRoster.find((member) => member.userName === (player?.basic?.user_name || name))
  const rosterKey = clanRoster.map((member) => member.id).join(',')

  useEffect(() => {
    if (!clanName) return undefined
    let active = true
    api.get('/api/clan/dashboard', { params: { clanName } })
      .then((response) => { if (active) { setDashboard(response.data ?? null); setDashboardError('') } })
      .catch((requestError) => { if (active) setDashboardError(getApiErrorMessage(requestError, '클랜 전력 데이터를 불러오지 못했습니다.')) })
      .finally(() => { if (active) setDashboardLoading(false) })
    return () => { active = false }
  }, [clanName, rosterKey])

  return (
    <div className="player-shell">
      <Header /><NavBar />
      <main className="player-page">
        <div className="player-container clan-container banner-content-layout">
          <div className="record-banner clan-banner"><img src="/sa-assets/banner-preview/no-outer-frame/sa-clan-banner-no-outer-frame.png" alt="Clan Operations" /></div>
          <section className="record-section clan-search-section">
            <div className="record-section-header">
              <h1 className="record-section-title">CLAN OPERATIONS</h1>
              <span className="record-section-sub">클랜 전투 분석</span>
            </div>
            <form className="clan-search-form" onSubmit={handleSubmit}>
              <label className="clan-visually-hidden" htmlFor="clan-name">닉네임</label>
              <input id="clan-name" value={query} onChange={(event) => setQuery(event.target.value)} placeholder="클랜원의 닉네임을 입력하세요" autoComplete="off" />
              <button type="submit" disabled={loading}>조회</button>
            </form>
          </section>

          {!name && !isOwnAccount && !error && <PageState label="CLAN SEARCH" title="클랜원의 닉네임으로 전투 기록을 조회하세요." copy="소속 클랜과 최근 클랜전 성과를 한 번에 확인할 수 있습니다." />}
          {(loading || clanDecisionPending) && <LoadingState />}
          {!loading && error && <section className="clan-page-state is-error" role="alert">{error}</section>}
          {!loading && !clanDecisionPending && player && !error && !clanName && <PageState label="NO CLAN DATA" title={`${player.userName || name} 님은 현재 소속 클랜이 없습니다.`} />}

          {!loading && !clanDecisionPending && player && clanName && !error && <>
            <ClanOverview
              clanName={clanName}
              userName={player.userName || name}
              summary={summary}
              registered={Boolean(registeredMember)}
              pending={rosterPending === 'add'}
              onAdd={handleAddMember}
            />
            <ClanRoster
              clanName={clanName}
              members={clanRoster}
              loading={rosterLoading}
              pending={rosterPending}
              message={rosterMessage}
              error={rosterError}
              onDelete={handleDeleteMember}
              onView={(member) => navigate(`/player/${encodeURIComponent(member.userName)}`)}
            />
            <ClanPowerRanking
              dashboard={dashboard}
              loading={dashboardLoading}
              error={dashboardError}
              onView={(member) => navigate(`/player/${encodeURIComponent(member.userName)}`)}
            />
            <ClanTeamBuilder members={clanRoster} />
            <section className="record-section clan-match-section">
              <div className="record-section-header">
                <h2 className="record-section-title">RECENT CLAN MATCHES</h2>
                <span className="record-section-sub">최근 10경기</span>
              </div>
              <div className="clan-match-list">
                {matches.length === 0 ? <div className="clan-page-state compact">최근 클랜전 기록이 없습니다.</div> : matches.map((match) => (
                  <div className="match-card-wrap" key={match.match_id}>
                    <MatchCard match={match} selected={selectedId === match.match_id} onToggle={handleToggle} />
                    {selectedId === match.match_id && <div className="match-inline-detail">
                      {detailLoading ? <div className="clan-detail-state">상세 기록을 불러오는 중입니다.</div>
                        : detailError ? <div className="clan-detail-state is-error">{detailError}</div>
                          : detail ? <MatchDetail match={match} detail={detail} teams={teams} /> : null}
                    </div>}
                  </div>
                ))}
              </div>
            </section>
          </>}
        </div>
      </main>
      <Footer />
    </div>
  )
}

function ClanPowerRanking({ dashboard, loading, error, onView }) {
  if (loading) return <section className="record-section clan-power-loading" aria-busy="true" aria-label="클랜 전력을 집계하는 중입니다."><div className="clan-power-loading-card clan-shimmer" /><div className="clan-power-loading-card clan-shimmer" /><div className="clan-power-loading-table clan-shimmer" /></section>

  const members = Array.isArray(dashboard?.members) ? dashboard.members : []
  return <section className="record-section clan-power-section">
    <div className="record-section-header"><h2 className="record-section-title">CLAN POWER RANKING</h2><span className="record-section-sub">로스터 전력 집계</span></div>
    {error ? <div className="clan-roster-message error" role="alert">{error}</div> : <>
      <div className="clan-power-summary">
        <PowerMetric label="ROSTER" value={`${dashboard?.memberCount ?? 0}명`} sub={`${dashboard?.analyzedMemberCount ?? 0}명 분석`} />
        <PowerMetric label="CLAN MATCHES" value={dashboard?.totalMatchCount ?? 0} sub="멤버 기록 합계" />
        <PowerMetric label="AVG WIN RATE" value={`${decimal(dashboard?.averageWinRate)}%`} sub="분석 멤버 평균" accent />
        <PowerMetric label="AVG K / D" value={decimal(dashboard?.averageKillDeathRatio)} sub="클랜전 평균" />
      </div>
      {members.length === 0 ? <div className="clan-roster-empty">로스터에 클랜원을 등록하면 전력 순위가 표시됩니다.</div> : <div className="clan-power-table">
        <div className="clan-power-head"><span>RANK</span><span>PLAYER</span><span>MATCH</span><span>WIN RATE</span><span>K / D</span><span>W · L · D</span><span>ACTION</span></div>
        {members.map((member, index) => <div className={member.available ? 'clan-power-row' : 'clan-power-row unavailable'} key={member.id}>
          <strong>{member.available ? String(index + 1).padStart(2, '0') : '--'}</strong>
          <div><b>{member.userName}</b><em>{member.available ? '분석 완료' : '데이터 없음'}</em></div>
          <span>{member.available ? member.matchCount : '-'}</span>
          <span className="win-rate">{member.available ? `${decimal(member.winRate)}%` : '-'}</span>
          <span>{member.available ? decimal(member.averageKillDeathRatio) : '-'}</span>
          <span>{member.available ? `${member.winCount} · ${member.loseCount} · ${member.drawCount}` : '-'}</span>
          <button type="button" onClick={() => onView(member)}>프로필</button>
        </div>)}
      </div>}
    </>}
  </section>
}

function PowerMetric({ label, value, sub, accent = false }) {
  return <article className={accent ? 'clan-power-metric accent' : 'clan-power-metric'}><span>{label}</span><strong>{value}</strong><em>{sub}</em></article>
}

function ClanTeamBuilder({ members }) {
  const [selectedIds, setSelectedIds] = useState([])
  const [teamSize, setTeamSize] = useState(3)
  const [result, setResult] = useState(null)
  const [variant, setVariant] = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const memberIds = useMemo(() => new Set(members.map((member) => member.id)), [members])
  const validSelectedIds = selectedIds.filter((id) => memberIds.has(id))
  const canBalance = validSelectedIds.length >= teamSize * 2
    && validSelectedIds.length <= 10
    && validSelectedIds.length % teamSize === 0
  const teamCount = canBalance ? validSelectedIds.length / teamSize : 0

  const toggleMember = (memberId) => {
    setResult(null)
    setError('')
    setSelectedIds((current) => current.includes(memberId)
      ? current.filter((id) => id !== memberId)
      : current.length >= 10 ? current : [...current, memberId])
  }

  const selectAvailable = () => {
    const limit = Math.min(10, members.length)
    const selectableCount = limit - (limit % teamSize)
    setSelectedIds(members.slice(0, selectableCount).map((member) => member.id))
    setResult(null)
    setError('')
  }

  const requestBalance = async (nextVariant) => {
    if (!canBalance) {
      setError(`팀당 ${teamSize}명으로 2개 이상의 팀을 만들 수 있도록 인원을 선택해주세요.`)
      return
    }

    try {
      setLoading(true)
      setError('')
      const { data } = await api.post('/api/clan/team-balance', {
        memberIds: validSelectedIds,
        teamSize,
        variant: nextVariant,
      })
      setResult(data)
      setVariant(Number(data?.variant) || 0)
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '팀을 편성하지 못했습니다.'))
    } finally {
      setLoading(false)
    }
  }

  return <section className="record-section clan-team-builder-section">
    <div className="record-section-header">
      <div>
        <h2 className="record-section-title">AUTO TEAM BUILDER</h2>
        <p className="clan-team-builder-copy">K/D·승률·평균 킬과 주무기 역할을 함께 비교해 전력을 나눕니다.</p>
      </div>
      <span className="record-section-sub">{validSelectedIds.length}명 선택 · 최대 10명</span>
    </div>

    <div className="clan-team-builder-controls">
      <div className="clan-team-size-picker">
        <div><span>TEAM SIZE</span><p>팀당 인원을 선택하면 전체 인원에 맞춰 팀 개수가 자동으로 결정됩니다.</p></div>
        <div className="clan-team-size-options" role="radiogroup" aria-label="팀당 인원">
          {TEAM_SIZE_OPTIONS.map((size) => <button
            type="button"
            role="radio"
            aria-checked={teamSize === size}
            className={teamSize === size ? 'active' : ''}
            key={size}
            onClick={() => { setTeamSize(size); setResult(null); setError(''); setVariant(0) }}
          >{size} VS {size}</button>)}
        </div>
      </div>

      <div className="clan-team-select-actions">
        <button type="button" onClick={selectAvailable} disabled={members.length < teamSize * 2}>가능 인원 선택</button>
        <button type="button" onClick={() => { setSelectedIds([]); setResult(null); setError('') }} disabled={selectedIds.length === 0}>선택 해제</button>
        <span>{selectionGuide(validSelectedIds.length, teamSize)}</span>
      </div>

      {members.length === 0
        ? <div className="clan-team-empty">이 클랜의 내 로스터에 멤버를 등록하면 자동 팀 편성을 사용할 수 있습니다.</div>
        : <div className="clan-team-member-selector">
          {members.map((member) => {
            const checked = validSelectedIds.includes(member.id)
            const disabled = !checked && validSelectedIds.length >= 10
            return <label className={checked ? 'clan-team-select-card selected' : 'clan-team-select-card'} key={member.id}>
              <input type="checkbox" checked={checked} disabled={disabled} onChange={() => toggleMember(member.id)} />
              <span>{checked ? 'SELECTED' : 'ROSTER'}</span>
              <strong>{member.userName}</strong>
              <small>{member.clanName}</small>
            </label>
          })}
        </div>}

      {error && <div className="clan-team-error" role="alert">{error}</div>}
      <div className="clan-team-submit-row">
        <p>전적 표본이 부족한 멤버는 선택 인원의 평균 점수로 보정됩니다.</p>
        <button type="button" disabled={!canBalance || loading} onClick={() => requestBalance(0)}>
          {loading ? 'ANALYZING...' : 'BALANCE TEAMS'}
        </button>
      </div>
    </div>

    {loading && <div className="clan-team-loading" aria-busy="true">{Array.from({ length: Math.max(2, teamCount) }, (_, index) => <div className="clan-shimmer" key={index} />)}</div>}
    {!loading && result && <TeamBalanceResult result={result} onReroll={() => requestBalance(variant + 1)} />}
  </section>
}

function TeamBalanceResult({ result, onReroll }) {
  const teams = Array.isArray(result?.teams) ? result.teams : []
  return <div className="clan-team-result">
    <div className="clan-team-result-summary">
      <div><span>BALANCE SCORE</span><strong>{result.balanceScore}<small>/100</small></strong></div>
      <div><span>POWER GAP</span><strong>{decimal(result.powerDifference)}</strong></div>
      <div><span>DATA READY</span><strong>{result.analyzedCount}<small>/{result.selectedCount}</small></strong></div>
      <div><span>TEAM FORMAT</span><strong>{result.teamSize}<small> x {teams.length}</small></strong></div>
      <p>{result.basis}</p>
      <button type="button" onClick={onReroll}>다른 조합</button>
    </div>
    <div className="clan-balanced-teams">
      {teams.map((team) => <BalancedTeam team={team} key={team.key} />)}
    </div>
  </div>
}

function BalancedTeam({ team }) {
  const roleText = Object.entries(team.roleCounts || {}).map(([role, count]) => `${role} ${count}`).join(' · ')
  return <article className={`clan-balanced-team ${String(team.key || '').toLowerCase()}`}>
    <header>
      <div><span>{team.key}</span><h3>{team.name}</h3><p>{roleText || '역할 분석 대기'}</p></div>
      <strong>{decimal(team.averagePower)}<small> POWER</small></strong>
    </header>
    <div className="clan-balanced-team-metrics">
      <span>승률 <b>{decimal(team.averageWinRate)}%</b></span>
      <span>K/D <b>{decimal(team.averageKillDeathRatio)}</b></span>
      <span>평균 킬 <b>{decimal(team.averageKill)}</b></span>
    </div>
    <div className="clan-balanced-member-list">
      {(team.members || []).map((member, index) => <div className={member.available ? 'clan-balanced-member' : 'clan-balanced-member unavailable'} key={member.id}>
        <em>{String(index + 1).padStart(2, '0')}</em>
        <div><strong>{member.userName}</strong><small>{member.primaryClass} · {member.combatType}</small></div>
        <span>{member.available ? `${decimal(member.killDeathRatio)} K/D` : '표본 부족'}</span>
        <b>{decimal(member.powerScore)}</b>
      </div>)}
    </div>
  </article>
}

function selectionGuide(count, teamSize) {
  const minimum = teamSize * 2
  if (count < minimum) return `${minimum - count}명 더 선택하면 ${teamSize} VS ${teamSize} 편성이 가능합니다.`
  const remainder = count % teamSize
  if (remainder !== 0) return `${teamSize - remainder}명 더 선택하면 팀당 ${teamSize}명으로 나눌 수 있습니다.`
  return `팀당 ${teamSize}명 · ${count / teamSize}개 팀 편성 가능`
}

function ClanRoster({ clanName, members, loading, pending, message, error, onDelete, onView }) {
  return <section className="record-section clan-roster-section">
    <div className="record-section-header"><h2 className="record-section-title">CLAN ROSTER</h2><span className="record-section-sub">{clanName} · {members.length}명</span></div>
    {(message || error) && <div className={error ? 'clan-roster-message error' : 'clan-roster-message'} aria-live="polite">{error || message}</div>}
    {loading ? <div className="clan-roster-loading"><div className="clan-roster-skeleton clan-shimmer" /><div className="clan-roster-skeleton clan-shimmer" /></div>
      : members.length === 0 ? <div className="clan-roster-empty">아직 등록된 클랜원이 없습니다.</div>
        : <div className="clan-roster-grid">{members.map((member) => <article className="clan-roster-card" key={member.id}>
          <img src="/sa-assets/sa-clan-basic.png" alt="" />
          <div><span>CLAN MEMBER</span><h3>{member.userName}</h3><p>{member.clanName}</p></div>
          <time dateTime={member.createdAt}>{formatRosterDate(member.createdAt)}</time>
          <div className="clan-roster-actions"><button type="button" onClick={() => onView(member)}>프로필 보기</button><button type="button" className="remove" disabled={pending === String(member.id)} onClick={() => onDelete(member)}>{pending === String(member.id) ? '삭제 중' : '삭제'}</button></div>
        </article>)}</div>}
  </section>
}

function ClanOverview({ clanName, userName, summary, registered, pending, onAdd }) {
  const kd = Number(summary?.averageDeath) > 0 ? Number(summary.averageKill) / Number(summary.averageDeath) : Number(summary?.averageKill ?? 0)
  const metrics = [
    ['MATCHES', summary?.matchCount ?? 0],
    ['WIN RATE', `${decimal(summary?.winRate)}%`],
    ['K / D', decimal(kd)],
    ['W · L · D', `${summary?.winCount ?? 0} · ${summary?.loseCount ?? 0} · ${summary?.drawCount ?? 0}`],
  ]
  return <section className="record-section clan-overview">
    <div className="clan-identity">
      <img className="clan-emblem" src="/sa-assets/sa-clan-basic.png" alt="" />
      <div className="clan-identity-copy"><span>ACTIVE CLAN</span><h2>{userName}</h2><p>{clanName}</p></div>
      <button
        className={registered ? 'clan-identity-roster-button registered' : 'clan-identity-roster-button'}
        type="button"
        disabled={registered || pending}
        onClick={onAdd}
      >
        {pending ? '등록 중' : registered ? '로스터 등록됨' : '클랜 로스터'}
      </button>
    </div>
    <div className="clan-metrics">{metrics.map(([label, value]) => <div className="clan-metric" key={label}><span>{label}</span><strong>{value}</strong></div>)}</div>
  </section>
}

function MatchDetail({ match, detail, teams }) {
  const type = detail.matchType || match.match_type || '-'
  return <><div className="match-detail-summary"><strong>{detail.matchMap || 'UNKNOWN MAP'}</strong><span>{detail.matchMode || match.match_mode || '-'} / {type}</span><em>{formatDate(detail.dateMatch || match.date_match)}</em></div>
    <div className="team-table-grid"><TeamTable title="RED TEAM" players={teams.red} matchType={type} /><TeamTable title="BLUE TEAM" players={teams.blue} matchType={type} /></div></>
}

function PageState({ label, title, copy }) {
  return <section className="clan-page-state"><span>{label}</span><strong>{title}</strong>{copy && <p>{copy}</p>}</section>
}

function LoadingState() {
  return <section className="record-section clan-loading" aria-busy="true"><span className="clan-visually-hidden">클랜 정보를 불러오는 중입니다.</span><div className="clan-loading-identity clan-shimmer" /><div className="clan-loading-metrics">{[0, 1, 2, 3].map((item) => <div className="clan-loading-metric clan-shimmer" key={item} />)}</div></section>
}

function mergeMatches(matches) {
  const unique = new Map(matches.filter((match) => match?.match_id).map((match) => [match.match_id, match]))
  return [...unique.values()].sort((a, b) => new Date(b.date_match) - new Date(a.date_match))
}

function splitTeams(players) {
  if (!players.length) return { red: [], blue: [] }
  const groups = new Map()
  players.forEach((player) => { const id = String(player.team_id ?? ''); if (!groups.has(id)) groups.set(id, []); groups.get(id).push(player) })
  const teams = [...groups.values()]
  return teams.length >= 2 ? { red: teams[0], blue: teams[1] } : { red: players, blue: [] }
}

function decimal(value) { const number = Number(value); return Number.isFinite(number) ? number.toFixed(1) : '0.0' }
function sameText(left, right) { return String(left ?? '').trim().toLocaleLowerCase('ko-KR') === String(right ?? '').trim().toLocaleLowerCase('ko-KR') }
function formatDate(value) { const date = new Date(value); return Number.isNaN(date.getTime()) ? '-' : new Intl.DateTimeFormat('ko-KR', { timeZone: 'Asia/Seoul', dateStyle: 'short', timeStyle: 'short' }).format(date) }
function formatRosterDate(value) { const date = new Date(value); return Number.isNaN(date.getTime()) ? '등록일 -' : `등록 ${new Intl.DateTimeFormat('ko-KR', { dateStyle: 'short' }).format(date)}` }

export default ClanPage
