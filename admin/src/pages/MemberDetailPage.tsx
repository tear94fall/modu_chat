import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getMember, type Member } from '../api/members'
import MemberCard from '../components/MemberCard'
import RemoteImage from '../components/RemoteImage'
import { formatRole } from '../util/format'

export default function MemberDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [member, setMember] = useState<Member | null>(null)
  const [friendCount, setFriendCount] = useState(0)
  const [friends, setFriends] = useState<Member[]>([])
  const [createdDate, setCreatedDate] = useState<string | undefined>(undefined)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getMember(id)
      .then((result) => {
        if (cancelled) return
        setMember(result.member)
        setFriendCount(result.friendCount)
        setFriends(result.friends ?? [])
        setCreatedDate(result.createdDate)
      })
      .catch(() => {
        if (!cancelled) setError('회원 정보를 불러오지 못했습니다')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id])

  if (loading) return <p>불러오는 중...</p>
  if (error) return <p className="error-text">{error}</p>
  if (!member) return <p>회원을 찾을 수 없습니다</p>

  const goToMember = (friendId: number) => navigate(`/members/${friendId}`)

  return (
    <div>
      <Link to="/members" className="back-link">
        ← 회원 목록
      </Link>

      <MemberCard member={member} friendCount={friendCount} createdDate={createdDate} />

      <h2>친구 {friendCount}명</h2>
      {friends.length === 0 ? (
        <p>친구가 없습니다</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th aria-label="프로필" />
              <th>이름</th>
              <th>이메일</th>
              <th>사용자 ID</th>
              <th>권한</th>
            </tr>
          </thead>
          <tbody>
            {friends.map((f) => (
              <tr
                key={f.id}
                className="clickable-row"
                role="button"
                tabIndex={0}
                onClick={() => goToMember(f.id)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault()
                    goToMember(f.id)
                  }
                }}
              >
                <td className="avatar-cell">
                  <RemoteImage filename={f.profileImage} alt={f.username} className="avatar avatar--sm" />
                </td>
                <td>{f.username}</td>
                <td>{f.email}</td>
                <td>{f.userId}</td>
                <td>{formatRole(f.role)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
