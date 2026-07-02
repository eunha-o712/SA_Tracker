import ClanTeamTable from './ClanTeamTable'
import NormalTeamTable from './NormalTeamTable'
import './TeamTable.css'

function TeamTable({
  title,
  players = [],
  matchType = '',
}) {
  if (isClanMatch(matchType)) {
    return (
      <ClanTeamTable
        title={title}
        players={players}
      />
    )
  }

  return (
    <NormalTeamTable
      title={title}
      players={players}
    />
  )
}

function isClanMatch(matchType) {
  const value = normalizeText(matchType)

  return (
    value === '클랜전' ||
    value === '퀵매치클랜전' ||
    value === '클랜랭크전'
  )
}

function normalizeText(value) {
  return String(value ?? '')
    .trim()
    .replace(/\s/g, '')
    .replace(/-/g, '')
    .replace(/_/g, '')
    .toLowerCase()
}

export default TeamTable