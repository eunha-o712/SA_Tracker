import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import api, { getApiErrorMessage } from '../../api/api'
import Footer from '../../components/Footer/Footer'
import Header from '../../components/Header/Header'
import NavBar from '../../components/NavBar/NavBar'
import { clearAuthSession, saveAuthSession } from '../../utils/authSession'
import './AuthPage.css'

function AuthPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams, setSearchParams] = useSearchParams()
  const [resetToken] = useState(() => searchParams.get('resetToken') || '')
  const [verifyEmailToken] = useState(() => searchParams.get('verifyEmailToken') || '')
  const [mode, setMode] = useState(() => resetToken ? 'reset' : 'login')
  const [form, setForm] = useState({ email: '', password: '', passwordConfirm: '' })
  const [verificationEmail, setVerificationEmail] = useState('')
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const emailVerificationStarted = useRef(false)
  const isRegister = mode === 'register'
  const isForgot = mode === 'forgot'
  const isReset = mode === 'reset'
  const isVerificationPending = mode === 'verify'

  useEffect(() => {
    if (!resetToken) return
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      next.delete('resetToken')
      return next
    }, { replace: true })
  }, [resetToken, setSearchParams])

  useEffect(() => {
    if (!verifyEmailToken || emailVerificationStarted.current) return
    emailVerificationStarted.current = true

    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      next.delete('verifyEmailToken')
      return next
    }, { replace: true })
    setMode('login')
    setError('')
    setSuccess('')
    setIsSubmitting(true)

    api.post('/api/auth/email-verification/confirm', { token: verifyEmailToken })
      .then(({ data }) => {
        setSuccess(data?.message || '이메일 인증이 완료되었습니다. 로그인해 주세요.')
      })
      .catch((requestError) => {
        setError(getApiErrorMessage(requestError, '이메일 인증을 완료하지 못했습니다.'))
      })
      .finally(() => {
        setIsSubmitting(false)
      })
  }, [verifyEmailToken, setSearchParams])

  const changeMode = (nextMode) => {
    setMode(nextMode)
    setError('')
    setSuccess('')
    setForm((current) => ({ ...current, password: '', passwordConfirm: '' }))
    if (nextMode !== 'reset') {
      setSearchParams({})
    }
  }

  const handleChange = ({ target }) => {
    setForm((current) => ({ ...current, [target.name]: target.value }))
  }

  const handleVerificationResend = async () => {
    if (!verificationEmail || isSubmitting) return
    setError('')
    setSuccess('')
    setIsSubmitting(true)

    try {
      const { data } = await api.post('/api/auth/email-verification/resend', {
        email: verificationEmail,
      })
      setSuccess(data?.message || '인증이 필요한 계정이라면 이메일 인증 링크를 전송했습니다.')
    } catch (requestError) {
      setError(getApiErrorMessage(requestError, '인증메일을 다시 보내지 못했습니다.'))
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    setError('')
    setSuccess('')
    setIsSubmitting(true)

    try {
      if (isForgot) {
        const { data } = await api.post('/api/auth/password-reset/request', { email: form.email })
        setSuccess(data?.message || '가입된 계정이 있다면 비밀번호 재설정 링크를 전송했습니다.')
        return
      }

      if (isReset) {
        if (form.password !== form.passwordConfirm) {
          setError('새 비밀번호와 비밀번호 확인이 일치하지 않습니다.')
          return
        }
        const { data } = await api.post('/api/auth/password-reset/confirm', {
          token: resetToken,
          password: form.password,
          passwordConfirm: form.passwordConfirm,
        })
        clearAuthSession()
        setForm({ email: '', password: '', passwordConfirm: '' })
        setMode('login')
        setSuccess(data?.message || '비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.')
        return
      }

      if (isRegister) {
        const { data } = await api.post('/api/auth/register', {
          email: form.email,
          password: form.password,
        })
        setVerificationEmail(form.email.trim())
        setForm((current) => ({ ...current, password: '', passwordConfirm: '' }))
        setMode('verify')
        setSuccess(data?.message || '입력한 이메일로 인증 링크를 전송했습니다.')
        return
      }

      const { data } = await api.post('/api/auth/login', {
        email: form.email,
        password: form.password,
      })
      saveAuthSession(data)

      const requestedPath = location.state?.from
        ? `${location.state.from.pathname}${location.state.from.search || ''}${location.state.from.hash || ''}`
        : ''
      navigate(requestedPath || '/', { replace: true })
    } catch (requestError) {
      if (requestError?.response?.data?.code === 'EMAIL_NOT_VERIFIED') {
        setVerificationEmail(form.email.trim())
        setMode('verify')
      }
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
          <div className="auth-panel__eyebrow">{isReset ? '계정 보안' : 'MEMBER ACCESS'}</div>
          <h1 id="auth-title" className="auth-panel__title">
            {isForgot
              ? 'RESET ACCESS'
              : isReset
                ? 'NEW PASSWORD'
                : isVerificationPending
                  ? 'CHECK EMAIL'
                  : isRegister
                    ? 'CREATE ACCOUNT'
                    : 'LOGIN'}
          </h1>
          <p className={`auth-panel__description${isReset ? ' auth-panel__description--reset' : ''}`}>
            {isForgot
              ? 'Enter your email and we will send a password reset link.'
              : isReset
                ? 'SA-TRACKER 계정에 사용할 새 비밀번호를 입력해 주세요.'
                : isVerificationPending
                  ? 'Open the verification link in your inbox before signing in.'
                  : isRegister
                    ? 'Sign up with email only. Verify your email before signing in.'
                    : 'Enter with your SA-Tracker account.'}
          </p>

          {!isReset && !isVerificationPending && (
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

          {!isVerificationPending && <form className="auth-form" onSubmit={handleSubmit}>
            {!isReset && (
              <label className="auth-field">
                <span>EMAIL</span>
                <input name="email" type="email" value={form.email} onChange={handleChange}
                  maxLength="254" autoComplete="email" placeholder="you@example.com" required />
              </label>
            )}

            {!isForgot && (
              <label className="auth-field">
                <span>{isReset ? '새 비밀번호' : 'PASSWORD'}</span>
                <input name="password" type="password" value={form.password} onChange={handleChange}
                  minLength="8" maxLength="72" autoComplete={isRegister || isReset ? 'new-password' : 'current-password'}
                  placeholder={isReset ? '8자 이상 입력해 주세요' : '8+ characters'} required />
              </label>
            )}

            {isReset && (
              <label className="auth-field">
                <span>새 비밀번호 확인</span>
                <input name="passwordConfirm" type="password" value={form.passwordConfirm} onChange={handleChange}
                  minLength="8" maxLength="72" autoComplete="new-password"
                  placeholder="새 비밀번호를 한 번 더 입력해 주세요" required />
              </label>
            )}

            <div className="auth-message" role="alert" aria-live="polite">{error}</div>
            {success && <div className="auth-message auth-message--success" role="status">{success}</div>}
            <button className="auth-submit" type="submit" disabled={isSubmitting}>
              {isSubmitting
                ? isReset ? '변경 중...' : 'CONNECTING...'
                : isForgot
                  ? 'SEND RESET LINK'
                  : isReset
                    ? '비밀번호 재설정'
                    : isRegister
                      ? 'CREATE ACCOUNT'
                      : 'ENTER'}
            </button>
          </form>}

          {isVerificationPending && (
            <div className="auth-verification">
              <p className="auth-verification__email">{verificationEmail}</p>
              <div className="auth-message" role="alert" aria-live="polite">{error}</div>
              {success && <div className="auth-message auth-message--success" role="status">{success}</div>}
              <button className="auth-submit" type="button" disabled={isSubmitting}
                onClick={handleVerificationResend}>
                {isSubmitting ? 'SENDING...' : 'RESEND EMAIL'}
              </button>
              <button className="auth-text-button" type="button" onClick={() => changeMode('login')}>
                Back to login
              </button>
            </div>
          )}

          {!isRegister && !isReset && !isVerificationPending && (
            <button className="auth-text-button" type="button" onClick={() => changeMode(isForgot ? 'login' : 'forgot')}>
              {isForgot ? 'Back to login' : 'Forgot password?'}
            </button>
          )}

          <p className="auth-panel__note">
            서든어택 계정 연동은 선택 사항이며, 가입 후 마이페이지에서 진행할 수 있습니다.
          </p>
        </section>
      </main>

      <Footer />
    </div>
  )
}

export default AuthPage
