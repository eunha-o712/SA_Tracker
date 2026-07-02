import { useState } from 'react'
import api from '../../api/api'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import MatchCard from '../../components/MatchCard/MatchCard'
import MatchFilter from '../../components/MatchFilter/MatchFilter'
import NavBar from '../../components/NavBar/NavBar'
import TeamTable from '../../components/TeamTable/TeamTable'
import './MatchDetailPage.css'

const PAGE_SIZE = 20
const DEFAULT_FILTERS = {
  scope: 'RECENT',
  matchType: '일반전',
  matchMode: '폭파미션',
  matchMap: 'ALL',
}

function MatchDetailPage() {
  const [userName, setUserName] = useState('')
  const [searchName, setSearchName] = useState('')
  const [matches, setMatches] = useState([])
  const [page, setPage] = useState(1)
  const [totalCount, setTotalCount] = useState(0)
  const [scope, setScope] = useState(DEFAULT_FILTERS.scope)
  const [matchType, setMatchType] = useState(DEFAULT_FILTERS.matchType)
  const [matchMode, setMatchMode] = useState(DEFAULT_FILTERS.matchMode)
  const [matchMap, setMatchMap] = useState(DEFAULT_FILTERS.matchMap)
  const [appliedFilters, setAppliedFilters] = useState(DEFAULT_FILTERS)
  const [selectedMatchId, setSelectedMatchId] = useState('')
  const [matchDetail, setMatchDetail] = useState(null)
  const [detailCache, setDetailCache] = useState({})
  const [loading, setLoading] = useState(false)
  const [detailLoading, setDetailLoading] = useState(false)
  const [error, setError] = useState('')
  const [detailError, setDetailError] = useState('')

  const fetchMatches = async (targetUserName, targetPage, filters) => {
    const nextUserName = targetUserName.trim()
    if (!nextUserName) return

    try {
      setLoading(true)
      setError('')
      closeDetail()

      const response = await api.get('/api/match', {
        params: {
          userName: nextUserName,
          page: targetPage,
          scope: filters.scope,
          matchType: filters.matchType,
          matchMode: filters.matchMode,
          matchMap: filters.matchMap,
        },
      })

      const data = response.data ?? {}
      setUserName(nextUserName)
      setPage(Number(data.page) || targetPage)
      setMatches(Array.isArray(data.matches) ? data.matches : [])
      setTotalCount(Number(data.totalCount) || 0)
    } catch (requestError) {
      setMatches([])
      setTotalCount(0)
      setError('매치 기록을 불러오지 못했습니다.')
    } finally {
      setLoading(false)
    }
  }

  const handleNicknameSearch = () => {
    const nextUserName = searchName.trim()
    if (!nextUserName) return

    const filters = { scope: 'RECENT', matchType, matchMode: 'ALL', matchMap: 'ALL' }
    setScope('RECENT')
    setMatchMode(DEFAULT_FILTERS.matchMode)
    setMatchMap('ALL')
    setAppliedFilters(filters)
    setDetailCache({})
    fetchMatches(nextUserName, 1, filters)
  }

  const handleScopeChange = (nextScope) => {
    const nextMatchMode = getScopeMatchMode(nextScope)
    const nextMatchType = nextScope === 'CLAN' ? '퀵매치 클랜전' : matchType
    setScope(nextScope)
    setMatchType(nextMatchType)
    setMatchMode(nextMatchMode === 'ALL' ? DEFAULT_FILTERS.matchMode : nextMatchMode)
    setMatchMap('ALL')

    if (!userName) return

    const filters = {
      scope: nextScope,
      matchType: nextMatchType,
      matchMode: nextMatchMode,
      matchMap: 'ALL',
    }
    setAppliedFilters(filters)
    fetchMatches(userName, 1, filters)
  }

  const handleFilterSearch = () => {
    if (!userName) return

    const filters = { scope, matchType, matchMode, matchMap }
    setAppliedFilters(filters)
    fetchMatches(userName, 1, filters)
  }

  const handleMatchModeChange = (nextMatchMode) => {
    setMatchMode(nextMatchMode)
    setScope(getMatchModeScope(nextMatchMode))
  }

  const handleFilterReset = () => {
    setScope(DEFAULT_FILTERS.scope)
    setMatchType(DEFAULT_FILTERS.matchType)
    setMatchMode(DEFAULT_FILTERS.matchMode)
    setMatchMap(DEFAULT_FILTERS.matchMap)
    setAppliedFilters(DEFAULT_FILTERS)

    if (userName) {
      fetchMatches(userName, 1, DEFAULT_FILTERS)
    }
  }

  const handlePageChange = (nextPage) => {
    if (nextPage < 1 || nextPage > totalPages || nextPage === page || loading) return
    fetchMatches(userName, nextPage, appliedFilters)
  }

  const handleMatchToggle = async (matchId) => {
    if (!matchId) return
    if (selectedMatchId === matchId) {
      closeDetail()
      return
    }

    setSelectedMatchId(matchId)
    setMatchDetail(null)
    setDetailError('')

    const cachedDetail = detailCache[matchId]
    if (cachedDetail) {
      setMatchDetail(cachedDetail)
      return
    }

    try {
      setDetailLoading(true)
      const response = await api.get('/api/match/detail', { params: { matchId } })
      const detail = response.data ?? null
      setMatchDetail(detail)
      setDetailCache((previousCache) => ({ ...previousCache, [matchId]: detail }))
    } catch (requestError) {
      setMatchDetail(null)
      setDetailError('상세 기록을 불러오지 못했습니다.')
    } finally {
      setDetailLoading(false)
    }
  }

  const closeDetail = () => {
    setSelectedMatchId('')
    setMatchDetail(null)
    setDetailError('')
    setDetailLoading(false)
  }

  const totalPages = Math.max(1, Math.ceil(totalCount / PAGE_SIZE))
  const pageNumbers = getPageNumbers(page, totalPages)
  const teams = splitTeams(matchDetail?.matchDetail ?? [])

  return (
    <div className="match-detail-shell">
      <Header />
      <NavBar />

      <main className="match-detail-page">
        <div className="match-detail-container">
          <div className="match-archive-banner">
            <img src="/sa-assets/sa-match-banner.png" alt="Match Archive" />
          </div>

          <section className="record-section match-data-section">
            <div className="record-section-header">
              <h2 className="record-section-title">MATCH DATA</h2>
              <span className="record-section-sub">전술 전투 기록</span>
            </div>

            <div className="match-search-row">
              <input
                type="search"
                value={searchName}
                placeholder="닉네임 입력"
                aria-label="닉네임"
                onChange={(event) => setSearchName(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === 'Enter') handleNicknameSearch()
                }}
              />
              <button type="button" disabled={loading} onClick={handleNicknameSearch}>검색</button>
            </div>

            <MatchFilter
              scope={scope}
              matchType={matchType}
              matchMode={matchMode}
              matchMap={matchMap}
              disabled={loading}
              onScopeChange={handleScopeChange}
              onMatchTypeChange={setMatchType}
              onMatchModeChange={handleMatchModeChange}
              onMatchMapChange={setMatchMap}
              onSearch={handleFilterSearch}
              onReset={handleFilterReset}
            />

            <div className="match-page-info">
              <span>MATCH ARCHIVE</span>
              <em>PAGE {page} / {totalPages}</em>
            </div>

            {loading && <div className="match-page-state">매치 기록을 불러오는 중입니다.</div>}
            {!loading && error && <div className="match-page-state error">{error}</div>}

            {!loading && !error && (
              <div className="match-page-list">
                {matches.length === 0 ? (
                  <div className="match-page-state">표시할 매치 기록이 없습니다.</div>
                ) : (
                  matches.map((match) => (
                    <div className="match-card-wrap" key={match.match_id}>
                      <MatchCard
                        match={match}
                        selected={selectedMatchId === match.match_id}
                        onToggle={handleMatchToggle}
                      />

                      {selectedMatchId === match.match_id && (
                        <div className="match-inline-detail">
                          {detailLoading ? (
                            <div className="match-page-state">상세 기록을 불러오는 중입니다.</div>
                          ) : detailError ? (
                            <div className="match-page-state error">{detailError}</div>
                          ) : matchDetail ? (
                            <MatchInlineDetail match={match} detail={matchDetail} teams={teams} />
                          ) : (
                            <div className="match-page-state error">상세 기록이 없습니다.</div>
                          )}
                        </div>
                      )}
                    </div>
                  ))
                )}
              </div>
            )}

            {userName && !loading && (
              <div className="match-pagination">
                <button
                  type="button"
                  className="page-arrow"
                  disabled={page <= 1}
                  aria-label="이전 페이지"
                  onClick={() => handlePageChange(page - 1)}
                >
                  &lt;
                </button>

                {pageNumbers.map((pageNumber) => (
                  <button
                    type="button"
                    key={pageNumber}
                    className={page === pageNumber ? 'active' : ''}
                    aria-current={page === pageNumber ? 'page' : undefined}
                    onClick={() => handlePageChange(pageNumber)}
                  >
                    {pageNumber}
                  </button>
                ))}

                <button
                  type="button"
                  className="page-arrow"
                  disabled={page >= totalPages}
                  aria-label="다음 페이지"
                  onClick={() => handlePageChange(page + 1)}
                >
                  &gt;
                </button>
              </div>
            )}
          </section>
        </div>
      </main>

      <Footer />
    </div>
  )
}

function MatchInlineDetail({ match, detail, teams }) {
  const matchMode = detail.matchMode || match.match_mode || '-'
  const matchType = detail.matchType || match.match_type || '-'
  const matchMap = detail.matchMap || 'UNKNOWN MAP'
  const dateMatch = detail.dateMatch || match.date_match

  return (
    <>
      <div className="match-detail-summary">
        <strong>{matchMap}</strong>
        <span>{matchMode} / {matchType}</span>
        <em>{formatDate(dateMatch)}</em>
      </div>
      <div className="team-table-grid">
        <TeamTable title="RED TEAM" players={teams.red} matchType={matchType} />
        <TeamTable title="BLUE TEAM" players={teams.blue} matchType={matchType} />
      </div>
    </>
  )
}

function splitTeams(players) {
  if (!Array.isArray(players) || players.length === 0) return { red: [], blue: [] }

  const groups = new Map()
  players.forEach((player) => {
    const teamId = String(player.team_id ?? '').trim()
    if (!teamId) return
    if (!groups.has(teamId)) groups.set(teamId, [])
    groups.get(teamId).push(player)
  })

  const teamGroups = Array.from(groups.values())
  return teamGroups.length >= 2
    ? { red: teamGroups[0], blue: teamGroups[1] }
    : { red: players, blue: [] }
}

function getPageNumbers(currentPage, totalPages) {
  const groupSize = 10
  const startPage = Math.floor((currentPage - 1) / groupSize) * groupSize + 1
  const endPage = Math.min(startPage + groupSize - 1, totalPages)
  return Array.from({ length: endPage - startPage + 1 }, (_, index) => startPage + index)
}

function getScopeMatchMode(scope) {
  if (scope === 'BOMB') return '폭파미션'
  if (scope === 'DEATHMATCH') return '데스매치'
  if (scope === 'SOLO') return '개인전'
  if (scope === 'CLAN') return '폭파미션'
  return 'ALL'
}

function getMatchModeScope(matchMode) {
  if (matchMode === '폭파미션') return 'BOMB'
  if (matchMode === '데스매치') return 'DEATHMATCH'
  if (matchMode === '개인전') return 'SOLO'
  return 'RECENT'
}

function formatDate(value) {
  if (!value) return '-'
  const utcDate = new Date(value)
  if (Number.isNaN(utcDate.getTime())) return String(value).replace('T', ' ').slice(0, 16)

  return new Intl.DateTimeFormat('ko-KR', {
    timeZone: 'Asia/Seoul',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
    .format(utcDate)
    .replace(/\. /g, '-')
    .replace('.', '')
}

export default MatchDetailPage
