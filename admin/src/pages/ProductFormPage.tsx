import { type FormEvent, useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ApiError } from '../api/client'
import { createProduct, deleteProduct, getProduct, type ProductInput, updateProduct, validationMessage } from '../api/products'

/** /products/new 는 등록, /products/:id 는 수정·삭제. 입력칸은 같다. */
export default function ProductFormPage() {
  const { id } = useParams<{ id: string }>()
  const editing = id !== undefined
  const navigate = useNavigate()

  const [name, setName] = useState('')
  const [price, setPrice] = useState('')
  const [description, setDescription] = useState('')
  const [imageUrl, setImageUrl] = useState('')
  /** 삭제 확인 문구에 쓴다. 입력 중인 이름이 아니라 서버에 저장된 이름이다. */
  const [savedName, setSavedName] = useState('')
  /** 미리보기를 못 그린 주소. 주소를 고치면 지금 주소와 달라져 실패 표시가 저절로 사라진다. */
  const [failedPreview, setFailedPreview] = useState<string | null>(null)

  const [loading, setLoading] = useState(editing)
  const [notFound, setNotFound] = useState(false)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!id) return
    let cancelled = false
    setLoading(true)
    getProduct(id)
      .then((p) => {
        if (cancelled) return
        setName(p.name)
        setPrice(String(p.price))
        setDescription(p.description)
        setImageUrl(p.imageUrl ?? '')
        setSavedName(p.name)
      })
      .catch((err) => {
        if (cancelled) return
        if (err instanceof ApiError && err.status === 404) setNotFound(true)
        else setLoadError('상품을 불러오지 못했습니다')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [id])

  const input = (): ProductInput => ({
    name: name.trim(),
    description: description.trim(),
    price: Number(price),
    imageUrl: imageUrl.trim() === '' ? null : imageUrl.trim(),
  })

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault()
    setMessage(null)
    setError(null)
    setSubmitting(true)
    try {
      if (id) {
        const saved = await updateProduct(id, input())
        setSavedName(saved.name)
        setMessage('저장했습니다')
      } else {
        await createProduct(input())
        navigate('/products')
      }
    } catch (err) {
      setError(validationMessage(err) ?? '저장하지 못했습니다')
    } finally {
      setSubmitting(false)
    }
  }

  const onDelete = async () => {
    if (!id || !window.confirm(`'${savedName}' 상품을 삭제할까요?`)) return
    setMessage(null)
    setError(null)
    setSubmitting(true)
    try {
      await deleteProduct(id)
      navigate('/products')
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) setNotFound(true)
      else setError('삭제하지 못했습니다')
      setSubmitting(false)
    }
  }

  const backLink = (
    <Link to="/products" className="back-link">
      ← 상품 목록
    </Link>
  )

  if (loading) return <p>불러오는 중...</p>
  if (notFound)
    return (
      <div>
        {backLink}
        <p>상품을 찾을 수 없습니다</p>
      </div>
    )
  if (loadError) return <p className="error-text">{loadError}</p>

  const preview = imageUrl.trim()

  return (
    <div>
      {backLink}
      <h1>{editing ? '상품 수정' : '상품 등록'}</h1>
      <form className="form-card" onSubmit={onSubmit}>
        <div className="form-section">
          <div className="form-field">
            <label htmlFor="product-name">이름</label>
            <input id="product-name" value={name} maxLength={100} required onChange={(e) => setName(e.target.value)} />
          </div>
          <div className="form-field">
            <label htmlFor="product-price">가격</label>
            <input
              id="product-price"
              type="number"
              min={0}
              step={1}
              value={price}
              required
              onChange={(e) => setPrice(e.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="product-description">설명</label>
            <textarea
              id="product-description"
              rows={4}
              maxLength={500}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div className="form-field">
            <label htmlFor="product-image">이미지 URL</label>
            <input
              id="product-image"
              type="url"
              placeholder="https://"
              value={imageUrl}
              onChange={(e) => setImageUrl(e.target.value)}
            />
            {preview !== '' &&
              (failedPreview === preview ? (
                <p className="form-hint">이미지를 불러올 수 없습니다</p>
              ) : (
                <img src={preview} alt="미리보기" className="product-preview" onError={() => setFailedPreview(preview)} />
              ))}
          </div>
        </div>
        <div className="form-actions">
          {editing && (
            <button type="button" className="btn btn--danger" onClick={onDelete} disabled={submitting}>
              삭제
            </button>
          )}
          <button type="submit" className="btn btn--primary" disabled={submitting}>
            {editing ? '저장' : '등록'}
          </button>
        </div>
      </form>

      {message && <p className="result-text">{message}</p>}
      {error && <p className="error-text">{error}</p>}
    </div>
  )
}
