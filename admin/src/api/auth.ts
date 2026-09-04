import { api } from './client'
export interface TokenResponse { accessToken: string; refreshToken: string }
export const login = (email: string, password: string) =>
  api<TokenResponse>('/auth-service/api-public/admin/login', { method: 'POST', body: JSON.stringify({ email, password }) })

// 게이트웨이의 /auth-service/api-public/** 는 ROLE_USER 필터라 admin 토큰으로는 401 이 돌아온다(호출부에서 catch 로 무시한다).
// 그래도 호출해 두면 나중에 필터를 완화했을 때 refresh token 이 정상적으로 지워진다.
export const logout = () => api<void>('/auth-service/api-public/auth/logout', { method: 'POST' })
