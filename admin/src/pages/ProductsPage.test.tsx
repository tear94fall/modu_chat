import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import * as products from '../api/products'
import ProductsPage from './ProductsPage'

const tumbler = { id: 2, name: '모두 텀블러 500ml', description: '하루 종일 차가운', price: 24000, imageUrl: null }
const keyboard = { id: 4, name: '모두 기계식 키보드', description: '저소음 적축', price: 129000, imageUrl: 'https://img/k.png' }
const page = (content: products.Product[], totalPages = 1) => ({
  content,
  totalElements: content.length,
  totalPages,
  number: 0,
  size: 15,
})

const renderPage = () =>
  render(
    <MemoryRouter initialEntries={['/products']}>
      <Routes>
        <Route path="/products" element={<ProductsPage />} />
        <Route path="/products/:id" element={<p>수정 화면</p>} />
      </Routes>
    </MemoryRouter>,
  )

describe('ProductsPage', () => {
  beforeEach(() => vi.restoreAllMocks())

  it('lists products with formatted prices', async () => {
    vi.spyOn(products, 'searchProducts').mockResolvedValue(page([keyboard, tumbler]))
    renderPage()

    expect(await screen.findByText('모두 기계식 키보드')).toBeInTheDocument()
    expect(screen.getByText('129,000원')).toBeInTheDocument()
    expect(screen.getByText('24,000원')).toBeInTheDocument()
  })

  it('goes back to the first page when searching', async () => {
    const search = vi.spyOn(products, 'searchProducts').mockResolvedValue(page([keyboard], 3))
    renderPage()
    await screen.findByText('모두 기계식 키보드')

    await userEvent.click(screen.getByRole('button', { name: '다음' }))
    expect(search).toHaveBeenLastCalledWith('', 1)

    await userEvent.type(await screen.findByLabelText('상품 검색'), '키보드')
    await userEvent.click(screen.getByRole('button', { name: '검색' }))
    expect(search).toHaveBeenLastCalledWith('키보드', 0)
  })

  it('opens the edit page when a row is clicked', async () => {
    vi.spyOn(products, 'searchProducts').mockResolvedValue(page([tumbler]))
    renderPage()

    await userEvent.click(await screen.findByText('모두 텀블러 500ml'))

    expect(await screen.findByText('수정 화면')).toBeInTheDocument()
  })

  it('says so when nothing matches the search', async () => {
    const search = vi.spyOn(products, 'searchProducts').mockResolvedValue(page([keyboard]))
    renderPage()
    await screen.findByText('모두 기계식 키보드')

    search.mockResolvedValue(page([]))
    await userEvent.type(screen.getByLabelText('상품 검색'), '없는상품')
    await userEvent.click(screen.getByRole('button', { name: '검색' }))

    expect(await screen.findByText('검색 결과가 없습니다')).toBeInTheDocument()
  })

  it('links to the create page', async () => {
    vi.spyOn(products, 'searchProducts').mockResolvedValue(page([]))
    renderPage()

    expect(await screen.findByRole('link', { name: '상품 등록' })).toHaveAttribute('href', '/products/new')
  })
})
