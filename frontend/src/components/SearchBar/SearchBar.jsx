import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import './SearchBar.css'

function SearchBar() {
  const navigate = useNavigate()
  const [value, setValue] = useState('')

  const handleSubmit = (e) => {
    e.preventDefault()
    const trimmed = value.trim()
    if (!trimmed) return
    navigate(`/player/${encodeURIComponent(trimmed)}`)
  }

  return (
    <form className="sa-searchbar" onSubmit={handleSubmit}>
      <div className="sa-search-label">PLAYER SEARCH</div>

      <div className="sa-search-frame">
        <div className="sa-search-left-dot" />

        <div className="sa-search-input-wrap">
          <input
            type="text"
            placeholder="NICKNAME"
            value={value}
            onChange={(e) => setValue(e.target.value)}
          />
          <span>ENTER PLAYER NAME</span>
        </div>

        <button type="submit">SEARCH</button>
      </div>
    </form>
  )
}

export default SearchBar