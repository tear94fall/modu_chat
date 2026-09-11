import { afterEach, describe, expect, it, vi } from 'vitest'
import { login, logout } from './auth'
import { setRefreshToken, clearToken } from '../auth/token'

describe('auth api', () => {
  afterEach(() => {
    vi.restoreAllMocks()
    clearToken()
  })

  it('login posts the admin_password grant as a form and maps the OAuth2 response', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ access_token: 'at', refresh_token: 'rt', token_type: 'Bearer' }), { status: 200 }),
    )

    const result = await login('admin@modu.local', 'pw')

    expect(result).toEqual({ accessToken: 'at', refreshToken: 'rt' })
    const [url, init] = fetchMock.mock.calls[0]
    expect(String(url)).toMatch(/\/auth-service\/oauth2\/token$/)
    const headers = new Headers(init?.headers)
    expect(headers.get('Content-Type')).toBe('application/x-www-form-urlencoded')
    const body = new URLSearchParams(String(init?.body))
    expect(body.get('grant_type')).toBe('urn:modu:params:oauth:grant-type:admin_password')
    expect(body.get('client_id')).toBe('modu-admin')
    expect(body.get('email')).toBe('admin@modu.local')
    expect(body.get('password')).toBe('pw')
  })

  it('logout revokes the stored refresh token and does nothing without one', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 200 }))

    await logout()
    expect(fetchMock).not.toHaveBeenCalled()

    setRefreshToken('rt')
    await logout()
    const [url, init] = fetchMock.mock.calls[0]
    expect(String(url)).toMatch(/\/auth-service\/oauth2\/revoke$/)
    const body = new URLSearchParams(String(init?.body))
    expect(body.get('token')).toBe('rt')
    expect(body.get('client_id')).toBe('modu-admin')
  })
})
