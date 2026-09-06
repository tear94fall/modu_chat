import { api, PAGE_SIZE } from './client'
import type { Page } from './members'

export interface Notice {
  id: number
  title: string
  content: string
  /** 서버가 게이트웨이 헤더의 관리자 userId 로 찾아 넣는다. 본문으로 보내는 값이 아니다. */
  writer?: string
  createdDate?: string
}

export const listNotices = (page: number) =>
  api<Page<Notice>>(`/member-service/api-admin/notice?page=${page}&size=${PAGE_SIZE}`)

/** 저장과 전체 푸시를 서버가 한 번에 처리한다. push=false 면 저장만 한다. */
export const createNotice = (body: { title: string; content: string; push: boolean }) =>
  api<Notice>('/member-service/api-admin/notice', { method: 'POST', body: JSON.stringify(body) })
