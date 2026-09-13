import { useState } from 'react'
import { faqItems } from '../../data/landingData'

export function Faq() {
  const [openIndex, setOpenIndex] = useState<number | null>(null)

  return (
    <section className="section faq-section" id="faq" aria-labelledby="faq-title">
      <div className="container faq-layout">
        <div className="section-heading">
          <p className="eyebrow">CẦN BIẾT TRƯỚC KHI BẮT ĐẦU</p>
          <h2 id="faq-title">Câu hỏi thường gặp.</h2>
          <p>Nếu bạn còn câu hỏi khác, đội ngũ EduFit luôn sẵn sàng hỗ trợ.</p>
        </div>
        <div className="faq-list">
          {faqItems.map((item, index) => {
            const isOpen = openIndex === index
            const answerId = `faq-answer-${index}`
            return (
              <article className="faq-item" key={item.question}>
                <h3>
                  <button type="button" aria-expanded={isOpen} aria-controls={answerId} onClick={() => setOpenIndex(isOpen ? null : index)}>
                    <span>{item.question}</span><span aria-hidden="true">{isOpen ? '−' : '+'}</span>
                  </button>
                </h3>
                <div id={answerId} className="faq-answer" hidden={!isOpen}><p>{item.answer}</p></div>
              </article>
            )
          })}
        </div>
      </div>
    </section>
  )
}
