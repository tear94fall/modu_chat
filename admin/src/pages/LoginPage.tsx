import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { setToken } from '../auth/token'

export default function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const { accessToken } = await login(email, password)
      setToken(accessToken)
      navigate('/members')
    } catch {
      setError('이메일 또는 비밀번호가 올바르지 않습니다')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="login-page">
      <form className="form-card login-form" onSubmit={onSubmit}>
        <h1>관리자 로그인</h1>
        <div className="form-section">
          <div className="form-field">
            <label htmlFor="email">이메일</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
            />
          </div>
          <div className="form-field">
            <label htmlFor="password">비밀번호</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>
        </div>
        <div className="form-actions">
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            로그인
          </button>
        </div>
        {error && <p className="error-text">{error}</p>}
      </form>
    </div>
  )
}
