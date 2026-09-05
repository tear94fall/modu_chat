import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearImageCache } from '../api/imageCache'
import * as members from '../api/members'
import * as storage from '../api/storage'
import MePage from './MePage'

describe('MePage', () => {
  beforeEach(() => {
    clearImageCache()
  })

  it('shows the current admin member info', async () => {
    vi.spyOn(members, 'getMe').mockResolvedValue({
      member: {
        id: 1,
        userId: 'u1',
        email: 'admin@b.c',
        username: '관리자',
        role: 'ROLE_ADMIN',
      },
      friendCount: 0,
      friends: [],
    })
    vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')

    render(
      <MemoryRouter>
        <MePage />
      </MemoryRouter>,
    )

    expect(await screen.findByRole('heading', { level: 1, name: '내 정보' })).toBeInTheDocument()
    expect((await screen.findAllByText('관리자')).length).toBeGreaterThanOrEqual(2)
  })

  it('수정 후 이름만 바꾸면 이미지 파일명은 그대로 저장된다', async () => {
    vi.spyOn(members, 'getMe').mockResolvedValue({
      member: {
        id: 1,
        userId: 'u1',
        email: 'admin@b.c',
        username: '관리자',
        role: 'ROLE_ADMIN',
        statusMessage: '기존 상태',
        profileImage: 'existing-profile.jpg',
        wallpaperImage: 'existing-wallpaper.jpg',
      },
      friendCount: 0,
      friends: [],
    })
    vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')
    const updateMe = vi.spyOn(members, 'updateMe').mockResolvedValue({
      member: {
        id: 1,
        userId: 'u1',
        email: 'admin@b.c',
        username: '새이름',
        role: 'ROLE_ADMIN',
        statusMessage: '기존 상태',
        profileImage: 'existing-profile.jpg',
        wallpaperImage: 'existing-wallpaper.jpg',
      },
      friendCount: 0,
      friends: [],
    })

    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <MePage />
      </MemoryRouter>,
    )

    await screen.findByRole('heading', { level: 1, name: '관리자' })
    await user.click(screen.getByRole('button', { name: '수정' }))

    const nameInput = screen.getByLabelText('이름')
    await user.clear(nameInput)
    await user.type(nameInput, '새이름')

    await user.click(screen.getByRole('button', { name: '저장' }))

    await waitFor(() =>
      expect(updateMe).toHaveBeenCalledWith({
        username: '새이름',
        statusMessage: '기존 상태',
        profileImage: 'existing-profile.jpg',
        wallpaperImage: 'existing-wallpaper.jpg',
      }),
    )

    expect(await screen.findByText('새이름')).toBeInTheDocument()
  })

  it('5MB 넘는 파일을 고르면 업로드하지 않고 안내 문구를 보여준다', async () => {
    vi.spyOn(members, 'getMe').mockResolvedValue({
      member: {
        id: 1,
        userId: 'u1',
        email: 'admin@b.c',
        username: '관리자',
        role: 'ROLE_ADMIN',
      },
      friendCount: 0,
      friends: [],
    })
    vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')
    const uploadImage = vi.spyOn(storage, 'uploadImage')

    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <MePage />
      </MemoryRouter>,
    )

    await screen.findByRole('heading', { level: 1, name: '관리자' })
    await user.click(screen.getByRole('button', { name: '수정' }))

    const bigFile = new File([new ArrayBuffer(6 * 1024 * 1024)], 'big.png', { type: 'image/png' })
    await user.upload(screen.getByLabelText('프로필 이미지'), bigFile)

    expect(await screen.findByText('이미지 파일만 5MB 이하로 올릴 수 있습니다')).toBeInTheDocument()
    expect(uploadImage).not.toHaveBeenCalled()
  })

  it('유효한 이미지를 고르면 업로드하고, 저장 시 반환된 파일명을 보낸다', async () => {
    vi.spyOn(members, 'getMe').mockResolvedValue({
      member: {
        id: 1,
        userId: 'u1',
        email: 'admin@b.c',
        username: '관리자',
        role: 'ROLE_ADMIN',
        statusMessage: '',
        profileImage: 'old.jpg',
        wallpaperImage: '',
      },
      friendCount: 0,
      friends: [],
    })
    vi.spyOn(storage, 'fetchImageObjectUrl').mockResolvedValue('blob:fake')
    const uploadImage = vi.spyOn(storage, 'uploadImage').mockResolvedValue('new-profile.jpg')
    const updateMe = vi.spyOn(members, 'updateMe').mockResolvedValue({
      member: {
        id: 1,
        userId: 'u1',
        email: 'admin@b.c',
        username: '관리자',
        role: 'ROLE_ADMIN',
        statusMessage: '',
        profileImage: 'new-profile.jpg',
        wallpaperImage: '',
      },
      friendCount: 0,
      friends: [],
    })

    const user = userEvent.setup()
    render(
      <MemoryRouter>
        <MePage />
      </MemoryRouter>,
    )

    await screen.findByRole('heading', { level: 1, name: '관리자' })
    await user.click(screen.getByRole('button', { name: '수정' }))

    const goodFile = new File(['x'], 'good.png', { type: 'image/png' })
    await user.upload(screen.getByLabelText('프로필 이미지'), goodFile)

    await waitFor(() => expect(uploadImage).toHaveBeenCalledWith(goodFile))

    await user.click(screen.getByRole('button', { name: '저장' }))

    await waitFor(() =>
      expect(updateMe).toHaveBeenCalledWith(expect.objectContaining({ profileImage: 'new-profile.jpg' })),
    )
  })
})
