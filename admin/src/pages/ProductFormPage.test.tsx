import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ApiError } from '../api/client'
import * as products from '../api/products'
import ProductFormPage from './ProductFormPage'

const tumbler = { id: 7, name: '모두 텀블러 500ml', description: '하루 종일 차가운', price: 24000, imageUrl: 'https://img/t.png' }

const renderAt = (path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/products" element={<p>상품 목록 화면</p>} />
        <Route path="/products/new" element={<ProductFormPage />} />
        <Route path="/products/:id" element={<ProductFormPage />} />
      </Routes>
    </MemoryRouter>,
  )

describe('ProductFormPage', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('creates a product with a numeric price and a null image, then returns to the list', async () => {
    const create = vi.spyOn(products, 'createProduct').mockResolvedValue({ ...tumbler, id: 9 })
    renderAt('/products/new')

    expect(screen.queryByRole('button', { name: '삭제' })).not.toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('이름'), '모두 우산')
    await userEvent.type(screen.getByLabelText('가격'), '15000')
    await userEvent.type(screen.getByLabelText('설명'), '자동 우산')
    await userEvent.click(screen.getByRole('button', { name: '등록' }))

    expect(create).toHaveBeenCalledWith({ name: '모두 우산', description: '자동 우산', price: 15000, imageUrl: null })
    expect(await screen.findByText('상품 목록 화면')).toBeInTheDocument()
  })

  it('loads the product into the form and saves changes', async () => {
    vi.spyOn(products, 'getProduct').mockResolvedValue(tumbler)
    const update = vi.spyOn(products, 'updateProduct').mockResolvedValue({ ...tumbler, name: '모두 텀블러 750ml' })
    renderAt('/products/7')

    const name = await screen.findByLabelText('이름')
    expect(name).toHaveValue('모두 텀블러 500ml')
    expect(screen.getByLabelText('가격')).toHaveValue(24000)
    expect(screen.getByAltText('미리보기')).toHaveAttribute('src', 'https://img/t.png')

    await userEvent.clear(name)
    await userEvent.type(name, '모두 텀블러 750ml')
    await userEvent.click(screen.getByRole('button', { name: '저장' }))

    expect(update).toHaveBeenCalledWith('7', {
      name: '모두 텀블러 750ml',
      description: '하루 종일 차가운',
      price: 24000,
      imageUrl: 'https://img/t.png',
    })
    expect(await screen.findByText('저장했습니다')).toBeInTheDocument()
  })

  it('shows the server reason when the save is rejected with 400', async () => {
    vi.spyOn(products, 'getProduct').mockResolvedValue(tumbler)
    vi.spyOn(products, 'updateProduct').mockRejectedValue(new ApiError(400, '{"message":"price: 가격은 0 이상이어야 합니다."}'))
    renderAt('/products/7')

    await userEvent.click(await screen.findByRole('button', { name: '저장' }))

    expect(await screen.findByText('price: 가격은 0 이상이어야 합니다.')).toBeInTheDocument()
  })

  it('deletes after confirming and returns to the list', async () => {
    vi.spyOn(products, 'getProduct').mockResolvedValue(tumbler)
    const remove = vi.spyOn(products, 'deleteProduct').mockResolvedValue(undefined)
    const confirm = vi.spyOn(window, 'confirm').mockReturnValue(true)
    renderAt('/products/7')

    await userEvent.click(await screen.findByRole('button', { name: '삭제' }))

    expect(confirm).toHaveBeenCalledWith("'모두 텀블러 500ml' 상품을 삭제할까요?")
    expect(remove).toHaveBeenCalledWith('7')
    expect(await screen.findByText('상품 목록 화면')).toBeInTheDocument()
  })

  it('does not delete when the confirmation is cancelled', async () => {
    vi.spyOn(products, 'getProduct').mockResolvedValue(tumbler)
    const remove = vi.spyOn(products, 'deleteProduct').mockResolvedValue(undefined)
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    renderAt('/products/7')

    await userEvent.click(await screen.findByRole('button', { name: '삭제' }))

    expect(remove).not.toHaveBeenCalled()
  })

  it('says the product is gone when it was not found', async () => {
    vi.spyOn(products, 'getProduct').mockRejectedValue(new ApiError(404, '{"message":"없음"}'))
    renderAt('/products/7')

    expect(await screen.findByText('상품을 찾을 수 없습니다')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '← 상품 목록' })).toHaveAttribute('href', '/products')
  })
})
