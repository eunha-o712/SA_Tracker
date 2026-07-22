import './MatchFilter.css'

const QUICK_SCOPES = [
  { label: '최근 매치', value: 'RECENT' },
  { label: '폭파미션', value: 'BOMB' },
  { label: '팀데스매치', value: 'DEATHMATCH' },
  { label: '개인전', value: 'SOLO' },
  { label: '클랜전', value: 'CLAN' },
]

const MATCH_TYPES = [
  '일반전',
  '클랜전',
  '퀵매치 클랜전',
  '클랜 랭크전',
  '랭크전 솔로',
  '랭크전 파티',
  '토너먼트',
]

const MATCH_MODES = [
  { label: '폭파미션', value: '폭파미션' },
  { label: '팀데스매치', value: '데스매치' },
  { label: '개인전', value: '개인전' },
]

const MATCH_MAPS_BY_MODE = {
  폭파미션: [
    '3보급창고',
    'A보급창고',
    '드래곤로드',
    '프로방스',
    '크로스포트',
    '시티캣',
    '듀오',
  ],
  데스매치: [
    '웨어하우스',
    '민속촌',
  ],
  개인전: [
    '빌라',
    '케이지-개인전',
    '3보급-개인전',
    '스쿨',
    '3보급-웨폰챌린지',
  ],
}

function MatchFilter({
  scope,
  matchType,
  matchMode,
  matchMap,
  onScopeChange,
  onMatchTypeChange,
  onMatchModeChange,
  onMatchMapChange,
  onSearch,
  onReset,
  disabled = false,
}) {
  const availableMaps = MATCH_MAPS_BY_MODE[matchMode] ?? []

  return (
    <div className="match-filter">
      <div className="match-type-tabs" role="tablist" aria-label="빠른 매치 조회">
        {QUICK_SCOPES.map((item) => (
          <button
            type="button"
            role="tab"
            key={item.value}
            className={scope === item.value ? 'active' : ''}
            aria-selected={scope === item.value}
            disabled={disabled}
            onClick={() => onScopeChange(item.value)}
          >
            {item.label}
          </button>
        ))}
      </div>

      <div className="match-filter-main">
        <div className="match-filter-title">
          <img src="/sa-assets/sa-icon-filter.png" alt="" />
          <div>
            <strong>MATCH CONTROL</strong>
            <span>TACTICAL FILTER SYSTEM</span>
          </div>
        </div>

        <div className="match-filter-actions">
          <select
            value={matchType}
            disabled={disabled}
            aria-label="매치 유형"
            onChange={(event) => onMatchTypeChange(event.target.value)}
          >
            {MATCH_TYPES.map((type) => (
              <option key={type} value={type}>{type}</option>
            ))}
          </select>

          <select
            value={matchMode}
            disabled={disabled}
            aria-label="매치 모드"
            onChange={(event) => onMatchModeChange(event.target.value)}
          >
            {MATCH_MODES.map((mode) => (
              <option key={mode.value} value={mode.value}>{mode.label}</option>
            ))}
          </select>

          <select
            value={matchMap}
            disabled={disabled}
            aria-label="맵"
            onChange={(event) => onMatchMapChange(event.target.value)}
          >
            <option value="ALL">전체 맵</option>
            {availableMaps.map((map) => (
              <option key={map} value={map}>{map}</option>
            ))}
          </select>

          <button type="button" disabled={disabled} onClick={onSearch}>조회</button>
          <button
            type="button"
            className="match-filter-reset"
            disabled={disabled}
            onClick={onReset}
          >
            초기화
          </button>
        </div>
      </div>
    </div>
  )
}

export default MatchFilter
