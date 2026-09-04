import { fetchImageObjectUrl } from './storage'

const MAX_ENTRIES = 200
const cache = new Map<string, Promise<string>>()

/**
 * 파일명별로 object URL 을 한 번만 만든다. 목록에서 같은 이미지가 여러 번 쓰이거나
 * 페이지를 오갈 때 매번 다시 받지 않게 한다. 실패한 항목은 캐시에 남기지 않는다.
 */
export function loadImage(filename: string): Promise<string> {
  const hit = cache.get(filename)
  if (hit) return hit

  const pending = fetchImageObjectUrl(filename).catch((error) => {
    cache.delete(filename)
    throw error
  })
  cache.set(filename, pending)
  evictOldest()
  return pending
}

function evictOldest() {
  while (cache.size > MAX_ENTRIES) {
    const oldest = cache.keys().next().value
    if (oldest === undefined) return
    const evicted = cache.get(oldest)
    cache.delete(oldest)
    evicted?.then((url) => URL.revokeObjectURL(url)).catch(() => {})
  }
}

/** 테스트 전용. */
export function clearImageCache() {
  cache.forEach((p) => p.then((url) => URL.revokeObjectURL(url)).catch(() => {}))
  cache.clear()
}
