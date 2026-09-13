import { trustItems } from '../../data/landingData'

export function TrustStrip() {
  return (
    <section className="trust-strip" aria-label="Cam kết của EduFit">
      <div className="container trust-strip__grid">
        {trustItems.map((item) => (
          <article className="trust-item" key={item.index}>
            <span className="trust-item__index">{item.index}</span>
            <div>
              <h2>{item.label}</h2>
              <p>{item.detail}</p>
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}
