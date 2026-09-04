import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import * as auth from '../api/auth'
import { getToken } from '../auth/token'
import LoginPage from './LoginPage'

describe('LoginPage', () => {
  it('stores the access token after a successful login', async () => {
    vi.spyOn(auth, 'login').mockResolvedValue({ accessToken: 'at', refreshToken: 'rt' })
    render(<MemoryRouter><LoginPage /></MemoryRouter>)
    await userEvent.type(screen.getByLabelText('이메일'), 'admin@example.com')
    await userEvent.type(screen.getByLabelText('비밀번호'), 'pw')
    await userEvent.click(screen.getByRole('button', { name: '로그인' }))
    expect(auth.login).toHaveBeenCalledWith('admin@example.com', 'pw')
    expect(getToken()).toBe('at')
  })

  it('shows an error when login fails', async () => {
    vi.spyOn(auth, 'login').mockRejectedValue(new Error('401'))
    render(<MemoryRouter><LoginPage /></MemoryRouter>)
    await userEvent.type(screen.getByLabelText('이메일'), 'x@y.z')
    await userEvent.type(screen.getByLabelText('비밀번호'), 'pw')
    await userEvent.click(screen.getByRole('button', { name: '로그인' }))
    expect(await screen.findByText('이메일 또는 비밀번호가 올바르지 않습니다')).toBeInTheDocument()
  })
})
