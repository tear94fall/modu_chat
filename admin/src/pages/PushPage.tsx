import { type FormEvent, useState } from 'react'
import { ApiError } from '../api/client'
import { broadcast, sendToUser } from '../api/push'

type Tab = 'all' | 'user'

export default function PushPage() {
  const [tab, setTab] = useState<Tab>('all')
  const [userId, setUserId] = useState('')
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [image, setImage] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setResult(null)
    setError(null)
    setSubmitting(true)
    const message = { title, body, image: image || undefined }
    try {
      if (tab === 'all') {
        const { groups } = await broadcast(message)
        setResult(`${groups} 그룹 발송`)
      } else {
        await sendToUser(userId, message)
        setResult('발송 완료')
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setError('해당 사용자에게 등록된 푸시 토큰이 없습니다. 앱에 로그인하면 토큰이 등록됩니다.')
      } else {
        setError('발송에 실패했습니다')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      <h1>푸시 발송</h1>
      <div className="tabs">
        <button
          type="button"
          className={tab === 'all' ? 'btn btn--ghost tab active' : 'btn btn--ghost tab'}
          onClick={() => setTab('all')}
        >
          전체
        </button>
        <button
          type="button"
          className={tab === 'user' ? 'btn btn--ghost tab active' : 'btn btn--ghost tab'}
          onClick={() => setTab('user')}
        >
          특정 사용자
        </button>
      </div>

      <form className="form-card push-form" onSubmit={onSubmit}>
        <div className="form-section">
          {tab === 'user' && (
            <div className="form-field">
              <label htmlFor="userId">사용자 ID</label>
              <input id="userId" value={userId} onChange={(e) => setUserId(e.target.value)} required />
            </div>
          )}
          <div className="form-field">
            <label htmlFor="title">제목</label>
            <input id="title" value={title} onChange={(e) => setTitle(e.target.value)} required />
          </div>
          <div className="form-field">
            <label htmlFor="body">내용</label>
            <input id="body" value={body} onChange={(e) => setBody(e.target.value)} required />
          </div>
          <div className="form-field">
            <label htmlFor="image">이미지 URL</label>
            <input id="image" value={image} onChange={(e) => setImage(e.target.value)} />
          </div>
        </div>
        <div className="form-actions">
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            발송
          </button>
        </div>
      </form>

      {result && <p className="result-text">{result}</p>}
      {error && <p className="error-text">{error}</p>}
    </div>
  )
}
