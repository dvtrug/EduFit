const metrics = [
  { value: '4/5', label: 'Milestone' },
  { value: '82%', label: 'Quiz gần nhất' },
  { value: '6', label: 'Buổi đã học' },
]

const tasks = [
  { icon: 'ƒ', title: 'Ôn phương trình', meta: 'Toán 9 · Hạn 18/09', tone: 'honey' },
  { icon: '✓', title: 'Quiz Đại số tuần 3', meta: 'Đã hoàn thành · 82%', tone: 'lavender' },
]

export function ProgressShowcase() {
  return (
    <section className="section progress-section" id="tien-do" aria-labelledby="progress-title">
      <div className="container">
        <div className="progress-intro">
          <div>
            <p className="eyebrow eyebrow--orange">TIẾN BỘ NHÌN THẤY ĐƯỢC</p>
            <h2 id="progress-title">Mỗi buổi học đều để lại một dấu mốc.</h2>
          </div>
          <p>
            Dữ liệu học tập và nhận xét của gia sư được tổng hợp thành báo cáo dễ đọc cho học sinh và phụ huynh.
          </p>
        </div>

        <div className="progress-stage">
          <div className="progress-stage__summary">
            <span>Tuần học của Minh</span>
            <strong>Đã hoàn thành 4 mục tiêu trong tuần này.</strong>
            <a href="#bat-dau" className="button button--light">Xem cách theo dõi <span aria-hidden="true">↗</span></a>
          </div>
          <div className="progress-stage__milestones">
            <article className="milestone-card milestone-card--lavender">
              <span>01 · ĐẠI SỐ</span><b>Phương trình bậc hai</b><small>4 hoạt động · 80%</small>
              <div className="progress"><span style={{ width: '80%' }} /></div>
            </article>
            <article className="milestone-card milestone-card--peach">
              <span>02 · HÌNH HỌC</span><b>Đường tròn</b><small>3 hoạt động · 60%</small>
              <div className="progress"><span style={{ width: '60%' }} /></div>
            </article>
            <article className="milestone-card milestone-card--lime">
              <span>03 · KỸ NĂNG</span><b>Trình bày lời giải</b><small>2 hoạt động · 45%</small>
              <div className="progress"><span style={{ width: '45%' }} /></div>
            </article>
          </div>
        </div>

        <div className="dashboard-grid">
          <div className="dashboard-main">
            <div className="metric-grid">
              {metrics.map((metric) => (
                <article className="metric-card" key={metric.label}>
                  <strong>{metric.value}</strong><span>{metric.label}</span>
                </article>
              ))}
              <article className="metric-card metric-card--wide">
                <div><strong>8.5h</strong><span>Thời gian học tháng này</span></div>
                <svg viewBox="0 0 180 64" role="img" aria-label="Xu hướng thời gian học ổn định">
                  <path d="M4 52 C30 40, 42 18, 62 34 S92 48, 104 26 S126 54, 140 22 S158 18, 176 10" />
                </svg>
              </article>
            </div>

            <div className="task-panel">
              <div className="panel-heading"><h3>Nhiệm vụ gần nhất</h3><span>2 hoạt động</span></div>
              <ul className="task-list">
                {tasks.map((task) => (
                  <li key={task.title}>
                    <span className={`task-icon task-icon--${task.tone}`} aria-hidden="true">{task.icon}</span>
                    <span><strong>{task.title}</strong><small>{task.meta}</small></span>
                    <span className="task-status">Đúng tiến độ</span>
                  </li>
                ))}
              </ul>
            </div>
          </div>

          <aside className="schedule-panel" aria-label="Lịch học sắp tới">
            <div className="panel-heading"><h3>Tháng 9</h3><span>2026</span></div>
            <div className="mini-calendar" aria-label="Lịch minh họa tháng 9 năm 2026">
              {['T2','T3','T4','T5','T6','T7','CN'].map((day) => <span className="mini-calendar__day" key={day}>{day}</span>)}
              {[14,15,16,17,18,19,20,21,22,23,24,25,26,27].map((date) => (
                <span className={date === 18 ? 'mini-calendar__date mini-calendar__date--active' : 'mini-calendar__date'} key={date}>{date}</span>
              ))}
            </div>
            <article className="upcoming-card">
              <span className="upcoming-card__icon" aria-hidden="true">∑</span>
              <div><small>19:00–20:30</small><strong>Toán 9 với cô Minh Anh</strong><span>Ôn phương trình bậc hai</span></div>
              <span aria-hidden="true">→</span>
            </article>
          </aside>
        </div>
      </div>
    </section>
  )
}
