import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearImageCache } from '../api/imageCache'
import * as members from '../api/members'
import * as storage from '../api/storage'
import MembersPage from './MembersPage'

describe('MembersPage', () => {
  beforeEach(() => {
    clearImageCache()
  })

  it('renders columns in order (이름, 이메일, 사용자 ID, 권한, 가입일) with formatted createdDate', async () => {
    vi.spyOn(members, 'searchMembers').mockResolvedValue({
      content: [
        {
          id: 1,
          userId: 'u1',
          email: 'a@b.c',
          username: 'Alice',
          role: 'ROLE_MEMBER',
          createdDate: '2026-09-04T12:34:56',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    })

    render(
      <MemoryRouter>
        <MembersPage />
      </MemoryRouter>,
    )

    const headers = await screen.findAllByRole('columnheader')
    expect(headers.map((h) => h.textContent)).toEqual(['', '이름', '이메일', '사용자 ID', '권한', '가입일'])

    expect(await screen.findByText('일반 회원')).toBeInTheDocument()
    expect(await screen.findByText('2026-09-04 12:34')).toBeInTheDocument()
  })

  it('shows the profile image via a blob object URL when profileImage is set', async () => {
    vi.spyOn(members, 'searchMembers').mockResolvedValue({
      content: [
        {
          id: 1,
          userId: 'u1',
          email: 'a@b.c',
          username: 'Alice',
          role: 'ROLE_MEMBER',
          profileImage: 'a.jpg',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    })
    const fetchImageObjectUrl = vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')

    render(
      <MemoryRouter>
        <MembersPage />
      </MemoryRouter>,
    )

    const img = (await screen.findAllByAltText('Alice')).find((el) => el.tagName === 'IMG') as HTMLImageElement
    expect(img).toBeDefined()
    expect(img.src).toBe('blob:fake')
    expect(fetchImageObjectUrl).toHaveBeenCalledWith('a.jpg')
  })

  it('shows the first letter as a placeholder when profileImage is missing', async () => {
    vi.spyOn(members, 'searchMembers').mockResolvedValue({
      content: [
        {
          id: 2,
          userId: 'u2',
          email: 'b@c.d',
          username: 'Bob',
          role: 'ROLE_MEMBER',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    })

    render(
      <MemoryRouter>
        <MembersPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('Bob')).toBeInTheDocument()
    expect(screen.getByText('B')).toBeInTheDocument()
  })
})
