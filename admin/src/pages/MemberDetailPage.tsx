import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getMember, type Member } from '../api/members'
import MemberCard from '../components/MemberCard'

export default function MemberDetailPage() {
  const { id } = useParams<{ id: string }>()
  const [member, setMember] = useState<Member | null>(null)
  const [friendCount, setFriendCount] = useState(0)
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

  return (
    <div>
      <Link to="/members" className="back-link">
        ← 회원 목록
      </Link>

      <MemberCard member={member} friendCount={friendCount} createdDate={createdDate} />
    </div>
  )
}
