import { API_BASE_URL, ApiError } from './client'
import { getToken } from '../auth/token'

/** 이미지에는 Authorization 헤더가 필요해 <img src> 로 직접 못 부른다. blob 으로 받아 object URL 을 만든다. */
export async function fetchImageObjectUrl(filename: string): Promise<string> {
  const token = getToken()
  const res = await fetch(`${API_BASE_URL}/storage-service/api-admin/view/${encodeURIComponent(filename)}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })
  if (!res.ok) throw new ApiError(res.status, 'image load failed')
  return URL.createObjectURL(await res.blob())
}

/** FormData 는 Content-Type 을 브라우저가 정해야 하므로 api() 를 쓰지 않는다. 저장된 파일명을 돌려준다. */
export async function uploadImage(file: File): Promise<string> {
  const token = getToken()
  const formData = new FormData()
  formData.append('file', file)

  const res = await fetch(`${API_BASE_URL}/storage-service/api-admin/upload`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: formData,
  })
  if (!res.ok) throw new ApiError(res.status, await res.text())
  return (await res.text()).trim()
}
