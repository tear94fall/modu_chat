import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import * as notices from '../api/notices'
import * as push from '../api/push'
import PushPage from './PushPage'

const emptyPage = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 15 }

const renderPage = () =>
  render(
    <MemoryRouter>
      <PushPage />
    </MemoryRouter>,
  )

describe('PushPage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.spyOn(notices, 'listNotices').mockResolvedValue(emptyPage)
  })

  it('shows a token-not-registered message when sendToUser fails with 404', async () => {
    vi.spyOn(push, 'sendToUser').mockRejectedValue(new ApiError(404, 'not found'))
    renderPage()

    await userEvent.click(screen.getByRole('button', { name: '특정 사용자' }))
    await userEvent.type(screen.getByLabelText('사용자 ID'), 'user-1')
    await userEvent.type(screen.getByLabelText('제목'), '제목')
    await userEvent.type(screen.getByLabelText('내용'), '내용')
    await userEvent.click(screen.getByRole('button', { name: '발송' }))

    expect(
      await screen.findByText('해당 사용자에게 등록된 푸시 토큰이 없습니다. 앱에 로그인하면 토큰이 등록됩니다.'),
    ).toBeInTheDocument()
  })

  it('posts a notice and pushes it by default', async () => {
    const create = vi.spyOn(notices, 'createNotice').mockResolvedValue({ id: 1, title: 'ㅈ', content: 'ㄴ' })
    renderPage()

    await userEvent.type(screen.getByLabelText('제목'), '점검 안내')
    await userEvent.type(screen.getByLabelText('내용'), '오늘 밤 점검이 있습니다')
    await userEvent.click(screen.getByRole('button', { name: '공지 올리기' }))

    expect(create).toHaveBeenCalledWith({ title: '점검 안내', content: '오늘 밤 점검이 있습니다', push: true })
    expect(await screen.findByText('공지를 올리고 푸시를 보냈습니다')).toBeInTheDocument()
  })

  it('can post a notice without pushing', async () => {
    const create = vi.spyOn(notices, 'createNotice').mockResolvedValue({ id: 2, title: 'ㅈ', content: 'ㄴ' })
    renderPage()

    await userEvent.type(screen.getByLabelText('제목'), '조용한 공지')
    await userEvent.type(screen.getByLabelText('내용'), '알림 없이 올립니다')
    await userEvent.click(screen.getByLabelText('올리면서 전체에게 푸시도 보내기'))
    await userEvent.click(screen.getByRole('button', { name: '공지 올리기' }))

    expect(create).toHaveBeenCalledWith({ title: '조용한 공지', content: '알림 없이 올립니다', push: false })
    expect(await screen.findByText('공지를 올렸습니다')).toBeInTheDocument()
  })

  it('lists notices that were already posted', async () => {
    vi.spyOn(notices, 'listNotices').mockResolvedValue({
      ...emptyPage,
      content: [
        { id: 1, title: '점검 안내', content: '오늘 밤 점검', writer: '운영팀 임준섭', createdDate: '2026-09-06T10:00:00' },
      ],
      totalElements: 1,
      totalPages: 1,
    })
    renderPage()

    expect(await screen.findByText('점검 안내')).toBeInTheDocument()
    expect(screen.getByText('오늘 밤 점검')).toBeInTheDocument()
    expect(screen.getByText('운영팀 임준섭')).toBeInTheDocument()
  })

  /** writer 가 없던 시절 글도 목록에서 빈칸으로 보이면 안 된다. */
  it('falls back to 관리자 when a notice has no writer', async () => {
    vi.spyOn(notices, 'listNotices').mockResolvedValue({
      ...emptyPage,
      content: [{ id: 2, title: '옛날 공지', content: '본문', createdDate: '2026-09-01T10:00:00' }],
      totalElements: 1,
      totalPages: 1,
    })
    renderPage()

    expect(await screen.findByText('옛날 공지')).toBeInTheDocument()
    expect(screen.getByText('관리자')).toBeInTheDocument()
  })
})
