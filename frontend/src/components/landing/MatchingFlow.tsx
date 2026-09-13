import { matchingSteps } from '../../data/landingData'

export function MatchingFlow() {
  return (
    <section className="section matching-flow" id="cach-hoat-dong" aria-labelledby="flow-title">
      <div className="container">
        <div className="section-heading section-heading--center">
          <p className="eyebrow">BA BƯỚC, MỘT LỰA CHỌN RÕ RÀNG</p>
          <h2 id="flow-title">Từ nhu cầu học đến gia sư phù hợp.</h2>
          <p>Không phải tự lọc hàng chục hồ sơ. EduFit giúp bạn đi qua từng quyết định cần thiết.</p>
        </div>
        <ol className="step-grid">
          {matchingSteps.map((step) => (
            <li className={`step-card step-card--${step.tone}`} key={step.number}>
              <span className="step-card__number">{step.number}</span>
              <span className="step-card__symbol" aria-hidden="true">✦</span>
              <h3>{step.title}</h3>
              <p>{step.detail}</p>
              <span className="step-card__line" aria-hidden="true" />
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}
