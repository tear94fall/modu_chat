import { ApiError, api, PAGE_SIZE } from './client'
import type { Page } from './members'

/** commerce-service 상품. 앱과 같은 응답 모양이다. */
export interface Product {
  id: number
  name: string
  description: string
  price: number
  imageUrl: string | null
}

/** 등록·수정 본문. 이름 길이·가격 범위·이미지 주소 형식은 서버가 검증하고 400 으로 이유를 준다. */
export interface ProductInput {
  name: string
  description: string
  price: number
  imageUrl: string | null
}

const BASE = '/commerce-service/api-admin/v1/products'

export const searchProducts = (keyword: string, page: number) =>
  api<Page<Product>>(`${BASE}?q=${encodeURIComponent(keyword)}&page=${page}&size=${PAGE_SIZE}`)

export const getProduct = (id: string) => api<Product>(`${BASE}/${encodeURIComponent(id)}`)

export const createProduct = (input: ProductInput) => api<Product>(BASE, { method: 'POST', body: JSON.stringify(input) })

export const updateProduct = (id: string, input: ProductInput) =>
  api<Product>(`${BASE}/${encodeURIComponent(id)}`, { method: 'PUT', body: JSON.stringify(input) })

export const deleteProduct = (id: string) => api<void>(`${BASE}/${encodeURIComponent(id)}`, { method: 'DELETE' })

/** 400 응답 본문 {"message": "price: ..."} 에서 사람이 읽을 이유를 꺼낸다. 그 밖의 오류는 null. */
export function validationMessage(err: unknown): string | null {
  if (!(err instanceof ApiError) || err.status !== 400) return null
  try {
    const body = JSON.parse(err.message) as { message?: unknown }
    return typeof body.message === 'string' ? body.message : null
  } catch {
    return null
  }
}
