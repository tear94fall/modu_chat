import { type FormEvent, useCallback, useEffect, useState } from 'react'
import { ApiError } from '../api/client'
import { createNotice, listNotices, type Notice } from '../api/notices'
import { sendToUser } from '../api/push'
import Pager from '../components/Pager'
import { formatDateTime } from '../util/format'

type Tab = 'notice' | 'user'

export default function PushPage() {
  const [tab, setTab] = useState<Tab>('notice')
  const [userId, setUserId] = useState('')
  const [title, setTitle] = useState('')
  const [body, setBody] = useState('')
  const [image, setImage] = useState('')
  const [push, setPush] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [notices, setNotices] = useState<Notice[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [listError, setListError] = useState<string | null>(null)

  const loadNotices = useCallback(() => {
    listNotices(page)
      .then((result) => {
        setNotices(result.content)
        setTotalPages(result.totalPages)
        setListError(null)
      })
      .catch(() => setListError('공지 목록을 불러오지 못했습니다'))
  }, [page])

  useEffect(() => {
    if (tab === 'notice') loadNotices()
  }, [tab, loadNotices])

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setResult(null)
    setError(null)
    setSubmitting(true)
    try {
      if (tab === 'notice') {
        await createNotice({ title, content: body, push })
        setResult(push ? '공지를 올리고 푸시를 보냈습니다' : '공지를 올렸습니다')
        setTitle('')
        setBody('')
        setPage(0)
        loadNotices()
      } else {
        await sendToUser(userId, { title, body, image: image || undefined })
        setResult('발송 완료')
      }
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setError('해당 사용자에게 등록된 푸시 토큰이 없습니다. 앱에 로그인하면 토큰이 등록됩니다.')
      } else {
        setError(tab === 'notice' ? '공지를 올리지 못했습니다' : '발송에 실패했습니다')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div>
      <h1>공지 · 푸시</h1>
      <div className="tabs">
        <button
          type="button"
          className={tab === 'notice' ? 'btn btn--ghost tab active' : 'btn btn--ghost tab'}
          onClick={() => setTab('notice')}
        >
          공지사항
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
          {tab === 'user' && (
            <div className="form-field">
              <label htmlFor="image">이미지 URL</label>
              <input id="image" value={image} onChange={(e) => setImage(e.target.value)} />
            </div>
          )}
          {tab === 'notice' && (
            <label className="form-check" htmlFor="push">
              <input id="push" type="checkbox" checked={push} onChange={(e) => setPush(e.target.checked)} />
              올리면서 전체에게 푸시도 보내기
            </label>
          )}
        </div>
        <div className="form-actions">
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            {tab === 'notice' ? '공지 올리기' : '발송'}
          </button>
        </div>
      </form>

      {result && <p className="result-text">{result}</p>}
      {error && <p className="error-text">{error}</p>}

      {tab === 'notice' && (
        <>
          <h2>올린 공지</h2>
          {listError && <p className="error-text">{listError}</p>}
          {!listError && notices.length === 0 && <p>올린 공지가 없습니다</p>}
          {!listError && notices.length > 0 && (
            <>
              <table>
                <thead>
                  <tr>
                    <th>제목</th>
                    <th>내용</th>
                    <th>올린 사람</th>
                    <th>올린 시각</th>
                  </tr>
                </thead>
                <tbody>
                  {notices.map((n) => (
                    <tr key={n.id}>
                      <td>{n.title}</td>
                      <td>{n.content}</td>
                      <td>{n.writer ?? '관리자'}</td>
                      <td>{formatDateTime(n.createdDate)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Pager page={page} totalPages={totalPages} onChange={setPage} />
            </>
          )}
        </>
      )}
    </div>
  )
}
