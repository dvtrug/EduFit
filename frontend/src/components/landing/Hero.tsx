import { matchCards } from '../../data/landingData'

export function Hero() {
  return (
    <section className="hero section" id="top" aria-labelledby="hero-title">
      <div className="container hero__grid">
        <div className="hero__copy">
          <p className="eyebrow">MATCHING MINH BẠCH · HỌC CÓ LỘ TRÌNH</p>
          <h1 id="hero-title">Tìm gia sư phù hợp với cách bạn học.</h1>
          <p className="hero__lead">
            EduFit chọn lọc gia sư, giải thích lý do phù hợp và giúp gia đình
            theo dõi tiến độ sau từng buổi học.
          </p>
          <div className="hero__actions">
            <a className="button button--primary" href="#bat-dau">
              Bắt đầu tìm gia sư <span aria-hidden="true">↗</span>
            </a>
            <a className="button button--secondary" href="#cach-hoat-dong">
              Xem EduFit hoạt động
            </a>
          </div>
          <p className="hero__note">Miễn phí khám phá · Bạn quyết định khi nào bắt đầu</p>
        </div>

        <div className="matching-stage" aria-label="Minh họa kết quả matching EduFit">
          <div className="matching-stage__intro">
            <span>Gợi ý dành cho Minh</span>
            <strong>Đã tìm thấy lựa chọn phù hợp.</strong>
            <p>Gợi ý rõ ràng, không phải một danh sách dài khó chọn.</p>
          </div>
          <div className="matching-stage__cards">
            {matchCards.map((card) => (
              <article className={`match-card match-card--${card.tone}`} key={card.eyebrow}>
                <span className="match-card__eyebrow">{card.eyebrow}</span>
                <span className="match-card__icon" aria-hidden="true">✦</span>
                <h2>{card.title}</h2>
                <p>{card.detail}</p>
                <div className="progress" aria-label={`${card.progress}%`}>
                  <span style={{ width: `${card.progress}%` }} />
                </div>
              </article>
            ))}
          </div>
          <div className="matching-stage__scribble" aria-hidden="true">✳</div>
        </div>
      </div>
    </section>
  )
}
