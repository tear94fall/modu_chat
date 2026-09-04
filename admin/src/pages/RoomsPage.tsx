import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { listRooms, type RoomSummary } from '../api/rooms'
import Pager from '../components/Pager'
import RemoteImage from '../components/RemoteImage'

export default function RoomsPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [rooms, setRooms] = useState<RoomSummary[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    listRooms(page)
      .then((result) => {
        if (cancelled) return
        setRooms(result.content)
        setTotalPages(result.totalPages)
      })
      .catch(() => {
        if (!cancelled) setError('채팅방 목록을 불러오지 못했습니다')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [page])

  return (
    <div>
      <h1>채팅방 관리</h1>

      {loading && <p>불러오는 중...</p>}
      {error && <p className="error-text">{error}</p>}

      {!loading && !error && (
        <>
          <table>
            <thead>
              <tr>
                <th aria-label="채팅방 사진" />
                <th>채팅방 이름</th>
                <th>멤버 수</th>
                <th>마지막 메시지</th>
                <th>마지막 시각</th>
              </tr>
            </thead>
            <tbody>
              {rooms.map((r) => (
                <tr key={r.id} onClick={() => navigate(`/rooms/${r.roomId}`)}>
                  <td className="avatar-cell">
                    <RemoteImage filename={r.roomImage} alt={r.roomName} className="avatar avatar--sm" />
                  </td>
                  <td>{r.roomName}</td>
                  <td>{r.memberCount}</td>
                  <td>{r.lastChatMsg ?? '-'}</td>
                  <td>{r.lastChatTime ?? '-'}</td>
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
