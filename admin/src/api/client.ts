import { clearToken, getToken } from '../auth/token'

export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8000').replace(/\/$/, '')

/** 백오피스 목록의 한 페이지 크기. 화면마다 제각각이면 페이지 번호가 어긋나 보여 한 곳에서 정한다. */
export const PAGE_SIZE = 15

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.status = status
  }
}

/** 401 응답을 받았을 때 실행할 동작. 테스트에서 주입해 location.assign 을 우회할 수 있다. */
export let onUnauthorized = () => {
  if (!location.pathname.startsWith('/login')) location.assign('/login')
}

export function setOnUnauthorized(fn: () => void) {
  onUnauthorized = fn
}

/** 게이트웨이 호출. 401 이면 토큰을 지우고 로그인으로 보낸다. */
export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Accept', 'application/json')
  if (init.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)

  const res = await fetch(`${API_BASE_URL}${path}`, { ...init, headers })
  if (res.status === 401) {
    clearToken()
    onUnauthorized()
    throw new ApiError(401, 'unauthorized')
  }
  if (!res.ok) throw new ApiError(res.status, await res.text())
  if (res.status === 204) return undefined as T
  return (await res.json()) as T
}
