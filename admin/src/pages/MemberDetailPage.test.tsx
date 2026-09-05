import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearImageCache } from '../api/imageCache'
import * as members from '../api/members'
import * as storage from '../api/storage'
import MemberDetailPage from './MemberDetailPage'

const renderPage = (id = '1') =>
  render(
    <MemoryRouter initialEntries={[`/members/${id}`]}>
      <Routes>
        <Route path="/members/:id" element={<MemberDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )

describe('MemberDetailPage', () => {
  beforeEach(() => {
    clearImageCache()
  })

  it('shows the profile image via a blob object URL when profileImage is set', async () => {
    vi.spyOn(members, 'getMember').mockResolvedValue({
      member: {
        id: 1,
        userId: 'u1',
        email: 'a@b.c',
        username: '민수',
        role: 'ROLE_MEMBER',
        profileImage: 'a.jpg',
        createdDate: '2026-09-04T12:34:56',
      },
      friendCount: 3,
      friends: [],
    })
    const fetchImageObjectUrl = vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')

    renderPage()

    expect(await screen.findByText('민수')).toBeInTheDocument()
    expect(screen.getByText('일반 회원')).toBeInTheDocument()

    const img = (await screen.findAllByAltText(/민수/)).find((el) => el.tagName === 'IMG') as HTMLImageElement
    expect(img).toBeDefined()
    expect(img.src).toBe('blob:fake')
    expect(fetchImageObjectUrl).toHaveBeenCalledWith('a.jpg')
  })

  it('shows the first letter as a placeholder when profileImage is missing, without calling fetchImageObjectUrl', async () => {
    vi.spyOn(members, 'getMember').mockResolvedValue({
      member: {
        id: 2,
        userId: 'u2',
        email: 'b@c.d',
        username: '민수',
        role: 'ROLE_ADMIN',
      },
      friendCount: 0,
      friends: [],
    })
    const fetchImageObjectUrl = vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')

    renderPage('2')

    expect(await screen.findByText('민수')).toBeInTheDocument()
    expect(screen.getByText('관리자')).toBeInTheDocument()
    expect(screen.getByText('민')).toBeInTheDocument()
    expect(fetchImageObjectUrl).not.toHaveBeenCalled()
  })

  it('renders the friend list and lets you open a friend', async () => {
    vi.spyOn(members, 'getMember').mockResolvedValue({
      member: { id: 1, userId: 'u1', email: 'a@b.c', username: '민수', role: 'ROLE_MEMBER' },
      friendCount: 2,
      friends: [
        { id: 50, userId: 'demo-jiwoo', email: 'jiwoo@modu.chat', username: '김지우', role: 'ROLE_MEMBER' },
        { id: 51, userId: 'demo-minjun', email: 'minjun@modu.chat', username: '박민준', role: 'ROLE_MEMBER' },
      ],
    })

    renderPage()

    expect(await screen.findByText('친구 2명')).toBeInTheDocument()
    expect(screen.getByText('김지우')).toBeInTheDocument()
    expect(screen.getByText('박민준')).toBeInTheDocument()
    expect(screen.getByText('demo-jiwoo')).toBeInTheDocument()
  })

  it('says so when the member has no friends', async () => {
    vi.spyOn(members, 'getMember').mockResolvedValue({
      member: { id: 1, userId: 'u1', email: 'a@b.c', username: '민수', role: 'ROLE_MEMBER' },
      friendCount: 0,
      friends: [],
    })

    renderPage()

    expect(await screen.findByText('친구 0명')).toBeInTheDocument()
    expect(screen.getByText('친구가 없습니다')).toBeInTheDocument()
  })


  it('places the friend list beside the member card, not under it', async () => {
    vi.spyOn(members, 'getMember').mockResolvedValue({
      member: { id: 1, userId: 'u1', email: 'a@b.c', username: '민수', role: 'ROLE_MEMBER' },
      friendCount: 1,
      friends: [
        { id: 50, userId: 'demo-jiwoo', email: 'jiwoo@modu.chat', username: '김지우', role: 'ROLE_MEMBER' },
      ],
    })

    const { container } = renderPage()
    expect(await screen.findByText('친구 1명')).toBeInTheDocument()

    // 카드와 친구 목록이 같은 2단 컨테이너의 형제로 들어가야 옆에 나란히 놓인다.
    const columns = container.querySelector('.member-columns')
    expect(columns).toBeInTheDocument()
    expect(columns?.querySelector('.profile-card')).toBeInTheDocument()
    expect(columns?.querySelector('table')).toBeInTheDocument()
    expect(columns?.children.length).toBe(2)
  })

})
