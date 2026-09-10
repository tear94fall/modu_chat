import { api, PAGE_SIZE } from './client'
export interface Member {
  id: number
  userId: string
  email: string
  username: string
  role: string
  statusMessage?: string
  profileImage?: string
  wallpaperImage?: string
  createdDate?: string
  /** 회원 상세의 친구 목록에서만 온다: 그 회원이 이 친구에게 정한 이름 */
  friendName?: string
}
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface MemberDetail { member: Member; friendCount: number; createdDate?: string; friends: Member[] }
export const searchMembers = (keyword: string, page: number) =>
  api<Page<Member>>(`/member-service/api-admin/member?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${PAGE_SIZE}`)
export const getMember = (id: string) => api<MemberDetail>(`/member-service/api-admin/member/${id}`)
export const getMe = () => api<MemberDetail>('/member-service/api-admin/member/me')
export const updateMe = (body: { username?: string; statusMessage?: string; profileImage?: string; wallpaperImage?: string }) =>
  api<MemberDetail>('/member-service/api-admin/member/me', { method: 'PUT', body: JSON.stringify(body) })
