import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from './client'
import { createProduct, deleteProduct, searchProducts, updateProduct, validationMessage } from './products'

const json = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status })

describe('products api', () => {
  afterEach(() => vi.restoreAllMocks())

  it('searches through the gateway admin route with page size 15', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(json({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 15 }))

    await searchProducts('텀블러', 2)

    const [url, init] = fetchMock.mock.calls[0]
    expect(String(url)).toMatch(/\/commerce-service\/api-admin\/v1\/products\?q=%ED%85%80%EB%B8%94%EB%9F%AC&page=2&size=15$/)
    expect(init?.method ?? 'GET').toBe('GET')
  })

  it('creates, updates and deletes with the right methods and JSON bodies', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
    const input = { name: '모두 우산', description: '자동 우산', price: 15000, imageUrl: null }

    fetchMock.mockResolvedValueOnce(json({ id: 7, ...input }, 201))
    await createProduct(input)
    expect(fetchMock.mock.calls[0][1]?.method).toBe('POST')
    expect(JSON.parse(String(fetchMock.mock.calls[0][1]?.body))).toEqual(input)

    fetchMock.mockResolvedValueOnce(json({ id: 7, ...input }))
    await updateProduct('7', input)
    expect(String(fetchMock.mock.calls[1][0])).toMatch(/\/v1\/products\/7$/)
    expect(fetchMock.mock.calls[1][1]?.method).toBe('PUT')

    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }))
    await expect(deleteProduct('7')).resolves.toBeUndefined()
    expect(fetchMock.mock.calls[2][1]?.method).toBe('DELETE')
  })

  it('reads the server message only from a 400 JSON body', () => {
    expect(validationMessage(new ApiError(400, '{"message":"price: 가격은 0 이상이어야 합니다."}'))).toBe(
      'price: 가격은 0 이상이어야 합니다.',
    )
    expect(validationMessage(new ApiError(500, '{"message":"boom"}'))).toBeNull()
    expect(validationMessage(new ApiError(400, 'not json'))).toBeNull()
    expect(validationMessage(new Error('x'))).toBeNull()
  })
})
