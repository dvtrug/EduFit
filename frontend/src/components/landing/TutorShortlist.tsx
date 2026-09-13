import { tutors } from '../../data/landingData'

export function TutorShortlist() {
  return (
    <section className="section tutor-section" id="gia-su" aria-labelledby="tutor-title">
      <div className="container tutor-section__layout">
        <div className="section-heading">
          <p className="eyebrow eyebrow--orange">GỢI Ý CÓ GIẢI THÍCH</p>
          <h2 id="tutor-title">Không chỉ là một danh sách gia sư.</h2>
          <p>
            Mỗi gợi ý cho biết điểm phù hợp đến từ đâu. Gia đình có đủ thông tin để tự đưa ra lựa chọn cuối cùng.
          </p>
          <ul className="check-list">
            <li>Đúng môn và cấp độ</li>
            <li>Hợp mục tiêu và cách học</li>
            <li>Khớp lịch, khu vực và ngân sách</li>
          </ul>
        </div>

        <div className="product-window" aria-label="Danh sách gia sư minh họa">
          <div className="product-window__header">
            <div>
              <span className="product-window__label">KẾT QUẢ CHO MINH</span>
              <h3>Gia sư phù hợp</h3>
            </div>
            <span className="demo-badge">Dữ liệu minh họa</span>
          </div>
          <div className="tutor-list">
            {tutors.map((tutor) => (
              <article className="tutor-card" key={tutor.name}>
                <div className={`avatar avatar--${tutor.tone}`} aria-hidden="true">{tutor.initials}</div>
                <div className="tutor-card__identity">
                  <h4>{tutor.name}</h4>
                  <p>{tutor.subject}</p>
                </div>
                <div className="tutor-card__match">
                  <strong>{tutor.score}</strong>
                  <div className="tag-list">
                    {tutor.reasons.map((reason) => <span key={reason}>{reason}</span>)}
                  </div>
                </div>
                <a href="#bat-dau" className="text-link" aria-label={`Xem hồ sơ ${tutor.name}`}>
                  Xem hồ sơ <span aria-hidden="true">→</span>
                </a>
              </article>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
