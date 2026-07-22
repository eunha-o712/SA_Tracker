import { useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import { preloadSessionData } from '../../api/apiCache'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import { saveAuthSession } from '../../utils/authSession'
import './AuthPage.css'

function AuthPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const resetToken = searchParams.get('resetToken') || ''
  const [mode, setMode] = useState(() => resetToken ? 'reset' : 'login')
  const [form, setForm] = useState({ email: '', password: '' })
  const [resetUrl, setResetUrl] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const isRegister = mode === 'register'
  const isForgot = mode === 'forgot'
  const isReset = mode === 'reset'

  const changeMode = (nextMode) => {
    setMode(nextMode)
    setError('')
    setSuccess('')
    setResetUrl('')
    if (nextMode !== 'reset') {
      setSearchParams({})
    }
  }

  const handleChange = ({ target }) => {
    setForm((current) => ({ ...current, [target.name]: target.value }))
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    setIsSubmitting(true)

    try {
      if (isForgot) {
        const { data } = await api.post('/api/auth/password-reset/request', { email: form.email })
        setSuccess(data?.message || 'If the email exists, a password reset link has been sent.')
        setResetUrl(data?.devResetUrl || '')
        return
      }

      if (isReset) {
        const { data } = await api.post('/api/auth/password-reset/confirm', {
          token: resetToken,
          password: form.password,
        })
        saveAuthSession(data)
        navigate(getAccountLandingPath(data.user), { replace: true })
        return
      }

      const endpoint = isRegister ? '/api/auth/register' : '/api/auth/login'
      const { data } = await api.post(endpoint, {
        email: form.email,
        password: form.password,
      })
      saveAuthSession(data)

      const linkedName = data?.user?.suddenNickname || ''
      void preloadSessionData(linkedName)

      const requestedPath = location.state?.from
        ? `${location.state.from.pathname}${location.state.from.search || ''}${location.state.from.hash || ''}`
        : ''
      navigate(requestedPath || getAccountLandingPath(data.user, isRegister), { replace: true })
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, 'Request failed. Please try again.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="auth-shell">
      <Header />
      <NavBar />

      <main className="auth-page">
        <section className="auth-panel" aria-labelledby="auth-title">
          <div className="auth-panel__eyebrow">MEMBER ACCESS</div>
          <h1 id="auth-title" className="auth-panel__title">
            {isForgot ? 'RESET ACCESS' : isReset ? 'NEW PASSWORD' : isRegister ? 'CREATE ACCOUNT' : 'LOGIN'}
          </h1>
          <p className="auth-panel__description">
            {isForgot
              ? 'Enter your email and we will send a password reset link.'
              : isReset
                ? 'Choose a new password for your SA-Tracker account.'
                : isRegister
                  ? 'Sign up with email only. Your account starts as user001-style ID.'
                  : 'Enter with your SA-Tracker account.'}
          </p>

          {!isReset && (
            <div className="auth-tabs" role="tablist" aria-label="Account menu">
              <button className={`auth-tab${!isRegister && !isForgot ? ' is-active' : ''}`} type="button" role="tab"
                aria-selected={!isRegister && !isForgot} onClick={() => changeMode('login')}>
                LOGIN
              </button>
              <button className={`auth-tab${isRegister ? ' is-active' : ''}`} type="button" role="tab"
                aria-selected={isRegister} onClick={() => changeMode('register')}>
                SIGN UP
              </button>
            </div>
          )}

          <form className="auth-form" onSubmit={handleSubmit}>
            {!isReset && (
              <label className="auth-field">
                <span>EMAIL</span>
                <input name="email" type="email" value={form.email} onChange={handleChange}
                  maxLength="254" autoComplete="email" placeholder="you@example.com" required />
              </label>
            )}

            {!isForgot && (
              <label className="auth-field">
                <span>{isReset ? 'NEW PASSWORD' : 'PASSWORD'}</span>
                <input name="password" type="password" value={form.password} onChange={handleChange}
                  minLength="8" maxLength="72" autoComplete={isRegister || isReset ? 'new-password' : 'current-password'}
                  placeholder="8+ characters" required />
              </label>
            )}

            <div className="auth-message" role="alert" aria-live="polite">{error}</div>
            {success && <div className="auth-message auth-message--success" role="status">{success}</div>}
            {resetUrl && (
              <a className="auth-reset-link" href={resetUrl}>
                Open dev reset link
              </a>
            )}
            <button className="auth-submit" type="submit" disabled={isSubmitting}>
              {isSubmitting
                ? 'CONNECTING...'
                : isForgot
                  ? 'SEND RESET LINK'
                  : isReset
                    ? 'RESET PASSWORD'
                    : isRegister
                      ? 'CREATE ACCOUNT'
                      : 'ENTER'}
            </button>
          </form>

          {!isRegister && !isReset && (
            <button className="auth-text-button" type="button" onClick={() => changeMode(isForgot ? 'login' : 'forgot')}>
              {isForgot ? 'Back to login' : 'Forgot password?'}
            </button>
          )}

          <p className="auth-panel__note">
            Sudden Attack account linking is optional and handled in My Page after sign up.
          </p>
        </section>
      </main>

      <Footer />
    </div>
  )
}

function getAccountLandingPath(user, preferMyPage = false) {
  if (preferMyPage || !user?.suddenNickname) return '/mypage'
  return `/player/${encodeURIComponent(user.suddenNickname)}`
}

export default AuthPage
