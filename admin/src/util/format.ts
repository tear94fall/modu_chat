export function formatDateTime(iso?: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 백오피스 표시용. 서버는 ROLE_ADMIN / ROLE_MEMBER 를 그대로 쓴다. */
export function formatRole(role?: string): string {
  if (role === 'ROLE_ADMIN') return '관리자'
  if (role === 'ROLE_MEMBER') return '일반 회원'
  return role ?? ''
}
