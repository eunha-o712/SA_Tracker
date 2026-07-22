import { useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  addRecentSearch,
  clearRecentSearches,
  readRecentSearches,
  removeRecentSearch,
} from '../../utils/recentSearches'
import { readAuthSession, subscribeToAuthSession } from '../../utils/authSession'
import './SearchBar.css'

function SearchBar({ compact = false, suggestionsUp = false }) {
  const navigate = useNavigate()
  const searchBarRef = useRef(null)
  const [value, setValue] = useState('')
  const [isAuthenticated, setIsAuthenticated] = useState(() => Boolean(readAuthSession()))
  const [recentSearches, setRecentSearches] = useState(() => (
    readAuthSession() ? readRecentSearches() : []
  ))
  const [isOpen, setIsOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(-1)

  const suggestions = useMemo(() => {
    if (!isAuthenticated) return []

    const normalizedValue = value.trim().toLocaleLowerCase('ko-KR')
    if (!normalizedValue) return recentSearches
    return recentSearches.filter((item) =>
      item.toLocaleLowerCase('ko-KR').includes(normalizedValue)
    )
  }, [isAuthenticated, recentSearches, value])

  useEffect(() => subscribeToAuthSession(() => {
    const authenticated = Boolean(readAuthSession())
    setIsAuthenticated(authenticated)
    setRecentSearches(authenticated ? readRecentSearches() : [])
    setIsOpen(false)
    setActiveIndex(-1)
  }), [])

  useEffect(() => {
    const handlePointerDown = (event) => {
      if (!searchBarRef.current?.contains(event.target)) {
        setIsOpen(false)
        setActiveIndex(-1)
      }
    }

    document.addEventListener('pointerdown', handlePointerDown)
    return () => document.removeEventListener('pointerdown', handlePointerDown)
  }, [])

  const handleSubmit = (e) => {
    e.preventDefault()
    if (activeIndex >= 0 && suggestions[activeIndex]) {
      runSearch(suggestions[activeIndex])
      return
    }
    runSearch(value)
  }

  const runSearch = (userName) => {
    const trimmed = userName.trim()
    if (!trimmed) return

    setValue(trimmed)
    if (isAuthenticated) {
      setRecentSearches(addRecentSearch(trimmed))
    }
    setIsOpen(false)
    setActiveIndex(-1)
    navigate(`/player/${encodeURIComponent(trimmed)}`)
  }

  const handleKeyDown = (event) => {
    if (event.key === 'Escape') {
      setIsOpen(false)
      setActiveIndex(-1)
      return
    }

    if (event.key === 'Enter' && isOpen && activeIndex >= 0) {
      event.preventDefault()
      runSearch(suggestions[activeIndex])
      return
    }

    if (!suggestions.length) return

    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setIsOpen(true)
      setActiveIndex((current) => (current + 1) % suggestions.length)
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault()
      setIsOpen(true)
      setActiveIndex((current) =>
        current <= 0 ? suggestions.length - 1 : current - 1
      )
    }
  }

  const handleRemove = (userName) => {
    setRecentSearches(removeRecentSearch(userName))
    setActiveIndex(-1)
  }

  const handleClear = () => {
    setRecentSearches(clearRecentSearches())
    setIsOpen(false)
    setActiveIndex(-1)
  }

  return (
    <form
      className={`sa-searchbar${compact ? ' sa-searchbar--compact' : ''}${suggestionsUp ? ' sa-searchbar--suggestions-up' : ''}`}
      onSubmit={handleSubmit}
      ref={searchBarRef}
    >
      <div className="sa-search-label">PLAYER SEARCH</div>

      <div className="sa-search-frame">
        <div className="sa-search-left-dot" />

        <div className="sa-search-input-wrap">
          <input
            type="text"
            placeholder="NICKNAME"
            value={value}
            role={isAuthenticated ? 'combobox' : undefined}
            aria-autocomplete={isAuthenticated ? 'list' : undefined}
            aria-expanded={isAuthenticated ? isOpen && suggestions.length > 0 : undefined}
            aria-controls={isAuthenticated ? 'recent-player-searches' : undefined}
            aria-activedescendant={
              isAuthenticated && activeIndex >= 0
                ? `recent-player-search-${activeIndex}`
                : undefined
            }
            autoComplete="off"
            onChange={(e) => {
              setValue(e.target.value)
              setIsOpen(isAuthenticated)
              setActiveIndex(-1)
            }}
            onFocus={() => setIsOpen(isAuthenticated)}
            onKeyDown={handleKeyDown}
          />
          <span>ENTER PLAYER NAME</span>
        </div>

        <button type="submit">SEARCH</button>
      </div>

      {isAuthenticated && isOpen && suggestions.length > 0 && (
        <div className="sa-search-suggestions">
          <div className="sa-search-suggestions-head">
            <span>{value.trim() ? '일치하는 검색어' : '최근 검색어'}</span>
            <button type="button" onClick={handleClear}>전체 삭제</button>
          </div>

          <ul id="recent-player-searches" role="listbox">
            {suggestions.map((userName, index) => (
              <li key={userName}>
                <button
                  id={`recent-player-search-${index}`}
                  className={`sa-search-suggestion ${activeIndex === index ? 'active' : ''}`}
                  type="button"
                  role="option"
                  aria-selected={activeIndex === index}
                  onMouseEnter={() => setActiveIndex(index)}
                  onClick={() => runSearch(userName)}
                >
                  <span>{userName}</span>
                  <small>다시 검색</small>
                </button>
                <button
                  className="sa-search-remove"
                  type="button"
                  aria-label={`${userName} 최근 검색어 삭제`}
                  onClick={() => handleRemove(userName)}
                >
                  삭제
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}
    </form>
  )
}

export default SearchBar
