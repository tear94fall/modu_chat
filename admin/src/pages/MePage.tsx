import { useEffect, useState } from 'react'
import { getMe, updateMe, type Member } from '../api/members'
import { uploadImage } from '../api/storage'
import MemberCard from '../components/MemberCard'
import RemoteImage from '../components/RemoteImage'

const MAX_IMAGE_BYTES = 5 * 1024 * 1024
const IMAGE_GUARD_MESSAGE = '이미지 파일만 5MB 이하로 올릴 수 있습니다'

type ImageField = 'profileImage' | 'wallpaperImage'

interface EditForm {
  username: string
  statusMessage: string
  profileImage: string
  wallpaperImage: string
}

function toForm(member: Member): EditForm {
  return {
    username: member.username ?? '',
    statusMessage: member.statusMessage ?? '',
    profileImage: member.profileImage ?? '',
    wallpaperImage: member.wallpaperImage ?? '',
  }
}

export default function MePage() {
  const [member, setMember] = useState<Member | null>(null)
  const [friendCount, setFriendCount] = useState(0)
  const [createdDate, setCreatedDate] = useState<string | undefined>(undefined)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const [editing, setEditing] = useState(false)
  const [form, setForm] = useState<EditForm | null>(null)
  const [uploading, setUploading] = useState<Record<ImageField, boolean>>({
    profileImage: false,
    wallpaperImage: false,
  })
  const [imageError, setImageError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saveMessage, setSaveMessage] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    getMe()
      .then((result) => {
        if (cancelled) return
        setMember(result.member)
        setFriendCount(result.friendCount)
        setCreatedDate(result.createdDate)
      })
      .catch(() => {
        if (!cancelled) setError('내 정보를 불러오지 못했습니다')
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [])

  function startEdit() {
    if (!member) return
    setForm(toForm(member))
    setImageError(null)
    setSaveError(null)
    setSaveMessage(null)
    setEditing(true)
  }

  function cancelEdit() {
    setEditing(false)
    setForm(null)
    setImageError(null)
    setSaveError(null)
  }

  async function handleImageChange(field: ImageField, file: File | null) {
    if (!file) return
    setImageError(null)

    if (!file.type.startsWith('image/') || file.size > MAX_IMAGE_BYTES) {
      setImageError(IMAGE_GUARD_MESSAGE)
      return
    }

    setUploading((prev) => ({ ...prev, [field]: true }))
    try {
      const filename = await uploadImage(file)
      setForm((prev) => (prev ? { ...prev, [field]: filename } : prev))
    } catch {
      setImageError('이미지를 올리지 못했습니다')
    } finally {
      setUploading((prev) => ({ ...prev, [field]: false }))
    }
  }

  function clearImage(field: ImageField) {
    setForm((prev) => (prev ? { ...prev, [field]: '' } : prev))
  }

  async function handleSave() {
    if (!form) return
    const username = form.username.trim()
    if (!username) {
      setSaveError('이름을 입력하세요')
      return
    }

    setSaving(true)
    setSaveError(null)
    setSaveMessage(null)
    try {
      const result = await updateMe({
        username,
        statusMessage: form.statusMessage,
        profileImage: form.profileImage,
        wallpaperImage: form.wallpaperImage,
      })
      setMember(result.member)
      setFriendCount(result.friendCount)
      setCreatedDate(result.createdDate)
      setEditing(false)
      setForm(null)
      setSaveMessage('저장했습니다')
    } catch {
      setSaveError('저장하지 못했습니다')
    } finally {
      setSaving(false)
    }
  }

  if (loading) return <p>불러오는 중...</p>
  if (error) return <p className="error-text">{error}</p>
  if (!member) return <p>회원을 찾을 수 없습니다</p>

  return (
    <div>
      <h1>내 정보</h1>

      {saveMessage && <p className="result-text">{saveMessage}</p>}

      {!editing && (
        <>
          <MemberCard member={member} friendCount={friendCount} createdDate={createdDate} />
          <div className="actions-frame">
            <div className="form-actions">
              <button type="button" className="btn btn--primary" onClick={startEdit}>
                수정
              </button>
            </div>
          </div>
        </>
      )}

      {editing && form && (
        <form
          className="form-card"
          onSubmit={(e) => {
            e.preventDefault()
            handleSave()
          }}
        >
          <div className="form-section">
            <div className="form-field">
              <label htmlFor="me-username">이름</label>
              <input
                id="me-username"
                value={form.username}
                required
                onChange={(e) => setForm({ ...form, username: e.target.value })}
              />
            </div>

            <div className="form-field">
              <label htmlFor="me-status-message">상태 메시지</label>
              <input
                id="me-status-message"
                value={form.statusMessage}
                onChange={(e) => setForm({ ...form, statusMessage: e.target.value })}
              />
            </div>

            <div className="form-field">
              <label>프로필 이미지</label>
              <div className="file-field">
                <div className="file-preview">
                  <RemoteImage
                    filename={form.profileImage}
                    alt="프로필 이미지 미리보기"
                    className="avatar avatar--md"
                    fallback={
                      <div className="avatar avatar--md avatar-placeholder">
                        {(form.username || member.username).charAt(0)}
                      </div>
                    }
                  />
                </div>
                <input
                  id="me-profile-image"
                  type="file"
                  accept="image/*"
                  className="file-input"
                  aria-label="프로필 이미지"
                  onChange={(e) => handleImageChange('profileImage', e.target.files?.[0] ?? null)}
                />
                {uploading.profileImage ? (
                  <span className="form-hint">업로드 중…</span>
                ) : (
                  <div className="file-actions">
                    <label htmlFor="me-profile-image" className="btn btn--secondary btn--sm">
                      이미지 선택
                    </label>
                    {form.profileImage && (
                      <button
                        type="button"
                        className="btn btn--danger btn--sm"
                        onClick={() => clearImage('profileImage')}
                      >
                        제거
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>

            <div className="form-field">
              <label>배경 이미지</label>
              <div className="file-field">
                <div className="file-preview file-preview--banner">
                  <RemoteImage
                    filename={form.wallpaperImage}
                    alt="배경 이미지 미리보기"
                    className="profile-banner"
                    fallback={<div className="profile-banner profile-banner--empty" />}
                  />
                </div>
                <input
                  id="me-wallpaper-image"
                  type="file"
                  accept="image/*"
                  className="file-input"
                  aria-label="배경 이미지"
                  onChange={(e) => handleImageChange('wallpaperImage', e.target.files?.[0] ?? null)}
                />
                {uploading.wallpaperImage ? (
                  <span className="form-hint">업로드 중…</span>
                ) : (
                  <div className="file-actions">
                    <label htmlFor="me-wallpaper-image" className="btn btn--secondary btn--sm">
                      이미지 선택
                    </label>
                    {form.wallpaperImage && (
                      <button
                        type="button"
                        className="btn btn--danger btn--sm"
                        onClick={() => clearImage('wallpaperImage')}
                      >
                        제거
                      </button>
                    )}
                  </div>
                )}
              </div>
            </div>
          </div>

          {imageError && <p className="error-text">{imageError}</p>}
          {saveError && <p className="error-text">{saveError}</p>}

          <div className="form-actions">
            <button type="button" className="btn btn--secondary" onClick={cancelEdit}>
              취소
            </button>
            <button
              type="submit"
              className="btn btn--primary"
              disabled={saving || uploading.profileImage || uploading.wallpaperImage}
            >
              저장
            </button>
          </div>
        </form>
      )}
    </div>
  )
}
