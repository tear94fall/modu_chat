import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import * as push from '../api/push'
import PushPage from './PushPage'

describe('PushPage', () => {
  it('shows a token-not-registered message when sendToUser fails with 404', async () => {
    vi.spyOn(push, 'sendToUser').mockRejectedValue(new ApiError(404, 'not found'))
    render(
      <MemoryRouter>
        <PushPage />
      </MemoryRouter>,
    )
    await userEvent.click(screen.getByRole('button', { name: '특정 사용자' }))
    await userEvent.type(screen.getByLabelText('사용자 ID'), 'user-1')
    await userEvent.type(screen.getByLabelText('제목'), '제목')
    await userEvent.type(screen.getByLabelText('내용'), '내용')
    await userEvent.click(screen.getByRole('button', { name: '발송' }))
    expect(
      await screen.findByText('해당 사용자에게 등록된 푸시 토큰이 없습니다. 앱에 로그인하면 토큰이 등록됩니다.'),
    ).toBeInTheDocument()
  })
})
