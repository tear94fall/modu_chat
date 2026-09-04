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
    })
    const fetchImageObjectUrl = vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')

    renderPage('2')

    expect(await screen.findByText('민수')).toBeInTheDocument()
    expect(screen.getByText('관리자')).toBeInTheDocument()
    expect(screen.getByText('민')).toBeInTheDocument()
    expect(fetchImageObjectUrl).not.toHaveBeenCalled()
  })
})
