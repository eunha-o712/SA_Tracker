import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import api from '../../api/api'
import { clearAuthSession, readAuthSession, subscribeToAuthSession } from '../../utils/authSession'
import './Header.css'

function Header() {
  const navigate = useNavigate()
  const [session, setSession] = useState(readAuthSession)

  useEffect(() => subscribeToAuthSession(() => setSession(readAuthSession())), [])

  const handleLogout = async () => {
    try {
      await api.post('/api/auth/logout')
    } finally {
      clearAuthSession()
      setSession(null)
      navigate('/')
    }
  }

  return (
    <header className="global-header">
      <Link className="global-header__inner" to="/" aria-label="SA-Tracker 메인 페이지로 이동">
        <img
          src="/sa-assets/sa-global-header-ver1.png"
          alt="SA-Tracker global header"
          className="global-header__image"
        />
      </Link>
      <div className="global-header__account">
        {session ? (
          <>
            <Link className="global-header__user" to="/mypage">
              {session.user.suddenNickname || session.user.displayName || session.user.loginId || session.user.email}
              <HeaderBadge user={session.user} />
            </Link>
            <Link className="global-header__account-link" to="/mypage">
              MY PAGE
            </Link>
            <button className="global-header__account-link" type="button" onClick={handleLogout}>
              LOGOUT
            </button>
          </>
        ) : (
          <Link className="global-header__account-link" to="/login">LOGIN</Link>
        )}
      </div>
    </header>
  )
}

function HeaderBadge({ user }) {
  if (!user?.ouid) return null
  const status = user.nicknameVerified ? 'verified' : 'linked'
  const label = status === 'verified' ? '운영자 수동 확인 완료' : 'OUID 연결'
  return <span className={`global-header__badge ${status}`} role="img" aria-label={label} title={label} />
}

export default Header
