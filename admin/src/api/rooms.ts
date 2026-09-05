import { api, PAGE_SIZE } from './client'
import type { Page } from './members'
export interface RoomSummary { id: number; roomId: string; roomName: string; roomImage?: string; memberCount: number; lastChatMsg?: string; lastChatTime?: string }
export interface RoomDetail {
  id: number
  roomId: string
  roomName: string
  roomImage?: string
  lastChatMsg?: string
  lastChatId?: number
  lastChatTime?: string
  members: { id: number; userId: string; email: string; username: string; role?: string; profileImage?: string }[]
}
export interface Chat { id: number; sender: string; message: string; chatTime: string; chatType: number }
export const listRooms = (page: number) => api<Page<RoomSummary>>(`/chat-service/api-admin/chat/rooms?page=${page}&size=${PAGE_SIZE}`)
export const getRoom = (roomId: string) => api<RoomDetail>(`/chat-service/api-admin/chat/rooms/${roomId}`)
export const getRoomChats = (roomId: string, page: number) =>
  api<Page<Chat>>(`/chat-service/api-admin/chat/rooms/${roomId}/chats?page=${page}&size=${PAGE_SIZE}`)
