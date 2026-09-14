import { render, screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('EduFit landing page', () => {
  it('keeps the navigation bar at the top with a decorative underlay', () => {
    const { container } = render(<App />)

    expect(container.querySelector('header.site-header--sticky')).toBeInTheDocument()
    expect(container.querySelector('.site-header__underlay[aria-hidden="true"]')).toBeInTheDocument()
  })

  it('presents the core promise and primary matching action', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', {
        level: 1,
        name: /tìm gia sư phù hợp với cách bạn học/i,
      }),
    ).toBeInTheDocument()
    expect(
      screen.getAllByRole('link', { name: /bắt đầu tìm gia sư/i }).length,
    ).toBeGreaterThan(0)
  })

  it('exposes the main sections through accessible navigation', () => {
    render(<App />)

    const navigation = screen.getByRole('navigation', { name: /chính/i })
    expect(navigation).toBeInTheDocument()
    expect(within(navigation).getByRole('link', { name: /cách hoạt động/i })).toHaveAttribute(
      'href',
      '#cach-hoat-dong',
    )
    expect(within(navigation).getByRole('link', { name: /tiến độ học/i })).toHaveAttribute(
      'href',
      '#tien-do',
    )
  })

  it('lets visitors open an FAQ answer', async () => {
    const user = userEvent.setup()
    render(<App />)

    const question = screen.getByRole('button', {
      name: /edufit xác minh gia sư như thế nào/i,
    })
    expect(question).toHaveAttribute('aria-expanded', 'false')

    await user.click(question)

    expect(question).toHaveAttribute('aria-expanded', 'true')
    expect(
      screen.getByText(/hồ sơ chuyên môn và tài liệu minh chứng/i),
    ).toBeVisible()
  })
})
