import { useEffect, useState, type ReactNode } from 'react'
import { loadImage } from '../api/imageCache'

interface RemoteImageProps {
  filename?: string | null
  alt: string
  className?: string
  fallback?: ReactNode
}

/** 회원 프로필·배경 이미지를 게이트웨이 관리자 API 에서 blob 으로 받아 보여준다. */
export default function RemoteImage({ filename, alt, className, fallback }: RemoteImageProps) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null)
  const [status, setStatus] = useState<'loading' | 'loaded' | 'error'>(filename ? 'loading' : 'error')

  useEffect(() => {
    if (!filename) {
      setObjectUrl(null)
      setStatus('error')
      return
    }

    let cancelled = false
    setStatus('loading')
    setObjectUrl(null)

    loadImage(filename)
      .then((result) => {
        if (cancelled) return
        setObjectUrl(result)
        setStatus('loaded')
      })
      .catch(() => {
        if (!cancelled) setStatus('error')
      })

    return () => {
      cancelled = true
    }
  }, [filename])

  if (status === 'error') {
    return (
      <>{fallback ?? <div className={`avatar-placeholder ${className ?? ''}`}>{alt.charAt(0)}</div>}</>
    )
  }

  if (status === 'loading' || !objectUrl) {
    return <div className={`image-placeholder ${className ?? ''}`} aria-label={`${alt} 불러오는 중`} />
  }

  return <img src={objectUrl} alt={alt} className={className} />
}
