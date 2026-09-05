import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearImageCache } from '../api/imageCache'
import * as rooms from '../api/rooms'
import * as storage from '../api/storage'
import RoomDetailPage from './RoomDetailPage'

const room = {
  id: 1,
  roomId: 'r1',
  roomName: '테스트방',
  lastChatMsg: '이전 대화 요약',
  lastChatTime: '2025-02-08 11:10:33',
  members: [{ id: 11, userId: 'u1', username: '민수', email: 'm@x.y', role: 'ROLE_MEMBER' }],
}

describe('RoomDetailPage', () => {
  beforeEach(() => {
    clearImageCache()
  })

  it('paginates messages via the Pager', async () => {
    vi.spyOn(rooms, 'getRoom').mockResolvedValue(room)
    const getRoomChats = vi.spyOn(rooms, 'getRoomChats').mockImplementation((_roomId, page) =>
      Promise.resolve(
        page === 0
          ? {
              content: [
                { id: 1, sender: 'u1', message: '첫번째 메시지', chatTime: '10:00', chatType: 0 },
                { id: 2, sender: 'u1', message: '두번째 메시지', chatTime: '10:01', chatType: 0 },
              ],
              totalElements: 3,
              totalPages: 2,
              number: 0,
              size: 50,
            }
          : {
              content: [{ id: 3, sender: 'u1', message: '세번째 메시지', chatTime: '10:02', chatType: 0 }],
              totalElements: 3,
              totalPages: 2,
              number: 1,
              size: 50,
            },
      ),
    )

    render(
      <MemoryRouter initialEntries={['/rooms/r1']}>
        <Routes>
          <Route path="/rooms/:roomId" element={<RoomDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByText('첫번째 메시지')).toBeInTheDocument()
    expect(screen.getByText('두번째 메시지')).toBeInTheDocument()

    const messageRow = screen.getByText('첫번째 메시지').closest('tr')
    expect(messageRow).not.toBeNull()
    expect(within(messageRow as HTMLElement).getByText('민수')).toBeInTheDocument()
    expect(within(messageRow as HTMLElement).queryByText('u1')).not.toBeInTheDocument()

    await userEvent.click(screen.getByRole('button', { name: '다음' }))

    expect(await screen.findByText('세번째 메시지')).toBeInTheDocument()
    expect(getRoomChats).toHaveBeenCalledWith('r1', 1)
  })

  it('renders the message table headers in order (메시지, 이름, 보낸 시각)', async () => {
    vi.spyOn(rooms, 'getRoom').mockResolvedValue(room)
    vi.spyOn(rooms, 'getRoomChats').mockResolvedValue({
      content: [{ id: 1, sender: 'u1', message: '첫번째 메시지', chatTime: '10:00', chatType: 0 }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 50,
    })

    render(
      <MemoryRouter initialEntries={['/rooms/r1']}>
        <Routes>
          <Route path="/rooms/:roomId" element={<RoomDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    const messageHeading = await screen.findByRole('heading', { name: '메시지' })
    const messageSection = messageHeading.closest('div') as HTMLElement
    const headers = within(messageSection).getAllByRole('columnheader')
    expect(headers.map((h) => h.textContent)).toEqual(['메시지', '이름', '보낸 시각'])
  })

  it('shows the room info card with room name and member count', async () => {
    vi.spyOn(rooms, 'getRoom').mockResolvedValue(room)
    vi.spyOn(rooms, 'getRoomChats').mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
    })

    render(
      <MemoryRouter initialEntries={['/rooms/r1']}>
        <Routes>
          <Route path="/rooms/:roomId" element={<RoomDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '테스트방' })).toBeInTheDocument()
    expect(screen.getByText('1')).toBeInTheDocument()
    expect(screen.getByText('일반 회원')).toBeInTheDocument()
  })

  it('shows the room-name fallback letter when roomImage is empty', async () => {
    vi.spyOn(rooms, 'getRoom').mockResolvedValue(room)
    vi.spyOn(rooms, 'getRoomChats').mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
    })

    render(
      <MemoryRouter initialEntries={['/rooms/r1']}>
        <Routes>
          <Route path="/rooms/:roomId" element={<RoomDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { name: '테스트방' })).toBeInTheDocument()
    expect(screen.getByText('테')).toBeInTheDocument()
  })

  it('shows a member avatar image via a blob object URL when profileImage is set', async () => {
    vi.spyOn(rooms, 'getRoom').mockResolvedValue({
      ...room,
      members: [{ ...room.members[0], profileImage: 'a.jpg' }],
    })
    vi.spyOn(rooms, 'getRoomChats').mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
    })
    const fetchImageObjectUrl = vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')

    render(
      <MemoryRouter initialEntries={['/rooms/r1']}>
        <Routes>
          <Route path="/rooms/:roomId" element={<RoomDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )

    const img = (await screen.findAllByAltText('민수')).find((el) => el.tagName === 'IMG') as HTMLImageElement
    expect(img).toBeDefined()
    expect(img.src).toBe('blob:fake')
    expect(fetchImageObjectUrl).toHaveBeenCalledWith('a.jpg')
  })

  it('navigates to the member detail page when a member row is clicked', async () => {
    vi.spyOn(rooms, 'getRoom').mockResolvedValue(room)
    vi.spyOn(rooms, 'getRoomChats').mockResolvedValue({
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
    })

    render(
      <MemoryRouter initialEntries={['/rooms/r1']}>
        <Routes>
          <Route path="/rooms/:roomId" element={<RoomDetailPage />} />
          <Route path="/members/:id" element={<div>회원 상세 스텁</div>} />
        </Routes>
      </MemoryRouter>,
    )

    const memberRow = (await screen.findByText('민수')).closest('tr')
    expect(memberRow).not.toBeNull()

    await userEvent.click(memberRow as HTMLElement)

    expect(await screen.findByText('회원 상세 스텁')).toBeInTheDocument()
  })
})
