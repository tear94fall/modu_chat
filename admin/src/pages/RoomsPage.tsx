import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { PAGE_SIZE } from '../api/client'
import { DEFAULT_ROOM_SORT, listRooms, type RoomSort, type RoomSummary } from '../api/rooms'
import Pager from '../components/Pager'
import RemoteImage from '../components/RemoteImage'
import SortableHeader from '../components/SortableHeader'

export default function RoomsPage() {
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [sort, setSort] = useState<RoomSort>(DEFAULT_ROOM_SORT)
  const [rooms, setRooms] = useState<RoomSummary[]>([])
  /** 서버가 돌려준 페이지 번호(0-based). 순번은 지금 표에 깔린 데이터 기준으로 매긴다. */
  const [pageNumber, setPageNumber] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  /** 정렬이 바뀌면 지금 보던 페이지 번호는 의미가 없다. 다른 방들이 그 자리에 온다. */
  const changeSort = (value: string) => {
    setPage(0)
    setSort(value as RoomSort)
  }

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    listRooms(page, sort)
      .then((result) => {
        if (cancelled) return
        setRooms(result.content)
        setPageNumber(result.number)
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
  }, [page, sort])

  return (
    <div>
      <h1>채팅방 관리</h1>
      {loading && <p>불러오는 중...</p>}
      {error && <p className="error-text">{error}</p>}

      {!loading && !error && (
        <>
          <table className="list-table">
            {/* 열 너비를 비율로 못 박는다. 안 그러면 페이지마다 내용 길이를 따라 열이 들썩인다. */}
            <colgroup>
              <col style={{ width: '6%' }} />
              <col style={{ width: '8%' }} />
              <col style={{ width: '22%' }} />
              <col style={{ width: '8%' }} />
              <col style={{ width: '32%' }} />
              <col style={{ width: '24%' }} />
            </colgroup>
            <thead>
              <tr>
                <th className="num-cell">번호</th>
                <th aria-label="채팅방 사진" />
                <SortableHeader
                  label="채팅방 이름"
                  field="roomName"
                  currentSort={sort}
                  defaultDir="asc"
                  onChange={changeSort}
                />
                <SortableHeader
                  label="멤버 수"
                  field="memberCount"
                  currentSort={sort}
                  defaultDir="desc"
                  onChange={changeSort}
                />
                <SortableHeader
                  label="마지막 메시지"
                  field="lastChatMsg"
                  currentSort={sort}
                  defaultDir="asc"
                  onChange={changeSort}
                />
                <SortableHeader
                  label="마지막 시각"
                  field="lastChatTime"
                  currentSort={sort}
                  defaultDir="desc"
                  onChange={changeSort}
                />
              </tr>
            </thead>
            <tbody>
              {rooms.map((r, i) => (
                <tr key={r.id} onClick={() => navigate(`/rooms/${r.roomId}`)}>
                  <td className="num-cell">{pageNumber * PAGE_SIZE + i + 1}</td>
                  <td className="avatar-cell">
                    <RemoteImage filename={r.roomImage} alt={r.roomName} className="avatar avatar--sm" />
                  </td>
                  {/* 열이 고정폭이라 긴 값은 잘린다. title 로 전체 값을 남겨 둬야 마우스를 올려 읽을 수 있다. */}
                  <td title={r.roomName}>{r.roomName}</td>
                  <td>{r.memberCount}</td>
                  <td title={r.lastChatMsg ?? '-'}>{r.lastChatMsg ?? '-'}</td>
                  <td title={r.lastChatTime ?? '-'}>{r.lastChatTime ?? '-'}</td>
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
