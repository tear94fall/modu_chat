import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import Layout from './Layout'

const renderLayout = () =>
  render(
    <MemoryRouter>
      <Layout />
    </MemoryRouter>,
  )

describe('Layout', () => {
  it('shows the brand logo next to the title', () => {
    const { container } = renderLayout()

    const logo = container.querySelector('.brand-logo')
    expect(logo).toBeInTheDocument()
    expect(logo).toHaveAttribute('src', '/favicon.svg')
    // 옆 글자가 이름을 말하므로 로고는 장식이다 — 낭독기가 두 번 읽으면 안 된다.
    expect(logo).toHaveAttribute('alt', '')
    expect(screen.getByText('모두메신저 백오피스')).toBeInTheDocument()
  })

  it('links to the main sections', () => {
    renderLayout()

    expect(screen.getByRole('link', { name: '회원' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '채팅방' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '상품' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '푸시' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '앱 설정' })).toBeInTheDocument()
  })

  it('groups the menu by service: 회원 / 채팅 / 커머스', () => {
    const { container } = renderLayout()

    // 섹션 제목 → 그 아래 링크 순서. 푸시·앱 설정은 채팅 앱 기능이라 채팅 묶음에 들어간다.
    const sections = Array.from(container.querySelectorAll('.nav-section')).map((section) => ({
      title: section.querySelector('.nav-section-title')?.textContent,
      links: Array.from(section.querySelectorAll('a')).map((a) => a.textContent),
    }))

    expect(sections).toEqual([
      { title: '회원', links: ['회원'] },
      { title: '채팅', links: ['채팅방', '푸시', '앱 설정'] },
      { title: '커머스', links: ['상품'] },
    ])
  })

  it('keeps 내 정보 and 로그아웃 in the footer, outside the sections', () => {
    const { container } = renderLayout()

    const footer = container.querySelector('.sidebar-footer')
    expect(footer?.querySelector('a')?.textContent).toBe('내 정보')
    expect(footer?.querySelector('button')?.textContent).toBe('로그아웃')
    expect(footer?.closest('.nav-section')).toBeNull()
  })
})
