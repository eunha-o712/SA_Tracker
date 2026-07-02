import './ClanTeamTable.css'

function ClanTeamTable({
  title,
  players = [],
}) {
  return (
    <section className="clan-team-table">
      <div className="clan-team-table-title">
        <span>{title}</span>
        <em>{getMainClanName(players)}</em>
      </div>

      <div className="clan-team-table-head">
        <span>닉네임</span>
        <span>클랜</span>
        <span>K</span>
        <span>D</span>
        <span>A</span>
        <span>HS</span>
        <span>DMG</span>
      </div>

      {players.length === 0 ? (
        <div className="team-table-empty">
          팀 데이터가 없습니다.
        </div>
      ) : (
        players.map((player, index) => (
          <div
            className="clan-team-table-row"
            key={`${getPlayerName(player)}-${index}`}
          >
            <span>{getPlayerName(player)}</span>
            <span>{getClanName(player)}</span>
            <strong>{formatNumber(player.kill)}</strong>

            <strong className="death">
              {formatNumber(player.death)}
            </strong>

            <strong>{formatNumber(player.assist)}</strong>
            <strong>{formatNumber(player.headshot)}</strong>
            <strong>{formatDamage(player.damage)}</strong>
          </div>
        ))
      )}
    </section>
  )
}

function getPlayerName(player) {
  const playerName =
    player?.user_name ??
    player?.userName ??
    ''

  return String(playerName).trim() || '-'
}

function getClanName(player) {
  const clanName =
    player?.clan_name ??
    player?.clanName ??
    ''

  return String(clanName).trim() || '-'
}

function getMainClanName(players) {
  const clanName = players
    .map((player) => getClanName(player))
    .find((name) => name !== '-')

  return clanName || 'CLAN MATCH'
}

function formatNumber(value) {
  if (value === null || value === undefined) {
    return '-'
  }

  const numberValue = Number(value)

  if (Number.isNaN(numberValue)) {
    return value
  }

  return numberValue
}

function formatDamage(value) {
  if (value === null || value === undefined) {
    return '-'
  }

  const numberValue = Number(value)

  if (Number.isNaN(numberValue)) {
    return value
  }

  return Math.round(numberValue)
}

export default ClanTeamTable