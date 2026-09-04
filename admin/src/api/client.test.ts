import { beforeEach, describe, expect, it, vi } from 'vitest'
import { api, setOnUnauthorized } from './client'
import { clearToken, getToken, setToken } from '../auth/token'

describe('api client', () => {
  beforeEach(() => { clearToken(); vi.restoreAllMocks() })

  it('attaches bearer token and parses json', async () => {
    setToken('abc')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('{"ok":true}', { status: 200 }))
    const body = await api<{ ok: boolean }>('/x')
    expect(body.ok).toBe(true)
    const headers = new Headers(fetchMock.mock.calls[0][1]!.headers)
    expect(headers.get('Authorization')).toBe('Bearer abc')
  })

  it('clears token on 401', async () => {
    setToken('abc')
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('', { status: 401 }))
    setOnUnauthorized(() => {})
    await expect(api('/x')).rejects.toMatchObject({ status: 401 })
    expect(getToken()).toBeNull()
  })
})
