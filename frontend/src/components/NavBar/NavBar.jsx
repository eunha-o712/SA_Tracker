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

  const menus = [
    { label: 'PROFILE', to: profileTo },
    { label: 'WEAPONS', to: '/weapons' },
    { label: 'MATCHES', to: '/match' },
    { label: 'RANKING', to: '/ranking' },
    { label: 'CLAN', to: clanTo, state: isLoggedIn ? undefined : { from: { pathname: '/clan' } } },
    {
      label: 'BOARD',
      to: '/board/free',
      children: [
        { label: '자유게시판', to: '/board/free' },
        { label: '문의사항', to: '/board/support' },
        ...(session?.user?.admin ? [{ label: '게시판 관리', to: '/board/admin' }] : []),
      ],
    },
  ]

  const isActive = (label) => {
    if (label === 'PROFILE') {
      return pathname === '/player' || pathname.startsWith('/player/')
    }

    if (label === 'WEAPONS') {
      return pathname === '/weapons' || pathname.startsWith('/weapons/')
    }

    if (label === 'RANKING') {
      return pathname === '/ranking' || pathname.startsWith('/ranking/')
    }

    if (label === 'CLAN') {
      return pathname === '/clan' || pathname.startsWith('/clan/')
    }

    if (label === 'BOARD') {
      return pathname === '/board' || pathname.startsWith('/board/')
    }

    return label === 'MATCHES' && pathname.startsWith('/match')
  }

  return (
    <nav className="sa-navbar" aria-label="Main menu">
      <div className="sa-nav-dot left" aria-hidden="true" />

      {menus.map(({ label, to, state, children }) => {
        const active = isActive(label)

        return (
          <div className={`sa-nav-item${children ? ' has-submenu' : ''}`} key={label}>
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
                    key={child.to}
                    to={child.to}
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
