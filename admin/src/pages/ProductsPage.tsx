import { type FormEvent, type KeyboardEvent, useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { PAGE_SIZE } from '../api/client'
import { searchProducts, type Product } from '../api/products'
import Pager from '../components/Pager'
import { formatPrice } from '../util/format'

export default function ProductsPage() {
  const navigate = useNavigate()
  const [keyword, setKeyword] = useState('')
  const [searchTerm, setSearchTerm] = useState('')
  const [page, setPage] = useState(0)
  const [products, setProducts] = useState<Product[]>([])
  /** 서버가 돌려준 페이지 번호(0-based). 순번은 지금 표에 깔린 데이터 기준으로 매긴다. */
  const [pageNumber, setPageNumber] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    searchProducts(searchTerm, page)
      .then((result) => {
        if (cancelled) return
        setProducts(result.content)
        setPageNumber(result.number)
        setTotalPages(result.totalPages)
      })
      .catch(() => {
        if (!cancelled) setError('상품 목록을 불러오지 못했습니다')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [searchTerm, page])

  const onSearch = (e: FormEvent) => {
    e.preventDefault()
    setPage(0)
    setSearchTerm(keyword.trim())
  }

  const open = (id: number) => navigate(`/products/${id}`)
  const onRowKey = (e: KeyboardEvent, id: number) => {
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault()
      open(id)
    }
  }

  return (
    <div>
      <h1>상품 관리</h1>
      <div className="list-controls">
        <form className="search-form" onSubmit={onSearch}>
          <input
            type="text"
            aria-label="상품 검색"
            placeholder="상품 이름/설명 검색"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
          <button type="submit" className="btn btn--primary">
            검색
          </button>
        </form>
        <Link to="/products/new" className="btn btn--primary">
          상품 등록
        </Link>
      </div>

      {loading && <p>불러오는 중...</p>}
      {error && <p className="error-text">{error}</p>}
      {!loading && !error && products.length === 0 && (
        <p>{searchTerm ? '검색 결과가 없습니다' : '등록된 상품이 없습니다'}</p>
      )}

      {!loading && !error && products.length > 0 && (
        <>
          <table className="list-table">
            {/* 열 너비를 비율로 못 박는다. 안 그러면 페이지마다 내용 길이를 따라 열이 들썩인다. */}
            <colgroup>
              <col style={{ width: '6%' }} />
              <col style={{ width: '8%' }} />
              <col style={{ width: '30%' }} />
              <col style={{ width: '14%' }} />
              <col style={{ width: '42%' }} />
            </colgroup>
            <thead>
              <tr>
                <th className="num-cell">번호</th>
                <th aria-label="사진" />
                <th>이름</th>
                <th>가격</th>
                <th>설명</th>
              </tr>
            </thead>
            <tbody>
              {products.map((p, i) => (
                <tr
                  key={p.id}
                  className="clickable-row"
                  role="button"
                  tabIndex={0}
                  onClick={() => open(p.id)}
                  onKeyDown={(e) => onRowKey(e, p.id)}
                >
                  <td className="num-cell">{pageNumber * PAGE_SIZE + i + 1}</td>
                  <td className="avatar-cell">
                    {/* 옆 칸이 이름을 말하므로 사진은 장식이다. */}
                    {p.imageUrl ? (
                      <img src={p.imageUrl} alt="" className="product-thumb" />
                    ) : (
                      <div className="product-thumb image-placeholder" />
                    )}
                  </td>
                  <td title={p.name}>{p.name}</td>
                  <td>{formatPrice(p.price)}</td>
                  <td title={p.description}>{p.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pager page={page} totalPages={totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}
