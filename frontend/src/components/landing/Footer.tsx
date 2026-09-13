import { Logo } from './Logo'

export function Footer() {
  return (
    <>
      <section className="section final-cta" id="bat-dau" aria-labelledby="cta-title">
        <div className="container">
          <div className="final-cta__card">
            <span className="final-cta__spark" aria-hidden="true">✦</span>
            <p className="eyebrow">SẴN SÀNG HỌC RÕ RÀNG HƠN?</p>
            <h2 id="cta-title">Bắt đầu bằng một lựa chọn phù hợp hơn.</h2>
            <p>Chia sẻ mục tiêu của bạn và khám phá những gia sư phù hợp nhất.</p>
            <a className="button button--honey" href="#gia-su">Xem gợi ý gia sư <span aria-hidden="true">↗</span></a>
          </div>
        </div>
      </section>
      <footer className="site-footer">
        <div className="container site-footer__main">
          <div><Logo /><p>Đúng gia sư · Đúng mục tiêu · Thấy rõ tiến bộ</p></div>
          <nav aria-label="Điều hướng cuối trang">
            <a href="#cach-hoat-dong">Cách hoạt động</a><a href="#gia-su">Gia sư</a><a href="#faq">Hỗ trợ</a>
          </nav>
        </div>
        <div className="container site-footer__bottom"><span>© 2026 EduFit</span><span>Quyền riêng tư · Điều khoản</span></div>
      </footer>
    </>
  )
}
