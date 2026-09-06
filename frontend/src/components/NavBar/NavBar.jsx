import { useEffect, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import api from '../../api/api'
import { readAuthSession, saveAuthSession, subscribeToAuthSession } from '../../utils/authSession'
import './NavBar.css'

function NavBar() {
  const { pathname } = useLocation()
  const [session, setSession] = useState(readAuthSession)

  useEffect(() => subscribeToAuthSession(() => setSession(readAuthSession())), [])

  useEffect(() => {
    const currentSession = readAuthSession()
    if (!currentSession || typeof currentSession.user?.admin === 'boolean') return
    api.get('/api/auth/me')
      .then(({ data }) => saveAuthSession({ ...currentSession, user: data }))
      .catch(() => {})
  }, [])

  const accountName = session?.user?.suddenNickname || ''
  const isLoggedIn = Boolean(session)
  const profileTo = isLoggedIn
    ? (accountName ? `/player/${encodeURIComponent(accountName)}` : '/mypage')
    : '/player'
  const clanTo = isLoggedIn ? '/clan' : '/login'
  const boardTo = isLoggedIn ? '/board/free' : '/login'

  const menus = [
    { id: 'profile', label: '프로필', to: profileTo },
    { id: 'weapons', label: '무기', to: '/weapons' },
    { id: 'matches', label: '전적', to: '/match' },
    { id: 'ranking', label: '랭킹', to: '/ranking' },
    { id: 'clan', label: '클랜', to: clanTo, state: isLoggedIn ? undefined : { from: { pathname: '/clan' } } },
    {
      id: 'board',
      label: '게시판',
      to: boardTo,
      state: isLoggedIn ? undefined : { from: { pathname: '/board/free' } },
      children: [
        {
          label: '자유게시판',
          to: isLoggedIn ? '/board/free' : '/login',
          state: isLoggedIn ? undefined : { from: { pathname: '/board/free' } },
        },
        {
          label: '문의사항',
          to: isLoggedIn ? '/board/support' : '/login',
          state: isLoggedIn ? undefined : { from: { pathname: '/board/support' } },
        },
        ...(session?.user?.admin ? [{ label: '게시판 관리', to: '/board/admin' }] : []),
      ],
    },
  ]

  const isActive = (id) => {
    if (id === 'profile') {
      return pathname === '/player' || pathname.startsWith('/player/')
    }

    if (id === 'weapons') {
      return pathname === '/weapons' || pathname.startsWith('/weapons/')
    }

    if (id === 'ranking') {
      return pathname === '/ranking' || pathname.startsWith('/ranking/')
    }

    if (id === 'clan') {
      return pathname === '/clan' || pathname.startsWith('/clan/')
    }

    if (id === 'board') {
      return pathname === '/board' || pathname.startsWith('/board/')
    }

    return id === 'matches' && pathname.startsWith('/match')
  }

  return (
    <nav className="sa-navbar" aria-label="주요 메뉴">
      <div className="sa-nav-dot left" aria-hidden="true" />

      {menus.map(({ id, label, to, state, children }) => {
        const active = isActive(id)

        return (
          <div className={`sa-nav-item${children ? ' has-submenu' : ''}`} key={id}>
            <Link
              className={`sa-nav-link${active ? ' is-active' : ''}`}
              to={to}
              state={state}
              aria-current={active ? 'page' : undefined}
              aria-haspopup={children ? 'menu' : undefined}
            >
              {label}
            </Link>
            {children && (
              <div className="sa-nav-submenu" role="menu" aria-label="게시판 메뉴">
                {children.map((child) => (
                  <Link
                    key={child.label}
                    to={child.to}
                    state={child.state}
                    role="menuitem"
                    className={pathname === child.to ? 'is-active' : undefined}
                  >
                    {child.label}
                  </Link>
                ))}
              </div>
            )}
          </div>
        )
      })}

      <div className="sa-nav-dot right" aria-hidden="true" />
    </nav>
  )
}

export default NavBar
