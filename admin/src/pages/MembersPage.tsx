import { type FormEvent, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { searchMembers, type Member } from '../api/members'
import Pager from '../components/Pager'
import RemoteImage from '../components/RemoteImage'
import { formatDateTime, formatRole } from '../util/format'

export default function MembersPage() {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [page, setPage] = useState(0)
  const [members, setMembers] = useState<Member[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    searchMembers(searchTerm, page)
      .then((result) => {
        if (cancelled) return
        setMembers(result.content)
        setTotalPages(result.totalPages)
      })
      .catch(() => {
        if (!cancelled) setError('회원 목록을 불러오지 못했습니다')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [searchTerm, page])

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    setPage(0)
    setSearchTerm(keyword)
  }

  return (
    <div>
      <h1>회원 관리</h1>
      <form className="search-form" onSubmit={onSearch}>
        <input
          type="text"
          placeholder="이메일/아이디/이름 검색"
          value={keyword}
          onChange={(e) => setKeyword(e.target.value)}
        />
        <button type="submit" className="btn btn--primary">
          검색
        </button>
      </form>

      {loading && <p>불러오는 중...</p>}
      {error && <p className="error-text">{error}</p>}

      {!loading && !error && (
        <>
          <table>
            <thead>
              <tr>
                <th aria-label="프로필" />
                <th>이름</th>
                <th>이메일</th>
                <th>사용자 ID</th>
                <th>권한</th>
                <th>가입일</th>
              </tr>
            </thead>
            <tbody>
              {members.map((m) => (
                <tr
                  key={m.id}
                  className="clickable-row"
                  role="button"
                  tabIndex={0}
                  onClick={() => navigate(`/members/${m.id}`)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      navigate(`/members/${m.id}`)
                    }
                  }}
                >
                  <td className="avatar-cell">
                    <RemoteImage filename={m.profileImage} alt={m.username} className="avatar avatar--sm" />
                  </td>
                  <td>{m.username}</td>
                  <td>{m.email}</td>
                  <td>{m.userId}</td>
                  <td>{formatRole(m.role)}</td>
                  <td>{formatDateTime(m.createdDate)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pager page={page} totalPages={totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
