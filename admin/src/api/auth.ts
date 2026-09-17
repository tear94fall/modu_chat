import { api } from './client'
import { getRefreshToken } from '../auth/token'

export interface TokenResponse { accessToken: string; refreshToken: string }

/** auth-service OAuth2 토큰 엔드포인트 응답(스네이크 케이스) */
interface OAuthTokenResponse { access_token: string; refresh_token?: string }

const CLIENT_ID = 'modu-admin'
const ADMIN_PASSWORD_GRANT = 'urn:modu:params:oauth:grant-type:admin_password'
const FORM = { 'Content-Type': 'application/x-www-form-urlencoded' }

/** 관리자 비밀번호 로그인. 토큰은 auth-service 의 표준 /oauth2/token 이 발급한다(aud=modu-admin). */
export const login = async (email: string, password: string): Promise<TokenResponse> => {
  const body = new URLSearchParams({ grant_type: ADMIN_PASSWORD_GRANT, client_id: CLIENT_ID, email, password })
  const res = await api<OAuthTokenResponse>('/auth-service/oauth2/token', { method: 'POST', headers: FORM, body: body.toString() })
  return { accessToken: res.access_token, refreshToken: res.refresh_token ?? '' }
}

/** refresh 토큰을 폐기한다. 이미 없거나 실패해도 호출부는 무시하고 로컬 토큰을 지운다. */
export const logout = () => {
  const refresh = getRefreshToken()
  if (!refresh) return Promise.resolve()
  const body = new URLSearchParams({ token: refresh, client_id: CLIENT_ID })
  return api<void>('/auth-service/oauth2/revoke', { method: 'POST', headers: FORM, body: body.toString() })
}
