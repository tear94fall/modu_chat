import { api } from './client'
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
}
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface MemberDetail { member: Member; friendCount: number; createdDate?: string }
export const searchMembers = (keyword: string, page: number) =>
  api<Page<Member>>(`/member-service/api-admin/member?keyword=${encodeURIComponent(keyword)}&page=${page}&size=20`)
export const getMember = (id: string) => api<MemberDetail>(`/member-service/api-admin/member/${id}`)
export const getMe = () => api<MemberDetail>('/member-service/api-admin/member/me')
export const updateMe = (body: { username?: string; statusMessage?: string; profileImage?: string; wallpaperImage?: string }) =>
  api<MemberDetail>('/member-service/api-admin/member/me', { method: 'PUT', body: JSON.stringify(body) })
