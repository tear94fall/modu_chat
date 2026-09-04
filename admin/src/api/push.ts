import { api } from './client'
export interface PushMessage { title: string; body: string; image?: string }
export const broadcast = (m: PushMessage) => api<{ groups: number }>('/push-service/api-admin/push/broadcast', { method: 'POST', body: JSON.stringify(m) })
export const sendToUser = (userId: string, m: PushMessage) => api<void>(`/push-service/api-admin/push/users/${encodeURIComponent(userId)}`, { method: 'POST', body: JSON.stringify(m) })
