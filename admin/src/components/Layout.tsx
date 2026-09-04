import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { logout as logoutRequest } from '../api/auth'
import { clearToken } from '../auth/token'

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
        <div className="brand">모두메신저 백오피스</div>
        <div className="sidebar-nav">
          <NavLink to="/members" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
            회원
          </NavLink>
          <NavLink to="/rooms" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
            채팅방
          </NavLink>
          <NavLink to="/push" className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}>
            푸시
          </NavLink>
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
