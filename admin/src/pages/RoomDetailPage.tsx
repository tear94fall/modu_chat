import { useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { getRoom, getRoomChats, type Chat, type RoomDetail } from '../api/rooms'
import type { Page } from '../api/members'
import Pager from '../components/Pager'
import RemoteImage from '../components/RemoteImage'
import { formatDateTime, formatRole } from '../util/format'

export default function RoomDetailPage() {
  const { roomId } = useParams<{ roomId: string }>()
  const navigate = useNavigate()
  const [room, setRoom] = useState<RoomDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [page, setPage] = useState(0)
  const [chats, setChats] = useState<Page<Chat> | null>(null)
  const [chatsError, setChatsError] = useState<string | null>(null)

  useEffect(() => {
    if (!roomId) return
    let cancelled = false
    setLoading(true)
    setError(null)
    getRoom(roomId)
      .then((result) => {
        if (cancelled) return
        setRoom(result)
      })
      .catch(() => {
        if (!cancelled) setError('채팅방 정보를 불러오지 못했습니다')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [roomId])

  useEffect(() => {
    if (!roomId) return
    let cancelled = false
    setChatsError(null)
    getRoomChats(roomId, page)
      .then((result) => {
        if (cancelled) return
        setChats(result)
      })
      .catch(() => {
        if (!cancelled) setChatsError('메시지를 불러오지 못했습니다')
      })
    return () => {
      cancelled = true
    }
  }, [roomId, page])

  const usernameByUserId = useMemo(() => {
    const map = new Map<string, string>()
    room?.members.forEach((m) => map.set(m.userId, m.username))
    return map
  }, [room])

  if (loading) return <p>불러오는 중...</p>
  if (error) return <p className="error-text">{error}</p>
  if (!room) return <p>채팅방을 찾을 수 없습니다</p>

  const goToMember = (memberId: number) => navigate(`/members/${memberId}`)

  return (
    <div>
      <div className="info-card">
        <div className="room-header">
          <RemoteImage filename={room.roomImage} alt={room.roomName} className="avatar avatar--md" />
          <h1>{room.roomName}</h1>
        </div>
        <dl className="detail-grid">
          <dt>채팅방 ID</dt>
          <dd>{room.roomId}</dd>
          <dt>멤버 수</dt>
          <dd>{room.members.length}</dd>
          <dt>마지막 메시지</dt>
          <dd className="ellipsis">{room.lastChatMsg ?? '-'}</dd>
          <dt>마지막 시각</dt>
          <dd>{formatDateTime(room.lastChatTime)}</dd>
        </dl>
      </div>

      <div className="room-columns">
        <div>
          <h2>멤버</h2>
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
              {room.members.map((m) => (
                <tr
                  key={m.userId}
                  className="clickable-row"
                  role="button"
                  tabIndex={0}
                  onClick={() => goToMember(m.id)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      goToMember(m.id)
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
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div>
          <h2>메시지</h2>
          {chatsError && <p className="error-text">{chatsError}</p>}
          {!chatsError && chats && chats.totalElements === 0 && <p>메시지가 없습니다</p>}
          {!chatsError && chats && chats.totalElements > 0 && (
            <>
              <table className="chat-table">
                <colgroup>
                  <col />
                  <col style={{ width: '120px' }} />
                  <col style={{ width: '140px' }} />
                </colgroup>
                <thead>
                  <tr>
                    <th>메시지</th>
                    <th>이름</th>
                    <th>보낸 시각</th>
                  </tr>
                </thead>
                <tbody>
                  {chats.content.map((c) => (
                    <tr key={c.id}>
                      <td className="message-cell">{c.message}</td>
                      <td>{usernameByUserId.get(c.sender) ?? c.sender}</td>
                      <td className="nowrap-cell">{formatDateTime(c.chatTime)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <Pager page={page} totalPages={chats.totalPages} onChange={setPage} />
            </>
          )}
        </div>
      </div>
    </div>
  )
}
