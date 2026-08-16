const RANK_DISPLAY_NAMES = Object.freeze({
  '특정중사 4호봉': '특전중사 4호봉',
})

export function formatRankDisplayName(rankName) {
  return RANK_DISPLAY_NAMES[rankName] || rankName
}
