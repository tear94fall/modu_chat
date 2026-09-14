import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { logout as logoutRequest } from '../api/auth'
import { clearToken } from '../auth/token'

/** 사이드바 메뉴를 서비스별로 묶는다. 푸시·앱 설정은 채팅 앱의 기능이라 채팅 묶음에 둔다. */
const SECTIONS = [
  { title: '회원', links: [{ to: '/members', label: '회원' }] },
  {
    title: '채팅',
    links: [
      { to: '/rooms', label: '채팅방' },
      { to: '/push', label: '푸시' },
      { to: '/settings', label: '앱 설정' },
    ],
  },
  { title: '커머스', links: [{ to: '/products', label: '상품' }] },
]

export default function Layout() {
  const navigate = useNavigate()

  const logout = () => {
    logoutRequest()
      .catch(() => {})
      .finally(() => {
        clearToken()
        navigate('/login')
      })
  }

  return (
    <div className="layout">
      <nav className="sidebar">
        <div className="brand">
          {/* 옆 글자가 이름을 말하므로 로고는 장식이다 — alt 를 비워 화면 낭독기가 두 번 읽지 않게 한다. */}
          <img src="/favicon.svg" alt="" className="brand-logo" />
          <span>모두메신저 백오피스</span>
        </div>
        <div className="sidebar-nav">
          {SECTIONS.map((section) => (
            <div key={section.title} className="nav-section">
              <div className="nav-section-title">{section.title}</div>
              {section.links.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
                >
                  {link.label}
                </NavLink>
              ))}
            </div>
          ))}
        </div>
        <div className="sidebar-footer">
          <NavLink to="/me" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
            내 정보
          </NavLink>
          <button type="button" className="btn btn--ghost" onClick={logout}>
            로그아웃
          </button>
        </div>
      </nav>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
