import { useState } from 'react'
import { navigation } from '../../data/landingData'
import { Logo } from './Logo'

export function Header() {
  const [open, setOpen] = useState(false)

  return (
    <header className="site-header site-header--sticky">
      <div className="container site-header__inner">
        <Logo />
        <button
          className="menu-button"
          type="button"
          aria-expanded={open}
          aria-controls="primary-navigation"
          onClick={() => setOpen((current) => !current)}
        >
          <span className="sr-only">Mở menu</span>
          <span aria-hidden="true">{open ? '×' : '☰'}</span>
        </button>
        <nav
          id="primary-navigation"
          className={`primary-nav${open ? ' primary-nav--open' : ''}`}
          aria-label="Điều hướng chính"
        >
          <div className="primary-nav__links">
            {navigation.map((item) => (
              <a key={item.href} href={item.href} onClick={() => setOpen(false)}>
                {item.label}
              </a>
            ))}
          </div>
          <div className="primary-nav__actions">
            <a className="button button--ghost" href="#dang-nhap">
              Đăng nhập
            </a>
            <a className="button button--primary button--compact" href="#bat-dau">
              Đăng ký
            </a>
          </div>
        </nav>
      </div>
      <span className="site-header__underlay" aria-hidden="true" />
    </header>
  )
}
