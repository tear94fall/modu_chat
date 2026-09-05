import type { Member } from '../api/members'
import { formatDateTime, formatRole } from '../util/format'
import RemoteImage from './RemoteImage'

interface MemberCardProps {
  member: Member
  friendCount: number
  createdDate?: string
}

export default function MemberCard({ member, friendCount, createdDate }: MemberCardProps) {
  const isAdmin = member.role === 'ROLE_ADMIN'

  return (
    <div className="profile-card">
      {member.wallpaperImage ? (
        <RemoteImage
          filename={member.wallpaperImage}
          alt={`${member.username} 배경 이미지`}
          className="profile-banner"
          fallback={<div className="profile-banner profile-banner--empty" />}
        />
      ) : (
        <div className="profile-banner profile-banner--empty" />
      )}

      <div className="profile-header">
        <RemoteImage
          filename={member.profileImage}
          alt={member.username}
          className="avatar"
          fallback={<div className="avatar avatar-placeholder">{member.username.charAt(0)}</div>}
        />

        <div className="profile-header-text">
          <div className="profile-name-row">
            <h1 className="profile-username">{member.username}</h1>
            <span className={`role-badge ${isAdmin ? 'role-badge--admin' : 'role-badge--member'}`}>
              {formatRole(member.role)}
            </span>
          </div>
          <p className="profile-email">{member.email}</p>
        </div>
      </div>

      <dl className="detail-grid">
        <dt>사용자 ID</dt>
        <dd>{member.userId}</dd>
        <dt>가입일</dt>
        <dd>{formatDateTime(createdDate ?? member.createdDate)}</dd>
        <dt>친구 수</dt>
        <dd>{friendCount}</dd>
        <dt>상태 메시지</dt>
        <dd>{member.statusMessage ?? '-'}</dd>
        <dt>프로필 이미지 파일명</dt>
        <dd className="detail-grid-filename">{member.profileImage ?? '-'}</dd>
        <dt>배경 이미지 파일명</dt>
        <dd className="detail-grid-filename">{member.wallpaperImage ?? '-'}</dd>
      </dl>
    </div>
  )
}
